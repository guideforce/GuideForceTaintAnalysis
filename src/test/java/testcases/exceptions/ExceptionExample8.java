package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample8 {

    // Effect: ac
    void throwExceptionsInHandler () throws Exception {
        try{
            TaintAPI.emitA();
            f();
            TaintAPI.emitB();  // unreachable
        } catch (AException e) {
            TaintAPI.emitC();
            g();
            TaintAPI.emitB();  // unreachable
        }
    }

    void f() throws AException {
        throw new AException();
    }

    void g() throws AnotherException {
        throw new AnotherException();
    }
}
