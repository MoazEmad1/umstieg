# 0005: Store changes, not every poll

Status: Accepted · 2026-10-30

## Context
Saving every stop update from every poll would be around 390 million rows a day, and most would just repeat the last value. For Phase 2 I need two things: what was known about a stop at a given time, and what finally happened. Two fields I'd like in keys, `start_date` and `stop_sequence`, are optional in GTFS-RT.

## Decision
Three tables:
- `rt_snapshot` has one row per poll, including empty or stale ones, so gaps are visible.
- `rt_stop_time_latest` has the current state per stop event.
- `rt_stop_time_change` is append-only and partitioned by day. It gets a row only when a delay, time, stop or status changes.

One `INSERT ... ON CONFLICT DO UPDATE ... WHERE ... IS DISTINCT FROM ... RETURNING` finds the changes, and they're written in the same transaction.

The key is `(source, trip_id, service_date, stop_key)`:
- `service_date` is `start_date` if present. Otherwise it's the Berlin local date of the trip's first time in the update, flagged as inferred.
- `stop_key` is `stop_sequence` if present. Otherwise it's `stop_id`, flagged. A loop trip visiting the same stop twice would collide, so I'll count those cases.

Each change row also stores the snapshot ID and the static GTFS version that was current.

## Alternatives
- Store everything: huge, and the raw files (0003) already have it.
- Only the latest state: loses how predictions changed over time.
- Change detection in Kafka Streams: fine, but one more moving part. Better saved for Phase 3.

## Consequences
Replays and redeliveries don't create new rows, and tests will check that. The key fallbacks are provisional until I measure how often the fields are missing. Before I design Phase 2 labels I need to know whether the feed drops stops a trip has already passed.
