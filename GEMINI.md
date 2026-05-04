# Project Guidelines: Island Ecosystem Simulator

## Java Coding Standards
- **Imports**: Never use Fully Qualified Names (FQNs) in the code body. Always use explicit imports at the top of the file.
- **Lombok**: Use Lombok annotations (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`) to minimize boilerplate code.
- **Compactness**: Maintain high signal-to-noise ratio. Avoid redundant comments or verbose Javadocs unless they provide significant value.
- **Concurrency**: Use `ReentrantLock` or `Concurrent` collections for thread-safe operations in the simulation engine.
- **Type Safety**: Prefer generics and avoid unsafe casting or `instanceof` checks outside of domain entry points.
