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
    public JmmMethod currentMethod;
    public ArrayList<String> tempVariables;
    public ArrayList<String> tempVariablesOllirCode;
    public Type currentAssignmentType;

    public ExprOllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
        this.nodesVisited = new ArrayList<>();
        this.tempMethodParamNum = 0;
        this.tempVariables = new ArrayList<>();
        this.tempVariablesOllirCode = new ArrayList<>();
        this.currentAssignmentType = null;
    }

    @Override
    public void buildVisitor() {
        // Expression rules
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("Lenght", this::dealWithLength);
        addVisit("NewArrayObject", this::dealWithNewArrayObject);
        addVisit("Array", this::dealWithArrayDeclaration);
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

    private List<Object> dealWithUnaryOp(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        JmmNode arrayLengthNode = node.getJmmChild(0);

        String unaryExprCode = (String) visit(arrayLengthNode, Collections.singletonList("NEW_ARRAY")).get(0);
        unaryExprCode = "!.bool " + unaryExprCode;
        if (data.get(0).equals("CONDITIONAL") || data.get(0).equals("LOOP") || data.get(0).equals("MEMBER_ACCESS")) {
            this.tempVariablesOllirCode.add(OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), ".bool", unaryExprCode));
            ollirCode.append("t" + this.tempMethodParamNum + ".bool");
        } else {
            ollirCode.append("!.bool" +  " " + unaryExprCode);
        }
        this.currentArithType = new Type("boolean", false);

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithLength(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        JmmNode exprNode = node.getJmmChild(0);

        String exprNodeOllirCode = (String) visit(exprNode, Collections.singletonList(data)).get(0);

        String exprNodeName = exprNodeOllirCode;
        int dotIndex = exprNodeName.indexOf("."); // has the type integrated in the objExpr
        if (exprNodeName.chars().filter(ch -> ch == '.').count() == 2) { // parameter (remove the param index and param type)
            exprNodeName = exprNodeName.substring(dotIndex + 1, exprNodeName.length());
            dotIndex = exprNodeName.indexOf("."); // index of the 2nd "."
            exprNodeName = exprNodeName.substring(0, dotIndex);
        } else {
            exprNodeName = exprNodeName.substring(0, dotIndex);
        }

        Pair<String, Symbol> pair = this.symbolTable.variableScope(this.currentMethod, exprNodeName);
        String varScope = pair.a;
        Symbol variable = pair.b;

        if (this.currentArithType.isArray() && !data.get(0).equals("ARRAY_ASSIGNMENT_VALUE") && !data.get(0).equals("ASSIGNMENT") && !data.get(0).equals("LOCAL_VARIABLES")) {
            String typeAcc = ".i32";
            String rightSide = "arraylength(" + exprNodeOllirCode + ")" + typeAcc;
            this.tempVariablesOllirCode.add(OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), typeAcc, rightSide));
            ollirCode.append("t" + this.tempMethodParamNum + typeAcc);
        } else {
            if (varScope.equals("parameterVariable")) {
                int paramIndex = this.currentMethod.getParameterIndex(variable.getName());
                ollirCode.append("$" + paramIndex + "." + variable.getName() + ".length");
            } else {
                ollirCode.append(exprNodeName + ".length");
            }
        }

        return Collections.singletonList(ollirCode.toString());

    }

    private List<Object> dealWithNewArrayObject(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        JmmNode arrayLengthNode = node.getJmmChild(0);

        String arrayLength = (String) visit(arrayLengthNode, Collections.singletonList("NEW_ARRAY")).get(0);
        Type assignmentType = new Type(this.currentAssignmentType.getName(), true);

        ollirCode.append("new(array, " + arrayLength + ")" + OllirTemplates.type(assignmentType));

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithArrayDeclaration(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        JmmNode arrNameNode = node.getJmmChild(0);
        JmmNode arrIndexNode = node.getJmmChild(1);

        String arrName = (String) visit(arrNameNode, Collections.singletonList("ARRAY_DECLARATION")).get(0); // array name
        String arrIndex = (String) visit(arrIndexNode, Collections.singletonList("ARRAY_DECLARATION")).get(0); // index or temporary variable

        int dotIndex = arrName.indexOf("."); // has the type integrated in the objExpr
        String nameTypeStr = new String();
        if (arrNameNode.getNumChildren() == 0 && dotIndex != -1) { // terminal symbol
            if (arrName.chars().filter(ch -> ch == '.').count() == 2) { // parameter (remove the param index and param type)
                List<String> parts = Arrays.asList(arrName.split("\\.")); // Escape the dot with double backslashes
                if (parts.contains("array")) { // varName.array.type
                    String allName = arrName; // varName.array.type
                    arrName = allName.substring(0, dotIndex); // varName
                    String otherPart = allName.substring(dotIndex + 1, allName.length()); // array.type
                    dotIndex = otherPart.indexOf(".");
                    nameTypeStr = otherPart.substring(dotIndex + 1, otherPart.length()); // type
                } else {
                    arrName = arrName.substring(dotIndex + 1, arrName.length());
                    dotIndex = arrName.indexOf("."); // index of the 2nd "."
                    nameTypeStr = arrName.substring(dotIndex + 1, arrName.length());
                    arrName = arrName.substring(0, dotIndex);
                }
            } else if (arrName.chars().filter(ch -> ch == '.').count() == 3) { // array parameter (remove the param index, the array keyword and the array type)
                arrName = arrName.substring(dotIndex + 1, arrName.length());
                dotIndex = arrName.indexOf("."); // index of the 2nd "."
                arrName = arrName.substring(0, dotIndex);
            } else { // localVariable or classField
                nameTypeStr = arrName.substring(dotIndex + 1, arrName.length());
                arrName = arrName.substring(0, dotIndex);
            }
        }


        Pair<String, Symbol> pair = this.symbolTable.variableScope(this.currentMethod, arrName);
        String varScope = pair.a;
        Symbol arrayVariable = pair.b;
        int paramIndex = 0;

        if (data.get(0).equals("ASSIGNMENT") || data.get(0).equals("ARRAY_ASSIGNMENT_VALUE") || data.get(0).equals("LOCAL_VARIABLES") || data.get(0).equals("LOOP")) {
            switch (varScope) {
                case "localVariable":
                    this.currentArithType = new Type(arrayVariable.getType().getName(), false);
                    ollirCode.append(arrayVariable.getName() + ".array" + OllirTemplates.type(arrayVariable.getType()) + "[" + arrIndex + "]" + OllirTemplates.type(this.currentArithType));
                    break;
                case "parameterVariable":
                    this.currentArithType = new Type(arrayVariable.getType().getName(), false);
                    paramIndex = this.currentMethod.getParameterIndex(arrayVariable.getName());
                    ollirCode.append("$" + paramIndex + "." + arrayVariable.getName() + ".array" + OllirTemplates.type(arrayVariable.getType()) + "[" + arrIndex + "]" + OllirTemplates.type(this.currentArithType));
                    break;
                case "fieldVariable":
                    this.currentArithType = new Type(arrayVariable.getType().getName(), false);
                    this.tempVariablesOllirCode.add(OllirTemplates.getField((++this.tempMethodParamNum), arrayVariable));
                    Symbol tempSymbol = new Symbol(arrayVariable.getType(), "t" + this.tempMethodParamNum);
                    ollirCode.append(tempSymbol.getName() + ".array" + OllirTemplates.type(arrayVariable.getType()));
                    break;
                default:
                    if (data.get(0).equals("ASSIGNMENT")) {
                        ollirCode.append(arrName + ".array." + nameTypeStr + "[" + arrIndex + "]." + nameTypeStr);
                    } else {
                        String tempVar = "t" + (++this.tempMethodParamNum) + ".array." + nameTypeStr;
                        tempVariablesOllirCode.add(tempVar + " :=" + nameTypeStr + " " + arrName + "[" + arrIndex + "]." + nameTypeStr + ";\n");
                        ollirCode.append(tempVar);
                    }
                    break;
            }
        } else {
            switch (varScope) {
                case "localVariable":
                    this.currentArithType = new Type(arrayVariable.getType().getName(), false);
                    String tempVar = "t" + (++this.tempMethodParamNum) + OllirTemplates.type(this.currentArithType);
                    tempVariablesOllirCode.add(tempVar + " :=" + OllirTemplates.type(this.currentArithType) + " " + arrayVariable.getName() + OllirTemplates.type(arrayVariable.getType()) + "[" + arrIndex + "]" + OllirTemplates.type(new Type(arrayVariable.getType().getName(), false)) + ";\n");
                    tempVariables.add(tempVar);
                    ollirCode.append(tempVar);
                    break;
                case "parameterVariable":
                    this.currentArithType = new Type(arrayVariable.getType().getName(), false);
                    paramIndex = this.currentMethod.getParameterIndex(arrayVariable.getName());
                    String tempVar2 = "t" + (++this.tempMethodParamNum) + OllirTemplates.type(this.currentArithType);
                    tempVariablesOllirCode.add(tempVar2 + " :=" + OllirTemplates.type(this.currentArithType) + " $" + paramIndex + "." + arrayVariable.getName() + OllirTemplates.type(arrayVariable.getType()) + "[" + arrIndex + "]" + OllirTemplates.type(new Type(arrayVariable.getType().getName(), false)) + ";\n");
                    tempVariables.add(tempVar2);
                    ollirCode.append(tempVar2);
                    break;
                case "fieldVariable":
                    this.currentArithType = new Type(arrayVariable.getType().getName(), false);
                    String tempVar3 = "t" + (++this.tempMethodParamNum) + OllirTemplates.type(this.currentArithType);
                    this.tempVariablesOllirCode.add(OllirTemplates.getField(this.tempMethodParamNum, arrayVariable));
                    tempVariables.add(tempVar3);
                    String tempVar4 = "t" + (++this.tempMethodParamNum) + OllirTemplates.type(this.currentArithType);
                    tempVariablesOllirCode.add(tempVar4 + " :=" + OllirTemplates.type(this.currentArithType) + " " + arrayVariable.getName() + OllirTemplates.type(arrayVariable.getType()) + "[" + arrIndex + "]" + OllirTemplates.type(new Type(arrayVariable.getType().getName(), false)) + ";\n");
                    ollirCode.append(tempVar4);
                    break;
                default:
                    String tempVar5 = "t" + (++this.tempMethodParamNum) + ".array" + nameTypeStr;
                    tempVariablesOllirCode.add(tempVar5 + " :=" + OllirTemplates.type(this.currentArithType) + " " + arrName + ".array" + nameTypeStr + "[" + arrIndex + "]." + nameTypeStr + ";\n"); // CRITICAL (this.currentArithType may be null)
                    ollirCode.append(tempVar5);
                    break;
            }
        }

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithNewObject(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        StringBuilder ollirCode = new StringBuilder();
        String objClassName = node.get("val");
        String tempVar = "t" + (++this.tempMethodParamNum) + "." + objClassName;

        if (data.get(0).equals("ASSIGNMENT") || data.get(0).equals("LOCAL_VARIABLES")) {
            ollirCode.append("new(" + objClassName + ")." + objClassName + ";\n");
            ollirCode.append("invokespecial(" + node.getJmmParent().get("varName") + "." + objClassName + ", \"<init>\").V");
        } else {
            this.tempVariables.add(tempVar);
            this.tempVariablesOllirCode.add(OllirTemplates.newObjectTemplate(tempVar, objClassName));
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


        if (data.get(0).equals("ASSIGNMENT") || data.get(0).equals("ARRAY_ASSIGNMENT") || data.get(0).equals("LOCAL_VARIABLES")) {
            String rightSide = "";
            if (Arrays.asList("<", "<=", ">", ">=", "&&", "||").contains(op)) {
                rightSide = leftExprCode + " " + op + ".bool" + " " + rightExprCode;
                this.currentArithType = new Type("boolean", false);
            } else {
                rightSide = leftExprCode + " " + op + OllirTemplates.type(this.currentAssignmentType) + " " + rightExprCode;
            }
            ollirCode.append(rightSide);
        } else if (data.get(0).equals("LOOP")) {
            String rightSide = "";
            if (Arrays.asList("<", "<=", ">", ">=", "&&", "||").contains(op)) {
                rightSide = leftExprCode + " " + op + ".bool" + " " + rightExprCode;
                this.currentArithType = new Type("boolean", false);
            } else {
                rightSide = leftExprCode + " " + op + OllirTemplates.type(this.currentArithType) + " " + rightExprCode;
            }
            String operationString = OllirTemplates.temporaryVariableTemplate((++this.tempMethodParamNum), ".bool", rightSide);
            this.tempVariables.add("t" + this.tempMethodParamNum);
            this.tempVariablesOllirCode.add(operationString);
            ollirCode.append("t" + this.tempMethodParamNum + ".bool");
        } else {
            String rightSide = "";
            if (Arrays.asList("<", "<=", ">", ">=", "&&", "||").contains(op)) {
                rightSide = leftExprCode + " " + op + ".bool" + " " + rightExprCode;
                this.currentArithType = new Type("boolean", false);
            } else {
                rightSide = leftExprCode + " " + op + OllirTemplates.type(this.currentArithType) + " " + rightExprCode;
            }
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

        String funcName = node.get("id");
        // Get the function method
        JmmMethod funcMethod = this.symbolTable.getMethod(funcName);
        List<JmmNode> parameters = node.getChildren().subList(1, node.getNumChildren());
        List<String> parameterString = new ArrayList<>();
        for (JmmNode parameter : parameters) {
            String paramOllirCode = (String) visit(parameter, Collections.singletonList("MEMBER_ACCESS")).get(0); // value or the temporary variable
            parameterString.add(paramOllirCode);
        }

        String objExpr = (String) visit(node.getJmmChild(0), Collections.singletonList("MEMBER_ACCESS")).get(0);
        int dotIndex = objExpr.indexOf("."); // has the type integrated in the objExpr
        String retAcc = OllirTemplates.type(this.currentMethod.getReturnType());
        if ((dotIndex == -1 && !objExpr.equals("this"))) { // objExpr it's an import, use invokestatic
            if (data.get(0).equals("ASSIGNMENT") || data.get(0).equals("ARRAY_ASSIGNMENT") || data.get(0).equals("LOCAL_VARIABLES") || data.get(0).equals("LOOP")) {
                String invokeStaticStr = OllirTemplates.invokestatic(objExpr, funcName, parameterString, OllirTemplates.type(this.currentAssignmentType));
                ollirCode.append(invokeStaticStr.substring(0, invokeStaticStr.length() - 2));
            } else {
                String tempVar = "t" + (++this.tempMethodParamNum) + retAcc;
                this.tempVariables.add(tempVar);
                this.tempVariablesOllirCode.add(((data.get(0).equals("BINARY_OP") || data.get(0).equals("RETURN") || data.get(0).equals("MEMBER_ACCESS")) ? (tempVar + " :=" + retAcc + " ") : "") + OllirTemplates.invokestatic(objExpr, funcName, parameterString, retAcc));
                ollirCode.append(tempVar);
            }
        } else { // use invokevirtual
            String objExprName = new String();
            if (objExpr.equals("this")) {
                objExprName = "this";
            } else {
                if (objExpr.chars().filter(ch -> ch == '.').count() == 2) { // parameter (remove the param index and param type)
                    objExprName = objExpr.substring(dotIndex + 1, objExpr.length());
                    dotIndex = objExprName.indexOf("."); // index of the 2nd "."
                    objExprName = objExprName.substring(0, dotIndex);
                } else {
                    objExprName = objExpr.substring(0, dotIndex);
                }
            }

            if (funcMethod == null) {
                retAcc = OllirTemplates.type(this.currentAssignmentType);
            } else {
                retAcc = OllirTemplates.type(funcMethod.getReturnType());
            }

            if (data.get(0).equals("ASSIGNMENT") || data.get(0).equals("ARRAY_ASSIGNMENT") || data.get(0).equals("LOCAL_VARIABLES") || data.get(0).equals("LOOP")) {
                String invokeStaticStr = OllirTemplates.invokevirtual(objExpr, funcName, parameterString, retAcc);
                ollirCode.append(invokeStaticStr.substring(0, invokeStaticStr.length() - 2));
            } else {
                String tempVar = "t" + (++this.tempMethodParamNum) + retAcc;
                this.tempVariables.add(tempVar);
                this.tempVariablesOllirCode.add(((data.get(0).equals("BINARY_OP") || data.get(0).equals("RETURN") || data.get(0).equals("MEMBER_ACCESS")) ? (tempVar + " :=" + retAcc + " ") : "") + OllirTemplates.invokevirtual(objExpr, funcName, parameterString, retAcc));
                ollirCode.append(tempVar);
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
                ollirCode.append(val + "." + this.symbolTable.getClassName()); // "this" keyword
                break;
            case "Identifier":
                Pair<String, Symbol> varScope = this.symbolTable.variableScope(this.currentMethod, val);
                switch (varScope.a) {
                    case "localVariable":
                        //ollirCode.append(varScope.b.getName() + OllirTemplates.type(varScope.b.getType()));
                        ollirCode.append(OllirTemplates.variableCall(varScope.b)); // returns the variable
                        this.currentArithType = varScope.b.getType();
                        break;
                    case "parameterVariable":
                        int paramIndex = this.currentMethod.getParameterIndex(val);
                        ollirCode.append(OllirTemplates.variableCall(varScope.b, paramIndex)); // returns the variable
                        this.currentArithType = varScope.b.getType();
                        break;
                    case "fieldVariable":
                        this.tempVariablesOllirCode.add(OllirTemplates.getField((++this.tempMethodParamNum), varScope.b));
                        Symbol tempSymbol = new Symbol(varScope.b.getType(), "t" + this.tempMethodParamNum);
                        //ollirCode.append(tempSymbol.getName() + OllirTemplates.type(tempSymbol.getType()));
                        ollirCode.append(OllirTemplates.variableCall(tempSymbol));
                        this.currentArithType = varScope.b.getType();
                        break;
                    default: // Access members (imports)
                        ollirCode.append(val);
                        break;
                }

        }

        return Collections.singletonList(ollirCode.toString());
    }
}
