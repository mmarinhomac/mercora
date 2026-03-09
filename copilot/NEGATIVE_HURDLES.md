## Constraints

You MUST NOT modify files that are not explicitly mentioned in the plan.

You may ONLY create or update the files listed in the step-by-step instructions.

---

## DO NOT

- Hardcode configuration values (ports, addresses, credentials, bootstrap servers, etc.) directly
  in the code. All configuration must be read from Spring's environment abstraction (e.g., `@Value`, `Environment`,
  or auto-configured properties that Spring populates from environment variables)
- Add defensive handling, cleanup logic, retry mechanisms, or exception recovery beyond what is explicitly
  requested. Robustness improvements are done in separate future tasks
- Add extra fields, methods, classes, or abstractions beyond the minimum required to satisfy the described behavior
- Fix, refactor, or improve production code that is not part of the current task, even if it appears incomplete
  or broken
