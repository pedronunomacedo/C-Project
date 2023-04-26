package pt.up.fe.comp2023.Jasmin;

import java.util.Collections;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class BackendJasmin implements JasminBackend{

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit classUnit=ollirResult.getOllirClass();
        classUnit.buildCFGs();
        classUnit.buildVarTables();
        classUnit.show();
        JasminBuilder jasminBuilder = new JasminBuilder(classUnit);
        String jasminCode = jasminBuilder.build();
        return new JasminResult(ollirResult, jasminCode, Collections.emptyList());
    }
}
