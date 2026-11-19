package com.econest.task7;

import com.econest.task7.storage.CsvRepository;
import com.econest.task7.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class App {
    private App() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default Swing look and feel is still usable.
            }
            new MainFrame(new CsvRepository()).setVisible(true);
        });
    }
}
