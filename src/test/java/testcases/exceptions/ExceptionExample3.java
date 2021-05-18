package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample3 {

    boolean cond;

    // Effect: a | c
    void throwExceptions () throws Exception {
        if (cond) {
            TaintAPI.emitA();
            throw new AException();
        } else {
            TaintAPI.emitC();
            throw new AnotherException();
        }
    }
}
