package testcases.paperexamples;

import ourlib.nonapp.TaintAPI;

public class OverriddenMethodCall {

    void e() {
        A x = new B();
        x.f();
    }
}

class A {
    void f() {
        TaintAPI.emitA();
    }
}

class B extends A {
    void f() {
        TaintAPI.emitB();
    }
}
