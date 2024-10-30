/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package tarif_application;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.DefaultListModel;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class Tarif_application {

    
    
    public static void main(String[] args) {
        try {
    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
            UIManager.setLookAndFeel(info.getClassName());
            break;
        }
    }
} catch (Exception e) {
    e.printStackTrace();
}
        Font genelFont = new Font("Arial", Font.PLAIN, 14);  // Genel yazı tipi ayarı
UIManager.put("Label.font", genelFont);
UIManager.put("Button.font", genelFont);
UIManager.put("TextField.font", genelFont);
        // Veritabanı bağlantısını kontrol etmek için
        Connection conn = DatabaseConnection.connect();
            testDatabaseConnection(conn); // Bağlantıyı test ediyoruz
             testMalzemeMiktarlari(conn);

        if (conn != null) {
            System.out.println("Veritabanına başarılı bir şekilde bağlandı!");

            // GUI'yi başlat
            TarifApplicationGUI gui = new TarifApplicationGUI();
            DefaultListModel<String> tarifListesiModel = new DefaultListModel<>();
            gui.startGUI(tarifListesiModel,conn); // GUI'yi çalıştır ve modeli geçir

            // Tarifleri veritabanından çekip listeye ekleyelim
            fetchTarifler(conn, tarifListesiModel);

        } else {
            System.out.println("Veritabanı bağlantısı başarısız.");
        }
        
    }

      public static void testDatabaseConnection(Connection conn) {
    if (conn != null) {
        System.out.println("Veritabanı bağlantısı başarılı.");
    } else {
        System.out.println("Veritabanı bağlantısı başarısız.");
    }
}
    
    public static void testMalzemeMiktarlari(Connection conn) {
    String sql = "SELECT MalzemeAdi, ToplamMiktar FROM malzemeler";
    
    try {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        
        while (rs.next()) {
            String malzemeAdi = rs.getString("MalzemeAdi");
            float miktar = rs.getFloat("ToplamMiktar");
            System.out.println("Malzeme: " + malzemeAdi + ", Miktar: " + miktar + "g");
        }

        rs.close();
        stmt.close();
    } catch (SQLException e) {
        System.out.println("Veri çekme hatası: " + e.getMessage());
    }
}
  

  
    
    // Veritabanından tarifleri çeken fonksiyon
    public static void fetchTarifler(Connection conn, DefaultListModel<String> listModel) {
        String sql = "SELECT TarifAdi FROM Tarifler";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            // Sonuçları model listesine ekle
            while (rs.next()) {
                String tarifAdi = rs.getString("TarifAdi");
                listModel.addElement(tarifAdi); // Her bir tarif adını listeye ekliyoruz
            }

            rs.close();
            stmt.close();
        } catch (Exception e) {
            System.out.println("Tarifleri çekerken hata oluştu: " + e.getMessage());
        }
    }
    public static void addTarif(Connection conn, String tarifAdi, String kategori, int hazirlamaSuresi, String talimatlar, List<String> malzemeListesi) {
    // Duplicate kontrolü için SQL sorgusu
    String checkSql = "SELECT COUNT(*) FROM Tarifler WHERE TarifAdi = ?";
    
    try {
        // Tarifin veritabanında olup olmadığını kontrol et
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setString(1, tarifAdi);
        ResultSet checkRs = checkStmt.executeQuery();
        
        checkRs.next();
        int count = checkRs.getInt(1);  // Aynı tarif adından kaç tane olduğunu al

        if (count > 0) {
            // Eğer aynı tarif zaten mevcutsa eklemeyi durdur
            System.out.println("Bu tarif zaten mevcut, aynı tarifi tekrar ekleyemezsiniz.");
            JOptionPane.showMessageDialog(null, "Bu tarif zaten mevcut!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            checkRs.close();
            checkStmt.close();
            return;  // Fonksiyondan çıkış yapıyoruz, yeni tarif eklenmiyor
        }

        checkRs.close();
        checkStmt.close();
        
        // Eğer tarif duplicate değilse, yeni tarif eklemeye devam ediyoruz
        String sqlTarif = "INSERT INTO Tarifler (TarifAdi, Kategori, HazirlamaSuresi, Talimatlar) VALUES (?, ?, ?, ?)";
        
        PreparedStatement pstmt = conn.prepareStatement(sqlTarif, Statement.RETURN_GENERATED_KEYS);
        pstmt.setString(1, tarifAdi);
        pstmt.setString(2, kategori);
        pstmt.setInt(3, hazirlamaSuresi);
        pstmt.setString(4, talimatlar);
        pstmt.executeUpdate();

        // Eklenen tarife ait ID'yi al
        ResultSet rs = pstmt.getGeneratedKeys();
        int tarifID = -1;
        if (rs.next()) {
            tarifID = rs.getInt(1);  // Tarif ID'sini alıyoruz
        }

        rs.close();
        pstmt.close();

        // Seçilen malzemeleri tarif-malzeme tablosuna ekle
        for (String malzemeVeGramaj : malzemeListesi) {
            String[] parcalar = malzemeVeGramaj.split(" - ");
            String malzemeAdi = parcalar[0];
            float miktar = Float.parseFloat(parcalar[1].replaceAll("[^0-9.]", ""));
            int malzemeID = getMalzemeIDByName(conn, malzemeAdi);  // Malzeme adından ID'yi bul
            addTarifMalzeme(conn, tarifID, malzemeID, miktar);
        }

        System.out.println("Tarif başarıyla eklendi!");

    } catch (SQLException e) {
        System.out.println("Tarif ekleme hatası: " + e.getMessage());
    }
}

    private void showYeniMalzemeDialog(Connection conn, DefaultListModel<String> malzemeListModel) {
    JDialog yeniMalzemeDialog = new JDialog();
    yeniMalzemeDialog.setTitle("Yeni Malzeme Ekle");
    yeniMalzemeDialog.setSize(300, 200);

    JPanel yeniMalzemePanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel malzemeAdiLabel = new JLabel("Malzeme Adı:");
    JTextField malzemeAdiField = new JTextField(15);
    JLabel miktarLabel = new JLabel("Miktar (g):");
    JTextField miktarField = new JTextField(10);

    JButton kaydetMalzemeButton = new JButton("Kaydet");
    kaydetMalzemeButton.addActionListener(e -> {
        String malzemeAdi = malzemeAdiField.getText();
        String miktar = miktarField.getText();

        if (!malzemeAdi.isEmpty() && !miktar.isEmpty()) {
            try {
                // Veritabanına yeni malzemeyi ekle
                addNewMalzeme(conn, malzemeAdi, miktar);
                
                // Malzeme listesini güncelle
                malzemeListModel.addElement(malzemeAdi);

                yeniMalzemeDialog.dispose();  // Diyaloğu kapat
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(yeniMalzemeDialog, "Malzeme eklerken hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(yeniMalzemeDialog, "Lütfen malzeme adı ve miktarı girin.", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    });

    // Yeni malzeme paneline ekleme
    gbc.gridx = 0;
    gbc.gridy = 0;
    yeniMalzemePanel.add(malzemeAdiLabel, gbc);
    gbc.gridx = 1;
    yeniMalzemePanel.add(malzemeAdiField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    yeniMalzemePanel.add(miktarLabel, gbc);
    gbc.gridx = 1;
    yeniMalzemePanel.add(miktarField, gbc);

    gbc.gridx = 1;
    gbc.gridy = 2;
    yeniMalzemePanel.add(kaydetMalzemeButton, gbc);

    yeniMalzemeDialog.add(yeniMalzemePanel);
    yeniMalzemeDialog.setVisible(true);
}

public static void addNewMalzeme(Connection conn, String malzemeAdi, String miktar) throws SQLException {
    String sql = "INSERT INTO malzemeler (MalzemeAdi, ToplamMiktar, MalzemeBirim) VALUES (?, ?, ?)";
    PreparedStatement pstmt = conn.prepareStatement(sql);
    pstmt.setString(1, malzemeAdi);
    pstmt.setFloat(2, Float.parseFloat(miktar));
    pstmt.setString(3, "gram");  // Varsayılan birim olarak 'gram' ekleniyor

    pstmt.executeUpdate();
    pstmt.close();
}
public static void deleteMalzeme(Connection conn, String malzemeAdi) throws SQLException {
    String sql = "DELETE FROM malzemeler WHERE MalzemeAdi = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, malzemeAdi);
        pstmt.executeUpdate();
    }
}

public static void addNewMalzemeWithFiyat(Connection conn, String malzemeAdi, String miktar, String fiyat) throws SQLException {
    String sql = "INSERT INTO malzemeler (MalzemeAdi, ToplamMiktar, BirimFiyat, MalzemeBirim) VALUES (?, ?, ?, 'gram')";
    PreparedStatement pstmt = conn.prepareStatement(sql);
    pstmt.setString(1, malzemeAdi);
    pstmt.setFloat(2, Float.parseFloat(miktar));  // Miktar girdi olarak alınır
    pstmt.setFloat(3, Float.parseFloat(fiyat));   // Fiyat bilgisi girilir

    pstmt.executeUpdate();
    pstmt.close();
}

    // Veritabanında tarif silme fonksiyonu
public static void deleteTarif(Connection conn, String tarifAdi) {
    String sql = "DELETE FROM Tarifler WHERE TarifAdi = ?";
    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, tarifAdi);
        int affectedRows = pstmt.executeUpdate();

        if (affectedRows > 0) {
            System.out.println("Tarif başarıyla silindi!");
        }
    } catch (SQLException e) {
        System.out.println("Tarif silme hatası: " + e.getMessage());
    }
}

    // Veritabanında tarif güncelleme fonksiyonu
public static void updateTarif(Connection conn, String eskiTarifAdi, String yeniTarifAdi, String kategori, int hazirlamaSuresi, String talimatlar, List<String> malzemeListesi) {
    // Tarif güncelleme sorgusu
    String sqlTarif = "UPDATE Tarifler SET TarifAdi = ?, Kategori = ?, HazirlamaSuresi = ?, Talimatlar = ? WHERE TarifAdi = ?";
    
    try {
        // 1. Tarif bilgilerini güncelle
        PreparedStatement pstmt = conn.prepareStatement(sqlTarif);
        pstmt.setString(1, yeniTarifAdi);
        pstmt.setString(2, kategori);
        pstmt.setInt(3, hazirlamaSuresi);
        pstmt.setString(4, talimatlar);
        pstmt.setString(5, eskiTarifAdi);
        int affectedRows = pstmt.executeUpdate();

        if (affectedRows > 0) {
            System.out.println("Tarif başarıyla güncellendi!");

            // 2. Tarife ait eski malzemeleri sil
            int tarifID = getTarifIDByName(conn, eskiTarifAdi);
            String sqlDeleteMalzemeler = "DELETE FROM tarif_malzeme WHERE TarifID = ?";
            PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteMalzemeler);
            pstmtDelete.setInt(1, tarifID);
            pstmtDelete.executeUpdate();
            pstmtDelete.close();

            // 3. Yeni malzemeleri ekle
            for (String malzemeVeMiktar : malzemeListesi) {
                String[] parcalar = malzemeVeMiktar.split(" - ");
                String malzemeAdi = parcalar[0];
                float miktar = Float.parseFloat(parcalar[1].replaceAll("[^0-9.]", ""));
                int malzemeID = getMalzemeIDByName(conn, malzemeAdi);
                addTarifMalzeme(conn, tarifID, malzemeID, miktar);
            }

            System.out.println("Tarif malzemeleri başarıyla güncellendi!");
        }

        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Tarif güncelleme hatası: " + e.getMessage());
    }
}

public static boolean tarifteEksikMalzemeVarMi(Connection conn, int tarifID) {
    String sql = "SELECT m.MalzemeAdi, tm.MalzemeMiktar, m.ToplamMiktar " +
                 "FROM tarif_malzeme tm " +
                 "LEFT JOIN malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                 "WHERE tm.TarifID = ?";

    boolean eksikMalzemeVar = false;

    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, tarifID);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            String malzemeAdi = rs.getString("MalzemeAdi");
            float gerekenMiktar = rs.getFloat("MalzemeMiktar"); // Gereken malzeme miktarı
            String mevcutMiktarStr = rs.getString("ToplamMiktar"); // Mevcut miktar (null olabilir)

            System.out.println("Malzeme: " + malzemeAdi);
            System.out.println("Gereken Miktar: " + gerekenMiktar + "g");
            System.out.println("Mevcut Miktar: " + mevcutMiktarStr);

            // Eğer malzeme veritabanında yoksa veya miktar boşsa eksik olarak işaretle
            if (mevcutMiktarStr == null || mevcutMiktarStr.isEmpty()) {
                System.out.println("Eksik malzeme (veritabanında bulunmuyor): " + malzemeAdi);
                eksikMalzemeVar = true;
                continue;
            }

            // Mevcut miktarı float'a çevirme
            float mevcutMiktar = Float.parseFloat(mevcutMiktarStr);

            // Gereken miktarı karşılayamıyorsa, eksik malzeme olarak işaretle
            if (mevcutMiktar < gerekenMiktar) {
                System.out.println("Eksik malzeme: " + malzemeAdi + 
                                   ", Gereken: " + gerekenMiktar + "g, Mevcut: " + mevcutMiktar + "g");
                eksikMalzemeVar = true;
            }
        }

        rs.close();
        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Eksik malzeme kontrol hatası: " + e.getMessage());
    } catch (NumberFormatException e) {
        System.out.println("Miktar ayrıştırma hatası: " + e.getMessage());
    }

    return eksikMalzemeVar;
}





// Birim dönüşümü için fonksiyon (her birimi grama çeviriyoruz)
public static float convertToGrams(float miktar, String birim) {
    switch (birim.toLowerCase()) {
        case "kg":
            return miktar * 1000;  // Kilogram -> Gram
        case "g":
            return miktar;  // Zaten gram, değişiklik yok
        case "mg":
            return miktar / 1000;  // Miligram -> Gram
        case "l":
            return miktar * 1000;  // Litreyi gram olarak kabul edelim (örneğin su)
        case "ml":
            return miktar;  // Mililitre -> Gram, 1:1 kabul ediyoruz
        default:
            System.out.println("Bilinmeyen birim: " + birim);
            return miktar;  // Bilinmeyen birim, dönüşüm yapmadan miktarı döndür
    }
}

public static int getMalzemeIDByName(Connection conn, String malzemeAdi) {
    String sql = "SELECT MalzemeID FROM malzemeler WHERE MalzemeAdi = ?";
    int malzemeID = -1;
    
    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, malzemeAdi);
        ResultSet rs = pstmt.executeQuery();
        
        if (rs.next()) {
            malzemeID = rs.getInt("MalzemeID");
        }

        rs.close();
        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Malzeme ID bulunurken hata oluştu: " + e.getMessage());
    }

    return malzemeID;
}
public static void addTarifMalzeme(Connection conn, int tarifID, int malzemeID, float miktar) {
    String sql = "INSERT INTO tarif_malzeme (TarifID, MalzemeID, MalzemeMiktar) VALUES (?, ?, ?)";
    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, tarifID);
        pstmt.setInt(2, malzemeID);
        pstmt.setFloat(3, miktar);
        pstmt.executeUpdate();
        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Tarif-Malzeme ekleme hatası: " + e.getMessage());
    }
}
public static void loadMalzemeler(Connection conn, DefaultListModel<String> malzemeListModel) {
    String sql = "SELECT MalzemeAdi FROM malzemeler";
    
    try {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        // Malzemeleri listeye ekleyelim
        while (rs.next()) {
            String malzemeAdi = rs.getString("MalzemeAdi");
            malzemeListModel.addElement(malzemeAdi);  // Malzeme adını listeye ekliyoruz
        }

        rs.close();
        stmt.close();
    } catch (SQLException e) {
        System.out.println("Malzemeleri yüklerken hata oluştu: " + e.getMessage());
    }
}
private static float parseGramaj(String gramaj) {
    // "200g" gibi bir değerden sadece sayısal kısmı alıyoruz
    return Float.parseFloat(gramaj.replaceAll("[^0-9.]", ""));
}

public static int getTarifIDByName(Connection conn, String tarifAdi) {
    String sql = "SELECT TarifID FROM Tarifler WHERE LOWER(TarifAdi) = ?";
    int tarifID = -1;

    try {
        // Parametreyi Java tarafında küçük harfe dönüştür
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, tarifAdi.toLowerCase()); // tarifAdi'yi küçük harfe çeviriyoruz
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            tarifID = rs.getInt("TarifID");
        }

        rs.close();
        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Tarif ID bulunurken hata oluştu: " + e.getMessage());
    }

    return tarifID;
}

public Map<String, Float> parseGramajBilgisi(String gramajBilgisi) {
    Map<String, Float> malzemeGramajMap = new HashMap<>();
    
    // Her satırı ayrıştır
    String[] satirlar = gramajBilgisi.split("\n");
    for (String satir : satirlar) {
        String[] parcalar = satir.split(":");
        if (parcalar.length == 2) {
            String malzemeAdi = parcalar[0].trim();  // Malzeme adı
            String miktarStr = parcalar[1].trim().replaceAll("[^0-9.]", ""); // Sadece miktar kısmı
            try {
                float miktar = Float.parseFloat(miktarStr);  // Miktarı float'a çevir
                malzemeGramajMap.put(malzemeAdi, miktar);    // Haritaya ekle
            } catch (NumberFormatException e) {
                System.out.println("Gramaj hatalı: " + miktarStr);
            }
        }
    }
    
    return malzemeGramajMap;
}
public static List<String> getTarifMalzemeler(Connection conn, int tarifID) {
    List<String> malzemelerListesi = new ArrayList<>();
    String sql = "SELECT m.MalzemeAdi, tm.MalzemeMiktar FROM tarif_malzeme tm JOIN malzemeler m ON tm.MalzemeID = m.MalzemeID WHERE tm.TarifID = ?";
    
    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, tarifID);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            String malzemeAdi = rs.getString("MalzemeAdi");
            float miktar = rs.getFloat("MalzemeMiktar");
            malzemelerListesi.add(malzemeAdi + " - " + miktar + "g");
        }

        rs.close();
        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Tarif malzemeleri getirilirken hata oluştu: " + e.getMessage());
    }

    return malzemelerListesi;
}

public static float calculateTarifMaliyeti(Connection conn, int tarifID) {
    float toplamMaliyet = 0.0f;

    // Tüm gerekli bilgileri tek bir sorgu ile alıyoruz
    String sql = "SELECT m.MalzemeAdi, tm.MalzemeMiktar, m.BirimFiyat " +
                 "FROM tarif_malzeme tm " +
                 "JOIN malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                 "WHERE tm.TarifID = ?";
    
    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, tarifID);
        ResultSet rs = pstmt.executeQuery();
        
        // Her bir malzeme için maliyet hesaplama
        while (rs.next()) {
            String malzemeAdi = rs.getString("MalzemeAdi");
            float malzemeMiktar = rs.getFloat("MalzemeMiktar");  // Tarif için kullanılan miktar (gram cinsinden)
            float birimFiyat = rs.getFloat("BirimFiyat");  // Kilogram başına fiyat

            // Eğer birim fiyat veya miktar geçersiz ise, bu malzemeyi atlayalım
            if (birimFiyat == 0) {
                System.out.println("Uyarı: " + malzemeAdi + " için birim fiyat bulunamadı veya sıfır.");
                continue;
            }
            if (malzemeMiktar == 0) {
                System.out.println("Uyarı: " + malzemeAdi + " için miktar sıfır.");
                continue;
            }

            // Gram başına fiyatı bulmak için kilogram başına fiyatı 1000'e bölüyoruz
            float gramFiyat = birimFiyat / 1000;
            float malzemeMaliyeti = malzemeMiktar * gramFiyat;  // Malzeme maliyeti

            System.out.println("Malzeme: " + malzemeAdi + ", Miktar: " + malzemeMiktar + "g, Birim Fiyat (kg): " + birimFiyat + 
                               " TL, Hesaplanan Maliyet: " + malzemeMaliyeti + " TL");

            // Toplam maliyete ekleme
            toplamMaliyet += malzemeMaliyeti;
        }

        rs.close();
        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Maliyet hesaplama hatası: " + e.getMessage());
    }

    System.out.println("Toplam Maliyet: " + toplamMaliyet + " TL");
    return toplamMaliyet;
}


 // Veritabanından tarifleri arama ve kategoriye göre filtreleme (Malzemelerle birlikte)
public static void filtreleTarifler(Connection conn, DefaultListModel<String> listModel, String query, String kategori) {
    listModel.clear(); // Mevcut listeyi temizle

    // SQL sorgusunu oluşturma
    String sql = "SELECT DISTINCT t.TarifAdi " +
                 "FROM Tarifler t " +
                 "LEFT JOIN tarif_malzeme tm ON t.TarifID = tm.TarifID " +
                 "LEFT JOIN malzemeler m ON tm.MalzemeID = m.MalzemeID " +
                 "WHERE (t.TarifAdi LIKE ? OR m.MalzemeAdi LIKE ?) ";

    if (!kategori.equals("Tüm Kategoriler")) {
        sql += "AND t.Kategori = ?";
    }

    try {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, "%" + query + "%"); // Arama çubuğundaki ifadeye göre filtreleme (Tarif adı)
        pstmt.setString(2, "%" + query + "%"); // Arama çubuğundaki ifadeye göre filtreleme (Malzeme adı)

        if (!kategori.equals("Tüm Kategoriler")) {
            pstmt.setString(3, kategori); // Kategoriye göre filtreleme
        }

        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            String tarifAdi = rs.getString("TarifAdi");
            listModel.addElement(tarifAdi); // Eşleşen tarifleri listeye ekle
        }

        rs.close();
        pstmt.close();
    } catch (SQLException e) {
        System.out.println("Tarif filtreleme hatası: " + e.getMessage());
    }
}

public static float calculateMatchingRatio(Connection conn, int tarifID, List<String> userIngredients) {
    List<String> tarifMalzemeler = getTarifMalzemeler(conn, tarifID);
    int totalIngredients = tarifMalzemeler.size();
    int matchedIngredients = 0;

    // Tarifin malzemelerini kullanıcı malzemeleriyle kıyasla
    for (String tarifMalzeme : tarifMalzemeler) {
        String[] parts = tarifMalzeme.split(" - ");
        String malzemeAdi = parts[0].trim().toLowerCase();

        System.out.println("Tarif Malzemesi: " + malzemeAdi);  // Tarifin malzemesini yazdır

        // Kullanıcı malzemeleri ile eşleştirme yap
        for (String userIngredient : userIngredients) {
            System.out.println("Karşılaştırılan Kullanıcı Malzemesi: " + userIngredient);  // Karşılaştırılan kullanıcı malzemesini yazdır
            if (userIngredient.equals(malzemeAdi)) {
                matchedIngredients++;
                break;  // Eşleşen malzemeyi bulduysak daha fazla kontrol etmeye gerek yok
            }
        }
    }

    // Eşleşme oranını hesapla
    if (totalIngredients == 0) return 0;
    return (float) matchedIngredients / totalIngredients * 100;
}



public static void tarifOner(Connection conn, List<String> userIngredients, DefaultListModel<String> recipeListModel) {
    List<String> suggestedRecipes = new ArrayList<>();

    // Kullanıcı malzemelerini temizleyelim (küçük harfe çevir ve boşlukları kaldır)
    List<String> temizlenmisMalzemeler = new ArrayList<>();
    for (String malzeme : userIngredients) {
        String temizMalzeme = malzeme.trim().toLowerCase();
        temizlenmisMalzemeler.add(temizMalzeme);
    }

    // Tüm tarifleri veritabanından çek
    String sql = "SELECT TarifID, TarifAdi FROM Tarifler";
    try {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            int tarifID = rs.getInt("TarifID");
            String tarifAdi = rs.getString("TarifAdi");

            // Tarif için eşleşme oranını hesapla
            float matchRatio = calculateMatchingRatio(conn, tarifID, temizlenmisMalzemeler);

            // Eşleşme oranı belirli bir eşik değerin üzerindeyse, eksik malzeme kontrolünü yap
            if (matchRatio > 50) {
                boolean eksikMalzemeVar = tarifteEksikMalzemeVarMi(conn, tarifID);
                
                // Eksik malzeme varsa kırmızı, yeterliyse yeşil olarak işaretle
                String renk = eksikMalzemeVar ? "red" : "green";
                String recipeDisplay = String.format("<html><font color='%s'>%s - Eşleşme Oranı: %%%.2f</font></html>", 
                                                     renk, tarifAdi, matchRatio);
                
                suggestedRecipes.add(recipeDisplay);
            }
        }

        // Sonuçları model listesine ekleyelim
        recipeListModel.clear();
        for (String recipe : suggestedRecipes) {
            recipeListModel.addElement(recipe);
        }

        rs.close();
        stmt.close();
    } catch (SQLException e) {
        System.out.println("Tarif önerisi hatası: " + e.getMessage());
    }
}



     public static String[] fetchTarifDetaylari(Connection conn, String tarifAdi) {
        String sql = "SELECT Kategori, HazirlamaSuresi, Talimatlar FROM Tarifler WHERE TarifAdi = ?";
        String[] tarifDetaylari = new String[3]; // 0: Kategori, 1: Süre, 2: Talimatlar
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, tarifAdi);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                tarifDetaylari[0] = rs.getString("Kategori");
                tarifDetaylari[1] = String.valueOf(rs.getInt("HazirlamaSuresi"));
                tarifDetaylari[2] = rs.getString("Talimatlar");
            }

            rs.close();
            pstmt.close();
        } catch (Exception e) {
            System.out.println("Tarif detaylarını çekerken hata oluştu: " + e.getMessage());
        }

        return tarifDetaylari;
    }
}
    
    
    
    


