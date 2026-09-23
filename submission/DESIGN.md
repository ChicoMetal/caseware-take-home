# Design

## 1. High-Level Architecture

The system introduces an **event-driven update tracking layer** between the existing Template DB and Engagement DB, avoiding the ~1 minute per-engagement loading constraint entirely.

### Components

The architecture follows a CQRS (Command Query Responsibility Segregation) pattern, separating the write path (event processing) from the read path (client queries):

| Component | Role | Backed by |
|-----------|------|-----------|
| **Engagement-Template Index** | Lightweight read model storing materialized state per engagement: `(engagementId, firmId, templateId, currentVersion, latestVersion, status, declinedVersion, summaryAvailable)`. | Database table (fast reads) |
| **Update Summary Store** | Pre-computed human-readable change summaries, keyed by `(templateId, fromVersion, toVersion)`. Shared across all firms — same template gap produces identical summaries. | Database table |
| **Template Update Processor** *(write side)* | Reacts to template publication events. Computes diffs, transforms them into summaries, stores results, and materializes engagement state in the index. | Domain service |
| **Update State Resolver** *(read side)* | Serves client queries. Lists engagements (pure index read), retrieves pre-computed summaries, and processes user decisions with optimistic concurrency. | Domain service |
| **Angular Client** | Presents engagement status, change summaries, and collects apply/decline decisions. Redux-inspired architecture: API → Store → Effects → Facade. | Angular 18+ with signals |

### Data Flow

**Write path — when a template version is published:**

1. Publication event triggers `TemplateUpdateProcessor`.
2. Processor queries the Index for engagements where `currentVersion < publishedVersion`.
3. Groups affected engagements by their current version to identify distinct version gaps.
4. For each distinct gap, invokes the existing diff tool (`TemplateDiffProvider`) to compute both a collapsed diff (current → latest) and step-by-step diffs (each consecutive version).
5. Transforms each raw diff into a human-readable summary (`DiffSummaryTransformer`) and stores them in the Summary Store.
6. Updates each engagement's materialized state in the Index: sets `latestVersion`, resolves `status` (PENDING or stays DECLINED if the decline still covers this version), and marks `summaryAvailable = true`.

**Read path — when the client requests data:**

1. **List engagements:** Resolver reads directly from the Index — no computation, pure lookup. Maps each record to the API response shape with display name from `TemplateVersionProvider`.
2. **View details:** Resolver reads the pre-computed collapsed summary and step-by-step summaries from the Summary Store. If summaries aren't computed yet (status = COMPUTING), throws an error so the client can show a loading state.
3. **Submit decision:** Resolver validates `targetVersion` matches `latestVersion` (optimistic concurrency). For APPLY: advances `currentVersion` to target, sets status to UP_TO_DATE. For DECLINE: records `declinedVersion`, sets status to DECLINED. Both update the Index in a single atomic write.

**Reconciliation — safety net for missed events:**

A periodic job calls `TemplateUpdateProcessor.reconcile()`, which scans for engagements with missing summaries and computes them. This ensures eventual consistency even if a publication event is lost.

### Client / Server Boundary

The server is responsible for all data retrieval, diff invocation, and the transformation of raw diffs into structured human-readable summaries. The client is responsible only for presentation and collecting user decisions. This boundary keeps the client thin and the transformation logic cacheable and testable.

### Client / Server Contract

```typescript
// GET /api/firms/{firmId}/engagements/updates
interface EngagementUpdateSummary {
  engagementId: string;
  firmId: string;
  engagementName: string;
  templateId: string;
  templateDisplayName: string;
  currentVersion: number;
  latestVersion: number;
  status: 'UP_TO_DATE' | 'PENDING' | 'COMPUTING' | 'DECLINED' | 'ERROR';
  pendingUpdateCount: number;
  summaryAvailable: boolean;
  lastCheckedAt: string; // ISO-8601
  declinedVersion: number | null;
}

// GET /api/firms/{firmId}/engagements/{engagementId}/update-details
interface EngagementUpdateDetails {
  engagementId: string;
  currentVersion: number;
  latestVersion: number;
  collapsedSummary: ChangeSummary;      // direct diff: currentVersion → latestVersion
  stepByStepSummaries: ChangeSummary[]; // consecutive diffs for detailed context
  freshness: {
    computedAt: string;
    templatePublishedAt: string;
  };
}

interface ChangeSummary {
  fromVersion: number;
  toVersion: number;
  publishedAt: string;
  sections: SectionChange[];
  totalChanges: number;
}

interface SectionChange {
  sectionPath: string;
  sectionDisplayName: string;
  changes: HumanReadableChange[];
}

interface HumanReadableChange {
  type: 'ADDED' | 'MODIFIED' | 'REMOVED';
  description: string;
  impact: 'HIGH' | 'MEDIUM' | 'LOW';
}

// POST /api/firms/{firmId}/engagements/{engagementId}/decision
interface UpdateDecisionRequest {
  decision: 'APPLY' | 'DECLINE';
  targetVersion: number; // optimistic concurrency: must match current latest
}

interface UpdateDecisionResponse {
  engagementId: string;
  decision: 'APPLY' | 'DECLINE';
  previousVersion: number;
  targetVersion: number;
  status: 'ACCEPTED' | 'PROCESSING';
}
```

**Freshness and unavailable data:** The `status` field distinguishes between `PENDING` (summary ready), `COMPUTING` (summary being generated), and `DECLINED` (user declined the current latest version). The `summaryAvailable` flag allows the client to show a loading state for change details while still indicating that an update exists. `lastCheckedAt` communicates data freshness. When `status` is `DECLINED`, `declinedVersion` records which version was declined; if a newer version is published, the engagement returns to `PENDING`.

### Human-Readable Change Summary

The raw-to-human transformation is performed **server-side**, inside the `RuleBasedDiffSummaryTransformer` adapter. Reasons:

1. **Cacheable** — The same `(templateId, fromVersion, toVersion)` tuple produces identical summaries for all firms. Compute once, serve many.
2. **Template metadata proximity** — Resolving JSON paths (e.g., `/sections/planning/questions/7`) to display names (e.g., "Planning > Questions") requires template section metadata, which lives on the server.
3. **Consistency** — All users see the same language for the same change, regardless of client version.
4. **Testability** — Pure function: `(diff, metadata) → summary`. Easy to unit test.
5. **Extensibility** — A future LLM-based implementation can be swapped in behind the same `DiffSummaryTransformer` interface without touching the client.

## 2. Non-Functional Requirements

### Scale

- ~100 engagements per firm, ~tens of firms using shared product templates.
- Template updates published approximately weekly — not high-frequency.
- Change summaries keyed by `(templateId, fromVersion, toVersion)` are shared across all firms. Computed once per version gap, served to all. Storage volume is low (~tens of summary records).

### Performance

- **Hard constraint:** Loading an engagement file takes ~1 minute. The entire architecture exists to avoid this at query time.
- **Read path:** Pure index and store lookups. No diff computation, no engagement loading, no template fetching. Latency is bounded by database read time only.
- **Write path:** Diffs computed and summaries materialized asynchronously at template publication time. Processing is off the user's critical path — they see results only after materialization completes (`summaryAvailable` flag).

### Security

- **Firm-level data isolation (multi-tenancy):** All API endpoints are scoped under `/api/firms/{firmId}/`, making tenant context explicit at the URL level. The engagement index is partitioned by `firmId` — list queries enter through `findByFirmId(firmId)`, and single-entity lookups through `findById(firmId, engagementId)`, which returns empty if the engagement does not belong to the requesting firm. Cross-firm data leakage is a structural impossibility at the port boundary, not a correctness dependency on application-layer filtering.
- **Role-based authorization:** A `UserContext` record (`userId`, `firmId`, `role`) is extracted from the authentication token by the infrastructure layer and passed into domain services. The domain enforces access rules directly:

  | Operation | Required Role | Enforcement |
  |-----------|--------------|-------------|
  | List engagements | Any authenticated (ADMIN, VIEWER) | Tenant isolation via `firmId` |
  | View update details | Any authenticated (ADMIN, VIEWER) | Tenant isolation via `firmId` |
  | Apply / Decline decision | ADMIN only | `processDecision` throws `SecurityException` for non-ADMIN |

  The domain never inspects tokens — it receives a validated `UserContext` and checks `role` before mutating state. This keeps authorization logic testable without infrastructure dependencies.
- **Data residency:** The index and summary stores contain only version metadata and human-readable summaries derived from shared templates. No firm-specific financial data leaves the engagement boundary. For deployments with data-residency requirements, the lightweight index can be co-located with the engagement store in the required region; summaries (template-derived, not firm-specific) can be replicated globally.
- **Decision audit trail:** Every apply/decline decision records `(engagementId, decision, previousVersion, targetVersion, status)`. In the audit domain, every state transition must be traceable and defensible — this record is the minimum viable audit trail for template version decisions.

### Observability

- **Metrics:** Engagements per status (per firm, per template). Summary computation latency. Apply/decline rates. Time-to-decision from first notification.
- **Alerts:** Engagements stuck in `COMPUTING` for more than 5 minutes. Index-to-template version inconsistencies detected by the reconciliation job.
- **Logging:** Template publication events processed. Diff invocations and summary transformations. User decisions recorded. Reconciliation job results.

## 3. Implementation Plan

1. **Domain models** — Records and enums representing templates, engagements, diffs, and summaries.
2. **Port interfaces** — Boundaries for external systems: diff tool, engagement index, template version provider, summary transformer.
3. **Domain services (CQRS)** — `TemplateUpdateProcessor` (write side) computes summaries and materializes state at event time. `UpdateStateResolver` (read side) reads pre-materialized state and handles decisions.
4. **Adapter** — `RuleBasedDiffSummaryTransformer` implements the deterministic diff-to-summary transformation.
5. **Angular models** — TypeScript interfaces mirroring the API contract.
6. **Angular API layer** — Observable-based HTTP simulation with configurable failure rate and latency. Returns `Observable<T>` using `structuredClone()` to prevent fixture mutation across calls.
7. **Angular Store** — Signal-based state container holding engagements, selected details, loading flags, and errors. Exposes derived signals (`pendingCount`, `hasError`).
8. **Angular Effects** — Side-effect orchestrator. Each command wraps its API call in `defer()` + `retry()` with exponential backoff. Clears stale state before each load to prevent showing deprecated data on failure. Retry configuration is injectable via `InjectionToken<RetryConfig>` for testability.
9. **Angular Facade** — Public API for components. Exposes read-only signals and delegates commands to Effects. Components never touch the API or Store directly.
10. **Angular components** — Engagement list with status indicators and error/retry UI. Update detail rendered in a modal overlay, reads directly from the Store via the Facade, and owns its own apply/decline actions.

## 4. Testing Strategy

**Server (Java):**
- `TemplateUpdateProcessorTest` — Write side: publish computes summaries and updates state, preserves declined status when `declinedVersion >= publishedVersion`, supersedes decline on newer version (clears `declinedVersion` to null), reconcile computes missing summaries, reconcile skips existing.
- `UpdateStateResolverTest` — Read side: list returns pre-materialized state, COMPUTING state surfaced, detail reads from pre-computed store, missing summary throws, APPLY decision updates to UP_TO_DATE, DECLINE records `declinedVersion`, stale `targetVersion` rejected, VIEWER role denied on decision (SecurityException).
- `RuleBasedDiffSummaryTransformerTest` — Verifies all three operation types (`add`, `replace`, `remove`) produce correct `HumanReadableChange` entries with appropriate types, descriptions, and impact levels. Verifies changes are grouped by section.

**Client (Angular):**
- Facade integration tests (6 scenarios) using `fakeAsync`/`tick` with zero-delay retry configuration via `InjectionToken<RetryConfig>`:
  - Load engagements successfully through the API → Store → Facade pipeline.
  - Error propagation after exhausting retries (verifies retry count and error message).
  - Load engagement details by ID.
  - `APPLY` decision transitions engagement from `PENDING` to `UP_TO_DATE`, updates version, and clears selection.
  - `DECLINE` decision transitions engagement to `DECLINED` with `declinedVersion` recorded.
  - Recovery after transient API failure (retries succeed on subsequent attempts).

**Cross-cutting:**
- Manual verification that TypeScript interfaces, Java records, and the API contract in this document use identical field names and types.

## 5. Failure Modes & Tradeoffs

1. **Lost events** — If a template publication event is dropped, the reconciliation job (running periodically) detects engagements whose index version is behind the template's latest and triggers summary computation. The system is eventually consistent.
2. **Concurrent template publication during user review** — The `targetVersion` field in the decision request acts as an optimistic concurrency check. If a new version was published while the user was reviewing, the server rejects the decision and the client refreshes to show the new update.
3. **Backfill of existing engagements** — A one-time offline batch job loads each existing engagement (~1 min each), extracts `(templateId, version)`, and writes to the index. For ~100 engagements, this takes ~100 minutes and runs during low-traffic hours.
4. **Index staleness (CQRS tradeoff)** — The lightweight index introduces eventual consistency. The index may briefly lag behind actual engagement state. Mitigation: events propagate within seconds; reconciliation catches gaps.
5. **Summary store vs. in-memory cache** — A database table is sufficient for the expected volume (~tens of summary records). Redis/ElastiCache can be introduced if the number of templates or version pairs grows significantly.
6. **COMPUTING state visibility** — We expose the intermediate state to users rather than hiding it. This adds minor UI complexity but ensures users never see incomplete data presented as final.
