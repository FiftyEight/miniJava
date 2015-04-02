package miniJava.SyntacticAnalyzer;

import java.util.Arrays;
import java.util.List;
import java.util.Arrays;


public class Token{

    public TokenKind kind;
    public String spelling;
    public SourcePosition posn;

    //an array of keyword spellings to make constructor simpler
    private static List<String> keyword_spellings = Arrays.asList(
            "class",
            "public",
            "private",
            "static",
            "void",
            "int",
            "boolean",
            "return",
            "this",
            "if",
            "else",
            "while",
            "true",
            "false",
            "new"
    );

    public Token(TokenKind kind, String spelling){
        //creates a token of type 'kind' with identifier 'spelling'

        this.kind = kind;
        this.spelling = spelling;

        if(this.kind == TokenKind.ID){
            //figure our what kind of token it is (keyword, id)
            if(keyword_spellings.contains(spelling)){
                //TODO: DOES THIS WORK?
                this.kind = TokenKind.valueOf(spelling.toUpperCase());
            }
        }
    }

}
