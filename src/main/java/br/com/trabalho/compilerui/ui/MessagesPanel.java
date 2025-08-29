package br.com.trabalho.compilerui.ui;

import javax.swing.*;
import java.awt.*;

public class MessagesPanel {

    private final JTextArea textArea;
    private final JScrollPane scrollPane;

    public MessagesPanel() {
        // não “colar” na largura do viewport → preserva barra horizontal
        textArea = new JTextArea() {
            @Override public boolean getScrollableTracksViewportWidth() { return false; }
        };
        textArea.setEditable(false);          // req. 6
        textArea.setLineWrap(false);          // evita wrap (mantém barra H)
        textArea.setWrapStyleWord(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setRows(5);                  // altura inicial
        textArea.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(
                textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS    // req. 7
        );

        // pintura consistente (evita “cinza fantasma” do viewport)
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(Color.WHITE);
    }

    /** Define o texto das mensagens e volta o caret para o topo */
    public void setText(String text) {
        textArea.setText(text == null ? "" : text);
        textArea.setCaretPosition(0);
    }

    /** Adiciona uma linha ao final (útil para logs) */
    public void appendLine(String line) {
        if (line == null) line = "";
        if (textArea.getText().isEmpty()) {
            textArea.append(line);
        } else {
            textArea.append(System.lineSeparator() + line);
        }
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    public void clear() { setText(""); }

    public JComponent getContainer() { return scrollPane; }
    public JTextArea getTextArea() { return textArea; }
}