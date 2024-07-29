import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class Main {
    private static JTable tablo;
    private static DefaultTableModel tabloModeli;
    private static final String[] simgeler = {"Tezgah", "Patlatma", "Cila", "Tambur", "Dokum"};
    private static JTextField[] formAlanlari;
    private static JComboBox<Integer> ayarComboBox;
    private static JPanel formPaneli;
    private static String[] sutunAdlari;
    private static String mevcutTabloAdi;
    private static Backend arkaPlan;
    private static JPopupMenu popupMenu;
    private static JPanel aramaPaneli;
    private static JTextField[] aramaAlanlari;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VeritabaniBaglantisi veritabaniBaglantisi = new VeritabaniBaglantisi(); // Veritabanı bağlantısını başlat.
            arkaPlan = new Backend(veritabaniBaglantisi); // Backend sınıfı başlatılıyor.
    
            JFrame cerceve = new JFrame("Dunaf Kuyumculuk");
            cerceve.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
            initialize(cerceve);
    
            cerceve.setExtendedState(JFrame.MAXIMIZED_BOTH);
            cerceve.setVisible(true);
    
            cerceve.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    arkaPlan.veritabaniKapat(); // Pencere kapanırken veritabanı bağlantısını kapat.
                }
            });
        });
    }
    
    private static void initialize(JFrame cerceve) {
        formPaneli = new JPanel(new GridBagLayout());
        formPaneli.setBorder(BorderFactory.createTitledBorder("Yeni Kayıt Ekle"));
        JScrollPane formScrollPane = new JScrollPane(formPaneli);

        tabloModeli = new DefaultTableModel(new Object[][]{}, new String[]{}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        tablo = new JTable(tabloModeli);
        tablo.setFillsViewportHeight(true);
        JScrollPane tabloScrollPane = new JScrollPane(tablo);

        JToolBar toolBar = new JToolBar();
        for (String simge : simgeler) {
            JButton dugme = new JButton(simge);
            dugme.addActionListener(new DugmeDinleyici());
            toolBar.add(dugme);
        }

        

        
        aramaPaneli = new JPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formScrollPane, tabloScrollPane);
        splitPane.setDividerLocation(300); // Başlangıçta ayırıcıyı 300 pikselde ayarlayın

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(aramaPaneli, BorderLayout.SOUTH);

        cerceve.setContentPane(mainPanel);

        initializePopupMenu();

        tablo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && tablo.getSelectedRow() != -1) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        tabloyuVeFormuGuncelle("Tezgah");

        cerceve.pack();
    }

   
    private static void initializePopupMenu() {
        popupMenu = new JPopupMenu();
        JMenuItem silItem = new JMenuItem("Kaydı Sil");
        JMenuItem duzenleItem = new JMenuItem("Kaydı Düzenle");
        JMenuItem cilayaAlItem = new JMenuItem("Cilaya al");
        JMenuItem tezgahaAlItem = new JMenuItem("Tezgaha al");
        JMenuItem patlatmayaAlItem = new JMenuItem("Patlatmaya al");
        JMenuItem tamburaAlItem = new JMenuItem("Tambura al");
        JMenuItem islemGecmisiItem = new JMenuItem("İşlem Geçmişini Görüntüle");
        JMenuItem islemiSonlandirItem = new JMenuItem("İşlemi Sonlandır");

        silItem.addActionListener(e -> kayitSil());
        duzenleItem.addActionListener(e -> kayitDuzenle());
        cilayaAlItem.addActionListener(e -> kayitTransferEt("Cila"));
        tezgahaAlItem.addActionListener(e -> kayitTransferEt("Tezgah"));
        patlatmayaAlItem.addActionListener(e -> kayitTransferEt("Patlatma"));
        tamburaAlItem.addActionListener(e -> kayitTransferEt("Tambur"));
        islemGecmisiItem.addActionListener(e -> detayliTransferGecmisiniGoster());
        islemiSonlandirItem.addActionListener(e -> islemiSonlandir());

        popupMenu.add(silItem);
        popupMenu.add(duzenleItem);
        popupMenu.add(cilayaAlItem);
        popupMenu.add(tezgahaAlItem);
        popupMenu.add(patlatmayaAlItem);
        popupMenu.add(tamburaAlItem);
        popupMenu.add(islemGecmisiItem);
        popupMenu.add(islemiSonlandirItem);
    }
    
    private static void islemiSonlandir() {
        int seciliSatir = tablo.getSelectedRow();
        if (seciliSatir == -1) {
            JOptionPane.showMessageDialog(null, "Lütfen sonlandırmak için bir kayıt seçin.");
            return;
        }
    
        try {
            // Seçilen tablodaki kayıttan URUN_AD ve CIKAN_MIKTAR kolonlarını al
            String urunAd = (String) tablo.getValueAt(seciliSatir, tabloModeli.findColumn("URUN_AD"));
            float cikanMiktar = tablo.getValueAt(seciliSatir, tabloModeli.findColumn("CIKAN_MIKTAR")) != null ?
                    ((Number) tablo.getValueAt(seciliSatir, tabloModeli.findColumn("CIKAN_MIKTAR"))).floatValue() : 0.0f;
    
            // Seçilen kaydın ID'sini al
            int kayitId = (int) tablo.getValueAt(seciliSatir, tabloModeli.findColumn("ID"));
    
            // İşlem geçmişinden ilgili kayıtları bul
            int dokumId = -1;
            int tezgahId = -1;
            String sorgu = "SELECT * FROM IslemGecmisi WHERE hedefKayitId = ? AND hedefTablo = ?";
            try (PreparedStatement preparedStatement = arkaPlan.getVeritabaniBaglantisi().getConnection().prepareStatement(sorgu)) {
                preparedStatement.setInt(1, kayitId);
                preparedStatement.setString(2, mevcutTabloAdi);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs != null && rs.next()) {
                    dokumId = rs.getInt("kaynakKayitId");
                    tezgahId = rs.getInt("kaynakKayitId"); // TezgahId'yi almak için başka bir sorgu gerekebilir
                }
            }
    
            if (dokumId == -1 || tezgahId == -1) {
                JOptionPane.showMessageDialog(null, "İşlem geçmişinde ilgili kayıtlar bulunamadı.");
                return;
            }
    
            // Dokum tablosundan TAS_MIKTARI, AYAR ve HESAP kolonlarını al
            String dokumSorgu = "SELECT TAS_MIKTARI, AYAR, HESAP FROM Dokum WHERE ID = " + dokumId;
            ResultSet dokumRs = arkaPlan.getVeritabaniBaglantisi().get(dokumSorgu);
            float tasMiktari = 0.0f;
            int ayar = 0;
            float hesap = 0.0f;
            if (dokumRs != null && dokumRs.next()) {
                tasMiktari = dokumRs.getFloat("TAS_MIKTARI");
                ayar = dokumRs.getInt("AYAR");
                hesap = dokumRs.getFloat("HESAP");
            }
    
            // Tezgah tablosundan B_TAS kolonunu al
            String tezgahSorgu = "SELECT B_TAS FROM Tezgah WHERE ID = " + tezgahId;
            ResultSet tezgahRs = arkaPlan.getVeritabaniBaglantisi().get(tezgahSorgu);
            float bTas = 0.0f;
            if (tezgahRs != null && tezgahRs.next()) {
                bTas = tezgahRs.getFloat("B_TAS");
            }
    
            // İscilik sonucunu hesapla
            float sonuc = cikanMiktar - bTas - tasMiktari;
            float katsayi = (float) arkaPlan.getKatsayiFromKatsayilarIscilik(ayar);
            float iscilikSonucu = sonuc * katsayi / cikanMiktar;
    
            // Islemler tablosuna ekle
            String eklemeSorgusu = "INSERT INTO Islemler (URUN_AD, TAS_MIKTARI, B_TAS, CIKAN_MIKTAR, HESAP, ISCILIK, AYAR) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = arkaPlan.getVeritabaniBaglantisi().getConnection().prepareStatement(eklemeSorgusu)) {
                preparedStatement.setString(1, urunAd);
                preparedStatement.setFloat(2, tasMiktari);
                preparedStatement.setFloat(3, bTas);
                preparedStatement.setFloat(4, cikanMiktar);
                preparedStatement.setFloat(5, hesap);
                preparedStatement.setFloat(6, iscilikSonucu);
                preparedStatement.setInt(7, ayar);
                preparedStatement.executeUpdate();
            }
    
            // Tüm işlem geçmişindeki tablolarda DURUM kolonunu güncelle
            String[] tabloIsimleri = {"Tezgah", "Patlatma", "Cila", "Tambur", "Dokum"};
            for (String tabloAdi : tabloIsimleri) {
                String guncelleSorgu = "UPDATE " + tabloAdi + " SET DURUM = 1 WHERE ID = " + kayitId;
                try (PreparedStatement preparedStatement = arkaPlan.getVeritabaniBaglantisi().getConnection().prepareStatement(guncelleSorgu)) {
                    preparedStatement.executeUpdate();
                }
            }
    
            JOptionPane.showMessageDialog(null, "İşlem başarıyla sonlandırıldı!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "İşlem sonlandırılırken bir hata oluştu: " + e.getMessage());
        } catch (ClassCastException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Veri türü hatası: " + e.getMessage());
        }
    }
    
    
    private static void kayitSil() {
        int seciliSatir = tablo.getSelectedRow();
        if (seciliSatir == -1) return;

        int kayitId = (int) tablo.getValueAt(seciliSatir, 1); // Assuming ID is at index 1
        arkaPlan.kayitSil(mevcutTabloAdi, kayitId);
        tabloyuVeFormuGuncelle(mevcutTabloAdi);
    }

    private static void kayitDuzenle() {
        int seciliSatir = tablo.getSelectedRow();
        if (seciliSatir == -1) {
            JOptionPane.showMessageDialog(null, "Lütfen düzenlemek için bir kayıt seçin.");
            return;
        }
    
        int kayitId = (int) tablo.getValueAt(seciliSatir, 1); // Assuming ID is at index 1
        System.out.println("Düzenlenecek Kayıt ID: " + kayitId);
    
        // Mevcut verileri çekmek için kaydı al
        if (mevcutTabloAdi == null || mevcutTabloAdi.isEmpty()) {
           mevcutTabloAdi="Tezgah";
            
        }
    
        Object[] mevcutVeri = arkaPlan.kayitVerileriniGetir(mevcutTabloAdi, kayitId);
        if (mevcutVeri == null) {
            JOptionPane.showMessageDialog(null, "Seçilen kaydın verileri alınamadı.");
            return;
        }
    
        // Düzenleme formu oluştur
        JFrame duzenleCercevesi = new JFrame("Kaydı Düzenle");
        duzenleCercevesi.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        duzenleCercevesi.setLayout(new GridBagLayout());
    
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
    
        JTextField[] duzenleFormAlanlari = new JTextField[sutunAdlari.length];
        JComboBox<Integer> duzenleAyarComboBox = null;
    
        for (int i = 0; i < sutunAdlari.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            duzenleCercevesi.add(new JLabel(sutunAdlari[i] + ":"), gbc);
    
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
    
            if (sutunAdlari[i].equalsIgnoreCase("AYAR")) {
                duzenleAyarComboBox = new JComboBox<>(new Integer[]{8, 14, 18, 22});
                if (mevcutVeri[i + 1] instanceof Float) {
                    duzenleAyarComboBox.setSelectedItem(((Float) mevcutVeri[i + 1]).intValue());
                } else {
                    duzenleAyarComboBox.setSelectedItem((Integer) mevcutVeri[i + 1]);
                }
                duzenleCercevesi.add(duzenleAyarComboBox, gbc);
            } else if (!sutunAdlari[i].equalsIgnoreCase("DURUM")) {
                duzenleFormAlanlari[i] = new JTextField(20);
                if (mevcutVeri[i + 1] != null) {
                    duzenleFormAlanlari[i].setText(mevcutVeri[i + 1].toString());
                } else {
                    duzenleFormAlanlari[i].setText("");
                }
                duzenleCercevesi.add(duzenleFormAlanlari[i], gbc);
            }
        }
    
        JButton kaydetDugmesi = new JButton("Kaydet");
        gbc.gridx = 1;
        gbc.gridy = sutunAdlari.length;
        gbc.anchor = GridBagConstraints.CENTER;
        duzenleCercevesi.add(kaydetDugmesi, gbc);
    
        JComboBox<Integer> finalDuzenleAyarComboBox = duzenleAyarComboBox;
        kaydetDugmesi.addActionListener(e -> {
            arkaPlan.kayitGuncelle(mevcutTabloAdi, kayitId, duzenleFormAlanlari, finalDuzenleAyarComboBox, sutunAdlari);
            duzenleCercevesi.dispose();
            tabloyuVeFormuGuncelle(mevcutTabloAdi);
        });
    
        duzenleCercevesi.pack();
        duzenleCercevesi.setLocationRelativeTo(null);
        duzenleCercevesi.setVisible(true);
    }
    
    

    private static void kayitTransferEt(String hedefTablo) {
        int seciliSatir = tablo.getSelectedRow();
        if (seciliSatir == -1) return;
    
        int kayitId = (int) tablo.getValueAt(seciliSatir, 1); // Assuming ID is at index 1
    
        // Mevcut kaydın URUN_AD değerini al
        String urunAd = arkaPlan.getUrunAdFromKaynakTablo(mevcutTabloAdi, kayitId);
    
        // Formu aç ve yeni kaydı ekle
        JFrame transferCercevesi = new JFrame("Yeni Kayıt: " + hedefTablo);
        transferCercevesi.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        transferCercevesi.setLayout(new GridBagLayout());
    
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
    
        String[] hedefSutunAdlari = arkaPlan.formSutunAdlariniGetir("SELECT * FROM " + hedefTablo);
        JTextField[] hedefFormAlanlari = new JTextField[hedefSutunAdlari.length];
    
        for (int i = 0; i < hedefSutunAdlari.length; i++) {
            if (!hedefSutunAdlari[i].equalsIgnoreCase("kaynakTablo") && !hedefSutunAdlari[i].equalsIgnoreCase("DURUM") && !hedefSutunAdlari[i].equalsIgnoreCase("URUN_AD")) {
                gbc.gridx = 0;
                gbc.gridy = i;
                transferCercevesi.add(new JLabel(hedefSutunAdlari[i] + ":"), gbc);
    
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
    
                hedefFormAlanlari[i] = new JTextField(20);
                transferCercevesi.add(hedefFormAlanlari[i], gbc);
            }
        }
    
        JButton transferDugmesi = new JButton("Kaydet");
        gbc.gridx = 1;
        gbc.gridy = hedefSutunAdlari.length;
        gbc.anchor = GridBagConstraints.CENTER;
        transferCercevesi.add(transferDugmesi, gbc);
    
        transferDugmesi.addActionListener(e -> {
            System.out.println("Transfer düğmesine basıldı");
            System.out.println("Hedef Tablo: " + hedefTablo);
            System.out.println("Mevcut Tablo Adı: " + mevcutTabloAdi);
    
            arkaPlan.hedefTabloyaKayitEkle(hedefTablo, hedefFormAlanlari, hedefSutunAdlari, mevcutTabloAdi, kayitId, urunAd);
    
            transferCercevesi.dispose();
        });
    
        transferCercevesi.pack();
        transferCercevesi.setLocationRelativeTo(null);
        transferCercevesi.setVisible(true);
    }
    
    
    
    private static void detayliTransferGecmisiniGoster() {
        int seciliSatir = tablo.getSelectedRow();
        if (seciliSatir == -1) return;
    
        int kayitId = (int) tablo.getValueAt(seciliSatir, 1); // Assuming ID is at index 1
    
        List<Backend.TabloVerisi> gecmis = arkaPlan.detayliTransferGecmisiniGetir(kayitId, mevcutTabloAdi);
        if (gecmis.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Bu kayıt için işlem geçmişi bulunmamaktadır.");
            return;
        }
    
        JFrame gecmisCercevesi = new JFrame("İşlem Geçmişi");
        gecmisCercevesi.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gecmisCercevesi.setLayout(new GridLayout(gecmis.size(), 1));
    
        for (Backend.TabloVerisi tabloVerisi : gecmis) {
            String[] sutunAdlari = arkaPlan.tabloIcinSutunAdlariniGetir(tabloVerisi.tabloAdi);
            DefaultTableModel gecmisTabloModeli = new DefaultTableModel(tabloVerisi.veriler.toArray(new Object[0][]), sutunAdlari);
            JTable gecmisTablosu = new JTable(gecmisTabloModeli);
            gecmisCercevesi.add(new JScrollPane(gecmisTablosu));
        }
    
        gecmisCercevesi.setSize(800, 600);
        gecmisCercevesi.setLocationRelativeTo(null);
        gecmisCercevesi.setVisible(true);
    }
    

    static class DugmeDinleyici implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            mevcutTabloAdi = actionCommand;
            tabloyuVeFormuGuncelle(actionCommand);
        }
    }
   
   
    public static void tabloyuVeFormuGuncelle(String tabloAdi) {
        if (tabloAdi.equals("Islem Gecmisi")) {
            transferGecmisiniGuncelle();
            return;
        }
        String sorgu = "SELECT * FROM " + tabloAdi + " ORDER BY ID DESC LIMIT 100";
        tabloyuGuncelle(sorgu);
        formuGuncelle(sorgu);
    }
    private static void transferGecmisiniGuncelle() {
        List<Object[]> sonuclar = arkaPlan.transferGecmisiniGetir();
        String[] sutunAdlari = {"ID", "Kaynak Tablo", "Hedef Tablo", "Kaynak Kayıt ID", "Hedef Kayıt ID", "Tarih"};
    
        Object[][] veri = sonuclar.toArray(new Object[0][]);
        tabloModeli.setDataVector(veri, sutunAdlari);
    
        formPaneli.removeAll();
        formPaneli.revalidate();
        formPaneli.repaint();
    }
    
    private static void tabloyuGuncelle(String sorgu) {
        List<Object[]> sonuclar = arkaPlan.tabloVerileriniGetir(sorgu);
        String[] sutunAdlari = arkaPlan.sutunAdlariniGetir(sorgu);

        Object[][] veri = sonuclar.toArray(new Object[0][]);
        tabloModeli.setDataVector(veri, sutunAdlari);
        filtrelemePaneliniOlustur(sutunAdlari);
    }
    private static void formuGuncelle(String sorgu) {
        sutunAdlari = arkaPlan.formSutunAdlariniGetir(sorgu);
    
        formPaneli.removeAll();
        formPaneli.setLayout(new GridBagLayout());
    
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
    
        formAlanlari = new JTextField[sutunAdlari.length];
        for (int i = 0; i < sutunAdlari.length; i++) {

    
            if (sutunAdlari[i].equalsIgnoreCase("AYAR")) {
                gbc.gridx = 0;
                gbc.gridy = i;
                formPaneli.add(new JLabel(sutunAdlari[i] + ":"), gbc);
        
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                ayarComboBox = new JComboBox<>(new Integer[]{8, 14, 18, 22});
                formPaneli.add(ayarComboBox, gbc);
            } else if (!sutunAdlari[i].equalsIgnoreCase("HESAP") && !sutunAdlari[i].equalsIgnoreCase("DURUM")&& !sutunAdlari[i].equalsIgnoreCase("kaynakTablo")) {
                gbc.gridx = 0;
                gbc.gridy = i;
                formPaneli.add(new JLabel(sutunAdlari[i] + ":"), gbc);
        
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                formAlanlari[i] = new JTextField(20);
                formPaneli.add(formAlanlari[i], gbc);
            }
        }
    
        JButton ekleDugmesi = new JButton("Ekle");
        ekleDugmesi.addActionListener(new EkleDugmesiDinleyici());
        gbc.gridx = 1;
        gbc.gridy = sutunAdlari.length;
        gbc.anchor = GridBagConstraints.CENTER;
        formPaneli.add(ekleDugmesi, gbc);
    
        formPaneli.revalidate();
        formPaneli.repaint();
    }
    



    private static void filtrelemePaneliniOlustur(String[] sutunAdlari) {
        aramaPaneli.removeAll();
        aramaPaneli.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        aramaAlanlari = new JTextField[sutunAdlari.length];
        for (int i = 1; i < sutunAdlari.length; i++) {
            if (sutunAdlari[i].equalsIgnoreCase("DURUM")) {
                continue;
            }
            gbc.gridx = i - 1;
            gbc.gridy = 0;
            aramaPaneli.add(new JLabel(sutunAdlari[i]), gbc);

            gbc.gridy = 1;
            aramaAlanlari[i] = new JTextField(10);
            aramaAlanlari[i].getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    filtrele();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    filtrele();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    filtrele();
                }
            });
            aramaPaneli.add(aramaAlanlari[i], gbc);
        }

        aramaPaneli.revalidate();
        aramaPaneli.repaint();
    }
    private static void filtrele() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tabloModeli);
        tablo.setRowSorter(sorter);
    
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        for (int i = 1; i < aramaAlanlari.length; i++) {
            if (aramaAlanlari[i] != null) { // Null kontrolü ekliyoruz
                String text = aramaAlanlari[i].getText();
                if (!text.trim().isEmpty()) {
                    filters.add(RowFilter.regexFilter("^" + text, i));
                }
            }
        }
        sorter.setRowFilter(RowFilter.andFilter(filters));
    }
    
    

    static class EkleDugmesiDinleyici implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (mevcutTabloAdi == null || sutunAdlari == null) return;

            StringBuilder sorguOlusturucu = new StringBuilder("INSERT INTO ");
            sorguOlusturucu.append(mevcutTabloAdi).append(" (");

            boolean ilk = true;
            for (int i = 0; i < sutunAdlari.length; i++) {
                if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("HESAP") && !sutunAdlari[i].equalsIgnoreCase("DURUM")) {
                    if (!ilk) {
                        sorguOlusturucu.append(", ");
                    }
                    sorguOlusturucu.append(sutunAdlari[i]);
                    ilk = false;
                }
            }
            sorguOlusturucu.append(") VALUES (");

            ilk = true;
            for (int i = 0; i < formAlanlari.length; i++) {
                if (!sutunAdlari[i].equalsIgnoreCase("ID") && !sutunAdlari[i].equalsIgnoreCase("HESAP") && !sutunAdlari[i].equalsIgnoreCase("DURUM")) {
                    if (!ilk) {
                        sorguOlusturucu.append(", ");
                    }
                    sorguOlusturucu.append("?");
                    ilk = false;
                }
            }
            sorguOlusturucu.append(")");

            String sorgu = sorguOlusturucu.toString();

            arkaPlan.kayitEkle(sorgu, formAlanlari, ayarComboBox, sutunAdlari, mevcutTabloAdi);
        }
    }
}