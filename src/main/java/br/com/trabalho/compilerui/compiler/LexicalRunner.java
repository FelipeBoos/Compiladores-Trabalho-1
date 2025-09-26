package br.com.trabalho.compilerui.compiler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LexicalRunner {

    // conjunto de símbolos “soltos” para rotular como "simbolo" (caso não sejam
    // tokens compostos como ==, ~=, <-, etc., que já saem como "token" via Constants)
    private static final Set<String> SPECIAL_SYMBOLS = new HashSet<>(List.of(
            "+","-","*","/","(",")","[","]","{","}","^",".",":","?","|","=",","
    ));

    private LexicalRunner() {}

    public static List<String> run(String source) {
        List<String> out = new ArrayList<>();
        Lexico lex = new Lexico();
        // GALS usa Reader:
        lex.setInput(new java.io.StringReader(source));

        // cabeçalho no formato do PDF:
        out.add(String.format("%-5s %-18s %s", "linha", "classe", "lexema"));

        try {
            while (true) {
                Token t = lex.nextToken();
                if (t == null) break;

                String classe = classify(t);
                if (classe == null) continue;

                int linha = toLineNumber(source, t.getPosition());
                out.add(String.format("%-5d %-18s %s", linha, classe, t.getLexeme()));
            }
            out.add("programa compilado com sucesso");
        } catch (LexicalError e) {
            int pos = e.getPosition();
            int linha = toLineNumber(source, pos);

            // tenta recuperar o símbolo culpado
            char ch = '?';
            if (pos >= 0 && pos < source.length()) ch = source.charAt(pos);

            // normaliza a mensagem para bater com o PDF
            String msg = normalizeMsg(e.getMessage()); // troca "Caractere não esperado" -> "símbolo inválido"

            out.clear(); // << não listar tokens anteriores
            out.add(String.format("linha %d: %c %s", linha, ch, msg));
            return out;
        }

        return out;
    }

    /** Classificação com base nos IDs gerados pelo GALS. */
    private static String classify(Token t) {
        int id = t.getId();

        if (id == Constants.t_identificador) return "identificador";
        if (id == Constants.t_cint)         return "constante_int";
        if (id == Constants.t_cfloat)       return "constante_float";
        if (id == Constants.t_cstring)      return "constante_string";

        // Palavras reservadas
        if (id >= Constants.t_pr_abstrato && id <= Constants.t_pr_until)
            return "palavra reservada";

        // Símbolos especiais (parenteses, chaves, colchetes, etc.)
        if (id >= Constants.t_TOKEN_33 && id <= Constants.t_TOKEN_49)
            return "símbolo especial";

        // Demais (comparadores, vírgula, etc.) – se quiser, ajuste aqui
        // para também rotulá-los como "símbolo especial".
        return "símbolo especial";
    }

    /** Converte offset (0-based) para número de linha (1-based). */
    private static int toLineNumber(String src, int pos) {
        if (pos <= 0) return 1;
        int lines = 1;
        int limit = Math.min(pos, src.length());
        for (int i = 0; i < limit; i++) {
            if (src.charAt(i) == '\n') lines++;
        }
        return lines;
    }

    private static String normalizeMsg(String msg) {
        if (msg == null || msg.isEmpty()) return "erro léxico";
        // Deixa igual ao texto do PDF
        if ("Caractere não esperado".equals(msg)) return "símbolo inválido";
        return msg;
    }
}