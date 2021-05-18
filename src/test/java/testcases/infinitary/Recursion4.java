package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class Recursion4 {

    // c a^* | empty sequence (when diverging)
    int factorial(int n) {
        if (n == 1) {
            TaintAPI.emitC();
            return 1;
        } else {
            int m = factorial(n - 1);
            TaintAPI.emitA();
            return n * m;
        }
    }
}
