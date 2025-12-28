package com.praktikum.database.testing.library.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseConfig {
    private static final Logger logger = Logger.getLogger(DatabaseConfig.class.getName());
    private static Properties properties = new Properties();
    private static String DB_URL;
    private static String DB_USERNAME;
    private static String DB_PASSWORD;
    private static String DB_DRIVER;

    static {
        loadProperties();
        testConnection();
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("Error: File database.properties tidak ditemukan di classpath");
            }
            properties.load(input);
            DB_URL = properties.getProperty("db.url");
            DB_USERNAME = properties.getProperty("db.username");
            DB_PASSWORD = properties.getProperty("db.password");
            DB_DRIVER = properties.getProperty("db.driver");
            validateConfiguration();
            Class.forName(DB_DRIVER);
            logger.info("Database configuration berhasil di-load");
        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Error: Gagal load database configuration: " + e.getMessage());
            throw new RuntimeException("Error konfigurasi database", e);
        }
    }

    private static void validateConfiguration() {
        if (DB_URL == null || DB_URL.trim().isEmpty()) {
            throw new RuntimeException("Database URL harus diisi");
        }
        if (DB_USERNAME == null || DB_USERNAME.trim().isEmpty()) {
            throw new RuntimeException("Database username harus diisi");
        }
        if (DB_PASSWORD == null || DB_PASSWORD.trim().isEmpty()) {
            throw new RuntimeException("Database password harus diisi");
        }
        if (DB_DRIVER == null || DB_DRIVER.trim().isEmpty()) {
            throw new RuntimeException("Database driver harus diisi");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                logger.fine("Koneksi database ditutup");
            } catch (SQLException e) {
                logger.warning("Gagal menutup koneksi database: " + e.getMessage());
            }
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn != null && !conn.isClosed();
            if (isValid) {
                logger.info("Test koneksi database: BERHASIL");
            } else {
                logger.severe("Test koneksi database: GAGAL - Koneksi null atau closed");
            }
            return isValid;
        } catch (SQLException e) {
            logger.severe("Test koneksi database: GAGAL - " + e.getMessage());
            return false;
        }
    }

    public static void printDatabaseInfo() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            logger.info("Informasi Database:");
            logger.info("Product: " + metaData.getDatabaseProductName());
            logger.info("Version: " + metaData.getDatabaseProductVersion());
            logger.info("URL: " + metaData.getURL());
            logger.info("User: " + metaData.getUserName());
            logger.info("Driver: " + metaData.getDriverName() + " " + metaData.getDriverVersion());
        } catch (SQLException e) {
            logger.warning("Gagal mendapatkan info database: " + e.getMessage());
        }
    }

    public static String getDbUrl() { return DB_URL; }
    public static String getDbUsername() { return DB_USERNAME; }
}