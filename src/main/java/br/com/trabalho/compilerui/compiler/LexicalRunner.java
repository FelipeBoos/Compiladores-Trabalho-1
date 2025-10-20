package br.com.trabalho.compilerui.compiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Executa o léxico e formata a saída no padrão do enunciado. */
public final class LexicalRunner {

    /** Símbolos “simples” apenas para rotular como "símbolo especial". */
    private static final Set<String> SPECIAL_SYMBOLS = Set.of(
            "+","-","*","/","(",")","[","]","{","}","^",".",":","?","|","=",",",";","<",">"
    );

    /** Conjunto de IDs que o GALS usa para palavras-reservadas. */
    private static final Set<Integer> RESERVED_IDS = new HashSet<>();
    static {
        for (int v : ScannerConstants.SPECIAL_CASES_VALUES) {
            RESERVED_IDS.add(v);
        }
    }

    private LexicalRunner() {}

    public static List<String> run(String source) {
        List<String> out = new ArrayList<>();
        Lexico lex = new Lexico();
        lex.setInput(new java.io.StringReader(source));

        // Cabeçalho como no PDF
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
            // Exibir somente o erro (não listar tokens anteriores)
            out.clear();

            int pos = e.getPosition();
            int linha = toLineNumber(source, pos);
            String msg = normalizeMsg(e.getMessage());

            // tenta extrair classe do erro
            if (msg.contains("cstring") || msg.contains("string")) {
                out.add(String.format("linha %d: constante_string inválida", linha));
            } else if (msg.contains("cint")) {
                out.add(String.format("linha %d: constante_int inválida", linha));
            } else if (msg.contains("cfloat")) {
                out.add(String.format("linha %d: constante_float inválida", linha));
            } else if (msg.contains("identificador")) {
                out.add(String.format("linha %d: identificador inválido", linha));
            } else if (msg.equals("símbolo inválido")) {
                out.add(String.format("linha %d: símbolo inválido", linha));
            } else {
                out.add(String.format("linha %d: erro léxico", linha));
            }
        }

        return out;
    }

    /** Classificação sem depender de nomes t_pr_* ou t_TOKEN_*. */
    private static String classify(Token t) {
        int id = t.getId();
        String lx = t.getLexeme();

        if (id == Constants.t_identificador) return "identificador";
        if (id == Constants.t_cint)         return "constante_int";
        if (id == Constants.t_cfloat)       return "constante_float";
        if (id == Constants.t_cstring)      return "constante_string";

        // Palavras-reservadas: qualquer ID mapeado via SPECIAL_CASES
        if (RESERVED_IDS.contains(id)) return "palavra reservada";

        // Símbolos/operadores: trate tudo que sobrar como “símbolo especial”
        if (lx != null && !lx.isEmpty()) {
            // cobre os simples e também operadores compostos (==, ~=, <- etc.)
            if (SPECIAL_SYMBOLS.contains(lx) || "==".equals(lx) || "~=".equals(lx) || "<-".equals(lx)) {
                return "símbolo especial";
            }
            // por segurança, qualquer outro token não-classificado cai aqui
            return "símbolo especial";
        }

        // fallback (não deve ocorrer)
        return "símbolo especial";
    }

    /** Converte offset (0-based) em número de linha (1-based). */
    private static int toLineNumber(String src, int pos) {
        if (pos <= 0) return 1;
        int lines = 1;
        int limit = Math.min(pos, src.length());
        for (int i = 0; i < limit; i++) if (src.charAt(i) == '\n') lines++;
        return lines;
    }

    /** Ajusta mensagens para o formato do PDF da Parte 3. */
    private static String normalizeMsg(String msg) {
        if (msg == null || msg.isEmpty()) return "erro léxico";

        String lower = msg.toLowerCase();

        // Substituições pedidas no enunciado
        if (msg.startsWith("Caractere não esperado")) return "símbolo inválido";
        if (lower.contains("cstring")) return "constante_string inválida";
        if (lower.contains("cfloat"))  return "constante_float inválida";
        if (lower.contains("cint"))    return "constante_int inválida";

        return msg;
    }
}
