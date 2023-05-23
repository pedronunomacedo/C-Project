package pt.up.fe.comp2023.Ollir;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp2023.ast.JmmSymbolTable;
import pt.up.fe.comp2023.Ollir.OllirVisitor;

public class OptimizationStage implements JmmOptimization {
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        boolean optimize = semanticsResult.getConfig().get("optimize") != null
                && semanticsResult.getConfig().get("optimize").equals("true");

        if (!optimize) return semanticsResult;

        boolean debug = semanticsResult.getConfig().get("debug") != null && semanticsResult.getConfig().get("debug").equals("true");

        System.out.println("Performing optimizations before OLLIR ...");

        // Implement the optimizations

        if (debug) {
            System.out.println("Optimized annotated AST : \n" + semanticsResult.getRootNode().toTree());
        }

        return semanticsResult;
    }

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
