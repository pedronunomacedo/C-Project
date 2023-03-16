package pt.up.fe.comp2023.symbolTable;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.lang.model.type.NullType;
import java.util.*;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends AJmmVisitor<String, String> {
    private JmmSymbolTable symbolTable;
    private List<Report> reports;
    private String scope;

    public SymbolTableVisitor() {
        this.symbolTable = new JmmSymbolTable();
    }

    @Override
    public void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("ExtendedClass", this::dealWithExtendedClassDeclaration);
        addVisit("VariableDeclaration", this::dealWithVariableDeclaration);
        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("ClassParameters", this::dealWithClassParameters);
        addVisit("LocalVariables", this::dealWithLocalVariables);
        addVisit("Statement", this::dealWithStatement);
    }

    public JmmSymbolTable getSymbolTable(JmmNode node) {
        visit(node);
        return symbolTable;
    }


    private String dealWithProgram(JmmNode node, String space) {
        space = ((space != null) ? space : "");

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Import")) { // Import
                String path = child.get("id");
                for (JmmNode grandChild : child.getChildren()) { // SubImports
                    path += '.' + grandChild.get("id");
                }

                this.symbolTable.addImport(path);
            }
            else { // Class
                visit(child, null);
            }
        }

        return null;
    }

    public String dealWithClassDeclaration(JmmNode node, String space) {
        space = ((space != null) ? space : "");

        this.scope = "CLASS";

        var className = node.get("className");
        this.symbolTable.addClassName(className);


        for (JmmNode child : node.getChildren()) {
            visit(child, null);
        }

        return null;
    }

    public String dealWithExtendedClassDeclaration(JmmNode node, String space) {
        space = ((space != null) ? space : "");

        // deal with extended classes
        var extendedClassName = node.get("extendedClassName");
        this.symbolTable.addSuperClassName(extendedClassName);

        return null;
    }

    public String dealWithVariableDeclaration(JmmNode node, String space) {
        space = ((space != null) ? space : "");

        String variableName = node.get("varName");


        if (this.scope.equals("CLASS")) {
            for (JmmNode child : node.getChildren()) {
                Type varType = new Type(JmmSymbolTable.getType(child, "typeName").getName(), child.getKind().equals("IntegerArrayType"));
                Symbol field = new Symbol(varType, variableName);

                this.symbolTable.addClassField(field);
            }
        }

        return null;
    }

    public String dealWithMethodDeclaration(JmmNode node, String space) {
        space = ((space != null) ? space : "");
        this.scope = "METHOD";

        String nodeKind = node.getKind();

        if (nodeKind.equals("MethodDeclarationMain")) { // MethodDeclarationMain
            this.scope = "MAIN";

            this.symbolTable.addMethod("main", new Type("void", false));
            node.put("params", "");

        } else { // MethodDeclarationOther
            this.scope = "METHOD";

            String methodName = node.get("methodName");

            // Visit the children of the node
            for (JmmNode child: node.getChildren()) {
                if (child.getKind().equals("ReturnType")) {
                    Type returnType = JmmSymbolTable.getType(child.getChildren().get(0), "typeName");
                    this.symbolTable.addMethod(methodName, returnType);
                } else if (child.getKind().equals("ReturnObj")) {
                    continue; // ignore
                } else {
                    visit(child);
                }
            }
        }

        return null;
    }

    public String dealWithClassParameters(JmmNode node, String space) {
        space = ((space != null) ? space : "");

        if (scope.equals("METHOD")) {
            // this.symbolTable.getCurrentMethod().addParameter(new Symbol(new Type(node.get("keyType"), false), node.get("value")));
            var parameterType = node.getChildren().get(0);
            var parameterValue = node.get("value");

            Type type = JmmSymbolTable.getType(parameterType, "typeName");

            Symbol symbol = new Symbol(type, parameterValue);

            this.symbolTable.getCurrentMethod().addParameter(symbol);
        }

        return null;
    }

    public String dealWithLocalVariables(JmmNode node, String space) {
        space = ((space != null) ? space : "");

        if (scope.equals("METHOD")) {
            if (node.getChildren().size() > 0) {
                String variableName = node.get("varName");

                Type localVarType = JmmSymbolTable.getType(node.getChildren().get(0), "typeName");
                Symbol localVarSymbol = new Symbol(localVarType, variableName);
                this.symbolTable.getCurrentMethod().addLocalVariable(localVarSymbol);
            } else {
                String variableName = node.get("varName");

                Symbol localVarSymbol = new Symbol(new Type("", false), variableName);
                this.symbolTable.getCurrentMethod().addLocalVariable(localVarSymbol);
            }
        }

        return null;
    }

    public String dealWithStatement(JmmNode node, String space) {
        space = ((space != null) ? space : "");

        return null;
    }
}
