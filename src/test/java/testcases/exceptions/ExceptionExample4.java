package testcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample4 {

    // Effect: ab
    void catchAException () {
        try{
            TaintAPI.emitA();
            throw new AException();
        } catch (AException e) {
            TaintAPI.emitB();
        }
    }
}
