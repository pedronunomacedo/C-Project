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
        return ".field public " + field.getName() + ";\n";
    }

    public static String methodTemplate(String name, List<String> parameters, String returnType, boolean isMain) {
        StringBuilder ollir = new StringBuilder(".method public ");

        if (isMain) ollir.append("static ");

        // parameters
        ollir.append(name).append("(");
        ollir.append(String.join(", ", parameters)); // method parameters/arguments
        ollir.append(")");

        // return type
        ollir.append(returnType);

        ollir.append(openBrackets());

        return ollir.toString();
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
}
