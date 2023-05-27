package pt.up.fe.comp2023.Ollir.optimization.optimizers;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class ConstFoldingVisitor extends AJmmVisitor<String, Boolean> {
    @Override
    protected void buildVisitor() {
        setDefaultVisit(this::defaultVisit);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("ExprParentheses", this::dealWithExprParentheses);
    }

    int i = 0;

    public Boolean dealWithBinaryOp(JmmNode node, String data) {
        JmmNode leftNode = node.getJmmChild(0);
        JmmNode rightNode = node.getJmmChild(1);

        System.out.println("--------- BinaryOp " + i + " ---------");
        System.out.println("[BinaryOp] leftNode: " + leftNode);
        System.out.println("[BinaryOp] op: " + node.get("op"));
        System.out.println("[BinaryOp] rightNode: " + rightNode);
        System.out.println("-----------------------------");

        boolean changes = visit(leftNode);
        changes = visit(rightNode) || changes;

        System.out.println("-----------------------------");
        System.out.println("[BinaryOp] changes: " + changes);
        System.out.println("[BinaryOp] newLeftNode: " + leftNode);
        System.out.println("[BinaryOp] newRightNode: " + rightNode);
        System.out.println("--------- End BinaryOp " + i + " ---------");

        // These kids may have changed during the visit
        leftNode = node.getJmmChild(0);
        rightNode = node.getJmmChild(1);

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

        return changes;
    }

    public Boolean dealWithUnaryOp(JmmNode node, String data) {
        System.out.println("[dealWithUnaryOp] node.getAttributes(): " + node.getAttributes());
        boolean changes = visit(node.getJmmChild(0)); // visit unary expression
        JmmNode childNode = node.getJmmChild(0);
        if (childNode.getKind().equals("Bool")) {
            String value = childNode.get("val");

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
        changes = visit(child);
        JmmNode newNode = node;

        if (child.getKind().equals("Integer") || child.getKind().equals("Bool")) {
            node.replace(child);
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
