package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample6 {

    // Effect: a
    void noExceptionCaught () throws Exception {
        try{
            TaintAPI.emitA();
            f();
            TaintAPI.emitB(); // unreachable
        } catch (SubException e) {  // subclass of AException
            TaintAPI.emitC();  // unreachable
        } catch (AnotherException e) {
            TaintAPI.emitC();  // unreachable
        }
        TaintAPI.emitB(); // unreachable
    }

    void f() throws Exception {
        throw new AException();
    }
}
