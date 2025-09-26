package br.com.trabalho.compilerui.ui;

import javax.swing.*;
import java.awt.*;

public class MessagesPanel {

    private final JTextArea textArea;
    private final JScrollPane scroll;

    public MessagesPanel() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);

        // >>> Sem mensagem fixa inicial <<<
        textArea.setText("");

        scroll = new JScrollPane(textArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
    }

    public JComponent getContainer() {
        return scroll;
    }

    public void setText(String s) {
        textArea.setText(s == null ? "" : s);
        textArea.setCaretPosition(0);
        textArea.repaint();
    }

    public void clear() {
        setText("");
    }

    public JTextArea getTextArea() {
        return textArea;
    }
}