import java.io.*;
import java.lang.*;

import java_cup.parser;
import java_cup.runtime.*;
public class Compiler {
    FileReader inFile;
    private PrintWriter outFile;
    private static PrintStream outStream = System.err;

    public static final int RESULT_CORRECT = 0;
    public static final int RESULT_SYNTAX_ERROR = 1;
    public static final int RESULT_NAME_ERROR = 2;
    public static final int RESULT_TYPE_ERROR = 3;
    public static final int RESULT_OTHER_ERROR = -1;
    public Compiler(){
    }
    private Compiler(String[] args){

        if (args.length < 2) {
	        String msg = "please supply name of file to be parsed"
			        + "and name of file for unparsed version.";
	        pukeAndDie(msg);
        }

        try{
            setInfile(args[0]);
            setOutfile(args[1]);
        } catch(BadInfileException e){
            pukeAndDie(e.getMessage());			
        } catch(BadOutfileException e){
            pukeAndDie(e.getMessage());
        }
    }
    public void setInfile(String filename) throws BadInfileException{
        try {
            inFile = new FileReader(filename);
        } catch (FileNotFoundException ex) {
            throw new BadInfileException(ex, filename);
        }
    }
    public void setOutfile(String filename) throws BadOutfileException{
        try {
            Codegen.p = new PrintWriter(filename);
        } catch (FileNotFoundException ex) {
            throw new BadOutfileException(ex, filename);
        }
    }
    public void cleanup(){
        if (inFile != null){
            try {
                inFile.close();
            } catch (IOException e) {
            }
        }
        if (outFile != null){
            outFile.flush();
            outFile.close();
        }
    }
    private void pukeAndDie(String error){
        pukeAndDie(error, -1);
    }
    private void pukeAndDie(String error, int retCode){
        outStream.println(error);
        cleanup();
        System.exit(-1);		
    }
    private Symbol parseCFG(){
        try {
            parser P = new parser(new Yylex(inFile));
            return P.parse();
        } catch (Exception e){
            return null;
        }
    }

    public int process(){
        Symbol cfgRoot = parseCFG();

        ProgramNode astRoot = (ProgramNode)cfgRoot.value; 
        if (ErrMsg.getErr()) {  
            return Compiler.RESULT_SYNTAX_ERROR;
        }

        astRoot.nameAnalysis();
        if (ErrMsg.getErr()) {

            return Compiler.RESULT_NAME_ERROR;
        }		

        astRoot.typeCheck();
        if (ErrMsg.getErr()){

            return Compiler.RESULT_TYPE_ERROR;
        }

        astRoot.codeGen();
        return Compiler.RESULT_CORRECT;
    }

    public void run(){
        int resultCode = process();
        if (resultCode == RESULT_CORRECT){
            cleanup();
            return;
        }

        switch(resultCode){
        case RESULT_SYNTAX_ERROR:
            pukeAndDie("Syntax error", resultCode);
        case RESULT_NAME_ERROR:
            pukeAndDie("Name analysis error", resultCode);
        case RESULT_TYPE_ERROR:
            pukeAndDie("Type checking error", resultCode);
        default:
            pukeAndDie("Type checking error", RESULT_OTHER_ERROR);
        }
    }

    private class BadInfileException extends Exception{
        private static final long serialVersionUID = 1L;		
        private String message;

        public BadInfileException(Exception cause, String filename) {
            super(cause);
            this.message = "Could not open " + filename + " for reading";
        }

        @Override
        public String getMessage(){
            return message;
        }
    }

    private class BadOutfileException extends Exception{
        private static final long serialVersionUID = 1L;		
        private String message;

        public BadOutfileException(Exception cause, String filename) {
            super(cause);
            this.message = "Could not open " + filename + " for reading";
        }

        @Override
        public String getMessage(){
            return message;
        }
    }

    public static void main(String[] args){
        Compiler instance = new Compiler(args);
        instance.run();
        Codegen.p.close();
    }
}
