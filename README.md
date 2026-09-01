# AxiomAntiVPN

Anti-VPN and proxy protection for Paper/Bukkit, BungeeCord, and Velocity networks. Requires Java 21 and an API key from `https://antivpn.mathsanalysis.com`.

## Features

- VPN, proxy, Tor, datacenter, country, and risk-score detection
- Per-type and per-risk actions: `ALLOW`, `ALERT`, `KICK`, `COMMAND`
- `MONITOR` mode for safe evaluation without enforcement
- SQLite history and operational statistics
- Privacy-aware Discord webhook alerts
- Caffeine-backed result cache and persistent whitelist
- Optional PlaceholderAPI, LuckPerms, and MiniMessage support
- Signed proxy-first decisions for BungeeCord/Velocity networks

## Commands

- `/vpn check <ip>`
- `/vpn stats`
- `/vpn history <player> [limit]`
- `/vpn whitelist add <ip|player>`
- `/vpn whitelist remove <ip|player>`
- `/vpn whitelist list`
- `/vpn status`
- `/vpn cache clear`
- `/vpn reload`

Permission: `antivpn.admin`. Bypass: `antivpn.bypass`. Staff alerts: `antivpn.alerts`.

## Actions and monitor mode

```yaml
mode: "ENFORCE" # ENFORCE or MONITOR
actions:
  default: "KICK"
  by-type: {VPN: "KICK", PROXY: "KICK", TOR: "KICK", DATACENTER: "ALERT"}
  by-risk-score: {75: "ALERT", 90: "KICK"}
  commands:
    - "tempban {player} 1d VPN detected: {detection} ({risk_score})"
```

`MONITOR` records detections and sends alerts but never kicks or runs configured commands.

## Discord

```yaml
alerts:
  discord:
    enabled: true
    webhook-url: "https://discord.com/api/webhooks/id/token"
    ip-display: "MASKED" # FULL, MASKED, HIDDEN
    allow-custom-hosts: false
```

Webhook URLs are validated, never logged, and use asynchronous delivery.

## Optional integrations

PlaceholderAPI exposes `%axiomantivpn_risk_score%`, `%axiomantivpn_country%`, `%axiomantivpn_country_code%`, `%axiomantivpn_isp%`, `%axiomantivpn_detection%`, `%axiomantivpn_action%`, and `%axiomantivpn_cache_hit%` for a player's latest cached result.

LuckPerms works through standard permission nodes. MiniMessage is enabled per message by prefixing its value with `mm:`; legacy `&` and existing gradient syntax remain supported.

## Proxy-first setup

1. Generate a random secret containing at least 32 bytes.
2. Set identical `network.channel-secret` values on proxy and Bukkit backends.
3. Set proxy `network.mode` to `PROXY`.
4. Set backend Bukkit `network.mode` to `BACKEND`.
5. Restart proxy, then backends.

Proxy performs login enforcement once. Signed HMAC-SHA256 decisions are forwarded through `axiomantivpn:decision`; backend verifies UUID, IP, signature, and 30-second freshness before storing history. Invalid messages are discarded.

## Build

```bash
./gradlew clean build
```

Artifacts are produced under each platform module's `build/libs` directory.
