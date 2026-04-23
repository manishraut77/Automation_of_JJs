import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnect {
    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("MYSQL_HOST", "localhost");
        String port = System.getenv().getOrDefault("MYSQL_PORT", "3306");
        String database = System.getenv().getOrDefault("MYSQL_DB", "jjs_automation");
        String user = System.getenv().getOrDefault("MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("MYSQL_PASSWORD", "your_password");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?serverTimezone=America/New_York";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            if (conn != null) {
                System.out.println("Connected to the database!");
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
