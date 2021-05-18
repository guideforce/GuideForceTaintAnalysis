package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop2 {

    // a (b c)^\omega
    void infiniteLoop() {
        TaintAPI.emitA();
        while (true) {
            TaintAPI.emitB();
            TaintAPI.emitC();
        }
    }
}
