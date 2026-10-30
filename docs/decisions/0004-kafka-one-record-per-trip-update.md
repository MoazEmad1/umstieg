# 0004: One Kafka record per TripUpdate

Status: Accepted · 2026-10-30

## Context
A snapshot is about 3.4 MB with roughly 2,700 TripUpdates. That's over Kafka's default 1 MB message limit. The GTFS-RT protobuf is already a stable schema. `start_date` is optional in GTFS-RT, so I can't count on it for keys.

## Decision

| Topic | Key | Value |
|---|---|---|
| `vbb.gtfsrt.trip-updates.v1` | `trip_id` | the original `FeedEntity` protobuf bytes |
| `vbb.gtfsrt.snapshots.v1` | `source` | JSON summary of each poll (time, status, size, entity count, stale or not) |
| `vbb.gtfsrt.trip-updates.v1.dlq` | same as input | records that failed |

Trip-update records carry `snapshot_id`, `source`, `feed_ts` and `fetched_at` as headers. The snapshot record is sent after its trip updates, so a consumer can tell when a snapshot is complete. Keying by `trip_id` keeps each trip's updates in order.

## Alternatives
- Whole snapshot as one message: needs bigger limits everywhere, and one bad entity blocks everything.
- My own JSON/Avro schema: needs a schema registry or manual versioning, and hides the original data.
- Key `start_date:trip_id`: breaks when `start_date` is missing.

## Consequences
Reading values needs the GTFS-RT bindings, so the topic isn't human-readable in `kcat`. Snapshot boundaries come from the headers and the snapshots topic, not from Kafka itself.
