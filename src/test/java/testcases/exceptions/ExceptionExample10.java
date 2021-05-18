package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample10 {

    boolean cond;

    // Effect: {b,ac}^\omega
    void catchExceptionsInLoop () {
        while (true) {
            try {
                f();
                TaintAPI.emitB();
            } catch (AException e) {
                TaintAPI.emitC();
            }
        }
    }

    void f() throws AException {
        if (cond) {
            TaintAPI.emitA();
            throw new AException();
        }
    }
}
