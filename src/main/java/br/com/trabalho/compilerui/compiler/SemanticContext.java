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

    // ===== Constantes =====

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

    public void emitirConstanteTrue() {
        emitirLinha("ldc.i4.1");
        pushTipo("bool");
    }

    public void emitirConstanteFalse() {
        emitirLinha("ldc.i4.0");
        pushTipo("bool");
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

    // ===== Operadores lógicos =====

    public void operarLogicoBinario(String opIL) {
        // desempilha dois tipos e empilha bool
        String t2 = popTipo();
        String t1 = popTipo();
        pushTipo("bool");
        emitirLinha(opIL); // and / or
    }

    public void operarNot() {
        // not: resultado é bool; usamos padrão "== 0"
        emitirLinha("ldc.i4.0");
        emitirLinha("ceq");
        // tipo continua bool, então não mexemos na pilha_tipos
    }

    // ===== Saída: print =====

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

    // ===== Entrada: read =====

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

    // ===== Controle de fluxo: if / else =====

    /** Ação #125 - após avaliar a expressão do if. */
    public void iniciarIf() {
        // consumir tipo da expressão condicional na pilha de tipos
        if (!pilhaTipos.isEmpty()) {
            pilhaTipos.pop();
        }

        String rotuloFimIf = novoRotulo();
        pilhaRotulos.push(rotuloFimIf);

        // se condição for falsa, salta para o fim (ou para o else, se existir)
        emitirLinha("brfalse " + rotuloFimIf);
    }

    /** Ação #126 - ao encontrar 'else'. */
    public void iniciarElse() {
        // rótulo que marcava o fim do bloco 'then'
        String rotuloFimIf = pilhaRotulos.pop();

        // novo rótulo para o fim total do if/else
        String rotuloFimTotal = novoRotulo();

        // salta para depois do else
        emitirLinha("br " + rotuloFimTotal);

        // marca início do bloco else
        emitirLinha(rotuloFimIf + ":");

        // empilha o novo rótulo para ser fechado em #127
        pilhaRotulos.push(rotuloFimTotal);
    }

    /** Ação #127 - ao encontrar 'end' do if (com ou sem else). */
    public void finalizarIfElse() {
        String rotuloFim = pilhaRotulos.pop();
        emitirLinha(rotuloFim + ":");
    }

    // ===== Controle de fluxo: do / until =====

    /** Ação #128 - início do 'do'. */
    public void iniciarDo() {
        String rotuloInicio = novoRotulo();
        pilhaRotulos.push(rotuloInicio);
        emitirLinha(rotuloInicio + ":");
    }

    /** Ação #129 - após avaliar expressão do 'until'. */
    public void finalizarDoUntil() {
        // consumir tipo da expressão condicional na pilha de tipos
        if (!pilhaTipos.isEmpty()) {
            pilhaTipos.pop();
        }

        String rotuloInicio = pilhaRotulos.pop();
        // until E -> repete enquanto E for falso -> brfalse
        emitirLinha("brfalse " + rotuloInicio);
    }

    public void atribuirParaIdentificador(String id, int pos) throws SemanticError {
        String tipoVar = tipoDe(id);
        if (tipoVar == null) {
            throw new SemanticError("identificador nao declarado: " + id, pos);
        }

        // tipo da expressão calculada
        String tipoExp = popTipo(); // não vamos validar compatibilidade fina aqui

        // A regra geral:
        // - nossas expressões numéricas deixam um float64 na pilha IL
        // - se a variável é int64, convertemos para i8
        // - se a variável é float64, NÃO convertemos (mantém r8)
        // - para bool/string, assumimos que a expressão já deixou o tipo correto

        if ("int64".equals(tipoVar)) {
            // precisamos armazenar como int64
            emitirLinha("conv.i8");
        } else if ("float64".equals(tipoVar)) {
            // já está em r8, não faz conversão para inteiro
            // NADA aqui
        } else {
            // bool, string etc: não mexemos
        }

        emitirLinha("stloc " + id);
    }
}
