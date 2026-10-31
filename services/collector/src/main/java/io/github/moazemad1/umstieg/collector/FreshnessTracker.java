package io.github.moazemad1.umstieg.collector;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class FreshnessTracker {

    public enum Transition { NONE, WENT_STALE, RECOVERED }

    private final Duration maxFeedAge;
    private final Duration staleAfter;
    private Instant lastFreshAt;
    private boolean stale;

    public FreshnessTracker(FreshnessProperties properties, Instant startedAt) {
        this.maxFeedAge = properties.maxFeedAge();
        this.staleAfter = properties.staleAfter();
        this.lastFreshAt = startedAt;
    }

    public synchronized Transition record(Instant now, Optional<Instant> feedTimestamp, int entityCount) {
        boolean fresh = entityCount > 0
                && feedTimestamp.map(ts -> Duration.between(ts, now).compareTo(maxFeedAge) < 0).orElse(false);
        if (fresh) {
            lastFreshAt = now;
        }
        boolean nowStale = Duration.between(lastFreshAt, now).compareTo(staleAfter) >= 0;
        Transition transition = nowStale == stale ? Transition.NONE
                : nowStale ? Transition.WENT_STALE : Transition.RECOVERED;
        stale = nowStale;
        return transition;
    }

    public synchronized boolean stale() {
        return stale;
    }

    public synchronized Instant lastFreshAt() {
        return lastFreshAt;
    }
}
