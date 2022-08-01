package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class ForLoop3 {

    // a^* c | a^\omega
    void forEachLoop(int n) {
        A[] as = new A[n];
        for (A a : as) {
            a = new A();
        }
        TaintAPI.emitC();
    }

    class A {
        A () {
            TaintAPI.emitA();
        }
    }
}