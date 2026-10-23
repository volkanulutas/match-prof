package com.dersbul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DersbulApplication {
    public static void main(String[] args) {
        // Açılış 4 dakikayı aşarsa nerede takıldığını göstermek için thread dökümü yazar.
        Thread watchdog = new Thread(() -> {
            try { Thread.sleep(240_000); } catch (InterruptedException e) { return; }
            System.out.println("=== STARTUP WATCHDOG: 240 sn geçti, thread dökümü ===");
            Thread.getAllStackTraces().forEach((t, st) -> {
                System.out.println("--- " + t.getName() + " [" + t.getState() + "]");
                for (StackTraceElement el : st) System.out.println("    at " + el);
            });
        }, "startup-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        SpringApplication.run(DersbulApplication.class, args);
        watchdog.interrupt();
    }
}
