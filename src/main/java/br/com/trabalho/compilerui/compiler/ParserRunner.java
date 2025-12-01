package br.com.trabalho.compilerui.compiler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class ParserRunner {

    private ParserRunner() {}

    public static List<String> run(String source) {
        List<String> out = new ArrayList<>();

        Lexico lexico = new Lexico();
        lexico.setInput(new StringReader(source));

        Sintatico sintatico = new Sintatico();
        EnhancedSemantico semantico = new EnhancedSemantico();

        try {
            sintatico.parse(lexico, semantico);
            out.add("programa compilado com sucesso");

            // Opcional: exibir também o código IL gerado
            String il = semantico.getCodigoGerado();
            if (il != null && !il.isBlank()) {
                out.add("----------- CÓDIGO IL -----------");
                for (String linha : il.split("\\R")) {
                    if (!linha.isBlank()) {
                        out.add(linha);
                    }
                }
            }

            return out;

        } catch (LexicalError e) {
            int pos = e.getPosition();
            int linha = SyntaxMessageBuilder.toLineNumber(source, pos);
            String raw = e.getMessage();

            // String mal formada, conforme enunciado (parte 2)
            if (raw != null && raw.toLowerCase().contains("cstring")) {
                out.add(String.format("linha %d: constante_string inválida", linha));
                return out;
            }

            char ch = (pos >= 0 && pos < source.length()) ? source.charAt(pos) : '?';
            String msg = normalizeLexMsg(raw);
            out.add(String.format("linha %d: %c %s", linha, ch, msg));
            return out;

        } catch (SyntaticError e) {
            int pos = e.getPosition();
            int linha = SyntaxMessageBuilder.toLineNumber(source, pos);

            String encontrado = SyntaxMessageBuilder.guessFoundLexeme(source, pos);
            String esperadoRaw = e.getMessage();
            String esperado = normalizeExpected(source, pos, encontrado, esperadoRaw);

            // Recuar linha SOMENTE quando encontrado == end ou EOF e esperado inclui , ou )
            if (shouldShiftLineBack(encontrado, esperado)) {
                linha = Math.max(1, linha - 1);
            }

            out.add(String.format("linha %d: encontrado %s esperado %s", linha, encontrado, esperado));
            return out;

        } catch (SemanticError e) {
            int pos = e.getPosition();
            int linha = SyntaxMessageBuilder.toLineNumber(source, pos);
            out.add(String.format("linha %d: erro semântico: %s", linha, e.getMessage()));
            return out;
        }
    }

    // === Helpers ===

    private static String normalizeLexMsg(String msg) {
        if (msg == null || msg.isEmpty()) return "erro léxico";
        // mapeamentos exigidos na parte 2:
        if ("Caractere não esperado".equals(msg)) return "símbolo inválido";
        if (msg.contains("<ignorar>")) return "símbolo inválido"; // bloco /* ... */ não fechado ou similar
        if (msg.equals("Erro identificando cstring")) return "constante_string inválida";
        return msg;
    }

    /** Normaliza mensagens de não-terminais e tokens conforme observações do enunciado. */
    private static String normalizeExpected(String src, int pos, String encontrado, String raw) {
        if (raw == null || raw.isBlank()) return "símbolo";

        if (raw.toLowerCase().contains("fim de programa")) return "EOF";

        // Erro logo no início: <programa> inválido → esperado begin
        if (raw.contains("<programa>")) {
            return "begin";
        }

        // <lista_entrada> ou <lista_entrada1> => esperado ", )"
        if (raw.contains("<lista_entrada1>") || raw.contains("<lista_entrada>")) {
            String enc = encontrado.toLowerCase();

            if (enc.equals("if") || enc.equals("else") || enc.equals("end") || enc.equals("eof")) {
                return ", )";
            }

            return "identificador constante_string";
        }

        // S3/S2: Se encontramos ';' dentro de call (print/read) com '(' não fechado, priorize ')'
        if (";".equals(encontrado) && insideCallMissingParen(src, pos)) {
            return ")";
        }

        // Conjunto que deve virar “expressão”
        if (raw.contains("<lista_expressões>") || raw.contains("<lista_de_expressoes>")
                || raw.contains("<expressao>") || raw.contains("<expressao_>")
                || raw.contains("<valor>") || raw.contains("<relacional>")
                || raw.contains("<relacional_>") || raw.contains("<aritmetica>")
                || raw.contains("<aritmetica_>") || raw.contains("<termo>")
                || raw.contains("<termo_>") || raw.contains("<fator>") || raw.contains("<fator_>")
                || raw.contains("<elemento>") || raw.contains("<posição>") || raw.contains("<posicao>")) {
            return "expressão";
        }



        // Se for algum não-terminal 'inválido' e estamos dentro de call com '(' aberto,
        // ainda assim preferimos ')'
        if (raw.contains("inválido") && insideCallMissingParen(src, pos)) {
            return ")";
        }

        // Identificador e constantes conforme enunciado
        String r = raw.replace("Era esperado identificador", "identificador")
                .replace("Era esperado cint", "constante_int")
                .replace("Era esperado cfloat", "constante_float")
                .replace("Era esperado cstring", "constante_string");

        // Remover aspas de tokens literais
        r = r.replace("\"", "");

        // Lista de identificadores em declaração
        if (raw.contains("<lista_id>") || raw.contains("<lista_id1>")) {
            if (encontrado != null && encontrado.matches("\\d.*")) {
                return "identificador";
            }
        }

        // Por fim: qualquer outro "inválido" vira "símbolo"
        if (r.contains("inválido")) return "símbolo";

        int idx = r.indexOf("Era esperado ");
        if (idx >= 0) {
            String tail = r.substring(idx + "Era esperado ".length()).trim();
            return tail.isEmpty() ? "símbolo" : tail;
        }

        return r.trim().isEmpty() ? "símbolo" : r.trim();
    }

    private static boolean insideCallMissingParen(String src, int pos) {
        // volta ao início da linha
        int lineStart = pos;
        while (lineStart > 0 && src.charAt(lineStart - 1) != '\n') lineStart--;

        String slice = src.substring(lineStart, Math.min(src.length(), pos));

        // procurar a última ocorrência de 'print' ou 'read' na linha antes do erro
        int idxPrint = slice.lastIndexOf("print");
        int idxRead  = slice.lastIndexOf("read");
        int anchor = Math.max(idxPrint, idxRead);
        if (anchor < 0) return false;

        // procurar '(' a partir do anchor
        int openIdx = slice.indexOf('(', anchor);
        if (openIdx < 0) return false;

        // contar parênteses de openIdx até o ponto do erro
        int opens = 0, closes = 0;
        for (int i = openIdx; i < slice.length(); i++) {
            char ch = slice.charAt(i);
            if (ch == '(') opens++;
            else if (ch == ')') closes++;
        }
        return opens > closes; // há '(' sem o ')' correspondente
    }

    /**
     * Recuar uma linha apenas quando o parser parou em 'end' ou 'EOF' e
     * o esperado contém ')' ou ','. Isso cobre o caso clássico de bloco/chamada
     * não fechada cuja falta só é percebida no 'end' ou no fim do arquivo.
     */
    private static boolean shouldShiftLineBack(String encontrado, String esperado) {
        if (esperado == null) return false;
        boolean expectingCloseOrComma = esperado.contains(")") || esperado.contains(",");
        if (!expectingCloseOrComma) return false;

        String enc = encontrado.toLowerCase();
        return enc.equals("end") || enc.equals("eof");
    }
}
