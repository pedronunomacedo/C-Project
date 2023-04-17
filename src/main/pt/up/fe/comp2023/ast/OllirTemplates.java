package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class OllirTemplates {
    public static String subImportTemplate(String name) {
        return "." + name;
    }

    public static String importTemplate(String name, String subImport) {
        return "import " + name + subImport + ";\n";
    }

    public static String classTemplate(String name, String extendedClass) {
        StringBuilder ollir = new StringBuilder();
        ollir.append(name).append(extendedClass).append(openBrackets());
        return ollir.toString();
    }

    public static String extendedClassTemplate(String extendedName) {
        StringBuilder ollir = new StringBuilder();
        ollir.append(String.format(" extends %s ", extendedName));
        return ollir.toString();
    }

    public static String openBrackets() {
        return "{\n";
    }

    public static String closeBrackets() {
        return "}\n\n";
    }

    public static String fieldTemplate(Symbol field) {
        return ".field private " + field.getName() + type(field.getType()) +  ";\n";
    }

    public static String methodTemplate(String name, List<Symbol> parameters, Type returnType, boolean isMain) {
        StringBuilder ollirCode = new StringBuilder(".method public ");

        if (isMain) ollirCode.append("static ");

        // parameters
        ollirCode.append(name).append("(");
        if (isMain) {
            ollirCode.append(declareVariable(parameters.get(0), true));
        } else {
            // method parameters/arguments

            if (parameters.size() != 0) {
                ollirCode.append(declareVariable(parameters.get(0), true));
                if (parameters.size() > 1) {
                    for (int i = 1; i < parameters.size(); i++) {
                        ollirCode.append(", ").append(declareVariable(parameters.get(i), true));
                    }
                }
            }
        }

        ollirCode.append(")");
        ollirCode.append(type(returnType));
        ollirCode.append(openBrackets());

        return ollirCode.toString();
    }

    public static String type(Type type) {
        StringBuilder ollir = new StringBuilder();

        if (type.isArray()) ollir.append(".array");

        if ("int".equals(type.getName())) {
            ollir.append(".i32");
        } else if ("boolean".equals(type.getName())) {
            ollir.append(".bool");
        } else if ("void".equals(type.getName())) {
            ollir.append(".V");
        } else {
            ollir.append(".").append(type.getName());
        }

        return ollir.toString();
    }

    public static String variableType(String type) {
        StringBuilder ollirCode = new StringBuilder("");
        if (type.equals("Integer") || type.equals("int")) ollirCode.append(".i32");
        else if (type.equals("Bool") || type.equals("bool") || type.equals("boolean")) ollirCode.append(".bool");
        else {
            ollirCode.append("." + type);
        }

        return ollirCode.toString();
    }

    public static String declareVariable(Symbol variable, Boolean isField) {
        StringBuilder param = new StringBuilder(variable.getName());
        param.append(type(variable.getType()));
        if (!isField) param.append(";\n");
        return param.toString();
    }

    public static String putField(Symbol variable, String newValue) {
        System.out.println("variable (putField): " + variable);
        String typeAcc = type(variable.getType());
        StringBuilder ollirCode = new StringBuilder("putfield(");
        ollirCode.append("this, ");
        ollirCode.append(variable.getName()).append(typeAcc).append(", ");
        ollirCode.append(newValue);
        ollirCode.append(")" + typeAcc);
        ollirCode.append(";\n");

        return ollirCode.toString();
    }

    public static String defaultConstructor(String className) {
        StringBuilder ollirCode = new StringBuilder("\n.construct " + className + "().V ");
        ollirCode.append(OllirTemplates.openBrackets());
        ollirCode.append(OllirTemplates.invokespecial(null, "<init>", new Type("void", false)));
        ollirCode.append(OllirTemplates.closeBrackets());

        return ollirCode.toString();
    }

    public static String invokespecial(String var, String method, Type returnType) {
        return String.format("invokespecial(%s, \"%s\")%s;\n", var != null ? var : "this", method, type(returnType));
    }

    public static String invokestatic(String importStmt) { // methods imported (method, libraries, other classes)

        //return String.format("invokestatic(%s, \"%s\").V;\n", importStmt, method, type(returnType));
        return null;
    }

    public static String localVariableAssignment(Symbol variable, String value) {
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append(variable.getName() + type(variable.getType()));
        ollirCode.append(" :=" + type(variable.getType()) + " ");
        ollirCode.append(value + ";\n");

        return ollirCode.toString();
    }

    public static String parameterAssignment(Symbol variable, String newValue, int paramIndex) {
        // Here iwe need to distinguish between array and other type of variable
        StringBuilder ollirCode = new StringBuilder();
        String varType = type(variable.getType());
        ollirCode.append("$" + paramIndex + "." + variable.getName() + varType);
        ollirCode.append(" :=" + varType + " ");
        ollirCode.append(newValue + varType);
        ollirCode.append(";\n");

        return ollirCode.toString();
    }

    public static String variableAssignment(Symbol variable, String index, String value) { // local variable assignment
        StringBuilder ollirCode = new StringBuilder();
        String varType = type(variable.getType());
        if (variable.getType().isArray()) {
            ollirCode.append(variable.getName() + "[" + index + ".i32]" + varType);
            ollirCode.append(" :=" + varType + " ");
            ollirCode.append(value + ";\n");
        } else {
            ollirCode.append(variable.getName() + varType);
            ollirCode.append(" :=" + varType + " ");
            ollirCode.append(value + ";\n");
        }

        return ollirCode.toString();
    }

    public static String variableAssignment(Symbol variable, String value, int paramIndex) { // method parameter assignment
        StringBuilder ollirCode = new StringBuilder();
        String varType = type(variable.getType());
        ollirCode.append("$" + paramIndex + "." + variable.getName() + varType);
        ollirCode.append(" :=" + varType + " ");
        ollirCode.append(value);
        ollirCode.append(";\n");

        return ollirCode.toString();
    }

    public static String variableAssignment(int tempVariableIndex, Symbol variable, String value) { // class field assignment
        StringBuilder ollirCode = new StringBuilder();
        String varType = type(variable.getType());

        ollirCode.append(putField(variable, value));

        return ollirCode.toString();
    }

    public static String arrayAssignment(Symbol variable, String indexValue, String value) {
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append(variable.getName());
        ollirCode.append("[" + indexValue + "]");
        ollirCode.append(" :=" + variable.getType().getName() + " ");
        ollirCode.append(value + ";");

        return ollirCode.toString();
    }

    public static String returnTemplate(String returnObj) {
        return "ret" + returnObj + ";\n";
    }

    public static String returnTemplate(String type, String returnObj) { // Local Variable
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append("ret" + type + " ");
        ollirCode.append(returnObj);
        ollirCode.append(";\n");

        return ollirCode.toString();
    }

    public static String returnTemplate(Type type, String returnObj, Integer index, String varType) { // MethodParameter or ClassField
        StringBuilder ollirCode = new StringBuilder();

        switch (varType) {
            case "methodParameter":
                ollirCode.append("ret" + type(type) + " ");
                ollirCode.append("$" + index + "." + returnObj + variableType(type.getName()));
                ollirCode.append(";");
                break;
            case "classField":
                String tempVarStr = new String("t" + index);
                ollirCode.append(tempVarStr + variableType(type.getName()) + " ");
                ollirCode.append(" :=" + variableType(type.getName()) + " ");
                ollirCode.append("getField(this, " + returnObj + variableType(type.getName()) + ")" + variableType(type.getName()));
                ollirCode.append("ret" + variableType(type.getName()) + " " + tempVarStr + variableType(type.getName()));
                ollirCode.append(";");
                break;
            default:
                break;
        }

        return ollirCode.toString();
    }

    public static String variableCall(Type type, String returnObj) { // Local Variable
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append(returnObj + variableType(type.getName()));

        return ollirCode.toString();
    }

    public static String variableCall(Type type, String returnObj, Integer index) { // MethodParameter or ClassField
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append("$" + index + "." + returnObj + variableType(type.getName()));

        return ollirCode.toString();
    }

    public static String createOpAssignment(String type, int tempIndex, String returnValueStr) {
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append("t" + tempIndex + type + " :=" + type + " ");
        ollirCode.append(returnValueStr + ";\n");

        return ollirCode.toString();
    }

    public static String createMemberAccess(String tempVar, List<String> parametersTempVariables, String first, String method, String parameters, String currentArithType, String varScope) {
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append(String.join("\n", parametersTempVariables));
        ollirCode.append(tempVar);
        if (varScope.equals("import")) {
            ollirCode.append("invokestatic(" + first + ", ");
        } else {
            ollirCode.append("invokevirtual(" + first + ", ");
        }
        ollirCode.append("\"" + method + "\"");
        if (!parameters.equals("")) {
            ollirCode.append( ", ");
            ollirCode.append(parameters);
        }

        ollirCode.append(")" + currentArithType + ";\n");

        return ollirCode.toString();
    }

    public static String temporaryVariableTemplate(int tempVariableNum, String typeAcc, String rightSide) {
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append("t" + tempVariableNum + typeAcc);
        ollirCode.append(" :=" + typeAcc + " ");
        ollirCode.append(rightSide + ";\n");

        return ollirCode.toString();
    }

    public static String newObjectTemplate(int tempVariableNum, String objClassName) {
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append("t" + tempVariableNum + "." + objClassName);
        ollirCode.append(" :=." + objClassName);
        ollirCode.append(" new(" + objClassName + ")." + objClassName + ";\n");

        return ollirCode.toString();
    }
}
