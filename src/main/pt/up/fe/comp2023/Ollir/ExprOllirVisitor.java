package pt.up.fe.comp2023.Ollir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.ast.JmmMethod;
import pt.up.fe.comp2023.ast.JmmSymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExprOllirVisitor extends AJmmVisitor<List<Object>, List<Object>> {
    private JmmSymbolTable symbolTable;
    private List<Report> reports;
    private ArrayList<JmmNode> nodesVisited;
    public int tempMethodParamNum;
    public Type currentArithType;
    private String scope;
    public JmmMethod currentMethod;
    public ArrayList<String> tempVariables;
    public ArrayList<String> tempVariablesOllirCode;

    public ExprOllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
        this.nodesVisited = new ArrayList<>();
        this.scope = "EXPRESSION";
        this.tempMethodParamNum = 0;
        this.tempVariables = new ArrayList<>();
        this.tempVariablesOllirCode = new ArrayList<>();
    }

    @Override
    public void buildVisitor() {
        // Expression rules
        /* addVisit("Array", this::dealWithArrayDeclaration); addVisit("Lenght", this::dealWithExpression); addVisit("UnaryOp", this::dealWithExpression); addVisit("NewArrayObject", this::dealWithExpression); */
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("BinaryOp", this::dealWithBinaryOp); // creates and returns the OLLIR code with the temporary variables
        addVisit("MemberAccess", this::dealWithMemberAccess);
        addVisit("ExprParentheses", this::dealWithExprParentheses); // (returns the OLLIR code, if BinaryOp is the father) or (returns the parentheses and the child code)
        addVisit("Integer", this::dealWithSingleExpression); // terminal nodes
        addVisit("Bool", this::dealWithSingleExpression); // terminal nodes
        addVisit("SelfCall", this::dealWithSingleExpression); // terminal nodes
        addVisit("Identifier", this::dealWithSingleExpression); // terminal nodes
    }

    public void resetTempVariables() {
        this.tempVariables = new ArrayList<>();
        this.tempVariablesOllirCode = new ArrayList<>();
    }

    private List<Object> dealWithNewObject(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        String objClassName = node.get("val");
        String tempVar = "t" + this.tempMethodParamNum + "." + objClassName;

        if (data.get(0).equals("ASSIGNMENT") || data.get(0).equals("LOCAL_VARIABLES")) {
            ollirCode.append("new(" + objClassName + ")." + objClassName + ";\n");
            ollirCode.append("invokespecial(" + node.getJmmParent().get("varName") + "." + objClassName + ", \"<init>\").V");
        } else {
            this.tempVariables.add(tempVar);
            this.tempVariablesOllirCode.add(OllirTemplates.newObjectTemplate((++this.tempMethodParamNum), objClassName));
            ollirCode.append(tempVar);
        }

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithBinaryOp(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        JmmNode leftExpr = node.getJmmChild(0);
        String op = node.get("op");
        JmmNode rightExpr = node.getJmmChild(1);

        String leftExprCode = (String) visit(leftExpr, Collections.singletonList("BINARY_OP")).get(0);
        String rightExprCode = (String) visit(rightExpr, Collections.singletonList("BINARY_OP")).get(0);
        String rightSide = leftExprCode + " " + op + OllirTemplates.type(this.currentArithType) + " " + rightExprCode;

        if (data.get(0).equals("ASSIGNMENT") || data.get(0).equals("LOCAL_VARIABLES")) {
            System.out.println("rightSide: " + rightSide);
            ollirCode.append(rightSide);
        } else {
            String operationString = OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), OllirTemplates.type(this.currentArithType), rightSide);
            this.tempVariables.add("t" + this.tempMethodParamNum);
            this.tempVariablesOllirCode.add(operationString);
            ollirCode.append("t" + this.tempMethodParamNum + OllirTemplates.type(this.currentArithType));
        }

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithMemberAccess(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        String objExpr = (String) visit(node.getJmmChild(0), Collections.singletonList("MEMBER_ACCESS")).get(0);
        Pair<String, Symbol> objExprPair = this.symbolTable.variableScope(this.currentMethod, objExpr);
        String funcName = node.get("id");

        List<JmmNode> parameters = node.getChildren().subList(1, node.getNumChildren());

        List<String> parameterString = new ArrayList<>();
        for (JmmNode parameter : parameters) {
            String paramOllirCode = (String) visit(parameter, Collections.singletonList("MEMBER_ACCESS")).get(0); // value or the temporary variable
            parameterString.add(paramOllirCode);
        }

        if (objExprPair.b == null) { // comes from the imports (there is no variable - local, parameter, field - that corresponds to objExpr)
            if (data.get(0).equals("Expr")) {
                this.tempVariablesOllirCode.add(OllirTemplates.invokestatic(objExpr, funcName, parameterString, ".V"));
            }
            else if (data.get(0).equals("RETURN")) {
                String tempVar = "t" + (++this.tempMethodParamNum) + OllirTemplates.type(this.currentMethod.getReturnType());
                this.tempVariablesOllirCode.add(tempVar +  " :=" +  OllirTemplates.type(this.currentMethod.getReturnType()) + " " + OllirTemplates.invokevirtual(objExpr, funcName, parameterString));
                ollirCode.append(tempVar);
            } else {
                ollirCode.append(OllirTemplates.invokestatic(objExpr, funcName, parameterString, ".V"));
            }
        } else { // comes from the class or the 'this' keyword
            JmmMethod funcMethod = this.symbolTable.getMethod(funcName);
            this.tempMethodParamNum++;
            this.tempVariables.add("t" + this.tempMethodParamNum + OllirTemplates.type(funcMethod.getReturnType()));
            if (data.get(0).equals("Expr")) {
                this.tempVariablesOllirCode.add(OllirTemplates.invokevirtual(objExpr, funcName, parameterString));
            } else if (data.get(0).equals("RETURN")) {
                String tempVar = "t" + (++this.tempMethodParamNum) + OllirTemplates.type(this.currentMethod.getReturnType());
                this.tempVariablesOllirCode.add(tempVar +  " :=" +  OllirTemplates.type(this.currentMethod.getReturnType()) + " " + OllirTemplates.invokevirtual(objExpr, funcName, parameterString));
                ollirCode.append(tempVar);
            } else {
                ollirCode.append(OllirTemplates.invokevirtual(objExpr, funcName, parameterString));
            }
        }

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithExprParentheses(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append((String) visit(node.getJmmChild(0), data).get(0));

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithSingleExpression(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        String val = node.get("val");

        switch (node.getKind()) {
            case "Integer":
                ollirCode.append(val + ".i32");
                this.currentArithType = new Type("int", false);
                break;
            case "Bool":
                ollirCode.append(val + ".bool");
                this.currentArithType = new Type("boolean", false);
                break;
            case "SelfCall":
                ollirCode.append(val); // "this" keyword
                break;
            case "Identifier":
                System.out.println("-> Identifier");
                Pair<String, Symbol> varScope = this.symbolTable.variableScope(this.currentMethod, val);

                switch (varScope.a) {
                    case "localVariable":
                        ollirCode.append(OllirTemplates.variableCall(varScope.b)); // returns the variable
                        this.currentArithType = varScope.b.getType();
                        break;
                    case "parameterVariable":
                        int paramIndex = this.symbolTable.getCurrentMethod().getParameterIndex(val);
                        ollirCode.append(OllirTemplates.variableCall(varScope.b, paramIndex)); // returns the variable
                        this.currentArithType = varScope.b.getType();
                        break;
                    case "fieldVariable":
                        this.tempVariablesOllirCode.add(OllirTemplates.getField((++this.tempMethodParamNum), varScope.b));
                        Symbol tempSymbol = new Symbol(varScope.b.getType(), "t" + this.tempMethodParamNum);
                        ollirCode.append(OllirTemplates.variableCall(tempSymbol));
                        this.currentArithType = varScope.b.getType();
                        break;
                    default: // Access members
                        ollirCode.append(val);
                        break;
                }

        }

        return Collections.singletonList(ollirCode.toString());
    }
}