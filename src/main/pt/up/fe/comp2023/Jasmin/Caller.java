package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import static pt.up.fe.comp2023.Jasmin.JasminBuilder.classUnit;

public class Caller {
    public static String generateVirtualInvokeCode(CallInstruction call, Method method) {
        StringBuilder code = new StringBuilder();
        code.append("\t").append(Instructions.loadInst(call.getFirstArg(), method.getVarTable()));
        for (Element op : call.getListOfOperands()){
            code.append("\t").append(Instructions.loadInst(op, method.getVarTable()));
        }

        code.append("\t").append("invokevirtual ");

        if (((ClassType)call.getFirstArg().getType()).getName().equals("this")){
            code.append(method.getOllirClass().getClassName());
        }
        else{
            code.append(((ClassType) call.getFirstArg().getType()).getName());
        }

        code.append("/");
        code.append(((LiteralElement)call.getSecondArg()).getLiteral().replace("\"", ""));
        code.append("(");

        for (Element op : call.getListOfOperands()){
            code.append(JasminTypesInst.getType(op.getType(), classUnit.getImports(), true));
        }

        code.append(")").append(JasminTypesInst.getType(call.getReturnType(), classUnit.getImports(),false)).append("\n");

        return code.toString();
    }

    public static String generateJasminCodeForNEWInstructions(CallInstruction callInstr, Method method) {
        StringBuilder jasminCodeBuilder = new StringBuilder();

        for (Element elem : callInstr.getListOfOperands()){
            jasminCodeBuilder.append("\t").append(Instructions.loadInst(elem, method.getVarTable()));
        }

        if (callInstr.getFirstArg().getType().getTypeOfElement() == ElementType.OBJECTREF){
            jasminCodeBuilder.append("\tnew ").append(((Operand)callInstr.getFirstArg()).getName()).append("\n")
                    .append("\tdup").append("\n");
        }

        return jasminCodeBuilder.toString();
    }


    public static String generateSpecialInvokeCode(CallInstruction call, Method method) {
        StringBuilder code = new StringBuilder();
        code.append("\t").append(Instructions.loadInst(call.getFirstArg(), method.getVarTable()));
        code.append("\t").append("invokespecial ");

        if (call.getFirstArg().getType().getTypeOfElement() == ElementType.THIS){
            if (method.getOllirClass().getSuperClass() == null){
                code.append("java/lang/Object");
            }
            else{
                code.append(method.getOllirClass().getSuperClass());
            }
        }
        else{
            code.append(((ClassType) call.getFirstArg().getType()).getName());
        }

        code.append("/");
        code.append(((LiteralElement)call.getSecondArg()).getLiteral().replace("\"", ""));
        code.append("(");

        for (Element op : call.getListOfOperands()){
            code.append(JasminTypesInst.getType(op.getType(), classUnit.getImports(), false));
        }

        code.append(")").append(JasminTypesInst.getType(call.getReturnType(), classUnit.getImports(), false)).append("\n");

        return code.toString();
    }

    public static String generateStaticInvokeCode(CallInstruction call, Method method) {
        StringBuilder code = new StringBuilder();
        for (Element op : call.getListOfOperands()){
            code.append("\t").append(Instructions.loadInst(op, method.getVarTable()));
        }
        code.append("\t").append("invokestatic ");

        if (((Operand)call.getFirstArg()).getName().equals("this")){
            code.append(method.getOllirClass().getClassName());
        }
        else{
            code.append(((Operand)call.getFirstArg()).getName());
        }

        code.append("/");
        code.append(((LiteralElement)call.getSecondArg()).getLiteral().replace("\"", ""));
        code.append("(");

        for (Element op : call.getListOfOperands()){
            code.append(JasminTypesInst.getType(op.getType(), classUnit.getImports(), false));
        }

        code.append(")").append(JasminTypesInst.getType(call.getReturnType(), classUnit.getImports(), false)).append("\n");

        return code.toString();
    }


    public static String generateInvokeArrayLengthInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(Instructions.loadInst(instruction.getFirstArg(), method.getVarTable())).append("arraylength\n");

        return jasminCode.toString();
    }

    public static String generateInvokeLdcInstructions(CallInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(Instructions.loadInst(instruction.getFirstArg(), method.getVarTable()));

        return jasminCode.toString();
    }
}
