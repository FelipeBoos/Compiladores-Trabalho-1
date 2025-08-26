package br.com.trabalho.compilerui.ui;


import javax.swing.*;


public class MessagesPanel {
    private final JTextArea textArea;
    private final JScrollPane scrollPane;


    public MessagesPanel() {
        textArea = new JTextArea();
        textArea.setEditable(false); // req. 6
        scrollPane = new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS); // req. 7
    }


    public void setText(String text) {
        textArea.setText(text == null ? "" : text);
        textArea.setCaretPosition(0);
    }


    public void clear() { setText(""); }


    public JComponent getContainer() { return scrollPane; }
    public JTextArea getTextArea() { return textArea; }
}