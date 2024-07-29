import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;

public class Backend {
    private VeritabaniBaglantisi veritabaniBaglantisi;

    public Backend(VeritabaniBaglantisi veritabaniBaglantisi) {
        this.veritabaniBaglantisi = veritabaniBaglantisi; // Veritabanı bağlantısını al.
    }

    public List<Object[]> tabloVerileriniGetir(String sorgu) {
        ResultSet resultSet = veritabaniBaglantisi.get(sorgu);
        List<Object[]> sonuclar = new ArrayList<>();

        try {
            if (resultSet != null) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int sutunSayisi = metaData.getColumnCount();

                while (resultSet.next()) {
                    Object[] satir = new Object[sutunSayisi + 1];
                    satir[0] = false;
                    for (int i = 1; i <= sutunSayisi; i++) {
                        satir[i] = resultSet.getObject(i);
                    }
                    sonuclar.add(satir);
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sonuclar;
    }

    public String[] sutunAdlariniGetir(String sorgu) {
        ResultSet resultSet = veritabaniBaglantisi.get(sorgu);
        String[] sutunAdlari = {};

        try {
            if (resultSet != null) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int sutunSayisi = metaData.getColumnCount();

                sutunAdlari = new String[sutunSayisi + 1];
                sutunAdlari[0] = "Seç";
                for (int i = 1; i <= sutunSayisi; i++) {
                    sutunAdlari[i] = metaData.getColumnName(i);
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sutunAdlari;
    }

    public String[] formSutunAdlariniGetir(String sorgu) {
        ResultSet resultSet = veritabaniBaglantisi.get(sorgu);
        List<String> sutunListesi = new ArrayList<>();

        try {
            if (resultSet != null) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int sutunSayisi = metaData.getColumnCount();

                for (int i = 0; i < sutunSayisi; i++) {
                    String sutunAdi = metaData.getColumnName(i + 1);
                    if (!sutunAdi.equalsIgnoreCase("ID") && !sutunAdi.equalsIgnoreCase("FILE") && !sutunAdi.equalsIgnoreCase("DURUM")) {
                        sutunListesi.add(sutunAdi);
                    }
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sutunListesi.toArray(new String[0]);
    }

    public void kayitEkle(String sorgu, JTextField[] formAlanlari, JComboBox<Integer> ayarComboBox, String[] sutunAdlari, String mevcutTabloAdi) {
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu, Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            double altinMiktari = 0;
            int ayar = 0;
    
            for (int i = 0; i < sutunAdlari.length; i++) {
                if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("HESAP") && !sutunAdlari[i].equalsIgnoreCase("DURUM")) {
                    if (sutunAdlari[i].equalsIgnoreCase("AYAR")) {
                        ayar = ayarComboBox.getSelectedItem() != null ? (Integer) ayarComboBox.getSelectedItem() : 0;
                        preparedStatement.setInt(paramIndex++, ayar);
                    } else if (sutunAdlari[i].equalsIgnoreCase("Tarih")) {
                        String value = (formAlanlari[i] == null || formAlanlari[i].getText().trim().isEmpty())
                                ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                                : formAlanlari[i].getText().trim();
                        preparedStatement.setString(paramIndex++, value);
                    } else {
                        String value = formAlanlari[i] != null ? formAlanlari[i].getText().trim() : "0";
                        if (sutunAdlari[i].equalsIgnoreCase("ALTIN_MIKTARI")) {
                            altinMiktari = value.isEmpty() ? 0 : Double.parseDouble(value);
                        }
                        preparedStatement.setString(paramIndex++, value.isEmpty() ? "0" : value);
                    }
                }
            }
    
            preparedStatement.executeUpdate();
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int kayitId = generatedKeys.getInt(1);
                double katsayiHesap = getKatsayiFromKatsayilar(ayar);
                double hesap = altinMiktari * katsayiHesap;
                hesapKolonunuGuncelle(kayitId, hesap);
                fileHesaplaVeGuncelle(mevcutTabloAdi, kayitId);  // FILE değerini hesapla ve güncelle
            }
            JOptionPane.showMessageDialog(null, "Kayıt başarıyla eklendi!");
            Main.tabloyuVeFormuGuncelle(mevcutTabloAdi);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    private void fileHesaplaVeGuncelle(String tabloAdi, int kayitId) {
        String sorgu = "SELECT MIKTAR, CIKAN_MIKTAR FROM " + tabloAdi + " WHERE ID = " + kayitId;
        ResultSet resultSet = veritabaniBaglantisi.get(sorgu);

        try {
            if (resultSet != null && resultSet.next()) {
                Float miktar = resultSet.getFloat("MIKTAR");
                Float cikanMiktar = resultSet.getFloat("CIKAN_MIKTAR");
                Float file = miktar - cikanMiktar;

                String guncelleSorgu = "UPDATE " + tabloAdi + " SET FILE = ? WHERE ID = ?";
                try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(guncelleSorgu)) {
                    preparedStatement.setFloat(1, file);
                    preparedStatement.setInt(2, kayitId);
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public void kayitSil(String tabloAdi, int kayitId) {
        String sorgu = "DELETE FROM " + tabloAdi + " WHERE ID = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, kayitId);
            preparedStatement.executeUpdate();
            JOptionPane.showMessageDialog(null, "Kayıt başarıyla silindi!");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public Object[] kayitVerileriniGetir(String tabloAdi, int kayitId) {
        String sorgu = "SELECT * FROM " + tabloAdi + " WHERE ID = " + kayitId;
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int sutunSayisi = metaData.getColumnCount();
            if (resultSet.next()) {
                Object[] veri = new Object[sutunSayisi];
                for (int i = 1; i <= sutunSayisi; i++) {
                    veri[i - 1] = resultSet.getObject(i);
                }
                return veri;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    
    

    public void kayitGuncelle(String tabloAdi, int kayitId, JTextField[] formAlanlari, JComboBox<Integer> ayarComboBox, String[] sutunAdlari) {
        StringBuilder sorguOlusturucu = new StringBuilder("UPDATE ");
        sorguOlusturucu.append(tabloAdi).append(" SET ");

        boolean ilk = true;
        for (int i = 0; i < sutunAdlari.length; i++) {
            if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("FILE") && !sutunAdlari[i].equalsIgnoreCase("HESAP")) {
                if (!ilk) {
                    sorguOlusturucu.append(", ");
                }
                sorguOlusturucu.append(sutunAdlari[i]).append(" = ?");
                ilk = false;
            }
        }
        sorguOlusturucu.append(" WHERE ID = ?");

        String sorgu = sorguOlusturucu.toString();
        System.out.println("Güncelleme Sorgusu: " + sorgu);

        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            int paramIndex = 1;
            for (int i = 0; i < sutunAdlari.length; i++) {
                if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("FILE") && !sutunAdlari[i].equalsIgnoreCase("HESAP")) {
                    if (sutunAdlari[i].equalsIgnoreCase("AYAR") && ayarComboBox != null) {
                        preparedStatement.setInt(paramIndex++, (Integer) ayarComboBox.getSelectedItem());
                    } else if (formAlanlari[i] != null) {
                        String value = formAlanlari[i].getText().trim();
                        if (value.isEmpty()) {
                            value = "0"; // Default to "0" if the field is empty
                        }
                        preparedStatement.setString(paramIndex++, value);
                    } else if (sutunAdlari[i].equalsIgnoreCase("TARIH")) {
                        // Handle the case for the TARIH column
                        String currentTime = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date());
                        preparedStatement.setString(paramIndex++, currentTime);
                    } else if (sutunAdlari[i].equalsIgnoreCase("kaynakTablo")) {
                        // Handle the kaynakTablo column
                        preparedStatement.setString(paramIndex++, tabloAdi); // Or any value you want to set for kaynakTablo
                    } else {
                        preparedStatement.setString(paramIndex++, "0"); // Default to "0" if no value is provided
                    }
                }
            }
            preparedStatement.setInt(paramIndex, kayitId);
            preparedStatement.executeUpdate();

            // Eğer tablo Dokum ise hesap değerini güncelle
            if (tabloAdi.equalsIgnoreCase("Dokum")) {
                dokumHesaplaVeGuncelle(kayitId);
            }
            fileHesaplaVeGuncelle(tabloAdi, kayitId);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void dokumHesaplaVeGuncelle(int kayitId) {
        String sorgu = "SELECT AYAR, ALTIN_MIKTARI FROM Dokum WHERE ID = " + kayitId;
        ResultSet resultSet = veritabaniBaglantisi.get(sorgu);
    
        try {
            if (resultSet != null && resultSet.next()) {
                int ayar = resultSet.getInt("AYAR");
                float altinMiktari = resultSet.getFloat("ALTIN_MIKTARI");
               
                // Hesaplama işlemi
                double katsayiHesap = getKatsayiFromKatsayilar(ayar);
                double hesap = altinMiktari * katsayiHesap;
                hesapKolonunuGuncelle(kayitId, hesap);
    
              
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    
   public void hedefTabloyaKayitEkle(String hedefTablo, JTextField[] formAlanlari, String[] sutunAdlari, String kaynakTablo, int kaynakKayitId, String urunAd) {
    StringBuilder sorguOlusturucu = new StringBuilder("INSERT INTO ");
    sorguOlusturucu.append(hedefTablo).append(" (");

    boolean ilk = true;
    for (int i = 0; i < sutunAdlari.length; i++) {
        if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("KaynakTablo") && !sutunAdlari[i].equalsIgnoreCase("FILE") && !sutunAdlari[i].equalsIgnoreCase("URUN_AD")) {
            if (!ilk) {
                sorguOlusturucu.append(", ");
            }
            sorguOlusturucu.append(sutunAdlari[i]);
            ilk = false;
        }
    }
    sorguOlusturucu.append(", KaynakTablo, URUN_AD) VALUES (");

    ilk = true;
    for (int i = 0; i < formAlanlari.length; i++) {
        if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("KaynakTablo") && !sutunAdlari[i].equalsIgnoreCase("FILE") && !sutunAdlari[i].equalsIgnoreCase("URUN_AD")) {
            if (!ilk) {
                sorguOlusturucu.append(", ");
            }
            sorguOlusturucu.append("?");
            ilk = false;
        }
    }
    sorguOlusturucu.append(", ?, ?)");

    String sorgu = sorguOlusturucu.toString();

    try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu, Statement.RETURN_GENERATED_KEYS)) {
        int paramIndex = 1;
        for (int i = 0; i < formAlanlari.length; i++) {
            if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("KaynakTablo") && !sutunAdlari[i].equalsIgnoreCase("FILE") && !sutunAdlari[i].equalsIgnoreCase("URUN_AD")) {
                if (sutunAdlari[i].equalsIgnoreCase("Tarih") && (formAlanlari[i] == null || formAlanlari[i].getText().trim().isEmpty())) {
                    // Set current timestamp if Tarih is empty
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                    preparedStatement.setString(paramIndex++, now.format(formatter));
                } else if (formAlanlari[i] != null) {
                    preparedStatement.setString(paramIndex++, formAlanlari[i].getText().trim());
                } else {
                    preparedStatement.setString(paramIndex++, "");  // or some default value
                }
            }
        }
        preparedStatement.setString(paramIndex++, kaynakTablo);
        preparedStatement.setString(paramIndex, urunAd);
        preparedStatement.executeUpdate();

        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
            int yeniKayitId = generatedKeys.getInt(1);
            islemGecmisiKaydet(kaynakTablo, kaynakKayitId, hedefTablo, yeniKayitId);
            if (hedefTablo.equalsIgnoreCase("Tezgah") || hedefTablo.equalsIgnoreCase("Patlatma") ||
                hedefTablo.equalsIgnoreCase("Cila") || hedefTablo.equalsIgnoreCase("Tambur")) {
                fileHesaplaVeGuncelle(hedefTablo, yeniKayitId);
            }
        }

        JOptionPane.showMessageDialog(null, "Kayıt başarıyla " + hedefTablo + " tablosuna taşındı!");
    } catch (SQLException ex) {
        ex.printStackTrace();
    }
}
    
    
    public String getUrunAdFromKaynakTablo(String kaynakTablo, int kaynakKayitId) {
        String urunAd = null;
        String sorgu = "SELECT URUN_AD FROM " + kaynakTablo + " WHERE ID = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, kaynakKayitId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                urunAd = resultSet.getString("URUN_AD");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return urunAd;
    }
    

    
    public void islemGecmisiKaydet(String kaynakTablo, int kaynakKayitId, String hedefTablo, int hedefKayitId) {
        String sorgu = "INSERT INTO IslemGecmisi (kaynakTablo, kaynakKayitId, hedefTablo, hedefKayitId, oncesiKayitId) VALUES (?, ?, ?, ?, ?)";
        
        int oncesiKayitId = getOncesiKayitId(kaynakTablo, kaynakKayitId);
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, kaynakTablo);
            preparedStatement.setInt(2, kaynakKayitId);
            preparedStatement.setString(3, hedefTablo);
            preparedStatement.setInt(4, hedefKayitId);
            preparedStatement.setInt(5, oncesiKayitId);
            preparedStatement.executeUpdate();
    
            // Eğer işlem başarılıysa, sonrakiKayitId güncelle
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int yeniKayitId = generatedKeys.getInt(1);
                guncelleSonrakiKayitId(kaynakTablo, kaynakKayitId, yeniKayitId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private int getOncesiKayitId(String kaynakTablo, int kaynakKayitId) {
        String sorgu = "SELECT ID FROM IslemGecmisi WHERE hedefTablo = ? AND hedefKayitId = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setString(1, kaynakTablo);
            preparedStatement.setInt(2, kaynakKayitId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("ID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Önceki kayıt bulunamazsa
    }
    
    private void guncelleSonrakiKayitId(String kaynakTablo, int kaynakKayitId, int yeniKayitId) {
        String sorgu = "UPDATE IslemGecmisi SET sonrakiKayitId = ? WHERE kaynakTablo = ? AND kaynakKayitId = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, yeniKayitId);
            preparedStatement.setString(2, kaynakTablo);
            preparedStatement.setInt(3, kaynakKayitId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    

    public List<Object[]> transferGecmisiniGetir() {
        String sorgu = "SELECT * FROM IslemGecmisi";
        ResultSet resultSet = veritabaniBaglantisi.get(sorgu);
        List<Object[]> sonuclar = new ArrayList<>();

        try {
            if (resultSet != null) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int sutunSayisi = metaData.getColumnCount();

                while (resultSet.next()) {
                    Object[] satir = new Object[sutunSayisi];
                    for (int i = 0; i < sutunSayisi; i++) {
                        satir[i] = resultSet.getObject(i + 1);
                    }
                    sonuclar.add(satir);
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sonuclar;
    }

    public List<TabloVerisi> detayliTransferGecmisiniGetir(int kayitId, String tabloAdi) {
        List<TabloVerisi> detayliGecmis = new ArrayList<>();
        Set<String> ziyaretEdilenKayitlar = new HashSet<>();
        detayliTransferGecmisiniGetirRec(kayitId, tabloAdi, detayliGecmis, ziyaretEdilenKayitlar);
        return detayliGecmis;
    }
    
    private void detayliTransferGecmisiniGetirRec(int kayitId, String tabloAdi, List<TabloVerisi> detayliGecmis, Set<String> ziyaretEdilenKayitlar) {
        String kayitAnahtari = tabloAdi + "_" + kayitId;
        if (ziyaretEdilenKayitlar.contains(kayitAnahtari)) {
            return;
        }
        ziyaretEdilenKayitlar.add(kayitAnahtari);
    
        List<Object[]> kayitDetaylari = kayitDetaylariniGetir(kayitId, tabloAdi);
        if (!kayitDetaylari.isEmpty()) {
            detayliGecmis.add(new TabloVerisi(tabloAdi, kayitId, kayitDetaylari));
        }
    
        String sorgu = "SELECT * FROM IslemGecmisi WHERE kaynakKayitId = ? AND kaynakTablo = ? OR hedefKayitId = ? AND hedefTablo = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, kayitId);
            preparedStatement.setString(2, tabloAdi);
            preparedStatement.setInt(3, kayitId);
            preparedStatement.setString(4, tabloAdi);
    
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet != null) {
                while (resultSet.next()) {
                    int kaynakKayitId = resultSet.getInt("kaynakKayitId");
                    String kaynakTablo = resultSet.getString("kaynakTablo");
                    int hedefKayitId = resultSet.getInt("hedefKayitId");
                    String hedefTablo = resultSet.getString("hedefTablo");
    
                    if (!kaynakTablo.equals(tabloAdi) || kaynakKayitId != kayitId) {
                        detayliTransferGecmisiniGetirRec(kaynakKayitId, kaynakTablo, detayliGecmis, ziyaretEdilenKayitlar);
                    }
                    if (!hedefTablo.equals(tabloAdi) || hedefKayitId != kayitId) {
                        detayliTransferGecmisiniGetirRec(hedefKayitId, hedefTablo, detayliGecmis, ziyaretEdilenKayitlar);
                    }
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private List<Object[]> kayitDetaylariniGetir(int kayitId, String tabloAdi) {
        String sorgu = "SELECT * FROM " + tabloAdi + " WHERE ID = " + kayitId;
        List<Object[]> sonuclar = new ArrayList<>();
    
        try {
            ResultSet resultSet = veritabaniBaglantisi.get(sorgu);
            if (resultSet != null) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int sutunSayisi = metaData.getColumnCount();
    
                while (resultSet.next()) {
                    Object[] satir = new Object[sutunSayisi];
                    boolean bos = true;
                    for (int i = 0; i < sutunSayisi; i++) {
                        satir[i] = resultSet.getObject(i + 1);
                        if (satir[i] != null) {
                            bos = false;
                        }
                    }
                    if (!bos) {
                        sonuclar.add(satir);
                    }
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sonuclar;
    }
    
    
    public static class TabloVerisi {
        public String tabloAdi;
        public int kayitId;
        public List<Object[]> veriler;
    
        public TabloVerisi(String tabloAdi, int kayitId, List<Object[]> veriler) {
            this.tabloAdi = tabloAdi;
            this.kayitId = kayitId;
            this.veriler = veriler;
        }
    }
    
    
    public static class TransferKayit {
        public String tabloAdi;
        public int kayitId;
        public List<Object[]> veriler;
    
        public TransferKayit(String tabloAdi, int kayitId, List<Object[]> veriler) {
            this.tabloAdi = tabloAdi;
            this.kayitId = kayitId;
            this.veriler = veriler;
        }
    }
    
    
    


    public String[] tabloIcinSutunAdlariniGetir(String tabloAdi) {
        String sorgu = "SELECT * FROM " + tabloAdi + " LIMIT 1";
        List<String> sutunListesi = new ArrayList<>();

        try {
            ResultSet resultSet = veritabaniBaglantisi.get(sorgu);
            if (resultSet != null) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int sutunSayisi = metaData.getColumnCount();

                for (int i = 0; i < sutunSayisi; i++) {
                    sutunListesi.add(metaData.getColumnName(i + 1));
                }
                resultSet.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sutunListesi.toArray(new String[0]);
    }

   

    public boolean kullaniciDogrula(String kullaniciAdi, String sifre) {
        String sorgu = "SELECT * FROM kullanicilar WHERE kullaniciAdi = ? AND sifre = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setString(1, kullaniciAdi);
            preparedStatement.setString(2, sifre);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public VeritabaniBaglantisi getVeritabaniBaglantisi() {
        return veritabaniBaglantisi;
    }

    public void musteriEkle(String ad, String soyad, String telefon1, String telefon2) {
        String sorgu = "INSERT INTO Musteri (Ad, Soyad, Telefon1, Telefon2) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setString(1, ad);
            preparedStatement.setString(2, soyad);
            preparedStatement.setString(3, telefon1);
            preparedStatement.setString(4, telefon2);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void musteriSil(int musteriId) {
        String sorgu = "DELETE FROM Musteri WHERE ID = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, musteriId);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<Object[]> musteriListesiGetir() {
        List<Object[]> musteriler = new ArrayList<>();
        String sorgu = "SELECT * FROM Musteri";
        try (ResultSet resultSet = veritabaniBaglantisi.get(sorgu)) {
            while (resultSet.next()) {
                Object[] musteri = new Object[]{
                    resultSet.getInt("ID"),
                    resultSet.getString("Ad"),
                    resultSet.getString("Soyad"),
                    resultSet.getString("Telefon1"),
                    resultSet.getString("Telefon2")
                };
                musteriler.add(musteri);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return musteriler;
    }

    public List<Object[]> musteriDetayListesiGetir(int musteriId) {
        List<Object[]> detaylar = new ArrayList<>();
        String sorgu = "SELECT * FROM MusteriDetay WHERE MusteriID = " + musteriId;
        try (ResultSet resultSet = veritabaniBaglantisi.get(sorgu)) {
            while (resultSet.next()) {
                Object[] detay = new Object[]{
                    resultSet.getDate("Tarih"),
                    resultSet.getString("OdemeSekli"),
                    resultSet.getString("Aciklama"),
                    resultSet.getDouble("Borc"),
                    resultSet.getDouble("Alacak")
                };
                detaylar.add(detay);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return detaylar;
    }

    public void musteriDetaySil(int musteriDetayId) {
        String sorgu = "DELETE FROM MusteriDetay WHERE ID = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, musteriDetayId);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void veritabaniKapat() {
        veritabaniBaglantisi.close();
    }

    public double getKatsayiFromKatsayilar(int ayar) {
        double katsayi = 0.0;
        String sorgu = "SELECT Katsayi FROM katsayilar WHERE Ayar = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, ayar);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                katsayi = resultSet.getDouble("Katsayi");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return katsayi;
    }
    
    public double getKatsayiFromKatsayilarIscilik(int ayar) {
        double katsayi = 0.0;
        String sorgu = "SELECT Katsayi FROM katsayilariscilik WHERE Ayar = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, ayar);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                katsayi = resultSet.getDouble("Katsayi");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return katsayi;
    }
    
    public void hesapKolonunuGuncelle(int kayitId, double hesap) {
        String sorgu = "UPDATE Dokum SET HESAP = ? WHERE ID = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setDouble(1, hesap);
            preparedStatement.setInt(2, kayitId);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    public void durumuGuncelle(int kayitId, String tabloAdi) {
        String sorgu = "UPDATE " + tabloAdi + " SET DURUM = 1 WHERE ID = ?";
        try (PreparedStatement preparedStatement = veritabaniBaglantisi.getConnection().prepareStatement(sorgu)) {
            preparedStatement.setInt(1, kayitId);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
