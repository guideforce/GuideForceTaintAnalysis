package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample1 {
    
    // Effect: a
    void throwAException () throws AException {
        TaintAPI.emitA();
        throw new AException();
    }
}
