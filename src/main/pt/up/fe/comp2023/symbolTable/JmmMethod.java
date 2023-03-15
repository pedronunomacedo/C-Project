package pt.up.fe.comp2023.symbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class JmmMethod {
    private String name;
    private Type returnType;
    private final Map<String, Symbol> parameters;
    // Map from Symbol to Value -> null if the field is not initialized yet
    private final Map<String, Symbol> localVariables;

    public JmmMethod(String name) {
        this.name = name;
        this.parameters = new HashMap<String, Symbol>();
        this.localVariables = new HashMap<String, Symbol>();
    }

    public List<Symbol> getParameters() {
        return new ArrayList<>(this.parameters.values());
    }

    public Type getReturnType() {
        return this.returnType;
    }

    public List<String> getParameterTypes() {
        List<String> params = new ArrayList<>();

        for (Map.Entry<String, Symbol> parameter : parameters.entrySet()) {
            params.add(parameter.getKey());
        }

        return params;
    }

    public void addLocalVariable(Symbol variable) {
        this.localVariables.put(variable.getName(), variable);
    }

    public void addParameter(Symbol param) {
        this.parameters.put(param.getName(), param);
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVariables.values());
    }

    public String getName() { return this.name; }

    public void setReturnType(Type type) {
        this.returnType = type;
    }
}
