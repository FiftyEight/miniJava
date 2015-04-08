package miniJava.ContextualAnalysis;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

/**
 * Created by cloftin on 3/15/15.
 * Identifier decorates the AST with declarations for each id instance
 * a seperate class, TypeChecker, will perform a second pass to check that all infered types match
 * does it HAVE to implement the visitor interface?
 */
//TODO: ***enforce static access, public private
//TODO: remove all dictionary lookups, declaration parsing

    //GOAL FOR TODAY: GET THE TWO PASS VERION OF THE TYPE CHECKING WORKING, THEN MOVE ON TO ENFORCING ACCESS CONTROL
    //YOU STILL HAVE THE OLD VERSION

    //DO WE NEED IDENT TABLES AFTER WE HAVE IDENTIFIED?
    //when typeCheck is run, all IDs should have declarations, so we won't have scoping issues. We are only concerned with the types

public class TypeCheck<ArgType,ResultType> implements Visitor<ArgType,ResultType>{

    public void check(Package prog){
        //assume standard environment is good to go and fix call statements. should be able to fix that in an hour
        prog.visit(this, null);
    }

    public ResultType visitPackage(Package prog, ArgType arg){
        //iterate through a class declaration list
        Iterator<ClassDecl> it = prog.classDeclList.iterator();

        while(it.hasNext()){
            ClassDecl decl = it.next();
            decl.visit(this,null);
        }
        return null;
    }

    // Declarations
    public ResultType visitClassDecl(ClassDecl cd, ArgType arg){

        Iterator<FieldDecl> fi = cd.fieldDeclList.iterator();
        Iterator<MethodDecl> mi = cd.methodDeclList.iterator();

        //visit fields first, as they may show up in the methods
        while(fi.hasNext()){
            FieldDecl fd = fi.next();
            fd.visit(this,null);
        }

        while(mi.hasNext()){
            MethodDecl md = mi.next();
            md.visit(this,null);
        }

        //check for a single main method here

        return null;
    }


    public ResultType visitFieldDecl(FieldDecl fd, ArgType arg) {
        //visitng a field declaration returns null; we already know its type and nothing can go wrong
        return null;
    }

    public ResultType visitMethodDecl(MethodDecl md, ArgType arg) {

        Iterator<ParameterDecl> pi = md.parameterDeclList.iterator();
        Iterator<Statement> si = md.statementList.iterator();

        while(pi.hasNext()){
            ParameterDecl pd = pi.next();
            pd.visit(this,null);
        }

        while(si.hasNext()){
            Statement st = si.next(); //get the net statement in this statement block
            st.visit(this,null);
        }

        if(md.returnExp == null){
            //no return type, check that it is a void method
            if(md.type.typeKind == TypeKind.VOID){
                return null;
            }
            else{
                System.err.println("***Error (line " + md.posn.lineNumber + "):: no return type for non-void method " + md.name);
                return null;
            }
        }
        else{
            Type returnType = (Type) md.type;
            if(returnType == null){
                System.err.println("***Error (line " + md.posn.lineNumber + "):: return expression for " + md.name + " does not match methods type");
                return null;
            }
            if(!returnType.equals((Type)md.returnExp.visit(this,null))){
                System.err.println("***Error (line " + md.posn.lineNumber + "): return expression for " + md.name + " does not match methods type");
            }
        }
        //TODO: double check type equality methods

        return null;
    }

    public ResultType visitParameterDecl(ParameterDecl pd, ArgType arg) {
        return null;
    }

    public ResultType visitVarDecl(VarDecl decl, ArgType arg) {
        //nothing to do, this decl has already been applied
        return null;
    }

    // Types will be handles by type checker so this returns null
    public ResultType visitBaseType(BaseType type, ArgType arg) {
        return (ResultType) type;
    }

    public ResultType visitClassType(ClassType type, ArgType arg) {
        //TODO: double check class type visiting
        return (ResultType) type;
    }

    public ResultType visitArrayType(ArrayType type, ArgType arg) {
        return (ResultType) type; //just return the array type?
    }

    // Statements (open scope of some sort)
    public ResultType visitBlockStmt(BlockStmt stmt, ArgType arg) {
        //block statement has a statement list
        Iterator<Statement> it = stmt.sl.iterator();

        while(it.hasNext()){
            Statement st = it.next(); //get the net statement in this statement block
            st.visit(this,null);
        }
        return null;
        //TODO: implement sourcePosition
    }

    public ResultType visitVardeclStmt(VarDeclStmt stmt, ArgType arg) {

        Type varType = stmt.varDecl.type; //type declared
        Type valType = (Type) stmt.initExp.visit(this,null);
        if(varType != valType){
            System.err.println("***Error (line " + stmt.posn.lineNumber + "): types don't match in variable declaration");
        }
        return null;
    }

    //TODO: null return types should be UNSUPPORTED or ERROR types

    public ResultType visitAssignStmt(AssignStmt stmt, ArgType arg) {
        //check the variable being assigned and expression types match
        Type t1 = (Type) stmt.val.visit(this,null);
        Type t2 = (Type) stmt.ref.visit(this,null);

        //get the type of the reference, should already be declared
        if(t1.equals(t2)){
            return (ResultType) t1; //return type
        }
        else{
            System.err.println("***Error (line " + stmt.posn.lineNumber + "): types don't match at assignment");
            Type result = new BaseType(TypeKind.ERROR,null);
            return (ResultType) result;
        }
    }


    public ResultType visitCallStmt(CallStmt stmt, ArgType arg) {
        //calling a method, check that method name is in the current scope
        stmt.methodRef.visit(this,null);
        return null; //its a statement, don't bother returning a type
    }

    public ResultType visitIfStmt(IfStmt stmt, ArgType arg) {
        stmt.thenStmt.visit(this,null);

        if(stmt.elseStmt != null){
            stmt.elseStmt.visit(this,null);
        }

        return null;
    }

    //this thing is barely functional, what were you thinking...
    //still need an hour to install it on the department servers...

    public ResultType visitWhileStmt(WhileStmt stmt, ArgType arg) {
        //TODO: add expression ids to the scope
        if(((Type)stmt.cond.visit(this,null)) == null || ((Type)stmt.cond.visit(this,null)).typeKind != TypeKind.BOOLEAN) {
            System.out.println("***Error (line " + stmt.posn.lineNumber + "): while statement conditions must be booleans");
            return null;
        }

        stmt.body.visit(this,null);
        return null;
    }

    // Expressions: these should all return a type
    public ResultType visitUnaryExpr(UnaryExpr expr, ArgType arg) {
        Type t = (Type) expr.expr.visit(this,null);
        Type resultType = (Type) expr.operator.visit(this,null);

        if(t.equals(resultType)){
            return (ResultType) resultType; //cast as a result type, will this work
        }
        else{
            System.err.println("***Error (line " + expr.posn.lineNumber + "): invalid unary expression");
            return (ResultType) new BaseType(TypeKind.ERROR,null);
        }
    }

    //TODO: reference and literal expressions should link to the declarations
    //TODO: split traversal into two passes?

    public ResultType visitBinaryExpr(BinaryExpr expr, ArgType arg) {
        //TODO: this needs to be fixed

        Type lType = (Type) expr.left.visit(this,null);
        Type rType = (Type) expr.right.visit(this,null);
        Type opResult = (Type) expr.operator.visit(this,null);
        //you can do this
        if(lType.equals(rType)){
            return (ResultType) opResult;
        }
        else{
            System.err.println("***Error (line " + expr.posn.lineNumber + "): invalid binary expression");
            return (ResultType) new BaseType(TypeKind.ERROR,null);
        }
    }

    public ResultType visitRefExpr(RefExpr expr, ArgType arg) {
        return expr.ref.visit(this,null);

    }

    public ResultType visitCallExpr(CallExpr expr, ArgType arg) {
        //TODO: call references are a bit more complicated
        return expr.functionRef.visit(this,null);
    }

    public ResultType visitLiteralExpr(LiteralExpr expr, ArgType arg) {
        return expr.lit.visit(this,null);
    }


    //TODO: new object expressions need to be able to tell if somehting is a valid class, search through the list classes in the AST?
    public ResultType visitNewObjectExpr(NewObjectExpr expr, ArgType arg) {
        if(tables.containsKey(expr.classtype.className.spelling)){
            Type result = new ClassType(expr.classtype.className,null);
            return (ResultType) result; //we return a class type
        }
        else{
            System.out.println("***Error (line " + expr.posn.lineNumber + "): no class named " + expr.classtype.className.spelling + " to instantiate");
            Type result = new BaseType(TypeKind.ERROR,null);
            return (ResultType) result;
        }
    }

    //TODO: how do we test this thing?

    public ResultType visitNewArrayExpr(NewArrayExpr expr, ArgType arg) {
        Type arrType = expr.eltType;
        Type result;
        if(arrType.typeKind == TypeKind.BOOLEAN || arrType.typeKind == TypeKind.INT){
            result = new ArrayType(expr.eltType,null);
        }
        else if (arrType.typeKind == TypeKind.CLASS) {
            //class type, check that we have this class
            if(tables.containsKey(((ClassType)arrType).className.spelling)){
                //we have the classes needed to declare an array pf objects of this type
                result = new ArrayType(new ClassType(((ClassType)arrType).className,null),null);
            }
            else{
                System.out.println("***Error (line " + expr.posn.lineNumber + "): class not found for array instantiation");
                result = new BaseType(TypeKind.ERROR,null);
            }
        }
        else{
            //somethings wrong
            System.err.println("***Error (line " + expr.posn.lineNumber + "): invalid array instantiation");
            result = new BaseType(TypeKind.ERROR,null);
        }
        return (ResultType) result;
    }

    // References
    public ResultType visitQualifiedRef(QualifiedRef ref, ArgType arg) {
        //TODO: public private goes here
        return (ResultType) ref.decl.type;
    }

    /*public ResultType visitQualifiedRef(QualifiedRef ref, ArgType arg){
        Reference qualRef = ref.ref;
        Identifier qualId = ref.id;

        //we can only hav arrays of bools or ints, so we only have to worry about qualRef being this, id or qualified

        if(qualRef instanceof IdRef) {
            IdentificationTable thisScope = tables.get(((IdRef) qualRef).id.spelling);
        }
        else if(qualRef instanceof QualifiedRef){

        }
    }*/

    //TODO: standard environment, after debugging

    public ResultType visitIndexedRef(IndexedRef iRef, ArgType arg) {
        //TODO: visit indexedRef is incorrect. fix this and the implicit stuff and you should be fine
        Type t = (Type) iRef.ref.visit(this,null); //this will get the type of the reference and identify it
        Type indexType = (Type) iRef.indexExpr.visit(this,null);

        if(indexType.typeKind != TypeKind.INT){
            System.err.println("***Error (line " + iRef.posn.lineNumber + "): index to array " + iRef.decl.name + " must be an integer expression");
            //an invalid index doesn't really invalidate the whole thing
        }
        return (ResultType) t; //returns an array type
    }

    public ResultType visitIdRef(IdRef ref, ArgType arg) {
        Type t = (Type) ref.id.visit(this,null);
        if(t.typeKind == TypeKind.ERROR){
            System.err.println("***Error (line " + ref.posn.lineNumber + "): undeclared identifier " + ref.id.spelling);
            return (ResultType) new BaseType(TypeKind.ERROR,null);
        }
        else{
            ref.decl = ref.id.decl;
            return (ResultType) t;
        }

    }

    public ResultType visitThisRef(ThisRef ref, ArgType arg) {
        //check for current instance? should link ref to current class declaration
        //return a new classtype with the appropriate name
        return (ResultType) new ClassType(new Identifier(new Token(TokenKind.CLASS,ref.decl.name),null),null);
    }

    // Terminals
    public ResultType visitIdentifier(Identifier id, ArgType arg) {
        return (ResultType) id.decl.type;
    }

    public ResultType visitOperator(Operator op, ArgType arg) {
        //this should return the type accepted by that operator?
        //operator is a token, so we can switch
        //TODO: do we have to check if this operator will accept the supplied arguments? No, this has already been done
        //type assignment should occur here and not in the constructor for the AST, should all be self contained
        if(op.spelling.equals("+") || op.spelling.equals("*") || op.spelling.equals("/") || op.spelling.equals("-")){
            return (ResultType) new BaseType(TypeKind.INT,null);
        }
        else if(op.spelling.equals("<") || op.spelling.equals(">") || op.spelling.equals("<=") || op.spelling.equals(">=")
        || op.spelling.equals("&&") || op.spelling.equals("||") || op.spelling.equals("==") || op.spelling.equals("!=")){
            return (ResultType) new BaseType((TypeKind.BOOLEAN),null);
        }
        else{
            System.err.println("Error visiting operator " + op.spelling);
            return (ResultType) new BaseType(TypeKind.ERROR,null);
        }
    }

    public ResultType visitIntLiteral(IntLiteral num, ArgType arg) {
        return (ResultType) new BaseType(TypeKind.INT,null);
    }

    public ResultType visitBooleanLiteral(BooleanLiteral bool, ArgType arg) {
        return (ResultType) new BaseType(TypeKind.BOOLEAN,null);
    }

}
