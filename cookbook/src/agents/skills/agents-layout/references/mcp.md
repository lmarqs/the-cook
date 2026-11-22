# Share an MCP server

Edit `.agents/mcp/servers.yaml`, then re-cook `#connect` with every harness your team uses. Each top-level key names
one server. `#seed` creates the file with `{}`. Projects upgrading an existing layout can create it when they add
their first shared server.

```yaml
reports:
  url: https://reports.example.org/mcp
  oauth:
    clientId: reports-public-client
    callbackPort: 3118
    scopes: [reports.read]
issues:
  url: https://issues.example.org/mcp
  bearerTokenEnv: ISSUES_MCP_TOKEN
search:
  command: search-mcp
  args: ["--index", "project files"]
  envFrom: [SEARCH_MCP_KEY]
```

| Field | Meaning |
| --- | --- |
| `type` | Optional. `http` takes a `url`. `stdio` takes a `command`. |
| `url`, `headers` | HTTP endpoint and optional map of literal request header values. |
| `bearerTokenEnv` | Environment variable holding the HTTP bearer token. Use a service-specific name. |
| `oauth.clientId`, `oauth.callbackPort` | Public OAuth client ID and fixed callback port for this server. Use the port registered for the client. |
| `oauth.scopes` | List of requested scopes. Cook joins them with spaces for Claude and writes a list for Codex. |
| `command`, `args` | Stdio executable and optional ordered list of argument strings. A single argument can contain spaces. |
| `env`, `envFrom` | Map of literal stdio environment values and list of variable names to forward from the developer's environment. |

For credentials, commit only the variable names. Each developer sets the values in the environment that launches
the client. For OAuth, the developer completes sign-in through the client. The OAuth fields were checked against
Claude Code 2.1.330 and Codex 0.171.0, including the per-server `oauth.callback_port` in Codex.
[Claude Code MCP configuration](https://code.claude.com/docs/en/mcp) ·
[Codex 0.171.0 schema](https://github.com/openai/codex/blob/rust-v0.171.0/codex-rs/core/config.schema.json)

## Migrate and verify

1. Copy each Claude entry you want to share from the root `.mcp.json`'s `mcpServers` object into the YAML, using
   the server name as a top-level key. Convert a space-separated OAuth scopes string to a YAML list.
2. If Codex already has a `[mcp_servers.<name>]` table for that same server, rewrite it as one inline value
   under `[mcp_servers]`, such as `reports = { url = "https://reports.example.org/mcp" }`. Include its existing
   fields so the dry run shows any differences. Cook owns the named value as a whole. Other servers can keep
   their table headers.
3. Run `cook --dry-run io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex`
   with the version in `cook.lock` and the other arguments your team uses. Review the proposed server entries.
4. Apply the same cook without `--dry-run`. Commit the YAML, root `.mcp.json`, Codex adapter and updated
   `cook.lock` together. In a trusted project, open `/mcp` in each client to see the servers and their status.

Cook tracks each rendered server under its own name. To change or remove one, edit the YAML and re-cook.
Servers independently configured under other names stay in the native files.
