package io.github.moazemad1.umstieg.collector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.json.JsonMapper;

public class TelegramNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client;
    private final URI sendMessageUrl;
    private final String chatId;
    private final JsonMapper json;

    public TelegramNotifier(HttpClient client, TelegramProperties properties, JsonMapper json) {
        this.client = client;
        this.sendMessageUrl = properties.apiUrl().resolve("/bot" + properties.botToken() + "/sendMessage");
        this.chatId = properties.chatId();
        this.json = json;
    }

    @Override
    public void send(String text) {
        byte[] body = json.writeValueAsBytes(Map.of("chat_id", chatId, "text", text));
        var request = HttpRequest.newBuilder(sendMessageUrl)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Telegram returned {}: {}", response.statusCode(), response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Telegram message not sent: {}", e.toString());
        }
    }
}
