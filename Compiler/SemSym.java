import java.util.*;


public class SemSym {
    private Type type;
    private int offset;
    private int base;
    
    public SemSym(Type type, int offset) {
        this.type = type;
        this.offset = offset;
    }
    
    public Type getType() {
        return type;
    }

    public int getOffset() {
        return offset;
    }

    public int getNextOffset() {
        if (offset == 0) {
            return 0;
        }
        return offset - 4;
    }
    
    public String toString() {
        return type.toString();
    }
}


class FnSym extends SemSym {

    private Type returnType;
    private int numParams;
    private List<Type> paramTypes;
    private int size;
    
    public FnSym(Type type, int numparams, int size) {
        super(new FnType(), 0);
        returnType = type;
        numParams = numparams;
        this.size = size;
    }

    public void addFormals(List<Type> L) {
        paramTypes = L;
    }
    
    public Type getReturnType() {
        return returnType;
    }

    public int getNumParams() {
        return numParams;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public int getSize() {
        return size;
    }

    public String toString() {

        String str = "";
        boolean notfirst = false;
        for (Type type : paramTypes) {
            if (notfirst)
                str += ",";
            else
                notfirst = true;
            str += type.toString();
        }

        str += "->" + returnType.toString();
        return str;
    }
}


class StructSym extends SemSym {

    private IdNode structType;
    private int size;
    
    public StructSym(int offset, IdNode id, int size) {
        super(new StructType(id), offset);
        structType = id;
        this.size = size;
    }

    public IdNode getStructType() {
        return structType;
    }    

    public int getNextOffset() {
        return getOffset() - size;
    }

    public int getSize() {
        return size;
    }
}


class StructDefSym extends SemSym {

    private SymTable symTab;
    private int size;
    
    public StructDefSym(SymTable table, int size) {
        super(new StructDefType(), 4);
        symTab = table;
        this.size = size;
    }

    public SymTable getSymTable() {
        return symTab;
    }

    public int getSize() {
        return size;
    }
}
