# 0008: Alert when the feed goes stale

Status: Accepted · 2026-10-31

## Context
The feed has had outages lasting hours or days. On 2026-10-30, production served an empty, 5.5-hour-old snapshot with HTTP 200, so no error showed. If the collector misses that, I lose history without knowing.

## Decision
- A snapshot counts as fresh if the request succeeded, it has at least one entity, and its header timestamp is under 5 minutes old.
- If a source has no fresh snapshot for 15 minutes, I get one message, and another when it recovers. Nothing in between.
- The check runs inside the collector, so it works on the VPS.
- Channel: a Telegram bot. It's one HTTPS call to the Bot API and needs no mail server. The token and chat ID live only in `.env`. Without them the collector still runs and only logs.

## Alternatives
- Email over SMTP: needs a mail account, and mail from a VPS often lands in spam.
- Grafana alerts: Grafana doesn't run on the VPS in Phase 1.
- An uptime service: only sees HTTP status, not stale content.

## Consequences
A source that stays broken for days sends one message, not one per poll. If sending fails, the collector logs it and keeps polling, so a Telegram outage can hide a feed outage. If the VPS itself goes down, nothing alerts at all. I'd only notice from missing files.
