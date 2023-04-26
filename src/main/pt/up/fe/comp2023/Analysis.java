package pt.up.fe.comp2023;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.ast.JmmMethod;
import pt.up.fe.comp2023.ast.JmmSymbolTable;

public class Analysis {
    private List<Report> reports;
    private JmmSymbolTable symbolTable;

    private JmmMethod currMethod;

    Analysis(List<Report> reports, JmmSymbolTable symbolTable) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        this.currMethod = null;
    }

    Analysis(JmmSymbolTable symbolTable) {
        this.reports = new ArrayList<Report>();
        this.symbolTable = symbolTable;
        this.currMethod = null;
    }

    public JmmMethod getCurrMethod() {
        return currMethod;
    }

    public void setCurrMethod(JmmMethod currMethod) {
        this.currMethod = currMethod;
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
