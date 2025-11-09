import java.sql.*;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EviaAIDAO {
    private static volatile String lastError = null;

    public static String getLastError() {
        return lastError;
    }

    private static void setLastError(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        lastError = sw.toString();
    }

    /**
     * Writes lastError to a fixed file 'evia_last_error.log' (overwrites) for easy
     * debugging when the UI or automated systems can't capture timestamped logs.
     */
    private static void writeLastErrorToFixedFile() {
        if (lastError == null) return;
        try (FileWriter fw = new FileWriter("evia_last_error.log")) {
            fw.write(lastError);
            fw.flush();
        } catch (IOException ioe) {
            // If writing fails, print to stderr as a fallback
            ioe.printStackTrace();
        }
    }

    /**
     * Dump the last captured error stack trace to a timestamped log file in the
     * current working directory. Returns the filename written or null on failure.
     */
    public static String dumpLastErrorToFile() {
        if (lastError == null) return null;
        String filename = "evia_last_error_" + System.currentTimeMillis() + ".log";
        FileWriter fw = null;
        try {
            fw = new FileWriter(filename);
            fw.write(lastError);
            fw.flush();
            return filename;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        } finally {
            if (fw != null) {
                try { fw.close(); } catch (IOException ignored) {}
            }
        }
    }

    
    public User getUser(String id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            stmt.setString(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                User user = new User(rs.getString("id"), rs.getString("password"));
                user.credits = rs.getInt("credits");
                loadUserContributions(user);
                return user;
            }
            return null;
        } catch (SQLException e) {
            setLastError(e);
            e.printStackTrace();
            return null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    private void loadUserContributions(User user) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT name FROM items WHERE contributor = ?");
            stmt.setString(1, user.id);
            rs = stmt.executeQuery();

            while (rs.next()) {
                user.contributedItems.add(rs.getString("name"));
            }
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    public boolean saveUser(User user) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();

            // Try update first (portable across DBs)
            stmt = conn.prepareStatement("UPDATE users SET password = ?, credits = ? WHERE id = ?");
            stmt.setString(1, user.password);
            stmt.setInt(2, user.credits);
            stmt.setString(3, user.id);
            int updated = stmt.executeUpdate();
            closeStatement(stmt);

            if (updated == 0) {
                stmt = conn.prepareStatement("INSERT INTO users (id, password, credits) VALUES (?, ?, ?)");
                stmt.setString(1, user.id);
                stmt.setString(2, user.password);
                stmt.setInt(3, user.credits);
                stmt.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            setLastError(e);
            e.printStackTrace();
            writeLastErrorToFixedFile();
            return false;
        } finally {
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    public ItemInfo getItem(String name) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM items WHERE name = ?");
            stmt.setString(1, name);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new ItemInfo(
                    rs.getString("name"),
                    rs.getString("raw_materials"),
                    rs.getString("how_to_make"),
                    rs.getString("how_to_use"),
                    rs.getString("where_to_use"),
                    rs.getString("contributor")
                );
            }
            return null;
        } catch (SQLException e) {
            setLastError(e);
            e.printStackTrace();
            return null;
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    public boolean saveItem(ItemInfo item) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getConnection();

            // Try update first
            stmt = conn.prepareStatement(
                "UPDATE items SET raw_materials = ?, how_to_make = ?, how_to_use = ?, where_to_use = ?, contributor = ?, credits = ? WHERE name = ?"
            );
            stmt.setString(1, item.rawMaterials);
            stmt.setString(2, item.howToMake);
            stmt.setString(3, item.howToUse);
            stmt.setString(4, item.whereToUse);
            stmt.setString(5, item.contributor);
            stmt.setInt(6, item.credits);
            stmt.setString(7, item.name);
            int updated = stmt.executeUpdate();
            closeStatement(stmt);

            if (updated == 0) {
                stmt = conn.prepareStatement(
                    "INSERT INTO items (name, raw_materials, how_to_make, how_to_use, where_to_use, contributor, credits) VALUES (?, ?, ?, ?, ?, ?, ?)"
                );
                stmt.setString(1, item.name);
                stmt.setString(2, item.rawMaterials);
                stmt.setString(3, item.howToMake);
                stmt.setString(4, item.howToUse);
                stmt.setString(5, item.whereToUse);
                stmt.setString(6, item.contributor);
                stmt.setInt(7, item.credits);
                stmt.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            setLastError(e);
            e.printStackTrace();
            writeLastErrorToFixedFile();
            return false;
        } finally {
            closeStatement(stmt);
            closeConnection(conn);
        }
    }
    
    public List<User> getTopUsers() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<User> topUsers = new ArrayList<>();
        
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users ORDER BY credits DESC LIMIT 10");
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                User user = new User(rs.getString("id"), "");
                user.credits = rs.getInt("credits");
                loadUserContributions(user);
                topUsers.add(user);
            }
        } catch (SQLException e) {
            setLastError(e);
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
        
        return topUsers;
    }

    // ---- Embedded JDBC connectivity helpers (used instead of DatabaseUtil) ----
    // Configuration defaults (match DatabaseUtil defaults)
    private static final String DB_NAME = "eviaai_db";
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/" + DB_NAME + "?serverTimezone=UTC";
    private static final String MYSQL_SERVER_URL = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
    private static final String MYSQL_USERNAME = "root";
    private static final String MYSQL_PASSWORD = "root";

    private static final String H2_URL = "jdbc:h2:./eviaai_h2;AUTO_SERVER=TRUE";
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "";

    private static Connection getConnection() throws SQLException {
        // Allow forcing via system property or env var
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
        }

        // Try MySQL first
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try {
                return tryMySql();
            } catch (SQLException mysqlEx) {
                System.err.println("MySQL available but connection failed: " + mysqlEx.getMessage());
                // fall through to H2
            }
        } catch (ClassNotFoundException ignore) {
            // MySQL driver not present
        }

        // Try H2
        try {
            Class.forName("org.h2.Driver");
            return tryH2();
        } catch (ClassNotFoundException e) {
            // Attempt to auto-load local JDBC jars (if user placed them in the project folder but forgot to add to classpath)
            boolean loaded = false;
            try {
                // Try to load a matching H2 jar from the current directory
                loaded = tryLoadDriverFromJar("org.h2.Driver", "h2-*.jar");
                if (loaded) {
                    // If we loaded the driver class from a jar, try again
                    try {
                        Class.forName("org.h2.Driver");
                        return tryH2();
                    } catch (ClassNotFoundException ignore) {}
                }

                // Try to load a matching MySQL connector jar from the current directory
                loaded = tryLoadDriverFromJar("com.mysql.cj.jdbc.Driver", "mysql-connector-java-*.jar");
                if (loaded) {
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        try {
                            return tryMySql();
                        } catch (SQLException mysqlEx) {
                            System.err.println("MySQL available but connection failed after loading jar: " + mysqlEx.getMessage());
                        }
                    } catch (ClassNotFoundException ignore) {}
                }
            } catch (Throwable t) {
                // swallow - we'll write diagnostics below
            }

            // Provide clearer diagnostics in the logged error
            Exception detailed = new SQLException("Neither MySQL nor H2 JDBC drivers were found on the classpath."
                    + "\njava.class.path=" + System.getProperty("java.class.path")
                    + "\nWorkingDirectoryFiles=" + listWorkingDirectoryFiles(), e);
            setLastError(detailed);
            writeLastErrorToFixedFile();
            throw new SQLException("Neither MySQL nor H2 JDBC drivers were found on the classpath. See evia_last_error.log for details.", detailed);
        }
    }

    /**
     * Try to locate a jar in the current working directory matching the provided glob and load the driver class
     * using a URLClassLoader. Returns true if the driver class could be loaded from any matched jar.
     */
    private static boolean tryLoadDriverFromJar(String driverClass, String jarGlob) {
        try {
            Path cwd = Paths.get(System.getProperty("user.dir"));
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(cwd, jarGlob)) {
                for (Path p : ds) {
                    try {
                        URL url = p.toUri().toURL();
                        try (URLClassLoader loader = new URLClassLoader(new URL[] { url }, EviaAIDAO.class.getClassLoader())) {
                            try {
                                Class.forName(driverClass, true, loader);
                                return true;
                            } catch (ClassNotFoundException cnf) {
                                // continue to next jar
                            }
                        }
                    } catch (Throwable ignore) {
                        // ignore and try next
                    }
                }
            }
        } catch (Throwable t) {
            // ignore any IO errors
        }
        return false;
    }

    private static String listWorkingDirectoryFiles() {
        try {
            Path cwd = Paths.get(System.getProperty("user.dir"));
            StringBuilder sb = new StringBuilder();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(cwd)) {
                for (Path p : ds) {
                    sb.append(p.getFileName().toString()).append(',');
                }
            }
            return sb.toString();
        } catch (Throwable t) {
            return "<unable to list working directory: " + t.getMessage() + ">";
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
                // DB doesn't exist - create it
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
                return DriverManager.getConnection(MYSQL_URL, MYSQL_USERNAME, MYSQL_PASSWORD);
            }
            throw e;
        }
    }

    private static Connection tryH2() throws SQLException {
        Connection conn = DriverManager.getConnection(H2_URL, H2_USERNAME, H2_PASSWORD);
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

    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    private static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
    }
}