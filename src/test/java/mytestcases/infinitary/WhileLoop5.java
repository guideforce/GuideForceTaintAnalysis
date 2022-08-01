package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop5 {

    // a
    void infiniteLoopUnproductive() {
        TaintAPI.emitA();
        while (true) { }
    }
}
