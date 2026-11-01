package io.github.moazemad1.umstieg.collector;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
class CollectorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CollectorConfiguration.class);

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FeedFetcher feedFetcher(CollectorProperties properties, Clock clock) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new FeedFetcher(client, properties.userAgent(), clock);
    }

    @Bean
    SnapshotWriter snapshotWriter(CollectorProperties properties) {
        return new SnapshotWriter(properties.dataDir(), JsonMapper.builder().build());
    }

    @Bean
    Notifier notifier(TelegramProperties telegram) {
        if (!telegram.enabled()) {
            log.info("Telegram alerts disabled: collector.telegram.bot-token or chat-id not set");
            return Notifier.NONE;
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return new TelegramNotifier(client, telegram, JsonMapper.builder().build());
    }

    @Bean
    List<SourcePoller> sourcePollers(CollectorProperties properties, FreshnessProperties freshness,
            FeedFetcher fetcher, SnapshotWriter writer, Notifier notifier, Clock clock) {
        return properties.sources().stream()
                .map(source -> new SourcePoller(source, fetcher, writer,
                        new FreshnessTracker(freshness, clock.instant()), notifier, clock))
                .toList();
    }

    @Bean
    @ConditionalOnProperty("collector.pack.date")
    PackRunner packRunner(CollectorProperties properties, PackProperties pack, Clock clock) {
        var packer = new DayPacker(properties.dataDir(), pack, JsonMapper.builder().build());
        return new PackRunner(packer, properties, pack, clock);
    }

    @Bean(destroyMethod = "shutdownNow")
    @ConditionalOnBooleanProperty(name = "collector.polling-enabled", matchIfMissing = true)
    ScheduledExecutorService pollingExecutor(List<SourcePoller> pollers) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(pollers.size());
        for (SourcePoller poller : pollers) {
            executor.scheduleAtFixedRate(poller, 0, poller.source().interval().toMillis(), TimeUnit.MILLISECONDS);
        }
        return executor;
    }
}
