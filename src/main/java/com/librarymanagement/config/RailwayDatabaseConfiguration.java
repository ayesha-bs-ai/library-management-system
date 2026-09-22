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

/**
 * Railway-specific DataSource configuration.
 * This handles Railway's DATABASE_URL which is in postgres:// format
 * and converts it to jdbc:postgresql:// format.
 * 
 * This is a fallback for when EnvironmentPostProcessor doesn't work
 * in fat jar mode. It provides a DataSource bean directly.
 */
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
        
        // Determine the actual URL to use
        String effectiveUrl = null;
        String effectiveUsername = username;
        String effectivePassword = password;
        
        // Priority 1: JDBC_DATABASE_URL (direct JDBC URL)
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            effectiveUrl = jdbcUrl;
            System.out.println("[Railway DB Config] Using JDBC_DATABASE_URL");
        } 
        // Priority 2: DATABASE_PRIVATE_URL (Railway private networking)
        else if (privateUrl != null && !privateUrl.isBlank()) {
            if (privateUrl.startsWith("jdbc:")) {
                effectiveUrl = privateUrl;
                System.out.println("[Railway DB Config] Using DATABASE_PRIVATE_URL as JDBC");
            } else if (privateUrl.startsWith("postgres://") || privateUrl.startsWith("postgresql://")) {
                effectiveUrl = convertToJdbc(privateUrl);
                Credentials creds = extractCredentials(privateUrl);
                if (creds != null) {
                    if (effectiveUsername == null || effectiveUsername.isBlank()) effectiveUsername = creds.username;
                    if (effectivePassword == null || effectivePassword.isBlank()) effectivePassword = creds.password;
                }
                System.out.println("[Railway DB Config] Converted DATABASE_PRIVATE_URL to JDBC: " + maskPassword(effectiveUrl));
            }
        }
        // Priority 3: DATABASE_URL (Railway public URL or standard)
        else if (databaseUrl != null && !databaseUrl.isBlank()) {
            if (databaseUrl.startsWith("jdbc:")) {
                effectiveUrl = databaseUrl;
                System.out.println("[Railway DB Config] Using DATABASE_URL as JDBC");
            } else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                effectiveUrl = convertToJdbc(databaseUrl);
                Credentials creds = extractCredentials(databaseUrl);
                if (creds != null) {
                    if (effectiveUsername == null || effectiveUsername.isBlank()) effectiveUsername = creds.username;
                    if (effectivePassword == null || effectivePassword.isBlank()) effectivePassword = creds.password;
                }
                System.out.println("[Railway DB Config] Converted DATABASE_URL to JDBC: " + maskPassword(effectiveUrl));
            }
        }
        
        // If we have a converted URL, build DataSource
        if (effectiveUrl != null) {
            // Also try to get username/password from env if not yet set
            if ((effectiveUsername == null || effectiveUsername.isBlank()) && env != null) {
                String envUser = env.getProperty("PGUSER");
                if (envUser != null && !envUser.isBlank()) effectiveUsername = envUser;
                else {
                    envUser = env.getProperty("DATABASE_USERNAME");
                    if (envUser != null && !envUser.isBlank()) effectiveUsername = envUser;
                }
            }
            if ((effectivePassword == null || effectivePassword.isBlank()) && env != null) {
                String envPass = env.getProperty("PGPASSWORD");
                if (envPass != null && !envPass.isBlank()) effectivePassword = envPass;
                else {
                    envPass = env.getProperty("DATABASE_PASSWORD");
                    if (envPass != null && !envPass.isBlank()) effectivePassword = envPass;
                }
            }
            
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(effectiveUrl);
            if (effectiveUsername != null && !effectiveUsername.isBlank()) {
                ds.setUsername(effectiveUsername);
            }
            if (effectivePassword != null && !effectivePassword.isBlank()) {
                ds.setPassword(effectivePassword);
            }
            ds.setMaximumPoolSize(poolSize);
            ds.setMinimumIdle(2);
            ds.setDriverClassName("org.postgresql.Driver");
            
            System.out.println("[Railway DB Config] DataSource configured - URL: " + maskPassword(effectiveUrl) + 
                             ", User: " + effectiveUsername + ", Pool: " + poolSize);
            
            return ds;
        }
        
        // Fallback: Let Spring Boot auto-configure from properties
        String springUrl = env != null ? env.getProperty("spring.datasource.url") : null;
        if (springUrl != null && springUrl.startsWith("jdbc:")) {
            System.out.println("[Railway DB Config] Using spring.datasource.url: " + maskPassword(springUrl));
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(springUrl);
            String user = env.getProperty("spring.datasource.username");
            String pass = env.getProperty("spring.datasource.password");
            if (user != null) ds.setUsername(user);
            if (pass != null) ds.setPassword(pass);
            ds.setMaximumPoolSize(poolSize);
            ds.setMinimumIdle(2);
            return ds;
        }
        
        System.out.println("[Railway DB Config] No Railway DB URL found, falling back to default configuration");
        String fallbackUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/library");
        String fallbackUser = env.getProperty("spring.datasource.username", "library");
        String fallbackPass = env.getProperty("spring.datasource.password", "");
        
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(fallbackUrl);
        ds.setUsername(fallbackUser);
        ds.setPassword(fallbackPass);
        ds.setMaximumPoolSize(poolSize);
        ds.setMinimumIdle(2);
        return ds;
    }
    
    private String convertToJdbc(String postgresUrl) {
        try {
            URI uri = new URI(postgresUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();
            
            if (host == null) {
                return manualConvert(postgresUrl);
            }
            
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
            jdbc.append(host);
            if (port != -1) {
                jdbc.append(":").append(port);
            }
            if (path != null && !path.isBlank()) {
                jdbc.append(path);
            } else {
                jdbc.append("/postgres");
            }
            
            if (query != null && !query.isBlank()) {
                jdbc.append("?").append(query);
                if (!query.contains("sslmode")) {
                    jdbc.append("&sslmode=require");
                }
            } else {
                jdbc.append("?sslmode=require");
            }
            
            return jdbc.toString();
        } catch (Exception e) {
            System.out.println("[Railway DB Config] URI parsing failed, using manual conversion: " + e.getMessage());
            return manualConvert(postgresUrl);
        }
    }
    
    private String manualConvert(String url) {
        try {
            String withoutProtocol = url.replaceFirst("^postgres(ql)?://", "");
            int atIndex = withoutProtocol.lastIndexOf('@');
            String hostAndDb = atIndex != -1 ? withoutProtocol.substring(atIndex + 1) : withoutProtocol;
            
            int slashIndex = hostAndDb.indexOf('/');
            String hostPort;
            String dbAndParams;
            if (slashIndex != -1) {
                hostPort = hostAndDb.substring(0, slashIndex);
                dbAndParams = hostAndDb.substring(slashIndex);
            } else {
                hostPort = hostAndDb;
                dbAndParams = "/postgres";
            }
            
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
            jdbc.append(hostPort).append(dbAndParams);
            if (!dbAndParams.contains("sslmode")) {
                jdbc.append(dbAndParams.contains("?") ? "&sslmode=require" : "?sslmode=require");
            }
            return jdbc.toString();
        } catch (Exception e) {
            System.out.println("[Railway DB Config] Manual conversion failed: " + e.getMessage());
            return url.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
        }
    }
    
    private Credentials extractCredentials(String url) {
        try {
            URI uri = new URI(url);
            String userInfo = uri.getUserInfo();
            if (userInfo == null) {
                String withoutProtocol = url.replaceFirst("^postgres(ql)?://", "");
                int atIndex = withoutProtocol.lastIndexOf('@');
                if (atIndex != -1) {
                    userInfo = withoutProtocol.substring(0, atIndex);
                }
            }
            
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                String username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String password = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : null;
                return new Credentials(username, password);
            }
        } catch (Exception e) {
            System.out.println("[Railway DB Config] Failed to extract credentials: " + e.getMessage());
        }
        return null;
    }
    
    private String maskPassword(String url) {
        if (url == null) return null;
        return url.replaceAll("password=[^&]*", "password=***")
                  .replaceAll("://[^:]+:[^@]+@", "://***:***@");
    }
    
    private static class Credentials {
        String username;
        String password;
        Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
