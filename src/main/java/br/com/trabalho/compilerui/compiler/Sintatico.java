package br.com.trabalho.compilerui.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Sintatico implements Constants, ParserConstants {

    private final Stack<Integer> stack = new Stack<>();
    private Token currentToken;
    private Token previousToken;
    private Lexico scanner;
    private Semantico semanticAnalyser;

    private static boolean isTerminal(int x) {
        return x < FIRST_NON_TERMINAL;
    }
    private static boolean isNonTerminal(int x) {
        return x >= FIRST_NON_TERMINAL && x < FIRST_SEMANTIC_ACTION;
    }
    private static boolean isSemanticAction(int x) {
        return x >= FIRST_SEMANTIC_ACTION;
    }

    public void parse(Lexico scanner, Semantico semanticAnalyser)
            throws LexicalError, SyntaticError, SemanticError {
        this.scanner = scanner;
        this.semanticAnalyser = semanticAnalyser;

        stack.clear();
        stack.push(DOLLAR);
        stack.push(START_SYMBOL);

        currentToken = scanner.nextToken();

        while (!step()) { /* loop */ }
    }

    private boolean step() throws LexicalError, SyntaticError, SemanticError {
        if (currentToken == null) {
            int pos = 0;
            if (previousToken != null) {
                pos = previousToken.getPosition() + previousToken.getLexeme().length();
            }
            currentToken = new Token(DOLLAR, "$", pos);
        }

        int x = stack.pop();
        int a = currentToken.getId();

        if (x == EPSILON) {
            return false;
        } else if (isTerminal(x)) {
            if (x == a) {
                if (stack.empty()) return true;
                previousToken = currentToken;
                currentToken = scanner.nextToken();
                return false;
            } else {
                // erro: terminal no topo diferente do token de entrada
                String esperado = "esperado " + tokenDisplay(x);
                throw new SyntaticError(esperado, currentToken.getPosition());
            }
        } else if (isNonTerminal(x)) {
            if (pushProduction(x, a)) {
                return false;
            } else {
                // erro: não há produção – listar todos os TERM possíveis dessa linha
                String esperado = expectedFromTableRow(x);
                throw new SyntaticError(esperado, currentToken.getPosition());
            }
        } else { // ação semântica
            semanticAnalyser.executeAction(x - FIRST_SEMANTIC_ACTION, previousToken);
            return false;
        }
    }

    private boolean pushProduction(int topStack, int tokenInput) {
        int p = PARSER_TABLE[topStack - FIRST_NON_TERMINAL][tokenInput - 1];
        if (p >= 0) {
            int[] production = PRODUCTIONS[p];
            for (int i = production.length - 1; i >= 0; i--) {
                stack.push(production[i]);
            }
            return true;
        }
        return false;
    }

    /** Monta "esperado , )" a partir da linha da tabela. */
    private String expectedFromTableRow(int nonTerminal) {
        List<String> expects = new ArrayList<>();
        int row = nonTerminal - FIRST_NON_TERMINAL;

        // terminais são 1..(FIRST_NON_TERMINAL-1). Coluna = tokenId-1
        for (int term = 1; term < FIRST_NON_TERMINAL; term++) {
            int col = term - 1;
            if (PARSER_TABLE[row][col] >= 0) {
                String name = tokenDisplay(term);
                if (name != null && !expects.contains(name)) {
                    expects.add(name);
                }
            }
        }
        if (expects.isEmpty()) {
            // fallback: mensagem padrão do não-terminal
            return PARSER_ERROR[nonTerminal];
        }
        return "esperado " + String.join(" ", orderExpected(expects));

    }

    private List<String> orderExpected(List<String> list) {
        if (list.contains(",") && list.contains(")")) {
            List<String> ordered = new ArrayList<>();
            ordered.add(",");
            ordered.add(")");
            return ordered;
        }
        return list;
    }

    /** Converte id de token para o texto mostrado ao usuário. */
    private String tokenDisplay(int tokenId) {
        // tokens léxicos básicos
        if (tokenId == t_identificador) return "id";
        if (tokenId == t_cint)         return "cint";
        if (tokenId == t_cfloat)       return "cfloat";
        if (tokenId == t_cstring)      return "cstring";

        // palavras-reservadas usadas no projeto (adicione outras se desejar)
        if (tokenId == t_pr_begin)  return "begin";
        if (tokenId == t_pr_end)    return "end";
        if (tokenId == t_pr_read)   return "read";
        if (tokenId == t_pr_print)  return "print";
        if (tokenId == t_pr_int)    return "int";
        if (tokenId == t_pr_float)  return "float";
        if (tokenId == t_pr_string) return "string";
        if (tokenId == t_pr_if)     return "if";
        if (tokenId == t_pr_else)   return "else";
        if (tokenId == t_pr_do)     return "do";
        if (tokenId == t_pr_until)  return "until";
        if (tokenId == t_pr_true)   return "true";
        if (tokenId == t_pr_false)  return "false";

        // símbolos. Pelo GALS do projeto, 33..42 cobrem: ( ) = , <- == < > ; ~=
        switch (tokenId) {
            case 33: return "(";
            case 34: return ")";
            case 35: return "=";
            case 36: return ",";
            case 37: return "<-";
            case 38: return "==";
            case 39: return "<";
            case 40: return ">";
            case 41: return ";";
            case 42: return "~=";
            default: break;
        }
        // fallback geral (ainda legível)
        return "<token " + tokenId + ">";
    }
}