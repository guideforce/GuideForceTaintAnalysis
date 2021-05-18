package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample9 {

    boolean cond;

    // Effect: b^* a | b^\omega
    void throwExceptionsInLoop () throws Exception {
        while (true) {
            f();
            TaintAPI.emitB();
        }
    }

    void f() throws AException {
        if (cond) {
            TaintAPI.emitA();
            throw new AException();
        }
    }
}
