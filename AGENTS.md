# Kestra Solace Plugin

## What

- Provides plugin components under `io.kestra.plugin.solace`.
- Includes classes such as `Consume`, `Produce`, `Trigger`, `Serdes`.

## Why

- What user problem does this solve? Teams need to publish, consume, and trigger workflows with Solace messaging from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Solace steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Solace.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `solace`

### Key Plugin Classes

- `io.kestra.plugin.solace.Consume`
- `io.kestra.plugin.solace.Produce`
- `io.kestra.plugin.solace.Trigger`

### Project Structure

```
plugin-solace/
├── src/main/java/io/kestra/plugin/solace/service/
├── src/test/java/io/kestra/plugin/solace/service/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
