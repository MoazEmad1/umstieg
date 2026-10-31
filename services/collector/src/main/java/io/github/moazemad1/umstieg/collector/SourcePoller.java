package io.github.moazemad1.umstieg.collector;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.moazemad1.umstieg.collector.CollectorProperties.Source;
import io.github.moazemad1.umstieg.collector.FreshnessTracker.Transition;

public class SourcePoller implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SourcePoller.class);

    private final Source source;
    private final FeedFetcher fetcher;
    private final SnapshotWriter writer;
    private final FreshnessTracker freshness;
    private final Notifier notifier;
    private final Clock clock;

    private Optional<String> lastEtag = Optional.empty();
    private FeedSummary lastContent = FeedSummary.NONE;

    public SourcePoller(Source source, FeedFetcher fetcher, SnapshotWriter writer, FreshnessTracker freshness,
            Notifier notifier, Clock clock) {
        this.source = source;
        this.fetcher = fetcher;
        this.writer = writer;
        this.freshness = freshness;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Override
    public void run() {
        Instant startedAt = clock.instant();
        try {
            FetchResult result = fetcher.fetch(source, lastEtag);
            FeedSummary summary = FeedSummary.of(result);
            writer.write(result, summary);
            if (result.ok()) {
                lastEtag = result.etag();
                lastContent = summary;
            }
            log.info("{} status={} entities={} feedAge={} bytes={} latencyMs={}",
                    source.name(), result.status(), lastContent.entityCount(),
                    lastContent.feedTimestamp().map(ts -> Duration.between(ts, result.fetchedAt()).toSeconds() + "s").orElse("-"),
                    result.body().length, result.latency().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            log.warn("{} fetch failed: {}", source.name(), e.toString());
            try {
                writer.writeFailure(source.name(), startedAt, e);
            } catch (Exception writeError) {
                log.error("{} could not write failure record", source.name(), writeError);
            }
        }
        checkFreshness();
    }

    private void checkFreshness() {
        Instant now = clock.instant();
        Transition transition = freshness.record(now, lastContent.feedTimestamp(), lastContent.entityCount());
        switch (transition) {
            case WENT_STALE -> {
                log.warn("{} is STALE: no fresh snapshot since {}", source.name(), freshness.lastFreshAt());
                notifier.send("umstieg: " + source.name() + " is stale, no fresh snapshot since " + freshness.lastFreshAt());
            }
            case RECOVERED -> {
                log.info("{} recovered", source.name());
                notifier.send("umstieg: " + source.name() + " recovered at " + now);
            }
            case NONE -> { }
        }
    }

    public FreshnessTracker freshness() {
        return freshness;
    }

    public Source source() {
        return source;
    }
}
