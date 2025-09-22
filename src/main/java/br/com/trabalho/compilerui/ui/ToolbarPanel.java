package br.com.trabalho.compilerui.ui;

import br.com.trabalho.compilerui.compiler.LexicalRunner;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;

public class ToolbarPanel extends JPanel {

    private static final int BTN_WIDTH  = 170;
    private static final int BTN_HEIGHT = 40;
    private static final int ICON_SIZE  = 24;

    private final JToolBar toolBar;

    // Botões expostos (se você quiser ligar mais ações depois)
    private final JButton btnNovo;
    private final JButton btnAbrir;
    private final JButton btnSalvar;
    private final JButton btnCopiar;
    private final JButton btnColar;
    private final JButton btnRecortar;
    private final JButton btnCompilar;
    private final JButton btnEquipe;

    private final AppFrame app; // referência para acessar editor e painel de mensagens

    public ToolbarPanel(AppFrame app) {
        this.app = app;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 70)); // altura ~70px

        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // ==== Arquivo ====
        btnNovo   = createButton("novo [ctrl-n]",   "novo.png");
        btnAbrir  = createButton("abrir [ctrl-o]",  "abrir.png");
        btnSalvar = createButton("salvar [ctrl-s]", "salvar.png");

        toolBar.add(btnNovo);
        toolBar.add(btnAbrir);
        toolBar.add(btnSalvar);
        toolBar.addSeparator();

        // ==== Edição ====
        btnCopiar   = createButton("copiar [ctrl-c]",   "copiar.png");
        btnColar    = createButton("colar [ctrl-v]",    "colar.png");
        btnRecortar = createButton("recortar [ctrl-x]", "cortar.png");

        toolBar.add(btnCopiar);
        toolBar.add(btnColar);
        toolBar.add(btnRecortar);
        toolBar.addSeparator();

        // ==== Compilar ====
        btnCompilar = createButton("compilar [F7]", "compilar.png");
        btnCompilar.addActionListener(e -> onCompile());
        toolBar.add(btnCompilar);
        toolBar.addSeparator();

        // ==== Equipe ====
        btnEquipe = createButton("equipe [F1]", "equipe.png");
        toolBar.add(btnEquipe);

        add(toolBar, BorderLayout.CENTER);
    }

    /** Executa o runner léxico e envia as mensagens para o painel de saída. */
    private void onCompile() {
        String codigo = app.getEditorText();                 // você expõe esse getter no AppFrame
        List<String> linhas = LexicalRunner.run(codigo);     // roda o léxico (Parte 2)
        app.showMessages(linhas);                            // e mostra no MessagesPanel
    }

    private JButton createButton(String text, String iconFile) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setHorizontalTextPosition(SwingConstants.RIGHT);
        b.setIcon(loadIcon("/Icons/" + iconFile, ICON_SIZE, ICON_SIZE));
        b.setIconTextGap(8);

        Dimension d = new Dimension(BTN_WIDTH, BTN_HEIGHT);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        b.setMargin(new Insets(6, 10, 6, 10));
        return b;
    }

    private static Icon loadIcon(String path, int w, int h) {
        URL url = ToolbarPanel.class.getResource(path);
        if (url == null) {
            System.err.println("Ícone não encontrado no classpath: " + path);
            return null;
        }
        Image img = new ImageIcon(url).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    // Getters (se ainda quiser conectar outras ações fora da classe)
    public JButton getBtnNovo()      { return btnNovo; }
    public JButton getBtnAbrir()     { return btnAbrir; }
    public JButton getBtnSalvar()    { return btnSalvar; }
    public JButton getBtnCopiar()    { return btnCopiar; }
    public JButton getBtnColar()     { return btnColar; }
    public JButton getBtnRecortar()  { return btnRecortar; }
    public JButton getBtnCompilar()  { return btnCompilar; }
    public JButton getBtnEquipe()    { return btnEquipe; }
}
