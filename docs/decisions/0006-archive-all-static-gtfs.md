# 0006: Keep every static GTFS version

Status: Accepted · 2026-10-30

## Context
VBB publishes a new timetable zip about twice a week (65 MB on 2026-10-30). Realtime trip IDs refer to a specific version, and trips or stops can change between versions. `stop_times` is by far the largest table.

## Decision
- The loader checks the zip once a day. When the sha256 is new, it saves the zip as `data/gtfs/{date}_{hash}.zip`, adds a `gtfs_feed` row and loads the tables tagged with that feed ID.
- Zips are never deleted and are synced to object storage.
- In Postgres I keep agency, route, stop, trip and calendar data for every version. Only old `stop_times` may be dropped, since they can be reloaded from the zip.

## Alternatives
- Keep only the current version: old trip IDs lose their meaning.
- Keep all `stop_times` too: millions of rows twice a week for little gain.

## Consequences
About 0.5 GB of zips per month. Every realtime row can be joined to the timetable that was valid when it was seen.
