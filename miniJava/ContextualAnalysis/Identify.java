package miniJava.ContextualAnalysis;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by cloftin on 3/15/15.
 * Identifier decorates the AST with declarations for each id instance
 * a seperate class, TypeChecker, will perform a second pass to check that all infered types match
 *
 * does it HAVE to implement the visitor interface?
 */
public class Identify<ArgType,ResultType> implements Visitor<ArgType,ResultType>{
    //TODO: this still doesn't work. Talk to Prins after class?
    //underestimate the effect taking classes you hate would ahve on you

    private IdentificationTable currentTable; //top level identification currentTable
    private IdentificationTable topTable; //top level identification currentTable
    private HashMap<String,IdentificationTable> tables;
    private String thisClass; //keeps track of current class for resolving 'this'
    private boolean hasMain; //boolean to confirm that we have a

    public void identify(Package prog){
        //idTable = new IdentificationTable();
        topTable = new IdentificationTable("top"); //declare a standard environment, add stuff we need
        tables = new HashMap<String, IdentificationTable>();
        this.hasMain = false; //false until we find a main method
        //assume standard environment is good to go and fix call statements. should be able to fix that in an hour

        IdentificationTable str = new IdentificationTable("String");

        IdentificationTable _PrintStream = new IdentificationTable("_PrintStream");
        ParameterDeclList pml = new ParameterDeclList();
        pml.add(new ParameterDecl(new BaseType(TypeKind.INT,null),"n",null));
        FieldDecl fd = new FieldDecl(false, false, new BaseType(TypeKind.VOID,null),"println",null);
        MethodDecl print_decl = new MethodDecl(fd, pml, new StatementList(), null, null);
        _PrintStream.enterMethod("println",print_decl);
        tables.put("_PrintStream", _PrintStream);

        IdentificationTable syst = new IdentificationTable("System");
        FieldDecl fdl = new FieldDecl(false, true, new ClassType(new Identifier(new Token(TokenKind.CLASS, "_PrintStream"),null),null) ,"out",null);
        syst.enter("out", fdl);
        tables.put("System", syst);

        prog.visit(this, null);
    }

    public ResultType visitBaseType(BaseType type, ArgType arg) {
        return null;
    }

    public ResultType visitClassType(ClassType type, ArgType arg) {
        //TODO: double check class type visiting
        return null;
    }

    public ResultType visitArrayType(ArrayType type, ArgType arg) {
        return null; //just return the array type?
    }

    public ResultType visitPackage(Package prog, ArgType arg){
        Iterator<ClassDecl> it = prog.classDeclList.iterator();
        while(it.hasNext()){
            ClassDecl decl = it.next();
            decl.visit(this,null);
        }
        if(!this.hasMain){
            System.err.println("***Error: no valid main method found");
        }
        return null;
    }

    // Declarations
    //TODO: add code to create new entries for these declarations
    public ResultType visitClassDecl(ClassDecl cd, ArgType arg){
        topTable.enter(cd.toString(), cd);
        this.thisClass = cd.name;
        IdentificationTable classTable = new IdentificationTable(cd.name);
        tables.put(cd.name,classTable);
        this.currentTable = classTable;

        Iterator<FieldDecl> fi = cd.fieldDeclList.iterator();
        Iterator<MethodDecl> mi = cd.methodDeclList.iterator();

        while(fi.hasNext()){
            FieldDecl fd = fi.next();
            fd.visit(this,null);
        }

        while(mi.hasNext()){
            MethodDecl md = mi.next();
            md.visit(this,null);
        }

        //do main method check during identification?
        MethodDecl mainMeth = currentTable.retrieveMethod("main");
        if(mainMeth != null){
            if(mainMeth.isPrivate == false && mainMeth.isStatic == true && mainMeth.type.typeKind.equals(TypeKind.VOID)){
                this.hasMain = true;
            }
        }

        return null;
    }

    public ResultType visitFieldDecl(FieldDecl fd, ArgType arg) {
        if(currentTable.retrieve(fd.name) == null){
            currentTable.enter(fd.name, fd);
        }
        else{
            System.err.println("***Error (line " + fd.posn.lineNumber + "): Field " + fd.name + " has already been declared in this class");
        }
        //we will link the identifier to this entry using the currentTable
        return null;
    }

    public ResultType visitMethodDecl(MethodDecl md, ArgType arg) {
        if(currentTable.retrieveMethod(md.name) == null){
            currentTable.enterMethod(md.name, md);
        }
        else{
            System.err.println("***Error (line " + md.posn.lineNumber + "): a method with name " + md.name + " has already been declared in this class");
            return null;
        }

        Iterator<ParameterDecl> pi = md.parameterDeclList.iterator();
        Iterator<Statement> si = md.statementList.iterator();

        currentTable.openScope();

        while(pi.hasNext()){
            ParameterDecl pd = pi.next();
            pd.visit(this,null);
        }

        while(si.hasNext()){
            Statement st = si.next();
            st.visit(this,null);
        }
        currentTable.closeScope();

        return null;
    }

    public ResultType visitParameterDecl(ParameterDecl pd, ArgType arg) {
        if(currentTable.retrieve(pd.name) == null){
            currentTable.enter(pd.name, pd);
        }
        else{
            System.err.println("***Error (line " + pd.posn.lineNumber + "): parameter with name " + pd.name + " has already been declared in this method");
        }
        return null;
    }

    public ResultType visitVarDecl(VarDecl decl, ArgType arg) {
        Declaration check = currentTable.retrieve(decl.name);
        if (check == null) {
            //check that it hasn't already been declared
            currentTable.enter(decl.name, decl);
        }
        else{
            System.err.println("***Error (line " + check.posn.lineNumber + "): parameter with name " + check.name + " has already been declared in this method");
        }
        return null;
    }

    // Statements
    public ResultType visitBlockStmt(BlockStmt stmt, ArgType arg) {
        currentTable.openScope();
        //block statement has a statement list
        Iterator<Statement> it = stmt.sl.iterator();

        while(it.hasNext()){
            Statement st = it.next(); //get the net statement in this statement block
            st.visit(this,null);
        }

        currentTable.closeScope();
        return null;
    }

    public ResultType visitVardeclStmt(VarDeclStmt stmt, ArgType arg) {
        //TODO: make sure we are adding the declarations for identifiers
        if(currentTable.retrieve(stmt.varDecl.name) == null){
            currentTable.enter(stmt.varDecl.name,stmt.varDecl);
            //varDecl already has a type associated with it
            stmt.initExp.visit(this,null); //all expressions should have a type
        }
        else{
            System.err.println("***Error (line " + stmt.posn.lineNumber + "): variable " + stmt.varDecl.name + " has already been declared");
        }
        return null;
    }

    public ResultType visitAssignStmt(AssignStmt stmt, ArgType arg) {
        stmt.val.visit(this,null);
        stmt.ref.visit(this,null); //visits will assign type to ref and val
        return null;
    }

    public ResultType visitCallStmt(CallStmt stmt, ArgType arg) {
        stmt.methodRef.visit(this,null);
        Declaration decl = currentTable.retrieveMethod(stmt.methodRef.decl.name);
        if(decl != null){
            stmt.methodRef.decl = decl;
        }
        else{
            System.err.println("***Error: Method unknown");
        }
        return null;
    }

    public ResultType visitIfStmt(IfStmt stmt, ArgType arg) {
        currentTable.openScope();
        stmt.thenStmt.visit(this,null);
        currentTable.closeScope();

        currentTable.openScope();
        if(stmt.elseStmt != null){
            stmt.elseStmt.visit(this,null);
        }
        currentTable.closeScope();

        return null;
    }

    public ResultType visitWhileStmt(WhileStmt stmt, ArgType arg) {
        currentTable.openScope();
        stmt.cond.visit(this,null);
        stmt.body.visit(this,null);
        currentTable.closeScope();
        return null;
    }

    // Expressions
    public ResultType visitUnaryExpr(UnaryExpr expr, ArgType arg) {
        expr.expr.visit(this,null);
        expr.operator.visit(this,null);
        return null;
    }

    public ResultType visitBinaryExpr(BinaryExpr expr, ArgType arg) {
        expr.left.visit(this,null);
        expr.right.visit(this,null);
        expr.operator.visit(this,null);
        return null;
    }

    public ResultType visitRefExpr(RefExpr expr, ArgType arg) {
        expr.ref.visit(this,null);
        return null;
    }

    public ResultType visitCallExpr(CallExpr expr, ArgType arg) {
        expr.functionRef.visit(this,null);
        return null;
    }

    public ResultType visitLiteralExpr(LiteralExpr expr, ArgType arg) {
        expr.lit.visit(this,null);
        return null;
    }

    public ResultType visitNewObjectExpr(NewObjectExpr expr, ArgType arg)
    {
        return null;
    }

    public ResultType visitNewArrayExpr(NewArrayExpr expr, ArgType arg) {
        return null;
    }

    // References
    public ResultType visitQualifiedRef(QualifiedRef ref, ArgType arg) {
        QualifiedRef originalRef = ref;
        IdentificationTable originalTable = currentTable;
        Declaration decl;
        if(ref.ref instanceof ThisRef) {
            //just use the table of the current class
            decl = currentTable.retrieve(ref.id.spelling);
            if (decl != null) {
                ref.id.decl = decl;
                ref.decl = decl;
            } else {
                System.err.println("***Error (line " + ref.posn.lineNumber + "): can't find declaration in qualified reference");
            }
        }
        else if(ref.ref instanceof QualifiedRef){
            // (QualRef <- Ref),Id
            while(ref.ref instanceof QualifiedRef){ //keep going until we hit outer most scope
                ((QualifiedRef) ref.ref).id.visit(this,null); //visit first, so we have a type to work with
                currentTable = tables.get(((ClassType)((QualifiedRef) ref.ref).id.decl.type).className.spelling); //get table of class to the left
                String getThis = ((QualifiedRef) ref).id.spelling;
                Declaration enclosing = currentTable.retrieve(getThis);
                if(enclosing == null){
                    System.err.println("***Error (line " + ref.posn.lineNumber + "): class " + ((QualifiedRef) ref.ref).id.spelling + " has no field of type " + ref.id.spelling);
                    currentTable = originalTable;
                    return null; //identification failed, return an error
                }
                ref = (QualifiedRef)ref.ref;
            }
            //returing the wrong type
            currentTable = originalTable;
            //reached the outermost ref with no issues
            //depends on if its a classtype or a thistype
            IdentificationTable scopeTable = null;
            if( ref.ref instanceof IdRef){
                ((IdRef) ref.ref).id.visit(this,null);
                scopeTable = tables.get(((IdRef) ref.ref).id.spelling);
            }
            else if(ref.ref instanceof ThisRef){
                ((ThisRef)ref.ref).visit(this,null);
                scopeTable = tables.get(thisClass);
            }


            decl = scopeTable.retrieve(originalRef.id.spelling);
            if(decl != null){
                originalRef.id.decl = decl;
                originalRef.decl = decl;
            }
            else {
                System.err.println("***Error (line " + ref.posn.lineNumber + "): Identification in qualified reference");
            }

        }
        return null;
    }

    public ResultType visitIndexedRef(IndexedRef iRef, ArgType arg) {
        iRef.ref.visit(this,null);
        iRef.indexExpr.visit(this,null);
        Declaration decl = currentTable.retrieve(iRef.ref.decl.name);
        if(decl.type.typeKind == TypeKind.ARRAY){
            //the declaraiton retrieved should be for an array
            iRef.decl = decl; //add decl to reference as well
        }
        else{
            System.err.println("***Error (line " + iRef.posn.lineNumber + "): no array with name " + decl.name + " found within this scope");
        }
        return null;
    }

    public ResultType visitIdRef(IdRef ref, ArgType arg) {
        //need to make sure that we visit all the mehods first
        ref.id.visit(this,null);
        ref.decl = ref.id.decl;
        return null;
    }

    public ResultType visitThisRef(ThisRef ref, ArgType arg) {
        ref.decl = topTable.retrieve(this.thisClass);
        return null;
    }

    // Terminals
    public ResultType visitIdentifier(Identifier id, ArgType arg) {
        Declaration decl = currentTable.retrieve(id.spelling);
        if(decl != null){
            id.decl = decl;
            return null;
        }
        else{
            System.err.println("***Error (line " + id.posn.lineNumber + "): undeclared identifier " + id.spelling);
        }
        return null;
    }

    public ResultType visitOperator(Operator op, ArgType arg) {
        return null;
    }

    public ResultType visitIntLiteral(IntLiteral num, ArgType arg) {
        return null;
    }

    public ResultType visitBooleanLiteral(BooleanLiteral bool, ArgType arg) {
        return null;
    }
}
