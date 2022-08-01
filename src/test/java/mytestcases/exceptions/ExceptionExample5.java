package mytestcases.exceptions;

import ourlib.nonapp.TaintAPI;

public class ExceptionExample5 {

    // Effect: ba | bb | bc
    void catchExceptions () {
        try{
            f();
            TaintAPI.emitA();  // unreachable
        } catch (SubException e) {
            TaintAPI.emitB();
            TaintAPI.emitA();
        } catch (AException e) {
            TaintAPI.emitB();
            TaintAPI.emitB();
        } catch (AnotherException e) {
            TaintAPI.emitB();
            TaintAPI.emitC();
        } catch (Exception e) {
            TaintAPI.emitC();  // unreachable
        }
    }

    boolean cond1, cond2;

    void f() throws Exception {
        if (cond1) {
            throw new AException();
        } else if (cond2) {
            throw new SubException();
        } else {
            throw new AnotherException();
        }
    }
}
