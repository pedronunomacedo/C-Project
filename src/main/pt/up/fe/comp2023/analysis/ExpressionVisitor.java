package pt.up.fe.comp2023.analysis;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2023.ast.JmmMethod;

import java.util.Arrays;
import java.util.List;


public class ExpressionVisitor extends AJmmVisitor<Type, Type> {

    protected Analysis analysis;
    protected boolean isVariable;
    public ExpressionVisitor(Analysis analysis) {
        super();
        this.analysis = analysis;
        setDefaultVisit(this::defaultVisit);
    }

    protected Type defaultVisit(JmmNode node, Type method) {
        return visit(node.getChildren().get(0), method);
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Identifier", this::dealWithSingleExpression);
        addVisit("Bool", this::dealWithSingleExpression);
        addVisit("Integer", this::dealWithSingleExpression);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("NewArrayObject", this::dealWithNewArrayObjectExpression);
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("MemberAccess", this::dealWithMemberAccess);
        addVisit("Lenght", this::dealWithLenght);
        addVisit("Array", this::dealWithArray);
    }

    private Type dealWithBinaryOp(JmmNode node, Type method) {
        JmmNode leftChild = node.getJmmChild(0);
        JmmNode rightChild = node.getJmmChild(1);
        Type leftSideType = visit(leftChild, method);
        Type rightSideType = visit(rightChild, method);

        switch (node.get("op")) {
            case "&&", "||" -> {
                if (leftSideType == null || rightSideType == null) {
                    analysis.newReport(leftChild, "Types of both sides don't match");
                    return new Type("boolean", false);
                }
                if (!leftSideType.getName().equals("boolean") || leftSideType.isArray() || !rightSideType.getName().equals("boolean") || rightSideType.isArray()) {
                    analysis.newReport(node, "Invalid Type on Binary Operation");
                }
                return new Type("boolean", false);
            }
            case "+=", "-=", "*=", "/=", "*", "/", "%", "+", "-", "<", ">", "<=", ">=" -> {
                if (leftSideType == null || rightSideType == null) {
                    analysis.newReport(leftChild, "Types of both sides don't match");
                    return new Type("boolean", false);
                }
                if (!leftSideType.getName().equals("int") || leftSideType.isArray() || !rightSideType.getName().equals("int") || rightSideType.isArray()) {
                    analysis.newReport(node, "Invalid Type on Binary Operation");
                }
                return new Type("int", false);
            }
        }

        return rightSideType;
    }

    private Type dealWithSingleExpression(JmmNode node, Type method) {
        Type type = null;
        switch (node.getKind()) {
            case "Integer":
                type = new Type("int", false);
                break;
            case "Bool":
                type = new Type("boolean", false);
                break;
            case "Identifier":
                String val = node.get("val");
                System.out.println("val: " + val);
                Pair<String, Symbol> pair = analysis.getSymbolTable().variableScope(analysis.getSymbolTable().getCurrentMethod(), val);
                Symbol variable = pair.b;

                System.out.println("variable: " + variable);

                if (variable == null) {
                    // Check if it comes from the imports
                    if (this.analysis.getSymbolTable().getImports().contains(val)) {
                        isVariable = false;
                        return new Type(val, false);
                    } else {
                        analysis.newReport(node, "Variable " + val + " not declared");
                    }
                } else {
                    this.isVariable = true;
                    type = variable.getType();
                }
                break;
        };

        return type;
    }

    private Type dealWithNewObject(JmmNode node, Type method) {
        String val = node.get("val");

        if (!(this.analysis.getSymbolTable().getImports().contains(val) || this.analysis.getSymbolTable().getClassName().equals(val) || this.analysis.getSymbolTable().getSuper().equals(val))) {
            analysis.newReport(node, "Class " + val + " not found for object declaration");
            return null;
        }

        return new Type(val, false);
    }

    private Type dealWithNewArrayObjectExpression(JmmNode node, Type method) {
        Type indexType = visit(node.getJmmChild(0), method);

        if (!indexType.getName().equals("int")) {
            analysis.newReport(node, "Array index must be of type in but found " + indexType.getName());
        }

        return indexType;
    }

    private Type dealWithUnaryOp(JmmNode node, Type method) {
        Type exprType = visit(node.getJmmChild(0), method);

        if (!exprType.getName().equals("boolean")) {
            analysis.newReport(node, "UnaryOp type must be of type boolean, but found " + exprType.getName());
        }

        return exprType;
    }

    private Type dealWithMemberAccess(JmmNode node, Type method) {
        Type objectType = visit(node.getJmmChild(0), method);

        System.out.println("objectType: " + objectType);

        if (objectType == null && isVariable) {
            analysis.newReport(node, "objectType is null");
            return null;
        } else if (objectType != null) {
            if (Arrays.asList("int", "boolean").contains(objectType.getName())) {
                analysis.newReport(node, "Object member access must be a string but found " + objectType.getName());
                return objectType;
            }
        }
        this.isVariable = false;

        String methodName = node.get("id");
        JmmMethod accessedMethod = this.analysis.getSymbolTable().getMethod(methodName);

        if (accessedMethod == null && this.analysis.getSymbolTable().getSuper() == null && !this.analysis.getSymbolTable().getImports().contains(objectType.getName())) {
            analysis.newReport(node, "Method accessed " + methodName + " not found");
            return objectType;
        }

        if (accessedMethod == null) {
            //analysis.newReport(node, "Method accessed " + methodName + " not found");
            //System.out.println("Gonzallito666 ---> " + accessedMethod);
            Type type = this.analysis.getCurrMethod().getReturnType();
            return type;

        }

        List<Symbol> methodParams = accessedMethod.getParameters();
        if ((node.getNumChildren() - 1) != methodParams.size()) {
            analysis.newReport(node, "Expected " + methodParams.size() + " parameters but received " + (node.getNumChildren() - 1) + " parameters");
            return objectType;
        }

        for (Symbol parameter : methodParams) {
            Type paramType = visit(node.getJmmChild(1 + methodParams.indexOf(parameter)), method);
            Symbol param = accessedMethod.getParameter(parameter.getName());
            Type expectedType = param.getType();

            if (!paramType.equals(expectedType)) {
                analysis.newReport(node, "Expected parameter of type " + expectedType.getName() + " in method " + accessedMethod + " but found " + paramType.getName());
            }
        }

        return objectType;
    }

    private Type dealWithLenght(JmmNode node, Type method) {
        Type objectType = visit(node.getJmmChild(0), method);

        if (objectType.equals(new Type(objectType.getName(), true))) { // Suspicious!!! (only accepts length of arrays)
            analysis.newReport(node, "Object must be a string but found " + objectType.getName());
        }

        return objectType;
    }

    private Type dealWithArray(JmmNode node, Type method) {
        Type arrayType = visit(node.getJmmChild(0));
        Type indexType = visit(node.getJmmChild(1));

        if (arrayType == null) {
            analysis.newReport(node, "arrayType is null");
            return null;
        }

        if (indexType == null) {
            analysis.newReport(node, "indexType is null");
            return null;
        }

        if (!arrayType.equals(new Type(arrayType.getName(), true))) {
            analysis.newReport(node, "Array object must be an array but found " + arrayType.getName());
            return arrayType;
        }

        if (!indexType.getName().equals("int")) {
            analysis.newReport(node, "Array index expression is not Integer");
            return arrayType;
        }

        return arrayType;
    }

}
