package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class ForLoop1 {

    boolean cond;

    // a b^* c | a b^\omega
    void loop() {
        TaintAPI.emitA();
        for (int i = 0; cond; i++) {
            TaintAPI.emitB();
        }
        TaintAPI.emitC();
    }
}
