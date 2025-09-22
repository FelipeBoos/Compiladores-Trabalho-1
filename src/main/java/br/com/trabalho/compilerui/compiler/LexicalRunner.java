package br.com.trabalho.compilerui.compiler;

import java.util.ArrayList;
import java.util.List;

public final class LexicalRunner {

    public static List<String> run(String source) {
        List<String> out = new ArrayList<>();
        Lexico lex = new Lexico();
        lex.setInput(source); // API padrão do GALS gerado

        try {
            while (true) {
                Token t = lex.nextToken();               // se seu gerado usa getNextToken(), troque aqui
                if (t == null || t.getId() == Constants.EOF) break;

                String classe = classify(t);
                if (classe == null) continue;            // ignorados já não vêm, mas por garantia
                int linha = toLineNumber(source, t.getPosition());
                out.add(String.format("%d %s %s", linha, classe, t.getLexeme()));
            }
            out.add("programa compilado com sucesso");
        } catch (LexicalError e) {
            int linha = toLineNumber(source, e.getPosition());
            out.add(String.format("linha %d: %s", linha, normalizeMsg(e, source)));
        }
        return out;
    }

    private static String classify(Token t) {
        int id = t.getId();
        if (id == Constants.identifier) return "identificador";
        if (id == Constants.cint)       return "constante_int";
        if (id == Constants.cfloat)     return "constante_float";
        if (id == Constants.cstring)    return "constante_string";

        // keywords: os IDs começam com pr_ no Constants
        String name = tokenName(id);
        if (name.startsWith("pr_")) return "palavra reservada";

        // símbolos especiais (conjunto da linguagem)
        String lx = t.getLexeme();
        if (lx.equals("==")||lx.equals("~=")||lx.equals("<-")||
                lx.equals("+")||lx.equals("-")||lx.equals("*")||lx.equals("/")||
                lx.equals("<")||lx.equals(">")||lx.equals("=")||
                lx.equals("(")||lx.equals(")")||lx.equals(";")||lx.equals(",")) {
            return "símbolo especial";
        }
        return null; // nada a imprimir
    }

    private static String tokenName(int id) {
        try {
            for (var f : Constants.class.getFields())
                if (f.getType() == int.class && f.getInt(null) == id) return f.getName();
        } catch (Exception ignore) {}
        return "";
    }

    private static int toLineNumber(String src, int pos) {
        if (pos < 0) return 1;
        int line = 1;
        for (int i = 0; i < Math.min(pos, src.length()); i++)
            if (src.charAt(i) == '\n') line++;
        return line;
    }

    private static String normalizeMsg(LexicalError e, String src) {
        String msg = (e.getMessage() == null ? "" : e.getMessage()).toLowerCase();
        if (msg.contains("string"))  return "constante_string inválida";
        if (msg.contains("float"))   return "constante_float inválida";
        if (msg.contains("integer") || msg.contains("int")) return "constante_int inválida";
        if (msg.contains("comment") || msg.contains("coment") || msg.contains("unclosed"))
            return "comentário inválido ou não finalizado";
        int p = e.g
