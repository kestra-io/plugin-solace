# Kestra Solace Plugin

## What

- Provides plugin components under `io.kestra.plugin.solace`.
- Includes classes such as `Consume`, `Produce`, `Trigger`, `Serdes`.

## Why

- This plugin integrates Kestra with Solace.
- It provides tasks that publish, consume, and trigger workflows with Solace messaging.

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
