package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;

import java.util.ArrayList;

public class JasminTypesInst {
    public static String getType(Type t, ArrayList<String> imports, boolean methodParam) {
        if (t.getTypeOfElement() == ElementType.INT32) {
            return "I";
        } else if (t.getTypeOfElement() == ElementType.BOOLEAN) {
            return "Z";
        } else if (t.getTypeOfElement() == ElementType.VOID) {
            return "V";
        } else if (t.getTypeOfElement() == ElementType.STRING) {
            return "Ljava/lang/String;";
        } else if (t.getTypeOfElement() == ElementType.OBJECTREF) {
            return getObjectType((ClassType) t, imports, methodParam);
        } else if (t.getTypeOfElement() == ElementType.ARRAYREF) {
            return getArrayType((ArrayType) t, imports, methodParam);
        } else {
            return "";
        }
    }

    public static String getInstructionType(Instruction instr, Method m) {
        String result = "";
        if (instr.getInstType() == InstructionType.ASSIGN) {
            result = Instructions.assignInst((AssignInstruction) instr, m);
        }  else if (instr.getInstType() == InstructionType.GOTO) {
            StringBuilder jasminCode = new StringBuilder();
            GotoInstruction inst = (GotoInstruction) instr;
            jasminCode.append("\t").append("goto ").append(inst.getLabel()).append("\n");
            result = jasminCode.toString();
        } else if (instr.getInstType() == InstructionType.CALL) {
            result = Instructions.callInst((CallInstruction) instr, m);
        } else if (instr.getInstType() == InstructionType.BRANCH) {
            result = Instructions.branchInst((CondBranchInstruction) instr, m);
        }  else if (instr.getInstType() == InstructionType.PUTFIELD) {
            result = Instructions.putField((PutFieldInstruction) instr, m);
        } else if (instr.getInstType() == InstructionType.GETFIELD) {
            result = Instructions.getField((GetFieldInstruction) instr, m);
        } else if (instr.getInstType() == InstructionType.UNARYOPER) {
            StringBuilder jasminCode = new StringBuilder();
            UnaryOpInstruction instruct = (UnaryOpInstruction) instr;
            Element operand = instruct.getOperand();
            OperationType opType = instruct.getOperation().getOpType();

            switch (opType){
                case NOT, NOTB:
                    jasminCode.append(Instructions.loadInst(operand, m.getVarTable()));
                    jasminCode.append("ldc 1").append("\n");
                    Instructions.limitStack(1);
                    jasminCode.append("ixor").append("\n");
                    Instructions.limitStack(-1);
            }
            result = jasminCode.toString();
        } else if (instr.getInstType() == InstructionType.RETURN) {
            result = Instructions.returnInst((ReturnInstruction) instr, m);
        } else if (instr.getInstType() == InstructionType.BINARYOPER) {
            result = Instructions.binaryOp((BinaryOpInstruction) instr, m);
        } else if (instr.getInstType() == InstructionType.NOPER) {
            SingleOpInstruction ins = (SingleOpInstruction) instr;
            StringBuilder jasminCode = new StringBuilder();
            jasminCode.append(Instructions.loadInst(ins.getSingleOperand(), m.getVarTable()));
            result = jasminCode.toString();
        }
        return result;
    }

    private static String getArrayType(ArrayType arrayType, ArrayList<String> importList, boolean methodParam) {
        StringBuilder jasminCode = new StringBuilder();
        int num = arrayType.getNumDimensions();

        for (int i = 0; i < num; i++) {
            jasminCode.append("[");
        }


        String elementType = getType(arrayType.getElementType(), importList, methodParam);
        jasminCode.append(elementType);

        return jasminCode.toString();
    }


    private static String getObjectType(ClassType t, ArrayList<String> imp, boolean methodParam) {
        StringBuilder jasminCode = new StringBuilder();

        if (methodParam) {
            jasminCode.append("L");
        }

        int impSize = imp.size();
        boolean typeFound = false;

        switch (t.getName()) {
            case "String":
                jasminCode.append("java/lang/String;");
                typeFound = true;
                break;
            case "Object":
                jasminCode.append("java/lang/Object;");
                typeFound = true;
                break;
        }

        String[] split;

        for (int i = 0; i < impSize && !typeFound; i++) {
            split = imp.get(i).split("\\.");
            if (t.getName().equals(split[split.length - 1])) {
                for (int j = 0; j < split.length - 1; j++) {
                    jasminCode.append(split[j]).append("/");
                }
                jasminCode.append(t.getName());

                if (methodParam) {
                    jasminCode.append(";");
                }

                return jasminCode.toString();
            }
        }

        if (!typeFound) {
            jasminCode.append(t.getName()).append(";");
        }

        return jasminCode.toString();
    }
}
