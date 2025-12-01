package br.com.trabalho.compilerui.compiler;

import java.util.ArrayList;
import java.util.List;

final class ExpectedNormalizer {

    private ExpectedNormalizer() {}

    static String normalize(String galsMsg) {
        if (galsMsg == null || galsMsg.isEmpty()) return "esperado símbolo";

        // Mensagens do GALS começam com "Era esperado ..." ou "<não-terminal> inválido"
        if (galsMsg.startsWith("Era esperado")) {
            // Extrai a parte depois de "Era esperado "
            String rest = galsMsg.substring("Era esperado".length()).trim();
            return normalizeExpectedAtoms(rest);
        }

        // Não-terminais -> regras do enunciado
        // Tudo que é “expressão” (várias variantes no .gals) vira “expressão”
        if (galsMsg.contains("<expressao") || galsMsg.contains("<relacional")
                || galsMsg.contains("<aritmetica") || galsMsg.contains("<termo")
                || galsMsg.contains("<fator")) {
            return "esperado expressão";
        }
        // Lista de expressões do trabalho 3
        if (galsMsg.contains("<lista_expressões>") || galsMsg.contains("<lista_expressões1>")) {
            return "esperado expressao";
        }

        // Demais não-terminais: mantém genérico
        return "esperado símbolo";
    }

    /** Mapeia átomos individuais do GALS para o texto recomendado. */
    private static String normalizeExpectedAtoms(String rest) {
        // separa por espaço quando há só um; se for um dos símbolos entre aspas manter como está
        // o GALS já entrega um único token por vez aqui (para FIRST/FOLLOW simples)
        // mas deixamos pronto para múltiplos (se vierem separados por espaço).
        String[] rawAtoms = rest.split("\\s+");

        List<String> out = new ArrayList<>();
        for (String a : rawAtoms) {
            String t = a.trim();

            // Palavras-chave e classes léxicas
            if ("identificador".equals(t))           { out.add("identificador"); continue; }
            if ("cint".equals(t))                    { out.add("constante_int"); continue; }
            if ("cfloat".equals(t))                  { out.add("constante_float"); continue; }
            if ("cstring".equals(t))                 { out.add("constante_string"); continue; }
            if ("pr_int".equals(t) || "pr_float".equals(t) ||
                    "pr_bool".equals(t) || "pr_string".equals(t) || "pr_list".equals(t)) {
                // regra do enunciado pede: “tipo” quando TODOS são esperados ao mesmo tempo;
                // como aqui o GALS nos dá átomo a átomo, não conseguimos detectar o conjunto,
                // então preferimos emitir exatamente o literal que o programador deve digitar:
                out.add(mapReservedWord(t));
                continue;
            }

            // EOF
            if ("fim de programa".equalsIgnoreCase(t)) { out.add("EOF"); continue; }

            // Símbolos especiais: o GALS já vem entre aspas — manter
            if (t.startsWith("\"") && t.endsWith("\"")) {
                out.add(t.substring(1, t.length() - 1));
                continue;
            }

            // Por segurança, remove marcas estranhas tipo "<-"
            out.add(t);
        }

        // Junta: se vier mais de um, separe por espaço mesmo.
        // O ParserRunner encaixa “esperado ...” depois.
        String joined = String.join(" ", out);

        // Ajuste de estética para alguns símbolos muito comuns:
        joined = joined.replace("EOF", "EOF");

        return "esperado " + joined;
    }

    private static String mapReservedWord(String pr) {
        switch (pr) {
            case "pr_int":    return "int";
            case "pr_float":  return "float";
            case "pr_bool":   return "bool";
            case "pr_string": return "string";
            case "pr_list":   return "list";
            default:          return pr;
        }
    }
}
