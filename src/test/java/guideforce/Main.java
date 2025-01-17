package guideforce;

import guideforce.interproc.InterProcAnalysis;
import guideforce.policy.BinaryPolicy;
import guideforce.policy.Policy;
import soot.G;

import java.io.File;

public class Main {

    private static String classPath = "build/classes/java/test/" + File.pathSeparator +
            "build/classes/java/main/" + File.pathSeparator +
            "lib/cos.jar" + File.pathSeparator +
            "lib/j2ee.jar" + File.pathSeparator +
            "lib/java2html.jar";

    // Specify the entry-point method to run the analyzer
    private static String className = "package.class";
    private static String methodName = "method";
    // e.g.
    // private static String className = "testcases.CWE78_OS_Command_Injection.CWE78_OS_Command_Injection__connect_tcp_01";
    // private static String methodName = "good";
    private static Policy abcPolicy = new BinaryPolicy();

    public static void main(String[] args) {
        G.reset();
        TSA tsa = new TSA(classPath, className);
        InterProcAnalysis analysis = tsa.run(abcPolicy, 1, methodName);
        System.out.println(analysis.analysisResult());
    }
}
