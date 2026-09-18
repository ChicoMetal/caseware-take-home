# Design

## 1. High-Level Architecture

The system introduces an **event-driven update tracking layer** between the existing Template DB and Engagement DB, avoiding the ~1 minute per-engagement loading constraint entirely.

### Components

- **Engagement-Template Index** — A lightweight read-optimized table storing `(engagementId, firmId, templateId, currentVersion)` for every engagement. Populated via events on engagement creation and update decisions; existing engagements are backfilled through a one-time batch process.
- **Template Update Service** — Reacts to template publication events. Queries the index for affected engagements, invokes the existing diff tool for each distinct version gap, transforms raw diffs into human-readable summaries, and stores the results.
- **Update Summary Store** — Caches pre-computed summaries keyed by `(templateId, fromVersion, toVersion)`. Shared across all firms since templates are identical for everyone.
- **REST API** — Serves the Angular client with engagement update state, change summaries, and decision endpoints.
- **Angular Client** — Displays engagement update status, presents human-readable change summaries, and collects apply/decline decisions.

### Data Flow

1. **Template published →** event triggers the Update Service.
2. Service queries the Index for engagements with `currentVersion < publishedVersion`.
3. For each distinct lagging version, the service invokes the existing diff tool and transforms the result into a human-readable summary.
4. Summaries are stored in the Update Summary Store (one entry per version pair, reused across firms).
5. Client polls the API; engagements with pending updates are surfaced immediately from the index.

### Client / Server Boundary

The server is responsible for all data retrieval, diff invocation, and the transformation of raw diffs into structured human-readable summaries. The client is responsible only for presentation and collecting user decisions. This boundary keeps the client thin and the transformation logic cacheable and testable.

### Client / Server Contract

```typescript
// GET /api/engagements/updates?firmId={firmId}
interface EngagementUpdateSummary {
  engagementId: string;
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

// GET /api/engagements/{engagementId}/update-details
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

// POST /api/engagements/{engagementId}/decision
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

## 2. Implementation Plan

1. **Domain models** — Records and enums representing templates, engagements, diffs, and summaries.
2. **Port interfaces** — Boundaries for external systems: diff tool, engagement index, template version provider, summary transformer.
3. **Domain service** — `UpdateStateResolver` orchestrates state resolution and detail retrieval through ports.
4. **Adapter** — `RuleBasedDiffSummaryTransformer` implements the deterministic diff-to-summary transformation.
5. **Angular models** — TypeScript interfaces mirroring the API contract.
6. **Angular API layer** — Observable-based HTTP simulation with configurable failure rate and latency. Returns `Observable<T>` using `structuredClone()` to prevent fixture mutation across calls.
7. **Angular Store** — Signal-based state container holding engagements, selected details, loading flags, and errors. Exposes derived signals (`pendingCount`, `hasError`).
8. **Angular Effects** — Side-effect orchestrator. Each command wraps its API call in `defer()` + `retry()` with exponential backoff. Clears stale state before each load to prevent showing deprecated data on failure. Retry configuration is injectable via `InjectionToken<RetryConfig>` for testability.
9. **Angular Facade** — Public API for components. Exposes read-only signals and delegates commands to Effects. Components never touch the API or Store directly.
10. **Angular components** — Engagement list with status indicators and error/retry UI. Update detail rendered in a modal overlay, reads directly from the Store via the Facade, and owns its own apply/decline actions.

## 3. Testing Strategy

**Server (Java):**
- `UpdateStateResolverTest` — Three scenarios: engagement up-to-date (returns `UP_TO_DATE`), one version behind (produces 1 collapsed summary + 1 step), two versions behind (produces 1 collapsed + 2 step-by-step summaries). Ports are mocked to isolate domain logic.
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

## 4. Evaluation & Observability

- **Metrics:** Count of engagements per status (per firm, per template). Summary computation latency. Apply/decline rates. Time-to-decision from first notification.
- **Alerts:** Engagements stuck in `COMPUTING` for more than 5 minutes. Index-to-template version inconsistencies detected by the reconciliation job.
- **Logging:** Template publication events processed. Diff invocations and summary transformations. User decisions recorded. Reconciliation job results.

## 5. Failure Modes & Tradeoffs

1. **Lost events** — If a template publication event is dropped, the reconciliation job (running periodically) detects engagements whose index version is behind the template's latest and triggers summary computation. The system is eventually consistent.
2. **Concurrent template publication during user review** — The `targetVersion` field in the decision request acts as an optimistic concurrency check. If a new version was published while the user was reviewing, the server rejects the decision and the client refreshes to show the new update.
3. **Backfill of existing engagements** — A one-time offline batch job loads each existing engagement (~1 min each), extracts `(templateId, version)`, and writes to the index. For ~100 engagements, this takes ~100 minutes and runs during low-traffic hours.
4. **Index staleness (CQRS tradeoff)** — The lightweight index introduces eventual consistency. The index may briefly lag behind actual engagement state. Mitigation: events propagate within seconds; reconciliation catches gaps.
5. **Summary store vs. in-memory cache** — A database table is sufficient for the expected volume (~tens of summary records). Redis/ElastiCache can be introduced if the number of templates or version pairs grows significantly.
6. **COMPUTING state visibility** — We expose the intermediate state to users rather than hiding it. This adds minor UI complexity but ensures users never see incomplete data presented as final.
