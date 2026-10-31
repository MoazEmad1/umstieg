# 0003: Build a raw snapshot collector first

Status: Accepted · 2026-10-30

## Context
Phase 2 needs weeks of history, and every day I don't collect is lost. The full pipeline will take a while and its schema will change along the way. My laptop sleeps, so it can't be the collector.

## Decision
- Before anything else, a small collector saves each feed response as-is to `data/raw/{source}/YYYY/MM/DD/HHMMSS.pb.gz` (UTC), plus a small JSON file with fetch time, status, ETag, size and header timestamp.
- It fetches production every 60 seconds and staging every 5 minutes. It runs on a small Hetzner Cloud VPS, and files are synced to Hetzner Object Storage daily.
- Update 2026-10-31: to keep it free, it runs on an Azure for Students VM instead, and a packed archive of each day goes to a public Hugging Face dataset (0011).
- The full pipeline gets a replay mode that publishes these files to Kafka in order, using the original fetch time.

## Alternatives
- Wait for the full pipeline: I'd lose weeks of data.
- Run the whole stack on the VPS now: more to operate, and the rows would need migrating anyway. Raw files never do.

## Consequences
The raw files are the ground truth, so Postgres can always be rebuilt from them. My first estimate was 1.1 GB/day, based on staging alone. A live run on 2026-10-31 gave about 1.43 MB per production snapshot and 0.56 MB per staging one, so these intervals come to about 2.2 GB/day, roughly 66 GB a month. Replay means the sink has to be idempotent.
