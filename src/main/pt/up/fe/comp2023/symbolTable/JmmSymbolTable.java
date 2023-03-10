package pt.up.fe.comp2023.symbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {

    private String className;
    private String superClassName;

    private final List<String> imports;
    private final Map<String, Symbol> fields;
    private final Map<String, JmmMethod> methods;

    public JmmSymbolTable() {
        this.className = "";
        this.superClassName = "";
        this.imports = new ArrayList<String>();
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return null;
    }

    @Override
    public List<String> getMethods() {return null;}

    @Override
    public Type getReturnType(String s) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return null;
    }

    @Override
    public String print() {
        return SymbolTable.super.print();
    }


    public void addImport(String statement) {
        this.imports.add(statement);
    }

    public void addClassName(String className) {
        this.className = className;
    }

    public void addSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }
}
