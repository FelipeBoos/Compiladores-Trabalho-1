package br.com.trabalho.compilerui;

import br.com.trabalho.compilerui.ui.AppFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new AppFrame().setVisible(true);
        });
    }
}