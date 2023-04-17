package pt.up.fe.comp2023.ast;

import org.antlr.v4.runtime.misc.Pair;
import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.management.ObjectName;
import java.util.*;

public class OllirVisitor extends AJmmVisitor<List<Object>, List<Object>> {
    private final JmmSymbolTable symbolTable;
    private final List<Report> reports;
    List<JmmNode> nodesVisited;
    private String scope;
    private JmmMethod currentMethod;

    int tempMethodParamNum = 0;

    boolean dealWithReturnType = false;
    String currentArithType = "";

    public OllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        this.symbolTable = symbolTable;
        this.reports = reports;
        this.nodesVisited = new ArrayList<>();
    }

    @Override
    public void buildVisitor() {
        // Program rule
        addVisit("Program", this::dealWithProgramDeclaration);

        // ImportDeclaration rule
        addVisit("ImportDeclaration", this::dealWithImportDeclaration);

        // SubImportDeclaration rule
        addVisit("SubImportDeclaration", this::dealWithSubImportDeclaration);

        // ClassDeclaration rule
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);

        // ExtendsClassDeclaration rules
        addVisit("ExtendedClass", this::dealWithExtendedClassDeclaration);

        // MethodDeclaration rule
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);

        // ClassParameters rule
        addVisit("ClassParameters", this::dealWithClassParameters);

        // LocalVariable rule
        addVisit("LocalVariables", this::dealWithLocalVariables);

        // Statement rules
        /* addVisit("Brackets", this::defaultVisit); */
        addVisit("Conditional", this::dealWithConditional);
        addVisit("Loop", this::dealWithLoop);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Expr", this::dealWithExpression);

        // Expression rules
        /*
        addVisit("Array", this::dealWithArrayDeclaration);
        addVisit("Lenght", this::dealWithExpression);

        addVisit("UnaryOp", this::dealWithExpression);
        addVisit("NewArrayObject", this::dealWithExpression);
         */
        addVisit("NewObject", this::dealWithNewObject);

        addVisit("MemberAccess", this::dealWithMemberAccess);
        addVisit("BinaryOp", this::dealWithBinaryOp); // creates and returns the OLLIR code with the temporary variables
        addVisit("ExprParentheses", this::dealWithExprParentheses); // (returns the OLLIR code, if BinaryOp is the father) or (returns the parentheses and the child code)
        addVisit("Integer", this::dealWithSingleExpression); // terminal nodes
        addVisit("Bool", this::dealWithSingleExpression); // terminal nodes
        addVisit("SelfCall", this::dealWithSingleExpression); // terminal nodes
        addVisit("Identifier", this::dealWithSingleExpression); // terminal nodes


        setDefaultVisit(this::defaultVisit);
    }

    private List<Object> dealWithProgramDeclaration(JmmNode node, List<Object> data) {
        // Check if node was already visited
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithProgramDeclaration() function!");

        StringBuilder ollirCode = new StringBuilder();
        String result = new String();

        for (JmmNode child : node.getChildren()) {
            result += (String) visit(child, Collections.singletonList("PROGRAM")).get(0);
        }

        ollirCode.append(result);
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithImportDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("In dealWithImportDeclaration() function!");

        String importName = node.get("id");
        StringBuilder ollirCode = new StringBuilder();
        String subImportsCode = "";

        for (JmmNode childSubImport : node.getChildren()) {
            subImportsCode += (String) visit(childSubImport, Collections.singletonList("IMPORT")).get(0);
        }

        ollirCode.append(OllirTemplates.importTemplate(importName, subImportsCode));
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithSubImportDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithSubImportDeclaration() function!");
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append(OllirTemplates.subImportTemplate(node.get("id")));
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithClassDeclaration(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithClassDeclaration() function!");
        this.scope = "CLASS";

        StringBuilder ollirCode = new StringBuilder();

        if (this.symbolTable.getSuper() == null) { // No extended class
            ollirCode.append(OllirTemplates.classTemplate(this.symbolTable.getClassName(), ""));

            // Deal with class fields
            for (Symbol field : this.symbolTable.getFields()) {
                ollirCode.append(OllirTemplates.fieldTemplate(field));
            }

            // Deal with class default constructor
            if (this.symbolTable.getMethod(this.symbolTable.getClassName(), new ArrayList<>(), new Type("void", false)) == null) {
                ollirCode.append(OllirTemplates.defaultConstructor(this.symbolTable.getClassName()));
            }
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
            } else if (child.getKind().equals("MethodDeclarationOther") || child.getKind().equals("MethodDeclarationMain")) {
                String methodOllirCode = (String) visit(child, data).get(0);
                ollirCode.append(methodOllirCode);
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
        System.out.println("In dealWithExtendedClassDeclaration() function!");

        StringBuilder ollirCode = new StringBuilder();
        String extendedName = node.get("extendedClassName");

        ollirCode.append(OllirTemplates.extendedClassTemplate(extendedName));
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithLocalVariables(JmmNode node, List<Object> data) {
        if (this.nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithLocalVariables() function!");
        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName");
        Symbol localVarSymbol = this.currentMethod.getLocalVariable(varName);
        Symbol methodParam = this.currentMethod.getParameter(varName);
        Symbol classField = this.symbolTable.getField(varName);
        String varscope = new String();
        if (localVarSymbol != null) varscope = "localVariable";
        else if (methodParam != null) varscope = "paramVariable";
        else {
            varscope = "classField";
        }
        if (this.scope.equals("METHOD")) {
            // If it's a variable declaration inside a method, you are not supposed to add it in the OLLIR code
            JmmNode childNodeType = node.getChildren().get(0);
            Type varType = JmmSymbolTable.getType(childNodeType, "typeName");
            Symbol newLocalVariable = new Symbol(varType, varName);
            if (node.getChildren().size() > 1) { // node with variable declaration and assignment
                JmmNode exprNode = node.getJmmChild(node.getChildren().size() - 1); // Expression node
                String expressionOLLIRCode = (String) visit(exprNode, Collections.singletonList("LocalVariable")).get(0); // get the newValue OLLIR code
                System.out.println("exprNode.getChildren(): " + exprNode.getChildren());
                System.out.println("expressionOLLIRCode: " + expressionOLLIRCode);
                boolean simpleAssignment = !exprNode.getKind().equals("NewArrayObject") && !exprNode.getKind().equals("NewObject");
                for (JmmNode child : exprNode.getChildren()) {
                    System.out.println("child.getKind(): " + child.getKind());
                    if (child.getNumChildren() > 0 || child.getKind().equals("NewArrayObject") || child.getKind().equals("NewObject")) {
                        simpleAssignment = false;
                        break;
                    }
                }
                System.out.println("SimpleAssignment: " + simpleAssignment);
                if (simpleAssignment) {
                    System.out.println("Simple Assignment!");
                    System.out.println("exprNode: " + exprNode);
                    if (exprNode.getNumChildren() == 0) {
                        if (localVarSymbol != null) {
                            System.out.println("expressionOLLIRCode: " + expressionOLLIRCode);
                            System.out.println("localVarSymbol: " + localVarSymbol);
                            ollirCode.append(OllirTemplates.variableAssignment(localVarSymbol, "-1", expressionOLLIRCode));
                        } else if (methodParam != null) {
                            int paramIndex = this.currentMethod.getParameterIndex(methodParam.getName());
                            ollirCode.append(OllirTemplates.variableAssignment(localVarSymbol, Integer.toString(paramIndex), expressionOLLIRCode));
                        } else {
                            ollirCode.append(OllirTemplates.variableAssignment(-1, classField, expressionOLLIRCode));
                        }
                    } else {
                        System.out.println("expressionOLLIRCode: " + expressionOLLIRCode);
                        ollirCode.append(expressionOLLIRCode);
                        String tempVar = "t" + this.tempMethodParamNum + OllirTemplates.type(varType);
                        this.tempMethodParamNum++;
                        ollirCode.append(OllirTemplates.localVariableAssignment(newLocalVariable, tempVar));
                    }
                } else { // not a simple assignment (non-terminal symbols)
                    System.out.println("exprNode: " + exprNode);
                    System.out.println("exprNode.getChidlren(): " + exprNode.getChildren());

                    if (exprNode.getKind().equals("MemberAccess") || true) {
                        ollirCode.append(expressionOLLIRCode);
                        // Assign the new temporary variable to the correspondent variable name
                        String typeAcc = new String();
                        switch (varscope) {
                            case "localVariable":
                                typeAcc = OllirTemplates.type(localVarSymbol.getType());
                                String rightSide = "t" + this.tempMethodParamNum + typeAcc;
                                this.tempMethodParamNum++;
                                ollirCode.append(OllirTemplates.variableAssignment(localVarSymbol, typeAcc, rightSide));
                                break;
                            case "paramVariable":
                                typeAcc = OllirTemplates.type(methodParam.getType());
                                int paramIndex = this.currentMethod.getParameterIndex(varName);
                                String rightSide2 = "t" + this.tempMethodParamNum + typeAcc;
                                ollirCode.append(OllirTemplates.variableAssignment(methodParam, rightSide2, paramIndex));
                                break;
                            case "classField":
                                String rightSide3 = "t" + this.tempMethodParamNum + typeAcc;
                                ollirCode.append(OllirTemplates.putField(classField, rightSide3));
                                break;
                            default:
                                break;
                        }
                    }
                    /*
                    else {
                        for (JmmNode child : exprNode.getChildren()) {
                            if (child.getChildren().size() != 0) {
                                ollirCode.append(expressionOLLIRCode);
                                String tempVar = "t" + this.tempMethodParamNum + OllirTemplates.type(varType);
                                this.tempMethodParamNum++;
                                System.out.println("OllirTemplates.localVariableAssignment(newLocalVariable, tempVar): " + OllirTemplates.localVariableAssignment(newLocalVariable, tempVar));
                                ollirCode.append(OllirTemplates.localVariableAssignment(newLocalVariable, tempVar));
                            } else {
                                String newValue = child.get("val");
                            }
                        }
                    }
                     */
                }
            }
        }

        System.out.println("ollirCode: " + ollirCode.toString());
        System.out.println("\n\n\n\n\n\n\n\n");

        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithMethodDeclaration(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithMethodDeclaration() function!");

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

            ollirCode.append(OllirTemplates.methodTemplate(
                    "main",
                    currentMethod.getParameters(),
                    currentMethod.getReturnType(),
                    true));

            for (JmmNode child : node.getChildren()) {
                if (Arrays.asList("IntegerArrayType", "IntegerType", "BooleanType", "StringType", "VoidType", "IdType").contains(child.getKind())) {
                    visit(child);
                } else if (child.getKind().equals("LocalVariables")) {
                    String localVarOllirCode = (String) visit(child, Collections.singletonList("LocalVariables")).get(0);
                    ollirCode.append(localVarOllirCode);
                } else {
                    String statementsOllirCode = (String) visit(child, Collections.singletonList("LocalVariables")).get(0);
                    ollirCode.append(statementsOllirCode);
                }
            }
            ollirCode.append(OllirTemplates.closeBrackets());

        } else { // MethodDeclarationOther
            String methodName = node.get("methodName");
            Type returnType = this.symbolTable.getReturnType(methodName);

            try {
                this.currentMethod = this.symbolTable.getMethod(methodName);
            } catch (Exception e) { // if no method is found
                this.currentMethod = null;
                e.printStackTrace();
            }

            ollirCode.append(OllirTemplates.methodTemplate(
                    methodName,
                    this.currentMethod.getParameters(),
                    returnType, false));

            // Visit the children of the node
            for (JmmNode child: node.getChildren()) {
                System.out.println("Child: " + child.getKind());
                if (child.getKind().equals("ReturnType") || child.getKind().equals("ClassParameters")) {
                    visit(child, Collections.singletonList("")); // ignore the child
                } else if (child.getKind().equals("ReturnObj")) {
                    JmmNode expressionNode = child.getJmmChild(0);
                    this.dealWithReturnType = true;
                    System.out.println("expressionNode: " + expressionNode);
                    String returnObjStr = (String) visit(expressionNode, Collections.singletonList("ReturnObj")).get(0);
                    if (Arrays.asList("Array", "Lenght", "MemberAccess", "UnaryOp", "BinaryOp", "NewArrayObject", "NewObject").contains(expressionNode.getKind())) { // complex return types (returnObjStr has already the OLLIR code with the temp variables)
                        System.out.println("returnObjStr: " + returnObjStr);
                        if (expressionNode.getNumChildren() == 0) { // simple returns
                            this.tempMethodParamNum++;
                            ollirCode.append(OllirTemplates.createOpAssignment(this.currentArithType, this.tempMethodParamNum, returnObjStr));
                            ollirCode.append(OllirTemplates.returnTemplate(this.currentArithType + " t" + this.tempMethodParamNum + this.currentArithType));
                        } else { // complex return
                            ollirCode.append(returnObjStr);
                            String tempVariable = "t" + this.tempMethodParamNum + this.currentArithType;
                            ollirCode.append(OllirTemplates.returnTemplate(this.currentArithType + " " + tempVariable));
                        }
                    } else {
                        ollirCode.append(OllirTemplates.returnTemplate(this.currentArithType, returnObjStr));
                    }
                    this.dealWithReturnType = false;
                    this.currentArithType = null;

                } else if (child.getKind().equals("LocalVariables")) { // Local Variables
                    String localVariableOllirCode = (String) visit(child, Collections.singletonList("METHOD")).get(0);
                    ollirCode.append(localVariableOllirCode);
                } else { // deal with statements
                    String ollirStmtCode = (String) visit(child, Collections.singletonList("")).get(0);
                    ollirCode.append(ollirStmtCode);
                }
            }
            ollirCode.append(OllirTemplates.closeBrackets());
        }

        System.out.println("ollirCode (dealWithMethodDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithClassParameters(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithClassParameters() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        System.out.println("ollirCode(dealWithClassParameters): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithConditional(JmmNode node, List<Object> data) { // FINISH!
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithConditional() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        System.out.println("ollirCode(dealWithConditional): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithLoop(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithLoop() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        System.out.println("ollirCode(dealWithLoop): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithArrayAssignment(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithArrayAssignment() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();
        String indexOllirCode = new String();
        String valueOllirCode = new String();
        String indexValue = new String();
        String value = new String();


        String varName = node.get("varName"); // Name of the List
        JmmNode indexNode = node.getChildren().get(0);
        JmmNode valueNode = node.getChildren().get(1);

        // Deal with the index (left side)
        if (Arrays.asList("Integer", "Bool", "Identifier", "SelfCall").contains(indexNode.getKind())) { // terminal nodes
            indexValue = indexNode.get("val");
        } else {
            // Deal with the other "expression" types
            indexOllirCode = (String) visit(indexNode, Collections.singletonList("ArrayAssignment")).get(0); // The string returned has the temporary variables and the OLLIR code already implemented
            ollirCode.append(indexOllirCode);
        }

        // Deal with the new value (right side)
        if (Arrays.asList("Integer", "Bool", "Identifier", "SelfCall").contains(valueNode.getKind())) { // terminal nodes
            value = valueNode.get("val");
        } else {
            // Deal with the other "expression" types
            valueOllirCode = (String) visit(valueNode, Collections.singletonList("ArrayAssignment")).get(0); // The string returned has the temporary variables and the OLLIR code already implemented
            ollirCode.append(valueOllirCode);
        }

        boolean indexNodeIsTerminalSymbol = indexNode.getAttributes().contains("val");
        boolean valueNodeIsTerminalSymbol = valueNode.getAttributes().contains("val");
        Symbol variable = this.currentMethod.getLocalVariable(varName);
        System.out.println("this.currentArithType: " + this.currentArithType);
        if (indexNodeIsTerminalSymbol && valueNodeIsTerminalSymbol) {
            ollirCode.append(OllirTemplates.variableAssignment(variable, indexValue, value + OllirTemplates.type(new Type(variable.getType().getName(), false))));
        } else if (!indexNodeIsTerminalSymbol && valueNodeIsTerminalSymbol) {
            ollirCode.append(indexOllirCode);
            String tempVar = "t" + this.tempMethodParamNum;
            this.tempMethodParamNum++;
            ollirCode.append(OllirTemplates.variableAssignment(variable, tempVar, value + OllirTemplates.type(new Type(variable.getType().getName(), false))));
        } else if (indexNodeIsTerminalSymbol && !valueNodeIsTerminalSymbol) {
            ollirCode.append(valueOllirCode);
            String tempVar = "t" + this.tempMethodParamNum;
            this.tempMethodParamNum++;
            ollirCode.append(OllirTemplates.variableAssignment(variable, indexValue, tempVar));
        } else { // both symbols are not terminal symbols
            ollirCode.append(indexOllirCode);
            ollirCode.append(valueOllirCode);
            String temp1 = "t" + (this.tempMethodParamNum - 1); // index temp variable
            String temp2 = "t" + (this.tempMethodParamNum); // new value temp variable
            this.tempMethodParamNum++;
            ollirCode.append(OllirTemplates.variableAssignment(variable, temp1, temp2));
        }

        System.out.println("ollirCode(dealWithArrayAssignment): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithAssignment(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("In dealWithAssignment visitor (" + node +")!");

        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName");
        System.out.println("node.getChildren(): " + node.getChildren());
        JmmNode valueNode = node.getJmmChild(0);
        boolean valueNodeIsTerminalSymbol = (valueNode.getChildren().size() == 0);
        String newValueOllirCode = (String) visit(valueNode, Collections.singletonList("Assignment")).get(0); // Change because you are already receveing the ollir string with the tmeporary variables over here!!!!

        System.out.println("varName = " + varName);
        System.out.println("newValueOllirCode = " + newValueOllirCode);
        Pair<String, Symbol> varScope = this.symbolTable.variableScope(this.currentMethod, varName);
        String varSpot = varScope.a;
        Symbol variable = varScope.b;

        System.out.println("Dealing with binary operation: " + data.contains("BinaryOp"));
        switch (varSpot) {
            case "localVariable":
                if (valueNodeIsTerminalSymbol) {
                    ollirCode.append(OllirTemplates.variableAssignment(variable, "-1", newValueOllirCode));
                } else {
                    if (node.getJmmChild(0).getKind().equals("MemberAccess")) {
                        String typeAcc = OllirTemplates.type(variable.getType());
                        ollirCode.append(varName + typeAcc + " :=" + typeAcc + " ");
                        ollirCode.append(newValueOllirCode);
                    }
                    else if (node.getJmmChild(0).getKind().equals("BinaryOp")) {
                        ollirCode.append(newValueOllirCode);
                    } else {
                        ollirCode.append(newValueOllirCode);
                        Type varType = variable.getType();
                        this.tempMethodParamNum++;
                        String newValueTempVar = "t" + this.tempMethodParamNum + OllirTemplates.type(varType);
                        this.tempMethodParamNum++;
                        //ollirCode.append(OllirTemplates.variableAssignment(variable, null, newValueTempVar));
                    }
                }
                break;
            case "parameterVariable":
                int paramIndex = this.currentMethod.getParameterIndex(variable.getName());

                if (valueNodeIsTerminalSymbol) {
                    ollirCode.append(OllirTemplates.variableAssignment(variable, newValueOllirCode, paramIndex));
                } else {
                    ollirCode.append(newValueOllirCode);
                    Type varType = variable.getType();
                    String newValueTempVar = "t" + this.tempMethodParamNum + OllirTemplates.type(varType);
                    this.tempMethodParamNum++;
                    ollirCode.append(OllirTemplates.variableAssignment(variable, newValueTempVar, paramIndex));
                }
                break;
            case "fieldVariable":
                if (valueNodeIsTerminalSymbol) {
                    ollirCode.append(OllirTemplates.variableAssignment(this.tempMethodParamNum, variable, newValueOllirCode));
                } else {
                    ollirCode.append(newValueOllirCode);
                    Type varType = variable.getType();
                    String newValueTempVar = "t" + this.tempMethodParamNum + OllirTemplates.type(varType);
                    ollirCode.append(OllirTemplates.variableAssignment(this.tempMethodParamNum, variable, newValueTempVar));
                }
                break;
            default:

                break;
        }

        System.out.println("ollirCode(dealWithAssignment): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithExpression(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithExpression() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        String childOllirCode = (String) visit(node.getChildren().get(0), Collections.singletonList("Expr")).get(0);
        ollirCode.append(childOllirCode);

        System.out.println("ollirCode(dealWithExpression): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithArrayDeclaration(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithArrayDeclaration() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        System.out.println("ollirCode(dealWithExpression): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithMemberAccess(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithMemberAccess() function! (" + node + ")");
        StringBuilder ollirCode = new StringBuilder();

        JmmNode firstChild = node.getChildren().get(0);
        String memberAccessed = node.get("id");

        List<String> parameters = new ArrayList<String>();
        String parametersString = new String();
        List<String> parametersTempVariables = new ArrayList<String>();

        String firstChildStr = (String) visit(firstChild, Collections.singletonList("MemberAccess")).get(0);

        for (int i = 1; i < node.getChildren().size(); i++) {
            String paramOllirCode = (String) visit(node.getJmmChild(i), Collections.singletonList("MemberAccess")).get(0);
            System.out.println("paramOllirCode:" + paramOllirCode);
            System.out.println("data.get(0) = " + data.get(0));
            if ((data.get(0).equals("ArrayAssignment") || data.get(0).equals("Assignment") || data.get(0).equals("Expr") || data.get(0).equals("ReturnObj")) && node.getJmmChild(i).getNumChildren() > 0) { // complex parameters
                parameters.add("t" + this.tempMethodParamNum + this.currentArithType);
                parametersTempVariables.add(paramOllirCode);
            } else {
                parameters.add(paramOllirCode);
            }
        }
        System.out.println("parameters: " + parameters);
        System.out.println("parametersTempVariables: " + parametersTempVariables);

        parametersString = String.join(", ", parameters);

        ollirCode.append(String.join("\n", parametersTempVariables));

        if (firstChild.getChildren().size() == 0 && this.symbolTable.getImports().contains(firstChild.get("val"))) { // use invokestatic
            String tempVarSent = new String("");
            if (data.get(0).equals("BinaryOp")) { // save the result of invokestatic to a temporary variable
                this.tempMethodParamNum++;
                tempVarSent = "t" + this.tempMethodParamNum + this.currentArithType + " :=" + this.currentArithType + " ";
            }
            if (data.get(0).equals("LocalVariable")) {
                ollirCode.append(OllirTemplates.createMemberAccess(tempVarSent, parametersTempVariables, firstChildStr, memberAccessed, parametersString, this.currentArithType, "import"));
            }
            else if (data.get(0).equals("ReturnObj")) {
                ollirCode.append(OllirTemplates.createMemberAccess(tempVarSent, parametersTempVariables, firstChildStr, memberAccessed, parametersString, this.currentArithType, "import"));
            }
            else {
                ollirCode.append(OllirTemplates.createMemberAccess("", new ArrayList<String>(), firstChildStr, memberAccessed, parametersString, this.currentArithType, "import"));
            }
        } else {
            String tempVarSent = "t" + this.tempMethodParamNum + this.currentArithType + " :=" + this.currentArithType + " ";
            if (data.get(0).equals("LocalVariable")) {
                ollirCode.append(OllirTemplates.createMemberAccess(tempVarSent, parametersTempVariables, firstChildStr, memberAccessed, parametersString, this.currentArithType, ""));
            } else {
                ollirCode.append(OllirTemplates.createMemberAccess("", new ArrayList<String>(), firstChildStr, memberAccessed, parametersString, this.currentArithType, ""));
            }

        }

        System.out.println("ollirCode(dealWithMemberAccess): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithBinaryOp(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithBinaryOp() function! (" + node + ")");
        StringBuilder ollirCode = new StringBuilder();

        String binaryOp = node.get("op");
        JmmNode leftExpr = node.getChildren().get(0);
        JmmNode rightExpr = node.getChildren().get(1);

        String leftExpressOllirStr = (String) visit(leftExpr, Collections.singletonList("BinaryOp")).get(0);
        String rightExpressOllirStr = (String) visit(rightExpr, Collections.singletonList("BinaryOp")).get(0);

        System.out.println("leftExpressOllirStr: " + leftExpressOllirStr);
        System.out.println("rightExpressOllirStr: " + rightExpressOllirStr);

        boolean leftIsTerminalSymbol = (leftExpr.getKind().equals("Integer") || leftExpr.getKind().equals("Identifier"));
        boolean rightIsTerminalSymbol = (rightExpr.getKind().equals("Integer") || rightExpr.getKind().equals("Identifier"));

        if (leftIsTerminalSymbol && rightIsTerminalSymbol) { // terminal nodes
            System.out.println("data.get(0) = " + data.get(0));
            if (data.get(0).equals("LocalVariable")) {
                this.tempMethodParamNum++;
                ollirCode.append("t" + this.tempMethodParamNum + this.currentArithType + " :=" + this.currentArithType + " " + leftExpressOllirStr + " " + binaryOp + this.currentArithType + " " + rightExpressOllirStr + ";\n");
            } else {
                this.tempMethodParamNum++;
                String rightSide = leftExpressOllirStr + " " + binaryOp + this.currentArithType + " " + rightExpressOllirStr;
                ollirCode.append(OllirTemplates.temporaryVariableTemplate(this.tempMethodParamNum,  this.currentArithType, rightSide));
            }
        } else if (!leftIsTerminalSymbol && rightIsTerminalSymbol) {
            ollirCode.append(leftExpressOllirStr);
            String rightSide = ("t" + this.tempMethodParamNum + this.currentArithType) + " " + (binaryOp + this.currentArithType) + " " + rightExpressOllirStr;
            this.tempMethodParamNum++;
            ollirCode.append(OllirTemplates.temporaryVariableTemplate(this.tempMethodParamNum, this.currentArithType, rightSide));
        } else if (leftIsTerminalSymbol && !rightIsTerminalSymbol) {
            ollirCode.append(rightExpressOllirStr);
            String rightSide = leftExpressOllirStr + " " + (binaryOp + this.currentArithType) + " " + ("t" + this.tempMethodParamNum + this.currentArithType);
            this.tempMethodParamNum++;
            ollirCode.append(OllirTemplates.temporaryVariableTemplate(this.tempMethodParamNum, this.currentArithType, rightSide));
        } else { // both sides are not terminal symbols
            ollirCode.append(leftExpr);
            ollirCode.append(rightExpr);
            int temporaryVar1 = this.tempMethodParamNum - 1; // left temporary variable
            int temporaryVar2 = this.tempMethodParamNum;
            String rightSide = ("t" + temporaryVar1 + this.currentArithType) + " " + (binaryOp + this.currentArithType) + " " + ("t" + temporaryVar2 + this.currentArithType);
            this.tempMethodParamNum++;
            ollirCode.append(OllirTemplates.temporaryVariableTemplate(this.tempMethodParamNum, this.currentArithType, rightSide));
        }

        System.out.println("ollirCode(dealWithBinaryOp): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithExprParentheses(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithExprParentheses() function! (" + node + ")");
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append((data.get(0).equals("BinaryOp") || data.get(0).equals("LocalVariable")) ? "" : ollirCode.append("("));

        String expressionOllirCode = (String) visit(node.getChildren().get(0), data).get(0);

        ollirCode.append(expressionOllirCode); // (temp<num>.type :=.type leftChild + op + rightChild;\n) OR (io.println())
        ollirCode.append((data.get(0).equals("BinaryOp") || data.get(0).equals("LocalVariable")) ? "" : (ollirCode.append(")") + this.currentArithType));

        System.out.println("ollirCode(dealWithExprParentheses): " + ollirCode);
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithSingleExpression(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithSingleExpression() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();
        String returnTypeObj;

        String returnVal = node.get("val");

        switch (node.getKind()) {
            case "Integer" :
                ollirCode.append(returnVal + ".i32");
                this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? ".i32" : "";
                break;
            case "Bool":
                ollirCode.append(returnVal + ".bool");
                this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? ".bool" : "";
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
                    this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? returnTypeObj : "";
                } else if (parameterSymbol != null) {  // Method parameter
                    this.tempMethodParamNum++;
                    int paramIndex = this.currentMethod.getParameterIndex(returnVal);
                    ollirCode.append(OllirTemplates.variableCall(parameterSymbol.getType(), returnVal, paramIndex));
                    returnTypeObj = OllirTemplates.variableType(parameterSymbol.getType().getName());
                    this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? returnTypeObj : "";
                } else if (classField != null) { // Class field
                    this.tempMethodParamNum++;
                    ollirCode.append(OllirTemplates.variableCall(classField.getType(), returnVal));
                    returnTypeObj = OllirTemplates.variableType(classField.getType().getName());
                    this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess") || data.get(0).equals("BinaryOp")) ? returnTypeObj : "";
                } else {
                    ollirCode.append(returnVal);
                }
            default:
                break;
        }

        System.out.println("ollirCode(dealWithSingleExpression): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithNewObject(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithNewObject() function! (" + node + ")");
        StringBuilder ollirCode = new StringBuilder();

        String objClassName = node.get("val");
        this.tempMethodParamNum++;
        ollirCode.append(OllirTemplates.newObjectTemplate(this.tempMethodParamNum, objClassName));

        System.out.println("ollirCode(dealWithNewObject): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        return Collections.singletonList("");
    }
}
