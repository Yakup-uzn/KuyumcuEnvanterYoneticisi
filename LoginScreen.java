import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginScreen extends JFrame {
    private VeritabaniBaglantisi dbConnection;

    public LoginScreen() {
        dbConnection = new VeritabaniBaglantisi();

        setTitle("Login");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Kullanıcı Adı:"));
        JTextField usernameField = new JTextField();
        panel.add(usernameField);
        panel.add(new JLabel("Şifre:"));
        JPasswordField passwordField = new JPasswordField();
        panel.add(passwordField);

        JButton loginButton = new JButton("Giriş");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (authenticateUser(username, password)) {
                    dispose();
                    openAdminPage();
                } else {
                    JOptionPane.showMessageDialog(LoginScreen.this, "Geçersiz kullanıcı adı veya şifre", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        panel.add(new JLabel());
        panel.add(loginButton);

        add(panel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean authenticateUser(String username, String password) {
        String query = "SELECT * FROM KULLANICILAR WHERE KULLANICI_ADI = '" + username + "' AND SIFRE = '" + password + "'";
        ResultSet rs = dbConnection.get(query);
        try {
            return rs != null && rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void openAdminPage() {
        AdminPage adminPage = new AdminPage();
        adminPage.setExtendedState(JFrame.MAXIMIZED_BOTH); // Tam ekran
        adminPage.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginScreen::new);
    }
}
