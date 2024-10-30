package tarif_application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/yazlab1_1"; // Veritabanı adıyla değiştirilmelidir
    private static final String USER = "root";  // MySQL kullanıcı adı
    private static final String PASSWORD = "12345678";  // MySQL şifreniz

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Veritabanına başarılı bir şekilde bağlandı!");
        } catch (SQLException e) {
            System.out.println("Bağlantı hatası: " + e.getMessage());
        }
        return conn;
    }
  
    
}