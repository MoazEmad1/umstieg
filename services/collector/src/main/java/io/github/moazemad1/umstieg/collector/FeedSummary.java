package io.github.moazemad1.umstieg.collector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public record FeedSummary(Optional<Instant> feedTimestamp, int entityCount, Optional<String> parseError) {

    public static final FeedSummary NONE = new FeedSummary(Optional.empty(), 0, Optional.empty());

    public static FeedSummary of(FetchResult result) {
        if (!result.ok() || result.body().length == 0) {
            return NONE;
        }
        try (InputStream in = open(result)) {
            FeedMessage feed = FeedMessage.parseFrom(in);
            Optional<Instant> timestamp = feed.getHeader().hasTimestamp()
                    ? Optional.of(Instant.ofEpochSecond(feed.getHeader().getTimestamp()))
                    : Optional.empty();
            return new FeedSummary(timestamp, feed.getEntityCount(), Optional.empty());
        } catch (IOException e) {
            return new FeedSummary(Optional.empty(), 0, Optional.of(e.toString()));
        }
    }

    private static InputStream open(FetchResult result) throws IOException {
        InputStream raw = new ByteArrayInputStream(result.body());
        return result.gzipped() ? new GZIPInputStream(raw) : raw;
    }
}
