package pt.up.fe.comp2023.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.ast.OllirVisitor;

import java.util.Collections;
import java.util.List;

public class ExprOllirVisitor extends OllirVisitor {

    public ExprOllirVisitor(JmmSymbolTable symbolTable, List<Report> reports) {
        super(symbolTable, reports);
    }

    @Override
    public void buildVisitor() {
        super.buildVisitor();
        /*
        addVisit("Array", this::dealWithArrayDeclaration);
        addVisit("Lenght", this::dealWithExpression);
        addVisit("UnaryOp", this::dealWithExpression);
        addVisit("NewArrayObject", this::dealWithExpression);
        addVisit("NewObject", this::dealWithExpression);
         */

        /*
        addVisit("MemberAccess", this::dealWithMemberAccess);
        addVisit("BinaryOp", this::dealWithBinaryOp); // creates and returns the OLLIR code with the temporary variables
        addVisit("ExprParentheses", this::dealWithExprParentheses); // (returns the OLLIR code, if BinaryOp is the father) or (returns the parentheses and the child code)
        addVisit("Integer", this::dealWithSingleExpression); // terminal nodes
        addVisit("Bool", this::dealWithSingleExpression); // terminal nodes
        addVisit("SelfCall", this::dealWithSingleExpression); // terminal nodes
        addVisit("Identifier", this::dealWithSingleExpression); // terminal nodes
         */
    }


}
