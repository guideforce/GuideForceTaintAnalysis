package mytestcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class Recursion5 {

    int fibonacci(int n) {
        if (n <= 1) {
            TaintAPI.emitA();
            return n;
        } else {
            int a = fibonacci(n - 1);
            TaintAPI.emitB();
            int b = fibonacci(n - 2);
            TaintAPI.emitC();
            return a + b;
        }
    }
}
