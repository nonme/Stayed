/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.storage;

import com.kyuubisoft.core.storage.DatabaseManager;
import com.kyuubisoft.core.storage.PlayerDataStorage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;

public class MySQLPlayerDataStorage
implements PlayerDataStorage {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Storage");
    private final DatabaseManager db;
    private final String tablePrefix;

    public MySQLPlayerDataStorage(DatabaseManager db, String tablePrefix) {
        this.db = db;
        this.tablePrefix = tablePrefix != null ? tablePrefix : "ks_";
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public String loadJson(String tableName, UUID playerId) {
        String fullName = this.tablePrefix + tableName;
        String sql = "SELECT data FROM " + fullName + " WHERE uuid = ?";
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);){
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return null;
                String string = rs.getString("data");
                return string;
            }
        }
        catch (SQLException e) {
            LOGGER.warning("MySQL loadJson failed for " + String.valueOf(playerId) + " in " + fullName + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void saveJson(String tableName, UUID playerId, String username, String json) {
        String fullName = this.tablePrefix + tableName;
        String sql = "INSERT INTO " + fullName + " (uuid, username, data) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE username = VALUES(username), data = VALUES(data)";
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);){
            ps.setString(1, playerId.toString());
            ps.setString(2, username);
            ps.setString(3, json);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            LOGGER.warning("MySQL saveJson failed for " + String.valueOf(playerId) + " in " + fullName + ": " + e.getMessage());
        }
    }

    @Override
    public void delete(String tableName, UUID playerId) {
        String fullName = this.tablePrefix + tableName;
        String sql = "DELETE FROM " + fullName + " WHERE uuid = ?";
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);){
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            LOGGER.warning("MySQL delete failed for " + String.valueOf(playerId) + " in " + fullName + ": " + e.getMessage());
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean exists(String tableName, UUID playerId) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public void ensureTableExists(String tableName) {
        String fullName = this.tablePrefix + tableName;
        String sql = "CREATE TABLE IF NOT EXISTS " + fullName + " (uuid CHAR(36) PRIMARY KEY, username VARCHAR(32) NOT NULL, data MEDIUMTEXT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection conn = this.db.getConnection();
             Statement stmt = conn.createStatement();){
            stmt.executeUpdate(sql);
            LOGGER.info("Ensured table exists: " + fullName);
        }
        catch (SQLException e) {
            LOGGER.warning("Failed to create table " + fullName + ": " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String getTypeName() {
        return "mysql";
    }

    public DatabaseManager getDatabaseManager() {
        return this.db;
    }

    public String getTablePrefix() {
        return this.tablePrefix;
    }
}

