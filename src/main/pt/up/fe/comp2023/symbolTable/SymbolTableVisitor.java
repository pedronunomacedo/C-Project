package pt.up.fe.comp2023.symbolTable;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;
import java.util.stream.Collectors;

public class SymbolTableVisitor extends AJmmVisitor<String, String> {
    private JmmSymbolTable symbolTable;
    private List<Report> reports;

    public SymbolTableVisitor() {
        this.symbolTable = new JmmSymbolTable();
    }

    @Override
    public void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("ExtendedClass", this::dealWithExtendedClassDeclaration);
        addVisit("VariableDeclaration", this::dealWithVariableDeclaration);
    }

    public JmmSymbolTable getSymbolTable(JmmNode node) {
        visit(node);
        return symbolTable;
    }




    private String dealWithProgram(JmmNode node, String space) {
        System.out.println("In dealWithProgram() function! (" + node.getKind() + ")");
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
        System.out.println("In dealWithClassDeclaration() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        System.out.println("Child of " + node.getKind() + " are: " + node.getChildren().size());

        var className = node.get("className");
        this.symbolTable.addClassName(className);


        for (JmmNode child : node.getChildren()) {
            visit(child, null);
        }

        return null;
    }

    public String dealWithExtendedClassDeclaration(JmmNode node, String space) {
        System.out.println("In dealWithExtendedClassDeclaration() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        // deal with extended classes
        var extendedClassName = node.get("extendedClassName");
        this.symbolTable.addSuperClassName(extendedClassName);

        return null;
    }

    public String dealWithVariableDeclaration(JmmNode node, String space) {
        System.out.println("In dealWithVariableDeclaration() function! (" + node.getKind() + ")");
        space = ((space != null) ? space : "");

        // deal with extended classes
        var extendedClassName = node.get("varName");
        this.symbolTable.addSuperClassName(extendedClassName);

        return null;
    }
}
