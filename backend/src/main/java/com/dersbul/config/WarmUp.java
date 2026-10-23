package com.dersbul.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Tembel başlatma kullanıldığı için ilk gerçek kullanıcı beklemesin diye açılışta bir istek atar. */
@Component
public class WarmUp {
    @Value("${server.port:8080}") private int port;

    @EventListener(ApplicationReadyEvent.class)
    public void warm() {
        Thread t = new Thread(() -> {
            try {
                HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/categories")).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) { /* önemsiz */ }
        }, "warmup");
        t.setDaemon(true);
        t.start();
    }
}
