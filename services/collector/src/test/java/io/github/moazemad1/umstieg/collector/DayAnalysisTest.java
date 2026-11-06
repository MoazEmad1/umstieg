package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

import tools.jackson.databind.json.JsonMapper;

class DayAnalysisTest {

    private static final Instant START = Instant.parse("2026-11-02T08:00:00Z");

    @TempDir
    Path root;

    @Test
    void passedStopsThatStayAreCountedAsKept() {
        var analysis = new DayAnalysis();
        for (int minute = 0; minute < 25; minute++) {
            Instant now = START.plus(Duration.ofMinutes(minute));
            analysis.accept(ok(now, trip("t1", "20261102", stop(1, START), stop(2, START.plusSeconds(600)))));
        }

        var report = analysis.report();

        assertThat(report.passedObservations()).isPositive();
        assertThat(report.passedKept()).isEqualTo(report.passedObservations());
        assertThat(report.settledStops()).isEqualTo(2);
        assertThat(report.settledStopsChanged()).isZero();
    }

    @Test
    void passedStopsThatDisappearAreCountedAsDropped() {
        var analysis = new DayAnalysis();
        for (int minute = 0; minute < 20; minute++) {
            Instant now = START.plus(Duration.ofMinutes(minute));
            var stops = new ArrayList<StopTimeUpdate>();
            if (minute < 3) {
                stops.add(stop(1, START));
            }
            stops.add(stop(2, START.plusSeconds(3600)));
            analysis.accept(ok(now, trip("t1", "20261102", stops.toArray(StopTimeUpdate[]::new))));
        }

        var report = analysis.report();

        assertThat(report.passedObservations()).isPositive();
        assertThat(report.passedKept()).isZero();
    }

    @Test
    void settledStopWhoseTimeKeepsMovingIsCounted() {
        var analysis = new DayAnalysis();
        for (int minute = 0; minute < 20; minute++) {
            Instant now = START.plus(Duration.ofMinutes(minute));
            analysis.accept(ok(now, trip("t1", "20261102", stop(1, START.plusSeconds(minute)))));
        }

        var report = analysis.report();

        assertThat(report.settledStops()).isEqualTo(1);
        assertThat(report.settledStopsChanged()).isEqualTo(1);
    }

    @Test
    void settledStopPushedLaterIsCountedBySize() {
        var analysis = new DayAnalysis();
        for (int minute = 0; minute < 25; minute++) {
            Instant now = START.plus(Duration.ofMinutes(minute));
            Instant departure = minute < 15 ? START : START.plusSeconds(420);
            analysis.accept(ok(now, trip("t1", "20261102", stop(1, departure))));
        }

        var moves = analysis.report().moves();

        assertThat(moves.settledStops()).isEqualTo(1);
        assertThat(moves.moved()).isEqualTo(1);
        assertThat(moves.movedLater()).isEqualTo(1);
        assertThat(moves.withinMinute()).isZero();
        assertThat(moves.laterBySize()).containsExactly(0, 0, 0, 1, 0);
        assertThat(moves.departureStops()).isEqualTo(1);
        assertThat(moves.departureClose()).isZero();
    }

    @Test
    void smallEarlierCorrectionStaysCloseToDeparture() {
        var analysis = new DayAnalysis();
        for (int minute = 0; minute < 25; minute++) {
            Instant now = START.plus(Duration.ofMinutes(minute));
            Instant departure = minute < 12 ? START : START.minusSeconds(20);
            analysis.accept(ok(now, trip("t1", "20261102", stop(1, departure))));
        }

        var moves = analysis.report().moves();

        assertThat(moves.moved()).isEqualTo(1);
        assertThat(moves.movedLater()).isZero();
        assertThat(moves.withinMinute()).isEqualTo(1);
        assertThat(moves.earlierBySize()).containsExactly(1, 0, 0, 0, 0);
        assertThat(moves.departureClose()).isEqualTo(1);
    }

    @Test
    void stopThatNeverMovesCountsAsSettledWithoutMove() {
        var analysis = new DayAnalysis();
        for (int minute = 0; minute < 25; minute++) {
            analysis.accept(ok(START.plus(Duration.ofMinutes(minute)), trip("t1", "20261102", stop(1, START))));
        }

        var moves = analysis.report().moves();

        assertThat(moves.settledStops()).isEqualTo(1);
        assertThat(moves.moved()).isZero();
        assertThat(moves.withinMinute()).isEqualTo(1);
    }

    @Test
    void tripsSeenFewerThanTenTimesAreLeftOut() {
        var analysis = new DayAnalysis();
        for (int minute = 0; minute < 9; minute++) {
            Instant now = START.plus(Duration.ofMinutes(minute + 10));
            analysis.accept(ok(now, trip("t1", "20261102", stop(1, START))));
        }

        assertThat(analysis.report().passedObservations()).isZero();
    }

    @Test
    void missingStartDateAndStopSequenceAreCounted() {
        var analysis = new DayAnalysis();
        var noSequence = StopTimeUpdate.newBuilder().setStopId("s1")
                .setDeparture(StopTimeEvent.newBuilder().setTime(START.getEpochSecond())).build();
        analysis.accept(ok(START, trip("t1", null, noSequence, stop(2, START)), trip("t2", "20261102", stop(1, START))));

        var report = analysis.report();

        assertThat(report.tripUpdates()).isEqualTo(2);
        assertThat(report.tripUpdatesWithoutStartDate()).isEqualTo(1);
        assertThat(report.stopUpdates()).isEqualTo(3);
        assertThat(report.stopUpdatesWithoutSequence()).isEqualTo(1);
    }

    @Test
    void pollsAreSortedIntoOkNotModifiedFailedAndStale() {
        var analysis = new DayAnalysis();
        analysis.accept(ok(START, trip("t1", "20261102", stop(1, START))));
        analysis.accept(snapshot(START.plusSeconds(60), 304, null, null, 0, null));
        analysis.accept(snapshot(START.plusSeconds(120), null, null, null, null, "java.net.http.HttpTimeoutException"));
        analysis.accept(snapshot(START.plusSeconds(180), 200, START.minusSeconds(3600), feed(START.minusSeconds(3600)), 0, null));
        analysis.accept(ok(START.plusSeconds(240), trip("t1", "20261102", stop(1, START))));

        var report = analysis.report();

        assertThat(report.polls()).isEqualTo(5);
        assertThat(report.ok()).isEqualTo(2);
        assertThat(report.notModified()).isEqualTo(1);
        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.stale()).isEqualTo(2);
        assertThat(report.longestWithoutFresh()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void longGapWithoutFreshSnapshotsIsMeasured() {
        var analysis = new DayAnalysis();
        analysis.accept(ok(START, trip("t1", "20261102", stop(1, START))));
        for (int minute = 1; minute <= 16; minute++) {
            analysis.accept(snapshot(START.plus(Duration.ofMinutes(minute)), null, null, null, null, "timeout"));
        }

        assertThat(analysis.report().longestWithoutFresh()).isEqualTo(Duration.ofMinutes(16));
    }

    @Test
    void readsSnapshotsBackFromAPackedArchive() throws Exception {
        Path dayDir = Files.createDirectories(root.resolve("raw/production/2026/11/02"));
        var json = JsonMapper.builder().build();
        Files.write(dayDir.resolve("080000.pb.gz"), Feeds.gzip(feed(START, trip("t1", "20261102", stop(1, START))).toByteArray()));
        Files.write(dayDir.resolve("080000.json"), json.writeValueAsBytes(metadata(START, 200, START, 1, null, "080000.pb.gz")));
        Files.write(dayDir.resolve("080100.json"), json.writeValueAsBytes(metadata(START.plusSeconds(60), 304, null, 0, null, null)));
        var packer = new DayPacker(root.resolve("raw"), new PackProperties(null, root.resolve("archive"), 3, 27), json);
        packer.pack("production", LocalDate.of(2026, 11, 2));

        List<ArchivedSnapshot> read = new ArrayList<>();
        ArchivedSnapshot.read(root.resolve("archive/production/2026/production-2026-11-02.tar.zst"), json, read::add);

        assertThat(read).hasSize(2);
        assertThat(read.get(0).feed()).isPresent();
        assertThat(read.get(0).feed().get().getEntityCount()).isEqualTo(1);
        assertThat(read.get(1).metadata().status()).isEqualTo(304);
        assertThat(read.get(1).feed()).isEmpty();
    }

    private static ArchivedSnapshot ok(Instant now, TripUpdate... trips) {
        return snapshot(now, 200, now, feed(now, trips), trips.length, null);
    }

    private static ArchivedSnapshot snapshot(Instant now, Integer status, Instant feedTime, FeedMessage feed, Integer entities, String error) {
        return new ArchivedSnapshot("s", metadata(now, status, feedTime, entities, error, feed == null ? null : "x.pb.gz"),
                Optional.ofNullable(feed));
    }

    private static SnapshotMetadata metadata(Instant now, Integer status, Instant feedTime, Integer entities, String error, String file) {
        return new SnapshotMetadata("production", now.toString(), status, 100L, null, null, file == null ? null : 1000L,
                feedTime == null ? null : feedTime.toString(), entities, file, error);
    }

    private static FeedMessage feed(Instant timestamp, TripUpdate... trips) {
        var feed = FeedMessage.newBuilder()
                .setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0").setTimestamp(timestamp.getEpochSecond()));
        for (int i = 0; i < trips.length; i++) {
            feed.addEntity(FeedEntity.newBuilder().setId("e" + i).setTripUpdate(trips[i]));
        }
        return feed.build();
    }

    private static TripUpdate trip(String id, String startDate, StopTimeUpdate... stops) {
        var descriptor = TripDescriptor.newBuilder().setTripId(id);
        if (startDate != null) {
            descriptor.setStartDate(startDate);
        }
        return TripUpdate.newBuilder().setTrip(descriptor).addAllStopTimeUpdate(List.of(stops)).build();
    }

    private static StopTimeUpdate stop(int sequence, Instant departure) {
        return StopTimeUpdate.newBuilder()
                .setStopSequence(sequence)
                .setStopId("s" + sequence)
                .setDeparture(StopTimeEvent.newBuilder().setTime(departure.getEpochSecond()))
                .build();
    }
}
