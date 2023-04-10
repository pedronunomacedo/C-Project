package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;

public class JmmSymbolTable implements SymbolTable {

    private String className;
    private String superClassName;
    private final List<String> imports;
    private final HashMap<String, Symbol> fields;
    private final HashMap<String, JmmMethod> methods;
    private JmmMethod currentMethod;

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
    public List<Symbol> getFields() { return new ArrayList<>(this.fields.values()); }

    @Override
    public Type getReturnType(String methodName) {
        return this.methods.get(methodName).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return this.methods.get(methodName).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return this.methods.get(methodName).getLocalVariables();
    }

    public static Type getType(JmmNode node, String attribute) {
        Type type;
        if (node.getKind().equals("IntegerArrayType"))
            type = new Type("int", true);
        else if (node.get(attribute).equals("int"))
            type = new Type("int", false);
        else
            type = new Type(node.get(attribute), false);

        return type;
    }

    @Override
    public List<String> getMethods() {
        return List.copyOf(this.methods.keySet());
    }

    public JmmMethod getCurrentMethod() {
        return this.currentMethod;
    }

    public Symbol getField(String name) {
        for (Map.Entry<String, Symbol> field : this.fields.entrySet()) {
            if (field.getKey().equals(name))
                return field.getValue();
        }
        return null;
    }

    public boolean setField(String name, Symbol newSymbol) {
        for (Map.Entry<String, Symbol> field : this.fields.entrySet()) {
            if (field.getKey().equals(name)) {

                field.setValue(newSymbol);
                return true;
            }
        }
        return false;
    }

    public JmmMethod getMethod(String name, List<Type> params, Type returnType) {
        JmmMethod method = this.methods.get(name);

        if (method.getName().equals(name) && returnType.equals(method.getReturnType()) && params.size() == method.getParameters().size()) {
            if (JmmMethod.matchParameters(params, method.getParameterTypes())) {
                return method;
            }
        }

        throw new NoSuchMethodError();
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
        this.currentMethod = new JmmMethod(name);
        currentMethod.setReturnType(returnType);
        this.methods.put(name, currentMethod);
    }
}
