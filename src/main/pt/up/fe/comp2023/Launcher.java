package pt.up.fe.comp2023;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.symbolTable.JmmMethod;
import pt.up.fe.comp2023.symbolTable.JmmSymbolTable;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // ... add remaining stages

        // Create the symbolTable class object
        JmmSemanticsResult result = new AnalysisStage().semanticAnalysis(parserResult);

        System.out.println("\n\n\n\n");
        System.out.println("---- Symbol Table ----");
        System.out.println("-> Imports: " + result.getSymbolTable().getImports());

        System.out.println("-> ClassName: " + result.getSymbolTable().getClassName());

        System.out.println("-> SuperClassName: " + result.getSymbolTable().getSuper());

        System.out.println("-> ClassFields: " + result.getSymbolTable().getFields());

        System.out.println("-> Methods: " + result.getSymbolTable().getMethods());

        for (String method : result.getSymbolTable().getMethods()) {
            System.out.println("Return type of " + method + " is " + result.getSymbolTable().getReturnType(method));
        }

        /*
        System.out.println("SymbolTable:");
        System.out.println(result.getSymbolTable().print());
        */

        System.out.println("\n\n\n\n");
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
