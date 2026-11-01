---
license: cc-by-4.0
pretty_name: VBB GTFS-Realtime raw snapshots
tags:
  - gtfs-realtime
  - public-transport
  - berlin
---

# VBB GTFS-Realtime raw snapshots

Raw snapshots of the official GTFS-Realtime feed of VBB (Berlin and Brandenburg). They are collected by [umstieg](https://github.com/MoazEmad1/umstieg): production every 60 seconds, staging every 5 minutes.

Data: VBB Verkehrsverbund Berlin-Brandenburg GmbH, licensed [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Feed: https://production.gtfsrt.vbb.de/ and https://staging.gtfsrt.vbb.de/. This collection is not affiliated with VBB.

## Layout

One archive per source and UTC day, e.g. `production/2026/production-2026-11-01.tar.zst`, next to a `.manifest.json` with the name, size and sha256 of every file in it.

Inside the archive, each fetch is `{source}/YYYY/MM/DD/HHMMSS.pb` (the feed body exactly as received, a GTFS-Realtime `FeedMessage`) plus `HHMMSS.json` (fetch time, HTTP status, ETag, size, feed timestamp, entity count, error). A fetch with no new data or an error only has the `.json`. Gaps in the files are gaps in collection, not "no delays".

## Unpacking

```bash
zstd -d production-2026-11-01.tar.zst
tar xf production-2026-11-01.tar
```
