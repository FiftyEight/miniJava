/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BinaryExpr extends Expression
{

    //MODIFICATION: constructor now examines operator to determine comparison or arithmetic (is this the right way to do it)
    public BinaryExpr(Operator o, Expression e1, Expression e2, SourcePosition posn){
        super(posn);
        operator = o;
        left = e1;
        right = e2;

        String s = o.spelling;

        if(s != null){
            if(s.equals("<") || s.equals(">") || s.equals("<=") || s.equals(">=") || s.equals("!=") || s.equals("&&") || s.equals("||") || s.equals("==")){
                resultType = TypeKind.BOOLEAN;
            }
            else if(s.equals("+") || s.equals("*") || s.equals("/") || s.equals("-")){
                resultType = TypeKind.INT;
            }
        }

    }
        
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitBinaryExpr(this, o);
    }
    
    public Operator operator;
    public Expression left;
    public Expression right;
    public TypeKind resultType;
}