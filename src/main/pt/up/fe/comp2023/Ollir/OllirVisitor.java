package pt.up.fe.comp2023.Ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.ast.JmmMethod;
import pt.up.fe.comp2023.ast.JmmSymbolTable;

import java.util.*;

public class OllirVisitor extends AJmmVisitor<List<Object>, List<Object>> {
    private final JmmSymbolTable symbolTable;
    private final List<Report> reports;
    List<JmmNode> nodesVisited;
    private String scope;

    String currentArithType = "";

    private ExprOllirVisitor exprVisitor;

    public OllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
        this.nodesVisited = new ArrayList<>();
        this.exprVisitor = new ExprOllirVisitor(this.symbolTable, this.reports);
    }

    @Override
    public void buildVisitor() {
        addVisit("Program", this::dealWithProgramDeclaration); // Program rule
        addVisit("ClassDeclaration", this::dealWithClassDeclaration); // ClassDeclaration rule
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration); // MethodDeclaration rule
        addVisit("LocalVariables", this::dealWithLocalVariables); // LocalVariable rule

        // Statement rules
        /* addVisit("Brackets", this::defaultVisit); */
        addVisit("Conditional", this::dealWithConditional);
        addVisit("Loop", this::dealWithLoop);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Expr", this::dealWithExpression);

        setDefaultVisit(this::defaultVisit);
    }

    private String fillImports() {
        StringBuilder ollirCode = new StringBuilder();
        for (String imp : symbolTable.getImports()) ollirCode.append("import ").append(imp).append(";\n");

        return ollirCode.toString();
    }

    private List<Object> dealWithProgramDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);

        StringBuilder ollirCode = new StringBuilder();
        ollirCode.append(this.fillImports());
        JmmNode classNode = node.getJmmChild(node.getNumChildren() - 1);
        ollirCode.append((String) visit(classNode, Collections.singletonList("PROGRAM")).get(0));

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithClassDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        this.scope = "CLASS";
        StringBuilder ollirCode = new StringBuilder();

        if (this.symbolTable.getSuper() == null) { // No extended class
            ollirCode.append(OllirTemplates.classTemplate(this.symbolTable.getClassName(), ""));
        } else {
            ollirCode.append(OllirTemplates.classTemplate(this.symbolTable.getClassName(), " extends " + this.symbolTable.getSuper()));
        }

        // Deal with class fields
        for (Symbol field : this.symbolTable.getFields()) ollirCode.append(OllirTemplates.fieldTemplate(field));

        // Deal with class default constructor
        if (this.symbolTable.getMethod(this.symbolTable.getClassName(), new ArrayList<>(), new Type("void", false)) == null) ollirCode.append(OllirTemplates.defaultConstructor(this.symbolTable.getClassName()));

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("MethodDeclarationOther") || child.getKind().equals("MethodDeclarationMain")) {
                String methodOllirCode = (String) visit(child, data).get(0);
                ollirCode.append(methodOllirCode);
            }
        }

        ollirCode.append(OllirTemplates.closeBrackets());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithMethodDeclaration(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        this.scope = "METHOD";
        StringBuilder ollirCode = new StringBuilder();
        String methodName = node.get("methodName");
        this.exprVisitor.currentMethod = this.symbolTable.getMethod(methodName);

        ollirCode.append(OllirTemplates.methodTemplate(methodName, this.exprVisitor.currentMethod.getParameters(), this.exprVisitor.currentMethod.getReturnType()));

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ReturnObj")) {
                String childExpr = (String) this.exprVisitor.visit(child.getJmmChild(0), Collections.singletonList("RETURN")).get(0);
                ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode)); // append the temporary variables ollir code
                ollirCode.append(OllirTemplates.returnTemplate(childExpr, this.exprVisitor.currentArithType)); // Or chidlExpr = terminalSymbol OR childExpr = tempVariable
                this.exprVisitor.resetTempVariables();
            } else if (Arrays.asList("returnType", "classParameters").contains(child.getKind())) {
                // ignore
            } else { // statements and localVariables
                ollirCode.append((String) visit(child, Collections.singletonList("METHOD")).get(0));
            }
        }
        ollirCode.append(OllirTemplates.closeBrackets());

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithLocalVariables(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        String varName = node.get("varName");
        Symbol localVariable = this.exprVisitor.currentMethod.getLocalVariable(varName);

        if (node.getNumChildren() > 1) { // variable declaration and initialization
            JmmNode exprNode = node.getJmmChild(1);
            String childExpr = (String) this.exprVisitor.visit(exprNode, Collections.singletonList("LOCAL_VARIABLES")).get(0);
            ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
            ollirCode.append(OllirTemplates.localVariableAssignment(localVariable, childExpr));
        }

        this.exprVisitor.resetTempVariables();
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithConditional(JmmNode node, List<Object> data) { // FINISH!
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithLoop(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithArrayAssignment(JmmNode node, List<Object> data) { // When the index is an integer number (Integer terminal symbol, it gives me an error)
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName"); // Name of the List
        Pair<String, Symbol> variable = this.symbolTable.variableScope(this.exprVisitor.currentMethod, varName);

        JmmNode indexNode = node.getChildren().get(0);
        JmmNode valueNode = node.getChildren().get(1);

        String indexOllirCode = (String) this.exprVisitor.visit(indexNode, Collections.singletonList("ASSIGNMENT")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();
        String valueOllirCode = (String) this.exprVisitor.visit(valueNode, Collections.singletonList("ASSIGNMENT")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();
        ollirCode.append(OllirTemplates.variableAssignment(variable.b, indexOllirCode, valueOllirCode));

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithAssignment(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName");

        Pair<String, Symbol> pair = this.symbolTable.variableScope(this.exprVisitor.currentMethod, varName);
        Symbol variable = pair.b;

        JmmNode valueNode = node.getJmmChild(0);
        String valueOllirCode = (String) this.exprVisitor.visit(valueNode, Collections.singletonList("ASSIGNMENT")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();

        switch (pair.a) {
            case "localVariable":
                ollirCode.append(OllirTemplates.variableAssignment(variable, "", valueOllirCode));
                break;
            case "parameterVariable":
                int paramIndex = this.exprVisitor.currentMethod.getParameterIndex(varName);
                Symbol symbol = new Symbol(variable.getType(), "$" + paramIndex + "." + variable.getName());
                ollirCode.append(OllirTemplates.variableAssignment(variable, "", valueOllirCode));
                break;
            case "fieldVariable":
                ollirCode.append(OllirTemplates.putField(variable, valueOllirCode));
                break;
        }


        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithExpression(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        this.exprVisitor.visit(node.getChildren().get(0), Collections.singletonList("Expr")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        return Collections.singletonList("");
    }
}
