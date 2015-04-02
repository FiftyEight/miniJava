/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class Identifier extends Terminal {

  public Declaration decl; //we traverse the AST to link this to the appropriate declaration
  public Type type;

  public Identifier (Token t, SourcePosition sp) {
    super (t);
  }

  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitIdentifier(this, o);
  }

}
