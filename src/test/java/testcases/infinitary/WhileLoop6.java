package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop6 {

    // a (b c)^\omega
    void loopWithMethodCall() {
        TaintAPI.emitA();
        while (true) {
            f();
            TaintAPI.emitC();
        }
    }

    void f() {
        TaintAPI.emitB();
    }
}
