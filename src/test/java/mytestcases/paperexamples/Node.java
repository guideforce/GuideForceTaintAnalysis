package mytestcases.paperexamples;

import ourlib.nonapp.TaintAPI;

class Node {

    Node next;

    Node last() {
        TaintAPI.emitA();
        if (next == null) {
            return this;
        } else {
            return next.last();
        }
    }

}

class Test {

    Node linear () {
        Node x = new Node();
        Node y = new Node();
        y.next = x;
        return y.last();
    }

    Node cyclic () {
        Node z = new Node();
        z.next = z;
        return z.last();
    }
}
