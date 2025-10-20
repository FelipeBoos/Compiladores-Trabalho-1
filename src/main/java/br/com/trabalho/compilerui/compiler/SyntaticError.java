package br.com.trabalho.compilerui.compiler;

import br.com.trabalho.compilerui.compiler.AnalysisError;

public class SyntaticError extends AnalysisError
{
    public SyntaticError(String msg, int position)
	 {
        super(msg, position);
    }

    public SyntaticError(String msg)
    {
        super(msg);
    }
}
