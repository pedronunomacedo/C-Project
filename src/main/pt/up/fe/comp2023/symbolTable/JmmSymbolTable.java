package pt.up.fe.comp2023.symbolTable;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {

    private String className;
    private String superClassName;
    private final List<String> imports;
    private final Map<String, Symbol> fields;
    private final List<JmmMethod> methods;
    private JmmMethod currentMethod;

    public JmmSymbolTable() {
        this.className = "";
        this.superClassName = "";
        this.imports = new ArrayList<String>();
        this.fields = new HashMap<>();
        this.methods = new ArrayList<>();
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
    public List<Symbol> getFields() { return new ArrayList<>(this.fields.values()); }

    @Override
    public Type getReturnType(String methodName) {
        System.out.println("On file JmmSymbolTable in function getReturnType()!");
        for (JmmMethod method : this.methods) {
            if (method.getName().equals(methodName)) {
                return method.getReturnType();
            }
            System.out.println("Method name: " + method.getName() + " | returnType: " + method.getReturnType());
        }
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

    public static Type getType(JmmNode node, String attribute) {
        Type type;
        if (node.get(attribute).equals("int[]"))
            type = new Type("int", true);
        else if (node.get(attribute).equals("int"))
            type = new Type("int", false);
        else
            type = new Type(node.get(attribute), false);

        return type;
    }

    @Override
    public List<String> getMethods() {
        System.out.println("In file JmmSymbolTable in function getMethods()!");
        List<String> methodsList = new ArrayList<>();
        for (JmmMethod method : this.methods) {
            methodsList.add(method.getName());
        }

        return methodsList;
    }

    public JmmMethod getCurrentMethod() {
        return this.currentMethod;
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

    public void addClassField(Symbol field) {
        this.fields.put(field.getName(), field);
    }

    public void addMethod(String name, Type returnType) {
        this.currentMethod = new JmmMethod(name, returnType);
        this.methods.add(currentMethod);
    }
}
