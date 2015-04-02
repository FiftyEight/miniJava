package miniJava.SyntacticAnalyzer;

/**
 * Created by cloftin on 1/29/15.
 */
public class SyntaxError extends Error{
    private TokenKind token; //the token that was not accepted (differed from expected token)

}
