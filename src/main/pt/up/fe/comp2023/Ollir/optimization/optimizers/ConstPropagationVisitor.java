package pt.up.fe.comp2023.Ollir.optimization.optimizers;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.Ollir.OllirTemplates;

import java.util.Arrays;
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
        addVisit("BinaryOp", this::dealWithIteration);
        addVisit("ReturnObj", this::dealWithIteration);
        addVisit("Brackets", this::dealWithIteration);
        addVisit("Expr", this::dealWithIteration);

        addVisit("MethodDeclaration", this::dealWithMethodDeclaration);
        addVisit("LocalVariable", this::dealWithLocalVariableDeclaration);

        addVisit("IfConditional", this::dealWithIfConditional);
        addVisit("Assignment", this::dealWithAssignment);

        addVisit("Identifier", this::dealWithIdentifier);
    }

    private Boolean dealWithIdentifier(JmmNode node, String data) {
        boolean change = false;
        String varName = node.get("val");

        if (this.constantVariables.get(varName) != null) {
            String valueType = "";

            try {
                int number = Integer.parseInt(this.constantVariables.get(varName));
                valueType = "Integer";
            } catch (NumberFormatException e) {
                valueType = "Bool";
            }

            JmmNode newNode = new JmmNodeImpl(valueType);
            newNode.put("val", this.constantVariables.get(varName));
            newNode.put("lineStart", node.get("lineStart"));
            newNode.put("codeStart", node.get("colStart"));
            node.replace(newNode);
        }

        return change;
    }


    private Boolean dealWithAssignment(JmmNode node, String data) {
        boolean changes = false;
        String varName = node.get("varName");

        changes = visit(node.getJmmChild(0));

        System.out.println("node.getJmmChild(0).getKind(): " + node.getJmmChild(0).getKind());
        if (Arrays.asList("Bool", "Integer").contains(node.getJmmChild(0).getKind())) {
            System.out.println("varName: " + varName);
            System.out.println("node.getJmmChild(0).get(\"val\"): " + node.getJmmChild(0).get("val"));
            System.out.println("node.getJmmChild(0).getKind()): " + node.getJmmChild(0).getKind());
            this.constantVariables.put(varName, node.getJmmChild(0).get("val"));
            JmmNode newNode = new JmmNodeImpl(node.getJmmChild(0).getKind());
            newNode.put("val", node.getJmmChild(0).get("val"));
            newNode.put("lineStart", node.get("lineStart"));
            newNode.put("colStart", node.get("colStart"));
            node.getJmmChild(0).replace(newNode);

            System.out.println("node.getChildren(): " + node.getChildren());
        }

        return changes;
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

        for (JmmNode child : node.getChildren()) {
            changes = visit(child, data) || changes;
        }

        // JmmNode ifConditionNode = node.getJmmChild(0);
        // changes = visit(child, data) // condition expression

        /* CODE ELIMINATION (WORKING!)
        if (ifConditionNode.getKind().equals("Bool")) {
            switch (ifConditionNode.get("val")) {
                case "true" -> {
                    //node.replace(node.getJmmChild(1));
                    return true;
                }
                case "false" -> {
                    if (node.get("hasElse") != null) {
                        //node.replace(node.getJmmChild(2)); // dead code elimination
                        return true;
                    }
                }
            }
        }
         */

        return changes;
    }


    public Boolean defaultVisit(JmmNode node, String data) {
        return false;
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
