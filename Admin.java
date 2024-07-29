import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class Admin {
    private static JTable tablo;
    private static DefaultTableModel tabloModeli;
    private static final String[] simgeler = {"islemler"};
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
            cerceve.setExtendedState(JFrame.MAXIMIZED_BOTH);

            initialize(cerceve);

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

        tabloyuVeFormuGuncelle("islemler");

        cerceve.pack();
    }

    

    private static void initializePopupMenu() {
        popupMenu = new JPopupMenu();
        JMenuItem silItem = new JMenuItem("Kaydı Sil");
        JMenuItem duzenleItem = new JMenuItem("Kaydı Düzenle");
        
        silItem.addActionListener(e -> kayitSil());
        duzenleItem.addActionListener(e -> kayitDuzenle());
       
        popupMenu.add(silItem);
        popupMenu.add(duzenleItem);
      
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
            JOptionPane.showMessageDialog(null, "Mevcut tablo adı geçersiz.");
            return;
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
    
 

    static class DugmeDinleyici implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            mevcutTabloAdi = actionCommand;
            tabloyuVeFormuGuncelle(actionCommand);
        }
    }
    private static void tabloyuVeFormuGuncelle(String tabloAdi) {
        String sorgu = "SELECT * FROM " + tabloAdi + " ORDER BY ID DESC LIMIT 100";
        tabloyuGuncelle(sorgu);
        formuGuncelle(sorgu);
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
