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
//TODO: standard environment
//TODO: create an ID table entry class that includes metadata (rather than cechking info about the declaration)

public class TypeCheck<ArgType,ResultType> implements Visitor<ArgType,ResultType>{

    private IdentificationTable currentTable; //top level identification currentTable
    private IdentificationTable topTable; //top level identification currentTable
    private HashMap<String,IdentificationTable> tables;
    private String thisClass; //keeps track of current class for resolving 'this'
    private boolean hasMain; //boolean to confirm that we have a


    public void check(Package prog){
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

    public ResultType visitPackage(Package prog, ArgType arg){
        //iterate through a class declaration list
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
    public ResultType visitClassDecl(ClassDecl cd, ArgType arg){
        topTable.enter(cd.toString(), cd);
        this.thisClass = cd.name;
        IdentificationTable classTable = new IdentificationTable(cd.name);
        tables.put(cd.name,classTable);
        this.currentTable = classTable;

        Iterator<FieldDecl> fi = cd.fieldDeclList.iterator();
        Iterator<MethodDecl> mi = cd.methodDeclList.iterator();

        //visit fields first, as they may show up in the methods
        while(fi.hasNext()){
            FieldDecl fd = fi.next();
            fd.visit(this,null);
        }

        //visit methods
        while(mi.hasNext()){
            MethodDecl md = mi.next();
            currentTable.enter(md.name,md);
        }

        Iterator<MethodDecl> new_mi = cd.methodDeclList.iterator();

        while(new_mi.hasNext()){
            MethodDecl md = new_mi.next();
            md.visit(this,null);
        }

        //check for a single main method here
        MethodDecl mainMeth = currentTable.retrieveMethod("main");
        if(mainMeth != null){
            if(mainMeth.isPrivate == false && mainMeth.isStatic == true && mainMeth.type.typeKind.equals(TypeKind.VOID)){
                this.hasMain = true;
            }
        }

        return null;
        //TODO: source position
    }

    //TODO: use name and not toString()

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

        //TODO: replace all method calls with currentTable.enterMethod
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
            //visitParameterDecl(pd, null);
            pd.visit(this,null);
        }

        while(si.hasNext()){
            Statement st = si.next(); //get the net statement in this statement block
            st.visit(this,null);
        }

        if(md.returnExp == null){
            //no return type, check that it is a void method
            if(md.type.typeKind == TypeKind.VOID){
                currentTable.closeScope();
                return null;
            }
            else{
                System.err.println("***Error (line " + md.posn.lineNumber + "):: no return type for non-void method " + md.name);
                currentTable.closeScope();
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
        //a statement of statements, open scope
        currentTable.openScope();
        //block statement has a statement list
        Iterator<Statement> it = stmt.sl.iterator();

        while(it.hasNext()){
            Statement st = it.next(); //get the net statement in this statement block
            st.visit(this,null);
        }

        currentTable.closeScope();
        return null;
        //TODO: implement sourcePosition
    }

    public ResultType visitVardeclStmt(VarDeclStmt stmt, ArgType arg) {

        //TODO: still have to handle variables being used before they are declared
        //enter the variable into our currentTable, confirm that declared value matches declaration type
        if(currentTable.retrieve(stmt.varDecl.name) == null){
            currentTable.enter(stmt.varDecl.name,stmt.varDecl);
        }
        else{
            System.err.println("***Error (line " + stmt.posn.lineNumber + "): variable " + stmt.varDecl.name + " has already been declared");
            return null;
        }

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
        Declaration decl = currentTable.retrieveMethod(stmt.methodRef.decl.name);
        if(decl != null){
            stmt.methodRef.decl = decl;
        }
        else{
            System.err.println("***Error: Method unknown");
        }
        return null; //its a statement, don't bother returning a type
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

    //this thing is barely functional, what were you thinking...
    //still need an hour to install it on the department servers...

    public ResultType visitWhileStmt(WhileStmt stmt, ArgType arg) {
        currentTable.openScope();
        //TODO: add expression ids to the scope
        if(((Type)stmt.cond.visit(this,null)) == null || ((Type)stmt.cond.visit(this,null)).typeKind != TypeKind.BOOLEAN) {
            System.out.println("***Error (line " + stmt.posn.lineNumber + "): while statement conditions must be booleans");
            return null;
        }

        stmt.body.visit(this,null);
        currentTable.closeScope();
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


    //TODO: new object expressions
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
        QualifiedRef originalRef = ref;
        IdentificationTable originalTable = currentTable;
        Declaration decl;
        Type result = new BaseType(TypeKind.ERROR,null);

        if(ref.ref instanceof ThisRef){
            //just use the table of the current class
            decl = currentTable.retrieve(ref.id.spelling);
            if(decl != null){
                ref.id.decl = decl;
                ref.decl = decl;
                result = decl.type;
            }
            else{
                System.err.println("***Error (line " + ref.posn.lineNumber + "): can't find declaration in qualified reference");
                return (ResultType) new BaseType(TypeKind.ERROR,null);
            }
        }
        else if(ref.ref instanceof IdRef){
            //also simple, just look up the table
            decl = tables.get(((IdRef)ref.ref).id.spelling).retrieve(ref.id.spelling);
            if(decl != null){
                ref.id.decl = decl;
                ref.decl = decl;
                result = decl.type;
            }
            else{
                System.err.println("***Error (line " + ref.posn.lineNumber + "): can't find declaration in qualified reference");
                return (ResultType) new BaseType(TypeKind.ERROR,null);
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
                    return (ResultType) new BaseType(TypeKind.ERROR,null); //identification failed, return an error
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
                result = decl.type;
            }
            else {
                System.err.println("***Error (line " + ref.posn.lineNumber + "): Identification in qualified reference");
                return (ResultType) new BaseType(TypeKind.ERROR,null);
            }

        }

        //if everything checks out, the declaration for the id should be stored in the table of the class right next to it




            //could have a NPE if decl never gets instantiated
        return (ResultType) result;
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
        //return the type of the indexed reference and associate identifier if it is declared, otherwise throw an error and return null
        //check currentTable for a declaration with this name, also check that its declaration is that of an array
        //need to visit its reference first
        Type t = (Type) iRef.ref.visit(this,null); //this will get the type of the reference and identify it
        Type indexType = (Type) iRef.indexExpr.visit(this,null);
        Declaration decl = currentTable.retrieve(iRef.ref.decl.name);
        if(decl.type.typeKind == TypeKind.ARRAY){
            //the declaraiton retrieved should be for an array
            iRef.decl = decl; //add decl to reference as well
        }
        else{
            System.err.println("***Error (line " + iRef.posn.lineNumber + "): no array with name " + decl.name + " found within this scope");
            return (ResultType) new BaseType(TypeKind.ERROR,null);
        }

        //TODO: Static access and references are handled by visitQualifiedRef
        //Standard environment and protected shit will be handled later
        //also... debugging

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
        ref.decl = topTable.retrieve(this.thisClass);

        //return a new classtype with the appropriate name
        return (ResultType) new ClassType(new Identifier(new Token(TokenKind.CLASS,this.thisClass),null),null);
    }

    // Terminals
    public ResultType visitIdentifier(Identifier id, ArgType arg) {
        Declaration decl = currentTable.retrieve(id.spelling);
        if(decl != null){
            id.decl = decl;
            return (ResultType)decl.type;
        }
        else{
            //System.err.println("***Error (line " + id.posn.lineNumber + "): undeclared identifier " + id.spelling);
            return (ResultType) new BaseType(TypeKind.ERROR,null);
        }
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
