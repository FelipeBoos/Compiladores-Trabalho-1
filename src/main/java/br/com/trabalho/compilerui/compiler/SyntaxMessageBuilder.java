package br.com.trabalho.compilerui.compiler;

/** Monta a mensagem "linha X: encontrado Y esperado Z" conforme enunciado. */
final class SyntaxMessageBuilder {

    private SyntaxMessageBuilder() {}

    /** Retorna o lexema "encontrado" a partir da posição do erro. */
    static String guessFoundLexeme(String source, int pos) {
        if (pos < 0 || pos >= source.length()) return "EOF";

        // avança espaços
        int i = pos;
        while (i < source.length() && Character.isWhitespace(source.charAt(i))) i++;
        if (i >= source.length()) return "EOF";

        char c = source.charAt(i);

        // String literal
        if (c == '"') {
            return "constante_string";
        }

        // Identificadores/numéricos
        if (Character.isLetterOrDigit(c) || c == '_' ) {
            int j = i + 1;
            while (j < source.length()) {
                char cj = source.charAt(j);
                if (!(Character.isLetterOrDigit(cj) || cj == '_')) break;
                j++;
            }
            return source.substring(i, j);
        }

        // Operadores de 2 chars que a gramática usa
        if (i + 1 < source.length()) {
            String two = source.substring(i, i + 2);
            if (two.equals("<-") || two.equals("==") || two.equals("~=")) return two;
        }

        // Um único símbolo
        return String.valueOf(c);
    }

    /** Converte offset para número de linha (1-based). */
    static int toLineNumber(String src, int pos) {
        if (pos <= 0) return 1;
        int lines = 1;
        int lim = Math.min(Math.max(pos, 0), src.length());
        for (int i = 0; i < lim; i++) if (src.charAt(i) == '\n') lines++;
        return lines;
    }

    /** Heurística: quando parser para em 'end' faltando ',' ou ')', marcar linha anterior. */
    static int maybeShiftToPreviousLine(String source, int line, String found, String expected) {
        if ("end".equals(found) && (expected.contains(")") || expected.contains(","))) {
            return Math.max(1, line - 1);
        }
        return line;
    }
}
