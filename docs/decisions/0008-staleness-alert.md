# 0008: Alert when the feed goes stale

Status: Proposed · 2026-10-30

## Context
The feed has had outages lasting hours or days. On 2026-10-30, production served an empty, 5.5-hour-old snapshot with HTTP 200, so no error showed. If the collector misses that, I lose history without knowing.

## Decision
- A snapshot counts as fresh if the request succeeded, it has at least one entity, and its header timestamp is under 5 minutes old.
- If a source has no fresh snapshot for 15 minutes, I get one message, and another when it recovers. Nothing in between.
- The check runs inside the collector, so it works on the VPS.
- Channel: probably a Telegram bot (one HTTPS call, no mail server). The token and chat ID go in `.env`. Not decided yet.

## Alternatives
- Email over SMTP: needs a mail account, and mail from a VPS often lands in spam.
- Grafana alerts: Grafana doesn't run on the VPS in Phase 1.
- An uptime service: only sees HTTP status, not stale content.

## Consequences
While production is broken it would alert constantly, so its alert can be muted in config.
