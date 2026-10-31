package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class TelegramNotifierTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<JsonNode> body = new AtomicReference<>();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            body.set(json.readTree(exchange.getRequestBody().readAllBytes()));
            byte[] response = "{\"ok\":true}".getBytes();
            exchange.sendResponseHeaders(responseStatus.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void postsTextToChat() {
        notifier(serverUrl()).send("production is stale");

        assertThat(path.get()).isEqualTo("/bot123:abc/sendMessage");
        assertThat(body.get().get("chat_id").asString()).isEqualTo("42");
        assertThat(body.get().get("text").asString()).isEqualTo("production is stale");
    }

    @Test
    void errorResponseDoesNotThrow() {
        responseStatus.set(500);

        assertThatNoException().isThrownBy(() -> notifier(serverUrl()).send("x"));
    }

    @Test
    void unreachableApiDoesNotThrow() {
        assertThatNoException().isThrownBy(() -> notifier(URI.create("http://127.0.0.1:1")).send("x"));
    }

    @Test
    void disabledWithoutTokenOrChat() {
        assertThat(new TelegramProperties("", "42", serverUrl()).enabled()).isFalse();
        assertThat(new TelegramProperties("123:abc", null, serverUrl()).enabled()).isFalse();
        assertThat(new TelegramProperties("123:abc", "42", serverUrl()).enabled()).isTrue();
    }

    private TelegramNotifier notifier(URI apiUrl) {
        return new TelegramNotifier(HttpClient.newHttpClient(), new TelegramProperties("123:abc", "42", apiUrl), json);
    }

    private URI serverUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }
}
