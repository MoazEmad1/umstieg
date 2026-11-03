# Data sources

Notes from 2026-10-30. Numbers marked "measured" come from my own requests that day. Everything else is from the providers' pages, linked at the bottom.

All data used here is from VBB Verkehrsverbund Berlin-Brandenburg GmbH, licensed CC BY 4.0.

## Static timetable (GTFS)

VBB publishes the full timetable as a GTFS zip at `https://unternehmen.vbb.de/gtfs`. The old link on vbb.de redirects to their datasets page.

- The zip was 65,302,459 bytes (measured). I haven't unpacked it yet. I expect a few hundred MB, mostly `stop_times.txt`.
- It's updated "2x wöchentlich", with no fixed days. The page doesn't give a validity window, so I'll read it from `feed_info.txt` / `calendar.txt`.
- The license is CC BY 4.0. VBB gives no warranty and says the data may contain errors or be incomplete.
- There's also a `gtfs-flex` variant with on-demand services. I don't use it.
- VBB keeps yearly archives for 2010–2024. https://vbb-gtfs.jannisr.de/ (community, not official) keeps daily snapshots. That could help back-fill old versions later, but it asks people not to cause heavy traffic.

Because the timetable changes twice a week and realtime trip IDs refer to a specific version, I archive every zip I download (ADR 0006).

## Realtime feed (GTFS-Realtime)

VBB runs an official GTFS-RT feed and links it from its datasets page:

- production: `https://production.gtfsrt.vbb.de/data`
- staging ("for tests"): `https://staging.gtfsrt.vbb.de/data`

What the feed's page says:
- CC BY 4.0, no API key.
- Rate limit of 7 requests per minute from 2026-11-02, shared by everyone. Their note says that's still enough to fetch every 10 seconds.
- Send a User-Agent with contact info. They block generic ones.
- Ask for `gzip` or `zstd` compression.
- An ETag is sent, and announcements go out on an Atom feed (`/news.atom.xml`).

The feed is built by VBB's open-source matcher ([OpenDataVBB/gtfs-rt-feed](https://github.com/OpenDataVBB/gtfs-rt-feed)). It turns VDV-454 realtime data into GTFS-RT TripUpdates.

## What a snapshot looks like

I decoded one staging snapshot at 18:07 UTC on 2026-10-30:

- 2,693 TripUpdates and no other entity types. 67,961 stop time updates in total.
- About 3.4 MB raw, 744 KB gzip, 626 KB zstd. The download took 3.5–4.5 s.
- Stop IDs are per platform, e.g. `de:11000:900051203::1`.
- Each stop update has a delay and an absolute time. Many delays are non-zero.
- Stop updates by type: 382 were SKIPPED, 6 NO_DATA, the rest SCHEDULED.

The docs don't mention platform changes. They might show up as a different `stop_id`, but I haven't checked that yet.

## Reliability

This is the biggest risk. On 2026-10-30 at 18:07 UTC, production returned HTTP 200 with a 35-byte body: a header with no trips, 5.5 hours old. That was after a maintenance window announced for 12:30–13:30 CET the same day. Staging was fine.

On 2026-10-31 at 10:58 UTC production was back: about 6,900 trips per snapshot, 1.43 MB gzip, header 1–2 s old. Staging had about 2,200 trips at the same time. I don't know yet whether the June problem is fixed for good.

The Atom feed lists earlier problems:
- 2025-11-06: DB-operated trips possibly missing during DB maintenance.
- 2026-02-05: maintenance on the source.
- 2026-02-17/18: about a day offline because of a DNS mistake.
- 2026-03-10: source offline, no ETA.
- 2026-06-04: "lacking some data", no ETA, and no fix announced since.

On 2026-11-02 production stopped updating seven times, all between 19:57 and 02:00 UTC, for 14 to 30 minutes each. The server first answers 304, then serves an empty 35-byte feed with a header from 2026-10-30 19:04:09 UTC until normal data returns. Details are in [experiment 0001](experiments/0001-first-full-day.md).

So the collector has to notice empty or stale snapshots and record them (ADR 0005), and alert me (ADR 0008). Gaps in the data stay gaps; they must never be read as "no delay".

## Alternatives I'm not using yet

**transport.rest** (`v6.vbb.transport.rest`) is a community API that wraps the VBB app's backend.
- 100 requests/minute (burst 200). No terms of use or SLA are published.
- Departures include `when`, `plannedWhen`, `delay`, `platform`, `plannedPlatform`, `tripId` and `remarks`. Journeys give per-leg delays and platforms.
- It can be self-hosted (`derhuerst/vbb-rest:6`), but it still calls the same upstream.
- At that rate limit it can only watch a handful of stops, and it's unclear whether using the upstream this way is allowed. Maybe useful in Phase 3 for alternative routes.

**VBB's official REST API** (departureBoard, journeyDetail, trip, …): you request access at api@vbb.de, get a test server, then production after agreeing to their usage rules. I haven't read those rules yet. It's a candidate for Phase 3.

## Licence

Both sources I use in Phase 1 are CC BY 4.0. A public repo, a blog post and a dashboard are fine with attribution, and so are small trimmed feed samples as test fixtures. transport.rest stays out until its upstream terms are clear.

## Open questions

1. Is production coming back, and is long-term collection from staging OK? I'm asking the maintainer.
2. How are platform changes represented, if at all?
3. Does the feed keep updates for stops a trip has already passed, or drop them? Measured on 2026-11-02: it keeps them, but 11.6% of them still change more than 10 minutes after departure. Next I need to know by how much.
4. How often are `start_date` and `stop_sequence` missing? Measured on 2026-11-02: `start_date` never, `stop_sequence` in 1.25% of stop updates.
5. Which static version are realtime trip IDs matched against at a given moment? I assume the current one.
6. How big is the static GTFS unpacked, and how many rows does each table have?

## Sources

- VBB datasets: https://unternehmen.vbb.de/digitale-services/datensaetze/
- VBB API access: https://unternehmen.vbb.de/digitale-services/api/
- GTFS-RT feed: https://production.gtfsrt.vbb.de/ and https://production.gtfsrt.vbb.de/news.atom.xml
- Feed code: https://github.com/OpenDataVBB/gtfs-rt-feed
- transport.rest: https://v6.vbb.transport.rest/ and https://github.com/derhuerst/vbb-rest
- GTFS archive: https://vbb-gtfs.jannisr.de/
