# 0002: Use the official VBB GTFS-RT feed

Status: Accepted · 2026-10-30

## Context
VBB runs an open GTFS-RT feed for the whole network: CC BY 4.0, no key, 7 requests per minute. It has per-stop delays and skipped stops. On 2026-10-30 production was empty and hours old while staging worked. Details in [data-sources.md](../data-sources.md).

## Decision
- This feed is the only realtime source in Phase 1.
- I collect from staging, and I also poll production once a minute to see when it recovers. Every stored record has a `source` field.
- I stay well under the limit: at most 4 requests per minute to staging.
- Every request sends a User-Agent with a contact email, gzip and the last ETag. The email lives only in `.env`.
- I ingest the whole VBB network and filter later in queries.

## Alternatives
- transport.rest: too few requests to cover the network, and no published terms.
- VBB's REST API: needs an access request and works per stop or trip. Maybe for Phase 3.

## Consequences
Staging is meant "for tests", so I'm asking the maintainer whether collecting from it is OK. If not, switching to production is a config change. Feed outages show up as gaps, which get recorded (0005) and alerted on (0008).
