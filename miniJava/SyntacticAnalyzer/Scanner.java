package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.lang.StringBuilder;

public class Scanner{

    private char currentChar, nextChar; //current char being scanned and one after it
    private TokenKind currentKind; //I'm going to use an enum instead of an integer literal for readability
    private StringBuilder currentSpelling; //the spelling of this scanner token so far
    //private File source; //course file in question

    //private FileInputStream inputStream;
    public LineNumberReader inputStream;

    public Scanner(LineNumberReader inputStream){
        currentSpelling = new StringBuilder();
        this.inputStream = inputStream;

        //nextChar = ' ';

        readChar();
        readChar(); //read twice so nextChar points to the second character


    }

    //TODO: infinite loops for unterminate line comments

    private void take(char expectedChar){
        if(currentChar == expectedChar){
            currentSpelling.append(currentChar);
            nextChar();
        }
        return;
    }

    private void takeIt(){


        currentSpelling.append(currentChar);
        //System.out.println("current spelling: " + currentSpelling);
        nextChar();
        return;
    }

    private void nextChar(){
        /*if(currentSpelling.length() != 0 && currentSpelling.toString() == "//"){
            isComment =true;
        }
        if(isComment && (currentChar == '\n' || currentChar == '\r')){
            //this will take a while to debug, but it might work
            isComment = false;
            currentSpelling.setLength(0); //clear the string buffer
            readChar(); //start over on the next line
        }*/

        //TODO: removed the check for $ at each call of nextChar, dow this break anything
        readChar();


    }

    private void readChar(){
        currentChar = nextChar;
        //System.out.println("current char: " + currentChar);
        try {
            int c = inputStream.read();
            nextChar = (char) c; //look ahead by one

            //is the character an eof character?
            if (c == -1 || c == '\u0000') {
                nextChar = '\u0000'; //always default to the EOT token
            }

        }
        catch(IOException e){
            System.err.println("IO Exception, terminating scan");
            nextChar = '\u0000'; //terminate the read
        }

    }


    private boolean isLetter(char current){
        return (current >= 'a' && current <= 'z') || (current >= 'A' && current <= 'Z' || current == '_');
    }

    private boolean isDigit(char current){
        return (current >= '0' && current <= '9');
    }

    private boolean isOperator(char current){
        return (current == '+' || current == '-' || current == '*' || current == '/' ||
                current == '=' || current == '<' || current == '>' || current == '\\' ||
                current == '&' || current == '@' || current == '%' || current == '^' ||
                current == '?');
    }


    public Token scan(){
        //skip whitespace of any kind
        //TODO: this is clunky, hard to fix comment issues until we have this resolved

        //check if its the start of a (or several) comments and skip
        while ((currentChar == '/') && (nextChar == currentChar)) {
            //line comment, skip it
            while (!(currentChar == '\n' || currentChar == '\r')) {
                if(currentChar == '\u0000'){
                    return new Token(TokenKind.ERROR,""); //unterminated block comment
                }
                nextChar(); //skip over it
            }
            nextChar();

        }


        //check for block comments
        while((currentChar == '/') && (nextChar == '*')){
            nextChar();
            nextChar();
            while (!(currentChar == '*' && nextChar == '/')) {
                if(currentChar == '\u0000'){
                    return new Token(TokenKind.ERROR,""); //unterminated block comment
                }
                nextChar(); //skip over it
            }
            nextChar();
            nextChar();
        }

        //if whitespace, skip and then check if we have moved into a comment, loop
        while(currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r') {
            nextChar();

            while ((currentChar == '/') && (nextChar == currentChar)) {
                //line comment, skip it
                while (!(currentChar == '\n' || currentChar == '\r')) {
                    if(currentChar == '\u0000'){
                        return new Token(TokenKind.ERROR,""); //unterminated block comment
                    }
                    nextChar(); //skip over it
                }
                nextChar();

            }

            while((currentChar == '/') && (nextChar == '*')){
                //needs to skip two before continuing
                nextChar();
                nextChar();
                while (!(currentChar == '*' && nextChar == '/')) {
                    if(currentChar == '\u0000'){
                        return new Token(TokenKind.ERROR,""); //unterminated block comment
                    }
                    nextChar(); //skip over it
                }
                //skip twice for two chars in their terminating token
                nextChar();
                nextChar();
            }

        }



        currentSpelling = new StringBuilder(); //start building a new lexical token with string builder


        //TODO: removing this seems to work
        /*if(isLetter(currentChar)){

            //problem here, what if its a single char?
            takeIt();
        }*/

        TokenKind kind = scanToken();


        return new Token(kind, currentSpelling.toString());
    }

    private TokenKind scanToken(){

        //returns what kind of token it is
        //System.out.println("scanning new token");
        char check;
        switch(currentChar){
            case 'a':  case 'b':  case 'c':  case 'd':  case 'e':
            case 'f':  case 'g':  case 'h':  case 'i':  case 'j':
            case 'k':  case 'l':  case 'm':  case 'n':  case 'o':
            case 'p':  case 'q':  case 'r':  case 's':  case 't':
            case 'u':  case 'v':  case 'w':  case 'x':  case 'y':
            case 'z':
            case 'A':  case 'B':  case 'C':  case 'D':  case 'E':
            case 'F':  case 'G':  case 'H':  case 'I':  case 'J':
            case 'K':  case 'L':  case 'M':  case 'N':  case 'O':
            case 'P':  case 'Q':  case 'R':  case 'S':  case 'T':
            case 'U':  case 'V':  case 'W':  case 'X':  case 'Y':
            case 'Z':
                //System.out.println("accepting letter");
                takeIt();
                while (isLetter(currentChar) || isDigit(currentChar)){
                    //keep taking in chars and updating the spelling
                    takeIt();
                }
                    //System.out.println("new ID: " + currentSpelling.toString());
                    return TokenKind.ID; //gets returned as an ID, token will catch keywords


            case '0':  case '1':  case '2':  case '3':  case '4':
            case '5':  case '6':  case '7':  case '8':  case '9':
                takeIt();
                while (isDigit(currentChar))
                    takeIt();
                return TokenKind.NUM;

            case '+':case '*':case '/':
                check = currentChar;
                takeIt();
                if(currentChar == check) return TokenKind.ERROR;
                return TokenKind.BINOP;

            case '-':
                takeIt();
                if(currentChar == '-') return TokenKind.ERROR;
                return TokenKind.NEG;

            case '<':case '>':
                check = currentChar;
                takeIt();
                if(currentChar == check) return TokenKind.ERROR;
                if(currentChar == '='){
                    takeIt();
                }
                return TokenKind.BINOP;

            case '&':
                takeIt();
                if(currentChar == '&'){
                    takeIt();
                    return TokenKind.BINOP;
                }
                else return TokenKind.ERROR; //must have &&

            case '|':
                takeIt();
                if(currentChar == '|'){
                    takeIt();
                    return TokenKind.BINOP;
                }
                else return TokenKind.ERROR; //must have &&

            case '!':
                takeIt();
                if(currentChar == '='){
                    takeIt();
                    return TokenKind.BINOP;
                }
                else return TokenKind.UNOP;

            case '=':
                takeIt();
                if(currentChar == '='){
                    takeIt();
                    return TokenKind.BINOP;
                }
                else return TokenKind.EQUALS;

            case '\'':
                takeIt();
                takeIt();
                return TokenKind.ERROR;
            case '.':
                takeIt();
                return TokenKind.DOT;

            case ';':
                takeIt();
                return TokenKind.SEMIC;

            case ',':
                takeIt();
                return TokenKind.COMMA;

            case '(':
                takeIt();
                return TokenKind.LPAREN;

            case ')':
                takeIt();
                return TokenKind.RPAREN;

            case '[':
                takeIt();
                return TokenKind.LBRACK;

            case ']':
                takeIt();
                return TokenKind.RBRACK;

            case '{':
                takeIt();
                return TokenKind.LCURL;

            case '}':
                takeIt();
                return TokenKind.RCURL;

            case '\u0000':
                return TokenKind.EOT;

            default:
                takeIt();
                return TokenKind.ERROR;
        }
    }






}
