package br.com.trabalho.compilerui.compiler;

public class Lexico implements Constants {

    private int position;
    private String input;

    public Lexico() {
        setInput("");
    }

    public Lexico(java.io.Reader reader) {
        setInput(reader);
    }

    /** Conveniência para usar direto com String (como fazemos na UI). */
    public void setInput(String text) {
        this.input = (text != null) ? text : "";
        setPosition(0);
    }

    /** Mantém compatibilidade com o código gerado pelo GALS. */
    public void setInput(java.io.Reader reader) {
        StringBuilder sb = new StringBuilder();
        try {
            int c = reader.read();
            while (c != -1) {
                sb.append((char) c);
                c = reader.read();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        this.input = sb.toString();
        setPosition(0);
    }

    public void setPosition(int pos) {
        this.position = pos;
    }

    public Token nextToken() throws LexicalError {
        if (!hasInput()) return null;

        int start = position;
        int state = 0;
        int lastState = 0;
        int endState = -1;
        int end = -1;

        while (hasInput()) {
            lastState = state;
            state = nextState(nextChar(), state);

            if (state < 0) break;

            if (tokenForState(state) >= 0) {
                endState = state;
                end = position;
            }
        }

        if (endState < 0 || (endState != state && tokenForState(lastState) == -2)) {
            // posição do erro = início do lexema problemático (start),
            // isso é o que o ParserRunner espera para calcular a linha.
            throw new LexicalError(SCANNER_ERROR[lastState], start);
        }

        position = end;

        int token = tokenForState(endState);
        if (token == 0) {
            // 0 = ignorar (espaços/comentários, etc.)
            return nextToken();
        } else {
            String lexeme = input.substring(start, end);
            token = lookupToken(token, lexeme);
            return new Token(token, lexeme, start);
        }
    }

    private int nextState(char c, int state) {
        int start = SCANNER_TABLE_INDEXES[state];
        int end   = SCANNER_TABLE_INDEXES[state + 1] - 1;

        while (start <= end) {
            int half = (start + end) / 2;
            int sc = SCANNER_TABLE[half][0];

            if (sc == c) return SCANNER_TABLE[half][1];
            if (sc < c)  start = half + 1;
            else         end   = half - 1;
        }
        return -1;
    }

    private int tokenForState(int state) {
        if (state < 0 || state >= TOKEN_STATE.length) return -1;
        return TOKEN_STATE[state];
    }

    public int lookupToken(int base, String key) {
        int start = SPECIAL_CASES_INDEXES[base];
        int end   = SPECIAL_CASES_INDEXES[base + 1] - 1;

        while (start <= end) {
            int half = (start + end) / 2;
            int comp = SPECIAL_CASES_KEYS[half].compareTo(key);

            if (comp == 0) return SPECIAL_CASES_VALUES[half];
            if (comp < 0)  start = half + 1;
            else           end   = half - 1;
        }
        return base;
    }

    private boolean hasInput() {
        return position < input.length();
    }

    private char nextChar() {
        return hasInput() ? input.charAt(position++) : (char) -1;
    }
}
