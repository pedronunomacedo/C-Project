package pt.up.fe.comp2023.Ollir.optimization.optimizers;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.Ollir.OllirTemplates;

import java.util.*;

public class ConstPropagationVisitor extends AJmmVisitor<String, Boolean> {
    private final Map<String, String> constantVariables;
    private Set<String> constVariables;
    public ConstPropagationVisitor() {
        this.constantVariables = new HashMap<>();
        this.constVariables = new HashSet<>();
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
        addVisit("Loop", this::dealWithWhileDeclaration);
        addVisit("Assignment", this::dealWithAssignment);

        addVisit("Identifier", this::dealWithIdentifier);
    }

    private Boolean dealWithWhileDeclaration(JmmNode node, String data) {
        System.out.println("[OptStage - dealWithWhileDeclaration] node.getJmmParent().getKind(): " + node.getJmmParent());
        System.out.println("[OptStage - dealWithWhileDeclaration] data: " + data);
        boolean changes = false;
        JmmNode condNode = node.getJmmChild(0);
        JmmNode bodyLoopNode = node.getJmmChild(1);

        this.visitLoopAssignments(bodyLoopNode);

        System.out.println("[OptStage - dealWithWhileDeclaration] this.constantVariables: " + this.constantVariables);

        changes = visit(condNode, "LOOP_CONDITION");
        changes = visit(bodyLoopNode, "LOOP_BODY") || changes;

        //System.out.println("this.loopNotConstants: " + this.loopNotConstants);
        //System.out.println("this.variables: " + this.variables);

        return changes;
    }

    public void visitLoopAssignments(JmmNode bodyLoopNode) {
        System.out.println("node.getChildren(): " + bodyLoopNode.getKind());

        if (bodyLoopNode.getKind().equals("Brackets")) { // brackets
            for (JmmNode childLoopNode : bodyLoopNode.getChildren()) {
                if (!childLoopNode.getKind().equals("Assignment") && !childLoopNode.getKind().equals("ArrayAssignment")) {
                    this.constVariables.add(childLoopNode.get("varName"));
                }
            }
        } else { // single while loop body expression
            if (!bodyLoopNode.getKind().equals("Assignment") && !bodyLoopNode.getKind().equals("ArrayAssignment")) {
                this.constVariables.add(bodyLoopNode.get("varName"));
            }
        }
    }

    private Boolean dealWithIdentifier(JmmNode node, String data) {
        boolean change = false;
        String varName = node.get("val");
        System.out.println("[OptStage - dealWithIdentifier] varName: " + varName);
        System.out.println("[OptStage - dealWithIdentifier] this.constantVariables: " + this.constantVariables);

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

        if (data != null && data.equals("LOOP_BODY")) {
            String prevValue = this.constantVariables.get(varName);
            this.constantVariables.remove(varName);
            this.constVariables.remove(varName);
            changes = visit(node.getJmmChild(0));

            if (Arrays.asList("Bool", "Integer").contains(node.getJmmChild(0).getKind())) {
                if (node.getJmmChild(0).get("val").equals(prevValue)) { // the value inside the loop actually did not change (possible code elimination - the value of the variable did not change)
                    this.constantVariables.put(varName, prevValue);
                    this.constVariables.add(varName);
                }
            }
            return changes;
        }

        changes = visit(node.getJmmChild(0));

        System.out.println("[OptStage - dealWithAssignment] node.getJmmChild(0).getKind(): " + node.getJmmChild(0).getKind());
        System.out.println("[OptStage - dealWithAssignment] data: " + data);
        if (Arrays.asList("Bool", "Integer").contains(node.getJmmChild(0).getKind())) {
            if (data != null && data.equals("LOOP_BODY")) {
                this.constantVariables.remove(varName);
                this.constVariables.remove(varName);
            } else {
                this.constantVariables.put(varName, node.getJmmChild(0).get("val"));
                JmmNode newNode = new JmmNodeImpl(node.getJmmChild(0).getKind());
                newNode.put("val", node.getJmmChild(0).get("val"));
                newNode.put("lineStart", node.get("lineStart"));
                newNode.put("colStart", node.get("colStart"));
                node.getJmmChild(0).replace(newNode);
                this.constantVariables.put(varName, node.getJmmChild(0).get("val"));
                this.constVariables.add(varName);
            }

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
