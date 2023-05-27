package pt.up.fe.comp2023.Ollir.optimization.optimizers;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2023.Ollir.OllirTemplates;

import java.util.HashMap;
import java.util.Map;

public class ConstPropagationVisitor extends AJmmVisitor<String, Boolean> {
    private final Map<String, String> constantVariables;

    public ConstPropagationVisitor() {
        this.constantVariables = new HashMap<>();
    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::defaultVisit);

        addVisit("Program", this::dealWithIteration);
        addVisit("ClassDeclaration", this::dealWithIteration);

        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("LocalVariable", this::dealWithLocalVariableDeclaration);

        addVisit("IfConditional", this::dealWithIfConditional);
    }

    private Boolean dealWithLocalVariableDeclaration(JmmNode node, String data) {
        boolean changes = false;

        if (node.getNumChildren() > 0) { // declaration with assignment
            JmmNode valueNode = node.getJmmChild(1);
            Type varType = getType(valueNode);

            changes = visit(valueNode);

            if (valueNode.getKind().equals("Integer") || valueNode.getKind().equals("Bool") || valueNode.getKind().equals("SelfCall") || valueNode.getKind().equals("Identifier")) {
                this.constantVariables.put(node.get("varName"), valueNode.get("val"));
                return true;
            }
        }

        return changes;
    }

    private Boolean dealWithMethodDeclaration(JmmNode node, String data) {
        boolean changes = false;

        this.constantVariables.clear(); // clear the list of constant variables on the method scope

        return this.dealWithIteration(node, data); // Go to every child of the method node
    }

    public Boolean dealWithIteration(JmmNode node, String data) {
        boolean changes = false;

        for (JmmNode child : node.getChildren()) {
            changes = visit(child, data) || changes;
        }

        return changes;
    }

    private Boolean dealWithIfConditional(JmmNode node, String data) {
        boolean changes = false;

        JmmNode ifConditionNode = node.getJmmChild(0);
        changes = visit(ifConditionNode, data); // condition expression
        ifConditionNode = node.getJmmChild(0); // update the condition node

        if (ifConditionNode.getKind().equals("Bool")) {
            switch (ifConditionNode.get("val")) {
                case "true": {
                    ifConditionNode.removeParent();
                    if (node.get("hasElse") != null) {
                        if (node.getNumChildren() == 3) { // has else expression
                            node.getJmmChild(2).removeParent();
                        }
                    }

                    node.getJmmChild(1).setParent(node.getJmmParent()); // Remove the if statement
                }
                case "false": {
                    ifConditionNode.removeParent();
                    if (node.get("hasElse") != null) {
                        if (node.getNumChildren() == 3) { // has else expression
                            node.getJmmChild(2).setParent(node); // update the parent of the else statement
                        }
                    }
                    node.getJmmChild(1).removeParent(); // remove the parent from the if statement
                }
            }
        }

        return changes;
    }


    public Boolean defaultVisit(JmmNode jmmNode, String data) {
        boolean changes = false;

        for (JmmNode child : jmmNode.getChildren()) {
            changes = visit(child) || changes;
        }

        return changes;
    }

    private static Type getType(JmmNode nodeType) {
        return switch (nodeType.getKind()) {
            case "IntegerArrayType" -> new Type("int", true);
            case "IntegerType" -> new Type("int", false);
            case "BooleanType" -> new Type("boolean", false);
            case "StringType" -> new Type("String", false);
            case "VoidType" -> new Type("void", false);
            case "IdType" -> new Type(nodeType.get("typeName"), false);
            default -> throw new IllegalStateException("Unexpected value: " + nodeType.getKind());
        };
    }

}
