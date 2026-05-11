# Project Guidelines: Island Ecosystem Simulator

## Engine Library Architecture
- **API Isolation**: All implementation details MUST be in `com.island.engine.internal` or other non-exported packages.
- **Public API**: Use `@EngineAPI` for stable interfaces and `@InternalEngine` for public classes that are nonetheless implementation details.
- **Factories**: Always prefer static factory methods in interfaces (e.g., `EventBus.create()`) over direct instantiation of `Default*` classes.
- **JPMS**: Strictly follow `module-info.java` exports. Never export `.internal` packages.
- **Dependencies**: The engine module should have minimal external dependencies to facilitate its use as a JAR library.

## Documentation Governance
- **Docs as Code**: All documentation resides in `docs/` as Markdown. Documentation updates are part of the PR process.
- **Language**: Javadoc is English-only. Internal documentation (`docs/*.md`) is in Russian.
- **DoD**: Features are only complete when documentation is updated.
- **ADR**: Major architectural changes must be recorded in `docs/adr/`.
- **Glossary**: Code naming must strictly follow `docs/GLOSSARY.md`.
- **Javadoc**: All `@EngineAPI` components must be fully documented.
- **Coverage**: Engine: 75%+, Nature: 65%+, SimCity: 60%+.
- **Changelog**: Strictly follow "Keep a Changelog" format.

## Java Coding Standards
- **Imports**: Never use Fully Qualified Names (FQNs) in the code body. Always use explicit imports at the top of the file.
- **Lombok**: Use Lombok annotations (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`) to minimize boilerplate code.
- **Compactness**: Maintain high signal-to-noise ratio. Avoid redundant comments or verbose Javadocs unless they provide significant value.
- **Concurrency**: Use `ReentrantLock` or `Concurrent` collections for thread-safe operations in the simulation engine.
- **Type Safety**: Prefer generics and avoid unsafe casting or `instanceof` checks outside of domain entry points.
