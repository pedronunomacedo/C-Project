package pt.up.fe.comp2023.Ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
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
        ollir.append(name).append(extendedClass + " ").append(openBrackets());
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

    public static String methodTemplate(String name, List<Symbol> parameters, Type returnType) {
        StringBuilder ollirCode = new StringBuilder(".method public ");
        boolean isMain = name.equals("main");

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
        String typeAcc = type(variable.getType());
        StringBuilder ollirCode = new StringBuilder("putfield(");
        ollirCode.append("this, ");
        ollirCode.append(variable.getName()).append(typeAcc).append(", ");
        ollirCode.append(newValue);
        ollirCode.append(")" + ".V");
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

    public static String invokestatic(String importStmt, String funcName, List<String> parameters, String retAcc) { // methods imported (method, libraries, other classes)
        if (parameters.isEmpty()) {
            return String.format("invokestatic(%s, \"%s\")" + retAcc + ";\n", importStmt, funcName);
        }
        return String.format("invokestatic(%s, \"%s\", %s)" + retAcc + ";\n", importStmt, funcName, String.join(", ", parameters));
    }

    public static String invokevirtual(String objName, String funcName, List<String> parameters, String retAcc) {
        if (parameters.isEmpty()) {
            return String.format("invokevirtual(%s, \"%s\")" + retAcc + ";\n", objName, funcName);
        }
        return String.format("invokevirtual(%s, \"%s\", %s)" + retAcc + ";\n", objName, funcName, String.join(", ", parameters));
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

    public static String variableAssignment(Symbol variable, String index, String value, Type variableType, boolean newArrayObjectBool) { // local variable assignment
        StringBuilder ollirCode = new StringBuilder();
        String varTypeAcc = type(variableType);
        if (variable.getType().isArray()) {
            ollirCode.append(variable.getName() + (newArrayObjectBool ? "" : "[" + index + "]") + varTypeAcc);
            ollirCode.append(" :=" + varTypeAcc + " ");
            ollirCode.append(value + ";\n");
        } else {
            ollirCode.append(variable.getName() + varTypeAcc);
            ollirCode.append(" :=" + varTypeAcc + " ");
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

    public static String returnTemplate(String expr, Type retType) { // Local Variable
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append("ret" + type(retType) + " ");
        ollirCode.append(expr);
        ollirCode.append(";\n");

        return ollirCode.toString();
    }

    public static String variableCall(Symbol variable) { // Local Variable
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append(variable.getName() + variableType(variable.getType().getName()));

        return ollirCode.toString();
    }

    public static String variableCall(Symbol variable, Integer index) { // MethodParameter or ClassField
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append("$" + index + "." + variable.getName() + type(variable.getType()));

        return ollirCode.toString();
    }

    public static String temporaryVariableTemplate(int tempVariableNum, String typeAcc, String rightSide) {
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append("t" + tempVariableNum + typeAcc);
        ollirCode.append(" :=" + typeAcc + " ");
        ollirCode.append(rightSide + ";\n");

        return ollirCode.toString();
    }

    public static String newObjectTemplate(String tempVar, String objClassName) {
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append(tempVar);
        ollirCode.append(" :=." + objClassName);
        ollirCode.append(" new(" + objClassName + ")." + objClassName + ";\n");
        ollirCode.append("invokespecial(" + tempVar + ", \"<init>\").V;\n");

        return ollirCode.toString();
    }

    public static String getField(int tempVariableNum, Symbol field) {
        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append("t" + tempVariableNum + type(field.getType()));
        ollirCode.append(" :=" + type(field.getType()) + " ");
        ollirCode.append("getfield(this, ");
        ollirCode.append(field.getName() + type(field.getType()));
        ollirCode.append(")" + type(field.getType()) + ";\n");

        return ollirCode.toString();
    }
}
