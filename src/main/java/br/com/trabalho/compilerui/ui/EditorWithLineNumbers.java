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
        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        scrollPane = new JScrollPane(
                textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        );

        LineNumberView lineNumbers = new LineNumberView(textArea);
        scrollPane.setRowHeaderView(lineNumbers);
    }

    public JComponent getContainer() { return scrollPane; }
    public JTextArea getTextArea() { return textArea; }

    // --- Gutter para números de linha (corrigido) ---
    static class LineNumberView extends JComponent
            implements DocumentListener, CaretListener, PropertyChangeListener {

        private final JTextArea area;
        private final int MARGIN = 8;

        LineNumberView(JTextArea area) {
            this.area = area;
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            // repintar quando texto muda
            area.getDocument().addDocumentListener(this);
            // repintar quando caret muda (às vezes muda viewport por setCaretPosition)
            area.addCaretListener(this);
            // repintar quando rolar (mudança no viewport do scroll)
            area.addPropertyChangeListener("document", this);

            // também repinta quando o pai (viewport) muda de posição
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
            // altura do gutter acompanha a altura do text area
            return new Dimension(Math.max(40, width), area.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Rectangle clip = g.getClipBounds();
            Graphics2D g2 = (Graphics2D) g.create();

            // fundo + divisória
            g2.setColor(new Color(245, 245, 245));
            g2.fillRect(clip.x, clip.y, clip.width, clip.height);
            g2.setColor(new Color(200, 200, 200));
            g2.drawLine(getWidth() - 1, clip.y, getWidth() - 1, clip.y + clip.height);

            // números
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int ascent = fm.getAscent();
            int descent = fm.getDescent();

            // linhas visíveis
            Rectangle vr = area.getVisibleRect();
            int startOffset = area.viewToModel2D(new Point(0, vr.y));
            int endOffset = area.viewToModel2D(new Point(0, vr.y + vr.height));

            try {
                int startLine = area.getLineOfOffset(startOffset);
                int endLine = area.getLineOfOffset(endOffset);

                for (int line = startLine; line <= endLine; line++) {
                    int lineStart = area.getLineStartOffset(Math.min(line, area.getLineCount() - 1));
                    Rectangle r = area.modelToView2D(lineStart).getBounds();

                    String num = String.valueOf(line + 1);
                    int x = getWidth() - MARGIN - fm.stringWidth(num);
                    // baseline: topo da linha + altura - descent (ou topo + ascent)
                    int y = r.y + r.height - descent;

                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(num, x, y);
                }
            } catch (Exception ignored) { }

            g2.dispose();
        }

        private void refresh() {
            revalidate();   // atualiza largura conforme número de dígitos
            repaint();
        }

        // DocumentListener
        @Override public void insertUpdate(DocumentEvent e) { refresh(); }
        @Override public void removeUpdate(DocumentEvent e) { refresh(); }
        @Override public void changedUpdate(DocumentEvent e) { refresh(); }

        // CaretListener
        @Override public void caretUpdate(CaretEvent e) { repaint(); }

        // PropertyChangeListener (document swap)
        @Override public void propertyChange(PropertyChangeEvent evt) { refresh(); }
    }
}