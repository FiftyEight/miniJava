/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import  miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Expression extends AST {

  public Type type; //all expressions will be decorated witha type

  public Expression(SourcePosition posn) {
    super (posn);
  }
  
}
