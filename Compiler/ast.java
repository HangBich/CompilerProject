import java.io.*;
import java.util.*; 

abstract class ASTnode {
 
    abstract public void unparse(PrintWriter p, int indent); 
 
    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }
} 

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    } 
     
    public void nameAnalysis() {
        SymTable symTab = new SymTable();
        myDeclList.nameAnalysis(symTab, 0); 
 
        SemSym symbol = symTab.lookupLocal("main");
        if (symbol == null || !symbol.getType().isFnType()) {
            ErrMsg.fatal(0, 0, "No main function");
        }
    } 
     
    public void typeCheck() {
        myDeclList.typeCheck();
    } 
     
    public void codeGen() {
        myDeclList.codeGenGlobal();
    } 
    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    } 
 
    private DeclListNode myDeclList;
} 
class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    } 
     
    public int nameAnalysis(SymTable symTab, int offset) {
        return nameAnalysis(symTab, symTab, offset);
    } 
     
    public int nameAnalysis(SymTable symTab, SymTable globalTab, int offset) {
        for (DeclNode node : myDecls) {
            if (node instanceof VarDeclNode) {
                SemSym temp = ((VarDeclNode)node).nameAnalysis(symTab, globalTab, offset);
                if (temp != null) {
                    offset = temp.getNextOffset();
                }
            } else {
                SemSym temp = node.nameAnalysis(symTab, offset);
                if (temp != null) {
                    offset = temp.getNextOffset();
                }
            }
        }
        return offset;
    } 
     
    public void typeCheck() {
        for (DeclNode node : myDecls) {
            node.typeCheck();
        }
    } 
     
    public void codeGenGlobal() {
 
        boolean data = false;
        boolean text = false;
        for (DeclNode node : myDecls) {
            if (node instanceof VarDeclNode && !data) {
                Codegen.p.println("\t.data");
                data = true;
                text = false;
            } 
            if (node instanceof FnDeclNode && !text) {
                Codegen.p.println("\t.text");
                data = false;
                text = true;
            } 
            node.codeGen();
        }
    } 
     
    public void codeGen() {
        for (DeclNode node : myDecls) {
            node.codeGen();
        }
    } 
    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    } 
 
    private List<DeclNode> myDecls;
} 
class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    } 
     
    public List<Type> nameAnalysis(SymTable symTab, int offset) {
        List<Type> typeList = new LinkedList<Type>();
        for (FormalDeclNode node : myFormals) {
            offset += 4;
            SemSym sym = node.nameAnalysis(symTab, offset);
            if (sym != null) {
                typeList.add(sym.getType());
            }
        }
        return typeList;
    } 
     
    public int length() {
        return myFormals.size();
    } 
     
    public void codeGen() {
        for (int i = myFormals.size() - 1; i >= 0; --i) {
            myFormals.get(i).codeGen();
        }
    } 
    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { 
            it.next().unparse(p, indent);
            while (it.hasNext()) { 
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    } 
 
    private List<FormalDeclNode> myFormals;
} 
class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    } 
     
    public int nameAnalysis(SymTable symTab, int offset) {
        offset = myDeclList.nameAnalysis(symTab, -8);
        myStmtList.nameAnalysis(symTab, offset);
        return offset;
    } 
     
    public void typeCheck(Type retType) {
        myStmtList.typeCheck(retType);
    } 
     
    public void codeGen(String fn) {
        myDeclList.codeGen();
        myStmtList.codeGen(fn);
    } 
    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    } 
 
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
} 
class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        for (StmtNode node : myStmts) {
            node.nameAnalysis(symTab, offset);
        }
    } 
     
    public void typeCheck(Type retType) {
        for(StmtNode node : myStmts) {
            node.typeCheck(retType);
        }
    } 
     
    public void codeGen(String fn) {
        for (StmtNode node : myStmts) {
            node.codeGen(fn);
        }
    } 
    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    } 
 
    private List<StmtNode> myStmts;
} 
class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    } 
    public int size() {
        return myExps.size();
    } 
     
    public void nameAnalysis(SymTable symTab) {
        for (ExpNode node : myExps) {
            node.nameAnalysis(symTab);
        }
    } 
     
    public void typeCheck(List<Type> typeList) {
        int k = 0;
        try {
            for (ExpNode node : myExps) {
                Type actualType = node.typeCheck();  
                if (!actualType.isErrorType()) { 
                    Type formalType = typeList.get(k); 
                    if (!formalType.equals(actualType)) {
                        ErrMsg.fatal(node.lineNum(), node.charNum(),
                                "Type of actual does not match type of formal");
                    }
                }
                k++;
            }
        } catch (NoSuchElementException e) {
            System.err.println("unexpected NoSuchElementException in ExpListNode.typeCheck");
            System.exit(-1);
        }
    } 
     
    public void codeGenPush() {
        for (int i = myExps.size() - 1; i >= 0; --i) {
            myExps.get(i).codeGen();
        }
    } 
     
    public void codeGenPop() {
        for (ExpNode node : myExps) {
            Codegen.genPop(Codegen.T0);
        }
    } 
    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { 
            it.next().unparse(p, indent);
            while (it.hasNext()) { 
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    } 
 
    private List<ExpNode> myExps;
} 

abstract class DeclNode extends ASTnode {
     
    abstract public SemSym nameAnalysis(SymTable symTab, int offset); 
 
    public void typeCheck() { } 
 
    public void codeGen() { }
} 
class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    } 
     
    public SemSym nameAnalysis(SymTable symTab, int offset) {
        return nameAnalysis(symTab, symTab, offset);
    } 
    public SemSym nameAnalysis(SymTable symTab, SymTable globalTab, int offset) {
        boolean badDecl = false;
        String name = myId.name();
        SemSym sym = null;
        IdNode structId = null; 
        if (myType instanceof VoidNode) { 
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Non-function declared void");
            badDecl = true;
        } 
        else if (myType instanceof StructNode) {
            structId = ((StructNode)myType).idNode();
            sym = globalTab.lookupGlobal(structId.name()); 

            if (sym == null || !(sym instanceof StructDefSym)) {
                ErrMsg.fatal(structId.lineNum(), structId.charNum(),
                        "Invalid name of struct type");
                badDecl = true;
            }
            else {
                structId.link(sym);
            }
        } 
        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Multiply declared identifier");
            badDecl = true;
        } 
        if (!badDecl) { 
            try {
                if (myType instanceof StructNode) {
                    sym = new StructSym(offset, structId, ((StructDefSym)structId.sym()).getSize());
                }
                else {
                    sym = new SemSym(myType.type(), offset);
                }
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                        " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                        " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            }
        } 
        return sym;
    } 
     
    public void codeGen() {
 
        if (myId.sym().getOffset() == 0) {
            if (myId.sym().getType().isStructType()) {
                Codegen.generateLabeled("." + myId.name(), ".space", "", Integer.toString(((StructSym)myId.sym()).getSize()));
            }
            else {
                Codegen.generateLabeled("." + myId.name(), ".word", "", "0");
            }
        }
 
        else {
            myId.codeGenAddr();
            Codegen.genPop(Codegen.T0);
            Codegen.generate("li", Codegen.T1, 0);
            Codegen.generate("sw", Codegen.T1, "(" + Codegen.T0 + ")");
        }
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
        p.print("(");
        p.print(myId.sym().getOffset());
        p.println(");");
    } 
 
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  
    public static int NOT_STRUCT = -1;
} 
class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    } 
     
    public SemSym nameAnalysis(SymTable symTab, int offset) {
        String name = myId.name();
        FnSym sym = null; 
        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Multiply declared identifier");
        } 
        else { 
            try {
                sym = new FnSym(myType.type(), myFormalsList.length(), 0);
                symTab.addDecl(name, sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                        " in FnDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                        " in FnDeclNode.nameAnalysis");
                System.exit(-1);
            }
        } 
        symTab.addScope();  
 
        List<Type> typeList = myFormalsList.nameAnalysis(symTab, 0);
        if (sym != null) {
            sym.addFormals(typeList);
        } 
        int size = - myBody.nameAnalysis(symTab, -8) - 8;  
        try {
            symTab.removeScope(); 
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                    " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        } 
 
        try {
            symTab.removeDecl(name);
        } catch (DuplicateSymException ex) {
            System.err.println("Unexpected DuplicateSymException " +
                    " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                    " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        try {
            sym = new FnSym(myType.type(), myFormalsList.length(), size);
            if (sym != null) {
                sym.addFormals(typeList);
            }
            symTab.addDecl(name, sym);
            myId.link(sym);
        } catch (DuplicateSymException ex) {
            System.err.println("Unexpected DuplicateSymException " +
                    " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                    " in FnDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        return null;
    } 
     
    public void typeCheck() {
        myBody.typeCheck(myType.type());
    } 
     
    public void codeGen() {
 
        if (myId.name().equals("main")) {
            Codegen.p.println("\t.globl main");
            Codegen.genLabel(myId.name(), "MAIN FUNCTION ENTRY");
            Codegen.genLabel("__start", "");
            myFormalsList.codeGen();
        }
 
        else {
            Codegen.genLabel(myId.name(), "FUNCTION ENTRY");
        }
        Codegen.genPush(Codegen.RA);
        Codegen.genPush(Codegen.FP);
 
        Codegen.generate("addu", Codegen.FP, Codegen.SP, 8);
        Codegen.generate("subu", Codegen.SP, Codegen.SP, ((FnSym)myId.sym()).getSize());
        myBody.codeGen(myId.name()); 
 
        Codegen.genLabel("_" + myId.name() + "_Exit", "FUNCTION EXIT");
        Codegen.generateIndexed("lw", Codegen.RA, Codegen.FP, 0, "");
        Codegen.generateWithComment("move", "save control link", Codegen.T0, Codegen.FP);
        Codegen.generateIndexed("lw", Codegen.FP, Codegen.FP, -4, "restore FP");
        Codegen.generateWithComment("move", "restore SP", Codegen.SP, Codegen.T0);
        if (myId.name().equals("main")) {
            Codegen.generate("li", Codegen.V0, 10);
            Codegen.p.print("\tsyscall");
        }
        else {
            Codegen.generate("jr", Codegen.RA);
        }
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
        p.print("(");
        p.print(((FnSym)myId.sym()).getSize());
        p.print(") ");
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    } 
 
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
} 
class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    } 
     
    public SemSym nameAnalysis(SymTable symTab, int offset) {
        String name = myId.name();
        boolean badDecl = false;
        SemSym sym = null; 
        if (myType instanceof VoidNode) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Non-function declared void");
            badDecl = true;
        } 
        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Multiply declared identifier");
            badDecl = true;
        } 
        if (!badDecl) { 
            try {
                sym = new SemSym(myType.type(), offset);
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                        " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                        " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            }
        } 
        return sym;
    } 
     
    public void codeGen() {
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        p.print(myId.name());
        p.print("(");
        p.print(myId.sym().getOffset());
        p.print(")");
    } 
 
    private TypeNode myType;
    private IdNode myId;
} 
class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    } 
     
    public SemSym nameAnalysis(SymTable symTab, int offset) {
        String name = myId.name();
        boolean badDecl = false; 
        if (symTab.lookupLocal(name) != null) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Multiply declared identifier");
            badDecl = true;
        } 
        SymTable structSymTab = new SymTable(); 
 
        int size = 0;
        size = - myDeclList.nameAnalysis(structSymTab, symTab, -4);   
        if (!badDecl) {
            try { 
                StructDefSym sym = new StructDefSym(structSymTab, size);
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymException ex) {
                System.err.println("Unexpected DuplicateSymException " +
                        " in StructDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                        " in StructDeclNode.nameAnalysis");
                System.exit(-1);
            }
        } 
        return null;
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
        p.print(myId.name());
        p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n"); 
    } 
 
    private IdNode myId;
    private DeclListNode myDeclList;
} 

abstract class TypeNode extends ASTnode {
     
    abstract public Type type();
} 
class IntNode extends TypeNode {
    public IntNode() {
    } 
     
    public Type type() {
        return new IntType();
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }
} 
class BoolNode extends TypeNode {
    public BoolNode() {
    } 
     
    public Type type() {
        return new BoolType();
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }
} 
class VoidNode extends TypeNode {
    public VoidNode() {
    } 
     
    public Type type() {
        return new VoidType();
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
} 
class StructNode extends TypeNode {
    public StructNode(IdNode id) {
        myId = id;
    } 
    public IdNode idNode() {
        return myId;
    } 
     
    public Type type() {
        return new StructType(myId);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
        p.print(myId.name());
    } 
 
    private IdNode myId;
} 

abstract class StmtNode extends ASTnode {
    abstract public void nameAnalysis(SymTable symTab, int offset);
    abstract public void typeCheck(Type retType);
 
    abstract void codeGen(String fn);
} 
class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myAssign.nameAnalysis(symTab);
    } 
     
    public void typeCheck(Type retType) {
        myAssign.typeCheck();
    } 
     
    public void codeGen(String fn) {
        myAssign.codeGen();
        Codegen.genPop(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); 
        p.println(";");
    } 
 
    private AssignNode myAssign;
} 
class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myExp.nameAnalysis(symTab);
    } 
     
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck(); 
        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Arithmetic operator applied to non-numeric operand");
        }
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "Post Increment");
        myExp.codeGenAddr();
        myExp.codeGen();
 
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 1);
        Codegen.generate("add", Codegen.T1, Codegen.T0, Codegen.T1);
 
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sw", Codegen.T1, "(" + Codegen.T0 + ")");
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    } 
 
    private ExpNode myExp;
} 
class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myExp.nameAnalysis(symTab);
    } 
     
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck(); 
        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Arithmetic operator applied to non-numeric operand");
        }
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "Post Decrement");
        myExp.codeGenAddr();
        myExp.codeGen();
 
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 1);
        Codegen.generate("sub", Codegen.T1, Codegen.T0, Codegen.T1);
 
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sw", Codegen.T1, "(" + Codegen.T0 + ")");
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    } 
 
    private ExpNode myExp;
} 
class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myExp.nameAnalysis(symTab);
    } 
     
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck(); 
        if (type.isFnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Attempt to read a function");
        } 
        if (type.isStructDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Attempt to read a struct name");
        } 
        if (type.isStructType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Attempt to read a struct variable");
        }
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "READ");
        myExp.codeGenAddr();
 
        Codegen.generate("li", Codegen.V0, 5);
        Codegen.p.print("\tsyscall\n");
 
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sw", Codegen.V0, "(" + Codegen.T0 + ")");
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, 0);
        p.println(";");
    } 
 
    private ExpNode myExp;
} 
class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myExp.nameAnalysis(symTab);
    } 
     
    public void typeCheck(Type retType) {
        myType = myExp.typeCheck(); 
        if (myType.isFnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Attempt to write a function");
        } 
        if (myType.isStructDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Attempt to write a struct name");
        } 
        if (myType.isStructType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Attempt to write a struct variable");
        } 
        if (myType.isVoidType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Attempt to write void");
        }
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cout << ");
        myExp.unparse(p, 0);
        p.println(";");
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "WRITE");
        myExp.codeGen();
        Codegen.genPop(Codegen.A0); 
 
        if (myType.isStringType()) {
            Codegen.generate("li", Codegen.V0, 4);
        }
        else {
            Codegen.generate("li", Codegen.V0, 1);
        } 
        Codegen.p.println("\tsyscall");
    } 
 
    private ExpNode myExp;
    private Type myType;
} 
class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        offset = myDeclList.nameAnalysis(symTab, offset);
        myStmtList.nameAnalysis(symTab, offset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                    " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
    } 
     
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck(); 
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Non-bool expression used as an if condition");
        } 
        myStmtList.typeCheck(retType);
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "IF");
        String exit = Codegen.nextLabel();
        myExp.codeGen();
 
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 0);
        Codegen.generate("beq", Codegen.T0, Codegen.T1, exit);
 
        myDeclList.codeGen();
        myStmtList.codeGen(fn);
 
        Codegen.genLabel(exit, "IF exit");
    } 
 
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
} 
class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        int tempOffset = myThenDeclList.nameAnalysis(symTab, offset);
        myThenStmtList.nameAnalysis(symTab, tempOffset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                    " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
        symTab.addScope();
        tempOffset = myElseDeclList.nameAnalysis(symTab, offset);
        myElseStmtList.nameAnalysis(symTab, tempOffset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                    " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
    } 
     
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck(); 
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Non-bool expression used as an if condition");
        } 
        myThenStmtList.typeCheck(retType);
        myElseStmtList.typeCheck(retType);
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "IF ELSE");
        String elseBranch = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
        myExp.codeGen();
 
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 0);
        Codegen.generate("beq", Codegen.T0, Codegen.T1, elseBranch);
 
        myThenDeclList.codeGen();
        myThenStmtList.codeGen(fn);
        Codegen.generate("j", exit);
 
        Codegen.genLabel(elseBranch, "ELSE BRANCH");
        myElseDeclList.codeGen();
        myElseStmtList.codeGen(fn);
 
        Codegen.genLabel(exit, "IF-ELSE exit");
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    } 
 
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
} 
class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        offset = myDeclList.nameAnalysis(symTab, offset);
        myStmtList.nameAnalysis(symTab, offset);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                    " in IfStmtNode.nameAnalysis");
            System.exit(-1);
        }
    } 
     
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck(); 
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                    "Non-bool expression used as a while condition");
        } 
        myStmtList.typeCheck(retType);
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "WHILE");
        String predicate = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
 
        Codegen.genLabel(predicate, "WHILE predicate");
        myExp.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 0);
        Codegen.generate("beq", Codegen.T0, Codegen.T1, exit);
 
        myDeclList.codeGen();
        myStmtList.codeGen(fn);
        Codegen.generate("j", predicate);
 
        Codegen.genLabel(exit, "WHILE exit"); 
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    } 
 
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
} 
class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        myCall.nameAnalysis(symTab);
    } 
     
    public void typeCheck(Type retType) {
        myCall.typeCheck();
    } 
     
    public void codeGen(String fn) {
        myCall.codeGen();
        Codegen.genPop(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    } 
 
    private CallExpNode myCall;
} 
class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    } 
     
    public void nameAnalysis(SymTable symTab, int offset) {
        if (myExp != null) {
            myExp.nameAnalysis(symTab);
        }
    } 
     
    public void typeCheck(Type retType) {
        if (myExp != null) { 
            Type type = myExp.typeCheck(); 
            if (retType.isVoidType()) {
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                        "Return with a value in a void function");
            } 
            else if (!retType.isErrorType() && !type.isErrorType() && !retType.equals(type)){
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                        "Bad return value");
            }
        } 
        else { 
            if (!retType.isVoidType()) {
                ErrMsg.fatal(0, 0, "Missing return value");
            }
        } 
    } 
     
    public void codeGen(String fn) {
        Codegen.generateWithComment("", "RETURN");
        if (myExp != null) {
            myExp.codeGen();
            Codegen.genPop(Codegen.V0);
        }
        Codegen.generate("j", "_" + fn + "_Exit");
    } 
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    } 
 
    private ExpNode myExp; 
} 

abstract class ExpNode extends ASTnode {
     
    public void nameAnalysis(SymTable symTab) { } 
    abstract public Type typeCheck();
    abstract public int lineNum();
    abstract public int charNum(); 
     
    public void codeGen() { }
    public void codeGenAddr() { }
} 
class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    } 
     
    public int lineNum() {
        return myLineNum;
    } 
     
    public int charNum() {
        return myCharNum;
    } 
     
    public Type typeCheck() {
        return new IntType();
    } 
     
    public void codeGen() {
        Codegen.generate("li", Codegen.T0, myIntVal);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    } 
    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
} 
class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    } 
     
    public int lineNum() {
        return myLineNum;
    } 
     
    public int charNum() {
        return myCharNum;
    } 
     
    public Type typeCheck() {
        return new StringType();
    } 
     
    public void codeGen() {
 
        Codegen.p.println("\t.data");
        String label = Codegen.nextLabel();
        Codegen.generateLabeled(label, ".asciiz " + myStrVal, "");
 
        Codegen.p.println("\t.text");
        Codegen.generate("la", Codegen.T0, label);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    } 
    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
} 
class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    } 
     
    public int lineNum() {
        return myLineNum;
    } 
     
    public int charNum() {
        return myCharNum;
    } 
     
    public Type typeCheck() {
        return new BoolType();
    } 
     
    public void codeGen() {
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    } 
    private int myLineNum;
    private int myCharNum;
} 
class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    } 
     
    public int lineNum() {
        return myLineNum;
    } 
     
    public int charNum() {
        return myCharNum;
    } 
     
    public Type typeCheck() {
        return new BoolType();
    } 
     
    public void codeGen() {
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    } 
    private int myLineNum;
    private int myCharNum;
} 
class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
        myBase = 0;
    } 
     
    public void link(SemSym sym) {
        mySym = sym;
    } 
     
    public String name() {
        return myStrVal;
    } 
     
    public SemSym sym() {
        return mySym;
    } 
     
    public int lineNum() {
        return myLineNum;
    } 
     
    public int charNum() {
        return myCharNum;
    } 
     
    public void nameAnalysis(SymTable symTab) {
        SemSym sym = symTab.lookupGlobal(myStrVal);
        if (sym == null) {
            ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
        } else {
            link(sym);
        }
    } 
     
    public Type typeCheck() {
        if (mySym != null) {
            return mySym.getType();
        }
        else {
            System.err.println("ID with null sym field in IdNode.typeCheck");
            System.exit(-1);
        }
        return null;
    } 
     
    public void codeGen() {
        if (!mySym.getType().isFnType()) {
            int offset = mySym.getOffset();
 
            if (myFirst != null) {
                Codegen.generate("la", Codegen.T1, "." + myFirst);
                Codegen.generateIndexed("lw", Codegen.T0, Codegen.T1, -mySym.getOffset());
            }
            else if (offset == 0) {
                Codegen.generate("lw", Codegen.T0, "." + myStrVal);
            }
 
            else {
                Codegen.generateIndexed("lw", Codegen.T0, Codegen.FP, offset + myBase);
            }
            Codegen.genPush(Codegen.T0);
        }
    } 
     
    public void codeGenAddr() {
        int offset = mySym.getOffset();
 
        if (myFirst != null) {
            Codegen.generate("la", Codegen.T0, "." + myFirst);
        }
        else if (offset == 0) {
            Codegen.generate("la", Codegen.T0, "." + myStrVal);
        }
 
        else {
            if (offset > 0) {
                Codegen.generate("addu", Codegen.T0, Codegen.FP, offset + myBase);
            }
            else {
                Codegen.generate("subu", Codegen.T0, Codegen.FP, -(offset + myBase));
            }
        }
        Codegen.genPush(Codegen.T0);
    } 
     
    public void codeGenFn() {
        Codegen.generate("jal", myStrVal);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (mySym != null) {
            p.print("(" + mySym + ")");
        }
    } 
    public void setBase(int base) {
        myBase = base;
    } 
    public void setFirst(String first) {
        myFirst = first;
    } 
    public String getFirst() {
        return myFirst;
    } 
    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private SemSym mySym;
    private int myBase;
    private String myFirst; 
} 
class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;
        myId = id;
        mySym = null;
    } 
     
    public SemSym sym() {
        return mySym;
    } 
     
    public int lineNum() {
        return myId.lineNum();
    } 
     
    public int charNum() {
        return myId.charNum();
    } 
     
    public void nameAnalysis(SymTable symTab) {
        badAccess = false;
        SymTable structSymTab = null; 
        SemSym sym = null;
        int nextBase = 0; 
        myLoc.nameAnalysis(symTab);  
 
        if (myLoc instanceof IdNode) {
            IdNode id = (IdNode)myLoc;
            sym = id.sym(); 
  
            if (sym == null) { 
                badAccess = true;
            }
            else if (sym instanceof StructSym) {
 
                SemSym tempSym = ((StructSym)sym).getStructType().sym();
                structSymTab = ((StructDefSym)tempSym).getSymTable();
                nextBase = sym.getOffset();
 
                if (nextBase == 0) {
                    myFirst = id.name();
                }
            }
            else { 
                ErrMsg.fatal(id.lineNum(), id.charNum(),
                        "Dot-access of non-struct type");
                badAccess = true;
            }
        } 

        else if (myLoc instanceof DotAccessExpNode) {
            DotAccessExpNode loc = (DotAccessExpNode)myLoc; 
            if (loc.badAccess) { 
                badAccess = true; 
            }
            else { 
                sym = loc.sym(); 
                if (sym == null) { 
                    ErrMsg.fatal(loc.lineNum(), loc.charNum(),
                            "Dot-access of non-struct type");
                    badAccess = true;
                }
                else { 
                    if (sym instanceof StructDefSym) {
                        nextBase = myBase + sym.getOffset();
                        myFirst = loc.getFirst();
                        structSymTab = ((StructDefSym)sym).getSymTable();
                    }
                    else {
                        System.err.println("Unexpected Sym type in DotAccessExpNode");
                        System.exit(-1);
                    }
                }
            } 
        } 
        else { 
            System.err.println("Unexpected node type in LHS of dot-access");
            System.exit(-1);
        } 
 
        if (!badAccess) { 
            sym = structSymTab.lookupGlobal(myId.name()); 
            if (sym == null) { 
                ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                        "Invalid struct field name");
                badAccess = true;
            } 
            else {
                myId.setBase(nextBase);
                if (myFirst != null) {
                    myId.setFirst(myFirst);
                }
                myId.link(sym);  

                if (sym instanceof StructSym) {
                    mySym = ((StructSym)sym).getStructType().sym();
                }
            }
        }
    } 
     
    public Type typeCheck() {
        return myId.typeCheck();
    } 
    public void codeGen() {
        myId.codeGen();
    } 
    public void codeGenAddr() {
        myId.codeGenAddr();
    } 
    public void unparse(PrintWriter p, int indent) {
        myLoc.unparse(p, 0);
        p.print(".");
        myId.unparse(p, 0);
    } 
    public void setBase(int base) {
        myBase = base;
    } 
    public String getFirst() {
        return myFirst;
    } 
    public int idOffset() {
        return myId.sym().getOffset();
    } 
 
    private ExpNode myLoc;
    private IdNode myId;
    private SemSym mySym; 
    private int myBase;
    private String myFirst; 
    private boolean badAccess; 
} 
class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    } 
     
    public int lineNum() {
        return myLhs.lineNum();
    } 
     
    public int charNum() {
        return myLhs.charNum();
    } 
     
    public void nameAnalysis(SymTable symTab) {
        myLhs.nameAnalysis(symTab);
        myExp.nameAnalysis(symTab);
    } 
     
    public Type typeCheck() {
        Type typeLhs = myLhs.typeCheck();
        Type typeExp = myExp.typeCheck();
        Type retType = typeLhs; 
        if (typeLhs.isFnType() && typeExp.isFnType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Function assignment");
            retType = new ErrorType();
        } 
        if (typeLhs.isStructDefType() && typeExp.isStructDefType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Struct name assignment");
            retType = new ErrorType();
        } 
        if (typeLhs.isStructType() && typeExp.isStructType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Struct variable assignment");
            retType = new ErrorType();
        } 
        if (!typeLhs.equals(typeExp) && !typeLhs.isErrorType() && !typeExp.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Type mismatch");
            retType = new ErrorType();
        } 
        if (typeLhs.isErrorType() || typeExp.isErrorType()) {
            retType = new ErrorType();
        } 
        return retType;
    } 
     
    public void codeGen() {
        Codegen.generateWithComment("", "ASSIGN");
        myLhs.codeGenAddr();
        myExp.codeGen();
 
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        if (myLhs instanceof DotAccessExpNode) {
            DotAccessExpNode myNode = (DotAccessExpNode) myLhs;
            if (myNode.getFirst() != null) {
                Codegen.generateIndexed("sw", Codegen.T1, Codegen.T0, -myNode.idOffset());
            }
            else {
                Codegen.generate("sw", Codegen.T1, "(" + Codegen.T0 + ")");
            }
        }
        else {
            Codegen.generate("sw", Codegen.T1, "(" + Codegen.T0 + ")");
        }
        Codegen.genPush(Codegen.T1);
    } 
    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)  p.print(")");
    } 
 
    private ExpNode myLhs;
    private ExpNode myExp;
} 
class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    } 
    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    } 
     
    public int lineNum() {
        return myId.lineNum();
    } 
     
    public int charNum() {
        return myId.charNum();
    } 
     
    public void nameAnalysis(SymTable symTab) {
        myId.nameAnalysis(symTab);
        myExpList.nameAnalysis(symTab);
    } 
     
    public Type typeCheck() {
        if (!myId.typeCheck().isFnType()) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Attempt to call a non-function");
            return new ErrorType();
        } 
        FnSym fnSym = (FnSym)(myId.sym()); 
        if (fnSym == null) {
            System.err.println("null sym for Id in CallExpNode.typeCheck");
            System.exit(-1);
        } 
        if (myExpList.size() != fnSym.getNumParams()) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(),
                    "Function call with wrong number of args");
            return fnSym.getReturnType();
        } 
        myExpList.typeCheck(fnSym.getParamTypes());
        return fnSym.getReturnType();
    } 
     
    public void codeGen() {
        if (myExpList != null) {
            myExpList.codeGenPush();
        }
        myId.codeGenFn();
        if (myExpList != null) {
            myExpList.codeGenPop();
        }
        Codegen.genPush(Codegen.V0);
    } 
 
    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");
    } 
 
    private IdNode myId;
    private ExpListNode myExpList; 
} 
abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    } 
     
    public int lineNum() {
        return myExp.lineNum();
    } 
     
    public int charNum() {
        return myExp.charNum();
    } 
     
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    } 
 
    protected ExpNode myExp;
} 
abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    } 
     
    public int lineNum() {
        return myExp1.lineNum();
    } 
     
    public int charNum() {
        return myExp1.charNum();
    } 
     
    public void nameAnalysis(SymTable symTab) {
        myExp1.nameAnalysis(symTab);
        myExp2.nameAnalysis(symTab);
    } 
 
    protected ExpNode myExp1;
    protected ExpNode myExp2;
} 

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    } 
     
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new IntType(); 
        if (!type.isErrorType() && !type.isIntType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                    "Arithmetic operator applied to non-numeric operand");
            retType = new ErrorType();
        } 
        if (type.isErrorType()) {
            retType = new ErrorType();
        } 
        return retType;
    } 
     
    public void codeGen() {
        myExp.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 0);
        Codegen.generate("sub", Codegen.T0, Codegen.T1, Codegen.T0);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
} 
class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    } 
     
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new BoolType(); 
        if (!type.isErrorType() && !type.isBoolType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                    "Logical operator applied to non-bool operand");
            retType = new ErrorType();
        } 
        if (type.isErrorType()) {
            retType = new ErrorType();
        } 
        return retType;
    } 
     
    public void codeGen() {
        myExp.codeGen();
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genPop(Codegen.T1);
        Codegen.generate("subu", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(!");
        myExp.unparse(p, 0);
        p.print(")");
    }
} 

abstract class ArithmeticExpNode extends BinaryExpNode {
    public ArithmeticExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new IntType(); 
        if (!type1.isErrorType() && !type1.isIntType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                    "Arithmetic operator applied to non-numeric operand");
            retType = new ErrorType();
        } 
        if (!type2.isErrorType() && !type2.isIntType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                    "Arithmetic operator applied to non-numeric operand");
            retType = new ErrorType();
        } 
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        } 
        return retType;
    }
} 
abstract class LogicalExpNode extends BinaryExpNode {
    public LogicalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType(); 
        if (!type1.isErrorType() && !type1.isBoolType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                    "Logical operator applied to non-bool operand");
            retType = new ErrorType();
        } 
        if (!type2.isErrorType() && !type2.isBoolType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                    "Logical operator applied to non-bool operand");
            retType = new ErrorType();
        } 
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        } 
        return retType;
    }
} 
abstract class EqualityExpNode extends BinaryExpNode {
    public EqualityExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType(); 
        if (type1.isVoidType() && type2.isVoidType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                    "Equality operator applied to void functions");
            retType = new ErrorType();
        } 
        if (type1.isFnType() && type2.isFnType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                    "Equality operator applied to functions");
            retType = new ErrorType();
        } 
        if (type1.isStructDefType() && type2.isStructDefType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                    "Equality operator applied to struct names");
            retType = new ErrorType();
        } 
        if (type1.isStructType() && type2.isStructType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                    "Equality operator applied to struct variables");
            retType = new ErrorType();
        } 
        if (!type1.equals(type2) && !type1.isErrorType() && !type2.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                    "Type mismatch");
            retType = new ErrorType();
        } 
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        } 
        return retType;
    }
} 
abstract class RelationalExpNode extends BinaryExpNode {
    public RelationalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new BoolType(); 
        if (!type1.isErrorType() && !type1.isIntType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                    "Relational operator applied to non-numeric operand");
            retType = new ErrorType();
        } 
        if (!type2.isErrorType() && !type2.isIntType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                    "Relational operator applied to non-numeric operand");
            retType = new ErrorType();
        } 
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        } 
        return retType;
    }
} 
class PlusNode extends ArithmeticExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("add", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class MinusNode extends ArithmeticExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sub", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class TimesNode extends ArithmeticExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("mult", Codegen.T0, Codegen.T1);
        Codegen.generate("mflo", Codegen.T0);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class DivideNode extends ArithmeticExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("div", Codegen.T0, Codegen.T1);
        Codegen.generate("mflo", Codegen.T0);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class AndNode extends LogicalExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 0);
        Codegen.generate("beq", Codegen.T0, Codegen.T1, exit);
        myExp2.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.genLabel(exit, "AND EXIT");
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" && ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class OrNode extends LogicalExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 0);
        Codegen.generate("bne", Codegen.T0, Codegen.T1, exit);
        myExp2.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.genLabel(exit, "OR EXIT");
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" || ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class EqualsNode extends EqualityExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String branch = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
 
        Codegen.generate("beq", Codegen.T0, Codegen.T1, branch);
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.generate("j", exit);
        Codegen.genLabel(branch);
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genLabel(exit);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class NotEqualsNode extends EqualityExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String branch = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
 
        Codegen.generate("bne", Codegen.T0, Codegen.T1, branch);
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.generate("j", exit);
        Codegen.genLabel(branch);
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genLabel(exit);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" != ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class LessNode extends RelationalExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String branch = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sub", Codegen.T0, Codegen.T0, Codegen.T1);
 
        Codegen.generate("bltz", Codegen.T0, branch);
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.generate("j", exit);
        Codegen.genLabel(branch);
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genLabel(exit);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class GreaterNode extends RelationalExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String branch = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sub", Codegen.T0, Codegen.T0, Codegen.T1);
 
        Codegen.generate("bgtz", Codegen.T0, branch);
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.generate("j", exit);
        Codegen.genLabel(branch);
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genLabel(exit);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class LessEqNode extends RelationalExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String branch = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sub", Codegen.T0, Codegen.T0, Codegen.T1);
 
        Codegen.generate("blez", Codegen.T0, branch);
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.generate("j", exit);
        Codegen.genLabel(branch);
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genLabel(exit);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
} 
class GreaterEqNode extends RelationalExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    } 
     
    public void codeGen() {
        String branch = Codegen.nextLabel();
        String exit = Codegen.nextLabel();
        myExp1.codeGen();
        myExp2.codeGen();
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        Codegen.generate("sub", Codegen.T0, Codegen.T0, Codegen.T1);
 
        Codegen.generate("bgez", Codegen.T0, branch);
        Codegen.generate("li", Codegen.T0, 0);
        Codegen.generate("j", exit);
        Codegen.genLabel(branch);
        Codegen.generate("li", Codegen.T0, 1);
        Codegen.genLabel(exit);
        Codegen.genPush(Codegen.T0);
    } 
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}
