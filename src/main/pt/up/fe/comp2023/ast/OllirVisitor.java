package pt.up.fe.comp2023.ast;

import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.management.ObjectName;
import java.util.*;

public class OllirVisitor extends PreorderJmmVisitor<List<Object>, List<Object>> {
    private final JmmSymbolTable symbolTable;
    private final List<Report> reports;
    List<JmmNode> nodesVisited;
    private String scope;
    private JmmMethod currentMethod;

    public OllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
        this.nodesVisited = new ArrayList<>();
    }

    @Override
    public void buildVisitor() {
        addVisit("Program", this::dealWithProgramDeclaration);
        addVisit("ImportDeclaration", this::dealWithImportDeclaration);
        addVisit("SubImportDeclaration", this::dealWithSubImportDeclaration);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("ExtendedClass", this::dealWithExtendedClassDeclaration);
        addVisit("VariableDeclaration", this::dealWithVariableDeclaration);

        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassParameters", this::dealWithClassParameters);
        addVisit("LocalVariables", this::dealWithLocalVariables);

        addVisit("IntegerArrayType", this::dealWithVariableTypes);
        addVisit("IntegerType", this::dealWithVariableTypes);
        addVisit("BooleanType", this::dealWithVariableTypes);
        addVisit("StringType", this::dealWithVariableTypes);
        addVisit("VoidType", this::dealWithVariableTypes);
        addVisit("IdType", this::dealWithVariableTypes);
    }

    private List<Object> dealWithProgramDeclaration(JmmNode node, List<Object> data) {
        // Check if node was already visited
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithProgramDeclaration() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();
        String result = new String();

        for (JmmNode child : node.getChildren()) {
            result += (String) visit(child).get(0);
        }

        ollirCode.append(result);

        System.out.println("ollirCode(dealWithProgramDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithImportDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithImportDeclaration() function! (" + node + ")");

        String importName = node.get("id");
        StringBuilder ollirCode = new StringBuilder();
        String subImportsCode = "";

        for (JmmNode childSubImport : node.getChildren()) {
            subImportsCode += (String) visit(childSubImport).get(0);
        }

        ollirCode.append(OllirTemplates.importTemplate(importName, subImportsCode));

        System.out.println("ollirCode(dealWithImportDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithSubImportDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithSubImportDeclaration() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append(OllirTemplates.subImportTemplate(node.get("id")));

        System.out.println("ollirCode(dealWithSubImportDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithClassDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        this.scope = "CLASS";
        System.out.println("-> In dealWithClassDeclaration() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();
        String ollirCodeExtendedClass = new String();

        // Add class name and extended class name to OLLIR code
        if (this.symbolTable.getSuper() == null) {
            ollirCode.append(OllirTemplates.classTemplate(this.symbolTable.getClassName(), ""));
        }

        // Deal with class fields
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ExtendedClass")) {
                ollirCodeExtendedClass = (String) visit(child, Collections.singletonList("METHOD")).get(0);
                ollirCode.append(OllirTemplates.classTemplate(this.symbolTable.getClassName(), ollirCodeExtendedClass));
            } else if (child.getKind().equals("VariableDeclaration")) {
                String ollirFieldCode = (String) visit(child, Collections.singletonList("METHOD")).get(0);
                ollirCode.append(ollirFieldCode);
            }
        }
        ollirCode.append(OllirTemplates.closeBrackets());

        System.out.println("ollirCode(dealWithClassDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithExtendedClassDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithExtendedClassDeclaration() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();
        String extendedName = node.get("extendedClassName");

        ollirCode.append(OllirTemplates.extendedClassTemplate(extendedName));

        System.out.println("ollirCode(dealWithExtendedClassDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithLocalVariables(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithLocalVariables() function! (" + node + ")");
        System.out.println("node.getChildren() = " + node.getChildren());
        System.out.println("this.scope = " + this.scope);

        if (this.scope.equals("CLASS")) {
            System.out.println("node.getChildren() = " + node.getChildren());

            
        }

        return Collections.singletonList(new StringBuilder().toString());
    }

    private List<Object> dealWithVariableDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithVariableDeclaration() function! (" + node + ")");

        if (this.scope.equals("CLASS")) {
            Symbol field = this.symbolTable.getField(node.get("varName"));

            for (JmmNode child : node.getChildren()) { // Visit the type of the variable
                String childTypeString = (String) visit(child).get(0);
                System.out.println("-> -> childTypeString: " + childTypeString);
            }

            System.out.println("ollirCode(dealWithVariableDeclaration): " + OllirTemplates.fieldTemplate(field).toString());
            return Collections.singletonList(OllirTemplates.fieldTemplate(field).toString());
        }

        return Collections.singletonList("DEFAULT_VISIT");
    }

    private List<Object> dealWithMethodDeclaration(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithMethodDeclaration() function! (" + node + ")");

        this.scope = "METHOD";
        StringBuilder ollirCode = new StringBuilder();

        String nodeKind = node.getKind();

        if (nodeKind.equals("MethodDeclarationMain")) {
            this.scope = "MAIN";
            try {
                List<Type> parameters = Collections.singletonList(new Type("String", true));
                Type returnType = new Type("void", false);
                this.currentMethod = this.symbolTable.getMethod("main", parameters, returnType);
            } catch (Exception e) { // if no method is found
                this.currentMethod = null;
                e.printStackTrace();
            }

            ollirCode = new StringBuilder(OllirTemplates.methodTemplate(
                            "main",
                            currentMethod.transformParametersToOllir(),
                            OllirTemplates.type(currentMethod.getReturnType()),
                            true));

            List<String> methodBody = new ArrayList<>();

            for (JmmNode child : node.getChildren()) {
                String ollirChildCode = (String) visit(child, Collections.singletonList("METHOD")).get(0);

                if (ollirChildCode != null) {
                    if (ollirChildCode.equals("")) continue;
                    methodBody.add(ollirChildCode);
                }
            }

            ollirCode.append(String.join("\n", methodBody));
            ollirCode.append("\nret.V"); // Adds the "return" keyword and specifies that the return type of the "MAIN" method is "void" (.V)
            ollirCode.append(OllirTemplates.closeBrackets());

        } else { // MethodDeclarationOther
            this.scope = "METHOD";
            System.out.println("node.getChildren() = " + node.getChildren());
            String methodName = node.get("methodName");
            Type returnType = null;
            String parametersString = "";
            // Visit the children of the node
            for (JmmNode child: node.getChildren()) {
                if (child.getKind().equals("ReturnType")) {
                    returnType = JmmSymbolTable.getType(child.getChildren().get(0), "typeName");
                } else if (child.getKind().equals("ReturnObj")) {
                    continue; // ignore
                } else if (child.getKind().equals("ClassParameters")){ // ClassParameters
                    String ollirChild = (String) visit(child, Collections.singletonList("METHOD")).get(0);
                    if (parametersString.isEmpty()) {
                        parametersString += ollirChild;
                    } else {
                        parametersString += ", " + ollirChild;
                    }
                } else { // Local Variables
                    String localVariableOllirCode = (String) visit(child, Collections.singletonList("METHOD")).get(0);
                    System.out.println("localVariableOllirCode = " + localVariableOllirCode);
                }
            }

            try {
                List<Type> parameters = Collections.singletonList(new Type("String", true));
                this.currentMethod = this.symbolTable.getMethod("main", parameters, returnType);
            } catch (Exception e) { // if no method is found
                this.currentMethod = null;
                e.printStackTrace();
            }
        }

        System.out.println("ollirCode(dealWithMethodDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithClassParameters(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithClassParameters() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        if (this.scope.equals("METHOD")) {

        }

        System.out.println("ollirCode(dealWithClassParameters): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithVariableTypes(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithVariableTypes() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();
        if (node.getKind().equals("IntegerArrayType")) {
            ollirCode.append(".array.i32");
        } else if (node.getKind().equals("IntegerType")) {
            ollirCode.append(".i32");
        } else if (node.getKind().equals("BooleanType")) {
            ollirCode.append(".bool");
        } else if (node.getKind().equals("StringType")) {
            ollirCode.append(".String");
        } else if (node.getKind().equals("VoidType")) {
            ollirCode.append(".V");
        } else if (node.getKind().equals("IdType")) {
            if (this.symbolTable.getField(node.get("typeName")) == null) {
                throw new NullPointerException("Variable " + node.get("typeName") + " not found!");
            }
            ollirCode.append(this.symbolTable.getField(node.get("typeName")));
        }

        System.out.println("ollirCode(dealWithVariableTypes): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

}
