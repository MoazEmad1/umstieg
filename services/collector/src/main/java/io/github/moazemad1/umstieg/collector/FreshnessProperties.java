package io.github.moazemad1.umstieg.collector;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("collector.freshness")
public record FreshnessProperties(
        @DefaultValue("5m") Duration maxFeedAge,
        @DefaultValue("15m") Duration staleAfter) {
}
