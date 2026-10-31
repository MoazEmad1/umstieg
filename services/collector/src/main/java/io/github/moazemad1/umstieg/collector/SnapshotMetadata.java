package io.github.moazemad1.umstieg.collector;

public record SnapshotMetadata(
        String source,
        String fetchedAt,
        Integer status,
        Long latencyMs,
        String etag,
        String contentEncoding,
        Long bytes,
        String feedTimestamp,
        Integer entityCount,
        String snapshotFile,
        String error) {

    static SnapshotMetadata of(FetchResult result, FeedSummary summary, String snapshotFile) {
        return new SnapshotMetadata(
                result.source(),
                result.fetchedAt().toString(),
                result.status(),
                result.latency().toMillis(),
                result.etag().orElse(null),
                result.contentEncoding().orElse(null),
                (long) result.body().length,
                summary.feedTimestamp().map(Object::toString).orElse(null),
                summary.entityCount(),
                snapshotFile,
                summary.parseError().orElse(null));
    }

    static SnapshotMetadata failure(String source, String fetchedAt, String error) {
        return new SnapshotMetadata(source, fetchedAt, null, null, null, null, null, null, null, null, error);
    }
}
