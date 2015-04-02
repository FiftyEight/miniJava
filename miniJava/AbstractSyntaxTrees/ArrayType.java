/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayType extends Type {

	    public ArrayType(Type eltType, SourcePosition posn){
	        super(TypeKind.ARRAY, posn);
	        this.eltType = eltType;
	    }
	        
	    public <A,R> R visit(Visitor<A,R> v, A o) {
	        return v.visitArrayType(this, o);
	    }

		public boolean equals(Type t){
			if(t instanceof ArrayType){
				//two array types
				//you may have done something incorrectly... probably should have double checked
				if (((ArrayType)t).eltType.typeKind == this.eltType.typeKind){
					//two arrays containing the same kind of base types
					//don't waste too much time on an edge case
					if(((ArrayType)t).eltType.typeKind == typeKind.CLASS){
						return ((ClassType)((ArrayType)t).eltType).equals(this.eltType);
					}
					else return true;

				}
			}
			return false;
		}

	    public Type eltType;
	}

