package br.com.trabalho.compilerui;

import br.com.trabalho.compilerui.ui.AppFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Força LAF cross-platform (evita "barras ocultas")
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

                // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new AppFrame().setVisible(true);
        });
    }
}