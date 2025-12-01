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

            case 103: // cint
                ctx.emitirConstanteInteira(token.getLexeme());
                break;

            case 104: // cfloat
                ctx.emitirConstanteFloat(token.getLexeme());
                break;

            case 105: // cstring
                ctx.emitirConstanteString(token.getLexeme());
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
            case 130:
                String id = token.getLexeme();
                String tipo = ctx.tipoDe(id);

                if (tipo == null) {
                    // identificador não foi declarado
                    throw new SemanticError("identificador nao declarado: " + id,
                            token.getPosition());
                }

                // empilha o tipo do identificador para uso nas expressões
                ctx.pushTipo(tipo);

                // a geração de código IL (ldloc, conv, etc.) vamos adicionar depois
                break;
        }
    }
}
