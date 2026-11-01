package io.github.moazemad1.umstieg.collector;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class DayAnalysis {

    static final Duration STALE_AGE = Duration.ofMinutes(5);
    static final Duration ALERT_AFTER = Duration.ofMinutes(15);
    static final long PASSED_SECONDS = 5 * 60;
    static final long SETTLED_SECONDS = 10 * 60;
    static final int MIN_TRIP_SNAPSHOTS = 10;
    static final Duration FORGET_TRIP_AFTER = Duration.ofHours(3);

    public record Report(
            long polls, long ok, long notModified, long failed, long stale, Duration longestWithoutFresh,
            long receivedBytes,
            long tripUpdates, long tripUpdatesWithoutStartDate,
            long stopUpdates, long stopUpdatesWithoutSequence,
            long passedObservations, long passedKept,
            long settledStops, long settledStopsChanged) {

        public double share(long part, long whole) {
            return whole == 0 ? 0 : (double) part / whole;
        }
    }

    private static final class StopState {
        long time;
        boolean settledSeen;
        boolean settledChanged;
    }

    private static final class TripState {
        int snapshots;
        Instant lastSeen;
        long observations;
        long kept;
        final Map<String, StopState> stops = new HashMap<>();
    }

    private final Map<String, TripState> trips = new HashMap<>();

    private long polls, ok, notModified, failed, stale, receivedBytes;
    private long tripUpdates, withoutStartDate, stopUpdates, withoutSequence;
    private long passedObservations, passedKept, settledStops, settledChanged;
    private Instant lastFreshAt;
    private Optional<Instant> lastFeedTimestamp = Optional.empty();
    private Duration longestWithoutFresh = Duration.ZERO;

    public void accept(ArchivedSnapshot snapshot) {
        Instant now = snapshot.fetchedAt();
        countPoll(snapshot, now);
        snapshot.feed().ifPresent(feed -> {
            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    countTrip(entity.getTripUpdate(), now);
                }
            }
        });
        forgetTripsBefore(now.minus(FORGET_TRIP_AFTER));
    }

    public Report report() {
        forgetTripsBefore(Instant.MAX);
        return new Report(polls, ok, notModified, failed, stale, longestWithoutFresh, receivedBytes,
                tripUpdates, withoutStartDate, stopUpdates, withoutSequence,
                passedObservations, passedKept, settledStops, settledChanged);
    }

    private void countPoll(ArchivedSnapshot snapshot, Instant now) {
        SnapshotMetadata m = snapshot.metadata();
        polls++;
        if (lastFreshAt == null) {
            lastFreshAt = now;
        }
        if (m.snapshotFile() != null && m.bytes() != null) {
            receivedBytes += m.bytes();
        }

        int status = m.status() == null ? 0 : m.status();
        boolean isFailed = m.error() != null || (status != 200 && status != 304);
        boolean isOk = !isFailed && status == 200 && m.entityCount() != null && m.entityCount() > 0;
        if (status == 200 && m.feedTimestamp() != null) {
            lastFeedTimestamp = Optional.of(Instant.parse(m.feedTimestamp()));
        }

        boolean fresh;
        if (isFailed) {
            failed++;
            fresh = false;
        } else if (status == 304) {
            notModified++;
            fresh = lastFeedTimestamp.map(ts -> Duration.between(ts, now).compareTo(STALE_AGE) <= 0).orElse(false);
        } else {
            fresh = isOk && lastFeedTimestamp.map(ts -> Duration.between(ts, now).compareTo(STALE_AGE) <= 0).orElse(false);
        }
        if (isOk) {
            ok++;
        }
        if (fresh) {
            lastFreshAt = now;
        } else {
            stale++;
            Duration gap = Duration.between(lastFreshAt, now);
            if (gap.compareTo(longestWithoutFresh) > 0) {
                longestWithoutFresh = gap;
            }
        }
    }

    private void countTrip(TripUpdate update, Instant now) {
        tripUpdates++;
        if (!update.getTrip().hasStartDate()) {
            withoutStartDate++;
        }
        String tripKey = update.getTrip().getTripId() + "|" + update.getTrip().getStartDate();
        TripState trip = trips.computeIfAbsent(tripKey, k -> new TripState());
        trip.snapshots++;
        trip.lastSeen = now;

        Map<String, Long> current = new HashMap<>();
        for (StopTimeUpdate stop : update.getStopTimeUpdateList()) {
            stopUpdates++;
            if (!stop.hasStopSequence()) {
                withoutSequence++;
            }
            stopTime(stop).ifPresent(time -> current.put(stopKey(stop), time));
        }

        long nowSeconds = now.getEpochSecond();
        for (Map.Entry<String, StopState> known : trip.stops.entrySet()) {
            if (known.getValue().time + PASSED_SECONDS < nowSeconds) {
                trip.observations++;
                if (current.containsKey(known.getKey())) {
                    trip.kept++;
                }
            }
        }
        for (Map.Entry<String, Long> entry : current.entrySet()) {
            StopState state = trip.stops.get(entry.getKey());
            if (state == null) {
                state = new StopState();
                trip.stops.put(entry.getKey(), state);
            } else if (state.time + SETTLED_SECONDS < nowSeconds) {
                state.settledSeen = true;
                if (state.time != entry.getValue()) {
                    state.settledChanged = true;
                }
            }
            state.time = entry.getValue();
        }
    }

    private void forgetTripsBefore(Instant cutoff) {
        Iterator<TripState> it = trips.values().iterator();
        while (it.hasNext()) {
            TripState trip = it.next();
            if (trip.lastSeen.isBefore(cutoff)) {
                if (trip.snapshots >= MIN_TRIP_SNAPSHOTS) {
                    passedObservations += trip.observations;
                    passedKept += trip.kept;
                    for (StopState stop : trip.stops.values()) {
                        if (stop.settledSeen) {
                            settledStops++;
                            if (stop.settledChanged) {
                                settledChanged++;
                            }
                        }
                    }
                }
                it.remove();
            }
        }
    }

    private static String stopKey(StopTimeUpdate stop) {
        return stop.hasStopSequence() ? "seq:" + stop.getStopSequence() : "id:" + stop.getStopId();
    }

    private static Optional<Long> stopTime(StopTimeUpdate stop) {
        if (stop.hasDeparture() && stop.getDeparture().hasTime()) {
            return Optional.of(stop.getDeparture().getTime());
        }
        if (stop.hasArrival() && stop.getArrival().hasTime()) {
            return Optional.of(stop.getArrival().getTime());
        }
        return Optional.empty();
    }
}
