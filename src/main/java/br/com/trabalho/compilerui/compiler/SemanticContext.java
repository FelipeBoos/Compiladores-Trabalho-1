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
        // Esquema: empilhar tipo int64, gerar ldc.i8 e converter para float64
        emitirLinha("ldc.i8 " + lexeme);
        emitirLinha("conv.r8");
        pushTipo("int64"); // tipo da linguagem fonte
    }

    public void emitirConstanteFloat(String lexeme) {
        emitirLinha("ldc.r8 " + lexeme);
        pushTipo("float64");
    }

    public void emitirConstanteString(String lexeme) {
        emitirLinha("ldstr " + lexeme);
        pushTipo("string");
    }

    // ===== Operações aritméticas =====

    public void operarAritmeticaBinaria(String opIL) {
        // desempilha tipos dos operandos (direita e esquerda)
        String tipo2 = popTipo(); // operando da direita
        String tipo1 = popTipo(); // operando da esquerda

        // regra simples: se algum for float64, resultado é float64; senão int64
        String resultado;
        if ("float64".equals(tipo1) || "float64".equals(tipo2)) {
            resultado = "float64";
        } else {
            resultado = "int64";
        }

        // empilha tipo resultante
        pushTipo(resultado);

        // gera código IL correspondente: add, sub, mul, div
        emitirLinha(opIL);
    }

    public void operarMenosUnario() {
        // Esquema diz: gerar código objeto para efetuar a operação correspondente
        // Em IL, usamos 'neg' para trocar o sinal do topo da pilha
        emitirLinha("neg");
        // Tipo na pilha não muda, então não mexemos em pilha_tipos aqui
    }

    // ===== Operadores relacionais =====

    public void aplicarOperadorRelacional() {
        // desempilha tipos (não vamos validar compatibilidade, conforme esquema)
        String tipo2 = popTipo(); // direita
        String tipo1 = popTipo(); // esquerda

        // resultado de comparação é sempre bool
        pushTipo("bool");

        // gera IL conforme o operador armazenado
        switch (operadorRelacional) {
            case "==":
                emitirLinha("ceq");
                break;
            case "~=":
                // negação de igualdade: ceq, depois compara com 0
                emitirLinha("ceq");
                emitirLinha("ldc.i4.0");
                emitirLinha("ceq");
                break;
            case "<":
                emitirLinha("clt");
                break;
            case ">":
                emitirLinha("cgt");
                break;
        }
    }

    // ===== Constantes booleanas =====

    public void emitirConstanteTrue() {
        emitirLinha("ldc.i4.1");
        pushTipo("bool");
    }

    public void emitirConstanteFalse() {
        emitirLinha("ldc.i4.0");
        pushTipo("bool");
    }

    // ===== Operadores lógicos =====

    public void operarLogicoBinario(String opIL) {
        // desempilha dois tipos e empilha bool, conforme tabela de tipos
        String t2 = popTipo();
        String t1 = popTipo();
        pushTipo("bool");
        emitirLinha(opIL); // and / or
    }

    public void operarNot() {
        // not: resultado é bool; usamos padrão "== 0"
        // pilha: ... valor
        emitirLinha("ldc.i4.0");
        emitirLinha("ceq");
        // tipo continua bool, então não mexemos na pilha_tipos
    }

    // ===== Saída: escrever valor de expressão =====

    public void escreverExpressaoTopo() {
        String tipo = popTipo(); // tipo da linguagem (bool, int64, float64, string)

        if ("int64".equals(tipo)) {
            // valores int64 foram tratados como float64 em IL → converter de volta para int64
            emitirLinha("conv.i8");
            emitirLinha("call void [mscorlib]System.Console::Write(int64)");
        } else if ("float64".equals(tipo)) {
            emitirLinha("call void [mscorlib]System.Console::Write(float64)");
        } else if ("string".equals(tipo)) {
            emitirLinha("call void [mscorlib]System.Console::Write(string)");
        } else if ("bool".equals(tipo)) {
            emitirLinha("call void [mscorlib]System.Console::Write(bool)");
        } else {
            // fallback: escreve como string
            emitirLinha("call void [mscorlib]System.Console::Write(string)");
        }
    }

    public void escreverQuebraDeLinha() {
        emitirLinha("call void [mscorlib]System.Console::WriteLine()");
    }

    // ===== Entrada: prompt opcional =====

    public void escreverStringConstante(String lexeme) {
        // lexeme já vem com aspas
        emitirLinha("ldstr " + lexeme);
        emitirLinha("call void [mscorlib]System.Console::Write(string)");
    }

    public void lerEntradaEmId(String id, int pos) throws SemanticError {
        String tipo = tipoDe(id);

        if (tipo == null) {
            throw new SemanticError("identificador nao declarado: " + id, pos);
        }

        if ("bool".equals(tipo)) {
            // esquema: bool é inválido para comando de entrada
            throw new SemanticError("id invalido para comando de entrada", pos);
        }

        // Ler uma linha da entrada padrão
        emitirLinha("call string [mscorlib]System.Console::ReadLine()");

        if ("int64".equals(tipo)) {
            emitirLinha("call int64 [mscorlib]System.Int64::Parse(string)");
            emitirLinha("stloc " + id);
        } else if ("float64".equals(tipo)) {
            emitirLinha("call float64 [mscorlib]System.Double::Parse(string)");
            emitirLinha("stloc " + id);
        } else if ("string".equals(tipo)) {
            // já é string
            emitirLinha("stloc " + id);
        } else {
            // tipo estranho → trata como string
            emitirLinha("stloc " + id);
        }
    }
}
