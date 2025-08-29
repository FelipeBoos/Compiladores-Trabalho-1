package br.com.trabalho.compilerui.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class EditorWithLineNumbers {

    private final JTextArea textArea;
    private final JScrollPane scrollPane;

    public EditorWithLineNumbers() {
        // Não “colar” na largura do viewport (preserva barra horizontal)
        textArea = new JTextArea() {
            @Override public boolean getScrollableTracksViewportWidth() { return false; }
        };
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        textArea.setTabSize(4);
        textArea.setBackground(Color.WHITE); // fundo do editor

        scrollPane = new JScrollPane(
                textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        // garante pintura consistente
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Gutter (números de linha)
        LineNumberView lineNumbers = new LineNumberView(textArea);
        scrollPane.setRowHeaderView(lineNumbers);
        if (scrollPane.getRowHeader() != null) {
            scrollPane.getRowHeader().setOpaque(true);
            scrollPane.getRowHeader().setBackground(LineNumberView.BG_COLOR);
        }

        // Repaint do gutter ao rolar
        scrollPane.getViewport().addChangeListener(e -> lineNumbers.repaint());
    }

    public JComponent getContainer() { return scrollPane; }
    public JTextArea getTextArea() { return textArea; }

    // -------- Gutter para números de linha --------
    static class LineNumberView extends JComponent
            implements DocumentListener, CaretListener, PropertyChangeListener {

        static final Color BG_COLOR = new Color(245, 245, 245);
        static final Color SEP_COLOR = new Color(200, 200, 200);
        static final int MARGIN = 8;

        private final JTextArea area;

        LineNumberView(JTextArea area) {
            this.area = area;
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            setOpaque(true);
            setBackground(BG_COLOR);

            area.getDocument().addDocumentListener(this);
            area.addCaretListener(this);
            area.addPropertyChangeListener("document", this);

            addHierarchyListener(e -> {
                Component p = getParent();
                if (p instanceof JViewport vp) {
                    vp.addChangeListener(ev -> repaint());
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int lines = Math.max(1, area.getLineCount());
            int digits = String.valueOf(lines).length();
            int width = MARGIN * 2 + fm.charWidth('0') * digits;
            return new Dimension(Math.max(40, width), area.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            Rectangle clip = g2.getClipBounds();

            // fundo e divisória
            g2.setColor(getBackground());
            g2.fillRect(clip.x, clip.y, clip.width, clip.height);
            g2.setColor(SEP_COLOR);
            g2.drawLine(getWidth() - 1, clip.y, getWidth() - 1, clip.y + clip.height);

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int descent = fm.getDescent();

            // faixa visível do editor
            Rectangle vr = area.getVisibleRect();
            int startOffset = area.viewToModel2D(new Point(0, vr.y));
            int endOffset   = area.viewToModel2D(new Point(0, vr.y + vr.height));

            try {
                int startLine = area.getLineOfOffset(startOffset);
                int endLine   = area.getLineOfOffset(endOffset);

                for (int line = startLine; line <= endLine; line++) {
                    int lineStart = area.getLineStartOffset(Math.min(line, area.getLineCount() - 1));
                    Rectangle r = area.modelToView2D(lineStart).getBounds();

                    String num = String.valueOf(line + 1);
                    int x = getWidth() - MARGIN - fm.stringWidth(num);
                    int y = r.y + r.height - descent;

                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(num, x, y);
                }
            } catch (Exception ignored) { }

            g2.dispose();
        }

        private void refresh() { revalidate(); repaint(); }

        // DocumentListener
        @Override public void insertUpdate(DocumentEvent e) { refresh(); }
        @Override public void removeUpdate(DocumentEvent e) { refresh(); }
        @Override public void changedUpdate(DocumentEvent e) { refresh(); }

        // CaretListener
        @Override public void caretUpdate(CaretEvent e) { repaint(); }

        // PropertyChangeListener
        @Override public void propertyChange(PropertyChangeEvent evt) { refresh(); }
    }
}