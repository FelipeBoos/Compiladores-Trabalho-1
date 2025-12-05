package br.com.trabalho.compilerui.ui;

import br.com.trabalho.compilerui.compiler.ParserRunner;
import br.com.trabalho.compilerui.io.TextFileIO;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AppFrame extends JFrame {

    private final ToolbarPanel toolbar;
    private final EditorWithLineNumbers editor;
    private final MessagesPanel messages;
    private final StatusBar statusBar;

    private Path currentFile = null;

    public AppFrame() {
        super("Interface do Compilador");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1500, 800);
        setLocationRelativeTo(null);
        setResizable(false);

        setLayout(new BorderLayout());

        // Top: Toolbar
        toolbar = new ToolbarPanel(this);
        add(toolbar, BorderLayout.NORTH);

        // Center: Split editor/messages
        editor = new EditorWithLineNumbers();
        messages = new MessagesPanel();

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                editor.getContainer(),
                messages.getContainer()
        );
        split.setResizeWeight(0.8);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        // Bottom: Status bar
        statusBar = new StatusBar();
        add(statusBar, BorderLayout.SOUTH);

        wireActionsAndShortcuts();
    }

    private void wireActionsAndShortcuts() {
        // Botões
        toolbar.getBtnNovo().addActionListener(e -> doNovo());
        toolbar.getBtnAbrir().addActionListener(e -> doAbrir());
        toolbar.getBtnSalvar().addActionListener(e -> doSalvar());
        toolbar.getBtnCopiar().addActionListener(e -> editor.getTextArea().copy());
        toolbar.getBtnColar().addActionListener(e -> editor.getTextArea().paste());
        toolbar.getBtnRecortar().addActionListener(e -> editor.getTextArea().cut());
        toolbar.getBtnCompilar().addActionListener(e -> doCompilar());
        toolbar.getBtnEquipe().addActionListener(e -> doEquipe());

        // Atalhos
        bindKeyStroke("control N", "novo", this::doNovo);
        bindKeyStroke("control O", "abrir", this::doAbrir);
        bindKeyStroke("control S", "salvar", this::doSalvar);
        bindKeyStroke("control C", "copiar", () -> editor.getTextArea().copy());
        bindKeyStroke("control V", "colar", () -> editor.getTextArea().paste());
        bindKeyStroke("control X", "recortar", () -> editor.getTextArea().cut());
        bindKeyStroke("F7", "compilar", this::doCompilar);
        bindKeyStroke("F1", "equipe", this::doEquipe);
    }

    private void bindKeyStroke(String ks, String name, Runnable action) {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(ks), name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    // --- Ações ---

    private void doNovo() {
        editor.getTextArea().setText("");
        messages.clear();
        statusBar.clear();
        currentFile = null;
    }

    private void doAbrir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Abrir arquivo de texto");
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivos de texto (*.txt)", "txt"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();
            try {
                String content = TextFileIO.read(file);
                editor.getTextArea().setText(content);
                editor.getTextArea().setCaretPosition(0);
                messages.clear();
                statusBar.updatePath(file.toString());
                currentFile = file;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Erro ao abrir arquivo:\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doSalvar() {
        try {
            if (currentFile == null) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Salvar como");
                chooser.setFileFilter(new FileNameExtensionFilter("Arquivos de texto (*.txt)", "txt"));
                int result = chooser.showSaveDialog(this);
                if (result != JFileChooser.APPROVE_OPTION) return;

                Path file = chooser.getSelectedFile().toPath();
                if (!file.toString().toLowerCase().endsWith(".txt")) {
                    file = file.resolveSibling(file.getFileName().toString() + ".txt");
                }
                if (Files.exists(file)) {
                    int opt = JOptionPane.showConfirmDialog(this,
                            "Arquivo existe. Deseja sobrescrever?",
                            "Confirmar",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (opt != JOptionPane.YES_OPTION) return;
                }
                TextFileIO.write(file, editor.getTextArea().getText());
                messages.clear();
                statusBar.updatePath(file.toString());
                currentFile = file;
            } else {
                TextFileIO.write(currentFile, editor.getTextArea().getText());
                messages.clear();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao salvar arquivo:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Compilar: executa o analisador SINTÁTICO e exibe a saída
    private void doCompilar() {
        messages.clear();

        String source = editor.getTextArea().getText();
        if (source.isBlank()) {
            messages.setText("Nenhum código para compilar.");
            return;
        }

        // Exigir arquivo salvo para gerar o .il
        if (currentFile == null) {
            messages.setText("Salve o arquivo antes de compilar para gerar o código objeto (.il).");
            return;
        }

        try {
            List<String> result = ParserRunner.run(source);

            // fonte monoespaçada para legibilidade
            messages.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            // === ÁREA DE MENSAGENS ===
            // Mostrar APENAS a primeira linha (erro ou sucesso).
            if (result.isEmpty()) {
                messages.setText("Erro: compilação não retornou mensagem.");
            } else {
                messages.setText(result.get(0));
            }
            messages.getTextArea().setCaretPosition(0);

            // === GERAÇÃO DO ARQUIVO IL ===
            // Somente se compilou com sucesso.
            if (!result.isEmpty()
                    && result.get(0).trim().equalsIgnoreCase("programa compilado com sucesso")) {

                // Monta o conteúdo IL a partir das demais linhas (result[1..])
                StringBuilder il = new StringBuilder();
                for (int i = 1; i < result.size(); i++) {
                    String line = result.get(i);

                    // Ignora o marcador "----------- CÓDIGO IL -----------"
                    if (line.startsWith("-----------")) continue;

                    il.append(line).append('\n');
                }

                // Caminho do arquivo .il na mesma pasta do .txt
                Path ilPath = currentFile.resolveSibling(
                        replaceExtension(currentFile.getFileName().toString(), ".il")
                );

                // Salvar arquivo .il
                TextFileIO.write(ilPath, il.toString());

                // Atualizar status bar (opcional)
                statusBar.updatePath(
                        currentFile.toString() +
                                "  (código IL gerado: " + ilPath.getFileName() + ")"
                );
            }

        } catch (Exception ex) {
            messages.setText("Erro durante a compilação: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void doEquipe() {
        messages.setText("Felipe Boos\nMatheus Hillesheim\nSofia Sofiatti");
    }

    // Utilidades
    public String getEditorText() { return editor.getTextArea().getText(); }

    public void showMessages(List<String> msgs) {
        messages.clear();
        StringBuilder sb = new StringBuilder();
        for (String l : msgs) sb.append(l).append('\n');
        messages.setText(sb.toString());
    }

    private String replaceExtension(String fileName, String newExt) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            return fileName.substring(0, idx) + newExt;
        }
        return fileName + newExt;
    }

    public ToolbarPanel getToolbar() { return toolbar; }
    public EditorWithLineNumbers getEditor() { return editor; }
    public MessagesPanel getMessages() { return messages; }
    public StatusBar getStatusBar() { return statusBar; }
}
