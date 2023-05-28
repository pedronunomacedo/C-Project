package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import java.util.HashMap;

import static org.specs.comp.ollir.CallType.*;
import static org.specs.comp.ollir.OperationType.*;
import static pt.up.fe.comp2023.Jasmin.JasminBuilder.classUnit;

public class Instructions {

    private static int labelsCount = 0;
    public static String binaryOp(BinaryOpInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();
        Element leftOp = instruction.getLeftOperand();
        Element rightOp = instruction.getRightOperand();

        switch (instruction.getOperation().getOpType()){
            case ADD:
                return manageOperations("iadd", leftOp, rightOp, method);
            case SUB:
                return manageOperations("isub", leftOp, rightOp, method);
            case MUL:
                return manageOperations("imul", leftOp, rightOp, method);
            case DIV:
                return manageOperations("idiv", leftOp, rightOp, method);
            case AND, ANDB:
                return manageOperations("iand", leftOp, rightOp, method);
            case OR, ORB:
                return manageOperations("ior", leftOp, rightOp, method);
            case NEQ : {
                jasminCode.append(loadInst(leftOp, method.getVarTable()));
                jasminCode.append(loadInst(rightOp, method.getVarTable()));
                String compLabel = "NEQ";
                String compOp = "if_icmpne " + compLabel + "_" + labelsCount;
                jasminCode.append(instGenerator(compLabel, compOp));
            }
            case EQ: {
                jasminCode.append(loadInst(leftOp, method.getVarTable()));
                jasminCode.append(loadInst(rightOp, method.getVarTable()));
                String compLabel = "EQ";
                String compOp = "if_icmpeq " + compLabel + "_" + labelsCount;
                jasminCode.append(instGenerator(compLabel, compOp));
            }
            case GTE: {
                jasminCode.append(loadInst(leftOp, method.getVarTable()));
                jasminCode.append(loadInst(rightOp, method.getVarTable()));
                String compLabel = "GTE";
                String compOp = "if_icmpge " + compLabel + "_" + labelsCount;
                jasminCode.append(instGenerator(compLabel, compOp));
            }
            case LTE: {
                jasminCode.append(loadInst(leftOp, method.getVarTable()));
                jasminCode.append(loadInst(rightOp, method.getVarTable()));
                String compLabel = "LTE";
                String compOp = "if_icmple " + compLabel + "_" + labelsCount;
                jasminCode.append(instGenerator(compLabel, compOp));
            }
            case GTH: {
                jasminCode.append(loadInst(leftOp, method.getVarTable()));
                jasminCode.append(loadInst(rightOp, method.getVarTable()));
                String compLabel = "GTH";
                String compOp = "if_icmpgt " + compLabel + "_" + labelsCount;
                jasminCode.append(instGenerator(compLabel, compOp));
            }
            case LTH: {
                jasminCode.append(loadInst(leftOp, method.getVarTable()));
                jasminCode.append(loadInst(rightOp, method.getVarTable()));
                String compLabel = "LTH";
                String compOp = "if_icmplt " + compLabel + "_" + labelsCount;
                jasminCode.append(instGenerator(compLabel, compOp));
            }
        }

        return jasminCode.toString();
    }

    private static String instGenerator(String compLabel, String compOp) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append("\t").append(compOp).append("\n");
        limitStack(-2);
        jasminCode.append("\t").append(JasminTypesInst.loadCType(0)).append("\n");
        limitStack(1);
        jasminCode.append("\t").append("goto ").append(compLabel +"_"+ labelsCount + "_end").append("\n");
        jasminCode.append("\t").append(compLabel).append("_").append(labelsCount).append(":\n");
        jasminCode.append("\t").append(JasminTypesInst.loadCType(1)).append("\n");
        limitStack(1);
        jasminCode.append("\t").append(compLabel).append("_").append(labelsCount).append("_end:\n");
        labelsCount++;
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

        limitStack(-2);
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

    public static String assignInst(AssignInstruction instruction, Method method) {
        Descriptor descriptor = getDescriptor(instruction.getDest(),method);
        StringBuilder jasminCode = new StringBuilder();
        Instruction rhs = instruction.getRhs();
        Element lhs = instruction.getDest();

        if (lhs instanceof ArrayOperand){
            Element index = ((ArrayOperand)lhs).getIndexOperands().get(0);
            int reg = method.getVarTable().get(((Operand)lhs).getName()).getVirtualReg();
            int indexReg = 0;
            if (!(index instanceof LiteralElement)){
                indexReg = method.getVarTable().get(((Operand) index).getName()).getVirtualReg();
            }
            else{
                indexReg = Integer.parseInt(((LiteralElement) index).getLiteral());
            }

            if (reg <= 3){
                jasminCode.append("\t").append("aload_").append(reg).append("\n");
            }
            else{
                jasminCode.append("\t").append("aload ").append(reg).append("\n");
            }
            limitStack(1);
            if (!(index instanceof LiteralElement)) {
                if (indexReg <= 3){
                    jasminCode.append("\t").append("iload_").append(indexReg).append("\n");
                }
                else{
                    jasminCode.append("\t").append("iload ").append(indexReg).append("\n");
                }
            }
            else {
                jasminCode.append("\t").append(JasminTypesInst.loadCType(indexReg)).append("\n");
            }
            limitStack(1);
            jasminCode.append(JasminTypesInst.getInstructionType(rhs,method));
            jasminCode.append("\t").append("iastore\n");
            limitStack(-3);
        }
        else{
            if (rhs instanceof BinaryOpInstruction binaryOpInstruction){
                if (binaryOpInstruction.getOperation().getOpType() == OperationType.ADD || binaryOpInstruction.getOperation().getOpType() == OperationType.SUB){
                    if (binaryOpInstruction.getRightOperand().getType().getTypeOfElement() == ElementType.INT32 && binaryOpInstruction.getRightOperand().isLiteral()){
                        if (getElementName(binaryOpInstruction.getRightOperand()) >= -128 && getElementName(binaryOpInstruction.getRightOperand()) <= 127) {
                            Descriptor varDescriptor;

                            Element leftOperand = binaryOpInstruction.getLeftOperand();
                            Element rightOperand = binaryOpInstruction.getRightOperand();
                            Descriptor leftDescriptor = getDescriptor(leftOperand,method);
                            Descriptor rightDescriptor = getDescriptor(rightOperand,method);

                            if (leftDescriptor == null || leftDescriptor.getVirtualReg()!=descriptor.getVirtualReg()){
                                if (rightDescriptor == null || rightDescriptor.getVirtualReg()!=descriptor.getVirtualReg()){
                                    jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
                                    jasminCode.append(storeElem(lhs, method.getVarTable()));
                                    return jasminCode.toString();
                                }
                                else {
                                    varDescriptor = rightDescriptor;
                                }
                            }
                            else {
                                varDescriptor = leftDescriptor;
                            }

                            if (binaryOpInstruction.getOperation().getOpType() == OperationType.SUB){
                                jasminCode.append("iinc ").append(varDescriptor.getVirtualReg()).append(" ").append(-getElementName(binaryOpInstruction.getRightOperand())).append("\n");
                            }
                            else{
                                jasminCode.append("iinc ").append(varDescriptor.getVirtualReg()).append(" ").append(getElementName(binaryOpInstruction.getRightOperand())).append("\n");
                            }
                        }
                        else {
                            jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
                            jasminCode.append(storeElem(lhs, method.getVarTable()));
                        }
                    }
                    else if (binaryOpInstruction.getLeftOperand().getType().getTypeOfElement() == ElementType.INT32 && binaryOpInstruction.getLeftOperand().isLiteral()){
                        if (getElementName(binaryOpInstruction.getLeftOperand()) >= -128 && getElementName(binaryOpInstruction.getLeftOperand()) <= 127){
                            Descriptor varDescriptor;

                            Element leftOperand = binaryOpInstruction.getLeftOperand();
                            Element rightOperand = binaryOpInstruction.getRightOperand();
                            Descriptor leftDescriptor = getDescriptor(leftOperand,method);
                            Descriptor rightDescriptor = getDescriptor(rightOperand,method);

                            if (leftDescriptor == null || leftDescriptor.getVirtualReg()!=descriptor.getVirtualReg()){
                                if (rightDescriptor == null || rightDescriptor.getVirtualReg()!=descriptor.getVirtualReg()){
                                    jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
                                    jasminCode.append(storeElem(lhs, method.getVarTable()));
                                    return jasminCode.toString();
                                }
                                else {
                                    varDescriptor = rightDescriptor;
                                }
                            }
                            else {
                                varDescriptor = leftDescriptor;
                            }

                            if (binaryOpInstruction.getOperation().getOpType() == OperationType.SUB){
                                jasminCode.append("iinc ").append(varDescriptor.getVirtualReg()).append(" ").append(-getElementName(binaryOpInstruction.getLeftOperand())).append("\n");
                            }
                            else{
                                jasminCode.append("iinc ").append(varDescriptor.getVirtualReg()).append(" ").append(getElementName(binaryOpInstruction.getLeftOperand())).append("\n");
                            }
                        }
                        else {
                            jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
                            jasminCode.append(storeElem(lhs, method.getVarTable()));
                        }
                    }
                    else {
                        jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
                        jasminCode.append(storeElem(lhs, method.getVarTable()));
                    }
                }
                else {
                    jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
                    jasminCode.append(storeElem(lhs, method.getVarTable()));
                }
            }
            else {
                jasminCode.append(JasminTypesInst.getInstructionType(rhs, method));
                jasminCode.append(storeElem(lhs, method.getVarTable()));
            }
        }
        return jasminCode.toString();
    }

    public static String returnInst(ReturnInstruction instruction, Method method) {
        StringBuilder result = new StringBuilder();
        Element operand = instruction.getOperand();

        if (!instruction.hasReturnValue()) {
            return "\treturn\n";
        }

        result.append(loadInst(operand, method.getVarTable()));

        if (operand.getType().getTypeOfElement() == ElementType.INT32 || operand.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            result.append("\tireturn\n");
            limitStack(-1);
        } else {
            result.append("\tareturn\n");
            limitStack(-1);
        }

        return result.toString();
    }

    public static String branchInst(CondBranchInstruction instruction, Method method) {
        StringBuilder jasminCode = new StringBuilder();

        if (instruction instanceof SingleOpCondInstruction) {
            Element conditionOperand = ((SingleOpCondInstruction) instruction).getCondition().getSingleOperand();
            jasminCode.append(loadInst(conditionOperand, method.getVarTable()));
            jasminCode.append("ifne ").append(instruction.getLabel()).append("\n");
            limitStack(-1);
        } else if (instruction instanceof OpCondInstruction) {
            OperationType operationType;
            OpInstruction conditionInstruction = ((OpCondInstruction) instruction).getCondition();

            if (conditionInstruction instanceof BinaryOpInstruction binaryOpInstruction) {
                operationType = binaryOpInstruction.getOperation().getOpType();
                jasminCode.append(loadInst(binaryOpInstruction.getLeftOperand(), method.getVarTable()));
                jasminCode.append(loadInst(binaryOpInstruction.getRightOperand(), method.getVarTable()));

                switch (operationType) {
                    case AND, ANDB -> {jasminCode.append("\t").append("iand").append("\n").append("\t").append("ifne ").append(instruction.getLabel()).append("\n"); limitStack(-2);}
                    case OR, ORB -> {jasminCode.append("\t").append("ior").append("\n").append("\t").append("ifne ").append(instruction.getLabel()).append("\n"); limitStack(-2);}
                    case LTH -> {jasminCode.append("\t").append("if_icmplt ").append(instruction.getLabel()).append("\n"); limitStack(-2);}
                    case GTH -> {jasminCode.append("\t").append("if_icmpgt ").append(instruction.getLabel()).append("\n"); limitStack(-2);}
                    case LTE -> {jasminCode.append("\t").append("if_icmple ").append(instruction.getLabel()).append("\n"); limitStack(-2);}
                    case GTE -> {jasminCode.append("\t").append("if_icmpge ").append(instruction.getLabel()).append("\n");}
                    case EQ -> {jasminCode.append("\t").append("if_icmpeq ").append(instruction.getLabel()).append("\n"); limitStack(-2);}
                    case NEQ -> {jasminCode.append("\t").append("if_icmpne ").append(instruction.getLabel()).append("\n"); limitStack(-2);}
                }
            } else if (conditionInstruction instanceof UnaryOpInstruction unaryOpInstruction) {
                operationType = unaryOpInstruction.getOperation().getOpType();
                jasminCode.append(loadInst(unaryOpInstruction.getOperand(), method.getVarTable()));

                switch (operationType) {
                    case NOT, NOTB -> {jasminCode.append("\t").append("ifeq ").append(instruction.getLabel()).append("\n");limitStack(-1);}
                }
            }
        }

        return jasminCode.toString();
    }



    public static String loadInst(Element op, HashMap<String, Descriptor> varTable) {
        if (op.isLiteral()){
            limitStack(1);
            return "\t" + JasminTypesInst.loadCType(Integer.parseInt((((LiteralElement)op).getLiteral()))) + "\n";
        }
        int virtualReg = varTable.get(((Operand)op).getName()).getVirtualReg();
        StringBuilder jasminCode = new StringBuilder();
        if (op.getType().getTypeOfElement() == ElementType.INT32 || op.getType().getTypeOfElement() == ElementType.BOOLEAN){
            if (varTable.get(((Operand)op).getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF){
                jasminCode.append(loadArrayElem(op, varTable));
            }
            else{
                if (virtualReg <= 3){
                    jasminCode.append("\t").append("iload_").append(virtualReg).append("\n");
                }
                else{
                    jasminCode.append("\t").append("iload ").append(virtualReg).append("\n");
                }
                limitStack(1);
            }
        }
        else {
            if (virtualReg <= 3){
                jasminCode.append("\t").append("aload_").append(virtualReg).append("\n");
            }
            else{
                jasminCode.append("\t").append("aload ").append(virtualReg).append("\n");
            }
            limitStack(1);
        }

        return jasminCode.toString();
    }

    private static String loadArrayElem(Element operand, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        Element index = ((ArrayOperand)operand).getIndexOperands().get(0);
        int reg = varTable.get(((Operand) operand).getName()).getVirtualReg();
        int indexReg = 0;

        if (!(index instanceof LiteralElement)){
            indexReg = varTable.get(((Operand) index).getName()).getVirtualReg();
        }
        else{
            indexReg = Integer.parseInt(((LiteralElement) index).getLiteral());
        }

        if (reg <= 3){
            jasminCode.append("aload_").append(reg).append("\n");
        }
        else{
            jasminCode.append("aload ").append(reg).append("\n");
        }
        limitStack(1);
        if (!(index instanceof LiteralElement)) {
            if (indexReg <= 3){
                jasminCode.append("\t").append("iload_").append(indexReg).append("\n");
            }
            else{
                jasminCode.append("\t").append("iload ").append(indexReg).append("\n");
            }
        }
        else {
            jasminCode.append("\t").append(JasminTypesInst.loadCType(indexReg)).append("\n");
        }
        limitStack(1);
        jasminCode.append("iaload").append("\n");
        limitStack(-1);

        return jasminCode.toString();
    }

    public static String storeElem(Element elem, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        int virtualReg = varTable.get(((Operand)elem).getName()).getVirtualReg();

        if (elem.getType().getTypeOfElement() == ElementType.INT32 || elem.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            if (varTable.get(((Operand)elem).getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                jasminCode.append(storeArray(elem, varTable));
            } else {
                if (virtualReg <= 3){
                    jasminCode.append("\t").append("istore_").append(virtualReg).append("\n");
                }
                else{
                    jasminCode.append("\t").append("istore ").append(virtualReg).append("\n");
                }
                limitStack(-1);
            }
        } else {
            if (virtualReg <= 3){
                jasminCode.append("\t").append("astore_").append(virtualReg).append("\n");
            }
            else{
                jasminCode.append("\t").append("astore ").append(virtualReg).append("\n");
            }
            limitStack(-1);
        }

        return jasminCode.toString();
    }

    private static String storeArray(Element operand, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();
        Element index = ((ArrayOperand)operand).getIndexOperands().get(0);
        int reg = varTable.get(((Operand) operand).getName()).getVirtualReg();
        int indexReg = 0;
        if (!(index instanceof LiteralElement)){
            indexReg = varTable.get(((Operand) index).getName()).getVirtualReg();
        }
        else{
            indexReg = Integer.parseInt(((LiteralElement) index).getLiteral());
        }
        if (reg <= 3){
            jasminCode.append("astore_").append(reg).append("\n");
        }
        else{
            jasminCode.append("astore ").append(reg).append("\n");
        }
        limitStack(-1);
        if (indexReg <= 3){
            jasminCode.append("istore_").append(indexReg).append("\n");
        }
        else{
            jasminCode.append("istore ").append(indexReg).append("\n");
        }
        limitStack(-1);
        jasminCode.append("iastore").append("\n");
        limitStack(-3);

        return jasminCode.toString();
    }

    public static String manageOperations(String op, Element left, Element right, Method m) {
        StringBuilder code = new StringBuilder();
        code.append(Instructions.loadInst(left, m.getVarTable()))
                .append(Instructions.loadInst(right, m.getVarTable()))
                .append(op).append("\n");

        limitStack(-1);
        return code.toString();
    }

    public static void limitStack(int sizeChange) {
        JasminBuilder.current += sizeChange;
        JasminBuilder.max = Math.max(JasminBuilder.max, JasminBuilder.current);
    }

    public static Descriptor getDescriptor(Element elem,Method method) {
        if (elem.isLiteral()){
            return null;
        }
        if(elem.getType().getTypeOfElement() == ElementType.THIS) {
            return method.getVarTable().get("this");
        }
        return method.getVarTable().get(((Operand)elem).getName());
    }

    public static int getElementName(Element elem) {
        if (elem.isLiteral())
            return Integer.parseInt(((LiteralElement) elem).getLiteral());
        return Integer.parseInt(((Operand) elem).getName());
    }
}
