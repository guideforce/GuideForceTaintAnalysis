package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample2 {

    boolean cond;

    // Effect: a | b
    void mayThrowAException () throws AException {
        if (cond) {
            TaintAPI.emitA();
            throw new AException();
        }
        TaintAPI.emitB();
    }
}
