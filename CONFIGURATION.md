# Configuration Reference

`mode` accepts `ENFORCE` or `MONITOR`.

`actions.default` is the fallback action. `actions.by-type` overrides it for `VPN`, `PROXY`, `TOR`, `DATACENTER`, `RESIDENTIAL`, or `MOBILE`. `actions.by-risk-score` selects the action belonging to the highest threshold not greater than the returned score. `COMMAND` uses `actions.commands`; available values are `{player}`, `{ip}`, `{risk_score}`, and `{detection}`.

`history.enabled`, `history.retention-days`, and `history.max-rows` control local SQLite history. Command query limits are constrained to 1–50.

`alerts.discord.ip-display` accepts `FULL`, `MASKED`, or `HIDDEN`. Custom webhook hosts require HTTPS and explicit `allow-custom-hosts: true`.

`network.mode` accepts `STANDALONE`, `PROXY`, or `BACKEND`. Non-standalone modes require the same secret of at least 32 UTF-8 bytes on every participating server. NukkitX has no `network` section.

## messages.yml

`kick-message` and `alert-message` accept a list of lines or a single string. Each line is rendered on its own.

Formatting on every platform: legacy `&a`, hex `&#RRGGBB`, `{rgb:R,G,B}`, `{gradient:#RRGGBB:#RRGGBB}text{/gradient}`, MiniMessage with the `mm:` prefix. `{prefix}` inside an `mm:` line is converted to MiniMessage automatically. NukkitX maps hex, RGB and gradients to the closest of the 16 Bedrock colours.

| Placeholder | Available in |
|-------------|--------------|
| `{prefix}` | every message |
| `{ip} {risk_score} {detection_type} {isp} {country} {country_code} {city} {region} {timezone} {asn} {org}` | `kick-message`, `alert-message`, `check-result` |
| `{vpn_status} {proxy_status} {tor_status} {datacenter_status} {residential_status} {mobile_status}` | same as above, rendered with `check-safe` / `check-blocked` |
| `{player} {action} {reason}` | `alert-message` |
| `{ip}` | `check-pending`, `whitelist-list-ip-entry` |
| `{error}` | `check-failed`, `player-resolve-failed` |
| `{target}` | `whitelist-add`, `whitelist-remove`, `whitelist-already`, `whitelist-not-found` |
| `{count}` | `whitelist-list-ips-header`, `whitelist-list-players-header` |
| `{name} {uuid}` | `whitelist-list-player-entry` |
| `{size}` | `cache-cleared` |
| `{status} {latency}` | `status-online` |

When the API is unreachable and `detection.block-on-api-failure` is enabled, `kick-message` is shown with `{risk_score}` = `N/A`, `{detection_type}` = `Unavailable` and the remaining check placeholders as `Unknown`.
