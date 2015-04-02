package miniJava.SyntacticAnalyzer;

/**
 * Created by cloftin on 1/26/15.
 */
public enum TokenKind {

    //lexical tokens
    NUM,
    BINOP,
    UNOP,
    ID,
    EOT,
    //TODO: implement EOT token

    //nonterminals
    FIELDDECL,
    METHDECL,
    CLASSDECL,

    //java keywords
    CLASS,
    PUBLIC,
    PRIVATE,
    STATIC,
    VOID,
    INT,
    BOOLEAN,
    RETURN,
    THIS,
    IF,
    ELSE,
    WHILE,
    TRUE,
    FALSE,
    NEW,

    //punctuation
    SEMIC,
    DOT,
    COMMA,

    //operators
    OR,
    AND,
    EQ,
    NEQ,
    LEQ,
    LESS,
    GEQ,
    GREATER,
    PLUS,
    MULT,
    DIV,
    NOT,

    //parentheses and brackets
    LCURL,
    RCURL,
    LPAREN,
    RPAREN,
    LBRACK,
    RBRACK,

    //error
    ERROR,

    BREAK, //accept(tokenKind.BREAK) will cause the parse to fail because BREAK is never scanned

    //special operators
    EQUALS,
    NEG,

    //comments
    LINE_COMMENT,
    BLOCK_COMMENT_START,
    BLOCK_COMMENT_END

}
