# 0002: How far passed stops still move

Status: predictions written · 2026-11-06. No analysis done yet.

## Why

Experiment 0001 showed that the feed keeps stops a trip has already passed, so the last value I see for a stop could be its real departure time. But 11.56% of stops whose time was more than 10 minutes in the past still changed after that. If those changes are a few seconds, the last value is good enough as the "true" delay for Phase 2. If they are minutes, I need something else.

## Data

- Production, 2026-11-04 (a Wednesday), 00:00 to 24:00 UTC, from the Hugging Face archive. I haven't looked at that day.
- Same rules as 0001: a trip is `trip_id` plus `start_date`, a stop is its `stop_sequence` (or `stop_id` if that's missing), and a stop's time is its departure time, else its arrival time. Only trips seen in at least 10 snapshots count.

For each stop I keep three values:
- **at departure**: its time in the first snapshot where that time is already in the past.
- **settled**: its time in the first snapshot where that time is more than 10 minutes in the past.
- **last**: its time in the last snapshot that has the stop.

A stop's **move** is last minus settled, in seconds. Positive means later.

## What I've already seen

- The 0001 result for 2026-11-02: 11.56% of 1.3 million settled stops changed. Nothing about size or direction.
- The night gaps in production between 19:57 and 02:00 UTC.

## Predictions

I think two things cause the changes. One is small corrections when the actual departure is reported. The other is trains running later than their forecast: the forecast time passes, the train hasn't left yet, and the feed pushes the time later. That kind would be large and always later.

**P1. The share is about the same.** Settled stops with any move: 8–15%. Confidence: 80%.

**P2. Most moves are small.** At least 90% of all settled stops move by less than 60 seconds in either direction. Confidence: 60%.

**P3. Moves go later.** Of the stops that move, at least 80% move later. Confidence: 70%.

**P4. The value at departure is already close.** For at least 85% of stops, last minus at departure is under 2 minutes in either direction. Confidence: 55%.

## How I'll check

I'll extend the day analysis to keep the three values per stop. It prints the four shares, judged against the thresholds above, and a count of moves by size (under 1 min, 1–2, 2–5, 5–15, over 15 min) and direction. The counts aren't judged; they're there to explain the result.

If a prediction is wrong, I write down why I think it was, and I don't change the thresholds.

## Results

Not done yet.
