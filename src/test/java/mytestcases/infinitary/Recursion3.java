package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class Recursion3 {

    // (a b c)^\omega
    void mutualRecursion() {
        TaintAPI.emitA();
        f();
    }

    // (b c a)^\omega
    void f() {
        TaintAPI.emitB();
        g();
    }

    // (c a b)^\omega
    void g() {
        TaintAPI.emitC();
        mutualRecursion();
    }
}
