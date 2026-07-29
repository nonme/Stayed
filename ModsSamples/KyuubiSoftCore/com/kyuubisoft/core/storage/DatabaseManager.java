/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft DB");
    private HikariDataSource dataSource;

    public void initialize(String host, int port, String database, String username, String password, int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000L);
        config.setIdleTimeout(300000L);
        config.setMaxLifetime(600000L);
        config.setPoolName("KyuubiSoft-DB");
        this.dataSource = new HikariDataSource(config);
        LOGGER.info("HikariCP pool initialized: " + host + ":" + port + "/" + database + " (maxPool=" + maxPoolSize + ")");
    }

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null || this.dataSource.isClosed()) {
            throw new SQLException("DataSource is not available");
        }
        return this.dataSource.getConnection();
    }

    public boolean isConnected() {
        return this.dataSource != null && !this.dataSource.isClosed();
    }

    public void shutdown() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
            LOGGER.info("HikariCP pool closed");
        }
    }
}

