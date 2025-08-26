package br.com.trabalho.compilerui.ui;

import br.com.trabalho.compilerui.io.TextFileIO;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;

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
        toolbar = new ToolbarPanel();
        add(toolbar, BorderLayout.NORTH);

        // Center: Split editor/messages
        editor = new EditorWithLineNumbers();
        messages = new MessagesPanel();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editor.getContainer(), messages.getContainer());
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

    // --- Ações exigidas pelo enunciado ---

    // (10) Novo: limpa editor, mensagens e status; zera arquivo atual
    private void doNovo() {
        editor.getTextArea().setText("");
        messages.clear();
        statusBar.clear();
        currentFile = null;
    }

    // (11) Abrir: só .txt; se cancelar, nada muda
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
                JOptionPane.showMessageDialog(this, "Erro ao abrir arquivo:\n" + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
        // CANCEL_OPTION -> não faz nada (mantém editor/mensagens/status)
    }

    // (12) Salvar: novo → escolher .txt e atualizar status; existente → sobrescrever e manter status
    private void doSalvar() {
        try {
            if (currentFile == null) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Salvar como");
                chooser.setFileFilter(new FileNameExtensionFilter("Arquivos de texto (*.txt)", "txt"));
                int result = chooser.showSaveDialog(this);
                if (result != JFileChooser.APPROVE_OPTION) return;

                Path file = chooser.getSelectedFile().toPath();
                // garante extensão .txt
                if (!file.toString().toLowerCase().endsWith(".txt")) {
                    file = file.resolveSibling(file.getFileName().toString() + ".txt");
                }
                // se existir, confirmar sobrescrita
                if (Files.exists(file)) {
                    int opt = JOptionPane.showConfirmDialog(this,
                            "Arquivo existe. Deseja sobrescrever?", "Confirmar",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (opt != JOptionPane.YES_OPTION) return;
                }
                TextFileIO.write(file, editor.getTextArea().getText());
                messages.clear();
                statusBar.updatePath(file.toString()); // atualiza status
                currentFile = file;
            } else {
                TextFileIO.write(currentFile, editor.getTextArea().getText());
                messages.clear();
                // manter status (já mostra o caminho atual)
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // (14) Compilar: apenas a mensagem abaixo visível
    private void doCompilar() {
        messages.setText("compilação de programas ainda não foi implementada");
    }

    // (15) Equipe: apenas os nomes da equipe
    private void doEquipe() {
        // Substitua pelos nomes reais dos integrantes:
        messages.setText("Felipe Silva\nJoão Souza\nMaria Oliveira");
    }

    // getters (se precisar em outro ponto)
    public ToolbarPanel getToolbar() { return toolbar; }
    public EditorWithLineNumbers getEditor() { return editor; }
    public MessagesPanel getMessages() { return messages; }
    public StatusBar getStatusBar() { return statusBar; }
}