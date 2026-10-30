# 0003: Build a raw snapshot collector first

Status: Accepted · 2026-10-30

## Context
Phase 2 needs weeks of history, and every day I don't collect is lost. The full pipeline will take a while and its schema will change along the way. My laptop sleeps, so it can't be the collector.

## Decision
- Before anything else, a small collector saves each feed response as-is to `data/raw/{source}/YYYY/MM/DD/HHMMSS.pb.gz`, plus a small JSON file with fetch time, status, ETag, size and header timestamp.
- It fetches every 60 seconds per source and runs on a cheap always-on VPS (Hetzner or Lightsail). Files are synced to object storage daily.
- The full pipeline gets a replay mode that publishes these files to Kafka in order, using the original fetch time.

## Alternatives
- Wait for the full pipeline: I'd lose weeks of data.
- Run the whole stack on the VPS now: more to operate, and the rows would need migrating anyway. Raw files never do.

## Consequences
The raw files are the ground truth, so Postgres can always be rebuilt from them. My rough estimate is 1.1 GB/day at a 60-second interval, which I still have to measure. If that's too much, I'll fetch less often. Replay means the sink has to be idempotent.
