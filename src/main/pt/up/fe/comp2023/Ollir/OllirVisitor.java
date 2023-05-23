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
    private ExprOllirVisitor exprVisitor;
    private int while_label_sequence;
    private int conditional_label_sequence;

    public OllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
        this.nodesVisited = new ArrayList<>();
        this.exprVisitor = new ExprOllirVisitor(this.symbolTable, this.reports);
        this.while_label_sequence = 0;
        this.conditional_label_sequence = 0;
    }

    @Override
    public void buildVisitor() {
        addVisit("Program", this::dealWithProgramDeclaration); // Program rule
        addVisit("ClassDeclaration", this::dealWithClassDeclaration); // ClassDeclaration rule
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration); // MethodDeclaration rule
        addVisit("LocalVariables", this::dealWithLocalVariables); // LocalVariable rule

        // Statement rules
        addVisit("Brackets", this::dealWithBrackets);
        addVisit("IfConditional", this::dealWithIfConditional);
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

        if (node.getChildren().stream().noneMatch(child -> child.getKind().equals("ReturnObj"))) { // if the method does not return nothing, it means that it's the return type of the method is void (.V)
            ollirCode.append("ret.V;\n");
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
        this.exprVisitor.currentAssignmentType = localVariable.getType();

        if (node.getNumChildren() > 1) { // variable declaration and initialization
            JmmNode exprNode = node.getJmmChild(1);
            String childExpr = (String) this.exprVisitor.visit(exprNode, Collections.singletonList("LOCAL_VARIABLES")).get(0);
            ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
            ollirCode.append(OllirTemplates.localVariableAssignment(localVariable, childExpr));
        }

        this.exprVisitor.resetTempVariables();
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithBrackets(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        for (JmmNode stmtChild : node.getChildren()) {
            ollirCode.append((String) visit(stmtChild, data).get(0));
        }

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithIfConditional(JmmNode node, List<Object> data) { // FINISH!
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        int conditionalCount = ++this.while_label_sequence;

        String loopCondition = (String) this.exprVisitor.visit(node.getJmmChild(0), Collections.singletonList("CONDITIONAL")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();

        boolean elseStmt = node.getAttributes().contains("hasElse");

        ollirCode.append(String.format("if (!" + OllirTemplates.type(this.exprVisitor.currentArithType) + " " + loopCondition + ") " + (elseStmt ? "goto else%d;\n" : "goto endif%d;\n"), conditionalCount));
        List<JmmNode> ifStatements = node.getChildren().subList(1, ( elseStmt? (node.getNumChildren() - 1) : node.getNumChildren()));
        for (JmmNode statementChild : ifStatements) {
            ollirCode.append((String) visit(statementChild, Collections.singletonList("IF")).get(0));
        }
        if (elseStmt) ollirCode.append(String.format("goto endif%d;\n", conditionalCount));

        if (elseStmt) {
            ollirCode.append(String.format("else%d:\n", conditionalCount));
            JmmNode elseNode = node.getJmmChild(node.getNumChildren() - 1);
            ollirCode.append((String) visit(elseNode, Collections.singletonList("ELSE")).get(0));
        }

        ollirCode.append(String.format("endif%d:\n", conditionalCount));

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithLoop(JmmNode node, List<Object> data) { // While loop
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        int loopCount = ++this.while_label_sequence;

        ollirCode.append(String.format("goto Cond%d;\n", loopCount));
        ollirCode.append(String.format("Body%d:\n", loopCount));
            for (JmmNode statementChild : node.getChildren().subList(1, node.getChildren().size())) {
                ollirCode.append((String) visit(statementChild, Collections.singletonList("LOOP")).get(0));
            }
        ollirCode.append(String.format("Cond%d:\n", loopCount));
            String whileCondition = (String) this.exprVisitor.visit(node.getJmmChild(0), Collections.singletonList("LOOP")).get(0);
            ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
            this.exprVisitor.resetTempVariables();
            ollirCode.append(String.format("if (" + whileCondition + ") goto Body%d;\n", loopCount));

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithArrayAssignment(JmmNode node, List<Object> data) { // When the index is an integer number (Integer terminal symbol, it gives me an error)
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName"); // Name of the List
        Pair<String, Symbol> pair = this.symbolTable.variableScope(this.exprVisitor.currentMethod, varName);
        this.exprVisitor.currentAssignmentType = pair.b.getType();

        JmmNode indexNode = node.getChildren().get(0);
        JmmNode valueNode = node.getChildren().get(1);

        String indexOllirCode = (String) this.exprVisitor.visit(indexNode, Collections.singletonList("ARRAY_ASSIGNMENT_INDEX")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();
        String valueOllirCode = (String) this.exprVisitor.visit(valueNode, Collections.singletonList("ARRAY_ASSIGNMENT_VALUE")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();

        Type newVarType = new Type(pair.b.getType().getName(), false);

        switch (pair.a) {
            case "localVariable":
                ollirCode.append(OllirTemplates.variableAssignment(pair.b, indexOllirCode, valueOllirCode, newVarType, false));
                break;
            case "parameterVariable":
                int paramIndex = this.exprVisitor.currentMethod.getParameterIndex(varName);
                String newVarName = "$" + paramIndex + "." + varName;
                Symbol newVariable2 = new Symbol(pair.b.getType(), newVarName);
                ollirCode.append(OllirTemplates.variableAssignment(newVariable2, indexOllirCode, valueOllirCode, newVarType, false));
                break;
            case "fieldVariable":

                //ollirCode.append(OllirTemplates.putField(pair.b, valueOllirCode, newVarType));

                ollirCode.append(OllirTemplates.getField((++this.exprVisitor.tempMethodParamNum), pair.b));
                String tempVar = "t" + this.exprVisitor.tempMethodParamNum;
                Type newType = new Type(pair.b.getType().getName(), false);
                Symbol newSymbol = new Symbol(pair.b.getType(), tempVar);
                ollirCode.append(OllirTemplates.variableAssignment(newSymbol, indexOllirCode, valueOllirCode, newType, false));
        }

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithAssignment(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName");

        Pair<String, Symbol> pair = this.symbolTable.variableScope(this.exprVisitor.currentMethod, varName);
        Symbol variable = pair.b;
        this.exprVisitor.currentAssignmentType = variable.getType();

        JmmNode valueNode = node.getJmmChild(0);
        String valueOllirCode = (String) this.exprVisitor.visit(valueNode, Collections.singletonList("ASSIGNMENT")).get(0);
        ollirCode.append(String.join("", this.exprVisitor.tempVariablesOllirCode));
        this.exprVisitor.resetTempVariables();

        System.out.println("valueNode: " + valueNode);
        System.out.println("valueOllirCode: " + valueOllirCode);

        Type type;
        boolean newArrayObjectBool = false;
        if (valueNode.getKind().equals("NewArrayObject")) {
            newArrayObjectBool = true;
            type = variable.getType();
        } else {
            newArrayObjectBool = false;
            type = new Type(variable.getType().getName(), false);
        }

        switch (pair.a) {
            case "localVariable":
                ollirCode.append(OllirTemplates.variableAssignment(variable, "", valueOllirCode, type, newArrayObjectBool));
                break;
            case "parameterVariable":
                int paramIndex = this.exprVisitor.currentMethod.getParameterIndex(varName);
                Symbol newVariable = new Symbol(variable.getType(), "$" + paramIndex + "." + variable.getName());
                ollirCode.append(OllirTemplates.variableAssignment(newVariable, "", valueOllirCode, type, newArrayObjectBool));
                break;
            case "fieldVariable":
                ollirCode.append("t").append(++this.exprVisitor.tempMethodParamNum).append(OllirTemplates.type(this.exprVisitor.currentAssignmentType)).append(" :=").append(OllirTemplates.type(variable.getType())).append(" ").append(valueOllirCode).append(";\n");
                ollirCode.append(OllirTemplates.putField(variable, "t" + this.exprVisitor.tempMethodParamNum + OllirTemplates.type(variable.getType()), variable.getType()));
                //ollirCode.append(OllirTemplates.putField(variable, valueOllirCode));
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
