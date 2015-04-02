package miniJava;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.LineNumberInputStream;
import miniJava.SyntacticAnalyzer.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ContextualAnalysis.*;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.FileReader;

class Compiler{

    //you can do anything, but it will all require programming ability. Coding is the new reading and writing
    //TODO: PA3: Identification
    //TODO: modify Identifier class and add a declaration field
    public static void main(String args[]){

        //  fail 167? 168

        File file = new File("/Users/cloftin/miniJava/standard_test.java"); //3rd argument?
        //FileInputStream stream;
        FileReader reader;
        LineNumberReader stream;
        boolean valid;

        try{
            //stream = new FileInputStream(file);
            reader = new FileReader(file);
            stream = new LineNumberReader(reader);
            Scanner scanner = new Scanner(stream);
            Parser parser = new Parser(scanner);
            TypeCheck checker = new TypeCheck();

            AST ast = parser.parse();

            if(ast != null){
                ASTDisplay display = new ASTDisplay();
                //display.showTree(ast);
                //ident.identify((AbstractSyntaxTrees.Package)ast);
                checker.check((miniJava.AbstractSyntaxTrees.Package)ast);

                System.exit(0); //program was successfully parsed
            }
            else{
                System.exit(4); //invalid
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
}

//OR, AND, EQUALITY, RELATION, ADD/SUB, MULT, UN
