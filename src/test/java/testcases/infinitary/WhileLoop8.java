package testcases.infinitary;

import ourlib.nonapp.TaintAPI;

/**
 * Example from Hofmann and Chen's paper
 */
public class WhileLoop8 {

    // (a^* c b)^* a | (a^* c b)^* a^\omega | (a^* c b)^\omega | a^\omega
    void nestedLoops() {
        int i = 0;
        while (i < 1) {
            while (i++ < 1000){
                TaintAPI.emitA();
            }
            TaintAPI.emitC();
            TaintAPI.emitB();
        }
        TaintAPI.emitA();
    }
}
