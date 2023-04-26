package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import java.util.HashMap;

import static pt.up.fe.comp2023.Jasmin.JasminBuilder.classUnit;

public class Instructions {
    public static String noOpInstructions(SingleOpInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(loadInstruction(instruction.getSingleOperand(), method.getVarTable()));

        return jasminCode.toString();
    }

    public static String binaryOpInstructions(BinaryOpInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        Element leftOp = instruction.getLeftOperand();
        Element rightOp = instruction.getRightOperand();

        switch (instruction.getOperation().getOpType()){
            case ADD:
                return operationsManage("iadd", leftOp, rightOp, method);
            case SUB:
                return operationsManage("isub", leftOp, rightOp, method);
            case MUL:
                return operationsManage("imul", leftOp, rightOp, method);
            case DIV:
                return operationsManage("idiv", leftOp, rightOp, method);
        }

        return jasminCode.toString();
    }

    public static String unaryOpInstructions(UnaryOpInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        return jasminCode.toString();
    }

    public static String getFieldInstructions(GetFieldInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        Element firstOperand = instruction.getFirstOperand();
        Element secondOperand = instruction.getSecondOperand();
        String name;

        jasminCode.append("\t").append(loadInstruction(firstOperand, method.getVarTable()));

        if (((Operand)firstOperand).getName().equals("this")){
            name = method.getOllirClass().getClassName();
        }
        else{
            name = JasminTypesInst.getType(firstOperand.getType(), classUnit.getImports(), false);
        }

        jasminCode.append("\tgetfield ").append(name).append("/").append(((Operand)secondOperand).getName()).append(" ").append(JasminTypesInst.getType(secondOperand.getType(), classUnit.getImports(), false)).append("\n");

        return jasminCode.toString();
    }

    public static String putFieldInstructions(PutFieldInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        Element firstOperand = instruction.getFirstOperand();
        Element secondOperand = instruction.getSecondOperand();
        Element thirdOperand = instruction.getThirdOperand();
        String name;

        if(!loadInstruction(firstOperand, method.getVarTable()).contains("ldc")){
            jasminCode.append("\t");
        }

        if(!loadInstruction(thirdOperand, method.getVarTable()).contains("ldc")){
            jasminCode.append("\t");
        }

        jasminCode.append(loadInstruction(firstOperand, method.getVarTable()));
        jasminCode.append(loadInstruction(thirdOperand, method.getVarTable()));

        if (((Operand)firstOperand).getName().equals("this")){
            name = method.getOllirClass().getClassName();
        }
        else{
            name = JasminTypesInst.getType(firstOperand.getType(), classUnit.getImports(), false);
        }

        jasminCode.append("\tputfield ").append(name).append("/").append(((Operand)secondOperand).getName()).append(" ").append(JasminTypesInst.getType(secondOperand.getType(), classUnit.getImports(), false)).append("\n");

        return jasminCode.toString();
    }

    public static String callInstructions(CallInstruction instruction, Method method) {
        switch (instruction.getInvocationType()){
            case invokevirtual:
                return Caller.generateVirtualInvokeCode(instruction, method);
            case invokespecial:
                return Caller.generateSpecialInvokeCode(instruction, method);
            case invokestatic:
                return Caller.generateStaticInvokeCode(instruction, method);
            case NEW:
                return Caller.generateJasminCodeForNEWInstructions(instruction, method);
            case arraylength:
                return Caller.generateInvokeArrayLengthInstructions(instruction, method);
            case ldc:
                return Caller.generateInvokeLdcInstructions(instruction, method);
        }

        return "";
    }

    public static String assignInstructions(AssignInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        Instruction rhs = instruction.getRhs();
        Element lhs = instruction.getDest();
        jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
        jasminCode.append("\t").append(storeElement(lhs, method.getVarTable()));

        return jasminCode.toString();

    }


    public static String gotoInstructions(GotoInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append("\t").append("goto ").append(instruction.getLabel()).append("\n");

        return jasminCode.toString();

    }

    public static String returnInstructions(ReturnInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        if (!instruction.hasReturnValue()){
            return "\t" + "return" + "\n";
        }
        if(!loadInstruction(instruction.getOperand(), method.getVarTable()).contains("ldc")){
            jasminCode.append("\t");
        }
        jasminCode.append(loadInstruction(instruction.getOperand(), method.getVarTable()));

        if (instruction.getOperand().getType().getTypeOfElement() == ElementType.INT32 || instruction.getOperand().getType().getTypeOfElement() == ElementType.BOOLEAN){
            jasminCode.append("\t").append("ireturn").append("\n");
        }
        else{
            jasminCode.append("\t").append("areturn").append("\n");
        }

        return jasminCode.toString();
    }



    public static String loadInstruction(Element operand, HashMap<String, Descriptor> varTable) {
        if (operand.isLiteral()){
            return "\t" + "ldc " + (((LiteralElement)operand).getLiteral()) + "\n";
        }
        int virtualReg = varTable.get(((Operand)operand).getName()).getVirtualReg();
        StringBuilder jasminCode = new StringBuilder();
        if (operand.getType().getTypeOfElement() == ElementType.INT32 || operand.getType().getTypeOfElement() == ElementType.BOOLEAN){
            if (varTable.get(((Operand)operand).getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF){
                jasminCode.append(loadArrayElement(operand, varTable));
            }
            else{
                jasminCode.append("iload ").append(virtualReg).append("\n");
            }
        }
        else {
            jasminCode.append("aload ").append(virtualReg).append("\n");
        }

        return jasminCode.toString();
    }

    private static String loadArrayElement(Element operand, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        Element index = ((ArrayOperand)operand).getIndexOperands().get(0);
        int reg = varTable.get(((Operand) operand).getName()).getVirtualReg();
        int indexReg = varTable.get(((Operand) index).getName()).getVirtualReg();

        jasminCode.append("aload ").append(reg).append("\n");
        jasminCode.append("iload ").append(indexReg).append("\n");
        jasminCode.append("iaload").append("\n");

        return jasminCode.toString();
    }

    public static String storeElement(Element operand, HashMap<String, Descriptor> varTable){
        StringBuilder jasminCode = new StringBuilder();
        int virtualReg = varTable.get(((Operand)operand).getName()).getVirtualReg();
        if (operand.getType().getTypeOfElement() == ElementType.INT32 || operand.getType().getTypeOfElement() == ElementType.BOOLEAN){
            if (varTable.get(((Operand)operand).getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF){
                jasminCode.append(storeArray(operand, varTable));
            }
            else{
                jasminCode.append("istore ").append(virtualReg).append("\n");
            }
        }
        else {
            jasminCode.append("astore ").append(virtualReg).append("\n");
        }

        return jasminCode.toString();
    }

    private static String storeArray(Element operand, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        Element index = ((ArrayOperand)operand).getIndexOperands().get(0);
        int reg = varTable.get(((Operand) operand).getName()).getVirtualReg();
        int indexReg = varTable.get(((Operand) index).getName()).getVirtualReg();

        jasminCode.append("astore ").append(reg).append("\n");
        jasminCode.append("istore ").append(indexReg).append("\n");
        jasminCode.append("iastore").append("\n");

        return jasminCode.toString();
    }

    public static String operationsManage(String operation, Element leftOperand, Element rightOperand, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        if (leftOperand.isLiteral() && rightOperand.isLiteral()) {
            LiteralElement left = (LiteralElement) leftOperand;
            LiteralElement right = (LiteralElement) rightOperand;
            int result = 0;

            if (operation.equals("iadd")) {
                result = Integer.parseInt(left.getLiteral()) + Integer.parseInt(right.getLiteral());
            } else if (operation.equals("isub")) {
                result = Integer.parseInt(left.getLiteral()) - Integer.parseInt(right.getLiteral());
            } else if (operation.equals("imul")) {
                result = Integer.parseInt(left.getLiteral()) * Integer.parseInt(right.getLiteral());
            } else if (operation.equals("idiv")) {
                result = Integer.parseInt(left.getLiteral()) / Integer.parseInt(right.getLiteral());
            }

            LiteralElement resultLiteral = new LiteralElement(String.valueOf(result), new Type(ElementType.INT32));
            jasminCode.append(Instructions.loadInstruction(resultLiteral, method.getVarTable()));
        } else {
            jasminCode.append(Instructions.loadInstruction(leftOperand, method.getVarTable()))
                    .append(Instructions.loadInstruction(rightOperand, method.getVarTable()))
                    .append(operation).append("\n");
        }

        return jasminCode.toString();
    }
}
