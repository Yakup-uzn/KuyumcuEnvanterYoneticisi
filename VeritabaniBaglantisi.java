import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class VeritabaniBaglantisi {
    private Connection baglanti;

    public VeritabaniBaglantisi() {
        try {
            // Veritabanı bağlantısını başlat
            String url = "jdbc:mysql://localhost:3306/dunaf_kuyumculuk"; // Veritabanı URL'si
            String kullanici = "root"; // Veritabanı kullanıcı adı
            String parola = "1234"; // Veritabanı parola

            baglanti = DriverManager.getConnection(url, kullanici, parola);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet get(String sorgu) {
        try {
            Statement statement = baglanti.createStatement();
            return statement.executeQuery(sorgu);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int executeUpdate(String sorgu) {
        try {
            Statement statement = baglanti.createStatement();
            return statement.executeUpdate(sorgu);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Connection getConnection() {
        return baglanti;
    }

    public void close() {
        try {
            if (baglanti != null && !baglanti.isClosed()) {
                baglanti.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
