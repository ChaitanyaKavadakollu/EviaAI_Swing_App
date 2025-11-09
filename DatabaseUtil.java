import java.sql.*;

public class DatabaseUtil {
    private static final String DB_NAME = "eviaai_db";
    // MySQL config (if available)
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/" + DB_NAME + "?serverTimezone=UTC";
    private static final String MYSQL_SERVER_URL = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
    private static final String MYSQL_USERNAME = "root";
    private static final String MYSQL_PASSWORD = "root";

    // H2 embedded fallback (file-based database in project folder)
    private static final String H2_URL = "jdbc:h2:./eviaai_h2;AUTO_SERVER=TRUE";
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "";

    /**
     * Get a connection to the application database. Strategy:
     * 1) Try MySQL if its driver is available and reachable. If DB missing, create it.
     * 2) If MySQL isn't available or fails, try H2 embedded DB (requires H2 jar on classpath).
     * 3) If neither driver is available, throw a helpful SQLException.
     */
    public static Connection getConnection() throws SQLException {
        // Allow forcing the DB backend via system property or environment variable.
        // Usage: java -Devia.db=h2 EviaAI_Swing_App  OR set EVIA_DB=h2 in env.
        String forced = System.getProperty("evia.db");
        if (forced == null) forced = System.getenv("EVIA_DB");
        if (forced != null) {
            if (forced.equalsIgnoreCase("h2")) {
                try {
                    Class.forName("org.h2.Driver");
                    return tryH2();
                } catch (ClassNotFoundException e) {
                    throw new SQLException("H2 driver forced but org.h2.Driver not found on classpath.", e);
                }
            } else if (forced.equalsIgnoreCase("mysql")) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    return tryMySql();
                } catch (ClassNotFoundException e) {
                    throw new SQLException("MySQL driver forced but com.mysql.cj.jdbc.Driver not found on classpath.", e);
                }
            }
            // If forced value unrecognized, fall through to default detection below.
        }
        // Try MySQL first
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                return tryMySql();
            } catch (SQLException mysqlEx) {
                System.err.println("MySQL available but connection failed: " + mysqlEx.getMessage());
                // Fall through to H2 attempt
            }
        } catch (ClassNotFoundException ignore) {
            // MySQL driver not present — will try H2 next
        }

        // Try H2 embedded
        try {
            Class.forName("org.h2.Driver");
            return tryH2();
        } catch (ClassNotFoundException e) {
            String msg = "Neither MySQL (com.mysql.cj.jdbc.Driver) nor H2 (org.h2.Driver) JDBC drivers were found.\n" +
                    "Install one of them and add it to the classpath.\n" +
                    "Recommended (quick): download H2 (https://www.h2database.com) and run:\n" +
                    "  javac -cp .;h2-<version>.jar *.java\n" +
                    "  java -cp .;h2-<version>.jar EviaAI_Swing_App\n" +
                    "Or install MySQL Connector/J and run with its jar on the classpath. See README.md.";
            throw new SQLException(msg, e);
        }
    }

    private static Connection tryMySql() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USERNAME, MYSQL_PASSWORD);
            return conn;
        } catch (SQLException e) {
            int errorCode = e.getErrorCode();
            String sqlState = e.getSQLState();
            if (errorCode == 1049 || (sqlState != null && sqlState.equals("42000"))) {
                // DB doesn't exist — create it and tables
                try (Connection conn = DriverManager.getConnection(MYSQL_SERVER_URL, MYSQL_USERNAME, MYSQL_PASSWORD);
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
                    stmt.executeUpdate("USE " + DB_NAME);
                    stmt.executeUpdate(
                            "CREATE TABLE IF NOT EXISTS users (" +
                                    "id VARCHAR(100) PRIMARY KEY, " +
                                    "password VARCHAR(100) NOT NULL, " +
                                    "credits INT DEFAULT 0" +
                                    ")"
                    );
                    stmt.executeUpdate(
                            "CREATE TABLE IF NOT EXISTS items (" +
                                    "name VARCHAR(100) PRIMARY KEY, " +
                                    "raw_materials TEXT NOT NULL, " +
                                    "how_to_make TEXT NOT NULL, " +
                                    "how_to_use TEXT NOT NULL, " +
                                    "where_to_use TEXT NOT NULL, " +
                                    "contributor VARCHAR(100) NOT NULL, " +
                                    "credits INT DEFAULT 0, " +
                                    "FOREIGN KEY (contributor) REFERENCES users(id)" +
                                    ")"
                    );
                }
                // retry
                return DriverManager.getConnection(MYSQL_URL, MYSQL_USERNAME, MYSQL_PASSWORD);
            }
            throw e;
        }
    }

    private static Connection tryH2() throws SQLException {
        Connection conn = DriverManager.getConnection(H2_URL, H2_USERNAME, H2_PASSWORD);
        // Create tables if missing
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "id VARCHAR(100) PRIMARY KEY, " +
                            "password VARCHAR(100) NOT NULL, " +
                            "credits INT DEFAULT 0" +
                            ")"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS items (" +
                            "name VARCHAR(100) PRIMARY KEY, " +
                            "raw_materials CLOB NOT NULL, " +
                            "how_to_make CLOB NOT NULL, " +
                            "how_to_use CLOB NOT NULL, " +
                            "where_to_use CLOB NOT NULL, " +
                            "contributor VARCHAR(100) NOT NULL, " +
                            "credits INT DEFAULT 0, " +
                            "FOREIGN KEY (contributor) REFERENCES users(id)" +
                            ")"
            );
        }
        return conn;
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}