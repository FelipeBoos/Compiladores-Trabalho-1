package br.com.trabalho.compilerui.compiler;

public class EnhancedSemantico extends Semantico{

    private final SemanticContext ctx = new SemanticContext();

    public SemanticContext getCtx() {
        return ctx;
    }

    /** Vamos usar depois para pegar o código IL gerado. */
    public String getCodigoGerado() {
        return ctx.getCodigoFinal();
    }

    @Override
    public void executeAction(int action, Token token) throws SemanticError {
        // Se quiser debugar: System.out.println("Ação #" + action + ", Token: " + token);

        switch (action) {

            // ----------------------------------------------------
            // (1) Início e fim do programa
            // ----------------------------------------------------
            case 100: // <programa> ::= #100 pr_begin ...
                ctx.emitirCabecalhoPrograma();
                break;

            case 101: // ... pr_end #101 ;
                ctx.emitirRodapePrograma();
                break;

            // ----------------------------------------------------
            // (9) Saída: print
            // ----------------------------------------------------
            case 102:
                // cada expressão em <lista_expressões> chama #102
                ctx.escreverExpressaoTopo();
                break;

            case 103: // cint
                ctx.emitirConstanteInteira(token.getLexeme());
                break;

            case 104: // cfloat
                ctx.emitirConstanteFloat(token.getLexeme());
                break;

            case 105: // cstring
                ctx.emitirConstanteString(token.getLexeme());
                break;

            case 106: // "+" binário
                ctx.operarAritmeticaBinaria("add");
                break;

            case 107: // "-" binário
                ctx.operarAritmeticaBinaria("sub");
                break;

            case 108: // "*" binário
                ctx.operarAritmeticaBinaria("mul");
                break;

            case 109: // "/" binário
                ctx.operarAritmeticaBinaria("div");
                break;

            // ----------------------------------------------------
            // (6) Operador aritmético unário "-"
            // ----------------------------------------------------
            case 110:
                ctx.operarMenosUnario();
                break;

            case 111:
                // guardar operador relacional reconhecido
                ctx.setOperadorRelacional(token.getLexeme());
                break;

            case 112:
                // aplicar operador relacional, gerar código e empilhar bool
                ctx.aplicarOperadorRelacional();
                break;

            // ----------------------------------------------------
            // (8) Constantes booleanas e operadores lógicos
            // ----------------------------------------------------
            case 115: // true
                ctx.emitirConstanteTrue();
                break;

            case 116: // false
                ctx.emitirConstanteFalse();
                break;

            case 117: // not
                ctx.operarNot();
                break;

            case 113: // and
                ctx.operarLogicoBinario("and");
                break;

            case 114: // or
                ctx.operarLogicoBinario("or");
                break;

            case 118:
                // ao final do print(...)
                ctx.escreverQuebraDeLinha();
                break;

            // ----------------------------------------------------
            // (10) Entrada: read
            // ----------------------------------------------------
            case 124:
                // cstring opcional antes do id em read(...)
                ctx.escreverStringConstante(token.getLexeme());
                break;

            case 123:
                // id em read(...)
                ctx.lerEntradaEmId(token.getLexeme(), token.getPosition());
                break;

            // ----------------------------------------------------
            // (2) Declaração de variáveis
            // ----------------------------------------------------
            case 120:
                // guarda o tipo da declaração (int, float, string, bool)
                // token.getLexeme() deve ser "int", "float", "string" ou "bool"
                ctx.setTipoAtual(token.getLexeme());
                break;

            case 121:
                // adiciona identificador à lista temporária
                ctx.adicionarIdentificador(token.getLexeme());
                break;

            case 122: {
                // desempilha o tipo da expressão
                String tipoExpr = ctx.popTipo();

                // se expressão for do tipo int64, converter para int64 (conv.i8) antes de stloc
                if ("int64".equals(tipoExpr)) {
                    ctx.emitirLinha("conv.i8");
                }

                // recuperar id armazenado na lista_identificadores (deve ter exatamente 1)
                String idAtrib = null;
                java.util.List<String> ids = ctx.consumirListaIdentificadores();
                if (!ids.isEmpty()) {
                    idAtrib = ids.get(0);
                }

                if (idAtrib == null) {
                    throw new SemanticError("atribuição sem identificador", token.getPosition());
                }

                // opcional: garantir que o id foi declarado (segurança extra)
                String tipoVar = ctx.tipoDe(idAtrib);
                if (tipoVar == null) {
                    throw new SemanticError("identificador nao declarado: " + idAtrib,
                            token.getPosition());
                }

                // gerar stloc id
                ctx.emitirLinha("stloc " + idAtrib);
                break;
            }

            case 119:
                // final da declaração: registra todos ids com o tipo atual
                for (String id : ctx.consumirListaIdentificadores()) {
                    ctx.declararSimbolo(id, ctx.getTipoAtual());
                    // geração de .locals virá depois, quando formos montar o IL
                }
                break;

            // ----------------------------------------------------
            // (3) Uso de identificador em expressão
            // ----------------------------------------------------
            case 130: {
                String id = token.getLexeme();
                String tipo = ctx.tipoDe(id);

                if (tipo == null) {
                    throw new SemanticError("identificador nao declarado: " + id,
                            token.getPosition());
                }

                // empilha tipo do id na pilha_tipos
                ctx.pushTipo(tipo);

                // gera código para carregar o valor armazenado em id
                ctx.emitirLinha("ldloc " + id);

                // se id for int64, converter para float64 em IL (conv.r8)
                if ("int64".equals(tipo)) {
                    ctx.emitirLinha("conv.r8");
                }
                break;
            }
        }
    }
}
