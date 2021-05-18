package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

public class WhileLoop9 {

    // a^* c | a^\omega
    int sum(int n) {
        int sum = 0;
        while (n != 0) {
            sum = plus(n , sum);
            n--;
        }
        TaintAPI.emitC();
        return sum;
    }

    int plus(int a , int b) {
        TaintAPI.emitA();
        return a + b;
    }
}
