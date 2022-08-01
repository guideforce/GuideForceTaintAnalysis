package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class Recursion2 {

    // empty sequence
    void infiniteRecursionUnproductive() {
        infiniteRecursionUnproductive();
        TaintAPI.emitA();
    }
}
