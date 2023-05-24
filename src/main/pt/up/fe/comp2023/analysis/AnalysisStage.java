package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.ast.JmmSymbolTable;
import pt.up.fe.comp2023.ast.SymbolTableVisitor;

import java.util.*;

public class AnalysisStage implements JmmAnalysis {

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        // 1. Create the errors report

        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmSymbolTable symbolTable = new SymbolTableVisitor().getSymbolTable(parserResult.getRootNode());

        Analyser analyser = new Analyser(symbolTable);

        analyser.visit(parserResult.getRootNode());

        List<Report> reports = analyser.getReports();

        System.out.println("reports: " + reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
