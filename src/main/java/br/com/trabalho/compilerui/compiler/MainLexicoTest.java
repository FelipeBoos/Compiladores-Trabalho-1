package br.com.trabalho.compilerui.compiler;

import java.util.List;

public class MainLexicoTest {

    public static void main(String[] args) {
        // --- Casos do enunciado / validações básicas ---

        // T1: simples, sem erro
        String fonte1 = """
                begin
                  int x = 10
                  
                  string s = "ok"
                end
                """;

        // T2: cstring faltando aspas de fechamento (gera erro léxico)
        String fonte2 = """
                begin
                  int x = 1
                  string s =
                end
                """;

        // T3: cstring iniciada sem fechar (erro léxico)
        String fonte3 = """
                begin
                  string s =
                end
                """;

        // T4: números + strings OK
        String fonte4 = """
                begin
                  int x = 1
                  string s = "ok"
                end
                """;

        // T5: com linhas em branco e espaços
        String fonte5 = """
                begin

                  int x = 1

                  string s = "ok"
                end
                """;

        // T6: ponto solto após float (deve tokenizar '.' como simbolo)
        String fonte6 = """
                begin
                  float y = .
                end
                """;

        // T7: identificador após número (conforme regex, número e depois id)
        String fonte7 = """
                begin
                  int 1 abc = 0
                end
                """;

        // Exemplo do PDF (comentário de bloco, símbolos, etc.)
        String fontePdfOk = """
                {
                isso é um comentário de bloco
                }
                add area_1 (
                1.0
                0
                """;

        // Exemplo do PDF com erro: símbolo inválido '@' na linha 5
        String fontePdfErro = """
                {
                isso é um comentário de bloco
                }
                add area_1 (@
                1.00
                """;

        // Comentários (linha e bloco) + símbolos diversos
        String fonteComentarios = """
                begin
                  int x = 10
                  { bloco
                    de
                    comentario
                    com varias linhas e simbolos: == ~= <- + - * / ( ) { } [ ] : , ^ ? 
                  }
                  -- comentario de linha até o fim
                  string s = "ok"
                end
                """;

        // Rode os casos
        runCase("T1 - simples OK", fonte1);
        runCase("T2 - string faltando aspas", fonte2);
        runCase("T3 - string iniciada sem fechar", fonte3);
        runCase("T4 - exemplo OK (números e strings)", fonte4);
        runCase("T5 - espaços/linhas em branco", fonte5);
        runCase("T6 - ponto solto após float", fonte6);
        runCase("T7 - número seguido de identificador", fonte7);
        runCase("PDF - exemplo sem erro (ajustado)", fontePdfOk);
        runCase("PDF - exemplo com erro (@ inválido)", fontePdfErro);
        runCase("Comentários + símbolos", fonteComentarios);
    }

    private static void runCase(String name, String source) {
        System.out.println("==== " + name + " ====");
        List<String> out = LexicalRunner.run(source);
        out.forEach(System.out::println);
        System.out.println();
    }
}