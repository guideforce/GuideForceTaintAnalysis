package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class ForLoop2 {

    boolean cond;

    // a b (c b)^* | a (b c)^\omega
    void loopWithMethodCalls() {
        for (f1(); f2(); f3()) { }
    }

    void f1() {
        TaintAPI.emitA();
    }

    boolean f2() {
        TaintAPI.emitB();
        return cond;
    }

    void f3() {
        TaintAPI.emitC();
    }
}
