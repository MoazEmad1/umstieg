package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.moazemad1.umstieg.collector.DayAnalysis.Moves;
import io.github.moazemad1.umstieg.collector.DayAnalysis.Report;

class DayReportTextTest {

    @Test
    void judgesEachPredictionByItsThreshold() {
        var report = new Report(1440, 1430, 2, 8, 12, Duration.ofMinutes(4), 1_600_000_000L,
                9_000_000, 10_000, 200_000_000, 20_000_000,
                1000, 950, 500, 10,
                new Moves(400, 40, 36, 380, 600, 540, new long[] {20, 6, 5, 3, 2}, new long[] {4, 0, 0, 0, 0}));

        String text = DayReportText.format("production", "production-2026-11-02.tar.zst", 120_000_000L, report);

        assertThat(text)
                .contains("P1 passed stops: 95.00% of 1000 observations still present -> kept -> right")
                .contains("P1 extra, settled stops that still changed: 2.00% of 500 -> right")
                .contains("P2 TripUpdates without start_date: 0.11% of 9000000 -> right")
                .contains("P3 stop updates without stop_sequence: 10.00% of 200000000 -> wrong")
                .contains("P4 polls 200 with trips: 99.31%, failed: 0.56% -> right")
                .contains("P5 not modified (304): 0.14% -> right")
                .contains("alert would fire: no -> right")
                .contains("P7 production 1.60 GB as received, archive 13.3x smaller -> right")
                .contains("P1 settled stops that moved: 10.00% of 400 -> right")
                .contains("P2 settled stops that moved less than 60 s: 95.00% -> right")
                .contains("P3 moves that went later: 90.00% of 40 -> right")
                .contains("P4 last value within 2 min of the value at departure: 90.00% of 600 -> right")
                .contains("Moves later by size: under 1 min 20, 1-2 min 6, 2-5 min 5, 5-15 min 3, over 15 min 2");
    }

    @Test
    void movesOutsideTheirThresholdsAreWrong() {
        var report = new Report(1, 1, 0, 0, 0, Duration.ZERO, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                new Moves(100, 30, 10, 75, 100, 50, new long[5], new long[5]));

        String text = DayReportText.format("production", "p.tar.zst", 0, report);

        assertThat(text)
                .contains("P1 settled stops that moved: 30.00% of 100 -> wrong")
                .contains("P2 settled stops that moved less than 60 s: 75.00% -> wrong")
                .contains("P3 moves that went later: 33.33% of 30 -> wrong")
                .contains("P4 last value within 2 min of the value at departure: 50.00% of 100 -> wrong");
    }

    @Test
    void mixedPassedStopsCountAsWrong() {
        var report = new Report(1, 1, 0, 0, 0, Duration.ZERO, 0, 0, 0, 0, 0, 100, 50, 0, 0, Moves.none());

        String text = DayReportText.format("production", "p.tar.zst", 0, report);

        assertThat(text).contains("-> mixed -> wrong");
    }

    @Test
    void stagingOnlyGetsTheVolumeCheck() {
        var report = new Report(288, 288, 0, 0, 0, Duration.ZERO, 90_000_000L, 0, 0, 0, 0, 0, 0, 0, 0, Moves.none());

        String text = DayReportText.format("staging", "staging-2026-11-02.tar.zst", 9_000_000L, report);

        assertThat(text).contains("P7 staging under 0.2 GB: right").doesNotContain("P1");
    }
}
