package br.com.trabalho.compilerui.compiler;

import java.util.*;

public class SemanticContext {

    // Registradores semânticos básicos
    private String operadorRelacional = "";
    private String tipoAtual = ""; // tipo lido em <simples> para uso em declaração

    // Código objeto IL
    private final StringBuilder codigoObjeto = new StringBuilder();

    // Pilhas
    private final Deque<String> pilhaTipos   = new ArrayDeque<>();
    private final Deque<String> pilhaRotulos = new ArrayDeque<>();

    // Lista de identificadores em uso nas ações #119 e #122
    private final List<String> listaIdentificadores = new ArrayList<>();

    // Tabela de símbolos: id -> tipo (bool, int64, float64, string)
    private final Map<String, String> tabelaSimbolos = new LinkedHashMap<>();

    // Contador de rótulos para if/else/do-until
    private int contadorRotulos = 0;

    // ===== Getters / Setters simples =====

    public String getOperadorRelacional() {
        return operadorRelacional;
    }

    public void setOperadorRelacional(String operadorRelacional) {
        this.operadorRelacional = operadorRelacional;
    }

    public String getTipoAtual() {
        return tipoAtual;
    }

    public void setTipoAtual(String tipoAtual) {
        this.tipoAtual = tipoAtual;
    }

    public StringBuilder getCodigoObjeto() {
        return codigoObjeto;
    }

    public Deque<String> getPilhaTipos() {
        return pilhaTipos;
    }

    public Deque<String> getPilhaRotulos() {
        return pilhaRotulos;
    }

    public List<String> getListaIdentificadores() {
        return listaIdentificadores;
    }

    public Map<String, String> getTabelaSimbolos() {
        return tabelaSimbolos;
    }

    // ===== Helpers para rótulos =====

    /** Cria um rótulo novo: L0, L1, L2, ... */
    public String novoRotulo() {
        return "L" + (contadorRotulos++);
    }

    // ===== Helpers para tipos =====

    /** Empilha tipo de expressão. */
    public void pushTipo(String tipo) {
        pilhaTipos.push(tipo);
    }

    /** Desempilha tipo, lança IllegalStateException se vazio. */
    public String popTipo() {
        if (pilhaTipos.isEmpty()) {
            throw new IllegalStateException("Pilha de tipos vazia");
        }
        return pilhaTipos.pop();
    }

    /** Consulta o topo sem remover (ou null se vazia). */
    public String peekTipo() {
        return pilhaTipos.peek();
    }

    // ===== Helpers para identificadores =====

    public void adicionarIdentificador(String id) {
        listaIdentificadores.add(id);
    }

    public List<String> consumirListaIdentificadores() {
        List<String> copia = new ArrayList<>(listaIdentificadores);
        listaIdentificadores.clear();
        return copia;
    }

    // ===== Helpers para tabela de símbolos =====

    /**
     * Registra uma variável na tabela de símbolos.
     * tipoFonte = "int", "float", "string", "bool"
     * tipoIL    = "int64", "float64", "string", "bool"
     */
    public void declararSimbolo(String id, String tipoFonte) {
        String tipoIL = switch (tipoFonte) {
            case "int"    -> "int64";
            case "float"  -> "float64";
            case "string" -> "string";
            case "bool"   -> "bool";
            default       -> tipoFonte; // fallback
        };

        // registra na tabela de símbolos
        tabelaSimbolos.put(id, tipoIL);

        // gera declaração da variável em IL, conforme esquema:
        // .locals (tipo id)
        emitirLinha(".locals (" + tipoIL + " " + id + ")");
    }

    public String tipoDe(String id) {
        return tabelaSimbolos.get(id);
    }

    // ===== Helpers para código IL =====

    public void emitirLinha(String linha) {
        codigoObjeto.append(linha).append('\n');
    }

    public void emitirSemQuebra(String trecho) {
        codigoObjeto.append(trecho);
    }

    /** Retorna o código IL completo em string. */
    public String getCodigoFinal() {
        return codigoObjeto.toString();
    }

    // ===== Helpers específicos para o programa IL =====

    /** Gera o cabeçalho padrão do programa IL (ação #100). */
    public void emitirCabecalhoPrograma() {
        emitirLinha("// cabeçalho");
        emitirLinha(".assembly extern mscorlib {}");
        emitirLinha(".assembly _programa{}");
        emitirLinha(".module _programa.exe");
        emitirLinha(".class public _unica{");
        emitirLinha(" .method static public void _principal(){");
        emitirLinha(" .entrypoint");
        emitirLinha("// cabeçalho");
    }

    /** Fecha o método e a classe do programa IL (ação #101). */
    public void emitirRodapePrograma() {
        emitirLinha("// fim de programa");
        emitirLinha("ret");
        emitirLinha(" }");
        emitirLinha("}");
    }

    // ===== Helpers para constantes =====

    public void emitirConstanteInteira(String lexeme) {
        // Gera um inteiro 64 bits no stack
        emitirLinha("ldc.i8 " + lexeme);
        pushTipo("int64");
    }

    public void emitirConstanteFloat(String lexeme) {
        // IL usa ponto como separador decimal; assumimos que já vem assim do léxico
        emitirLinha("ldc.r8 " + lexeme);
        pushTipo("float64");
    }

    public void emitirConstanteString(String lexeme) {
        // lexeme já vem com aspas: "texto"
        emitirLinha("ldstr " + lexeme);
        pushTipo("string");
    }
}
