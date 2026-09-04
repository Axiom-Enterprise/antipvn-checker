# AxiomAntiVPN

Anti-VPN and proxy protection for Paper/Bukkit, Folia, BungeeCord, Velocity, NukkitX (Bedrock) and Minestom. Requires Java 21 and an API key from `https://antivpn.mathsanalysis.com`.

## Features

- VPN, proxy, Tor, datacenter, residential, mobile, country, and risk-score detection
- Per-type and per-risk actions: `ALLOW`, `ALERT`, `KICK`, `COMMAND`
- `MONITOR` mode for safe evaluation without enforcement
- SQLite history and operational statistics
- Privacy-aware Discord webhook alerts
- Caffeine-backed result cache and persistent whitelist
- Optional PlaceholderAPI and LuckPerms support
- Legacy `&`, hex, RGB, gradient and MiniMessage text formatting
- Signed proxy-first decisions for BungeeCord/Velocity networks

## Platforms

| Jar | Target | Notes |
|-----|--------|-------|
| `AxiomAntiVPN-Bukkit` | Paper, Spigot, Bukkit | PlaceholderAPI expansion `axiomantivpn`, BACKEND plugin channel |
| `AxiomAntiVPN-Folia` | Folia | Same code as Bukkit, `folia-supported`, region/entity schedulers |
| `AxiomAntiVPN-BungeeCord` | BungeeCord, Waterfall | PROXY mode forwards signed decisions |
| `AxiomAntiVPN-Velocity` | Velocity 3.x | PROXY mode forwards signed decisions |
| `AxiomAntiVPN-NukkitX` | NukkitX (Bedrock) | Hex/gradients downsampled to the 16 Bedrock colours |
| `AxiomAntiVPN-Minestom` | Minestom (library) | Call `AxiomAntiVpnMinestom.create(dataDir, permissionChecker)` from your bootstrap |

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

## Messages

`messages.yml` is identical on every platform. `kick-message` and `alert-message` are lists, one entry per line; a plain string still works.

```yaml
prefix: "&#7C3AED&lAxiom &8» &7"
kick-message:
  - "&c&lVPN Detected"
  - ""
  - "mm:<gradient:#7C3AED:#EC4899>Disable your VPN and reconnect</gradient>"
  - "&8Risk Score: {risk_score} | {detection_type}"
alert-message:
  - "{prefix}&e{player} &7was blocked &8(&c{detection_type}&8) &7IP: &f{ip} &7Score: &c{risk_score} &7Action: &f{action}"
```

Formatting: legacy `&a`, hex `&#RRGGBB`, `{rgb:R,G,B}`, `{gradient:#RRGGBB:#RRGGBB}text{/gradient}`, and MiniMessage when a value starts with `mm:`. Every line is rendered independently, so lists may mix syntaxes.

Placeholders: `{prefix}` everywhere; `{ip} {risk_score} {detection_type} {isp} {country} {country_code} {city} {region} {timezone} {asn} {org} {vpn_status} {proxy_status} {tor_status} {datacenter_status} {residential_status} {mobile_status}` wherever a check result is shown; `{player} {action} {reason}` in `alert-message`. The full list is documented at the top of `messages.yml`.

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

LuckPerms works through standard permission nodes. Minestom has no permission system: pass a `PermissionChecker` to `AxiomAntiVpnMinestom.create` (default is console only).

## Proxy-first setup

1. Generate a random secret containing at least 32 bytes.
2. Set identical `network.channel-secret` values on proxy and backends.
3. Set proxy `network.mode` to `PROXY`.
4. Set backend (Bukkit, Folia or Minestom) `network.mode` to `BACKEND`.
5. Restart proxy, then backends.

Proxy performs login enforcement once. Signed HMAC-SHA256 decisions are forwarded through `axiomantivpn:decision`; backend verifies UUID, IP, signature, and 30-second freshness before storing history. Invalid messages are discarded.

## Build

```bash
./gradlew clean build
```

Artifacts are produced under each platform module's `build/libs` directory.
