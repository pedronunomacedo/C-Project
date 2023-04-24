package pt.up.fe.comp2023;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.ast.JmmSymbolTable;

public class Analysis {
    private List<Report> reports;
    private JmmSymbolTable symbolTable;

    Analysis(List<Report> reports, JmmSymbolTable symbolTable) {
        this.reports = reports;
        this.symbolTable = symbolTable;
    }

    Analysis(JmmSymbolTable symbolTable) {
        this.reports = new ArrayList<Report>();
        this.symbolTable = symbolTable;
    }

    public List<Report> getReports() {
        return reports;
    }

    public JmmSymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void newReport(JmmNode node, String message) {
        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                //TODO get line and col
                Integer.parseInt(node.get("lineStart")), Integer.parseInt(node.get("colStart")),
                message));
    }
}
