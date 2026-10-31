package io.github.moazemad1.umstieg.collector;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("collector.telegram")
public record TelegramProperties(
        String botToken,
        String chatId,
        @DefaultValue("https://api.telegram.org") URI apiUrl) {

    public boolean enabled() {
        return botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank();
    }
}
