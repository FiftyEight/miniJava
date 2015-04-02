/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

public class Operator extends Terminal {

  //modify operator to store an allowed type (or types)

  public boolean boolOp = false;
  public boolean intOp = false;

  public Operator (Token t) {
    super (t);
    if(t.spelling.equals(">") || t.spelling.equals("<") || t.spelling.equals("<=") || t.spelling.equals(">=") ||
            t.spelling.equals("+") || t.spelling.equals("*") || t.spelling.equals("/") || t.spelling.equals("-")){
      this.boolOp = true;
    }
    if(t.spelling.equals("&&") || t.spelling.equals("||") || t.spelling.equals("!")){
      this.intOp = true;
    }
    if(t.spelling.equals("=") || t.spelling.equals("==") || t.spelling.equals("!=")){
      this.intOp = true;
      this.boolOp = true;
    }
  }

  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitOperator(this, o);
  }
}
