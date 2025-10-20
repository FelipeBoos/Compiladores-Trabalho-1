package br.com.trabalho.compilerui.compiler;


public class MainSintaticoTest {
    public static void main(String[] args) {
        String fonte = """
            begin
              int x = 10
              float y = 12.0
              string s = "ok"
            end
            """;
        for (String l : ParserRunner.run(fonte)) {
            System.out.println(l);
        }
    }
}
