package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class ForLoop4 {

    // b | a^* c | a^\omega
    int fibonacci(int n) {
        int a = 0;
        int b = 1;
        int c;

        if (n == 0) {
            TaintAPI.emitB();
            return a;
        }

        for (int i = 2; i <= n; i++) {
            c = plus(a , b);
            a = b;
            b = c;
        }
        TaintAPI.emitC();
        return b;
    }

    int plus(int a , int b) {
        TaintAPI.emitA();
        return a + b;
    }
}