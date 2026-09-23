# Submission Notes

## Assumptions Made

1. **Updates target only the latest version** — Users cannot apply an intermediate template version. Templates are cumulative; an intermediate version is already outdated when a newer one exists. The user sees the full change context via step-by-step summaries but makes a single decision: advance to the latest or stay.

2. **The diff tool supports direct comparison between any two versions** — Not limited to consecutive versions. This is confirmed by the provided sample file `template-diff-review-ca-v6-v8.json`, which compares v6 directly to v8.

3. **Existing engagements require a one-time backfill** — A batch job loads each existing engagement (~1 min each), extracts its template ID and version, and writes to the lightweight index. For ~100 engagements, this is ~100 minutes executed once during off-peak hours.

4. **Decline applies to the current latest version** — If a new template version is published after a decline, the engagement returns to PENDING status for re-evaluation.

5. **Decisions are per-engagement** — No bulk accept/decline across multiple engagements. The exercise explicitly excludes bulk actions from scope.

6. **Change summaries are shared across firms** — Since all firms use the same product templates, the same `(templateId, fromVersion, toVersion)` produces identical summaries. Computed once, served to all.

## AI Usage

### Where AI helped

- **Boilerplate generation:** Java record definitions and Angular interface types were generated from the API contract specification, then reviewed for field name consistency.
- **Fixture data construction:** The Angular service's hardcoded fixture data was partially generated from the provided sample JSON files, then validated against the expected API response shapes.
- **Design document structure:** AI helped organize the DESIGN.md sections and ensure the required headings from the exercise skeleton were addressed.
- **Pattern exploration:** Consulted AI on CQRS pattern applicability for the read-optimized index design, and on event-driven architecture patterns for the template publication flow. The CQRS split into `TemplateUpdateProcessor` (write side) and `UpdateStateResolver` (read side) was refined through iterative review.
- **Consistency audit:** Used AI in a reviewer role to systematically audit backend/client contract alignment after the CQRS refactor. The audit compared Java record fields against TypeScript interfaces, verified response shapes between services, and checked that business rules (decline supersession, decision status codes) were consistent across layers. This surfaced 5 issues: 2 medium bugs (publishedAt timestamp sourced from computation time instead of template publish time, declinedVersion not cleared when a newer version supersedes a decline) and 3 low-severity mismatches (APPLY returning wrong status code, incorrect previousVersion in fixture, naming difference). All were fixed except the naming difference, which was an intentional domain vs DTO distinction.

### Where I corrected, rewrote, or ignored AI output

- **DiffOperation helper methods:** AI initially suggested using Jackson `JsonNode` for the diff operation values. I rewrote these to use plain `Map<String, Object>` and `Object` types to avoid framework dependencies as the exercise requires.
- **Domain boundaries:** AI suggested putting the `DiffSummaryTransformer` implementation directly in the domain service package. I restructured to a proper DDD layout with ports (interfaces) in the domain and the implementation as an adapter.
- **Impact assessment logic:** AI's initial impact rules were too simplistic (all adds = HIGH). I refined the heuristics to distinguish required fields (HIGH) from optional additions (MEDIUM).
- **Angular state management:** AI initially suggested a flat service with BehaviorSubjects. I restructured into a full Redux-inspired architecture: API (Observable-based), Store (signals), Effects (side effects with retry), and Facade (public component API). This separation was driven by the need to simulate realistic API failure scenarios and demonstrate proper retry handling with exponential backoff.
- **Fixture data vs API layer separation:** AI initially mixed fixture data directly into the service layer. I separated concerns into a dedicated fixtures file for dummy data and an API service that simulates HTTP behavior (latency, failure rate, `structuredClone` to prevent mutation), so the boundary between real and simulated is a single file swap.
- **Component data flow:** AI initially passed data between components via `@Input` bindings from parent to child. I corrected this to have the detail component read directly from the Facade/Store, eliminating tight coupling and making the modal self-contained with its own loading state.
- **Template reuse in Angular:** AI duplicated the section-rendering HTML block across collapsed and step-by-step views. I extracted it into an `ng-template` with `ngTemplateOutlet` and implicit context, avoiding a new component while eliminating the duplication.
- **Summary computation timing (CQRS violation):** AI placed diff computation and summary generation inside the read-side query path (`resolveUpdateDetails`), meaning every client request would re-invoke the diff tool. I restructured into a proper CQRS split: `TemplateUpdateProcessor` (write side) pre-computes and stores summaries at template publication time, while `UpdateStateResolver` (read side) only reads from the store — no computation at query time.
- **Status materialization:** AI computed engagement status at request time by comparing versions. I moved status resolution to event time — when a template is published, the processor writes the correct status (`PENDING`, `DECLINED`) into the index, so the read side is a direct lookup.
- **Missing listing endpoint and decision handling:** AI omitted the engagement listing endpoint and the apply/decline decision flow entirely. I implemented `listEngagementUpdates(firmId)` as a pure index read and `processDecision` with optimistic concurrency checking on `targetVersion`.
- **Index repository consolidation:** AI generated separate `updateVersion` and `updateState` methods on the index repository. I consolidated into a single `updateState` method that atomically writes all materialized fields (version, status, latestVersion, declinedVersion, summaryAvailable) to prevent partial state updates.

### How I would guide other engineers using AI on this system

- **Use AI for boilerplate, verify against the contract.** The API contract is the source of truth. Any AI-generated model or interface must match it exactly — field names, types, and optionality.
- **Do not trust AI for domain rules.** Impact assessment, update accumulation logic, and state transitions encode business knowledge that AI cannot reliably infer. Write and test these manually.
- **Review all generated code for coherence.** AI may produce Java and TypeScript code that are individually correct but use different field names or types. Cross-check every shared type across the design document, server, and client.
- **Use AI to explore, not to decide.** AI is useful for surfacing design patterns and tradeoffs, but architectural decisions must be justified by the specific constraints of this system (the 1-minute load time, shared templates across firms, non-technical users).
- **Use AI to explain unfamiliar code.** When onboarding or reviewing code written by others, AI is effective at explaining idioms, patterns, and language-specific constructs (e.g., Java's `computeIfAbsent`, TypeScript's `satisfies never` exhaustiveness check, Angular's `ng-template` with implicit context). This accelerates understanding without requiring the original author's time — but always verify the explanation against the actual behavior.
- **Use AI to generate unit test scaffolding.** AI can produce test stubs, mock/stub implementations, and assertion structures quickly — especially for ports with multiple methods that need tracking implementations. However, always review that test scenarios cover the actual business rules and edge cases (e.g., decline supersession, optimistic concurrency rejection), not just happy paths. AI tends to test what the code does rather than what it should do.

### Where AI should not be trusted in this domain

- **Audit and compliance logic.** In the accounting/audit domain, every change to an engagement must be traceable and defensible. AI-generated logic for determining update states or recording decisions must be deterministic and verifiable — never probabilistic.
- **Template diff interpretation.** The transformation from raw diff to human-readable summary must be consistent and reproducible. An LLM could enhance summaries with natural language, but the base transformation must be rule-based to ensure the same diff always produces the same output.
- **Data integrity across systems.** The engagement index, summary store, and decision records form a chain of trust. AI should not be used to generate reconciliation or migration logic without thorough manual review and testing.

## Approximate Time Spent

- Design document and architecture decisions: ~25 minutes
- Java domain implementation (models, ports, services, adapter): ~30 minutes
- Java tests (processor + resolver + transformer): ~15 minutes
- Angular implementation (models, fixtures, API, Store, Effects, Facade, components): ~30 minutes
- Angular tests (6 facade integration scenarios with retry/backoff): ~10 minutes
- Security hardening (multi-tenancy, role-based auth, audit trail, port isolation): ~15 minutes
- Submission notes, documentation, and review: ~15 minutes
- **Total: ~2 hours**

## What I Would Do Next

1. **Persistence adapters** — Implement repository adapters for `EngagementIndexRepository`, `UpdateSummaryRepository`, and `TemplateVersionProvider` against a real database.
2. **Real HTTP integration** — Replace the Observable-based API simulation with actual `HttpClient` calls. The existing retry/backoff infrastructure and Store architecture carry over unchanged.
3. **Accessibility refinements** — Expand ARIA support beyond the current modal (which has `aria-modal`, `aria-label`, and Escape/backdrop dismiss). Add focus trapping inside the modal and screen reader announcements for state transitions.
4. **Pagination and performance** — For firms with hundreds of engagements, add server-side pagination and client-side virtual scrolling.
5. **E2E tests** — Integration tests covering the full flow from template publication event through to the user seeing the update and making a decision.
6. **Observability implementation** — Wire up the metrics, alerts, and logging described in the design document.
7. **LLM-enhanced summaries** — Implement an alternative `DiffSummaryTransformer` adapter that uses an LLM to generate more natural, context-aware change descriptions while keeping the rule-based version as the deterministic fallback.
