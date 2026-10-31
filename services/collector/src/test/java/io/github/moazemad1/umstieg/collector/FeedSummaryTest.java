package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class FeedSummaryTest {

    private static final Instant FEED_TS = Instant.parse("2026-10-30T18:07:00Z");

    @Test
    void readsHeaderTimestampAndEntityCountFromGzippedBody() throws Exception {
        var result = Feeds.ok("staging", FEED_TS, Feeds.gzip(Feeds.feedWithTrips(FEED_TS, 3)), true);

        FeedSummary summary = FeedSummary.of(result);

        assertThat(summary.feedTimestamp()).contains(FEED_TS);
        assertThat(summary.entityCount()).isEqualTo(3);
        assertThat(summary.parseError()).isEmpty();
    }

    @Test
    void readsPlainBody() {
        var result = Feeds.ok("staging", FEED_TS, Feeds.feedWithTrips(FEED_TS, 1), false);

        assertThat(FeedSummary.of(result).entityCount()).isEqualTo(1);
    }

    @Test
    void headerOnlyFeedHasZeroEntities() {
        var result = Feeds.ok("production", FEED_TS, Feeds.feedWithTrips(FEED_TS, 0), false);

        FeedSummary summary = FeedSummary.of(result);

        assertThat(summary.entityCount()).isZero();
        assertThat(summary.feedTimestamp()).contains(FEED_TS);
    }

    @Test
    void reportsGarbageAsParseError() {
        var result = Feeds.ok("staging", FEED_TS, new byte[] {1, 2, 3, 4, 5}, true);

        FeedSummary summary = FeedSummary.of(result);

        assertThat(summary.parseError()).isPresent();
        assertThat(summary.entityCount()).isZero();
    }

    @Test
    void nonOkResponseHasNoSummary() {
        assertThat(FeedSummary.of(Feeds.status("staging", FEED_TS, 304))).isEqualTo(FeedSummary.NONE);
    }
}
