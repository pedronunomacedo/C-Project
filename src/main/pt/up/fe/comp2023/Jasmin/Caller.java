package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import static pt.up.fe.comp2023.Jasmin.JasminBuilder.classUnit;

public class Caller {
    public static String invokeVirtualInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append("\t").append(Instructions.loadInstruction(instruction.getFirstArg(), method.getVarTable()));

        for (Element element : instruction.getListOfOperands()){
            jasminCode.append("\t").append(Instructions.loadInstruction(element, method.getVarTable()));
        }

        jasminCode.append("\t").append("invokevirtual ");

        if (((ClassType)instruction.getFirstArg().getType()).getName().equals("this")){
            jasminCode.append(method.getOllirClass().getClassName());
        }
        else{
            jasminCode.append(((ClassType) instruction.getFirstArg().getType()).getName());
        }

        jasminCode.append("/");
        jasminCode.append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""));
        jasminCode.append("(");

        for (Element element : instruction.getListOfOperands()){
            jasminCode.append(JasminTypesInst.getType(element.getType(), classUnit.getImports(), true));
        }

        jasminCode.append(")").append(JasminTypesInst.getType(instruction.getReturnType(), classUnit.getImports(),false)).append("\n");

        return jasminCode.toString();

    }

    public static String invokeSpecialInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append("\t").append(Instructions.loadInstruction(instruction.getFirstArg(), method.getVarTable()));

        jasminCode.append("\t").append("invokespecial ");

        if (instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS){
            if (method.getOllirClass().getSuperClass() == null){
                jasminCode.append("java/lang/Object");
            }
            else{
                jasminCode.append(method.getOllirClass().getSuperClass());
            }
        }
        else{
            jasminCode.append(((ClassType) instruction.getFirstArg().getType()).getName());
        }

        jasminCode.append("/");
        jasminCode.append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""));
        jasminCode.append("(");

        for (Element element : instruction.getListOfOperands()){
            jasminCode.append(JasminTypesInst.getType(element.getType(), classUnit.getImports(), false));
        }

        jasminCode.append(")").append(JasminTypesInst.getType(instruction.getReturnType(), classUnit.getImports(), false)).append("\n");

        return jasminCode.toString();
    }

    public static String invokeStaticInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        for (Element element : instruction.getListOfOperands()){
            jasminCode.append("\t").append(Instructions.loadInstruction(element, method.getVarTable()));
        }

        jasminCode.append("\t").append("invokestatic ");

        if (((Operand)instruction.getFirstArg()).getName().equals("this")){
            jasminCode.append(method.getOllirClass().getClassName());
        }
        else{
            jasminCode.append(((Operand)instruction.getFirstArg()).getName());
        }

        jasminCode.append("/");
        jasminCode.append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""));
        jasminCode.append("(");

        for (Element element : instruction.getListOfOperands()){
            jasminCode.append(JasminTypesInst.getType(element.getType(), classUnit.getImports(), false));
        }

        jasminCode.append(")").append(JasminTypesInst.getType(instruction.getReturnType(), classUnit.getImports(), false)).append("\n");

        return jasminCode.toString();
    }

    public static String invokeNEWInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        for (Element element : instruction.getListOfOperands()){
            jasminCode.append("\t").append(Instructions.loadInstruction(element, method.getVarTable()));
        }

        if (instruction.getFirstArg().getType().getTypeOfElement() == ElementType.OBJECTREF){
            jasminCode.append("\tnew ").append(((Operand)instruction.getFirstArg()).getName()).append("\n").append("\tdup").append("\n");
        }
        return jasminCode.toString();

    }

    public static String invokeArrayLengthInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(Instructions.loadInstruction(instruction.getFirstArg(), method.getVarTable())).append("arraylength\n");

        return jasminCode.toString();
    }

    public static String invokeLdcInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(Instructions.loadInstruction(instruction.getFirstArg(), method.getVarTable()));

        return jasminCode.toString();
    }
}
