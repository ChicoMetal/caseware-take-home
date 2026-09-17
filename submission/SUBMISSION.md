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
- **Pattern exploration:** Consulted AI on CQRS pattern applicability for the read-optimized index design, and on event-driven architecture patterns for the template publication flow.

### Where I corrected, rewrote, or ignored AI output

- **DiffOperation helper methods:** AI initially suggested using Jackson `JsonNode` for the diff operation values. I rewrote these to use plain `Map<String, Object>` and `Object` types to avoid framework dependencies as the exercise requires.
- **Domain boundaries:** AI suggested putting the `DiffSummaryTransformer` implementation directly in the domain service package. I restructured to a proper DDD layout with ports (interfaces) in the domain and the implementation as an adapter.
- **Impact assessment logic:** AI's initial impact rules were too simplistic (all adds = HIGH). I refined the heuristics to distinguish required fields (HIGH) from optional additions (MEDIUM).
- **Angular state management:** AI initially suggested using BehaviorSubjects (RxJS). I rewrote to use Angular Signals for a more modern approach consistent with Angular 18+.

### How I would guide other engineers using AI on this system

- **Use AI for boilerplate, verify against the contract.** The API contract is the source of truth. Any AI-generated model or interface must match it exactly — field names, types, and optionality.
- **Do not trust AI for domain rules.** Impact assessment, update accumulation logic, and state transitions encode business knowledge that AI cannot reliably infer. Write and test these manually.
- **Review all generated code for coherence.** AI may produce Java and TypeScript code that are individually correct but use different field names or types. Cross-check every shared type across the design document, server, and client.
- **Use AI to explore, not to decide.** AI is useful for surfacing design patterns and tradeoffs, but architectural decisions must be justified by the specific constraints of this system (the 1-minute load time, shared templates across firms, non-technical users).

### Where AI should not be trusted in this domain

- **Audit and compliance logic.** In the accounting/audit domain, every change to an engagement must be traceable and defensible. AI-generated logic for determining update states or recording decisions must be deterministic and verifiable — never probabilistic.
- **Template diff interpretation.** The transformation from raw diff to human-readable summary must be consistent and reproducible. An LLM could enhance summaries with natural language, but the base transformation must be rule-based to ensure the same diff always produces the same output.
- **Data integrity across systems.** The engagement index, summary store, and decision records form a chain of trust. AI should not be used to generate reconciliation or migration logic without thorough manual review and testing.

## Approximate Time Spent

- Design document and architecture decisions: ~50 minutes
- Java domain implementation (models, ports, service, adapter): ~60 minutes
- Java tests: ~20 minutes
- Angular implementation (models, service, components): ~40 minutes
- Angular test: ~10 minutes
- Submission notes and review: ~20 minutes
- **Total: ~3.5 hours** (slightly over the 3-hour target due to investing extra time in the DDD port/adapter structure)

## What I Would Do Next

1. **Complete decline flow** — Add `DECLINED` status, `declinedVersion` tracking, and the re-evaluation logic when a new version is published after a decline.
2. **Reconciliation job** — Implement the periodic reconciliation that detects index staleness and fills gaps from missed events.
3. **Real HTTP integration** — Replace the Angular fixture data with actual HTTP calls, including loading states, error handling, and retry logic.
4. **Accessibility** — Add ARIA labels, keyboard navigation, and screen reader support to the Angular components.
5. **Pagination and performance** — For firms with hundreds of engagements, add server-side pagination and client-side virtual scrolling.
6. **E2E tests** — Integration tests covering the full flow from template publication event through to the user seeing the update and making a decision.
7. **Observability implementation** — Wire up the metrics, alerts, and logging described in the design document.
8. **LLM-enhanced summaries** — Implement an alternative `DiffSummaryTransformer` adapter that uses an LLM to generate more natural, context-aware change descriptions while keeping the rule-based version as the deterministic fallback.
