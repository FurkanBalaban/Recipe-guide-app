/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarif_application;

import javax.swing.JPanel;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author furkanbalaban
 */
public class TarifListRenderer extends JPanel implements ListCellRenderer<String>  {

    private JLabel nameLabel;
    private Map<String, ImageIcon> categoryIcons;
    public TarifListRenderer() {
        
        setLayout(new BorderLayout(5, 5)); // Layout düzenlemesi
        nameLabel = new JLabel();
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));  // Yazı stili
        nameLabel.setOpaque(true);

        add(nameLabel, BorderLayout.CENTER);  // Label'ı ortalayarak ekliyoruz
        // Kategori simgelerini yüklüyoruz
        categoryIcons = new HashMap<>();
        categoryIcons.put("Ana Yemek", new ImageIcon("src/icons/anayemek.png"));
        categoryIcons.put("Tatlı", new ImageIcon("src/icons/tatli.png"));
        categoryIcons.put("Çorba", new ImageIcon("src/icons/corba.png"));

        // Kutu görünümü için kenarlık ve arka plan renkleri
       setBorder(BorderFactory.createLineBorder(Color.GRAY, 3));  // Kenarlık 3 piksel kalınlıkta olacak
// Kenar çizgisi ve alt çizgi ekliyoruz
       // Kenar çizgisi ve alt çizgi ekliyoruz
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, Color.GRAY),  // Alt çizgi 3 piksel
                BorderFactory.createEmptyBorder(5, 5, 5, 5)  // Hücre içi boşluk
        ));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
        nameLabel.setText(value);  // Tarif adını label'da göster

        if (isSelected) {
            setBackground(Color.LIGHT_GRAY);  // Seçili olduğunda gri arka plan
        } else {
            setBackground(Color.WHITE);  // Seçili olmadığında beyaz arka plan
        }

        return this;
    }    
}