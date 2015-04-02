package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by cloftin on 3/15/15.
 * ATTRIBUTE will be a pointer to the idents declaration
 * multiple tables
 */

//TODO: unit test identification table

public class IdentificationTable {
    //we keep a seperate identificaiton table for methods, as they can share names with variables
    //make this a linkedlist instead of an arraylist
    //ArrayList<HashMap<String, Declaration>> fieldTable;
    //ArrayList<HashMap<String, Declaration>> methodTable;
    int currentScope = 0;
    LinkedList<HashMap<String,Declaration>> mainTable;
    HashMap<String,MethodDecl> methodTable;
    String className;

    //need to have tables for each class, cant assume we have a single class
    //declare a new one for each class?

    public IdentificationTable(String name) {
        /*fieldTable = new ArrayList<HashMap<String, Declaration>>();
        methodTable = new ArrayList<HashMap<String, Declaration>>();
        HashMap<String,Declaration> firstFieldTable = new HashMap<String,Declaration>();

        fieldTable.add(currentScope, firstFieldTable); //add the standard environment table to the arraylist
        HashMap<String,Declaration> firstMethodTable = new HashMap<String,Declaration>();
        methodTable.add(currentScope, firstMethodTable);*/
        this.className = name;

        mainTable = new LinkedList<HashMap<String, Declaration>>();
        HashMap<String,Declaration> firstFieldTable = new HashMap<String,Declaration>();

        firstFieldTable.put("true", new FieldDecl(false, true, new BaseType(TypeKind.BOOLEAN,null),"true",null));
        firstFieldTable.put("false", new FieldDecl(false, true, new BaseType(TypeKind.BOOLEAN,null),"false",null));


        mainTable.add(firstFieldTable);
        methodTable = new HashMap<String, MethodDecl>();
    }

    public void enter(String id, Declaration decl){
        //add a entry to the idTable, want to add it to the last entry as that is the scope we are wrokign with
        mainTable.getLast().put(id, decl); //enter a declaration at whatever id table we are concerned with
    }

    public void enterMethod(String id, MethodDecl decl){
        //add a method, methods can only be added in the outermost scope
        methodTable.put(id, decl); //enter a declaration at whatever id table we are concerned with
    }


    public Declaration retrieve(String id){
        //retrive the declaration associated with an ID at the smallest possible scope
        Declaration decl = null;
        int scope = currentScope;
        while(decl == null && scope>-1){
            HashMap<String, Declaration> thisScope = mainTable.get(scope);
            decl = thisScope.get(id); //HashMap.get returns null if nothing is found
            scope--; //increase scope
        }
        return decl;

    }

    public MethodDecl retrieveMethod(String id){
        //methods are declared within a single scope
        /*Declaration decl = null;
        int scope = currentScope;
        while(decl == null && scope!=0){
            decl = methodTable.get(scope).get(id); //HashMap.get returns null if nothing is found
            scope--; //increase scope
        }
        return decl;*/

        return methodTable.get(id);
    }

    public void openScope(){
        //open up a new scope level
        currentScope++;
        mainTable.add(new HashMap<String, Declaration>());

    }

    public void closeScope(){
        mainTable.removeLast(); //remove hashtable for largest scope so farw
        currentScope--;
    }

    //scoping will be handled in the main traversal

}
