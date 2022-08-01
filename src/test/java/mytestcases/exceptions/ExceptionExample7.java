package mytestcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample7 {

    // Effect: a | ac
    void AExceptionNotCaught () throws Exception {
        try{
            TaintAPI.emitA();
            f();
            TaintAPI.emitB();  // unreachable
        } catch (AnotherException e) {
            TaintAPI.emitC();
        }
    }

    boolean cond;

    void f() throws Exception {
        Exception e;
        if (cond) {
            e = new AException();
        } else {
            e = new AnotherException();
        }
        throw e;
    }
}
