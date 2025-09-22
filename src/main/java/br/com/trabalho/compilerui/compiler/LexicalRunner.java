package br.com.trabalho.compilerui.compiler;

import java.util.ArrayList;
import java.util.List;

public final class LexicalRunner {

    private LexicalRunner() {}

    public static List<String> run(String source) {
        List<String> out = new ArrayList<>();
        Lexico lex = new Lexico();
        lex.setInput(source); // API padrão do GALS

        try {
            while (true) {
                Token t = lex.nextToken();
                if (t == null) break; // FIM DE ARQUIVO: GALS retorna null

                String classe = classify(t);
                if (classe == null) continue;

                int linha = toLineNumber(source, t.getPosition());
                // formato: "<linha> <classe> <lexema>"
                out.add(String.format("%d %s %s", linha, classe, t.getLexeme()));
            }
            out.add("programa compilado com sucesso");
        } catch (LexicalError e) {
            int linha = toLineNumber(source, e.getPosition());
            out.add(String.format("linha %d: %s", linha, normalizeMsg(e.getMessage())));
        }

        return out;
    }

    /** Classificação básica usando IDs gerados pelo GALS */
    private static String classify(Token t) {
        int id = t.getId();

        if (id == Constants.t_identificador) return "identificador";
        if (id == Constants.t_cint)         return "constante_int";
        if (id == Constants.t_cfloat)       return "constante_float";
        if (id == Constants.t_cstring)      return "constante_string";

        // Palavras reservadas: seus IDs estão no intervalo 6..32 no seu Constants
        if (id >= Constants.t_pr_abstrato && id <= Constants.t_pr_until) {
            return "palavra_reservada";
        }

        // Símbolos especiais: 33..49 no seu Constants
        if (id >= Constants.t_TOKEN_33 && id <= Constants.t_TOKEN_49) {
            return "simbolo";
        }

        // Qualquer outro ID (se houver)
        return "token";
    }

    /** Converte posição absoluta (offset) para número de linha (1-based). */
    private static int toLineNumber(String src, int pos) {
        if (pos <= 0) return 1;
        int lines = 1;
        int limit = Math.min(pos, src.length());
        for (int i = 0; i < limit; i++) {
            if (src.charAt(i) == '\n') lines++;
        }
        return lines;
    }

    /** Mensagem de erro mais amigável. */
    private static String normalizeMsg(String msg) {
        if (msg == null || msg.isEmpty()) return "erro léxico";
        return msg;
    }
}