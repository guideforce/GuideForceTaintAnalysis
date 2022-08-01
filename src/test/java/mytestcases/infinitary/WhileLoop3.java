package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop3 {

    boolean cond;

    // a {b,c}^\omega
    void infiniteLoopWithBranches() {
        TaintAPI.emitA();
        while (true) {
            if (cond) {
                TaintAPI.emitB();
            } else {
                TaintAPI.emitC();
            }
        }
    }
}
