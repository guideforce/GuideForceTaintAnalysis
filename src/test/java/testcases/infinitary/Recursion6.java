package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class Recursion6 {

    int gcd(int a, int b) {
        if (b == 0) {
            TaintAPI.emitA();
            return a;
        } else {
            int n = gcd(b, a % b);
            TaintAPI.emitB();
            return n;
        }
    }
}
