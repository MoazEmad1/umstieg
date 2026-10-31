package io.github.moazemad1.umstieg.collector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

final class Feeds {

    private Feeds() {
    }

    static byte[] feedWithTrips(Instant timestamp, int trips) {
        var feed = FeedMessage.newBuilder()
                .setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0").setTimestamp(timestamp.getEpochSecond()));
        for (int i = 0; i < trips; i++) {
            feed.addEntity(FeedEntity.newBuilder()
                    .setId("e" + i)
                    .setTripUpdate(TripUpdate.newBuilder().setTrip(TripDescriptor.newBuilder().setTripId("trip-" + i))));
        }
        return feed.build().toByteArray();
    }

    static FetchResult ok(String source, Instant fetchedAt, byte[] body, boolean gzipped) {
        return new FetchResult(source, fetchedAt, Duration.ofMillis(120), 200, Optional.of("\"etag-1\""),
                gzipped ? Optional.of("gzip") : Optional.empty(), body);
    }

    static FetchResult status(String source, Instant fetchedAt, int status) {
        return new FetchResult(source, fetchedAt, Duration.ofMillis(80), status, Optional.empty(), Optional.empty(), new byte[0]);
    }

    static byte[] gzip(byte[] data) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var gz = new GZIPOutputStream(out)) {
            gz.write(data);
        }
        return out.toByteArray();
    }
}
