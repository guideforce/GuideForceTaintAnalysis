package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop4 {

    boolean cond;

    // a b^* c | a b^\omega
    void infiniteLoopWithBreak() {
        TaintAPI.emitA();
        while (true) {
            if (cond) {
                TaintAPI.emitB();
            } else {
                break;
            }
        }
        TaintAPI.emitC();
    }
}
