package com.librarymanagement.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("prod")
public class RailwayDatabaseConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(Environment env,
                                 @Value("${DATABASE_URL:}") String databaseUrl,
                                 @Value("${DATABASE_PRIVATE_URL:}") String privateUrl,
                                 @Value("${JDBC_DATABASE_URL:}") String jdbcUrl,
                                 @Value("${DATABASE_USERNAME:}") String username,
                                 @Value("${DATABASE_PASSWORD:}") String password,
                                 @Value("${DB_POOL_SIZE:10}") int poolSize) {
        String effectiveUrl = null;
        String effectiveUsername = username;
        String effectivePassword = password;
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            effectiveUrl = jdbcUrl;
        } else if (privateUrl != null && !privateUrl.isBlank()) {
            if (privateUrl.startsWith("jdbc:")) effectiveUrl = privateUrl;
            else if (privateUrl.startsWith("postgres://") || privateUrl.startsWith("postgresql://")) {
                effectiveUrl = convertToJdbc(privateUrl);
                Credentials creds = extractCredentials(privateUrl);
                if (creds != null) {
                    if (effectiveUsername == null || effectiveUsername.isBlank()) effectiveUsername = creds.username;
                    if (effectivePassword == null || effectivePassword.isBlank()) effectivePassword = creds.password;
                }
            }
        } else if (databaseUrl != null && !databaseUrl.isBlank()) {
            if (databaseUrl.startsWith("jdbc:")) effectiveUrl = databaseUrl;
            else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                effectiveUrl = convertToJdbc(databaseUrl);
                Credentials creds = extractCredentials(databaseUrl);
                if (creds != null) {
                    if (effectiveUsername == null || effectiveUsername.isBlank()) effectiveUsername = creds.username;
                    if (effectivePassword == null || effectivePassword.isBlank()) effectivePassword = creds.password;
                }
            }
        }
        if (effectiveUrl != null) {
            if ((effectiveUsername == null || effectiveUsername.isBlank()) && env != null) {
                String envUser = env.getProperty("PGUSER");
                if (envUser != null && !envUser.isBlank()) effectiveUsername = envUser;
                else { envUser = env.getProperty("DATABASE_USERNAME"); if (envUser != null && !envUser.isBlank()) effectiveUsername = envUser; }
            }
            if ((effectivePassword == null || effectivePassword.isBlank()) && env != null) {
                String envPass = env.getProperty("PGPASSWORD");
                if (envPass != null && !envPass.isBlank()) effectivePassword = envPass;
                else { envPass = env.getProperty("DATABASE_PASSWORD"); if (envPass != null && !envPass.isBlank()) effectivePassword = envPass; }
            }
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(effectiveUrl);
            if (effectiveUsername != null && !effectiveUsername.isBlank()) ds.setUsername(effectiveUsername);
            if (effectivePassword != null && !effectivePassword.isBlank()) ds.setPassword(effectivePassword);
            ds.setMaximumPoolSize(poolSize);
            ds.setMinimumIdle(2);
            // Set driver based on URL
            if (effectiveUrl.contains("postgresql")) ds.setDriverClassName("org.postgresql.Driver");
            else if (effectiveUrl.contains("h2")) ds.setDriverClassName("org.h2.Driver");
            System.out.println("[Railway DB Config] DataSource configured - URL: " + maskPassword(effectiveUrl) + ", User: " + effectiveUsername);
            return ds;
        }
        String springUrl = env != null ? env.getProperty("spring.datasource.url") : null;
        if (springUrl != null && springUrl.startsWith("jdbc:")) {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(springUrl);
            String user = env.getProperty("spring.datasource.username");
            String pass = env.getProperty("spring.datasource.password");
            if (user != null) ds.setUsername(user);
            if (pass != null) ds.setPassword(pass);
            ds.setMaximumPoolSize(poolSize);
            ds.setMinimumIdle(2);
            if (springUrl.contains("postgresql")) ds.setDriverClassName("org.postgresql.Driver");
            else if (springUrl.contains("h2")) ds.setDriverClassName("org.h2.Driver");
            return ds;
        }
        String fallbackUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/library");
        String fallbackUser = env.getProperty("spring.datasource.username", "library");
        String fallbackPass = env.getProperty("spring.datasource.password", "");
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(fallbackUrl);
        ds.setUsername(fallbackUser);
        ds.setPassword(fallbackPass);
        ds.setMaximumPoolSize(poolSize);
        ds.setMinimumIdle(2);
        if (fallbackUrl.contains("postgresql")) ds.setDriverClassName("org.postgresql.Driver");
        else if (fallbackUrl.contains("h2")) ds.setDriverClassName("org.h2.Driver");
        return ds;
    }
    private String convertToJdbc(String postgresUrl) {
        try {
            URI uri = new URI(postgresUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();
            if (host == null) return manualConvert(postgresUrl);
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
            jdbc.append(host);
            if (port != -1) jdbc.append(":").append(port);
            if (path != null && !path.isBlank()) jdbc.append(path); else jdbc.append("/postgres");
            if (query != null && !query.isBlank()) { jdbc.append("?").append(query); if (!query.contains("sslmode")) jdbc.append("&sslmode=require"); }
            else jdbc.append("?sslmode=require");
            return jdbc.toString();
        } catch (Exception e) { return manualConvert(postgresUrl); }
    }
    private String manualConvert(String url) {
        try {
            String withoutProtocol = url.replaceFirst("^postgres(ql)?://", "");
            int atIndex = withoutProtocol.lastIndexOf('@');
            String hostAndDb = atIndex != -1 ? withoutProtocol.substring(atIndex + 1) : withoutProtocol;
            int slashIndex = hostAndDb.indexOf('/');
            String hostPort; String dbAndParams;
            if (slashIndex != -1) { hostPort = hostAndDb.substring(0, slashIndex); dbAndParams = hostAndDb.substring(slashIndex); }
            else { hostPort = hostAndDb; dbAndParams = "/postgres"; }
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
            jdbc.append(hostPort).append(dbAndParams);
            if (!dbAndParams.contains("sslmode")) jdbc.append(dbAndParams.contains("?") ? "&sslmode=require" : "?sslmode=require");
            return jdbc.toString();
        } catch (Exception e) { return url.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://"); }
    }
    private Credentials extractCredentials(String url) {
        try {
            URI uri = new URI(url);
            String userInfo = uri.getUserInfo();
            if (userInfo == null) {
                String withoutProtocol = url.replaceFirst("^postgres(ql)?://", "");
                int atIndex = withoutProtocol.lastIndexOf('@');
                if (atIndex != -1) userInfo = withoutProtocol.substring(0, atIndex);
            }
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                String username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String password = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : null;
                return new Credentials(username, password);
            }
        } catch (Exception e) {}
        return null;
    }
    private String maskPassword(String url) {
        if (url == null) return null;
        return url.replaceAll("password=[^&]*", "password=***").replaceAll("://[^:]+:[^@]+@", "://***:***@");
    }
    private static class Credentials {
        String username; String password;
        Credentials(String u, String p) { username = u; password = p; }
    }
}
