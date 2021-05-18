package guideforce;

import guideforce.interproc.EffectType;
import guideforce.interproc.InterProcAnalysis;
import guideforce.policy.*;
import guideforce.policy.AbstractDomain.*;
import org.junit.Test;
import soot.G;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class Evaluation {

    private static String classPath = "build/classes/java/test/" + File.pathSeparator +
            "build/classes/java/main/" + File.pathSeparator +
            "lib/cos.jar" + File.pathSeparator +
            "lib/j2ee.jar" + File.pathSeparator +
            "lib/java2html.jar";

    // a policy for taintedness analysis
    Policy binaryPolicy = new BinaryPolicy();

    // All accesses to sensitive data are authorized.
    Policy authorizedAccessPolicy = new AuthorizedAccessPolicy();

    // All accesses to sensitive data are logged.
    Policy loggedAccessPolicy = new LoggedAccessPolicy();

    // a policy for generating abstract effects such that
    // those representing finite words of length less than 3 can be distinguished
    Policy abcPolicy = new ABCPolicy();
    AbstractDomain domain = abcPolicy.getAbstractDomain();
    Finitary a = domain.makeFinitary(domain.read(ABCPolicy.Token.A));
    Finitary b = domain.makeFinitary(domain.read(ABCPolicy.Token.B));
    Finitary c = domain.makeFinitary(domain.read(ABCPolicy.Token.C));
    Infinitary emptyInfSeqence = domain.oneFinitary().asInfinitary();
    Finitary diverges = domain.zeroFinitary();  // Empty finitary effect means divergence
    Infinitary terminates = domain.zeroInfinitary();  // Empty infinitatry effects means termination

    /**
     * Verifies if the given method adheres to the given policy,
     * and then asserts true if the result is the same as expected.
     * @param className   the class of the method to verify
     * @param methodName  the method to verify
     * @param policy      the policy to verify
     * @param finExpect   the expected finitary effect
     * @param infExpect   the expected infinitary effect
     */
    public void test(String className, String methodName, Policy policy,
                     Finitary finExpect, Infinitary infExpect) {
        G.reset();
        TSA tsa = new TSA(classPath, className);
        InterProcAnalysis analysis = tsa.run(policy, 1, methodName);

        //System.out.println(analysis.analysisResult());
        EffectType te = analysis.getTypeAndEffectsAtEntryPoint();
        Finitary finitary = te.getAggregateFinitary();
        Infinitary infinitary = te.getInfinitary().getConstantTerm();

        assertEquals(finExpect, finitary);
        assertEquals(infExpect, infinitary);
    }

    /**
     * Verifies if the given method adheres to the taintedness policy,
     * and then asserts true if the result is the same as expected.
     * @param className   the class of the method to verify
     * @param methodName  the method to verify
     * @param expect      the expected result, i.e. whether the method adheres to the policy
     */
    public void taintednessTest(String className, String methodName, boolean expect) {
        G.reset();
        TSA tsa = new TSA(classPath, className);
        InterProcAnalysis analysis = tsa.run(binaryPolicy, 1, methodName);

        //System.out.println(analysis.analysisResult());
        EffectType te = analysis.getTypeAndEffectsAtEntryPoint();
        Finitary finitary = te.getAggregateFinitary();
        Infinitary infinitary = te.getInfinitary().getConstantTerm();

        boolean result = finitary.accepted() & infinitary.accepted();
        assertEquals(expect, result);
    }

    /* ======================== Testing paper examples ======================== */

    @Test
    public void authorizedAccess() {
        AbstractDomain domain = authorizedAccessPolicy.getAbstractDomain();
        Finitary auth = domain.makeFinitary(domain.read(AuthorizedAccessPolicy.Token.auth));
        Finitary access = domain.makeFinitary(domain.read(AuthorizedAccessPolicy.Token.access));
        Finitary log = domain.makeFinitary(domain.read(AuthorizedAccessPolicy.Token.log));
        Finitary finitary = auth.join(auth.multiply(access)).star().multiply(log);
        Infinitary infinitary = auth.join(auth.multiply(access)).omega();
        test("testcases.paperexamples.Server", "serve", authorizedAccessPolicy,
                finitary,
                infinitary) ;
    }

    @Test
    public void loggedAccess() {
        AbstractDomain domain = loggedAccessPolicy.getAbstractDomain();
        Finitary auth = domain.makeFinitary(domain.read(LoggedAccessPolicy.Token.auth));
        Finitary access = domain.makeFinitary(domain.read(LoggedAccessPolicy.Token.access));
        Finitary log = domain.makeFinitary(domain.read(LoggedAccessPolicy.Token.log));
        Finitary finitary = auth.join(auth.multiply(access)).star().multiply(log);
        Infinitary infinitary = auth.join(auth.multiply(access)).omega();
        test("testcases.paperexamples.Server", "serve", loggedAccessPolicy,
                finitary,
                infinitary) ;
    }

    @Test
    public void linear() {
        test("testcases.paperexamples.Test", "linear", abcPolicy,
                a.join(a.multiply(a)),
                domain.zeroInfinitary());
    }

    @Test
    public void cyclic() {
        Finitary aa = a.multiply(a);
        Finitary aaa = a.multiply(aa);
        Finitary aaaa = a.multiply(aaa);
        test("testcases.paperexamples.Test", "cyclic", abcPolicy,
                a.join(aa).join(aaa).join(aaaa),  // all nonempty finite sequences of as
                a.omega());
    }

    @Test
    public void EffectsForSeparateRegions() {
        test("testcases.paperexamples.EffectsForSeparateRegions", "e1", abcPolicy,
                a.join(b),
                domain.zeroInfinitary());
        test("testcases.paperexamples.EffectsForSeparateRegions", "e2", abcPolicy,
                a.multiply(a).join(b.multiply(b)),
                domain.zeroInfinitary());
        test("testcases.paperexamples.EffectsForSeparateRegions", "e3", abcPolicy,
                a.multiply(a).join(b.multiply(b)),
                domain.zeroInfinitary());
    }

    @Test
    public void OverwrittenMethodCall() {
        test("testcases.paperexamples.OverriddenMethodCall", "e", abcPolicy,
                b,
                domain.zeroInfinitary());
    }

    /* ======================== End of testing paper examples ======================== */


    /* ======================== Testing infinitary analysis ======================== */

    @Test
    public void WhileLoop01() {
        test("testcases.infinitary.WhileLoop1", "loop", abcPolicy,
                a.multiply(b.star()).multiply(c),  // a b^* c
                b.omega().multiplyLeft(a));  // a b^\omega
    }

    @Test
    public void WhileLoop02() {
        test("testcases.infinitary.WhileLoop2", "infiniteLoop", abcPolicy,
                domain.zeroFinitary(),
                b.multiply(c).omega().multiplyLeft(a));  // a (b c)^\omega
    }

    @Test
    public void WhileLoop03() {
        test("testcases.infinitary.WhileLoop3", "infiniteLoopWithBranches", abcPolicy,
                domain.zeroFinitary(),
                (b.join(c)).omega().multiplyLeft(a));  // a {b,c}^\omega
    }

    @Test
    public void WhileLoop04() {
        test("testcases.infinitary.WhileLoop4", "infiniteLoopWithBreak", abcPolicy,
                a.multiply(b.star()).multiply(c),  // a b^* c
                b.omega().multiplyLeft(a));  // a b^\omega
    }

    @Test
    public void WhileLoop05() {
        test("testcases.infinitary.WhileLoop5", "infiniteLoopUnproductive", abcPolicy,
                domain.zeroFinitary(),
                a.asInfinitary());  // a
    }

    @Test
    public void WhileLoop06() {
        test("testcases.infinitary.WhileLoop6", "loopWithMethodCall", abcPolicy,
                domain.zeroFinitary(),
                b.multiply(c).omega().multiplyLeft(a));  // a (b c)^\omega
    }

    @Test
    public void WhileLoop07() {
        test("testcases.infinitary.WhileLoop7", "loopWithMethodCallInCondition", abcPolicy,
                a.multiply(b).multiply(c.multiply(b).star()),  // a b (c b)^*
                c.multiply(b).omega().multiplyLeft(a.multiply(b)));  // a b (c b)^\omega
    }

    @Test
    public void WhileLoop08() {
        test("testcases.infinitary.WhileLoop8", "nestedLoops", abcPolicy,
                a.star().multiply(c).multiply(b).star().multiply(a),  // (a^* c b)^* a
                a.star().multiply(c).multiply(b).omega()  // (a^* c b)^\omega
                        .join(a.omega().multiplyLeft(a.star().multiply(c).multiply(b).star()))  // (a^* c b)^* a^\omega
                        .join(a.omega()));  // a^\omega
    }

    @Test
    public void WhileLoop09() {
        test("testcases.infinitary.WhileLoop9", "sum", abcPolicy,
                a.star().multiply(c),  // a^* c
                a.omega());  // a^\omega
    }

    @Test
    public void ForLoop01() {
        test("testcases.infinitary.ForLoop1", "loop", abcPolicy,
                a.multiply(b.star()).multiply(c),  // a b^* c
                b.omega().multiplyLeft(a));  // a b^\omega
    }

    @Test
    public void ForLoop02() {
        test("testcases.infinitary.ForLoop2", "loopWithMethodCalls", abcPolicy,
                a.multiply(b).multiply(c.multiply(b).star()),  // a b (c b)^*
                b.multiply(c).omega().multiplyLeft(a));  // a (b c)^\omega
    }

    @Test
    public void ForLoop03() {
        test("testcases.infinitary.ForLoop3", "forEachLoop", abcPolicy,
                a.star().multiply(c),  // a^* c
                a.omega());  // a^\omega
    }

    @Test
    public void ForLoop04() {
        test("testcases.infinitary.ForLoop4", "fibonacci", abcPolicy,
                b.join(a.star().multiply(c)),  // b | a^* c
                a.omega());  // a^\omega
    }

    @Test
    public void Recursion01() {
        test("testcases.infinitary.Recursion1", "infiniteRecursion", abcPolicy,
                domain.zeroFinitary(),
                a.omega());  // a^\omega
    }

    @Test
    public void Recursion02() {
        test("testcases.infinitary.Recursion2", "infiniteRecursionUnproductive", abcPolicy,
                domain.zeroFinitary(),
                emptyInfSeqence);  // empty sequence
    }

    @Test
    public void Recursion03() {
        test("testcases.infinitary.Recursion3", "mutualRecursion", abcPolicy,
                domain.zeroFinitary(),
                a.multiply(b).multiply(c).omega());  // (a b c)^\omega
    }

    @Test
    public void Recursion04() {
        test("testcases.infinitary.Recursion4", "factorial", abcPolicy,
                c.multiply(a.star()),  // c a^*
                emptyInfSeqence);  // empty sequence
    }

    @Test
    public void Recursion05() {
        Finitary abc = a.multiply(b).multiply(c);
        test("testcases.infinitary.Recursion5", "fibonacci", abcPolicy,
                a.join(abc),
                emptyInfSeqence
                        .join(a.multiply(b).asInfinitary())
                        .join(abc.asInfinitary())
                        .join(abc.omega()));
    }

    @Test
    public void Recursion06() {
        Finitary ab = a.multiply(b);
        Finitary abc = ab.multiply(c);
        test("testcases.infinitary.Recursion6", "gcd", abcPolicy,
                a.join(ab).join(abc),
                emptyInfSeqence);
    }

    /* ======================== End of testing infinitary analysis ======================== */


    /* ======================== Testing exceptions ======================== */

    @Test
    public void Exceptions01() {
        test("testcases.exceptions.ExceptionExample1", "throwAException", abcPolicy,
                a,
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions02() {
        test("testcases.exceptions.ExceptionExample2", "mayThrowAException", abcPolicy,
                a.join(b),  // a | b
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions03() {
        test("testcases.exceptions.ExceptionExample3", "throwExceptions", abcPolicy,
                a.join(c),  // a | c
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions04() {
        test("testcases.exceptions.ExceptionExample4", "catchAException", abcPolicy,
                a.multiply(b),  // ab
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions05() {
        test("testcases.exceptions.ExceptionExample5", "catchExceptions", abcPolicy,
                b.multiply(a).join(b.multiply(b)).join(b.multiply(c)),  // ba | bb | bc
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions06() {
        test("testcases.exceptions.ExceptionExample6", "noExceptionCaught", abcPolicy,
                a,
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions07() {
        test("testcases.exceptions.ExceptionExample7", "AExceptionNotCaught", abcPolicy,
                a.join(a.multiply(c)),  // a | ac
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions08() {
        test("testcases.exceptions.ExceptionExample8", "throwExceptionsInHandler", abcPolicy,
                a.multiply(c),  // ac
                domain.zeroInfinitary());
    }

    @Test
    public void Exceptions09() {
        test("testcases.exceptions.ExceptionExample9", "throwExceptionsInLoop", abcPolicy,
                b.star().multiply(a),  // b^* a
                b.omega());
    }

    @Test
    public void Exceptions10() {
        test("testcases.exceptions.ExceptionExample10", "catchExceptionsInLoop", abcPolicy,
                domain.zeroFinitary(),
                a.multiply(c).join(b).omega());
    }

    /* ======================== End of testing exceptions ======================== */


    /* ======================== Testing securibench micro ======================== */

    /* ------------------------ Testing aliasing ------------------------ */

    @Test
    public void Aliasing01() {
        taintednessTest("securibench.micro.aliasing.Aliasing1", "doGet", false) ;
    }

    @Test
    public void Aliasing02() {
        taintednessTest("securibench.micro.aliasing.Aliasing2", "doGet", true) ;
    }

    @Test
    public void Aliasing03() {
        taintednessTest("securibench.micro.aliasing.Aliasing3", "doGet", false) ;
    }

    @Test
    public void Aliasing04() {
        taintednessTest("securibench.micro.aliasing.Aliasing4", "doGet", false) ;
    }

    @Test
    public void Aliasing05() {
        taintednessTest("securibench.micro.aliasing.Aliasing5", "doGet", false) ;
    }

    @Test
    public void Aliasing06() {
        taintednessTest("securibench.micro.aliasing.Aliasing6", "doGet", false) ;
    }

    /* ------------------------ End of testing aliasing ------------------------ */

    /* ------------------------ Testing arrays ------------------------ */

    @Test
    public void Arrays01() {
        taintednessTest("securibench.micro.arrays.Arrays1", "doGet", false) ;
    }

    @Test
    public void Arrays02() {
        taintednessTest("securibench.micro.arrays.Arrays2", "doGet", false) ;
    }

    @Test
    public void Arrays03() {
        taintednessTest("securibench.micro.arrays.Arrays3", "doGet", false) ;
    }

    @Test
    public void Arrays04() {
        taintednessTest("securibench.micro.arrays.Arrays4", "doGet", false) ;
    }

    /* Fail: once an array has a tainted elements, we assume the array to be tainted,
             i.e. all its elements may be tainted.*/
    @Test
    public void Arrays05() {
        taintednessTest("securibench.micro.arrays.Arrays5", "doGet", true) ;
    }

    @Test
    public void Arrays06() {
        taintednessTest("securibench.micro.arrays.Arrays6", "doGet", false) ;
    }

    @Test
    public void Arrays07() {
        taintednessTest("securibench.micro.arrays.Arrays7", "doGet", false) ;
    }

    @Test
    public void Arrays08() {
        taintednessTest("securibench.micro.arrays.Arrays8", "doGet", false) ;
    }

    @Test
    public void Arrays09() {
        taintednessTest("securibench.micro.arrays.Arrays9", "doGet", false) ;
    }

    @Test
    public void Arrays10() {
        taintednessTest("securibench.micro.arrays.Arrays10", "doGet", false) ;
    }

    /* ------------------------ End of testing arrys ------------------------ */

    /* ------------------------ Testing basic ------------------------ */

    @Test
    public void Basic01() {
        taintednessTest("securibench.micro.basic.Basic1", "doGet", false) ;
    }

    @Test
    public void Basic02() {
        taintednessTest("securibench.micro.basic.Basic2", "doGet", false) ;
    }

    @Test
    public void Basic03() {
        taintednessTest("securibench.micro.basic.Basic3", "doGet", false) ;
    }

    @Test
    public void Basic04() {
        taintednessTest("securibench.micro.basic.Basic4", "doGet", false) ;
    }

    @Test
    public void Basic05() {
        taintednessTest("securibench.micro.basic.Basic5", "doGet", false) ;
    }

    @Test
    public void Basic06() {
        taintednessTest("securibench.micro.basic.Basic6", "doGet", false) ;
    }

    @Test
    public void Basic07() {
        taintednessTest("securibench.micro.basic.Basic7", "doGet", false) ;
    }

    @Test
    public void Basic08() {
        taintednessTest("securibench.micro.basic.Basic8", "doGet", false) ;
    }

    @Test
    public void Basic09() {
        taintednessTest("securibench.micro.basic.Basic9", "doGet", false) ;
    }

    @Test
    public void Basic10() {
        taintednessTest("securibench.micro.basic.Basic10", "doGet", false) ;
    }

    @Test
    public void Basic11() {
        taintednessTest("securibench.micro.basic.Basic11", "doGet", false) ;
    }

    @Test
    public void Basic12() {
        taintednessTest("securibench.micro.basic.Basic12", "doGet", false) ;
    }

    @Test
    public void Basic13() {
        taintednessTest("securibench.micro.basic.Basic13", "doGet", false) ;
    }

    @Test
    public void Basic14() {
        taintednessTest("securibench.micro.basic.Basic14", "doGet", false) ;
    }

    @Test
    public void Basic15() {
        taintednessTest("securibench.micro.basic.Basic15", "doGet", false) ;
    }

    @Test
    public void Basic16() {
        taintednessTest("securibench.micro.basic.Basic16", "doGet", false) ;
    }

    @Test
    public void Basic17() {
        taintednessTest("securibench.micro.basic.Basic17", "doGet", false) ;
    }

    @Test
    public void Basic18() {
        taintednessTest("securibench.micro.basic.Basic18", "doGet", false) ;
    }

    @Test
    public void Basic19() {
        taintednessTest("securibench.micro.basic.Basic19", "doGet", false) ;
    }

    @Test
    public void Basic20() {
        taintednessTest("securibench.micro.basic.Basic20", "doGet", false) ;
    }

    @Test
    public void Basic21() {
        taintednessTest("securibench.micro.basic.Basic21", "doGet", false) ;
    }

    @Test
    public void Basic22() {
        taintednessTest("securibench.micro.basic.Basic22", "doGet", false) ;
    }

    @Test
    public void Basic23() {
        taintednessTest("securibench.micro.basic.Basic23", "doGet", false) ;
    }

    @Test
    public void Basic24() {
        taintednessTest("securibench.micro.basic.Basic24", "doGet", false) ;
    }

    @Test
    public void Basic25() {
        taintednessTest("securibench.micro.basic.Basic25", "doGet", false) ;
    }

    @Test
    public void Basic26() {
        taintednessTest("securibench.micro.basic.Basic26", "doGet", false) ;
    }

    @Test
    public void Basic27() {
        taintednessTest("securibench.micro.basic.Basic27", "doGet", false) ;
    }

    @Test
    public void Basic28() {
        taintednessTest("securibench.micro.basic.Basic28", "doGet", false) ;
    }

    @Test
    public void Basic29() {
        taintednessTest("securibench.micro.basic.Basic29", "doGet", false) ;
    }

    @Test
    public void Basic30() {
        taintednessTest("securibench.micro.basic.Basic30", "doGet", false) ;
    }

    @Test
    public void Basic31() {
        taintednessTest("securibench.micro.basic.Basic31", "doGet", false) ;
    }

    @Test
    public void Basic32() {
        taintednessTest("securibench.micro.basic.Basic32", "doGet", false) ;
    }

    @Test
    public void Basic33() {
        taintednessTest("securibench.micro.basic.Basic33", "doGet", false) ;
    }

    @Test
    public void Basic34() {
        taintednessTest("securibench.micro.basic.Basic34", "doGet", false) ;
    }

    @Test
    public void Basic35() {
        taintednessTest("securibench.micro.basic.Basic35", "doGet", false) ;
    }

    @Test
    public void Basic36() {
        taintednessTest("securibench.micro.basic.Basic36", "doGet", false) ;
    }

    @Test
    public void Basic37() {
        taintednessTest("securibench.micro.basic.Basic37", "doGet", false) ;
    }

    @Test
    public void Basic38() {
        taintednessTest("securibench.micro.basic.Basic38", "doGet", false) ;
    }

    @Test
    public void Basic39() {
        taintednessTest("securibench.micro.basic.Basic39", "doGet", false) ;
    }

    @Test
    public void Basic40() {
        taintednessTest("securibench.micro.basic.Basic40", "doGet", false) ;
    }

    @Test
    public void Basic41() {
        taintednessTest("securibench.micro.basic.Basic41", "doGet", false) ;
    }

    @Test
    public void Basic42() {
        taintednessTest("securibench.micro.basic.Basic42", "doGet", false) ;
    }

    /* ------------------------ End of testing basic ------------------------ */

    /* ------------------------ Testing collections ------------------------ */

    @Test
    public void Collections01() {
        taintednessTest("securibench.micro.collections.Collections1", "doGet", false) ;
    }

    @Test
    public void Collections02() {
        taintednessTest("securibench.micro.collections.Collections2", "doGet", false) ;
    }

    @Test
    public void Collections03() {
        taintednessTest("securibench.micro.collections.Collections3", "doGet", false) ;
    }

    @Test
    public void Collections04() {
        taintednessTest("securibench.micro.collections.Collections4", "doGet", false) ;
    }

    @Test
    public void Collections05() {
        taintednessTest("securibench.micro.collections.Collections5", "doGet", false) ;
    }

    @Test
    public void Collections06() {
        taintednessTest("securibench.micro.collections.Collections6", "doGet", false) ;
    }

    @Test
    public void Collections07() {
        taintednessTest("securibench.micro.collections.Collections7", "doGet", false) ;
    }

    @Test
    public void Collections08() {
        taintednessTest("securibench.micro.collections.Collections8", "doGet", false) ;
    }

    @Test
    public void Collections09() {
        taintednessTest("securibench.micro.collections.Collections9", "doGet", true) ;
    }

    @Test
    public void Collections10() {
        taintednessTest("securibench.micro.collections.Collections10", "doGet", false) ;
    }

    @Test
    public void Collections11() {
        taintednessTest("securibench.micro.collections.Collections11", "doGet", false) ;
    }

    @Test
    public void Collections12() {
        taintednessTest("securibench.micro.collections.Collections12", "doGet", false) ;
    }

    @Test
    public void Collections13() {
        taintednessTest("securibench.micro.collections.Collections13", "doGet", false) ;
    }

    @Test
    public void Collections14() {
        taintednessTest("securibench.micro.collections.Collections14", "doGet", false) ;
    }

    /* ------------------------ End of testing collections ------------------------ */

    /* ------------------------ Testing datastructures ------------------------ */

    @Test
    public void Datastructures01() {
        taintednessTest("securibench.micro.datastructures.Datastructures1", "doGet", false) ;
    }

    @Test
    public void Datastructures02() {
        taintednessTest("securibench.micro.datastructures.Datastructures2", "doGet", false) ;
    }

    @Test
    public void Datastructures03() {
        taintednessTest("securibench.micro.datastructures.Datastructures3", "doGet", false) ;
    }

    @Test
    public void Datastructures04() {
        taintednessTest("securibench.micro.datastructures.Datastructures4", "doGet", true) ;
    }

    @Test
    public void Datastructures05() {
        taintednessTest("securibench.micro.datastructures.Datastructures5", "doGet", false) ;
    }

    @Test
    public void Datastructures06() {
        taintednessTest("securibench.micro.datastructures.Datastructures6", "doGet", false) ;
    }

    /* ------------------------ End of testing datastructures ------------------------ */

    /* ------------------------ Testing factories ------------------------ */

    @Test
    public void Factories01() {
        taintednessTest("securibench.micro.factories.Factories1", "doGet", false) ;
    }

    @Test
    public void Factories02() {
        taintednessTest("securibench.micro.factories.Factories2", "doGet", false) ;
    }

    @Test
    public void Factories03() {
        taintednessTest("securibench.micro.factories.Factories3", "doGet", false) ;
    }

    /* ------------------------ End of testing factories ------------------------ */

    /* ------------------------ Testing inter ------------------------ */

    @Test
    public void Inter01() {
        taintednessTest("securibench.micro.inter.Inter1", "doGet", false) ;
    }

    @Test
    public void Inter02() {
        taintednessTest("securibench.micro.inter.Inter2", "doGet", false) ;
    }

    @Test
    public void Inter03() {
        taintednessTest("securibench.micro.inter.Inter3", "doGet", false) ;
    }

    @Test
    public void Inter04() {
        taintednessTest("securibench.micro.inter.Inter4", "doGet", false) ;
    }

    @Test
    public void Inter05() {
        taintednessTest("securibench.micro.inter.Inter5", "doGet", false) ;
    }

    /* Fail: static initialization (<clinit>) is not handled yet.*/
    @Test
    public void Inter06() {
        taintednessTest("securibench.micro.inter.Inter6", "doGet", false) ;
    }

    @Test
    public void Inter07() {
        taintednessTest("securibench.micro.inter.Inter7", "doGet", false) ;
    }

    @Test
    public void Inter08() {
        taintednessTest("securibench.micro.inter.Inter8", "doGet", false) ;
    }

    @Test
    public void Inter09() {
        taintednessTest("securibench.micro.inter.Inter9", "doGet", false) ;
    }

    @Test
    public void Inter10() {
        taintednessTest("securibench.micro.inter.Inter10", "doGet", false) ;
    }

    @Test
    public void Inter11() {
        taintednessTest("securibench.micro.inter.Inter11", "doGet", false) ;
    }

    /* Fail: static initialization (<clinit>) is not handled yet.*/
    @Test
    public void Inter12() {
        taintednessTest("securibench.micro.inter.Inter12", "doGet", false) ;
    }

    @Test
    public void Inter13() {
        taintednessTest("securibench.micro.inter.Inter13", "doGet", false) ;
    }

    @Test
    public void Inter14() {
        taintednessTest("securibench.micro.inter.Inter14", "doGet", false) ;
    }

    /* ------------------------ End of testing inter ------------------------ */

    /* ------------------------ Testing pred ------------------------ */

    @Test
    public void Pred01() {
        taintednessTest("securibench.micro.pred.Pred1", "doGet", true) ;
    }

    @Test
    public void Pred02() {
        taintednessTest("securibench.micro.pred.Pred2", "doGet", false) ;
    }

    // Fail: The analysis is not path-sensitive.
    @Test
    public void Pred03() {
        taintednessTest("securibench.micro.pred.Pred3", "doGet", true) ;
    }

    @Test
    public void Pred04() {
        taintednessTest("securibench.micro.pred.Pred4", "doGet", false) ;
    }

    @Test
    public void Pred05() {
        taintednessTest("securibench.micro.pred.Pred5", "doGet", false) ;
    }

    // Fail: The analysis is not path-sensitive.
    @Test
    public void Pred06() {
        taintednessTest("securibench.micro.pred.Pred6", "doGet", true) ;
    }

    // Fail: The analysis is not path-sensitive.
    @Test
    public void Pred07() {
        taintednessTest("securibench.micro.pred.Pred7", "doGet", true) ;
    }

    @Test
    public void Pred08() {
        taintednessTest("securibench.micro.pred.Pred8", "doGet", false) ;
    }

    @Test
    public void Pred09() {
        taintednessTest("securibench.micro.pred.Pred8", "doGet", false) ;
    }

    /* ------------------------ End of testing pred ------------------------ */

    /* ------------------------ Testing reflection ------------------------ */

    /* Fail: reflection is not supported yet. */
    @Test
    public void Reflection01() {
        taintednessTest("securibench.micro.reflection.Refl1", "doGet", false) ;
    }

    /* Fail: reflection is not supported yet. */
    @Test
    public void Reflection02() {
        taintednessTest("securibench.micro.reflection.Refl2", "doGet", false) ;
    }

    /* Fail: reflection is not supported yet. */
    @Test
    public void Reflection03() {
        taintednessTest("securibench.micro.reflection.Refl3", "doGet", false) ;
    }

    /* Fail: reflection is not supported yet. */
    @Test
    public void Reflection04() {
        taintednessTest("securibench.micro.reflection.Refl4", "doGet", false) ;
    }

    /* ------------------------ End of testing reflection ------------------------ */

    /* ------------------------ Testing sanitizers ------------------------ */

    @Test
    public void Sanitizers01() {
        taintednessTest("securibench.micro.sanitizers.Sanitizers1", "doGet", false) ;
    }

    @Test
    public void Sanitizers02() {
        taintednessTest("securibench.micro.sanitizers.Sanitizers2", "doGet", true) ;
    }

    @Test
    public void Sanitizers03() {
        taintednessTest("securibench.micro.sanitizers.Sanitizers3", "doGet", true) ;
    }

    @Test
    public void Sanitizers04() {
        taintednessTest("securibench.micro.sanitizers.Sanitizers4", "doGet", false) ;
    }

    @Test
    public void Sanitizers05() {
        taintednessTest("securibench.micro.sanitizers.Sanitizers5", "doGet", false) ;
    }

    @Test
    public void Sanitizers06() {
        taintednessTest("securibench.micro.sanitizers.Sanitizers6", "doGet", true) ;
    }

    /* ------------------------ End of testing sanitizers ------------------------ */

    /* ------------------------ Testing session ------------------------ */

    @Test
    public void Session01() {
        taintednessTest("securibench.micro.session.Session1", "doGet", false) ;
    }

    @Test
    public void Session02() {
        taintednessTest("securibench.micro.session.Session2", "doGet", false) ;
    }

    @Test
    public void Session03() {
        taintednessTest("securibench.micro.session.Session3", "doGet", false) ;
    }

    /* ------------------------ End of testing session ------------------------ */

    /* ------------------------ Testing strong_updates ------------------------ */

    @Test
    public void StrongUpdates01() {
        taintednessTest("securibench.micro.strong_updates.StrongUpdates1", "doGet", true) ;
    }

    @Test
    public void StrongUpdates02() {
        taintednessTest("securibench.micro.strong_updates.StrongUpdates2", "doGet", true) ;
    }

    @Test
    public void StrongUpdates03() {
        taintednessTest("securibench.micro.strong_updates.StrongUpdates3", "doGet", true) ;
    }

    @Test
    public void StrongUpdates04() {
        taintednessTest("securibench.micro.strong_updates.StrongUpdates4", "doGet", false) ;
    }

    /* Error: 'synchronized' is not implemented yet. */
    @Test
    public void StrongUpdates05() {
        taintednessTest("securibench.micro.strong_updates.StrongUpdates5", "doGet", false) ;
    }

    /* ------------------------ End of testing strong_updates ------------------------ */

    /* ======================== End of esting securibench micro ======================== */
}
