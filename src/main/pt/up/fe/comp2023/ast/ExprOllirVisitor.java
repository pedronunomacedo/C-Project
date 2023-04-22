package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExprOllirVisitor extends AJmmVisitor<List<Object>, List<Object>> {
    private JmmSymbolTable symbolTable;
    private List<Report> reports;
    private ArrayList<JmmNode> nodesVisited;
    public int tempMethodParamNum;
    private Type currentArithType;
    private String scope;
    public boolean dealWithReturnType;
    public String currentArithTypeStr = "";
    public JmmMethod currentMethod;

    public ExprOllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
        this.nodesVisited = new ArrayList<>();
        this.scope = "EXPRESSION";
        this.tempMethodParamNum = 0;
        this.dealWithReturnType = false;
    }

    @Override
    public void buildVisitor() {
        // Expression rules
        /* addVisit("Array", this::dealWithArrayDeclaration); addVisit("Lenght", this::dealWithExpression); addVisit("UnaryOp", this::dealWithExpression); addVisit("NewArrayObject", this::dealWithExpression); */
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("MemberAccess", this::dealWithMemberAccess);
        addVisit("BinaryOp", this::dealWithBinaryOp); // creates and returns the OLLIR code with the temporary variables
        addVisit("ExprParentheses", this::dealWithExprParentheses); // (returns the OLLIR code, if BinaryOp is the father) or (returns the parentheses and the child code)
        addVisit("Integer", this::dealWithSingleExpression); // terminal nodes
        addVisit("Bool", this::dealWithSingleExpression); // terminal nodes
        addVisit("SelfCall", this::dealWithSingleExpression); // terminal nodes
        addVisit("Identifier", this::dealWithSingleExpression); // terminal nodes
    }

    public List<Object> dealWithMemberAccess(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        JmmNode firstChild = node.getChildren().get(0);
        String memberAccessed = node.get("id");
        List<String> parameters = new ArrayList<>();
        String parametersString;
        List<String> parametersTempVariables = new ArrayList<>();
        String firstChildStr = (String) visit(firstChild, Collections.singletonList("MemberAccess")).get(0);

        for (int i = 1; i < node.getChildren().size(); i++) {
            String paramOllirCode = (String) visit(node.getJmmChild(i), Collections.singletonList("MemberAccess")).get(0);
            if ((data.get(0).equals("ArrayAssignment") || data.get(0).equals("Assignment") || data.get(0).equals("Expr") || data.get(0).equals("ReturnObj")) && node.getJmmChild(i).getNumChildren() > 0) { // complex parameters
                parameters.add("t" + this.tempMethodParamNum + this.currentArithTypeStr);
                parametersTempVariables.add(paramOllirCode);
            } else {
                parameters.add(paramOllirCode);
            }
        }

        parametersString = String.join(", ", parameters);
        ollirCode.append(String.join("\n", parametersTempVariables));
        if (firstChild.getChildren().size() == 0 && this.symbolTable.getImports().contains(firstChild.get("val"))) { // use invokestatic
            String tempVarSent = new String("");
            if (data.get(0).equals("BinaryOp")) { // save the result of invokestatic to a temporary variable
                tempVarSent = "t" + (this.tempMethodParamNum++) + this.currentArithTypeStr + " :=" + this.currentArithTypeStr + " ";
            }
            if (data.get(0).equals("LocalVariable") || data.get(0).equals("ReturnObj")) {
                ollirCode.append(OllirTemplates.createMemberAccess(tempVarSent, parametersTempVariables, firstChildStr, memberAccessed, parametersString, this.currentArithTypeStr, "import"));
            } else {
                ollirCode.append(OllirTemplates.createMemberAccess("", new ArrayList<String>(), firstChildStr, memberAccessed, parametersString, this.currentArithTypeStr, "import"));
            }
        } else {
            String tempVarSent = "t" + this.tempMethodParamNum + this.currentArithTypeStr + " :=" + this.currentArithTypeStr + " ";
            if (data.get(0).equals("LocalVariable")) {
                ollirCode.append(OllirTemplates.createMemberAccess(tempVarSent, parametersTempVariables, firstChildStr, memberAccessed, parametersString, this.currentArithTypeStr, ""));
            } else {
                ollirCode.append(OllirTemplates.createMemberAccess("", new ArrayList<>(), firstChildStr, memberAccessed, parametersString, this.currentArithTypeStr, ""));
            }
        }

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithBinaryOp(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String binaryOp = node.get("op");
        JmmNode leftExpr = node.getChildren().get(0);
        JmmNode rightExpr = node.getChildren().get(1);

        String leftExpressOllirStr = (String) visit(leftExpr, Collections.singletonList("BinaryOp")).get(0);
        String rightExpressOllirStr = (String) visit(rightExpr, Collections.singletonList("BinaryOp")).get(0);

        boolean leftIsTerminalSymbol = (leftExpr.getKind().equals("Integer") || leftExpr.getKind().equals("Identifier"));
        boolean rightIsTerminalSymbol = (rightExpr.getKind().equals("Integer") || rightExpr.getKind().equals("Identifier"));
        if (leftIsTerminalSymbol && rightIsTerminalSymbol) { // terminal nodes
            System.out.println("data.get(0) = " + data.get(0));
            if (data.get(0).equals("LocalVariable")) {
                ollirCode.append("t" + (++this.tempMethodParamNum) + this.currentArithTypeStr + " :=" + this.currentArithTypeStr + " " + leftExpressOllirStr + " " + binaryOp + this.currentArithTypeStr + " " + rightExpressOllirStr + ";\n");
            } else {
                String rightSide = leftExpressOllirStr + " " + binaryOp + this.currentArithTypeStr + " " + rightExpressOllirStr;
                ollirCode.append(OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), this.currentArithTypeStr, rightSide));
            }
        } else if (!leftIsTerminalSymbol && rightIsTerminalSymbol) {
            ollirCode.append(leftExpressOllirStr);
            String rightSide = ("t" + this.tempMethodParamNum + this.currentArithTypeStr) + " " + (binaryOp + this.currentArithTypeStr) + " " + rightExpressOllirStr;
            ollirCode.append(OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), this.currentArithTypeStr, rightSide));
        } else if (leftIsTerminalSymbol && !rightIsTerminalSymbol) {
            ollirCode.append(rightExpressOllirStr);
            String rightSide = leftExpressOllirStr + " " + (binaryOp + this.currentArithTypeStr) + " " + ("t" + this.tempMethodParamNum + this.currentArithTypeStr);
            ollirCode.append(OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), this.currentArithTypeStr, rightSide));
        } else { // both sides are not terminal symbols
            ollirCode.append(leftExpr);
            ollirCode.append(rightExpr);
            int temporaryVar1 = this.tempMethodParamNum - 1; // left temporary variable
            int temporaryVar2 = this.tempMethodParamNum;
            String rightSide = ("t" + temporaryVar1 + this.currentArithTypeStr) + " " + (binaryOp + this.currentArithTypeStr) + " " + ("t" + temporaryVar2 + this.currentArithTypeStr);
            ollirCode.append(OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), this.currentArithTypeStr, rightSide));
        }

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithExprParentheses(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append((data.get(0).equals("BinaryOp") || data.get(0).equals("LocalVariable")) ? "" : ollirCode.append("("));
        String expressionOllirCode = (String) visit(node.getChildren().get(0), data).get(0);
        ollirCode.append(expressionOllirCode);
        ollirCode.append((data.get(0).equals("BinaryOp") || data.get(0).equals("LocalVariable")) ? "" : (ollirCode.append(")") + this.currentArithTypeStr));

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithSingleExpression(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String returnTypeObj = new String();
        String returnVal = node.get("val");

        switch (node.getKind()) {
            case "Integer":
                ollirCode.append(returnVal + ".i32");
                this.currentArithTypeStr = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? ".i32" : "";
                break;
            case "Bool":
                ollirCode.append(returnVal + ".bool");
                this.currentArithTypeStr = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? ".bool" : "";
                break;
            case "SelfCall":
                ollirCode.append(returnVal);
                break;
            case "Identifier":
                // Check if returnVal corresponds to a local variable, or to a method parameter or to a class field
                Symbol localVarSymbol = this.currentMethod.getLocalVariable(returnVal);
                Symbol parameterSymbol = this.currentMethod.getParameter(returnVal);
                Symbol classField = this.symbolTable.getField(returnVal);

                if (localVarSymbol != null) { // Local variable
                    ollirCode.append(OllirTemplates.variableCall(localVarSymbol.getType(), returnVal));
                    returnTypeObj = OllirTemplates.variableType(localVarSymbol.getType().getName());
                } else if (parameterSymbol != null) {  // Method parameter
                    this.tempMethodParamNum++;
                    int paramIndex = this.currentMethod.getParameterIndex(returnVal);
                    ollirCode.append(OllirTemplates.variableCall(parameterSymbol.getType(), returnVal, paramIndex));
                    returnTypeObj = OllirTemplates.variableType(parameterSymbol.getType().getName());
                } else if (classField != null) { // Class field
                    this.tempMethodParamNum++;
                    ollirCode.append(OllirTemplates.variableCall(classField.getType(), returnVal));
                    returnTypeObj = OllirTemplates.variableType(classField.getType().getName());
                } else {
                    ollirCode.append(returnVal);
                }
                this.currentArithTypeStr = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? returnTypeObj : "";
        }

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithNewObject(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        String objClassName = node.get("val");
        ollirCode.append(OllirTemplates.newObjectTemplate((++this.tempMethodParamNum), objClassName));
        this.tempMethodParamNum++;

        return Collections.singletonList(ollirCode.toString());
    }
}
