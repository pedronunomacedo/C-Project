package pt.up.fe.comp2023.symbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class JmmMethod {
    private String name;
    private Type returnType;
    private final List<Map.Entry<Symbol, String>> parameters = new ArrayList<>();
    // Map from Symbol to Value -> null if the field is not initialized yet
    private final Map<Symbol, Boolean> localVariables = new HashMap<>();

    public JmmMethod(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public List<Map.Entry<Symbol, String>> getParameters() {
        return this.parameters;
    }

    public Type getReturnType() {
        return this.returnType;
    }

    public List<String> getParameterTypes() {
            List<String> params = new ArrayList<>();

        for (Map.Entry<Symbol, String> parameter : parameters) {
            params.add(parameter.getKey().getName());
        }

        return params;
    }

    public void addLocalVariable(Symbol variable) {
        localVariables.put(variable, false);
    }

    public void addParameter(Symbol param) {
        this.parameters.add(Map.entry(param, "param"));
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVariables.keySet());
    }

    public String getName() { return this.name; }

    public void setReturnType(Type type) {
        this.returnType = type;
    }
}
