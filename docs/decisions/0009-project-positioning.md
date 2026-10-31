# 0009: What umstieg is for

Status: Accepted · 2026-10-31

## Context
Many apps already show live delays for Berlin, and the VBB and BVG apps push an alarm when a saved connection breaks. Those alarms are yes or no, they react to the current forecast, and none of the apps say how accurate that forecast is. Bahn-Vorhersage gives a probability that a journey's transfers work, but only for DB trains. The Berlin passenger association IGEB showed in SIGNAL 3/2025 that forecasts which are too early make people miss trains, and that a "±3 minutes" hit rate hides exactly those cases.

## Decision
umstieg does two things:
- A calibrated probability that a transfer between S-Bahn, U-Bahn, tram and bus in Berlin works.
- A public report on how accurate the VBB realtime forecasts are: by mode, line and time ahead, and whether they are early or late.

Phase 1 ends with that report. Phase 2 has to beat the feed's own forecast and the historical transfer success rate, not just the timetable. Phase 3 alerts only when the probability stays low for several polls, and takes alternatives from an existing journey planner.

## Alternatives
- A full journey planner: Google, HAFAS and Citymapper do routing far better.
- Long-distance trains: Bahn-Vorhersage and Railwise already cover them, with years of data.
- Show only live delays: every Berlin app does that already.

## Consequences
I don't build a router, tickets or maps. Every claim has to come with a number I can show, so the report and calibration checks are part of the product, not extras.
