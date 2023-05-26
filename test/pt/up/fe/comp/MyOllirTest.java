package pt.up.fe.comp;

import org.junit.Test;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.CpUtils;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.stream.Collectors;

public class MyOllirTest {

    static OllirResult getOllirResult(String filename) {
        return TestUtils.optimize(SpecsIo.getResource("pt/up/fe/comp/myTests/cpf/ollir/" + filename));
    }


    /* checks if method name is correct */
    @Test
    public void methods() {
        var result = getOllirResult("fullIntegration/test.jmm");
        int methods_passed = 0;

        for (String methodName : result.getSymbolTable().getMethods()) {
            Method method = CpUtils.getMethod(result, methodName);
        }

        var method = CpUtils.getMethod(result, "meth");

        CpUtils.assertEquals("Method return type", "void", CpUtils.toString(method.getReturnType()), result);
    }
}
