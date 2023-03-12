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
        addVisit("TypeDeclaration", this::dealWithTypeDeclaration);
    }

    public JmmSymbolTable getSymbolTable(JmmNode node) {
        visit(node);
        return symbolTable;
    }


    private String dealWithProgram(JmmNode node, String space) {
        System.out.println("-> In dealWithProgram() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        System.out.println("ROOT: " + node.getKind());

        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Import")) { // Import
                System.out.println("Child_root: " + child.getKind());
                String path = child.get("id");
                for (JmmNode grandChild : child.getChildren()) { // SubImports
                    System.out.println("GrandChild_root: " + grandChild.getKind());
                    path += '.' + grandChild.get("id");
                }

                System.out.println("Adding import statement");
                this.symbolTable.addImport(path);
            }
            else {
                System.out.println("Visiting child " + child.getKind() + " of " + node.getKind());
                visit(child, null);
            }
        }

        return null;
    }

    public String dealWithClassDeclaration(JmmNode node, String space) {
        System.out.println("-> In dealWithClassDeclaration() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        this.scope = "CLASS";

        System.out.println("Child of " + node.getKind() + " are: " + node.getChildren().size());

        var className = node.get("className");
        this.symbolTable.addClassName(className);


        for (JmmNode child : node.getChildren()) {
            System.out.println("Child of " + node.getKind() + " is " + child.getKind());
            visit(child, null);
        }

        return null;
    }

    public String dealWithExtendedClassDeclaration(JmmNode node, String space) {
        System.out.println("-> In dealWithExtendedClassDeclaration() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        // deal with extended classes
        var extendedClassName = node.get("extendedClassName");
        this.symbolTable.addSuperClassName(extendedClassName);

        return null;
    }

    public String dealWithVariableDeclaration(JmmNode node, String space) {
        System.out.println("-> In dealWithVariableDeclaration() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        String variableName = node.get("varName");
        System.out.println("Variable name: " + variableName);


        if (this.scope.equals("CLASS")) {

            for (JmmNode child : node.getChildren()) {
                String variableKind = child.getKind();
                Type variableType = JmmSymbolTable.getType(child, "typeName");

                Type varType = new Type(variableName, child.getKind().equals("IntegerArrayType"));
                Symbol field = new Symbol(varType, variableName);

                this.symbolTable.addClassField(field);
            }
        }

        return null;
    }

    public String dealWithMethodDeclaration(JmmNode node, String space) {
        System.out.println("-> In dealWithMethodDeclaration() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        String nodeKind = node.getKind();

        if (nodeKind.equals("MethodDeclarationMain")) { // MethodDeclarationMain
            System.out.println("---- MAIN METHOD DECLARATION ----");
            this.scope = "MAIN";

            this.symbolTable.addMethod("main", new Type("void", false));

            node.put("params", "");

        } else { // MethodDeclarationOther
            System.out.println("---- METHOD DECLARATION ----");
            this.scope = "METHOD";

            System.out.println("Node attributes: " + node.getAttributes());

            String methodName = node.get("methodName");
            System.out.println("Method name: " + methodName);
            this.symbolTable.addMethod(methodName, new Type("", false));

            System.out.println("Children node: " + node.getChildren());
        }

        return null;
    }

    public String dealWithClassParameters(JmmNode node, String space) {
        if (scope.equals("METHOD")) {
            System.out.println("METHOD kids: " + node.getChildren());
            this.symbolTable.getCurrentMethod().addParameter(new Symbol(new Type(node.get("keyType"), false), node.get("value")));
            System.out.println("Child parent: " + node.getJmmParent());
        }

        return null;
    }

    public String dealWithTypeDeclaration(JmmNode node, String space) {
        if (scope.equals("METHOD")) {
            System.out.println("Dealing with TypeDeclaration");
            this.symbolTable.getCurrentMethod().setReturnType(JmmSymbolTable.getType(node, "typeName"));
        }

        return null;
    }
}
