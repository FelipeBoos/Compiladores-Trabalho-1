package br.com.trabalho.compilerui.compiler;

final class LexicalMessageNormalizer {

    private LexicalMessageNormalizer() {}

    static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) return "erro léxico";

        if ("Caractere não esperado".equals(raw))      return "símbolo inválido";
        if (raw.contains("cstring"))                   return "constante_string inválida";
        if (raw.contains("identificador"))             return "identificador inválido";

        if (raw.contains("cfloat"))                    return "constante_float inválida";

        return raw;
    }
}
