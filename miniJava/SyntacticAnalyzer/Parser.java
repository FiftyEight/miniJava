package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.util.List;
import java.util.Arrays;
import miniJava.AbstractSyntaxTrees.*;

import javax.swing.plaf.nimbus.State;

public class Parser{



    private InputStream inputStream;
    private Token currentToken, nextToken, next_next_token;
    //private Token prevToken; //this might work
    private Scanner scanner;
    private boolean trace = false;
    private boolean valid = true;

    //this approach is incorrect, the starter lists are not disjunct

    //starter lists for all nonterminals (will make parse methods a bit more readable, lets us compare)
    private static List<TokenKind> reference_starters = Arrays.asList(TokenKind.ID, TokenKind.THIS);
    private static List<TokenKind> lxReference_starters = reference_starters;
    private static List<TokenKind> type_starters = Arrays.asList(TokenKind.INT, TokenKind.BOOLEAN,TokenKind.ID);
    private static List<TokenKind> primitive_starters = type_starters;
    private static List<TokenKind> array_starters = Arrays.asList(TokenKind.INT, TokenKind.ID);
    private static List<TokenKind> param_starters = type_starters;
    private static List<TokenKind> expression_starters = Arrays.asList(TokenKind.ID, TokenKind.THIS, TokenKind.UNOP, TokenKind.NEG,TokenKind.LPAREN,TokenKind.NUM,TokenKind.TRUE,TokenKind.FALSE,TokenKind.NEW, TokenKind.EQUALS);
    private static List<TokenKind> statement_starters = Arrays.asList(TokenKind.ID, TokenKind.THIS, TokenKind.INT, TokenKind.BOOLEAN, TokenKind.IF, TokenKind.WHILE,TokenKind.LCURL);
    private static List<TokenKind> arg_starters = expression_starters;
    private static List<TokenKind> decl_starters = Arrays.asList(TokenKind.PUBLIC, TokenKind.PRIVATE, TokenKind.VOID, TokenKind.STATIC, TokenKind.INT, TokenKind.BOOLEAN, TokenKind.ID);
    private static List<TokenKind> method_starters = decl_starters;
    private static List<TokenKind> field_starters = decl_starters;
    private static List<TokenKind> class_starters = Arrays.asList(TokenKind.CLASS);
    private static List<TokenKind> expression_literals = Arrays.asList(TokenKind.LPAREN,TokenKind.UNOP, TokenKind.NEG, TokenKind.NUM, TokenKind.TRUE, TokenKind.FALSE, TokenKind.NEW);
    private static List<String> type_kinds = Arrays.asList(
            "void",
            "int",
            "boolean",
            "class",
            "array"
    );

    //TODO: all source positions have been set to null, is this okay?

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    public AST parse(){
        //main parse method, all mutually recursive parsing methods come from this
        currentToken = scanner.scan();
        nextToken = scanner.scan();
        next_next_token = scanner.scan();
        AST ast = parseProgram();
        if(this.valid){
            return ast;
        }
        return null;
    }

    private void acceptIt(){
        //accepts the current token
        accept(currentToken.kind);
        return;
    }

    private void accept(TokenKind expectedTokenKind) {
        //accepts a given terminal
        if(currentToken.kind == expectedTokenKind){
            System.out.println("ACCEPTED: " + expectedTokenKind);
            currentToken = nextToken;
            nextToken = next_next_token;
            next_next_token = scanner.scan();
        }
        else{
            System.err.println("SYNTAX ERROR at line " + scanner.inputStream.getLineNumber() + ": found " + currentToken.kind + ", expected " + expectedTokenKind);
            valid = false; //this source is invalid
            System.exit(4); //TODO: remove this, its temporary
        }

        return;

    }

    //PARSING METHODS

    //TODO: make sure no fallthrough in switches
    //TODO: make sure switches default to ERROR

    private Statement parseStatement(){
        //STATEMENT ::=

        int lineNumber = scanner.inputStream.getLineNumber() + 1;
        SourcePosition sp = new SourcePosition(lineNumber);

        //NEED TO TEST THIS WITH THE OLD INTERPRETER, COULD BE THE PROBLEM

        if(currentToken.kind == TokenKind.THIS){
            //assign statement or method call
            Reference ref = parseReference(sp);

            switch(currentToken.kind){
                case LPAREN:
                    //CallStmt
                    acceptIt();
                    ExprList args = null;
                    if(arg_starters.contains(currentToken.kind)){
                        args = parseArgList(sp);
                    }
                    accept(TokenKind.RPAREN);
                    accept(TokenKind.SEMIC);
                    return new CallStmt(ref, args, sp);

                case EQUALS:
                    //AssignStmt
                    acceptIt();
                    Expression expr = parseExpression(sp);
                    accept(TokenKind.SEMIC);
                    return new AssignStmt(ref, expr, sp);
                default: return null;

            }


        }

        else if(currentToken.kind == TokenKind.INT || currentToken.kind == TokenKind.BOOLEAN){
            //has to be type
            //VARIABLE DECLARATION
            Type t = parseType(sp);
            String name = currentToken.spelling;
            VarDecl decl = new VarDecl(t,name,sp);
            accept(TokenKind.ID);
            accept(TokenKind.EQUALS);
            Expression exp = parseExpression(sp);
            accept(TokenKind.SEMIC);

            return new VarDeclStmt(decl,exp, sp);
        }



        else if(currentToken.kind == TokenKind.ID){

            //this is the tricky one, as it could be a VarDecl, CallStmt or AssignStmt
            //Identifier id = new Identifier(currentToken);
            //acceptIt();
            switch(nextToken.kind){
                case ID:
                    //TYPE ID = EXPRESSION
                    //VarDecl
                    Type t = parseType(sp);
                    String name = currentToken.spelling;
                    VarDecl decl = new VarDecl(t,name,sp);

                    acceptIt();
                    accept(TokenKind.EQUALS);
                    Expression expr = parseExpression(sp);
                    accept(TokenKind.SEMIC);
                    return new VarDeclStmt(decl, expr, sp);

                case DOT:

                    //REFERENCE (ARGUMENT LIST?);
                    //LxREFERENCE = EXPRESSION;
                    //could be a CallStmt or an AssignStmt
                    Identifier id = new Identifier(currentToken,sp);
                    Reference ref = parseReference(sp);

                    //WHAT IF WE HAVE empty brackets?
                    switch(currentToken.kind) {
                        case LPAREN:
                            //callStmt
                            acceptIt();
                            ExprList exprList;
                            if (arg_starters.contains(currentToken.kind)) {
                                exprList = parseArgList(sp);
                            }
                            else{
                                exprList = new ExprList();
                            }
                            accept(TokenKind.RPAREN);
                            accept(TokenKind.SEMIC);
                            //call statements should have an exprList even if empty
                            CallStmt stmt = new CallStmt(ref, exprList, sp);
                            System.out.println(stmt.argList);
                            return stmt;

                        case EQUALS:
                            //AssignStmt
                            acceptIt();
                            Expression exp = parseExpression(sp);
                            accept(TokenKind.SEMIC);
                            return new AssignStmt(ref, exp, sp);
                        case ID:
                            String new_id = currentToken.spelling;
                            acceptIt();
                            accept(TokenKind.EQUALS);
                            Expression array_expr = parseExpression(sp);
                            accept(TokenKind.SEMIC);
                            return new VarDeclStmt(new VarDecl(new ArrayType(new ClassType(id,null),sp),new_id,sp), array_expr,sp);
                        default:
                            return null;
                    }

                case LPAREN:
                    //Call Stmt
                    Reference refe = parseReference(sp);
                    accept(TokenKind.LPAREN);
                    ExprList argList;
                    if (arg_starters.contains(currentToken.kind)) {
                        argList = parseArgList(sp);
                    }
                    else{
                        argList = new ExprList();
                    }
                    accept(TokenKind.RPAREN);
                    accept(TokenKind.SEMIC);
                    return new CallStmt(refe, argList, sp);


                case EQUALS:
                    //assignStmt
                    Reference other_ref = parseReference(sp);
                    accept(TokenKind.EQUALS);
                    Expression other_expr = parseExpression(sp);
                    accept(TokenKind.SEMIC);
                    return new AssignStmt(other_ref, other_expr, sp);

                case LBRACK:
                    //could be an array type, a method call or an assignment
                    Identifier brack_id = new Identifier(currentToken,sp);
                    String brack_string = currentToken.spelling;

                    if(next_next_token.kind == TokenKind.RBRACK){
                        //declaring an array
                        Type arr_type = parseType(sp);
                        String arr_id = currentToken.spelling;
                        accept(TokenKind.ID);
                        accept(TokenKind.EQUALS);
                        Expression arr_expr = parseExpression(sp);
                        accept(TokenKind.SEMIC);
                        return new VarDeclStmt(new VarDecl(arr_type,arr_id,sp),arr_expr,sp);
                    }
                    else{
                        Reference arr_ref = parseReference(sp);
                        switch(currentToken.kind){
                            case LPAREN:
                                //callStmt
                                acceptIt();
                                ExprList exprList;
                                if (arg_starters.contains(currentToken.kind)) {
                                    exprList = parseArgList(sp);
                                }
                                else{
                                    exprList = new ExprList();
                                }
                                accept(TokenKind.RPAREN);
                                accept(TokenKind.SEMIC);
                                //call statements should have an exprList even if empty
                                CallStmt stmt = new CallStmt(arr_ref, exprList, sp);
                                System.out.println(stmt.argList);
                                return stmt;

                            case EQUALS:
                                //AssignStmt
                                acceptIt();
                                Expression exp = parseExpression(sp);
                                accept(TokenKind.SEMIC);
                                return new AssignStmt(arr_ref, exp, sp);
                        }

                    }
                default:
                    return null;
            }
        }

        else {

            //terminal starters, should be easy

            switch (currentToken.kind) {
                case LCURL:
                    //block statement
                    acceptIt();
                    StatementList statList = new StatementList();
                    while (currentToken.kind != TokenKind.RCURL) {
                        Statement stat = parseStatement();
                        statList.add(stat);
                    }
                    accept(TokenKind.RCURL);
                    return new BlockStmt(statList, sp);

                case IF:
                    //IF statement
                    acceptIt();
                    accept(TokenKind.LPAREN);
                    Expression condition = parseExpression(sp);
                    accept(TokenKind.RPAREN);
                    Statement statement_a = parseStatement();
                    if(currentToken.kind == TokenKind.ELSE){
                        acceptIt();
                        Statement statement_b = parseStatement();
                        return new IfStmt(condition,statement_a,statement_b,sp);
                    }
                    return new IfStmt(condition, statement_a, sp);

                case WHILE:
                    //WHILE STATEMENT
                    acceptIt();
                    accept(TokenKind.LPAREN);
                    Expression cond = parseExpression(sp);
                    accept(TokenKind.RPAREN);
                    Statement stat = parseStatement();
                    return  new WhileStmt(cond, stat, sp);

            }
        }
        return null;
    }

    private Expression parseExpression(SourcePosition sp) {

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);

        Expression left_side = parseAND(sp);

        while(currentToken.spelling.equals("||")){
            Operator orOP = new Operator(currentToken);
            acceptIt();
            Expression right_side = parseAND(sp);
            left_side = new BinaryExpr(orOP, left_side,right_side,sp);
        }

        return left_side;
    }


    private Expression parseExpressionTerminals(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);

        Expression final_expression = null;


        if(reference_starters.contains(currentToken.kind)){
            //REFERENCE
            Reference ref = parseReference(sp);
            if(currentToken.kind == TokenKind.LPAREN){
                //CALL EXPRESSION
                acceptIt();
                ExprList argList = null;
                if(arg_starters.contains(currentToken.kind)){
                    argList = parseArgList(sp);
                }
                else{
                    argList = new ExprList();
                }

                accept(TokenKind.RPAREN);
                return new CallExpr(ref, argList, sp);
            }
            return new RefExpr(ref,sp);
        }

        else if (expression_literals.contains(currentToken.kind)){
            switch(currentToken.kind){
                case LPAREN: //expression with isolated precedence
                    acceptIt();
                    parseExpression(sp);
                    accept(TokenKind.LPAREN);
                    break;
                case UNOP:case NEG: //
                    Operator new_unop = new Operator(currentToken);
                    acceptIt();
                    Expression unex = parseExpression(sp);
                    final_expression = new UnaryExpr(new_unop, unex,sp);
                    break;

                case NUM:
                    IntLiteral this_int = new IntLiteral(currentToken);
                    acceptIt();
                    final_expression = new LiteralExpr(this_int, sp);
                    break;
                case TRUE: case FALSE: //
                    BooleanLiteral bool_lit = new BooleanLiteral(currentToken);
                    acceptIt();
                    final_expression = new LiteralExpr(bool_lit, sp);
                    break;

                case NEW:
                    //NewExpr
                    acceptIt();
                    switch(currentToken.kind){
                        case INT:
                            acceptIt();
                            accept(TokenKind.LBRACK);
                            Expression exp = parseExpression(sp);
                            accept(TokenKind.RBRACK);
                            final_expression = new NewArrayExpr(new BaseType(TypeKind.INT,sp),exp,sp);
                            break;
                        case ID:
                            Identifier id = new Identifier(currentToken,sp);
                            acceptIt();
                            switch(currentToken.kind){
                                case LPAREN:
                                    acceptIt();
                                    accept(TokenKind.RPAREN);
                                    final_expression = new NewObjectExpr(new ClassType(id,sp),sp);
                                    break;
                                case LBRACK:
                                    acceptIt();
                                    Expression brack_exp = parseExpression(sp);
                                    accept(TokenKind.RBRACK);
                                    final_expression = new NewArrayExpr(new ClassType(id,sp),brack_exp,sp);
                                    break;
                            }
                            break;
                    }
                    break;
            }
        }

        //PARSE OR

        /*if(currentToken.kind == TokenKind.BINOP){

            //TRUE && FALSE || x

            //currentToken = prevToken; //this won't work if its was a reference

            System.out.println("parsing binary expression");

            //first expression ahs already been parsed at this point

            //OLD VERSION
            Expression other_side = parseAND();

            while(currentToken.spelling == "||"){
                Operator orOP = new Operator(currentToken);
                acceptIt();
                Expression also = parseAND();
                other_side = new BinaryExpr(orOP, other_side,also,null);
            }

            while(currentToken.kind == TokenKind.BINOP){
                if(currentToken.spelling == "||"){
                    Operator orOP = new Operator(currentToken);
                    acceptIt();
                    Expression other_side = parseAND();
                    final_expression = new BinaryExpr(orOP, final_expression,other_side,null);
                }
                else{
                    Expression other_side = parseAND();
                    final_expression = new BinaryExpr(orOP, final_expression,other_side,null);
                }
            }

        }*/


        /*if(currentToken.kind == TokenKind.BINOP || currentToken.kind == TokenKind.NEG){
            //EXPRESSION BINOP EXPRESSION

            Operator op = new Operator(currentToken);
            Expression other_side = parseExpression();
            final_expression = new BinaryExpr(op, final_expression, other_side,null);

            //DISALLOW --
            if (currentToken.kind == TokenKind.BINOP) return null; //this will throw an error if we have double BINOPS
            else{
                Expression other_side = parseExpression();
                final_expression = new BinaryExpr(op, final_expression, other_side,null);
            }
        }*/

        if(final_expression == null){
            //cannot have a null expression, accept an error token
            accept(TokenKind.BREAK);
        }
        return final_expression;
    }

    private Expression parseAND(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);
        //parses an expression
        Expression final_expression = parseEQ(sp);

        while(currentToken.kind == TokenKind.BINOP && currentToken.spelling.equals("&&")){
            Operator andOP = new Operator(currentToken);
            acceptIt();
            Expression other_side = parseEQ(sp);
            final_expression = new BinaryExpr(andOP, final_expression,other_side,sp);

        }
        return final_expression;
    }

    private Expression parseEQ(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);
        //parses an expression
        Expression final_expression = parseREL(sp);

        while(currentToken.kind == TokenKind.BINOP && (currentToken.spelling.equals("==") || currentToken.spelling.equals("!="))){
            Operator eqOP = new Operator(currentToken);
            acceptIt();
            Expression other_side = parseREL(sp);
            final_expression = new BinaryExpr(eqOP, final_expression,other_side,sp);

        }
        return final_expression;
    }

    private Expression parseREL(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);
        //parses an expression
        Expression final_expression = parseADD(sp);

        while(currentToken.kind == TokenKind.BINOP &&
                (currentToken.spelling.equals(">") || currentToken.spelling.equals("<") || currentToken.spelling.equals("<=") || currentToken.spelling.equals(">="))){
            Operator relOP = new Operator(currentToken);
            acceptIt();
            Expression other_side = parseADD(sp);
            final_expression = new BinaryExpr(relOP, final_expression,other_side,sp);

        }
        return final_expression;
    }

    private Expression parseADD(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);
        //parses an expression
        Expression final_expression = parseMUL(sp);

        while((currentToken.kind == TokenKind.BINOP && currentToken.spelling.equals("+")) || currentToken.kind == TokenKind.NEG){
            Operator addOP = new Operator(currentToken);
            acceptIt();
            Expression other_side = parseMUL(sp);
            final_expression = new BinaryExpr(addOP, final_expression,other_side,sp);

        }
        return final_expression;
    }

    private Expression parseMUL(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);
        //parses an expression
        Expression final_expression = parseUN(sp);

        while(currentToken.kind == TokenKind.BINOP && (currentToken.spelling.equals("*") || currentToken.spelling.equals("/"))){
            Operator mulOP = new Operator(currentToken);
            acceptIt();
            Expression other_side = parseUN(sp);
            final_expression = new BinaryExpr(mulOP, final_expression,other_side,sp);

        }
        return final_expression;
    }

    private Expression parseUN(SourcePosition sp){
        //this one needs to be different
        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);

        Expression expr = null;
        if(currentToken.kind == TokenKind.UNOP && (currentToken.spelling.equals("!")) || currentToken.kind == TokenKind.NEG){
            Operator unOP = new Operator(currentToken);
            acceptIt();
            expr = parseExpressionTerminals(sp);
            return new UnaryExpr(unOP, expr, sp);
        }
        expr = parseExpressionTerminals(sp);
        return expr;
    }

    /*private Expression parseLit(){
        Expression litExpr = null;
        switch (currentToken.kind){
            case NUM:
                litExpr = new LiteralExpr(new IntLiteral(currentToken),null);
                acceptIt();
                break;
            case BOOLEAN:
                litExpr = new LiteralExpr(new BooleanLiteral(currentToken),null);
                acceptIt();
                break;
            case ID:
                litExpr = new LiteralExpr(new BooleanLiteral(currentToken),null);
                acceptIt();
                break;
            case LPAREN:
                acceptIt();
                litExpr = parseExpression();
                accept(TokenKind.RPAREN);
        }

        return litExpr;
    }*/

    private miniJava.AbstractSyntaxTrees.Package parseProgram(){

        int lineNumber = scanner.inputStream.getLineNumber() + 1;
        SourcePosition sp = new SourcePosition(lineNumber);

        ClassDeclList declList = new ClassDeclList();
        while(class_starters.contains(currentToken.kind)){
            ClassDecl decl = parseClassDecl();
            declList.add(decl);
        }

        accept(TokenKind.EOT);
        return new miniJava.AbstractSyntaxTrees.Package(declList, sp);
    }

    private ClassDecl parseClassDecl(){

        int lineNumber = scanner.inputStream.getLineNumber() + 1;
        SourcePosition sp = new SourcePosition(lineNumber);

        accept(TokenKind.CLASS);
        String class_name = currentToken.spelling;
        accept(TokenKind.ID);
        accept(TokenKind.LCURL);

        FieldDeclList fieldList = new FieldDeclList();
        MethodDeclList methodList = new MethodDeclList();

        while(field_starters.contains(currentToken.kind) || method_starters.contains(currentToken.kind)){
            boolean[] entries = parseDeclarators(sp);
            Type type = parseType(sp);
            if(type == null){
                accept(TokenKind.VOID);
                type = new BaseType(TypeKind.VOID,sp);
            }
            String name = currentToken.spelling;
            accept(TokenKind.ID);

            switch (currentToken.kind){
                case LPAREN:
                    //METHOD
                    //use FieldDecl as md argument for method
                    ParameterDeclList paramList = new ParameterDeclList();
                    StatementList statList = new StatementList();
                    Expression retExp = null;
                    acceptIt();
                    if(param_starters.contains(currentToken.kind)){
                        paramList = parseParamList(sp);
                    }
                    accept(TokenKind.RPAREN);
                    accept(TokenKind.LCURL);


                    while(statement_starters.contains(currentToken.kind)){
                        Statement stat = parseStatement();
                        statList.add(stat);
                    }

                    if(currentToken.kind == TokenKind.RETURN){
                        acceptIt();
                        if(currentToken.kind == TokenKind.SEMIC){
                            retExp = null;
                            return null;
                        }
                        retExp = parseExpression(sp);

                    }

                    accept(TokenKind.RCURL);

                    methodList.add(new MethodDecl(new FieldDecl(entries[0],entries[1],type,name,sp),paramList,statList,retExp,sp));

                    break;

                case SEMIC:
                //FIELD
                    acceptIt();
                    fieldList.add(new FieldDecl(entries[0],entries[1],type,name,sp));
                default: break; //force it to throw an error
            }

        }
        //issue here
        accept(TokenKind.RCURL);
        return new ClassDecl(class_name,fieldList,methodList,sp);
    }

    private boolean[] parseDeclarators(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);
        //parse decl now returns an array of booleans to be used in the declaration


        boolean[] entries = new boolean[2]; //pair of booleans for public, private

        //(PUBLIC | PRIVATE )?
        if(currentToken.kind == TokenKind.PUBLIC || currentToken.kind == TokenKind.PRIVATE){
            switch(currentToken.kind){
                case PRIVATE:
                    accept(TokenKind.PRIVATE);
                    entries[0] = true;
                    break;
                case PUBLIC:
                    accept(TokenKind.PUBLIC);
                    entries[0] = false;
                    break;
            }
        }

        //STATIC?
        if(currentToken.kind == TokenKind.STATIC){
            entries[1] = true;
            acceptIt();
        }
        else entries[1] = false;


        return entries;

        /*if(type_starters.contains(currentToken.kind)){
            parseType();
            return;
        }

        else if(currentToken.kind == TokenKind.VOID) acceptIt();
        return;*/

    }

    private Type parseType(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);

        switch(currentToken.kind){
            case ID:
                //if its an ID it has to be a class type (its an instance of some class)
                Token ident = currentToken;
                acceptIt();

                if(currentToken.kind == TokenKind.LBRACK){
                    //array type
                    acceptIt();
                    accept(TokenKind.RBRACK);
                    //return an array of classes
                    return new ArrayType(new ClassType(new Identifier(ident,sp),sp),sp);
                }

                else return new ClassType(new Identifier(ident,sp),sp);

            case BOOLEAN:

                acceptIt();
                return new BaseType(TypeKind.BOOLEAN,sp);

            case INT:
                acceptIt();
                //could be an array or a primitive
                if(currentToken.kind == TokenKind.LBRACK){
                    acceptIt();
                    accept(TokenKind.RBRACK);
                    return new ArrayType(new BaseType(TypeKind.INT,sp),sp);
                }

                else return new BaseType(TypeKind.INT,sp);

        }

        //defaults to a null value
        return null;
    }

    private void parsePrimType(){
        if(currentToken.kind == TokenKind.BOOLEAN || currentToken.kind == TokenKind.INT){
            acceptIt();
        }
        return;
    }

    private void parseArrType(){
        if(currentToken.kind == TokenKind.ID || currentToken.kind == TokenKind.INT){
            acceptIt();
        }

        accept(TokenKind.LBRACK);
        accept(TokenKind.RBRACK);
        return;
    }

    private ParameterDeclList parseParamList(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);

        ParameterDeclList paramList = new ParameterDeclList();
        Type t = parseType(sp);
        String init_param_name = currentToken.spelling;
        accept(TokenKind.ID);

        //add the first parameter
        paramList.add(new ParameterDecl(t, init_param_name,sp));

        while(currentToken.kind == TokenKind.COMMA){
            acceptIt();
            t = parseType(sp);
            String name = currentToken.spelling;
            accept(TokenKind.ID);
            paramList.add(new ParameterDecl(t, name,sp));
        }
        return paramList;
    }

    private ExprList parseArgList(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);

        ExprList argList = new ExprList();
        Expression expr = parseExpression(sp);
        argList.add(expr);
        while(currentToken.kind == TokenKind.COMMA){
            acceptIt();
            expr = parseExpression(sp);
            argList.add(expr);
        }
        return argList;
    }

    private Reference parseReference(SourcePosition sp){

        //int lineNumber = scanner.inputStream.getLineNumber();
        //SourcePosition sp = new SourcePosition(lineNumber);

        //for qualified reference, its ref.id -> (ref.id).id ...

        Reference ref = null;

        if(currentToken.kind == TokenKind.THIS || currentToken.kind == TokenKind.ID){
            if(currentToken.kind == TokenKind.THIS){
                acceptIt();
                ref = new ThisRef(sp);
            }
            else{
                ref = new IdRef(new Identifier(currentToken,sp),sp);
                acceptIt();
            }


            while(currentToken.kind == TokenKind.DOT){
                acceptIt();
                Identifier id = new Identifier(currentToken,sp);
                ref = new QualifiedRef(ref,id,sp);
                accept(TokenKind.ID);
            }

            if(currentToken.kind == TokenKind.LBRACK){
                //LxRef
                acceptIt();
                Expression exp = parseExpression(sp);

                if(exp == null){
                    //didn't get a reference, not a valid indexed ref
                    return null;
                }
                accept(TokenKind.RBRACK);

                ref = new IndexedRef(ref, exp, sp);
            }
        }


        return ref;
    }

}
