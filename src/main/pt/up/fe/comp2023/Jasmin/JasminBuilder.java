package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;
import java.util.ArrayList;

public class JasminBuilder {

    public static int current;
    public static int max;
    public static ClassUnit classUnit;
    private StringBuilder jasminCode;

    public JasminBuilder(ClassUnit classUnit){
        this.jasminCode = new StringBuilder();
        JasminBuilder.classUnit = classUnit;
    }

    public String build(){
        addClass();
        addSuper();
        addFields();
        addMethods();

        return jasminCode.toString();
    }

    private void addClass(){
        jasminCode.append(".class ");
        if (!classUnit.getClassAccessModifier().name().equals("DEFAULT")){
            jasminCode.append(classUnit.getClassAccessModifier().name().toLowerCase());
        }
        else{
            jasminCode.append("public ");
        }
        if (classUnit.isFinalClass()){
            jasminCode.append(" final ");
        }
        else if (classUnit.isStaticClass()){
            jasminCode.append(" static ");
        }

        if (classUnit.getPackage() != null) {
            jasminCode.append(classUnit.getPackage()).append("/").append(classUnit.getClassName()).append("\n");
        }
        else{
            jasminCode.append(classUnit.getClassName()).append("\n");
        }
    }

    private void addSuper() {
        jasminCode.append(".super ");
        for (int i = 0; i < classUnit.getImports().size(); i++){
            if (classUnit.getSuperClass() != null){
                if (classUnit.getImport(i).contains(classUnit.getSuperClass())){
                    String[] parts = classUnit.getImport(i).split("\\.");
                    for (int j = 0; j < parts.length - 1; j++){
                        jasminCode.append(parts[j]).append("/");
                    }
                }
            }
        }
        if (classUnit.getSuperClass() != null){
            jasminCode.append(classUnit.getSuperClass()).append("\n");
        }
        else{
            jasminCode.append("java/lang/Object").append("\n");
        }
    }

    private void addFields() {
        for (int i = 0; i < classUnit.getFields().size(); i++){
            jasminCode.append(".field ");
            if(classUnit.getField(i).getFieldAccessModifier().name().equalsIgnoreCase("default")){
                jasminCode.append("public ");
            }
            else{
                jasminCode.append(classUnit.getField(i).getFieldAccessModifier().name().toLowerCase()).append(" ");
            }
            if (classUnit.getField(i).isStaticField()){
                jasminCode.append("static ");
            }
            if (classUnit.getField(i).isFinalField()){
                jasminCode.append("final ");
            }

            jasminCode.append(classUnit.getField(i).getFieldName()).append(" ");
            String jasminFieldType = JasminTypesInst.getType(classUnit.getField(i).getFieldType(), classUnit.getImports(), true);
            jasminCode.append(jasminFieldType);

            if (classUnit.getField(i).isInitialized()){
                jasminCode.append(" = ").append(classUnit.getField(i).getInitialValue());
            }

            jasminCode.append("\n");
        }
    }
    private void addMethods() {
        for (int index = 0; index < classUnit.getMethods().size(); index++) {
            current = 0;
            max = 0;
            boolean hasReturnStatement = false;
            Method method = classUnit.getMethod(index);

            jasminCode.append(".method ");
            if (method.getMethodAccessModifier().name().equalsIgnoreCase("default")) {
                jasminCode.append("public ");
            } else {
                jasminCode.append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");
            }

            if (method.isStaticMethod()) {
                jasminCode.append("static ");
            }
            if (method.isFinalMethod()) {
                jasminCode.append("final ");
            }

            if (method.isConstructMethod()) {
                jasminCode.append("<init>");
            } else {
                jasminCode.append(method.getMethodName());
            }

            jasminCode.append("(");

            if (method.getParams().size() == 1) {
                jasminCode.append(JasminTypesInst.getType(method.getParam(0).getType(), classUnit.getImports(), true));
            } else if (method.getParams().size() > 1) {
                for (int j = 0; j < method.getParams().size() - 1; j++) {
                    jasminCode.append(JasminTypesInst.getType(method.getParam(j).getType(), classUnit.getImports(), true));
                }
                jasminCode.append(JasminTypesInst.getType(method.getParam(method.getParams().size() - 1).getType(), classUnit.getImports(), true));
            }

            jasminCode.append(")").append(JasminTypesInst.getType(method.getReturnType(), classUnit.getImports(), true)).append("\n");

            StringBuilder instructionsCode = new StringBuilder();

            for (Instruction instruction : method.getInstructions()) {
                for (String label : method.getLabels(instruction)) {
                    instructionsCode.append("\t").append(label).append(":\n");
                }
                instructionsCode.append(JasminTypesInst.getInstructionType(instruction, method));
                if (instruction.getInstType() == InstructionType.RETURN) {
                    hasReturnStatement = true;
                }

                if (instruction.getInstType() == InstructionType.CALL) {
                    if (((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
                        instructionsCode.append("pop\n");
                        Instructions.limitStack(-1);
                    }
                }
            }

            ArrayList<Integer> variables = new ArrayList<>();
            for (Descriptor descriptor : method.getVarTable().values()) {
                if (!variables.contains(descriptor.getVirtualReg())) {
                    variables.add(descriptor.getVirtualReg());
                }
            }
            if (!variables.contains(0) && !method.isStaticMethod()) {
                variables.add(0);
            }

            jasminCode.append(".limit locals ").append(variables.size()).append("\n");
            jasminCode.append("\t").append(".limit stack ").append(max).append("\n");

            jasminCode.append(instructionsCode);

            if (!hasReturnStatement) {
                jasminCode.append("\treturn\n");
            }
            jasminCode.append(".end method").append("\n");
        }

        System.out.println(jasminCode);
    }
}
