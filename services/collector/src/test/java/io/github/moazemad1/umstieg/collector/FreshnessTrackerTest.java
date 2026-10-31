package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.moazemad1.umstieg.collector.FreshnessTracker.Transition;

class FreshnessTrackerTest {

    private static final Instant T0 = Instant.parse("2026-10-31T10:00:00Z");
    private static final FreshnessProperties PROPS = new FreshnessProperties(Duration.ofMinutes(5), Duration.ofMinutes(15));

    private final FreshnessTracker tracker = new FreshnessTracker(PROPS, T0);

    @Test
    void freshFeedNeverGoesStale() {
        for (int minute = 1; minute <= 30; minute++) {
            Instant now = T0.plusSeconds(60L * minute);
            assertThat(tracker.record(now, Optional.of(now.minusSeconds(20)), 2700)).isEqualTo(Transition.NONE);
        }
        assertThat(tracker.stale()).isFalse();
    }

    @Test
    void emptyFeedGoesStaleAfterFifteenMinutesOnce() {
        Optional<Instant> oldHeader = Optional.of(T0.minusSeconds(3600));

        assertThat(tracker.record(T0.plusSeconds(14 * 60), oldHeader, 0)).isEqualTo(Transition.NONE);
        assertThat(tracker.record(T0.plusSeconds(15 * 60), oldHeader, 0)).isEqualTo(Transition.WENT_STALE);
        assertThat(tracker.record(T0.plusSeconds(16 * 60), oldHeader, 0)).isEqualTo(Transition.NONE);
        assertThat(tracker.stale()).isTrue();
    }

    @Test
    void oldHeaderTimestampCountsAsNotFresh() {
        Instant now = T0.plusSeconds(15 * 60);

        assertThat(tracker.record(now, Optional.of(now.minus(Duration.ofMinutes(5))), 2700)).isEqualTo(Transition.WENT_STALE);
    }

    @Test
    void recoversWhenFreshSnapshotReturns() {
        Instant stale = T0.plusSeconds(20 * 60);
        tracker.record(stale, Optional.empty(), 0);

        Instant later = stale.plusSeconds(60);
        assertThat(tracker.record(later, Optional.of(later.minusSeconds(10)), 2700)).isEqualTo(Transition.RECOVERED);
        assertThat(tracker.stale()).isFalse();
    }
}
