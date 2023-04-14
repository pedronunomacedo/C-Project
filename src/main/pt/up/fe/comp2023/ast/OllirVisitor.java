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
        addVisit("VarDeclar", this::dealWithVarDeclaration);
        addVisit("Expr", this::dealWithExpression);

        // Expression rules
        /*
        addVisit("Array", this::dealWithArrayDeclaration);
        addVisit("Lenght", this::dealWithExpression);

        addVisit("UnaryOp", this::dealWithExpression);
        addVisit("NewArrayObject", this::dealWithExpression);
        addVisit("NewObject", this::dealWithExpression);
         */
        addVisit("MemberAccess", this::dealWithMemberAccess);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("ExprParentheses", this::dealWithExprParentheses);
        addVisit("Integer", this::dealWithSingleExpression);
        addVisit("Bool", this::dealWithSingleExpression);
        addVisit("SelfCall", this::dealWithSingleExpression);
        addVisit("Identifier", this::dealWithSingleExpression);


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
        if (this.scope.equals("METHOD")) {
            // If it's a variable declaration inside a method, you are not supposed to add it in the OLLIR code
            if (node.getAttributes().contains("val")) { // variable assignment
                String varValue = node.get("val");
                // Make lookup for finding the variable (Search on the localMethodVariables and the methodParameters)
                Symbol localVariable = this.currentMethod.getLocalVariable(varName);
                Symbol classField = this.symbolTable.getField(varName);
                Boolean methodParamBool = this.currentMethod.getParametersNames().contains(varName);

                // lookup for the localVariable
                if (localVariable == null && !methodParamBool && classField != null) { // class field
                    String ollirVarCode = OllirTemplates.putField(classField, varValue);
                    ollirCode.append(ollirVarCode);
                } else if (localVariable == null && methodParamBool) { // method parameter
                    Symbol parameter = this.currentMethod.getParameter(varName);
                    String ollirVarCode = OllirTemplates.putField(classField, varValue, this.currentMethod.getParameters().indexOf(parameter) + 1);
                    ollirCode.append(ollirVarCode);
                } else if (localVariable != null) { // method local variables assignments
                    String ollirLocalVarCode = OllirTemplates.localVariableAssignment(localVariable, varValue);
                    ollirCode.append(ollirLocalVarCode);
                }
            }
        }

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
                    String localVarOllirCode = (String) visit(child, Collections.singletonList("")).get(0);
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
                if (child.getKind().equals("ReturnType") || child.getKind().equals("ClassParameters")) {
                    visit(child, Collections.singletonList("")); // ignore the child
                } else if (child.getKind().equals("ReturnObj")) {
                    JmmNode expressionNode = child.getChildren().get(0);
                    this.dealWithReturnType = true;
                    String returnObjStr = (String) visit(child.getChildren().get(0), Collections.singletonList("")).get(0);
                    if (Arrays.asList("Array", "Lenght", "MemberAccess", "UnaryOp", "BinaryOp", "NewArrayObject", "NewObject").contains(expressionNode.getKind())) {
                        this.tempMethodParamNum++;
                        ollirCode.append(OllirTemplates.createOpAssignment(this.currentArithType, this.tempMethodParamNum, returnObjStr));
                        ollirCode.append(OllirTemplates.returnTemplate(this.currentArithType + " t" + this.tempMethodParamNum + this.currentArithType));
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
        String indexValueOllirCode = new String();
        String valueOllirCode = new String();
        String indexValue = new String();
        String value = new String();


        String varName = node.get("varName"); // Name of the List
        JmmNode indexNode = node.getChildren().get(0);
        JmmNode valueNode = node.getChildren().get(1);
        if (Arrays.asList("Integer", "Bool", "Identifier", "SelfCall").contains(indexNode.getKind())) {
            indexValue = indexNode.get("val");
        } else {
            // Deal with the other "expression" types
        }

        if (Arrays.asList("Integer", "Bool", "Identifier", "SelfCall").contains(valueNode.getKind())) {
            value = valueNode.get("val");
        } else {
            // Deal with the other "expression" types
        }

        Symbol variable = this.currentMethod.getLocalVariable(varName);
        ollirCode.append(OllirTemplates.variableAssignment(variable, indexValue, value));

        System.out.println("ollirCode(dealWithArrayAssignment): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithAssignment(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);

        StringBuilder ollirCode = new StringBuilder();

        String varName = node.get("varName");
        System.out.println("node.getChildren(): " + node.getChildren());
        String newValueOllirCode = (String) visit(node.getChildren().get(0), Collections.singletonList("")).get(0);
        Symbol variable = this.currentMethod.getLocalVariable(varName);
        ollirCode.append(OllirTemplates.variableAssignment(variable, null, newValueOllirCode));

        return Collections.singletonList(ollirCode.toString());
    }

    public List<Object> dealWithVarDeclaration(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithVarDeclaration() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        System.out.println("ollirCode(dealWithVarDeclaration): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithExpression(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithExpression() function! (" + node + ")");

        StringBuilder ollirCode = new StringBuilder();

        String childOllirCode = (String) visit(node.getChildren().get(0)).get(0);
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

        String firstChildStr = (String) visit(firstChild).get(0);

        for (int i = 1; i < node.getChildren().size(); i++) {
            this.tempMethodParamNum++;
            String paramOllirCode = (String) visit(node.getChildren().get(i), Collections.singletonList("MemberAccess")).get(0);
            String tempVariableString = OllirTemplates.createOpAssignment(this.currentArithType, this.tempMethodParamNum, paramOllirCode);
            parameters.add("t" + this.tempMethodParamNum + this.currentArithType);
            parametersTempVariables.add(tempVariableString);
        }
        parametersString = String.join(", ", parameters);

        ollirCode.append(OllirTemplates.createMemberAccess(parametersTempVariables, firstChildStr, memberAccessed, parametersString, this.currentArithType));

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

        ollirCode.append(leftExpressOllirStr).append(" ").append(binaryOp).append(this.currentArithType).append(" ").append(rightExpressOllirStr);

        System.out.println("ollirCode(dealWithBinaryOp): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> dealWithExprParentheses(JmmNode node, List<Object> data) {
        if (nodesVisited.contains(node)) return Collections.singletonList("DEFAULT_VISIT");
        this.nodesVisited.add(node);
        System.out.println("-> In dealWithExprParentheses() function! (" + node + ")");
        StringBuilder ollirCode = new StringBuilder();

        ollirCode.append("(");

        String expressionOllirCode = (String) visit(node.getChildren().get(0), data).get(0);

        ollirCode.append(expressionOllirCode);
        ollirCode.append(")");

        System.out.println("ollirCode(dealWithExpression): " + ollirCode.toString());
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
                this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess")) ? ".i32" : "";
                break;
            case "Bool":
                ollirCode.append(returnVal + ".bool");
                this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess")) ? ".bool" : "";
                break;
            case "Identifier":
                // Check if returnVal corresponds to a local variable, or to a method parameter or to a class field
                Symbol localVarSymbol = this.currentMethod.getLocalVariable(returnVal);
                Symbol parameterSymbol = this.currentMethod.getParameter(returnVal);
                Symbol classField = this.symbolTable.getField(returnVal);

                if (localVarSymbol != null) { // Local variable
                    ollirCode.append(OllirTemplates.variableCall(localVarSymbol.getType(), returnVal));
                    returnTypeObj = OllirTemplates.variableType(localVarSymbol.getType().getName());
                    this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess")) ? returnTypeObj : "";
                } else if (parameterSymbol != null) {  // Method parameter
                    this.tempMethodParamNum++;
                    int paramIndex = this.currentMethod.getParameterIndex(returnVal);
                    ollirCode.append(OllirTemplates.variableCall(parameterSymbol.getType(), returnVal, paramIndex));
                    returnTypeObj = OllirTemplates.variableType(parameterSymbol.getType().getName());
                    this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess")) ? returnTypeObj : "";
                } else if (classField != null) { // Class field
                    this.tempMethodParamNum++;
                    ollirCode.append(OllirTemplates.variableCall(classField.getType(), returnVal));
                    returnTypeObj = OllirTemplates.variableType(classField.getType().getName());
                    this.currentArithType = (this.dealWithReturnType || data.get(0).equals("MemberAccess")) ? returnTypeObj : "";
                } else {
                    ollirCode.append(returnVal);
                }

                break;
            default :
                break;
        }

        System.out.println("ollirCode(dealWithSingleExpression): " + ollirCode.toString());
        return Collections.singletonList(ollirCode.toString());
    }

    private List<Object> defaultVisit(JmmNode node, List<Object> data) {
        return Collections.singletonList("");
    }
}
