import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Date;

public class AdminPage extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JSplitPane customerSplitPane;
    private JList<String> customerList;
    private DefaultListModel<String> customerListModel;
    private JTable customerDetailsTable;
    private DefaultTableModel customerTableModel;
    private JTable finishedProductsTable;
    private DefaultTableModel finishedProductsTableModel;
    private JTable fileTable;
    private DefaultTableModel fileTableModel;
    private VeritabaniBaglantisi dbConnection;
    private JButton saveButton;
    private JLabel balanceLabel;
    private JTextField balanceField;
    private JLabel phoneLabel;
    private JTextField phoneField;
    private JTextField searchField;
    private JTextField filterField;
    private JButton addCustomerButton;
    private JButton addFileButton;
    private JPopupMenu customerPopupMenu;
    private JMenuItem editMenuItem;
    private JMenuItem deleteMenuItem;
    private JPopupMenu tablePopupMenu;
    private JMenuItem editTableItem;
    private JMenuItem deleteTableItem;
    private String[] tables = {"Patlatma", "Cila", "Tambur", "Tezgah"};

    public AdminPage() {
        dbConnection = new VeritabaniBaglantisi();

        setTitle("Admin Page");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Full screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Toolbar
        JToolBar toolBar = new JToolBar();
        JButton homeButton = new JButton("Ana Sayfa");
        JButton finishedProductsButton = new JButton("Biten Ürünler");
        JButton customersButton = new JButton("Müşteriler");
        toolBar.add(homeButton);
        toolBar.add(finishedProductsButton);
        toolBar.add(customersButton);

        // Create margin panel with balance and phone for customers view
        JPanel marginPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        balanceLabel = new JLabel("Genel Bakiye: ");
        balanceField = new JTextField(10);
        balanceField.setEditable(false);
        phoneLabel = new JLabel("Telefon: ");
        phoneField = new JTextField(10);
        phoneField.setEditable(false);

        marginPanel.add(phoneLabel);
        marginPanel.add(phoneField);
        marginPanel.add(Box.createRigidArea(new Dimension(20, 0))); // Add space between phone and balance
        marginPanel.add(balanceLabel);
        marginPanel.add(balanceField);

        add(toolBar, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create Customer Panel
        customerSplitPane = new JSplitPane();
        customerSplitPane.setDividerLocation(300);

        // Left Panel (Customer List)
        JPanel customerLeftPanel = new JPanel(new BorderLayout());
        searchField = new JTextField(15);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadCustomerList(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadCustomerList(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadCustomerList(searchField.getText());
            }
        });

        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Müşteri Ara:"));
        searchPanel.add(searchField);
        customerLeftPanel.add(searchPanel, BorderLayout.NORTH);

        customerListModel = new DefaultListModel<>();
        customerList = new JList<>(customerListModel);
        customerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showCustomerDetails(customerList.getSelectedIndex());
                    updateBalanceAndPhone();
                }
            }
        });
        customerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = customerList.locationToIndex(e.getPoint());
                    customerList.setSelectedIndex(index);
                    customerPopupMenu.show(customerList, e.getX(), e.getY());
                }
            }
        });
        JScrollPane customerLeftScrollPane = new JScrollPane(customerList);
        customerLeftPanel.add(customerLeftScrollPane, BorderLayout.CENTER);

        addCustomerButton = new JButton("Müşteri Ekle");
        addCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddCustomerDialog();
            }
        });
        customerLeftPanel.add(addCustomerButton, BorderLayout.SOUTH);

        customerSplitPane.setLeftComponent(customerLeftPanel);

        // Right Panel (Customer Details Table)
        JPanel customerRightPanel = new JPanel(new BorderLayout());
        customerTableModel = new DefaultTableModel();
        customerTableModel.addColumn("ID");
        customerTableModel.addColumn("Tarih");
        customerTableModel.addColumn("Ödeme Şekli");
        customerTableModel.addColumn("Açıklama");
        customerTableModel.addColumn("Borç");
        customerTableModel.addColumn("Alacak");
        customerDetailsTable = new JTable(customerTableModel);
        JScrollPane customerRightScrollPane = new JScrollPane(customerDetailsTable);
        customerRightPanel.add(customerRightScrollPane, BorderLayout.CENTER);

        // Save Button
        saveButton = new JButton("Kaydet");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Hücre düzenlemesini durdur
                if (customerDetailsTable.isEditing()) {
                    customerDetailsTable.getCellEditor().stopCellEditing();
                }
                saveCustomerDetails();
                updateBalanceAndPhone();
            }
        });
        customerRightPanel.add(saveButton, BorderLayout.SOUTH);

        customerSplitPane.setRightComponent(customerRightPanel);

        JPanel customerPanel = new JPanel(new BorderLayout());
        customerPanel.add(customerSplitPane, BorderLayout.CENTER);
        customerPanel.add(marginPanel, BorderLayout.SOUTH);

        mainPanel.add(customerPanel, "customers");

        // Create Finished Products Panel
        JSplitPane finishedProductsSplitPane = new JSplitPane();
        finishedProductsSplitPane.setDividerLocation(300);

        // Left Panel (Form to add new entry)
        JPanel finishedProductsLeftPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField[] finishedProductsFields = createFormFields("islemler");
        for (JTextField field : finishedProductsFields) {
            field.setColumns(30); // Set the columns to 30 to make the text fields wider
        }
        addFormFieldsToPanel(finishedProductsLeftPanel, finishedProductsFields, gbc);

        JButton addButton = new JButton("Ekle");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addEntryToTable("islemler", finishedProductsFields);
                loadFinishedProducts();
            }
        });

        gbc.gridx = 1;
        gbc.gridy = finishedProductsFields.length;
        finishedProductsLeftPanel.add(addButton, gbc);

        finishedProductsSplitPane.setLeftComponent(finishedProductsLeftPanel);

        // Right Panel (Table to display entries)
        JPanel finishedProductsRightPanel = new JPanel(new BorderLayout());
        finishedProductsTableModel = new DefaultTableModel();
        finishedProductsTableModel.addColumn("ID");
        finishedProductsTableModel.addColumn("Ürün Adı");
        finishedProductsTableModel.addColumn("Ayar");
        finishedProductsTableModel.addColumn("Taş Miktarı");
        finishedProductsTableModel.addColumn("Büyük Taş");
        finishedProductsTableModel.addColumn("Çıkan Miktar");
        finishedProductsTableModel.addColumn("İşçilik");
        finishedProductsTableModel.addColumn("Hesap");
        finishedProductsTableModel.addColumn("Tarih");
        finishedProductsTableModel.addColumn("Kaynak Tablo");

        finishedProductsTable = new JTable(finishedProductsTableModel);
        JScrollPane finishedProductsRightScrollPane = new JScrollPane(finishedProductsTable);
        finishedProductsRightPanel.add(finishedProductsRightScrollPane, BorderLayout.CENTER);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filtrele:"));
        filterField = new JTextField(20);
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }
        });
        filterPanel.add(filterField);
        finishedProductsRightPanel.add(filterPanel, BorderLayout.NORTH);

        finishedProductsSplitPane.setRightComponent(finishedProductsRightPanel);

        mainPanel.add(finishedProductsSplitPane, "finishedProducts");

        // Create Home Panel
        JSplitPane homeSplitPane = new JSplitPane();
        homeSplitPane.setDividerLocation(300);

        // Left Panel (Form to add new entry to FILE table)
        JPanel homeLeftPanel = new JPanel(new GridBagLayout());
        GridBagConstraints homeGbc = new GridBagConstraints();
        homeGbc.insets = new Insets(5, 5, 5, 5);
        homeGbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> tableComboBox = new JComboBox<>(tables);
        JTextField miktarField = new JTextField(10);

        homeGbc.gridx = 0;
        homeGbc.gridy = 0;
        homeLeftPanel.add(new JLabel("Tablo:"), homeGbc);

        homeGbc.gridx = 1;
        homeLeftPanel.add(tableComboBox, homeGbc);

        homeGbc.gridx = 0;
        homeGbc.gridy = 1;
        homeLeftPanel.add(new JLabel("Miktar:"), homeGbc);

        homeGbc.gridx = 1;
        homeLeftPanel.add(miktarField, homeGbc);

        addFileButton = new JButton("Ekle");
        addFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String tableName = (String) tableComboBox.getSelectedItem();
                String miktar = miktarField.getText().trim();
                int response = JOptionPane.showConfirmDialog(AdminPage.this, tableName + "'dan " + miktar + " gramı kullanmak istediğinize emin misiniz?", "Onay", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    addEntryToFileTable(tableName, miktar);
                    loadFileTable();
                }
            }
        });

        homeGbc.gridx = 1;
        homeGbc.gridy = 2;
        homeLeftPanel.add(addFileButton, homeGbc);

        homeSplitPane.setLeftComponent(homeLeftPanel);

        // Right Panel (Table to display summary)
        JPanel homeRightPanel = new JPanel(new BorderLayout());
        fileTableModel = new DefaultTableModel();
        fileTableModel.addColumn("Açıklama");
        fileTableModel.addColumn("Miktar");

        fileTable = new JTable(fileTableModel);
        JScrollPane fileRightScrollPane = new JScrollPane(fileTable);
        homeRightPanel.add(fileRightScrollPane, BorderLayout.CENTER);

        homeSplitPane.setRightComponent(homeRightPanel);

        mainPanel.add(homeSplitPane, "home");

        add(mainPanel, BorderLayout.CENTER);

        finishedProductsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "finishedProducts");
                toolBar.remove(marginPanel); // Remove balance and phone panel for finished products view
                loadFinishedProducts();
            }
        });

        customersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "customers");
                toolBar.add(marginPanel); // Add balance and phone panel for customers view
                loadCustomerList("");
            }
        });

        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "home");
                toolBar.remove(marginPanel); // Remove balance and phone panel for home view
                loadFileTable();
            }
        });

        cardLayout.show(mainPanel, "home");
        toolBar.remove(marginPanel); // Initially remove balance and phone panel for home view
        setVisible(false); // Initially hide the main frame

        createPopupMenu();
    }

    private void createPopupMenu() {
        customerPopupMenu = new JPopupMenu();

        editMenuItem = new JMenuItem("Düzenle");
        editMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEditCustomerDialog();
            }
        });
        customerPopupMenu.add(editMenuItem);

        deleteMenuItem = new JMenuItem("Sil");
        deleteMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCustomer();
            }
        });
        customerPopupMenu.add(deleteMenuItem);

        tablePopupMenu = new JPopupMenu();
        editTableItem = new JMenuItem("Düzenle");
        editTableItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editTableRow();
            }
        });
        tablePopupMenu.add(editTableItem);

        deleteTableItem = new JMenuItem("Sil");
        deleteTableItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteTableRow();
            }
        });
        tablePopupMenu.add(deleteTableItem);

        customerDetailsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = customerDetailsTable.rowAtPoint(e.getPoint());
                    customerDetailsTable.setRowSelectionInterval(row, row);
                    tablePopupMenu.show(customerDetailsTable, e.getX(), e.getY());
                }
            }
        });

        finishedProductsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = finishedProductsTable.rowAtPoint(e.getPoint());
                    finishedProductsTable.setRowSelectionInterval(row, row);
                    tablePopupMenu.show(finishedProductsTable, e.getX(), e.getY());
                }
            }
        });
    }

    private void loadCustomerList(String searchTerm) {
        customerListModel.clear();
        String query = "SELECT ID, Ad, Soyad FROM Musteri";
        if (!searchTerm.isEmpty()) {
            query += " WHERE Ad LIKE '%" + searchTerm + "%' OR Soyad LIKE '%" + searchTerm + "%'";
        }
        ResultSet rs = dbConnection.get(query);
        try {
            while (rs != null && rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("Ad") + " " + rs.getString("Soyad");
                customerListModel.addElement(id + " - " + name);
            }
        } catch (SQLException e) {
            showErrorDialog("Müşteri listesi yüklenirken bir hata oluştu.");
        }
    }

    private void showCustomerDetails(int index) {
        if (index < 0) return;

        String selectedValue = customerListModel.getElementAt(index);
        int customerId = Integer.parseInt(selectedValue.split(" - ")[0]);
        ResultSet rs = dbConnection.get("SELECT * FROM MusteriDetay WHERE MusteriID = " + customerId);

        customerTableModel.setRowCount(0); // Clear existing rows

        try {
            while (rs != null && rs.next()) {
                Object[] row = {
                        rs.getInt("ID"),
                        rs.getString("Tarih"),
                        rs.getString("OdemeSekli"),
                        rs.getString("Aciklama"),
                        rs.getDouble("Borc"),
                        rs.getDouble("Alacak")
                };
                customerTableModel.addRow(row);
            }
        } catch (SQLException e) {
            showErrorDialog("Müşteri detayları yüklenirken bir hata oluştu.");
        }

        // Add empty row for new entry
        customerTableModel.addRow(new Object[]{"", "", "", "", "", ""});}
        
private void saveCustomerDetails() {
    String selectedValue = customerList.getSelectedValue();
    if (selectedValue == null) return;

    int customerId = Integer.parseInt(selectedValue.split(" - ")[0]);

    for (int i = 0; i < customerTableModel.getRowCount(); i++) {
        Object id = customerTableModel.getValueAt(i, 0);
        Object tarih = customerTableModel.getValueAt(i, 1);
        Object odemeSekli = customerTableModel.getValueAt(i, 2);
        Object aciklama = customerTableModel.getValueAt(i, 3);
        Object borc = customerTableModel.getValueAt(i, 4);
        Object alacak = customerTableModel.getValueAt(i, 5);

        if ((tarih == null || tarih.toString().isEmpty()) && (odemeSekli == null || odemeSekli.toString().isEmpty())
                && (aciklama == null || aciklama.toString().isEmpty()) && (borc == null || borc.toString().isEmpty())
                && (alacak == null || alacak.toString().isEmpty())) {
            System.out.println("Skipping row " + i + " because no fields are filled.");
            continue;
        }

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        // Eğer tarih boşsa, güncel tarihi kullan
        if (tarih == null || tarih.toString().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            tarih = sdf.format(new java.util.Date());
        }

        columns.add("Tarih");
        values.add("'" + tarih.toString() + "'");

        if (odemeSekli != null && !odemeSekli.toString().isEmpty()) {
            columns.add("OdemeSekli");
            values.add("'" + odemeSekli.toString() + "'");
        }
        if (aciklama != null && !aciklama.toString().isEmpty()) {
            columns.add("Aciklama");
            values.add("'" + aciklama.toString() + "'");
        }
        if (borc != null && !borc.toString().isEmpty()) {
            try {
                BigDecimal borcDec = new BigDecimal(borc.toString());
                columns.add("Borc");
                values.add(borcDec.toString());
            } catch (NumberFormatException ex) {
                System.err.println("Invalid borc value at row " + i + ": " + borc);
            }
        }
        if (alacak != null && !alacak.toString().isEmpty()) {
            try {
                BigDecimal alacakDec = new BigDecimal(alacak.toString());
                columns.add("Alacak");
                values.add(alacakDec.toString());
            } catch (NumberFormatException ex) {
                System.err.println("Invalid alacak value at row " + i + ": " + alacak);
            }
        }

        if (columns.isEmpty()) {
            System.out.println("Skipping row " + i + " because no fields are filled.");
            continue;
        }

        String query = "";
        try {
            if (id == null || id.toString().isEmpty()) {
                // Insert new record
                query = "INSERT INTO MusteriDetay (MusteriID, " + String.join(", ", columns) + ") VALUES (" +
                        customerId + ", " + String.join(", ", values) + ")";
                dbConnection.executeUpdate(query);
            } else {
                // Update existing record
                List<String> updatePairs = new ArrayList<>();
                for (int j = 0; j < columns.size(); j++) {
                    updatePairs.add(columns.get(j) + "=" + values.get(j));
                }
                query = "UPDATE MusteriDetay SET " + String.join(", ", updatePairs) +
                        " WHERE ID=" + id;
                dbConnection.executeUpdate(query);
            }
        } catch (Exception e) {
            showErrorDialog("Kayıt işlemi sırasında bir hata oluştu: " + e.getMessage());
        }
    }

    showCustomerDetails(customerList.getSelectedIndex()); // Refresh details
}

    

    private void updateBalanceAndPhone() {
        String selectedValue = customerList.getSelectedValue();
        if (selectedValue == null) return;

        int customerId = Integer.parseInt(selectedValue.split(" - ")[0]);

        double totalBorc = 0.0;
        double totalAlacak = 0.0;
        ResultSet rs = dbConnection.get("SELECT Borc, Alacak FROM MusteriDetay WHERE MusteriID = " + customerId);
        try {
            while (rs != null && rs.next()) {
                totalBorc += rs.getDouble("Borc");
                totalAlacak += rs.getDouble("Alacak");
            }
        } catch (SQLException e) {
            showErrorDialog("Bakiye hesaplanırken bir hata oluştu.");
        }
        double balance = totalAlacak - totalBorc;
        balanceField.setText(String.valueOf(balance));

        rs = dbConnection.get("SELECT Telefon1 FROM Musteri WHERE ID = " + customerId);
        try {
            if (rs != null && rs.next()) {
                phoneField.setText(rs.getString("Telefon1"));
            }
        } catch (SQLException e) {
            showErrorDialog("Telefon bilgisi alınırken bir hata oluştu.");
        }
    }

    private void showAddCustomerDialog() {
        showFormDialog("Yeni Müşteri Ekle", "Musteri");
    }

    private void showEditCustomerDialog() {
        String selectedValue = customerList.getSelectedValue();
        if (selectedValue == null) return;

        int customerId = Integer.parseInt(selectedValue.split(" - ")[0]);

        showFormDialog("Müşteri Düzenle", "Musteri", customerId);
    }

    private void deleteCustomer() {
        String selectedValue = customerList.getSelectedValue();
        if (selectedValue == null) return;

        int customerId = Integer.parseInt(selectedValue.split(" - ")[0]);

        int confirm = JOptionPane.showConfirmDialog(this, "Müşteriyi silmek istediğinizden emin misiniz?", "Müşteri Sil", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String query = "DELETE FROM Musteri WHERE ID = " + customerId;
            dbConnection.executeUpdate(query);
            loadCustomerList("");
        }
    }

    private void loadFinishedProducts() {
        finishedProductsTableModel.setRowCount(0); // Clear existing rows
        ResultSet rs = dbConnection.get("SELECT * FROM islemler ORDER BY ID DESC");
        try {
            while (rs != null && rs.next()) {
                Object[] row = {
                        rs.getInt("id"),
                        rs.getString("urun_ad"),
                        rs.getFloat("ayar"),
                        rs.getFloat("tas_miktari"),
                        rs.getFloat("b_tas"),
                        rs.getFloat("cikan_miktar"),
                        rs.getFloat("iscilik"),
                        rs.getFloat("hesap"),
                        rs.getString("tarih"),
                        rs.getString("kaynakTablo")
                };
                finishedProductsTableModel.addRow(row);
            }
        } catch (SQLException e) {
            showErrorDialog("Biten ürünler yüklenirken bir hata oluştu.");
        }
    }

    private void filterTable() {
        String filter = filterField.getText();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(finishedProductsTableModel);
        finishedProductsTable.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filter));
    }

    private JTextField[] createFormFields(String tableName) {
        List<String> columns = getTableColumns(tableName);
        JTextField[] fields = new JTextField[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            fields[i] = new JTextField();
        }
        return fields;
    }

    private void addFormFieldsToPanel(JPanel panel, JTextField[] fields, GridBagConstraints gbc) {
        List<String> columns = getTableColumns("islemler");
        int fieldIndex = 0;
        for (String column : columns) {
            if (column.equalsIgnoreCase("id") || column.equalsIgnoreCase("tarih")) {
                continue; // Skip ID and Tarih columns
            }
            gbc.gridx = 0;
            gbc.gridy = fieldIndex;
            panel.add(new JLabel(column + ":"), gbc);
            gbc.gridx = 1;
            panel.add(fields[fieldIndex], gbc);
            fieldIndex++;
        }
    }

    private void addEntryToTable(String tableName, JTextField[] fields) {
        List<String> columns = getTableColumns(tableName);
        StringBuilder query = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder values = new StringBuilder("VALUES (");

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String currentDate = sdf.format(new Date());

        boolean firstColumn = true;
        int fieldIndex = 0; // Use a separate index for fields array
        for (String column : columns) {
            if (column.equalsIgnoreCase("id")) {
                continue; // Skip ID column
            }
            if (!firstColumn) {
                query.append(", ");
                values.append(", ");
            }
            query.append(column);

            String value = fields[fieldIndex].getText().trim();
            if (column.equalsIgnoreCase("tarih")) {
                values.append("'").append(currentDate).append("'");
            } else if (value.isEmpty()) {
                values.append("'0'");
            } else {
                values.append("'").append(value).append("'");
            }
            firstColumn = false;
            fieldIndex++; // Increment field index
        }

        query.append(") ").append(values).append(")");
        dbConnection.executeUpdate(query.toString());
        loadFinishedProducts(); // Refresh table after adding entry
    }

    private void addEntryToFileTable(String tableName, String miktar) {
        String query = "INSERT INTO FILE (TABLO, MIKTAR, TARIH) VALUES ('" + tableName + "', " + miktar + ", NOW())";
        dbConnection.executeUpdate(query);
        loadFileTable(); // Refresh table after adding entry
    }

    private void loadFileTable() {
        fileTableModel.setRowCount(0); // Clear existing rows
        for (String table : tables) {
            try {
                String query = "SELECT (SELECT COALESCE(SUM(FILE), 0) FROM " + table + ") - (SELECT COALESCE(SUM(MIKTAR), 0) FROM FILE WHERE TABLO = '" + table + "') AS fileDifference";
                ResultSet rs = dbConnection.get(query);
                if (rs != null && rs.next()) {
                    Object[] row = {table + "daki file miktarı", rs.getDouble("fileDifference")};
                    fileTableModel.addRow(row);
                }
            } catch (SQLException e) {
                showErrorDialog("File tablosu yüklenirken bir hata oluştu.");
            }
        }
    }

    private List<String> getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        ResultSet rs = dbConnection.get("SHOW COLUMNS FROM " + tableName);
        try {
            while (rs != null && rs.next()) {
                columns.add(rs.getString("Field"));
            }
        } catch (SQLException e) {
            showErrorDialog("Tablo sütunları alınırken bir hata oluştu.");
        }
        return columns;
    }

    private void showFormDialog(String title, String tableName) {
        showFormDialog(title, tableName, -1);
    }

    private void showFormDialog(String title, String tableName, int id) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new GridLayout(getTableColumns(tableName).size() + 1, 2));

        JTextField[] fields = createFormFields(tableName);
        addFormFieldsToDialog(dialog, fields, tableName, id);

        JButton saveButton = new JButton("Kaydet");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFormData(tableName, fields, id);
                dialog.dispose();
            }
        });

        dialog.add(new JLabel());
        dialog.add(saveButton);

        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addFormFieldsToDialog(JDialog dialog, JTextField[] fields, String tableName, int id) {
        List<String> columns = getTableColumns(tableName);
        int fieldIndex = 0;
        for (String column : columns) {
            if (column.equalsIgnoreCase("id")) {
                continue; // Skip ID column
            }
            dialog.add(new JLabel(column + ":"));
            dialog.add(fields[fieldIndex]);

            if (id != -1) {
                fillFieldData(fields[fieldIndex], tableName, column, id);
            }
            fieldIndex++;
        }
    }

    private void fillFieldData(JTextField field, String tableName, String columnName, int id) {
        String query = "SELECT " + columnName + " FROM " + tableName + " WHERE ID = " + id;
        ResultSet rs = dbConnection.get(query);
        try {
            if (rs != null && rs.next()) {
                field.setText(rs.getString(columnName));
            }
        } catch (SQLException e) {
            showErrorDialog("Alan verileri doldurulurken bir hata oluştu.");
        }
    }

    private void saveFormData(String tableName, JTextField[] fields, int id) {
        List<String> columns = getTableColumns(tableName);
        StringBuilder query = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String currentDate = sdf.format(new Date());

        if (id == -1) {
            query.append("INSERT INTO ").append(tableName).append(" (");
            StringBuilder values = new StringBuilder("VALUES (");

            boolean firstColumn = true;
            int fieldIndex = 0; // Use a separate index for fields array
            for (String column : columns) {
                if (column.equalsIgnoreCase("id")) {
                    continue; // Skip ID column
                }
                if (!firstColumn) {
                    query.append(", ");
                    values.append(", ");
                }
                query.append(column);

                String value = fields[fieldIndex].getText().trim();
                if (column.equalsIgnoreCase("tarih")) {
                    values.append("'").append(currentDate).append("'");
                } else if (value.isEmpty()) {
                    values.append("'0'");
                } else {
                    values.append("'").append(value).append("'");
                }
                firstColumn = false;
                fieldIndex++;
            }

            query.append(") ").append(values).append(")");
        } else {
            query.append("UPDATE ").append(tableName).append(" SET ");
            boolean firstColumn = true;
            int fieldIndex = 0; // Use a separate index for fields array
            for (String column : columns) {
                if (column.equalsIgnoreCase("id")) {
                    continue; // Skip ID column
                }
                if (!firstColumn) {
                    query.append(", ");
                }
                query.append(column).append(" = '").append(fields[fieldIndex].getText().trim().isEmpty() ? "0" : fields[fieldIndex].getText().trim()).append("'");
                firstColumn = false;
                fieldIndex++;
            }
            query.append(" WHERE ID = ").append(id);
        }

        dbConnection.executeUpdate(query.toString());
        if (tableName.equals("Musteri")) {
            loadCustomerList("");
        } else if (tableName.equals("islemler")) {
            loadFinishedProducts();
        }
    }

    private void showLoginDialog() {
        JDialog loginDialog = new JDialog(this, "Giriş", true);
        loginDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        loginDialog.add(new JLabel("Kullanıcı Adı:"), gbc);

        JTextField usernameField = new JTextField(15);
        gbc.gridx = 1;
        loginDialog.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        loginDialog.add(new JLabel("Şifre:"), gbc);

        JPasswordField passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        loginDialog.add(passwordField, gbc);

        JButton loginButton = new JButton("Giriş");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (authenticateUser(username, password)) {
                    loginDialog.dispose();
                    setVisible(true); // Show the main frame if login is successful
                    loadFileTable(); // Load the initial data for home page
                } else {
                    JOptionPane.showMessageDialog(loginDialog, "Geçersiz kullanıcı adı veya şifre", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add key listener for Enter key
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        });

        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        });

        gbc.gridx = 1;
        gbc.gridy = 2;
        loginDialog.add(loginButton, gbc);

        loginDialog.setSize(300, 200);
        loginDialog.setLocationRelativeTo(this);
        loginDialog.setVisible(true);
    }

    private boolean authenticateUser(String username, String password) {
        String query = "SELECT * FROM KULLANICILAR WHERE KULLANICI_ADI = '" + username + "' AND SIFRE = '" + password + "'";
        ResultSet rs = dbConnection.get(query);
        try {
            return rs != null && rs.next();
        } catch (SQLException e) {
            showErrorDialog("Kullanıcı doğrulama sırasında bir hata oluştu.");
        }
        return false;
    }

    private void editTableRow() {
        int selectedRow = customerDetailsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String id = customerDetailsTable.getValueAt(selectedRow, 0).toString();
            showFormDialog("Düzenle", "MusteriDetay", Integer.parseInt(id));
        } else {
            selectedRow = finishedProductsTable.getSelectedRow();
            if (selectedRow >= 0) {
                String id = finishedProductsTable.getValueAt(selectedRow, 0).toString();
                showFormDialog("Düzenle", "islemler", Integer.parseInt(id));
            }
        }
    }
    private void deleteTableRow() {
        int selectedRow = customerDetailsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String id = customerDetailsTable.getValueAt(selectedRow, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(this, "Kaydı silmek istediğinizden emin misiniz?", "Kayıt Sil", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String query = "DELETE FROM MusteriDetay WHERE ID = " + id;
                dbConnection.executeUpdate(query);
                showCustomerDetails(customerList.getSelectedIndex());
            }
        } else {
            selectedRow = finishedProductsTable.getSelectedRow();
            if (selectedRow >= 0) {
                String id = finishedProductsTable.getValueAt(selectedRow, 0).toString();
                int confirm = JOptionPane.showConfirmDialog(this, "Kaydı silmek istediğinizden emin misiniz?", "Kayıt Sil", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    String query = "DELETE FROM islemler WHERE ID = " + id;
                    dbConnection.executeUpdate(query);
                    loadFinishedProducts();
                }
            }
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Hata", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminPage adminPage = new AdminPage();
            adminPage.showLoginDialog(); // Show login dialog first
        });
    }
}
