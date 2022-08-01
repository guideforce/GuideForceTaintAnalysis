package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop7 {

    boolean cond;

    // a b (c b)^* | a b (c b)^\omega
    void loopWithMethodCallInCondition() {
        TaintAPI.emitA();
        while (f()) {
            TaintAPI.emitC();
        }
    }

    boolean f() {
        TaintAPI.emitB();
        return cond;
    }
}
