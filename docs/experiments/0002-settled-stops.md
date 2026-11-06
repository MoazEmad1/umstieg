# 0002: How far passed stops still move

Status: predictions written · 2026-11-06. Results added 2026-11-06.

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

I ran `make analyse` on the archive for 4 November, after committing the code.

That day had a problem I hadn't planned for. Production had no new data from 05:41 to 10:24 UTC, almost five hours of the morning, on top of the usual gaps in the evening. 27.7% of polls were stale, and VBB's news feed doesn't mention it. Trips running in that window are missing, which is why the counts are smaller than on 2 November.

- P1 right. 11.36% of 951,615 settled stops moved.
- P2 wrong, narrowly. 89.91% moved less than 60 seconds.
- P3 wrong. 65.99% of the 108,146 moves went later.
- P4 right, narrowly. For 85.37% of stops, the last value was within 2 minutes of the value at departure.

Two right, two wrong. Moves by size, later / earlier:
- under 1 min: 6,648 / 5,450
- 1–2 min: 29,951 / 15,492
- 2–5 min: 22,494 / 10,984
- 5–15 min: 7,816 / 3,883
- over 15 min: 4,453 / 975

Because of the outage I also ran the same analysis on 2 November. It isn't part of the predictions and doesn't change the verdicts above. That day 9.15% moved, 92.23% moved less than 60 seconds, 69.07% of moves went later, and 87.19% were close to the value at departure. P2 would have been right there, but the picture is the same on both days.

0001 counted a stop as changed if its value changed at any point after it settled. Here I only compare the last value with the settled one, so 2 November shows 9.15% here and 11.56% in 0001.

## Why the wrong ones were wrong

P3: I thought trains running later than their forecast would make up nearly all moves. They make up two thirds. The rest go earlier, so something also corrects times in the other direction after the fact.

P2 and the sizes: I expected the moves to be mostly seconds. They're mostly 1–5 minutes, and under a minute is one of the smallest groups. My guess is that the feed replaces its forecast with a reported actual time a few minutes after departure. I haven't checked that.

## What this changes

- The last value isn't a clean real departure time. About one stop in ten still moves by a minute or more after it has settled, and a few thousand a day by more than 15 minutes.
- Phase 2 can still use the last value as its label, but I shouldn't claim more than about a minute of precision, and I'll report how many labels moved late.
- Which value is closest to what really happened can't be answered from the feed alone.
- Whole mornings can be missing. Days with long gaps have to be marked as such, not used as if they were complete.
