package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.Arrays;
import java.util.*;

public class JmmMethod {
    private String name;
    private Type returnType;
    private final Map<String, Symbol> parameters;
    // Map from Symbol to Value -> null if the field is not initialized yet
    private final Map<String, Symbol> localVariables;
    private Symbol currentLocalVariable;

    public JmmMethod(String name) {
        this.name = name;
        this.parameters = new HashMap<String, Symbol>();
        this.localVariables = new HashMap<String, Symbol>();
    }

    public List<Symbol> getParameters() {
        return new ArrayList<>(this.parameters.values());
    }

    public int getParameterIndex(String parameter) {
        int index = 1;
        Symbol param = this.parameters.get(parameter);

        for (Map.Entry<String, Symbol> entry : this.parameters.entrySet()) {
            if (entry.getValue().equals(param)) return index;
            index++;
        }

        return index;
    }

    public List<String> getParametersNames() {
        return new ArrayList<>(this.parameters.keySet());
    }

    public Symbol getParameter(String paramName) {
        return this.parameters.get(paramName);
    }

    public Type getReturnType() {
        return this.returnType;
    }

    public List<Type> getParameterTypes() {
        List<Type> params = new ArrayList<>();

        for (Map.Entry<String, Symbol> parameter : parameters.entrySet()) {
            params.add(parameter.getValue().getType());
        }

        return params;
    }

    public void addLocalVariable(Symbol variable) {
        this.currentLocalVariable = variable;
        this.localVariables.put(variable.getName(), variable);
    }

    public void addParameter(Symbol param) {
        this.parameters.put(param.getName(), param);
    }

    public List<Symbol> getLocalVariables() {
        return new ArrayList<>(this.localVariables.values());
    }

    public String getName() { return this.name; }
    public Symbol getCurrentLocalVariable() { return this.currentLocalVariable; }

    public void setReturnType(Type type) {
        this.returnType = type;
    }

    public Symbol getLocalVariable(String varName) {
        return this.localVariables.get(varName);
    }

    public static boolean matchParameters(List<Symbol> types1, List<Symbol> types2) {
        for (int i = 0; i < types1.size(); i++) {
            if (!types1.get(i).equals(types2.get(i))) {
                return false;
            }
        }
        return true;
    }
}
