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

        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassParameters", this::dealWithClassParameters);
        addVisit("LocalVariables", this::dealWithLocalVariables);

        setDefaultVisit(this::defaultVisit);
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

        // Add class name and extended class name to OLLIR code
        if (this.symbolTable.getSuper() == null) {
            ollirCode.append(OllirTemplates.classTemplate(this.symbolTable.getClassName(), ""));
        }

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ExtendedClass")) {
                String ollirCodeExtendedClass = (String) visit(child, Collections.singletonList("METHOD")).get(0);
                ollirCode.append(OllirTemplates.classTemplate(this.symbolTable.getClassName(), ollirCodeExtendedClass));

                // Deal with class fields
                for (Symbol field : this.symbolTable.getFields()) {
                    ollirCode.append(OllirTemplates.fieldTemplate(field));
                }

                // Deal with class default constructor
                if (this.symbolTable.getMethod(this.symbolTable.getClassName(), new ArrayList<>(), new Type("void", false)) == null) {
                    ollirCode.append(OllirTemplates.defaultConstructor(this.symbolTable.getClassName()));
                }

            } else if (child.getKind().equals("MethodDeclarationOther")) {
                String methodOllirCode = (String) visit(child).get(0);
                ollirCode.append(methodOllirCode);
            } else if (child.getKind().equals("MethodDeclarationMain")) {
                String methodMainOllirCode = (String) visit(child).get(0);
                ollirCode.append(methodMainOllirCode);
            } else {
                visit(child);
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

    public List<Object> dealWithLocalVariables(JmmNode node, List<Object> data) throws NoSuchFieldException {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName");
        if (this.scope.equals("METHOD")) {
            if (!node.getAttributes().contains("val")) { // variable declaration
                String varDeclarOllirCode = OllirTemplates.declareVariable(this.currentMethod.getLocalVariable(varName));
                ollirCode.append(varDeclarOllirCode);
            } else { // variable assignment
                String varValue = node.get("val");
                // Make lookup for finding the variable (Search on the localMethodVariables and the methodParameters)
                Symbol localVariable = this.currentMethod.getLocalVariable(varName);
                Symbol classField = this.symbolTable.getField(varName);
                Boolean methodParamBool = this.currentMethod.getParametersNames().contains(varName);
                // lookup
                if (localVariable == null && !methodParamBool) { // class field
                    // Update variable value
                }
            }
        }

        return Collections.singletonList(new StringBuilder().toString());
    }

    private List<Object> dealWithMethodDeclaration(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithMethodDeclaration() function! (" + node + ")");

        this.scope = "METHOD";
        StringBuilder ollirCode = new StringBuilder();

        String nodeKind = node.getKind();

        if (nodeKind.equals("MethodDeclarationMain")) {
            // this.scope = "MAIN";

            try {
                Type type = new Type("String", true);
                Symbol symbol = new Symbol(type, "args");
                List<Symbol> parameters = Collections.singletonList(symbol);
                Type returnType = new Type("void", false);
                if ((this.currentMethod = this.symbolTable.getMethod("main", parameters, returnType)) == null) {
                    throw new NoSuchMethodError();
                };
            } catch (Exception e) { // if no method is found
                this.currentMethod = null;
                e.printStackTrace();
            }

            ollirCode = new StringBuilder(OllirTemplates.methodTemplate(
                    "main",
                    currentMethod.transformParametersToOllir(),
                    currentMethod.getReturnType(),
                    true));

            List<String> methodBody = new ArrayList<>();
            for (JmmNode child : node.getChildren()) {
                if (Arrays.asList("IntegerArrayType", "IntegerType", "BooleanType", "StringType", "VoidType", "IdType").contains(child.getKind())) {
                    visit(child);
                }
            }

            ollirCode.append(String.join("\n", methodBody));
            ollirCode.append(OllirTemplates.closeBrackets());

        } else { // MethodDeclarationOther
            // this.scope = "METHOD";

            String methodName = node.get("methodName");
            Type returnType = this.symbolTable.getReturnType(methodName);
            List<String> parameters = this.symbolTable.getMethod(methodName).getParametersNames();
            String parametersString = "";

            try {
                this.currentMethod = this.symbolTable.getMethod(methodName);
            } catch (Exception e) { // if no method is found
                this.currentMethod = null;
                e.printStackTrace();
            }

            ollirCode.append(OllirTemplates.methodTemplate(methodName, parameters, returnType, false));

            // Visit the children of the node
            for (JmmNode child: node.getChildren()) {
                if (child.getKind().equals("ReturnType") || child.getKind().equals("ClassParameters")) {
                    visit(child); // ignore the child
                } else if (child.getKind().equals("ReturnObj")) {
                    continue; // ignore
                } else { // Local Variables
                    String localVariableOllirCode = (String) visit(child, Collections.singletonList("METHOD")).get(0);
                    ollirCode.append(localVariableOllirCode);
                }
            }

            ollirCode.append(OllirTemplates.closeBrackets());
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

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        return Collections.emptyList();
    }
}
