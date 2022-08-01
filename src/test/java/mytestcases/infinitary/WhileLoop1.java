package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop1 {

    boolean cond;

    // a b^* c | a b^\omega
    void loop() {
        TaintAPI.emitA();
        while (cond) {
            TaintAPI.emitB();
        }
        TaintAPI.emitC();
    }
}
