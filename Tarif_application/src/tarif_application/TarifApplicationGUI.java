/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarif_application;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.ArrayList;
import static tarif_application.Tarif_application.loadMalzemeler;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import static tarif_application.Tarif_application.addNewMalzeme;
import static tarif_application.Tarif_application.addNewMalzemeWithFiyat;
import static tarif_application.Tarif_application.tarifOner;


public class TarifApplicationGUI {
    
    private boolean hazirlamaSureAscending = true;
    private boolean maliyetAscending=true;

    public void startGUI(DefaultListModel<String> tarifListesiModel, Connection conn) {
        
        
        // GUI bileşenlerini burada oluşturuyoruz
        JFrame frame = new JFrame("Tarif Rehberi Uygulaması");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

        // JSplitPane (Sol: Tarif Listesi, Sağ: Tarif Detayları)
        JSplitPane splitPane = new JSplitPane();

        // Sol taraf (Tarif Listesi)
        JList<String> tarifListesi = new JList<>(tarifListesiModel);
        splitPane.setLeftComponent(new JScrollPane(tarifListesi));

        // Sağ taraf (Tarif Detayları)
        JPanel detayPanel = new JPanel();
        detayPanel.setLayout(new BorderLayout());
        JTextArea tarifDetaylari = new JTextArea();
        tarifDetaylari.setEditable(false);  // Detayları sadece göstereceğiz
        tarifDetaylari.setText("Tarif detaylarını seçin...");
        detayPanel.add(new JScrollPane(tarifDetaylari), BorderLayout.CENTER);
        splitPane.setRightComponent(detayPanel);

// Tarif seçildiğinde detayları gösterme
tarifListesi.addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting()) {  // Bu, geçici seçimleri engeller
        String selectedTarif = tarifListesi.getSelectedValue();
        if (selectedTarif != null) {
            // Veritabanından seçilen tarifin detaylarını al
            String[] detaylar = Tarif_application.fetchTarifDetaylari(conn, selectedTarif);

            if (detaylar[0] != null) {
                // Veritabanından malzemeleri de alalım
                int tarifID = Tarif_application.getTarifIDByName(conn, selectedTarif);
                List<String> malzemeler = Tarif_application.getTarifMalzemeler(conn, tarifID);

                // Malzeme detaylarını hazırlıyoruz
                StringBuilder malzemeDetaylari = new StringBuilder();
                for (String malzeme : malzemeler) {
                    malzemeDetaylari.append(" • ").append(malzeme).append("\n");  // Malzemeleri yeni satırlara ekliyoruz
                }

                // Tarif maliyetini hesaplayalım
                float maliyet = Tarif_application.calculateTarifMaliyeti(conn, tarifID);

                // Tarife ait detayları, malzemeleri ve maliyeti gösterme
                tarifDetaylari.setText(
                    "KATEGORİ: " + detaylar[0] + "\n" +
                    "HAZIRLAMA SÜRESİ: " + detaylar[1] + " dakika\n\n" +
                    "TALİMATLAR:\n" + detaylar[2] + "\n\n" +
                    "MALZEMELER:\n" + malzemeDetaylari.toString() + "\n" +
                    "TOPLAM MALİYET: " + String.format("%.2f", maliyet) + " TL"
                );
            } else {
                tarifDetaylari.setText("Detaylar bulunamadı.");
            }
        }
    }
});



 // Sol tarafta tariflerin listelendiği JList için arka plan rengini ayarlayalım
// Sol tarafta tariflerin listelendiği JList için daha koyu bir gri arka plan rengi ayarlayalım
tarifListesi.setBackground(Color.darkGray);

tarifListesi.setOpaque(true);

        // Üst Menü (Arama ve Filtreleme)
        JPanel topPanel = new JPanel();
        JTextField aramaCubugu = new JTextField(20);
        JComboBox<String> kategoriFiltre = new JComboBox<>(new String[]{"Tüm Kategoriler", "Ana Yemek", "Tatlı", "Çorba"});
        topPanel.add(new JLabel("Ara:"));
        topPanel.add(aramaCubugu);
        topPanel.add(new JLabel("Kategori:"));
        topPanel.add(kategoriFiltre);
// Arama Çubuğuna KeyListener Ekleme
aramaCubugu.addKeyListener(new KeyAdapter() {
    @Override
    public void keyReleased(KeyEvent e) {
        String query = aramaCubugu.getText();
        // filtreleTarifler metodunu çağırıyoruz
        Tarif_application.filtreleTarifler(conn, tarifListesiModel, query, kategoriFiltre.getSelectedItem().toString());
    }
});

// Kategori Filtreleme
kategoriFiltre.addActionListener(e -> {
    String selectedCategory = kategoriFiltre.getSelectedItem().toString();
    // filtreleTarifler metodunu çağırıyoruz
    Tarif_application.filtreleTarifler(conn, tarifListesiModel, aramaCubugu.getText(), selectedCategory);
});
// Hazırlama Süresine Göre Sıralama Butonu
JButton hazirlamaSureSiralamaButton = new JButton("Süreye Göre Sırala");
topPanel.add(hazirlamaSureSiralamaButton);

// Maliyete Göre Sıralama Butonu
JButton maliyetSiralamaButton = new JButton("Maliyete Göre Sırala");
topPanel.add(maliyetSiralamaButton);

// Tarif Önerisi Butonu
JButton tarifOnerButton = new JButton("Tarif Önerisi");
topPanel.add(tarifOnerButton);
tarifOnerButton.setBackground(new Color(0x4CAF50));  // Yeşil arka plan
tarifOnerButton.setForeground(Color.WHITE);  // Beyaz yazı
tarifOnerButton.setFont(new Font("Arial", Font.BOLD, 14));  // Kalın ve büyük yazı tipi
tarifOnerButton.setBorderPainted(false);  // Kenarlığı kaldırır
tarifOnerButton.setFocusPainted(false);  // Düğmedeki odak çizgilerini kaldırır
tarifOnerButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));  // Yuvarlatılmış kenarlık
// Tarif Güncelle Butonu
JButton tarifGuncelleButton = new JButton("Tarif Güncelle");
topPanel.add(tarifGuncelleButton);
tarifGuncelleButton.setBackground(new Color(0x4CAF50));  // Yeşil arka plan
tarifGuncelleButton.setForeground(Color.WHITE);  // Beyaz yazı
tarifGuncelleButton.setFont(new Font("Arial", Font.BOLD, 14));  // Kalın ve büyük yazı tipi
tarifGuncelleButton.setBorderPainted(false);  // Kenarlığı kaldırır
tarifGuncelleButton.setFocusPainted(false);  // Düğmedeki odak çizgilerini kaldırır
tarifGuncelleButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));  // Yuvarlatılmış kenarlık

tarifListesi.setCellRenderer(new TarifListRenderer());  // Custom renderer ekleniyor
tarifListesi.setCellRenderer(new DefaultListCellRenderer() {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value.toString().contains("red")) {
            c.setForeground(Color.RED);
        } else if (value.toString().contains("green")) {
            c.setForeground(Color.GREEN);
        }
        return c;
    }
});

// Listeyi kaydırma paneliyle sarmalayalım
JScrollPane scrollPane = new JScrollPane(tarifListesi);
splitPane.setLeftComponent(scrollPane);

tarifListesi.setFixedCellHeight(40);  // Her hücre 40 piksel yüksekliğinde olacak
// Hazırlama Süresine Göre Sıralama Butonu İşlevi

hazirlamaSureSiralamaButton.addActionListener(e -> {
    hazirlamaSureAscending = !hazirlamaSureAscending; // Yönü değiştir

    // Güncel sıralama yönünü kontrol etmek için çıktı alalım
    System.out.println("Yeni sıralama yönü: " + (hazirlamaSureAscending ? "ASC" : "DESC"));

    String sql = "SELECT TarifAdi FROM Tarifler ORDER BY HazirlamaSuresi " + (hazirlamaSureAscending ? "ASC" : "DESC");
    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();

        // Modeli temizleyip sıralanmış sonuçları ekleyelim
        tarifListesiModel.clear();
        while (rs.next()) {
            tarifListesiModel.addElement(rs.getString("TarifAdi"));
        }

        rs.close();
        pstmt.close();
    } catch (SQLException ex) {
        System.out.println("Hazırlama süresine göre sıralama hatası: " + ex.getMessage());
    }
});

// Maliyete Göre Sıralama Butonu İşlevi
maliyetSiralamaButton.addActionListener(e -> {
    maliyetAscending = !maliyetAscending; // Yönü değiştir

    String sql = "SELECT t.TarifAdi, SUM(m.BirimFiyat * tm.MalzemeMiktar / 1000) AS ToplamMaliyet " +
                 "FROM Tarifler t " +
                 "JOIN tarif_malzeme tm ON t.TarifID = tm.TarifID " +
                 "JOIN malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                 "GROUP BY t.TarifID " +
                 "ORDER BY ToplamMaliyet " + (maliyetAscending ? "ASC" : "DESC");
    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();

        // Modeli temizleyip sıralanmış sonuçları ekleyelim
        tarifListesiModel.clear();
        while (rs.next()) {
            tarifListesiModel.addElement(rs.getString("TarifAdi"));
        }

        rs.close();
        pstmt.close();
    } catch (SQLException ex) {
        System.out.println("Maliyete göre sıralama hatası: " + ex.getMessage());
    }
});

// Tarif Güncelle Butonuna ActionListener Ekleme
tarifGuncelleButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        // Seçilen tarifin adını al
        String selectedTarif = tarifListesi.getSelectedValue();
        if (selectedTarif != null) {
            // Güncelleme penceresini aç
            showTarifGuncelleDialog(conn, selectedTarif, tarifListesiModel);
        } else {
            JOptionPane.showMessageDialog(frame, "Lütfen güncellemek için bir tarif seçin.");
        }
    }
});
//en son eklediğim
// Kullanıcının elindeki malzemeleri girebileceği alan:
// Tarif Önerisi Butonuna ActionListener ekleyelim
// Tarif Önerisi Butonuna ActionListener ekleyelim
tarifOnerButton.addActionListener(e -> {
    // Yeni bir JDialog penceresi oluştur
    JDialog tarifOneriDialog = new JDialog(frame, "Tarif Önerisi", true);
    tarifOneriDialog.setSize(400, 400);
    tarifOneriDialog.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Malzeme girişi için alanlar
    JLabel malzemeLabel = new JLabel("Malzeme Adı:");
    JTextField malzemeField = new JTextField(15);
    JLabel miktarLabel = new JLabel("Miktar (g):");
    JTextField miktarField = new JTextField(10);
    
    // Eklenen malzemeleri göstermek için liste modeli
    DefaultListModel<String> kullaniciMalzemeleriModel = new DefaultListModel<>();
    JList<String> kullaniciMalzemeleriList = new JList<>(kullaniciMalzemeleriModel);
    JScrollPane malzemeScroll = new JScrollPane(kullaniciMalzemeleriList);

    // Malzeme ekle butonu
    JButton malzemeEkleButton = new JButton("Malzeme Ekle");
    malzemeEkleButton.addActionListener(event -> {
        String malzemeAdi = malzemeField.getText();
        String miktar = miktarField.getText();
        if (!malzemeAdi.isEmpty() && !miktar.isEmpty()) {
            kullaniciMalzemeleriModel.addElement(malzemeAdi + " - " + miktar + "g");
            malzemeField.setText("");
            miktarField.setText("");
        } else {
            JOptionPane.showMessageDialog(tarifOneriDialog, "Lütfen malzeme adı ve miktarı girin.", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    });

    // Önerileri Göster ve İptal butonları
    JButton onerileriGosterButton = new JButton("Önerileri Göster");
    JButton iptalButton = new JButton("İptal");

    // İptal butonuna basıldığında pencereyi kapatalım
    iptalButton.addActionListener(event -> tarifOneriDialog.dispose());

    // Önerileri göster butonuna basıldığında tarif önerilerini göster
    onerileriGosterButton.addActionListener(event -> {
        List<String> kullaniciMalzemeleri = new ArrayList<>();
        for (int i = 0; i < kullaniciMalzemeleriModel.size(); i++) {
            String malzeme = kullaniciMalzemeleriModel.get(i).split(" - ")[0];  // Sadece malzeme adı alınıyor
            kullaniciMalzemeleri.add(malzeme);
        }

        // Tarif önerisini hesapla ve göster (bu fonksiyonu daha önce oluşturmuştuk)
        Tarif_application.tarifOner(conn, kullaniciMalzemeleri, tarifListesiModel);
// Eğer tarif önerileri yoksa bilgi verelim
    if (tarifListesiModel.isEmpty()) {
        JOptionPane.showMessageDialog(frame, "Eşleşen tarif bulunamadı!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
    } else {
        JOptionPane.showMessageDialog(frame, "Eşleşen tarifler başarıyla gösterildi!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
    }
        // Pencereyi kapat
        tarifOneriDialog.dispose();
        tarifListesi.repaint();
tarifListesi.revalidate();
    });

    // Layout ayarlamaları
    gbc.gridx = 0;
    gbc.gridy = 0;
    tarifOneriDialog.add(malzemeLabel, gbc);
    gbc.gridx = 1;
    tarifOneriDialog.add(malzemeField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    tarifOneriDialog.add(miktarLabel, gbc);
    gbc.gridx = 1;
    tarifOneriDialog.add(miktarField, gbc);

    gbc.gridx = 1;
    gbc.gridy = 2;
    tarifOneriDialog.add(malzemeEkleButton, gbc);

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.BOTH;
    tarifOneriDialog.add(malzemeScroll, gbc);

    // Önerileri Göster ve İptal butonlarını ekleyelim
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(onerileriGosterButton);
    buttonPanel.add(iptalButton);

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    tarifOneriDialog.add(buttonPanel, gbc);

    // Pencereyi görünür yap
    tarifOneriDialog.setVisible(true);
});



// Üst Panelin Sonuna "Malzeme Ekle" Butonunu Ekleyelim
JButton malzemeEkleButton = new JButton("Malzeme Ekle");
topPanel.add(malzemeEkleButton);
malzemeEkleButton.setBackground(new Color(0x4CAF50));  // Yeşil arka plan
malzemeEkleButton.setForeground(Color.WHITE);  // Beyaz yazı
malzemeEkleButton.setFont(new Font("Arial", Font.BOLD, 14));  // Kalın ve büyük yazı tipi
malzemeEkleButton.setBorderPainted(false);  // Kenarlığı kaldırır
malzemeEkleButton.setFocusPainted(false);  // Düğmedeki odak çizgilerini kaldırır
malzemeEkleButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));  // Yuvarlatılmış kenarlık

// Malzeme Ekle Butonuna ActionListener Ekle
malzemeEkleButton.addActionListener(e -> {
    // Malzeme eklemek için dialog açıyoruz
    showMalzemeEkleDialog(conn);
});

// **Tarif Sil Butonu eklemesi ve ActionListener**
    JButton tarifSilButton = new JButton("Tarif Sil");
    topPanel.add(tarifSilButton);
    tarifSilButton.setBackground(new Color(0x4CAF50));  // Yeşil arka plan
tarifSilButton.setForeground(Color.WHITE);  // Beyaz yazı
tarifSilButton.setFont(new Font("Arial", Font.BOLD, 14));  // Kalın ve büyük yazı tipi
tarifSilButton.setBorderPainted(false);  // Kenarlığı kaldırır
tarifSilButton.setFocusPainted(false);  // Düğmedeki odak çizgilerini kaldırır
tarifSilButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));  // Yuvarlatılmış kenarlık


    // Tarif Sil Butonuna ActionListener Ekleme
    tarifSilButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Seçilen tarifin adını al
            String selectedTarif = tarifListesi.getSelectedValue();
            if (selectedTarif != null) {
                int confirm = JOptionPane.showConfirmDialog(frame, "Tarifi silmek istediğinize emin misiniz?", "Tarif Sil", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    // Veritabanından tarifi sil
                    Tarif_application.deleteTarif(conn, selectedTarif);

                    // Listeden tarifi sil
                    tarifListesiModel.removeElement(selectedTarif);  // Listeden silme işlemi
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Lütfen silmek için bir tarif seçin.");
            }
        }
    });
   
    
        // Tarif Ekle Butonu
        JButton tarifEkleButton = new JButton("Tarif Ekle");
        topPanel.add(tarifEkleButton);
        tarifEkleButton.setBackground(new Color(0x4CAF50));  // Yeşil arka plan
        tarifEkleButton.setForeground(Color.WHITE);  // Beyaz yazı
tarifEkleButton.setFont(new Font("Arial", Font.BOLD, 14));  // Kalın ve büyük yazı tipi
tarifEkleButton.setBorderPainted(false);  // Kenarlığı kaldırır
tarifEkleButton.setFocusPainted(false);  // Düğmedeki odak çizgilerini kaldırır
tarifEkleButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));  // Yuvarlatılmış kenarlık



        // Tarif Ekle Butonuna ActionListener Ekleme
        tarifEkleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Tarif ekleme penceresini aç
                showTarifEkleDialog(conn, tarifListesiModel);
            }
        });
tarifListesi.setCellRenderer(new DefaultListCellRenderer() {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // Tarif ID'yi tarifin adından alarak eksik malzeme kontrolü yap
        String tarifAdi = value.toString();
        int tarifID = Tarif_application.getTarifIDByName(conn, tarifAdi);

        // Eksik malzeme var mı kontrolü
        boolean eksikMalzemeVar = Tarif_application.tarifteEksikMalzemeVarMi(conn, tarifID);
        System.out.println("Tarif: " + tarifAdi + " - Eksik malzeme var mı? " + eksikMalzemeVar);

        // Eksik malzeme varsa kırmızı, değilse yeşil
        if (eksikMalzemeVar) {
            c.setForeground(Color.RED);
        } else {
            c.setForeground(Color.GREEN);
        }

        return c;
    }
});







        // Ana pencere düzeni
        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(splitPane, BorderLayout.CENTER);

        // Pencereyi görünür yapma
        frame.setVisible(true);
    }
   

private void showMalzemeEkleDialog(Connection conn) {
    JDialog malzemeDialog = new JDialog();
    malzemeDialog.setTitle("Malzeme Ekle");
    malzemeDialog.setSize(400, 350);
    
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // **Mevcut malzeme listesi modeli**
    JLabel malzemelerLabel = new JLabel("Mevcut Malzemeler:");
    DefaultListModel<String> malzemeListModel = new DefaultListModel<>();
    JList<String> malzemeListesi = new JList<>(malzemeListModel);
    JScrollPane malzemeScroll = new JScrollPane(malzemeListesi);
    malzemeListesi.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    loadMalzemeler(conn, malzemeListModel);  // Mevcut malzemeleri yükleyelim
// Malzeme Ekle ve Sil Butonlarını bir panelde tutalım
JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
JButton malzemeEkleButton = new JButton("Malzeme Ekle");
JButton silButton = new JButton("Sil");

buttonPanel.add(malzemeEkleButton);
buttonPanel.add(silButton);

gbc.gridx = 1;
gbc.gridy = 5; // Butonları uygun bir satıra ekleyin
gbc.gridwidth = 2; // Butonları yan yana yerleştirmek için genişliği ayarlayın
panel.add(buttonPanel, gbc);
    // **Panel Düzeni**
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(malzemelerLabel, gbc);  // Malzemeler başlığını ekle
    
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.BOTH;  // Scrollable yapmak için hem yatay hem dikey doldur
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    panel.add(malzemeScroll, gbc);  // Malzeme listesini ekle

    // **Yeni Malzeme Adı, Miktar ve Birim Fiyat**
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0;
    gbc.weighty = 0;
    panel.add(new JLabel("Malzeme Adı:"), gbc);

    gbc.gridx = 1;
    gbc.gridy = 2;
    JTextField malzemeAdiField = new JTextField(15);
    panel.add(malzemeAdiField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 3;
    panel.add(new JLabel("Miktar (g):"), gbc);

    gbc.gridx = 1;
    gbc.gridy = 3;
    JTextField miktarField = new JTextField(10);
    panel.add(miktarField, gbc);

    // Birim Fiyat alanı ekleyelim
    gbc.gridx = 0;
    gbc.gridy = 4;
    panel.add(new JLabel("Birim Fiyat (TL):"), gbc);

    gbc.gridx = 1;
    gbc.gridy = 4;
    JTextField birimFiyatField = new JTextField(10);
    panel.add(birimFiyatField, gbc);

   
    silButton.addActionListener(e -> {
    String selectedMalzeme = malzemeListesi.getSelectedValue();
    if (selectedMalzeme != null) {
        int confirm = JOptionPane.showConfirmDialog(malzemeDialog, "Bu malzemeyi silmek istediğinize emin misiniz?", "Malzeme Sil", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Veritabanından seçili malzemeyi sil
                Tarif_application.deleteMalzeme(conn, selectedMalzeme);
                
                // Listeyi güncelle
                malzemeListModel.removeElement(selectedMalzeme);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(malzemeDialog, "Silme işleminde hata: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    } else {
        JOptionPane.showMessageDialog(malzemeDialog, "Lütfen silmek için bir malzeme seçin.");
    }
});
    
    // Malzeme ekleme işlemi
    malzemeEkleButton.addActionListener(e -> {
        String malzemeAdi = malzemeAdiField.getText();
        String miktar = miktarField.getText();
        String birimFiyat = birimFiyatField.getText();
        
        if (!malzemeAdi.isEmpty() && !miktar.isEmpty() && !birimFiyat.isEmpty()) {
            try {
                // Veritabanına yeni malzemeyi ekle
                Tarif_application.addNewMalzemeWithFiyat(conn, malzemeAdi, miktar, birimFiyat);
                
                // Listeye ekleyelim
                malzemeListModel.addElement(malzemeAdi);  // Yeni malzemeyi listeye ekle
                malzemeAdiField.setText("");
                miktarField.setText("");
                birimFiyatField.setText("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(malzemeDialog, "Hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(malzemeDialog, "Lütfen tüm alanları doldurun.", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    });

    malzemeDialog.add(panel);
    malzemeDialog.setVisible(true);
}

    // Tarif Güncelleme Penceresi (Form)
private void showTarifGuncelleDialog(Connection conn, String selectedTarif, DefaultListModel<String> listModel) {
    // Mevcut tarif bilgilerini al
    String[] mevcutDetaylar = Tarif_application.fetchTarifDetaylari(conn, selectedTarif);

    // Güncelleme penceresi oluştur
    JDialog dialog = new JDialog();
    dialog.setTitle("Tarif Güncelle");
    dialog.setSize(700, 600);  // Pencere boyutunu artırdık

    // **GridBagLayout'u ayarlıyoruz**
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);  // Bileşenler arasına boşluklar ekleyelim
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;  // Bileşenleri sola hizalayalım

    // Form alanları
    JTextField tarifAdiField = new JTextField(selectedTarif); // Tarif adını göster
    JComboBox<String> kategoriBox = new JComboBox<>(new String[]{"Ana Yemek", "Tatlı", "Çorba"});
    kategoriBox.setSelectedItem(mevcutDetaylar[0]); // Mevcut kategori
    JTextField hazirlamaSuresiField = new JTextField(mevcutDetaylar[1]); // Mevcut süre
    JTextArea talimatlarArea = new JTextArea(mevcutDetaylar[2],8,30); // Mevcut talimatlar
  talimatlarArea.setLineWrap(true); // Satır sonuna geldiğinde alt satıra geçsin
talimatlarArea.setWrapStyleWord(true); // Kelime bütünlüğü sağlasın


    // Tarif adı
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(new JLabel("Tarif Adı:"), gbc);
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    panel.add(tarifAdiField, gbc);

    // Kategori
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    panel.add(new JLabel("Kategori:"), gbc);
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    panel.add(kategoriBox, gbc);

    // Hazırlama Süresi
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    panel.add(new JLabel("Hazırlama Süresi (dk):"), gbc);
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    panel.add(hazirlamaSuresiField, gbc);

    // Talimatlar
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 1;
    panel.add(new JLabel("Talimatlar:"), gbc);
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    panel.add(new JScrollPane(talimatlarArea), gbc);

    // **Veritabanından mevcut malzemeleri alıyoruz**
    int tarifID = Tarif_application.getTarifIDByName(conn, selectedTarif);
    List<String> mevcutMalzemeler = Tarif_application.getTarifMalzemeler(conn, tarifID);

    // **Malzeme Listesi ve Miktar Güncelleme Alanı**
    JPanel malzemePanel = new JPanel(new GridBagLayout());  // Malzemeler için ayrı bir panel
    GridBagConstraints malzemeGbc = new GridBagConstraints();
    malzemeGbc.insets = new Insets(2, 2, 2, 2);  // Malzeme alanları için boşluklar

    List<JTextField> malzemeMiktarFieldList = new ArrayList<>();
    for (int i = 0; i < mevcutMalzemeler.size(); i++) {
        String[] malzemeBilgisi = mevcutMalzemeler.get(i).split(" - ");
        String malzemeAdi = malzemeBilgisi[0];
        String miktar = malzemeBilgisi[1].replace("g", "").trim();

        JLabel malzemeLabel = new JLabel(malzemeAdi);
        JTextField miktarField = new JTextField(miktar);

        // Sil Butonu
        JButton silButton = new JButton("Sil");
        int index = i;
        silButton.addActionListener(e -> {
            mevcutMalzemeler.remove(index);
            malzemeMiktarFieldList.remove(index);
            malzemePanel.remove(malzemeLabel);
            malzemePanel.remove(miktarField);
            malzemePanel.remove(silButton);
            dialog.revalidate();
            dialog.repaint();
        });

        // Malzeme adını ve sil butonunu hizalayalım
        malzemeGbc.gridx = 0;
        malzemeGbc.gridy = i;
        malzemePanel.add(malzemeLabel, malzemeGbc);

        malzemeGbc.gridx = 1;
        malzemePanel.add(miktarField, malzemeGbc);

        malzemeGbc.gridx = 2;
        malzemePanel.add(silButton, malzemeGbc);

        malzemeMiktarFieldList.add(miktarField);
    }

    // **Malzeme Ekleme Alanları**
    JTextField yeniMalzemeField = new JTextField(10);  // Yeni malzeme adı
    JTextField yeniMiktarField = new JTextField(5);    // Yeni malzeme miktarı
    JButton malzemeEkleButton = new JButton("Malzeme Ekle");

    malzemeEkleButton.addActionListener(e -> {
        String malzemeAdi = yeniMalzemeField.getText();
        String gramaj = yeniMiktarField.getText();
        if (!malzemeAdi.isEmpty() && !gramaj.isEmpty()) {
            mevcutMalzemeler.add(malzemeAdi + " - " + gramaj + "g");
            malzemePanel.add(new JLabel(malzemeAdi));
            JTextField miktarField = new JTextField(gramaj);
            malzemeMiktarFieldList.add(miktarField);
            malzemePanel.add(miktarField);
            dialog.revalidate();
            dialog.repaint();
            yeniMalzemeField.setText("");
            yeniMiktarField.setText("");
        } else {
            JOptionPane.showMessageDialog(dialog, "Lütfen malzeme ve gramaj girin.", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    });

    // Yeni malzeme ekleme alanları
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 1;
    panel.add(new JLabel("Yeni Malzeme:"), gbc);
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    panel.add(yeniMalzemeField, gbc);

    gbc.gridx = 2;
    panel.add(new JLabel("Miktar (g):"), gbc);
    gbc.gridx = 3;
    panel.add(yeniMiktarField, gbc);

    gbc.gridx = 4;
    panel.add(malzemeEkleButton, gbc);

    // Malzeme panelini ekleyelim
    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.gridwidth = 4;
    panel.add(new JLabel("Malzemeler:"), gbc);
    gbc.gridy = 6;
    panel.add(malzemePanel, gbc);

    // Kaydet ve İptal Butonları
    JButton kaydetButton = new JButton("Kaydet");
    JButton iptalButton = new JButton("İptal");

    gbc.gridx = 0;
    gbc.gridy = 7;
    gbc.gridwidth = 2;
    panel.add(kaydetButton, gbc);

    gbc.gridx = 2;
    gbc.gridwidth = 2;
    panel.add(iptalButton, gbc);

    // Kaydet Butonu için ActionListener
    kaydetButton.addActionListener(e -> {
        String tarifAdi = tarifAdiField.getText();
        String kategori = kategoriBox.getSelectedItem().toString();
        int hazirlamaSuresi = Integer.parseInt(hazirlamaSuresiField.getText());
        String talimatlar = talimatlarArea.getText();

        List<String> malzemeListesi = new ArrayList<>();
        for (int i = 0; i < mevcutMalzemeler.size(); i++) {
            String malzemeAdi = mevcutMalzemeler.get(i).split(" - ")[0];
            String miktar = malzemeMiktarFieldList.get(i).getText();
            malzemeListesi.add(malzemeAdi + " - " + miktar + "g");
        }

        Tarif_application.updateTarif(conn, selectedTarif, tarifAdi, kategori, hazirlamaSuresi, talimatlar, malzemeListesi);

        dialog.dispose();
    });

    // İptal Butonu: Pencereyi kapat
    iptalButton.addActionListener(e -> dialog.dispose());

    dialog.add(panel);  // Paneli dialoga ekliyoruz
    dialog.setVisible(true);
}


    // Tarif Ekleme Penceresi (Form)
    private void showTarifEkleDialog(Connection conn, DefaultListModel<String> listModel) {
     JDialog dialog = new JDialog();
    dialog.setTitle("Yeni Tarif Ekle");
    dialog.setSize(1200, 800);

    // Panel ve GridBagLayout oluşturuluyor
    JPanel panel = new JPanel();
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    panel.setLayout(layout);

    // Form alanları
    JLabel tarifAdiLabel = new JLabel("Tarif Adı:");
    JTextField tarifAdiField = new JTextField(20);
    JLabel kategoriLabel = new JLabel("Kategori:");
    JComboBox<String> kategoriBox = new JComboBox<>(new String[]{"Ana Yemek", "Tatlı", "Çorba"});
    JLabel hazirlamaSuresiLabel = new JLabel("Hazırlama Süresi (dk):");
    JTextField hazirlamaSuresiField = new JTextField(5);
    JLabel talimatlarLabel = new JLabel("Talimatlar:");
    JTextArea talimatlarArea = new JTextArea(4, 30);
    JScrollPane talimatlarScroll = new JScrollPane(talimatlarArea);
    talimatlarArea.setLineWrap(true); // Satır sonuna geldiğinde alt satıra geçsin
talimatlarArea.setWrapStyleWord(true); // Kelime bütünlüğü sağlasın

    // Malzeme Listesi
    JLabel malzemelerLabel = new JLabel("Malzemeler:");
    DefaultListModel<String> malzemeListModel = new DefaultListModel<>();
    JList<String> malzemeListesi = new JList<>(malzemeListModel);
    JScrollPane malzemeScroll = new JScrollPane(malzemeListesi);
    malzemeListesi.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    loadMalzemeler(conn, malzemeListModel);

    // Malzeme ve gramaj giriş alanları
    JLabel malzemeAdiLabel = new JLabel("Malzeme Adı:");
    JTextField malzemeField = new JTextField(10);
    JLabel gramajLabel = new JLabel("Gramaj:");
    JTextField gramajField = new JTextField(5);

    // Eklenen malzemeleri göstermek için bir model ve liste
    JLabel eklenenMalzemelerLabel = new JLabel("Eklenen Malzemeler:");
    DefaultListModel<String> eklenenMalzemelerModel = new DefaultListModel<>();
    JList<String> eklenenMalzemelerListesi = new JList<>(eklenenMalzemelerModel);
    JScrollPane eklenenMalzemelerScroll = new JScrollPane(eklenenMalzemelerListesi);
    

    // Malzeme ekle butonu
    JButton malzemeEkleButton = new JButton("Malzeme Ekle");
    malzemeEkleButton.addActionListener(e -> {
        String malzemeAdi = malzemeField.getText();
        String gramaj = gramajField.getText();
        if (!malzemeAdi.isEmpty() && !gramaj.isEmpty()) {
            eklenenMalzemelerModel.addElement(malzemeAdi + " - " + gramaj + "g");
            malzemeField.setText("");
            gramajField.setText("");
        } else {
            JOptionPane.showMessageDialog(dialog, "Lütfen malzeme ve gramaj girin.", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    });

    // Kaydet ve İptal Butonları
    JButton kaydetButton = new JButton("Kaydet");
    JButton iptalButton = new JButton("İptal");

    // GridBagLayout ile bileşenleri ekleme
    gbc.insets = new Insets(5, 5, 5, 5);  // Bileşenler arasına boşluk ekleyelim
    gbc.fill = GridBagConstraints.HORIZONTAL;

    // Tarif Adı
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel.add(tarifAdiLabel, gbc);
    gbc.gridx = 1;
    panel.add(tarifAdiField, gbc);

    // Kategori
    gbc.gridx = 0;
    gbc.gridy = 1;
    panel.add(kategoriLabel, gbc);
    gbc.gridx = 1;
    panel.add(kategoriBox, gbc);

    // Hazırlama Süresi
    gbc.gridx = 0;
    gbc.gridy = 2;
    panel.add(hazirlamaSuresiLabel, gbc);
    gbc.gridx = 1;
    panel.add(hazirlamaSuresiField, gbc);

    // Talimatlar
    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.anchor = GridBagConstraints.NORTH;
    panel.add(talimatlarLabel, gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(talimatlarScroll, gbc);

    // Malzemeler
    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(malzemelerLabel, gbc);
    gbc.gridx = 1;
    panel.add(malzemeScroll, gbc);

    // Malzeme Adı ve Gramaj
    gbc.gridx = 0;
    gbc.gridy = 5;
    panel.add(malzemeAdiLabel, gbc);
    gbc.gridx = 1;
    panel.add(malzemeField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 6;
    panel.add(gramajLabel, gbc);
    gbc.gridx = 1;
    panel.add(gramajField, gbc);

    // Malzeme Ekle Butonu
    gbc.gridx = 1;
    gbc.gridy = 7;
    panel.add(malzemeEkleButton, gbc);

    // Eklenen Malzemeler
    gbc.gridx = 0;
    gbc.gridy = 8;
    panel.add(eklenenMalzemelerLabel, gbc);
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(eklenenMalzemelerScroll, gbc);

    // Kaydet ve İptal Butonları
    gbc.gridx = 0;
    gbc.gridy = 9;
    panel.add(kaydetButton, gbc);
    gbc.gridx = 1;
    panel.add(iptalButton, gbc);
    

   kaydetButton.addActionListener(e -> {
    try {
        String tarifAdi = tarifAdiField.getText();
        String kategori = kategoriBox.getSelectedItem().toString();
        String hazirlamaSuresiText = hazirlamaSuresiField.getText(); // Hazırlama süresi
        String talimatlar = talimatlarArea.getText();

        // Hazırlama süresi kontrolü
        if (hazirlamaSuresiText == null || hazirlamaSuresiText.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Lütfen hazırlama süresini girin.", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int hazirlamaSuresi = Integer.parseInt(hazirlamaSuresiText);

        // Eklenen malzemeleri ve gramajları al
        List<String> eklenenMalzemeler = new ArrayList<>();
        for (int i = 0; i < eklenenMalzemelerModel.size(); i++) {
            String malzemeVeMiktar = eklenenMalzemelerModel.get(i);
            
            // Boşluk veya geçersiz veri var mı kontrol et
            if (malzemeVeMiktar == null || malzemeVeMiktar.trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Lütfen tüm malzemeleri ve miktarları girin.", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }

            eklenenMalzemeler.add(malzemeVeMiktar);
        }

        // Duplicate kontrolü yapılacak
        String checkSql = "SELECT COUNT(*) FROM Tarifler WHERE TarifAdi = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setString(1, tarifAdi);
        ResultSet checkRs = checkStmt.executeQuery();
        
        checkRs.next();
        int count = checkRs.getInt(1);  // Aynı tarif adından kaç tane olduğunu al

        if (count > 0) {
            // Eğer aynı tarif zaten mevcutsa eklemeyi durdur
            JOptionPane.showMessageDialog(dialog, "Bu tarif zaten mevcut, aynı tarifi tekrar ekleyemezsiniz.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            checkRs.close();
            checkStmt.close();
            return;  // Fonksiyondan çıkış yapıyoruz, yeni tarif eklenmiyor
        }

        checkRs.close();
        checkStmt.close();

        // Tarifi ve malzemeleri veritabanına ekle
        Tarif_application.addTarif(conn, tarifAdi, kategori, hazirlamaSuresi, talimatlar, eklenenMalzemeler);

        // Listeyi güncelle (duplicate değilse listeye ekle)
        listModel.addElement(tarifAdi);

        dialog.dispose();  // Pencereyi kapat
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(dialog, "Lütfen sayısal değerler doğru girilsin.", "Hata", JOptionPane.ERROR_MESSAGE);
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(dialog, "Tarif eklenirken bir hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
    }
});


    // İptal Butonu: Pencereyi kapat
    iptalButton.addActionListener(e -> dialog.dispose());

    dialog.add(panel);
    dialog.setVisible(true);
    }
}
