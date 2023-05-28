package pt.up.fe.comp2023.Ollir.optimization.optimizers;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2023.ast.JmmSymbolTable;

import java.util.*;

public class ConstFoldingVisitor extends AJmmVisitor<String, Boolean> {
    private JmmSymbolTable symbolTable;
    private HashMap<String, String> variables;
    private Set<String> loopNotConstants;
    public ConstFoldingVisitor(JmmSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.variables = new HashMap<>();
        this.loopNotConstants = new HashSet<>();
    }

    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::defaultVisit);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("ExprParentheses", this::dealWithExprParentheses);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Loop", this::dealWithLoop);

        // addVisit("Brackets", this::dealWithBrackets);
    }

    private Boolean dealWithBrackets(JmmNode node, String data) {
        boolean changes = false;
        if (data.equals("LOOP_BODY")) {
            for (JmmNode loopBodyChild : node.getChildren()) {

            }
        }

        return changes;
    }

    private Boolean dealWithLoop(JmmNode node, String data) {
        JmmNode condNode = node.getJmmChild(0);
        JmmNode bodyLoopNode = node.getJmmChild(1);

        this.visitLoopAssignments(bodyLoopNode);

        System.out.println("this.loopNotConstants: " + this.loopNotConstants);

        boolean changes = visit(condNode, "LOOP_CONDITION");
        changes = visit(bodyLoopNode, "LOOP_BODY") || changes;

        System.out.println("this.loopNotConstants: " + this.loopNotConstants);
        System.out.println("this.variables: " + this.variables);

        this.loopNotConstants = new HashSet<>();
        this.variables.clear();

        return changes;
    }

    public Boolean dealWithBinaryOp(JmmNode node, String data) {
        JmmNode leftNode = node.getJmmChild(0);
        JmmNode rightNode = node.getJmmChild(1);

        System.out.println("[dealWithBinaryOp] this.variables: " + this.variables);
        System.out.println("[dealWithBinaryOp] data: " + data);
        System.out.println("[dealWithBinaryOp] parent: " + node.getJmmParent().getKind());
        System.out.println("(Before) Visiting leftChild of " + node.get("op") + " : " + leftNode.getKind());
        boolean leftChanges = visit(leftNode, data);
        System.out.println("(Before) Visiting rightChild of " + node.get("op") + " : " + rightNode.getKind());
        boolean rightChanges = visit(rightNode, data);

        // These kids may have changed during the visit
        leftNode = node.getJmmChild(0);
        rightNode = node.getJmmChild(1);

        System.out.println("(After) Visiting leftChild of " + node.get("op") + " : " + leftNode.getKind());
        System.out.println("(After) Visiting rightChild of " + node.get("op") + " : " + rightNode.getKind());

        // Check if both kids are either both Bool or Integer
        boolean booleanKidsType = leftNode.getKind().equals("Bool") && rightNode.getKind().equals("Bool");
        boolean integerKidsType = leftNode.getKind().equals("Integer") && rightNode.getKind().equals("Integer");

        if (node.get("op").equals("&&") && leftNode.getKind().equals("Bool")) { // left child is a terminal boolean symbol
            if (leftNode.get("val").equals("true")) { // true && rightNode => rightNode
                node.replace(rightNode);
                return true;
            } else if (leftNode.get("val").equals("false")) {  // false && rightNode => false
                node.replace(leftNode);
                return true;
            }
        } else if (node.get("op").equals("||") && leftNode.getKind().equals("Bool")) {
            if (leftNode.get("val").equals("true")) { // true || rightNode => true
                node.replace(leftNode);
                return true;
            } else if (leftNode.get("val").equals("false")) {  // false || rightNode => rightNode
                node.replace(rightNode);
                return true;
            }
        } else if (booleanKidsType || integerKidsType) { // if both kids are either booleans or integers
            String newValue = ConstFoldingVisitor.getBinaryOpResult(leftNode.get("val"), node.get("op"), rightNode.get("val"));
            JmmNode newNode;

            if (newValue.equals("true") || newValue.equals("false")) {
                newNode = new JmmNodeImpl("Bool");
            } else {
                newNode = new JmmNodeImpl("Integer");
            }

            newNode.put("val", newValue);
            newNode.put("lineStart", node.get("lineStart"));
            newNode.put("colStart", node.get("colStart"));

            node.replace(newNode);

            return true;
        }

        return leftChanges || rightChanges;
    }

    public Boolean dealWithUnaryOp(JmmNode node, String data) {
        System.out.println("[dealWithUnaryOp] node.getAttributes(): " + node.getAttributes());
        boolean changes = visit(node.getJmmChild(0), data); // visit unary expression
        JmmNode childNode = node.getJmmChild(0);
        if (childNode.getKind().equals("Bool")) {
            String value = childNode.get("val");

            if (data.equals("LOOP_CONDITION")) {
                if (this.loopNotConstants.contains(value)) {
                    return false;
                }
            }

            JmmNode newNode = new JmmNodeImpl("Bool");
            newNode.put("lineStart", node.get("lineStart"));
            newNode.put("colStart", node.get("colStart"));

            if (value.equals("true")) {
                newNode.put("val", "false");
            } else {
                newNode.put("val", "true");
            }

            node.replace(newNode);

            return true;
        }

        return changes;
    }

    public Boolean dealWithExprParentheses(JmmNode node, String data) {
        boolean changes = false;
        JmmNode child = node.getJmmChild(0);
        changes = visit(child, data);
        child = node.getJmmChild(0); // Update the child variable (could be changed while being visited)

        if (child.getKind().equals("Integer") || child.getKind().equals("Bool")) {
            node.replace(child);
        }

        return changes;
    }

    public Boolean dealWithAssignment(JmmNode node, String data) {
        boolean changes = false;

        String varName = node.get("varName");
        JmmNode valueNode = node.getJmmChild(0);
        changes = visit(valueNode, data);
        valueNode = node.getJmmChild(0); // Update the changed value child

        if (valueNode.getKind().equals("Bool")) {
            this.variables.put(varName, valueNode.get("val"));
        } else if (valueNode.getKind().equals("Integer")) {
            if (data != null && data.equals("LOOP_BODY")) {
                if (this.loopNotConstants.contains(varName)) { // non constant loop variable
                    this.variables.put(varName, valueNode.get("val")); // update value
                } else {
                    return false;
                }
            } else {
                this.variables.put(varName, valueNode.get("val")); // update the value of the variable outside the loop
            }
        }

        return changes;
    }

    private Boolean dealWithIdentifier(JmmNode node, String data) {
        boolean changes = false;

        String varName = node.get("val");

        System.out.println("[dealWithIdentifier] data: " + data);
        System.out.println("[dealWithIdentifier] varName: " + varName);
        System.out.println("[dealWithIdentifier] this.variables: " + this.variables);

        if (this.variables.get(varName) != null) {
            String varStrType = null;
            if (this.variables.get(varName).equals("true") || this.variables.get(varName).equals("false")) {
                varStrType = "Bool";
            } else if (this.variables.get(varName).matches("-?\\d+")) { // Check if it's an integer number
                varStrType = "Integer";
            }

            if (data != null && data.equals("LOOP_CONDITION")) {
                System.out.println("[dealWithIdentifier] data: " + data);
                if (this.loopNotConstants.contains(varName)) { // if the variable that is in the loop condition and is not a constant (don't substitute)
                    return false;
                }
            } else if (data != null && data.equals("LOOP_BODY")) { // check if variable is in the loop body (all assignments inside the loop have already been visited and put in the loopNotConstants list)
                if (this.loopNotConstants.contains(varName)) { // check if the variable changes inside the loop
                    return false;
                }
            }

            if (Arrays.asList("Bool", "Integer").contains(varStrType)) { // substitute node
                JmmNode newNode = new JmmNodeImpl(varStrType);
                newNode.put("lineStart", node.get("lineStart"));
                newNode.put("colStart", node.get("colStart"));
                newNode.put("val", this.variables.get(varName));
                node.replace(newNode);
            }
        }

        return changes;
    }

    public Boolean defaultVisit(JmmNode jmmNode, String data) {
        boolean changes = false;

        for (JmmNode child : jmmNode.getChildren()) {
            changes = visit(child, data) || changes;
        }

        return changes;
    }

    public void visitLoopAssignments(JmmNode bodyLoopNode) {
        System.out.println("node.getChildren(): " + bodyLoopNode.getKind());

        if (bodyLoopNode.getKind().equals("Brackets")) { // brackets
            for (JmmNode childLoopNode : bodyLoopNode.getChildren()) {
                if (childLoopNode.getKind().equals("Assignment") || childLoopNode.getKind().equals("ArrayAssignment")) {
                    this.loopNotConstants.add(childLoopNode.get("varName"));
                }
            }
        } else { // single while loop body expression
            if (bodyLoopNode.getKind().equals("Assignment") || bodyLoopNode.getKind().equals("ArrayAssignment")) {
                this.loopNotConstants.add(bodyLoopNode.get("varName"));
            }
        }
    }

    public static JmmNode ancestorChecker(JmmNode node, String ancestorName) {
        if (node.getKind().equals("Program")) return null; // got to the root tree

        if (node.getKind().equals(ancestorName)) return node;

        return ancestorChecker(node.getJmmParent(), ancestorName);
    }

    public static String getBinaryOpResult(String left, String op, String right) {
        if (op.equals("&&")) {
            if (left.equals("true") && right.equals("true")) {
                return "true";
            } else {
                return "false";
            }
        } else if (op.equals("||")) {
            if (left.equals("true") || right.equals("true")) {
                return "true";
            } else {
                return "false";
            }
        } else if (op.equals("<")) {
            int leftNumber = Integer.parseInt(left);
            int rightNumber = Integer.parseInt(right);

            if (leftNumber < rightNumber) {
                return "true";
            } else {
                return "false";
            }
        } else if (op.equals("<=")) {
            int leftNumber = Integer.parseInt(left);
            int rightNumber = Integer.parseInt(right);

            if (leftNumber <= rightNumber) {
                return "true";
            } else {
                return "false";
            }
        } else if (op.equals(">")) {
            int leftNumber = Integer.parseInt(left);
            int rightNumber = Integer.parseInt(right);

            if (leftNumber > rightNumber) {
                return "true";
            } else {
                return "false";
            }
        } else if (op.equals(">=")) {
            int leftNumber = Integer.parseInt(left);
            int rightNumber = Integer.parseInt(right);

            if (leftNumber >= rightNumber) {
                return "true";
            } else {
                return "false";
            }
        } else {
            int leftNumber = Integer.parseInt(left);
            int rightNumber = Integer.parseInt(right);

            return switch (op) {
                case "+", "+=" -> Integer.toString(leftNumber + rightNumber);
                case "-", "-=" -> Integer.toString(leftNumber - rightNumber);
                case "*", "*=" -> Integer.toString(leftNumber * rightNumber);
                case "/", "/=" -> Integer.toString(leftNumber / rightNumber);
                default -> throw new IllegalStateException("Unexpected value: " + op);
            };
        }
    }
}
