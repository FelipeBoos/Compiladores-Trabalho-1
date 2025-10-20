package br.com.trabalho.compilerui.compiler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParserRunner {

    private ParserRunner() {}

    public static List<String> run(String source) {
        List<String> out = new ArrayList<>();

        Lexico lexico = new Lexico();
        lexico.setInput(new StringReader(source));

        Sintatico sintatico = new Sintatico();
        Semantico semantico = new Semantico();

        try {
            sintatico.parse(lexico, semantico);
            out.add("programa compilado com sucesso");
            return out;

        } catch (LexicalError e) {
            // ===== Parte 2 (léxico) – manter formato do enunciado =====
            int pos = e.getPosition();
            int linha = toLineNumber(source, pos);
            String raw = e.getMessage();

            if (raw != null && raw.toLowerCase(Locale.ROOT).contains("cstring")) {
                out.add(String.format("linha %d: constante_string inválida", linha));
                return out;
            }

            char ch = (pos >= 0 && pos < source.length()) ? source.charAt(pos) : '?';
            String msg = normalizeLexMsg(raw);
            out.add(String.format("linha %d: %c %s", linha, ch, msg));
            return out;

        } catch (SyntaticError e) {
            // ===== Parte 3 (sintático) – “encontrado … esperado …” =====
            int pos = e.getPosition();
            int linha = toLineNumber(source, pos);

            String encontrado = guessFoundTokenForMessage(source, pos);

            // Mensagem-base do GALS / tabela (pode conter “Era esperado …”, <token N>, <não-term>, etc.)
            String esperadoRaw = e.getMessage();

            // Opcional: se a mensagem do gerado também inclui lista de <token N>, tentamos resolvê-los.
            // Para ter os IDs, acusamos "encontrado" no próprio léxico…
            // Aqui não temos o ID do token corrente, então focamos apenas na string de “esperados”.
            String esperado = normalizeExpected(esperadoRaw);

            // Heurística: se travou em 'end' e faltava ',' ou ')', a linha-alvo é a anterior (caso típico do PDF)
            if ("end".equals(encontrado) && (esperado.contains(",") || esperado.contains(")"))) {
                linha = Math.max(1, linha - 1);
            }

            // Ajuste “EOF sem end”: se o arquivo não termina com \n, a linha é a seguinte
            if ("EOF".equals(encontrado) && !source.endsWith("\n")) {
                linha = linha + 1;
            }

            out.add(String.format("linha %d: encontrado %s %s", linha, encontrado, esperado));
            return out;

        } catch (SemanticError e) {
            int pos = e.getPosition();
            int linha = toLineNumber(source, pos);
            out.add(String.format("linha %d: erro semântico: %s", linha, e.getMessage()));
            return out;
        }
    }

    // ---------- Helpers ----------

    private static int toLineNumber(String src, int pos) {
        if (pos <= 0) return 1;
        int lines = 1;
        int lim = Math.min(pos, src.length());
        for (int i = 0; i < lim; i++) if (src.charAt(i) == '\n') lines++;
        return lines;
    }

    private static String normalizeLexMsg(String msg) {
        if (msg == null || msg.isEmpty()) return "erro léxico";
        if ("Caractere não esperado".equals(msg)) return "símbolo inválido";
        return msg;
    }

    /** “Encontrado …” conforme o enunciado. */
    private static String guessFoundTokenForMessage(String src, int pos) {
        if (pos < 0 || pos >= src.length()) return "EOF";
        int i = pos;
        while (i < src.length() && Character.isWhitespace(src.charAt(i))) i++;
        if (i >= src.length()) return "EOF";

        char c = src.charAt(i);
        if (c == '"') return "constante_string";

        if (Character.isLetterOrDigit(c) || c == '_') {
            int j = i + 1;
            while (j < src.length()
                    && !Character.isWhitespace(src.charAt(j))
                    && "()[]{};,:+-*/<>=!^|".indexOf(src.charAt(j)) == -1) j++;
            return src.substring(i, Math.min(j, i + 40));
        }
        if (i + 1 < src.length()) {
            String two = src.substring(i, i + 2);
            if ("==".equals(two) || "~=".equals(two) || "<-".equals(two)) return two;
        }
        return String.valueOf(c);
    }

    /** Normaliza “esperado …” conforme as regras do PDF + mapeia <token N> usando Constants. */
    private static String normalizeExpected(String msg) {
        if (msg == null || msg.isBlank()) return "esperado símbolo";

        String m = msg;

        // 1) “Era esperado …” -> “esperado …”
        m = m.replaceFirst("^[Ee]ra esperado\\s+", "esperado ");

        // 2) fim de programa -> EOF
        m = m.replace("fim de programa", "EOF");

        // 3) léxicos base
        m = m.replace("identificador", "identificador");
        m = m.replace("cint", "constante_int");
        m = m.replace("cfloat", "constante_float");
        m = m.replace("cstring", "constante_string");

        // 4) pr_* -> remover “pr_”
        m = m.replaceAll("\\bpr_", "");

        // 5) remover aspas dos símbolos
        m = m.replace("\"", "");

        // 6) família expressão
        if (m.contains("<expressao>") || m.contains("<expressao_>")
                || m.contains("<valor>") || m.contains("<relacional>") || m.contains("<relacional_>")
                || m.contains("<aritmetica>") || m.contains("<aritmetica_>")
                || m.contains("<termo>") || m.contains("<termo_>")
                || m.contains("<fator>") || m.contains("<fator_>")) {
            m = "esperado expressão";
        }

        // 7) lista de expressões
        if (m.contains("<lista_expressões>") || m.contains("<lista_expressões1>")) {
            m = "esperado expressao";
        }

        // 8) tipo
        if (m.contains("<tipo>")) {
            m = "esperado tipo";
        }

        // 9) converter todos os <token N> para nomes reais conforme Constants
        Pattern p = Pattern.compile("<token\\s+(\\d+)>");
        Matcher matcher = p.matcher(m);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            String name = tokenIdToDisplay(id);
            matcher.appendReplacement(sb, name);
        }
        matcher.appendTail(sb);
        m = sb.toString();

        // 10) limpar restos
        m = m.replaceAll("\\s{2,}", " ").trim();
        m = m.replaceAll(",\\s*,", ", ").replaceAll(",\\s*$", "");

        // 11) garantir exatamente um “esperado ” no começo
        if (!m.startsWith("esperado ")) m = "esperado " + m;
        m = m.replaceAll("^esperado\\s+esperado\\s+", "esperado ");

        return m;
    }

    /** Mapeia IDs de token (Constants) para o texto exibido na mensagem “esperado …”. */
    private static String tokenIdToDisplay(int id) {
        // classes léxicas
        if (id == Constants.t_identificador) return "identificador";
        if (id == Constants.t_cint)         return "constante_int";
        if (id == Constants.t_cfloat)       return "constante_float";
        if (id == Constants.t_cstring)      return "constante_string";

        // reservadas
        if (id == Constants.t_pr_add)    return "add";
        if (id == Constants.t_pr_and)    return "and";
        if (id == Constants.t_pr_begin)  return "begin";
        if (id == Constants.t_pr_bool)   return "bool";
        if (id == Constants.t_pr_count)  return "count";
        if (id == Constants.t_pr_delete) return "delete";
        if (id == Constants.t_pr_do)     return "do";
        if (id == Constants.t_pr_elementOf) return "elementOf";
        if (id == Constants.t_pr_else)   return "else";
        if (id == Constants.t_pr_end)    return "end";
        if (id == Constants.t_pr_false)  return "false";
        if (id == Constants.t_pr_float)  return "float";
        if (id == Constants.t_pr_if)     return "if";
        if (id == Constants.t_pr_int)    return "int";
        if (id == Constants.t_pr_not)    return "not";
        if (id == Constants.t_pr_or)     return "or";
        if (id == Constants.t_pr_print)  return "print";
        if (id == Constants.t_pr_list)   return "list";
        if (id == Constants.t_pr_read)   return "read";
        if (id == Constants.t_pr_size)   return "size";
        if (id == Constants.t_pr_string) return "string";
        if (id == Constants.t_pr_true)   return "true";
        if (id == Constants.t_pr_until)  return "until";

        // símbolos
        if (id == Constants.t_TOKEN_29) return "+";
        if (id == Constants.t_TOKEN_30) return "-";
        if (id == Constants.t_TOKEN_31) return "*";
        if (id == Constants.t_TOKEN_32) return "/";
        if (id == Constants.t_TOKEN_33) return "(";
        if (id == Constants.t_TOKEN_34) return ")";
        if (id == Constants.t_TOKEN_35) return "=";
        if (id == Constants.t_TOKEN_36) return ",";
        if (id == Constants.t_TOKEN_37) return "<-";
        if (id == Constants.t_TOKEN_38) return "==";
        if (id == Constants.t_TOKEN_39) return "<";
        if (id == Constants.t_TOKEN_40) return ">";
        if (id == Constants.t_TOKEN_41) return ";";
        if (id == Constants.t_TOKEN_42) return "~=";

        // fallback
        return "símbolo";
    }
}
