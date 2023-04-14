package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp2023.ast.JmmSymbolTable;
import pt.up.fe.comp2023.ast.OllirVisitor;

import java.util.Arrays;

public class OptimizationStage implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        JmmNode rootNode = semanticsResult.getRootNode();
        OllirVisitor visitor = new OllirVisitor((JmmSymbolTable) semanticsResult.getSymbolTable(), semanticsResult.getReports());

        System.out.println("\n\n\nGenerating OLLIR ...");
        String ollirCode = (String) visitor.visit(rootNode).get(0);

        System.out.println("\nOllir code: ");
        System.out.println(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, semanticsResult.getReports());
    }
}
