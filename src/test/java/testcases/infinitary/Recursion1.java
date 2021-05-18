package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class Recursion1 {

    // a^\omega
    void infiniteRecursion() {
        TaintAPI.emitA();
        infiniteRecursion();
    }
}
