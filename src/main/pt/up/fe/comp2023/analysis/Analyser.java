package pt.up.fe.comp2023.analysis;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.ast.JmmMethod;
import pt.up.fe.comp2023.ast.JmmSymbolTable;

import java.util.Arrays;
import java.util.List;

public class Analyser extends AJmmVisitor<String, Void> {


    private ExpressionVisitor expressionVisitor;
    private Analysis analysis;


    public Analyser(JmmSymbolTable symbolTable) {
        super();
        this.analysis = new Analysis(symbolTable);
        expressionVisitor = new ExpressionVisitor(analysis);
        setDefaultVisit(this::defaultVisit);
    }

    public void analyse(JmmNode node) {
        dealWithProgram(node, null);
    }

    public List<Report> getReports() {
        return analysis.getReports();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("LocalVariables", this::dealWithLocalVariables);
        addVisit("Statement", this::dealWithStatement);
    }

    private Void defaultVisit(JmmNode node, String method) {
        return visitAllChildren(node, method);
    }

    private Void dealWithProgram(JmmNode node, String method) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Import")) continue;
            else visit(child, method);
        }
        return null;
    }

    private Void dealWithClass(JmmNode node, String method) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ExtendedClass")) continue;
            else if (child.getKind().equals("VariableDeclaration")) continue;
            else visit(child, method);
        }
        return null;
    }

    private Void dealWithMethodDeclaration(JmmNode node, String method) {
        JmmMethod currMethod = analysis.getSymbolTable().getMethod(node.get("methodName"));
        analysis.setCurrMethod(currMethod);
        analysis.getSymbolTable().setCurrentMethod(currMethod);
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ReturnType")) continue;
            else if (child.getKind().equals("ClassParameters")) continue;
            else if (child.getKind().equals("ReturnObj")) {
                Type type = this.expressionVisitor.visit(child.getJmmChild(0));

                if (type != null) {
                    Type methodType = analysis.getSymbolTable().getMethod(node.get("methodName")).getReturnType();
                    if (!type.equals(methodType)) {
                        analysis.newReport(child.getJmmChild(0), "Return type does not match method type");
                    }
                }
            }
            else visit(child, method);
        }
        return null;
    }

    private Void dealWithLocalVariables(JmmNode node, String method) {
        String varName = node.get("varName");

        Pair<String, Symbol> pair = analysis.getSymbolTable().variableScope(analysis.getSymbolTable().getCurrentMethod(), varName);
        Symbol variable = pair.b;

        if (variable == null) {
            analysis.newReport(node, "Variable " + varName + " not declared");
        }

        Type varType = variable.getType();

        if (node.getNumChildren() > 1) {
            JmmNode nodeExpr = node.getJmmChild(1);
            Type expressionType = this.expressionVisitor.visit(nodeExpr);

            System.out.println("variable: " + variable.getType());
            System.out.println("expressionType: " + expressionType);

            if (expressionType == null) {
                analysis.newReport(node, "1) expressionTYpe is null");
                return  null;
            }

            if (!expressionType.equals(varType)) {
                analysis.newReport(node, "Expression " + expressionType.getName() + " is not Assignment Type");
            }
        }
        return null;
    }

    private Void dealWithStatement(JmmNode node, String method) {
        switch (node.getKind()) {
            case "Conditional":
                Type expressionTypeIf = this.expressionVisitor.visit(node.getJmmChild(0));
                if (!expressionTypeIf.equals("boolean")) {
                    analysis.newReport(node, " If Expression " + expressionTypeIf.getName() + " is not Boolean");
                }
                break;

            case "Loop":
                Type expressionTypeWhile = this.expressionVisitor.visit(node.getJmmChild(0));
                if (!expressionTypeWhile.equals("boolean")) {
                    analysis.newReport(node, " While Expression " + expressionTypeWhile.getName() + " is not Boolean");
                }
                break;

            case "ArrayAssignment":
                String varName = node.get("varName");
                Pair<String, Symbol> pair = analysis.getSymbolTable().variableScope(analysis.getSymbolTable().getCurrentMethod(), varName);
                String varScope = pair.a;
                Symbol variable = pair.b;

                if (variable == null) {
                    analysis.newReport(node, "1) Variable " + varName + " not declared");
                } else {
                    Type varType = new Type(variable.getType().getName(), false); // don't forget that it's an array, and you need to create a new type that is not an array and with name given by the variable
                    /*
                    if (!varType.isArray()) { // It's not supposed to be an array!
                        analysis.newReport(node, "2) Variable assignment is not Array");
                    }
                     */

                    Type indexType = this.expressionVisitor.visit(node.getJmmChild(0));
                    if (!indexType.getName().equals("int")) {
                        analysis.newReport(node, "3) Array index expression is not Integer");
                    }
                    else {
                        Type expressionType = this.expressionVisitor.visit(node.getJmmChild(1));
                        System.out.println("variable.getType(): " + variable.getType());
                        System.out.println("expressionType.getType(): " + expressionType);


                        if (expressionType == null) {
                            analysis.newReport(node, "4) expressionTYpe is null");
                            return null;
                        }
                        if (!varType.equals(expressionType)) {
                            analysis.newReport(node, "5) Expression " + expressionType.getName() + " is not Array Type");
                        }
                    }
                }
                break;

            case "Assignment":
                String varName2 = node.get("varName");
                Pair<String, Symbol> pair2 = analysis.getSymbolTable().variableScope(analysis.getSymbolTable().getCurrentMethod(), varName2);
                Symbol variable2 = pair2.b;

                if (variable2 == null) {
                    analysis.newReport(node, "Variable " + varName2 + " not declared");
                } else {
                    Type varType2 = variable2.getType();
                    Type expressionType = this.expressionVisitor.visit(node.getJmmChild(0));

                    System.out.println("variable2: " + variable2.getType());
                    System.out.println("expressionType: " + expressionType);

                    if (expressionType == null) {
                        analysis.newReport(node, "3) ExpressionType is null");
                        return null;
                    } else if (expressionType.getName().equals("this")) { // Child was a SelfCall
                        expressionType = new Type(this.analysis.getSymbolTable().getClassName(), false);
                    }

                    if (Arrays.asList("int", "boolean").contains(variable2.getType().getName()) && expressionType.getName().equals(variable2.getType().getName())) {
                        return null;
                    }

                    if (!((variable2.getType().getName().equals(this.analysis.getSymbolTable().getSuper()) || this.analysis.getSymbolTable().getImports().contains(variable2.getType().getName()) || this.analysis.getSymbolTable().getClassName().equals(variable2.getType().getName()))
                            &&
                            (expressionType.getName().equals(this.analysis.getSymbolTable().getClassName()) || this.analysis.getSymbolTable().getImports().contains(expressionType.getName())))) {
                        analysis.newReport(node, "1ExpressionType is not Assignment Type (rightSide type: " + variable2.getType().getName() + ", leftSide type: " + expressionType.getName());
                        return null;
                    }

                    if (!varType2.equals(expressionType) && !this.analysis.getSymbolTable().getImports().contains(variable2.getType().getName()) && !this.analysis.getSymbolTable().getImports().contains(expressionType.getName())) {
                        analysis.newReport(node, "2ExpressionType is not Assignment Type");
                        return null;
                    }

                    if (!expressionType.getName().equals(this.analysis.getSymbolTable().getSuper()) && !varType2.getName().equals(this.analysis.getSymbolTable().getSuper()) && !varType2.getName().equals(expressionType.getName())
                            &&
                            (!this.analysis.getSymbolTable().getImports().contains(variable2.getType().getName()) || !this.analysis.getSymbolTable().getImports().contains(expressionType.getName()))) {
                        analysis.newReport(node, "3ExpressionType is not Assignment Type");
                        return null;
                    }
                }
                break;

            case "Expr":
                this.expressionVisitor.visit(node.getJmmChild(0));
                break;
        }

        return null;
    }

}
