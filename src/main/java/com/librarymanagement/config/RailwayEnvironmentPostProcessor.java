package com.librarymanagement.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class RailwayEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> railwayProps = new HashMap<>();
        String jdbcUrl = resolveJdbcUrl(environment);
        if (jdbcUrl != null) {
            railwayProps.put("spring.datasource.url", jdbcUrl);
            System.out.println("[Railway] Resolved JDBC URL from DATABASE_URL");
            Credentials creds = extractCredentials(environment);
            if (creds != null) {
                if (creds.username != null && !hasProperty(environment, "spring.datasource.username", "DATABASE_USERNAME")) {
                    railwayProps.put("spring.datasource.username", creds.username);
                }
                if (creds.password != null && !hasProperty(environment, "spring.datasource.password", "DATABASE_PASSWORD")) {
                    railwayProps.put("spring.datasource.password", creds.password);
                }
            }
        }
        if (!hasProperty(environment, "app.base-url", "APP_BASE_URL")) {
            String publicDomain = getEnv(environment, "RAILWAY_PUBLIC_DOMAIN");
            if (publicDomain != null && !publicDomain.isBlank()) {
                String baseUrl = publicDomain.startsWith("http") ? publicDomain : "https://" + publicDomain;
                railwayProps.put("app.base-url", baseUrl);
                System.out.println("[Railway] Set app.base-url to " + baseUrl + " from RAILWAY_PUBLIC_DOMAIN");
            }
        }
        if (!hasProperty(environment, "app.mail-enabled", "MAIL_ENABLED")) {
            String mailHost = getEnv(environment, "MAIL_HOST");
            if (mailHost == null || mailHost.isBlank()) {
                railwayProps.put("app.mail-enabled", "false");
                railwayProps.put("spring.mail.host", "localhost");
                railwayProps.put("management.health.mail.enabled", "false");
            }
        }
        String port = getEnv(environment, "PORT");
        if (port != null && !port.isBlank()) {
            railwayProps.put("server.port", port);
        }
        if (!railwayProps.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("railway", railwayProps));
        }
    }

    private String resolveJdbcUrl(ConfigurableEnvironment environment) {
        String jdbcDirect = getEnv(environment, "JDBC_DATABASE_URL");
        if (jdbcDirect != null && !jdbcDirect.isBlank()) return jdbcDirect;
        String databaseUrl = getEnv(environment, "DATABASE_URL");
        String privateUrl = getEnv(environment, "DATABASE_PRIVATE_URL");
        String candidate = privateUrl != null && !privateUrl.isBlank() ? privateUrl : databaseUrl;
        if (candidate == null || candidate.isBlank()) return null;
        if (candidate.startsWith("jdbc:")) return candidate;
        if (candidate.startsWith("postgres://") || candidate.startsWith("postgresql://")) return convertPostgresToJdbc(candidate);
        return null;
    }

    private String convertPostgresToJdbc(String postgresUrl) {
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
            if (query != null && !query.isBlank()) {
                jdbc.append("?").append(query);
                if (!query.contains("sslmode")) jdbc.append("&sslmode=require");
            } else jdbc.append("?sslmode=require");
            return jdbc.toString();
        } catch (Exception e) {
            return manualConvert(postgresUrl);
        }
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
        } catch (Exception e) { return null; }
    }

    private Credentials extractCredentials(ConfigurableEnvironment environment) {
        String databaseUrl = getEnv(environment, "DATABASE_URL");
        String privateUrl = getEnv(environment, "DATABASE_PRIVATE_URL");
        String candidate = privateUrl != null && !privateUrl.isBlank() ? privateUrl : databaseUrl;
        if (candidate == null || candidate.isBlank() || candidate.startsWith("jdbc:")) return null;
        try {
            URI uri = new URI(candidate);
            String userInfo = uri.getUserInfo();
            if (userInfo == null) {
                String withoutProtocol = candidate.replaceFirst("^postgres(ql)?://", "");
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

    private String getEnv(ConfigurableEnvironment environment, String key) {
        String value = environment.getProperty(key);
        if (value != null) return value;
        return System.getenv(key);
    }

    private boolean hasProperty(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String val = environment.getProperty(key);
            if (val != null && !val.isBlank()) return true;
            if (System.getenv(key) != null && !System.getenv(key).isBlank()) return true;
        }
        return false;
    }

    private static class Credentials {
        String username; String password;
        Credentials(String u, String p) { username = u; password = p; }
    }
}
