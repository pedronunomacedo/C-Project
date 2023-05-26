package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.analysis.AnalysisStage;
import pt.up.fe.comp2023.Jasmin.BackendJasmin;
import pt.up.fe.comp2023.Ollir.OptimizationStage;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit(); // Setups console logging and other things

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

        // Create the symbolTable class object
        // Semantic Analysis
        JmmSemanticsResult semanticsResult = new AnalysisStage().semanticAnalysis(parserResult);

        // Check if there are parsing errors
        TestUtils.noErrors(semanticsResult);

        // Instantiate JmmOptimizer
        var optimizer = new OptimizationStage();

        // Optimization stage
        if (config.get("optimize") != null && config.get("optimize").equals("true")) {
            semanticsResult = optimizer.optimize(semanticsResult);
        }

        // ... add remaining stages

        // Transform to OLLIR code
        OllirResult ollirResult = optimizer.toOllir(semanticsResult);
        System.out.println("OllirResult: " + ollirResult.toString());

        // Transform OLLIR code to Jasmin code
        JasminResult jasminResult = new BackendJasmin().toJasmin(ollirResult);
        System.out.println("JasminResult: " + jasminResult.toString());

        // Compile and run the jasmin result
        jasminResult.compile();
        jasminResult.run();
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Create config
        Map<String, String> config = new HashMap<>();
        String rNum = "-1";
        String optimize = "false";
        String debug = "false";
        String inputFileName = null;

        for (String arg : args) {
            if (arg.startsWith("-r=")) { // Register allocation controller
                String[] argSplit = arg.split("=");
                if (argSplit.length == 2) {
                    String num = argSplit[1]; // number of maximum variables to use
                    int parsedNum;
                    try {
                        parsedNum = Integer.parseInt(num);
                    } catch (Exception e) {
                        printUsage();
                        throw new RuntimeException("Invalid integer in option -r. " + num + " not a integer.");
                    }

                    if (parsedNum >= -1 && parsedNum <= 255) { // variables_mode belongs to [-1, 255]
                        rNum = num;
                    } else {
                        printUsage();
                        throw new RuntimeException("Invalid option -r. Number needs to be between [-1, 255].");
                    }
                }
            } else if (arg.startsWith("-o")) { // Optimize controller
                optimize = "true";
            } else if (arg.startsWith("-d")) { // Debug option
                debug = "true";
            } else if (arg.startsWith("-i=")) { // Input file option
                String[] argSplit = arg.split("=");
                if (argSplit.length == 2) {
                    inputFileName = argSplit[1];
                }
            } else {
                printUsage();
                throw new RuntimeException("Invalid option " + arg + " .");
            }
        }

        if (inputFileName == null) {
            printUsage();
            throw new RuntimeException("Expected at least a single argument, a path to an existing input file.");
        }

        File inputFile = new File(inputFileName);
        if (!inputFile.isFile()) {
            printUsage();
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFileName + "'.");
        }

        // Create config
        config.put("inputFile", inputFileName);
        config.put("optimize", optimize);
        config.put("registerAllocation", rNum);
        config.put("debug", debug);

        return config;
    }

    public static void printUsage() {
        System.out.println("USAGE: ./Launcher [-r=<num>] [-o] [-d] -i=<input_file.jmm>");
    }
}
