# Configuration Reference

`mode` accepts `ENFORCE` or `MONITOR`.

`actions.default` is the fallback action. `actions.by-type` overrides it for `VPN`, `PROXY`, `TOR`, `DATACENTER`, `RESIDENTIAL`, or `MOBILE`. `actions.by-risk-score` selects the action belonging to the highest threshold not greater than the returned score. `COMMAND` uses `actions.commands`; available values are `{player}`, `{ip}`, `{risk_score}`, and `{detection}`.

`history.enabled`, `history.retention-days`, and `history.max-rows` control local SQLite history. Command query limits are constrained to 1–50.

`alerts.discord.ip-display` accepts `FULL`, `MASKED`, or `HIDDEN`. Custom webhook hosts require HTTPS and explicit `allow-custom-hosts: true`.

`network.mode` accepts `STANDALONE`, `PROXY`, or `BACKEND`. Non-standalone modes require the same secret of at least 32 UTF-8 bytes on every participating server.
