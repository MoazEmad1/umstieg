# 0001: What the feed looks like over one full day

Status: predictions written · 2026-11-01. No analysis done yet.

## Why

Before designing Phase 2, I need to know a few things the documentation doesn't say. Most important: whether the feed keeps updates for stops a trip has already passed. If it drops them, the last value I see for a stop is a forecast made shortly before the train got there, not the delay it actually had. That decides what I can use as the "true" delay.

## Data

- The first full UTC day collected on the VM: **2026-11-02**, 00:00 to 24:00 UTC. Collection started on 2026-11-01 at 03:27:58 UTC, so 1 November is incomplete and I leave it out.
- Production is polled every 60 seconds and staging every 5 minutes. I use the archives on Hugging Face, not files on the VM.
- I analyse production. Staging is only used for the volume numbers.

## What I've already seen

So these predictions aren't blind, here is what I looked at before writing them:
- One staging snapshot on 2026-10-30: 2,693 TripUpdates, 67,961 stop updates, 382 marked SKIPPED and 6 NO_DATA.
- Production log lines on 2026-10-31 and 2026-11-01: about 6,900 trips in the daytime and about 4,800 at night, with the feed header 1–17 seconds old.
- Ten production snapshots, one minute apart, to measure compression: 15.0 MB as single gzip files and 1.04 MB as one zstd archive.

I haven't looked inside any snapshot for passed stops or missing fields.

## Predictions

**P1. Passed stops are kept.** Once a trip has passed a stop, its stop update stays in the feed until the trip ends.
- Why: the feed is built from VDV-454 data, which as far as I know sends whole trips, including stops already served.
- Confidence: 65%.
- Measured as: for trips seen in at least 10 snapshots, the share where a stop whose departure time is more than 5 minutes in the past is still there in later snapshots. "Kept" means at least 90% of such stops are still there. "Dropped" means at most 10%. Anything in between I report as mixed, and P1 counts as wrong.
- Also: if they are kept, I expect the times of passed stops to stop changing within 2 minutes of departure, so the last value would be the actual time. I count the share of passed stops whose time still changes more than 10 minutes after departure, and predict under 5%.

**P2. `start_date` is almost always set.** Share of TripUpdates without `trip.start_date`: under 1%.

**P3. `stop_sequence` is almost always set.** Share of stop updates without `stop_sequence`: under 5%.

**P4. Polls work.** Production returns 200 with at least one entity on at least 98% of the 1,440 polls. Errors and timeouts together are under 2%.

**P5. Almost no "not modified" answers.** The header is usually less than 20 seconds old and I poll every 60 seconds, so 304 answers should be rare: under 1% of production polls.

**P6. Stale snapshots are rare.** The share of production polls where the header is more than 5 minutes old, or the feed is empty: under 2%. The 15-minute stale alert does not fire that day.

**P7. Volumes.**
- Production `.pb.gz` files: 1.2–2.2 GB for the day. Daytime snapshots are bigger than night ones, so it's less than 1,440 × 1.43 MB.
- Staging: under 0.2 GB.
- The production `.tar.zst` for the day is 8–20 times smaller than its `.pb.gz` files, so roughly 60–250 MB.

**P8. Packing fits the night.** Packing and uploading the day on the B2ats v2 VM takes under 90 minutes in total, and the VM doesn't run out of memory.

## How I'll check

- P1–P3 need a small analysis tool that reads the archive and decodes the snapshots. I'll write it as a command in the collector with tests. It only produces the numbers above, so I can't go looking for nicer ones afterwards.
- P4–P6 come from the `.json` sidecars (status, entity count, feed timestamp, error).
- P7 comes from the manifest (`bytes`, `archiveBytes`) and the sizes of the files on the VM before packing.
- P8 comes from `journalctl -u umstieg-archive`.

Every prediction is judged right or wrong by the thresholds written here. If one is wrong, I write down why I think it was, and I don't change the thresholds afterwards.

## Results

Not done yet.
