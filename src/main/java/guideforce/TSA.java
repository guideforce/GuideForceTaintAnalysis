package guideforce;

import com.google.common.base.Stopwatch;
import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.options.Options;
import soot.util.Chain;
import guideforce.interproc.InterProcAnalysis;
import guideforce.policy.Policy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class provides the entry point for analysing an application class.
 * It is responsible for setting up Soot, loading the required classes.
 * It provides a method for running the analysis on the application classes methods.
 * <p>
 * TODOs:
 * - The analysis currently ignores all static initializers.
 */
public final class TSA {

  /**
   * Packages that are considered as libraries. Their code will not be loaded.
   */
  private static final String[] library = {
          "jdk.*", "ourlib.nonapp.*", "com.oreilly.servlet.*", "javax.servlet.http.*"
  };

  /**
   * Upper bound on iterations for finitary analysis.
   */
  private static final int MAX_ITERATIONS = 40;

  /**
   * The analyzed application class.
   */
  private final SootClass mainApplicationClass;

  private final static Logger logger = LoggerFactory.getLogger(TSA.class);

  /**
   * Set up the analysis environment for the analysis of {@code mainApplicationClass}.
   *
   * @param sootClassPath            Soot classpath for the analysis
   * @param mainApplicationClassName Name of the application class that is to be analyzed
   * @param appClasses               Additional classes that should be loaded as application classes
   */
  TSA(String sootClassPath, String mainApplicationClassName, String... appClasses) {
    this.mainApplicationClass = setupSoot(sootClassPath, mainApplicationClassName, appClasses);
  }

  /**
   * Analyze a given method.
   *
   * @param policy                   Policy for analysis
   * @param kCFA                     Depth of calling contexts
   * @param methodNameOrSubSignature Name or subsignature of method to be analysed.
   * @return Object with analysis result or {@code null} if the analysis did not converge.
   */
  InterProcAnalysis run(Policy policy, int kCFA, String methodNameOrSubSignature) {
    // Resolve the method
    SootMethod method = getMethodByNameOrSubSignature(methodNameOrSubSignature);

    // Output Jimple representation of the application classes for debugging
    outputJimpleClasses(Scene.v().getApplicationClasses());

    logger.info("Application classes: " + Scene.v().getApplicationClasses() + "\n");
    logger.info("Library classes: " + Scene.v().getLibraryClasses() + "\n");

    // Perform analysis
    Stopwatch stopwatch = Stopwatch.createStarted();
    InterProcAnalysis analysis = new InterProcAnalysis(policy, kCFA, method);
    boolean success = analysis.doAnalysis(MAX_ITERATIONS);
    stopwatch.stop();

//    System.out.println("The analysis took " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms.");

    return success ? analysis : null;
  }

  private SootClass setupSoot(String sootClassPath, String entryPointClass, String... appClasses) {

    // We set up various soot options:
    Options.v().set_output_format(Options.output_format_jimple);
    Options.v().set_validate(true);
    Options.v().set_app(true);

    // Retain line numbers and use the original Java variable names in Jimple code
    Options.v().set_keep_line_number(true);
    Options.v().setPhaseOption("jb", "use-original-names:true");


    // We set the Soot class path.
    Options.v().set_soot_classpath(sootClassPath);
    Options.v().set_prepend_classpath(true);

    // Exclude library classes
    Options.v().set_exclude(Arrays.asList(library));

    logger.info("Working Directory = " + System.getProperty("user.dir"));
    logger.info("Java version: " + System.getProperty("java.version"));
    logger.info("Java classpath: " + System.getProperty("java.class.path"));
    logger.info("Soot classpath (options): " + Options.v().soot_classpath());
    logger.info("Soot classpath (scene): " + Scene.v().getSootClassPath());

    // Load the main class
    SootClass c = Scene.v().loadClassAndSupport(entryPointClass);
    c.setApplicationClass();

    // Load all mock classes
    addMockClasses();

    // Make the given classes application classes
    for (String cName : appClasses) {
      Scene.v().getSootClass(cName).setApplicationClass();
    }

    // Complete class loading
    Scene.v().loadNecessaryClasses();

    return c;
  }

  /**
   * Load all classes with package prefix "mockup." from the class path as application classes.
   */
  @SuppressWarnings("UnstableApiUsage")
  private static void addMockClasses() {

    // Construct a classLoader for the Soot classpath.
    List<URL> sootClassPath = new ArrayList<>();
    for (String path : Scene.v().getSootClassPath().split(File.pathSeparator)) {
      try {
        sootClassPath.add(new File(path).toURI().toURL());
      } catch (MalformedURLException e) {
        // do nothing
      }
    }
    // Construct classloader with null parent in order to load only soot classes
    ClassLoader classLoader = new URLClassLoader(sootClassPath.toArray(new URL[0]), null);

    // Make all classes in package "mockup" library classes.
    try {
      ClassPath classPath = ClassPath.from(classLoader);
      for (ClassPath.ClassInfo cl : classPath.getTopLevelClassesRecursive("mockup")) {
        String name = cl.getName();
        logger.trace("Loading class {} from mockup package.", name);
        Scene.v().loadClassAndSupport(name).setApplicationClass();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Ensure soot knows about all mocked classes
    MockInfo typeMap = new MockInfo();
    for (String mockedClass : typeMap.getMockedClasses()) {
      Scene.v().addBasicClass(mockedClass, SootClass.HIERARCHY);
    }
  }

  /**
   * Tries to resolve the given method by name first. If this does not succeed (e.g. because the
   * method is ambiguous), tries to use the argument as a method subsignature to resolve the method.
   * <p>
   * Throws an exception if the method cannot be found.
   *
   * @param nameOrSubSignature Method name or subsignature
   * @return Method
   */
  private SootMethod getMethodByNameOrSubSignature(String nameOrSubSignature) {
    try {
      // Generally, we write just the name, not the sub-signature.
      // However, when the method is ambiguous, we write the signature.
      return mainApplicationClass.getMethodByName(nameOrSubSignature);
    } catch (RuntimeException e) {
      return mainApplicationClass.getMethod(nameOrSubSignature);
    }
  }

  private static void outputJimpleClasses(Chain<SootClass> classes) {
    for (SootClass c : classes) {
      c.checkLevel(SootClass.BODIES);
      if ((c.isConcrete() || (c.isAbstract() && !c.isInterface()))) {
        outputJimpleClass(c);
      }
    }
  }

  private static void outputJimpleClass(SootClass sClass) {
    for (SootMethod m : sClass.getMethods()) {
      if (m.hasActiveBody()) {
        m.retrieveActiveBody();
      }
    }

    String filename = SourceLocator.v().getFileNameFor(sClass, Options.output_format_jimple);
    try (PrintWriter writer = new PrintWriter(filename)) {
      Printer.v().printTo(sClass, writer);
    } catch (IOException e) {
      logger.warn("Cannot write jimple class: " + e.getMessage());
    }
  }
}
