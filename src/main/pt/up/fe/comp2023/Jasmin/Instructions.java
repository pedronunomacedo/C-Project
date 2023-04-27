package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import java.util.HashMap;

import static org.specs.comp.ollir.CallType.*;
import static org.specs.comp.ollir.OperationType.*;
import static pt.up.fe.comp2023.Jasmin.JasminBuilder.classUnit;

public class Instructions {
    public static String binaryOp(BinaryOpInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        Element leftOp = instruction.getLeftOperand();
        Element rightOp = instruction.getRightOperand();

        if (instruction.getOperation().getOpType() == ADD) {
            return manageOperations("iadd", leftOp, rightOp, method);
        } else if (instruction.getOperation().getOpType() == MUL) {
            return manageOperations("imul", leftOp, rightOp, method);
        } else if (instruction.getOperation().getOpType() == SUB) {
            return manageOperations("isub", leftOp, rightOp, method);
        } else if (instruction.getOperation().getOpType() == DIV) {
            return manageOperations("idiv", leftOp, rightOp, method);
        }

        return jasminCode.toString();
    }

    public static String getField(GetFieldInstruction instruction, Method method) {
        StringBuilder jasminCodeBuilder = new StringBuilder();
        Element firstOperand = instruction.getFirstOperand();
        Element secondOperand = instruction.getSecondOperand();
        String className;

        if (((Operand)firstOperand).getName().equals("this")) {
            className = method.getOllirClass().getClassName();
        } else {
            className = JasminTypesInst.getType(firstOperand.getType(), classUnit.getImports(), false);
        }

        jasminCodeBuilder.append("\t")
                .append(loadInst(firstOperand, method.getVarTable()))
                .append("\tgetfield ")
                .append(className)
                .append("/")
                .append(((Operand)secondOperand).getName())
                .append(" ")
                .append(JasminTypesInst.getType(secondOperand.getType(), classUnit.getImports(), false))
                .append("\n");

        String jasminCode = jasminCodeBuilder.toString();
        return jasminCode;
    }

    public static String putField(PutFieldInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        Element firstOp = instruction.getFirstOperand();
        Element secondOp = instruction.getSecondOperand();
        Element thirdOp = instruction.getThirdOperand();
        String fieldName;

        if(!loadInst(firstOp, method.getVarTable()).contains("ldc")){
            jasminCode.append("\t");
        }

        if(!loadInst(thirdOp, method.getVarTable()).contains("ldc")){
            jasminCode.append("\t");
        }

        jasminCode.append(loadInst(firstOp, method.getVarTable()));
        jasminCode.append(loadInst(thirdOp, method.getVarTable()));

        if (((Operand)firstOp).getName().equals("this")){
            fieldName = method.getOllirClass().getClassName();
        }
        else{
            fieldName = JasminTypesInst.getType(firstOp.getType(), classUnit.getImports(), false);
        }

        jasminCode.append("\tputfield ").append(fieldName).append("/").append(((Operand)secondOp).getName())
                .append(" ").append(JasminTypesInst.getType(secondOp.getType(), classUnit.getImports(), false)).append("\n");

        return jasminCode.toString();
    }

    public static String callInst(CallInstruction instruction, Method method) {
        if (instruction.getInvocationType() == invokevirtual) {
            return Caller.generateVirtualInvokeCode(instruction, method);
        } else if (instruction.getInvocationType() == ldc) {
            return Caller.generateInvokeLdcInstructions(instruction, method);
        } else if (instruction.getInvocationType() == invokespecial) {
            return Caller.generateSpecialInvokeCode(instruction, method);
        } else if (instruction.getInvocationType() == invokestatic) {
            return Caller.generateStaticInvokeCode(instruction, method);
        } else if (instruction.getInvocationType() == arraylength) {
            return Caller.generateInvokeArrayLengthInstructions(instruction, method);
        } else if (instruction.getInvocationType() == NEW) {
            return Caller.generateJasminCodeForNEWInstructions(instruction, method);
        }

        return "";
    }

    public static String returnInst(ReturnInstruction instruction, Method method) {
        StringBuilder result = new StringBuilder();
        Element operand = instruction.getOperand();

        if (!instruction.hasReturnValue()) {
            return "\treturn\n";
        }

        if (!loadInst(operand, method.getVarTable()).contains("ldc")) {
            result.append("\t");
        }

        result.append(loadInst(operand, method.getVarTable()));

        if (operand.getType().getTypeOfElement() == ElementType.INT32 || operand.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            result.append("\tireturn\n");
        } else {
            result.append("\tareturn\n");
        }

        return result.toString();
    }



    public static String loadInst(Element op, HashMap<String, Descriptor> varTable) {
        if (op.isLiteral()){
            return "\t" + "ldc " + (((LiteralElement)op).getLiteral()) + "\n";
        }
        int virtualReg = varTable.get(((Operand)op).getName()).getVirtualReg();
        StringBuilder jasminCode = new StringBuilder();
        if (op.getType().getTypeOfElement() == ElementType.INT32 || op.getType().getTypeOfElement() == ElementType.BOOLEAN){
            if (varTable.get(((Operand)op).getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF){
                jasminCode.append(loadArrayElem(op, varTable));
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

    private static String loadArrayElem(Element operand, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        Operand arrayOperand = (Operand) operand;
        Element indexOperand = ((ArrayOperand) operand).getIndexOperands().get(0);

        int arrayVirtualReg = varTable.get(arrayOperand.getName()).getVirtualReg();
        int indexVirtualReg = varTable.get(((Operand)indexOperand).getName()).getVirtualReg();

        jasminCode.append("aload ").append(arrayVirtualReg).append("\n");
        jasminCode.append("iload ").append(indexVirtualReg).append("\n");
        jasminCode.append("iaload").append("\n");

        return jasminCode.toString();
    }

    public static String storeElem(Element elem, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        int virtualReg = varTable.get(((Operand)elem).getName()).getVirtualReg();

        if (elem.getType().getTypeOfElement() == ElementType.INT32 || elem.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            if (varTable.get(((Operand)elem).getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                jasminCode.append(storeArray(elem, varTable));
            } else {
                jasminCode.append("istore ").append(virtualReg).append("\n");
            }
        } else {
            jasminCode.append("astore ").append(virtualReg).append("\n");
        }

        return jasminCode.toString();
    }

    private static String storeArray(Element operand, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        Operand arrayOperand = (Operand) operand;
        int arrayReg = varTable.get(arrayOperand.getName()).getVirtualReg();

        Operand indexOperand = (Operand) ((ArrayOperand) operand).getIndexOperands().get(0);
        int indexReg = varTable.get(indexOperand.getName()).getVirtualReg();

        jasminCode.append("aload ").append(arrayReg).append("\n");
        jasminCode.append("iload ").append(indexReg).append("\n");
        jasminCode.append("astore ").append(arrayReg).append("\n");
        jasminCode.append("iastore").append("\n");

        return jasminCode.toString();
    }

    public static String manageOperations(String op, Element left, Element right, Method m) {
        StringBuilder code = new StringBuilder();

        if (left.isLiteral() && right.isLiteral()) {
            LiteralElement l = (LiteralElement) left;
            LiteralElement r = (LiteralElement) right;
            int result = 0;

            if (op.equals("iadd")) {
                result = Integer.parseInt(l.getLiteral()) + Integer.parseInt(r.getLiteral());
            } else if (op.equals("isub")) {
                result = Integer.parseInt(l.getLiteral()) - Integer.parseInt(r.getLiteral());
            } else if (op.equals("imul")) {
                result = Integer.parseInt(l.getLiteral()) * Integer.parseInt(r.getLiteral());
            } else if (op.equals("idiv")) {
                result = Integer.parseInt(l.getLiteral()) / Integer.parseInt(r.getLiteral());
            }

            LiteralElement resultLiteral = new LiteralElement(String.valueOf(result), new Type(ElementType.INT32));
            code.append(Instructions.loadInst(resultLiteral, m.getVarTable()));
        } else {
            code.append(Instructions.loadInst(left, m.getVarTable()))
                    .append(Instructions.loadInst(right, m.getVarTable()))
                    .append(op).append("\n");
        }

        return code.toString();
    }
}
