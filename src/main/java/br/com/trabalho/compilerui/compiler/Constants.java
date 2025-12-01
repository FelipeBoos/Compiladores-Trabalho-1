package br.com.trabalho.compilerui.compiler;

public interface Constants extends ScannerConstants, ParserConstants
{
    int EPSILON  = 0;
    int DOLLAR   = 1;

    int t_identificador = 2;
    int t_cint = 3;
    int t_cfloat = 4;
    int t_cstring = 5;
    int t_pr_add = 6;
    int t_pr_and = 7;
    int t_pr_begin = 8;
    int t_pr_bool = 9;
    int t_pr_count = 10;
    int t_pr_delete = 11;
    int t_pr_do = 12;
    int t_pr_elementOf = 13;
    int t_pr_else = 14;
    int t_pr_end = 15;
    int t_pr_false = 16;
    int t_pr_float = 17;
    int t_pr_if = 18;
    int t_pr_int = 19;
    int t_pr_not = 20;
    int t_pr_or = 21;
    int t_pr_print = 22;
    int t_pr_list = 23;
    int t_pr_read = 24;
    int t_pr_size = 25;
    int t_pr_string = 26;
    int t_pr_true = 27;
    int t_pr_until = 28;
    int t_TOKEN_29 = 29; //"+"
    int t_TOKEN_30 = 30; //"-"
    int t_TOKEN_31 = 31; //"*"
    int t_TOKEN_32 = 32; //"/"
    int t_TOKEN_33 = 33; //"=="
    int t_TOKEN_34 = 34; //"~="
    int t_TOKEN_35 = 35; //"<"
    int t_TOKEN_36 = 36; //">"
    int t_TOKEN_37 = 37; //"="
    int t_TOKEN_38 = 38; //"<-"
    int t_TOKEN_39 = 39; //"("
    int t_TOKEN_40 = 40; //")"
    int t_TOKEN_41 = 41; //";"
    int t_TOKEN_42 = 42; //","

}
