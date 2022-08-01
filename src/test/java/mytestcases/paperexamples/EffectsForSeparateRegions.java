package mytestcases.paperexamples;

import ourlib.nonapp.TaintAPI;

public class EffectsForSeparateRegions {

    boolean cond;

    C e() {
        if (cond) {
            return new C();
        } else {
            return new D();
        }
    }

    void e1() {
        C x = e();
        x.f();
    }

    void e2() {
        C x = e();
        x.f();
        x.f();
    }

    void e3() {
        C x = e();
        x.g();
    }
}

class C {

    void f() {
        TaintAPI.emitA();
    }

    void g() {
        f();
        f();
    }
}

class D extends C {

    void f() {
        TaintAPI.emitB();
    }
}
