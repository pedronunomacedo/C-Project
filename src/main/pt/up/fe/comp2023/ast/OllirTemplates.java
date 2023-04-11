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
        return "\n}\n\n";
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
            ollirCode.append(declareVariable(parameters.get(0)));
        } else {
            // method parameters/arguments
            ollirCode.append(declareVariable(parameters.get(0)));
            if (parameters.size() > 1) {
                for (int i = 1; i < parameters.size(); i++) {
                    ollirCode.append(", ").append(declareVariable(parameters.get(i)));
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

    public static String declareVariable(Symbol variable) {
        StringBuilder param = new StringBuilder(variable.getName());
        param.append(type(variable.getType()));

        return param.toString();
    }

    public static String putField(Symbol variable, String newValue) {
        StringBuilder ollirCode = new StringBuilder("putfield(");
        ollirCode.append("this, ");
        ollirCode.append(variable.getName() + type(variable.getType()));

        return ollirCode.toString();
    }

    public static String putField(Symbol variable, String newValue, Integer parameterIndex, String parameterName) {
        StringBuilder ollirCode = new StringBuilder("putfield()");
        ollirCode.append("this, ");
        ollirCode.append(variable.getName() + type(variable.getType()));
        ollirCode.append("$" + parameterIndex + "." + parameterName + "." + type(variable.getType()));

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
        return String.format("invokespecial(%s, \"%s\")%s;", var != null ? var : "this", method, type(returnType));
    }
}
