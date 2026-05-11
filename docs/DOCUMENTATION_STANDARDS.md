# Documentation Governance & Standards

## 1. Documentation as Code
- All documentation is stored in the repository as Markdown (`.md`) in the `docs/` directory.
- Changes to documentation MUST go through the same PR/Code Review process as code.
- **DoD (Definition of Done):** A feature is not complete until relevant documentation (API guides, glossary, etc.) is updated.

## 2. Layered Structure
We follow a tiered approach to documentation:
- **Onboarding (docs/)**: For quick start (CONTRIBUTING, GLOSSARY, ONBOARDING, ADR).
- **Development (docs/engine/, docs/testing/)**: Technical details for backend developers.
- **API/Frontend (docs/api/)**: Contracts for the frontend team.

## 3. Architecture Decision Records (ADR)
- Any significant architectural change (library choice, data storage strategy, pattern change) MUST be accompanied by an ADR in `docs/adr/NNN-name.md`.
- Explain "why" it was done, not just "how".

## 4. Glossary & Naming
- All terms in code (classes, variables) MUST match `docs/GLOSSARY.md`.
- New domain terms must be added to the glossary first.

## 5. Visualization
- Use **Mermaid.js** for diagrams to keep them versionable and searchable.
- Prefer text-based diagrams over static images.

## 6. Language Standards
- **Javadoc & Public API:** English only (standard for libraries).
- **Internal Team Documentation (docs/*.md):** Russian (for better internal communication).

## 7. Public API Javadoc Requirements
- Every class and method marked with `@EngineAPI` MUST have a comprehensive Javadoc.
- Documentation must include usage examples (`{@code ...}`), parameter descriptions, and return value details.
- This is mandatory for Maven Central publication and binary compatibility maintenance.

## 8. Quality Gates (JaCoCo Thresholds)
To maintain project health, the following minimum line coverage thresholds are enforced:
- **island-engine (Public API):** 75%
- **island-nature (Plugin):** 65%
- **island-simcity (Plugin):** 60%
- These thresholds are monitored by the CI pipeline.

## 9. Changelog Standards
- We follow the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.
- All entries must be grouped under one of the following headers: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`.
- Entries must be human-readable and explain "why" the change was made, not just "what" changed.

## 10. Maintenance
- If a PR changes behavior, configuration, or public API, update the documentation in the same PR.
