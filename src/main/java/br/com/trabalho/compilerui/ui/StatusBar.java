package br.com.trabalho.compilerui.ui;


import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;


public class StatusBar extends JPanel {
    private final JLabel label;


    public StatusBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 25));
        setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 210, 210)));
        label = new JLabel(" ");
        add(label, BorderLayout.WEST);
    }


    public void updatePath(String path) { label.setText(path == null ? " " : path); }
    public void clear() { label.setText(" "); }
}