package com.dersbul.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Render, Heroku, Neon gibi servislerin verdiği DATABASE_URL (postgres://kullanici:sifre@host:port/db)
 * değişkenini JDBC bağlantısına çevirir. DATABASE_URL tanımlı değilse hiç devreye girmez
 * ve normal spring.datasource.* ayarları (H2 veya DB_URL) kullanılır.
 */
@Configuration
@ConditionalOnProperty(name = "database.url")
public class DatabaseUrlConfig {

    @Bean
    DataSource dataSource(@Value("${database.url}") String raw) throws Exception {
        URI u = new URI(raw.trim().replaceFirst("^postgres(ql)?://", "postgresql://"));
        String[] userInfo = (u.getUserInfo() == null ? ":" : u.getUserInfo()).split(":", 2);
        int port = u.getPort() == -1 ? 5432 : u.getPort();
        String jdbc = "jdbc:postgresql://" + u.getHost() + ":" + port + u.getPath()
            + (u.getQuery() != null ? "?" + u.getQuery() : "");

        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(jdbc);
        c.setUsername(URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8));
        c.setPassword(userInfo.length > 1 ? URLDecoder.decode(userInfo[1], StandardCharsets.UTF_8) : "");
        c.setMaximumPoolSize(5);
        c.setMinimumIdle(0);
        c.setConnectionTimeout(30_000);
        c.setInitializationFailTimeout(60_000);   // uykudaki veritabanının uyanmasını bekler
        c.setMaxLifetime(600_000);
        return new HikariDataSource(c);
    }
}
