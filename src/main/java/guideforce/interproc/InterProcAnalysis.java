package guideforce.interproc;

import guideforce.intraproc.*;
import guideforce.intraproc.EquationSystem.Equation;
import guideforce.policy.AbstractDomain;
import guideforce.policy.AbstractDomain.Finitary;
import guideforce.policy.Policy;
import guideforce.regions.InputRegion;
import guideforce.regions.Region;
import guideforce.regions.SpecialRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.cfgcmd.CFGGraphType;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class InterProcAnalysis {

  private static boolean SHOW_JIMPLE_CODE = false;
  private static boolean PAINT_UNITGRAPH = false;
  private static boolean COUNTEREXAMPLE_REPORT = true;

  private final Policy policy;
  private final ClassTable state;
  private final MethodTable.Key entryPointKey;

  private final Map<MethodTable.Key, FinitaryEffectAnalysis> finitaryResults = new HashMap<>();
  private final EquationSystem infinitaryResults = new EquationSystem();

  private final List<FinitaryEffectAnalysis.UnitAndEffect> problematicPath = new LinkedList<>();

  private final Logger logger = LoggerFactory.getLogger(InterProcAnalysis.class);

  public InterProcAnalysis(Policy policy, int maxContextDepth, SootMethod entryPoint) {
    this.policy = policy;
    this.state = new ClassTable(policy, maxContextDepth, entryPoint);

    Region entryRegion = entryPoint.isStatic() ? SpecialRegion.STATIC_REGION :
            SpecialRegion.ENTRYPOINT_REGION;

    List<Region> refinedArgTypes = new LinkedList<>();
    int n = 0;
    for (Type type : entryPoint.getParameterTypes()) {
      refinedArgTypes.add(new InputRegion(n));
      n++;
    }

    CallingContext topContext = new CallingContext(maxContextDepth);
    this.entryPointKey = new MethodTable.Key(entryPoint.makeRef(), topContext,
            entryRegion, refinedArgTypes);

    // expand the method table to contain at least the entry point
    state.ensurePresent(entryPointKey);
  }

  public EffectType getTypeAndEffectsAtEntryPoint() {
    EffectType te = state.get(entryPointKey);
    // te is the typing with the unsolved effect term
    // replace it with the solved term
    Variable x = new MethodVariable(entryPointKey);
    AbstractDomain.Infinitary solved =
            infinitaryResults.get(x).getRightHandSide().getConstantTerm();
    return new EffectType(te.getType(), te.getExceptionalType(), solved);
  }

  public boolean doAnalysis(int maximumIteration) {
    boolean converged = doFinitaryAnalysis(maximumIteration);
    if (!converged) {
      return false;
    }

    doInfinitaryAnalysis();

    // If the program may not adhere to the guideline, try to find a problematic path.
    if (COUNTEREXAMPLE_REPORT) {
      if (!state.get(entryPointKey).getType().getAggregateFinitary().accepted()) {
        FinitaryEffectAnalysis fea = new FinitaryEffectAnalysis(finitaryResults.get(entryPointKey));
        boolean res = fea.searchCounterExample(problematicPath);
        if (!res) {
          problematicPath.clear();
        }
      } else if (!infinitaryResults.get(new MethodVariable(entryPointKey)).getRightHandSide().getConstantTerm().accepted()) {
        FinitaryEffectAnalysis fea = new FinitaryEffectAnalysis(finitaryResults.get(entryPointKey));
        boolean res = fea.searchCounterExampleInf(problematicPath);
        if (!res) {
          problematicPath.clear();
        }
      } else {
        problematicPath.clear();
      }
    }

    logger.info("======== Analysis result: \n");
    logger.info(analysisResult());

    return true;
  }


  private boolean doFinitaryAnalysis(int maximumIteration) {
    // Main.mainLog.info("[InterProcAnalysis] initial mTable:\n" + mTable);
    int iteration = 0;

    while (true) {
      ClassTable oldState = new ClassTable(state);

      logger.trace("======== Iteration: " + iteration + "\n");
      logger.trace("At the beginning of iteration, old tables: \n" + oldState);

      // we go through each entry in the method table
      for (MethodTable.Key key : oldState.getMethodTable().keySet()) {
        Body body = state.getBody(key);
        if (body == null) { // that is, the method has no body
          // Methods without a body already have their effect correctly initialized in
          // the {@code ClassTable} when their entry is added to the method table.
          // The logic for this is in {@code ClassTable.getDefaultTypeAndEffect}.
          // We may consider moving it here.
          continue;
        }

        // Methods may be ruled out statically by the region
        if (key.getRegion().impossible(key.getMethodRef())) {
          continue;
        }

        logger.trace("==== Analyzing method entry: " + key + "\n");

        BriefUnitGraph unitGraph = new BriefUnitGraph(body);
        FinitaryEffectAnalysis intra = new FinitaryEffectAnalysis(policy, state, key, unitGraph);
        finitaryResults.put(key, intra);

        logger.trace("==== Analysis result:\n");
        logger.trace(intra.annotatedMethod());

      }

      if (state.equals(oldState)) {
        return true;
      }
      if (iteration++ >= maximumIteration) {
        return false;
      }
    }
  }

  private void doInfinitaryAnalysis() {

    for (MethodTable.Key key : state.getMethodTable().keySet()) {

      Body body = state.getBody(key);

      if (body == null) {
        continue;
      }

      // Methods may be ruled out statically by the region
      if (key.getRegion().impossible(key.getMethodRef())) {
        continue;
      }

      BriefUnitGraph unitGraph = new BriefUnitGraph(state.getBody(key));
      FinitaryEffectAnalysis intra = finitaryResults.get(key);
      InfinitaryEffectAnalysis ia =
              new InfinitaryEffectAnalysis(policy.getAbstractDomain(), key, unitGraph, intra);
      EffectType te = state.get(key);
      EffectType te1 = new EffectType(te.getType(), te.getExceptionalType(), ia.getResult());
      state.joinIfPresent(key, te1);
    }

    for (MethodTable.Key key : state.getMethodTable().keySet()) {
      Variable xr = new MethodVariable(key);
      infinitaryResults.put(new Equation(xr, state.get(key).getInfinitary()));
    }

    logger.trace("==== Infinitary equations:\n");
    logger.trace(infinitaryResults.toString());

    infinitaryResults.solve();

    logger.trace("==== Infinitary solution:\n");
    logger.trace(infinitaryResults.toString());
  }

  String drawUnitGraph(UnitGraph unitGraph) {
    CFGToDotGraph drawer = new CFGToDotGraph();
    DotGraph canvas = CFGGraphType.ALT_COMPLETE_UNIT_GRAPH.drawGraph(drawer, unitGraph,
            unitGraph.getBody());
    String filename = soot.SourceLocator.v().getOutputDir();
    if (filename.length() > 0) {
      filename = filename + java.io.File.separator;
    }
    String methodName =
            unitGraph.getBody().getMethod().getSubSignature().replace(java.io.File.separatorChar,
                    '.');
    String classname = unitGraph.getBody().getMethod().getDeclaringClass().getName().replaceAll(
            "\\$", "\\.");
    filename = filename + classname + " " + methodName + DotGraph.DOT_EXTENSION;
    canvas.plot(filename);
    return filename;
  }

  // TODO: print the analysis result in a better way
  public String analysisResult() {
    Body body = state.getBody(entryPointKey);
    FinitaryEffectAnalysis intra = finitaryResults.get(entryPointKey);

    if (body == null || intra == null) {
      return "Method has no body or has not been analysed yet.";
    }

    EffectType effectType = state.get(entryPointKey);
    BriefUnitGraph unitGraph = new BriefUnitGraph(state.getBody(entryPointKey));

    StringBuilder buffer = new StringBuilder();

    String className = entryPointKey.getMethodRef().getDeclaringClass().toString();
    buffer.append("\n" + dashedLine(className.length()));
    buffer.append(className).append("\n");
    buffer.append(dashedLine(className.length()));

    String fileName = "";
    if (entryPointKey.getMethodRef().getDeclaringClass().getTags().isEmpty()) {
      buffer.append("Cannot find the file.");
    } else {
      fileName = entryPointKey.getMethodRef().getDeclaringClass().getTags().get(0).toString();
    }
    Iterator<Unit> itfl = unitGraph.iterator();
    LineNumberTag fl = (LineNumberTag) itfl.next().getTag("LineNumberTag");
    while (itfl.hasNext()) {
      fl = (LineNumberTag) itfl.next().getTag("LineNumberTag");
      if (fl != null)
        break;
    }

    // Draw the unitGraph
    if (PAINT_UNITGRAPH) {
      String graphFile = drawUnitGraph(unitGraph);
      buffer.append("The dot file is in: ").append(graphFile).append("\n\n");
    }

    // Acceptable effects
    buffer.append("\n------------------\n");
    buffer.append("Acceptable effects\n");
    buffer.append("------------------\n");
    buffer.append(policy.getAbstractDomain().getAcceptedInfinitary() + "\n\n");

    // Result of the analysed method
    String methodSig = entryPointKey.getMethodRef().resolve().getDeclaration().toString();
    buffer.append(dashedLine(methodSig.length()+17));
    buffer.append("Analysed Method: ");
    buffer.append(methodSig).append("\n");
    buffer.append(dashedLine(methodSig.length()+17));
    if (fl != null) {
      int flnum = fl.getLineNumber() - 1;
      buffer.append("* location: ").append(".(" + fileName + ":" + flnum + ")");
      buffer.append("\n");
    }
    buffer.append("* class: ");
    buffer.append(entryPointKey.getMethodRef().getDeclaringClass());
    buffer.append(" in region ");
    buffer.append("'").append(entryPointKey.getRegion()).append("'");
    buffer.append("\n");
    buffer.append("* return type: ").append(effectType.getType());
    buffer.append("\n");
    buffer.append("* exceptional type: ").append(effectType.getExceptionalType());
    buffer.append("\n");
    buffer.append("* nonterminating effect: ")
              .append(infinitaryResults.get(new MethodVariable(entryPointKey)).getRightHandSide());
    buffer.append("\n");
    if (effectType.getAggregateFinitary().accepted() && effectType.getInfinitary().getConstantTerm().accepted()) {
      buffer.append("* This method adheres to the given guideline.\n");
    } else {
      buffer.append("* This mehtod may NOT follow the given guideline.\n");
    }


    // Types of all the related methods
    buffer.append("\n------------\n");
    buffer.append("Method Types\n");
    buffer.append("------------\n\n");
    for (Map.Entry<MethodTable.Key, EffectType> methodEntry :
            state.getMethodTable().entrySet()) {
      buffer.append("- method: ");
      buffer.append(methodEntry.getKey().getMethodRef().getSubSignature());
      buffer.append("\n  class: ");
      buffer.append(methodEntry.getKey().getMethodRef().getDeclaringClass());
      buffer.append(" in region ");
      buffer.append("'").append(methodEntry.getKey().getRegion()).append("'");
      buffer.append("\n  calling context: ");
      buffer.append(methodEntry.getKey().getCallingContext());
      buffer.append("\n  argument types: ");
      buffer.append(methodEntry.getKey().getArgumentTypes());
      buffer.append("\n  return type: ");
      buffer.append(methodEntry.getValue().getType());
      buffer.append("\n  exceptional type: ");
      buffer.append(methodEntry.getValue().getExceptionalType());
      buffer.append("\n  nonterminating effect: ");
      buffer.append(infinitaryResults.get(new MethodVariable(methodEntry.getKey())).getRightHandSide());
      buffer.append("\n");
      buffer.append("\n");

      // Print the typing contexts of the method if it has a body
      if (SHOW_JIMPLE_CODE) {
        Body b = state.getBody(methodEntry.getKey());
        if (b == null) continue;
        FinitaryEffectAnalysis intra2 = finitaryResults.get(methodEntry.getKey());
        if (intra2 == null) continue;
        String name = methodEntry.getKey().getMethodRef().resolve().getName();
        buffer.append("  Typing contexts in the body of " + name + "\n");
        buffer.append("  " + dashedLine(name.length() + 31));
        BriefUnitGraph ug = new BriefUnitGraph(b);
        for (Unit u : ug) {
          buffer.append("  ").append(u).append("\n");
          buffer.append("  // - context: ");
          FinitaryEffectFlow flowBefore = intra2.getFlowAfter(u);
          buffer.append(flowBefore.get().stream()
                  .map(entry -> entry.getKey() + " & " + entry.getEffect())
                  .collect(Collectors.joining("\n  //       | ")));
          buffer.append("\n");
        }
        buffer.append("\n");
      }
    }

    // The source code of the analyzed method
    String filePath = "src/test/java/" + className.replace('.','/') + ".java";
    List<String> sourceCode;
    try {
      sourceCode = Files.readAllLines(Paths.get(filePath));
    } catch (IOException e) {
      buffer.append("Cannot find the file " + filePath);
      return buffer.toString();
    }

    // Flows in the body of the analyzed method
    String methodName = entryPointKey.getMethodRef().resolve().getName();
    buffer.append(dashedLine(methodName.length()+31));
    buffer.append("Typing contexts in the body of ");
    buffer.append(methodName).append("\n");
    buffer.append(dashedLine(methodName.length()+31));

    Iterator<Unit> it = unitGraph.iterator();
    Unit unit = it.next();
    LineNumberTag lineNum = (LineNumberTag) unit.getTag("LineNumberTag");
    boolean first = true;
    FinitaryEffectFlow flowBefore = intra.getFlowAfter(unit);
    while (it.hasNext()) {
      unit = it.next();
      LineNumberTag newLineNum = (LineNumberTag) unit.getTag("LineNumberTag");
      // Get the flow that contains all the parameters
      if (first) {
        if (unit instanceof IdentityStmt) {
          if (((IdentityStmt) unit).getRightOp() instanceof ParameterRef) {
            flowBefore = intra.getFlowAfter(unit);
          }
        }
      }
      // skip nodes with the same line number
      if (lineNum != null && newLineNum !=null && lineNum.getLineNumber() != newLineNum.getLineNumber()) {
        if (first) {
          Integer firstLine = lineNum.getLineNumber() - 1;
          while (sourceCode.get(firstLine - 1).trim().startsWith("//") | sourceCode.get(firstLine - 1).trim().isEmpty()) {
            firstLine--;
          }
          buffer.append(".(" + fileName + ":" + firstLine +"): " + sourceCode.get(firstLine - 1).trim() + "\n");
          first = false;
        }
        buffer.append("  // - context: ");
        buffer.append(flowBefore.get().stream()
                .map(entry -> entry.getKey() + " & " + entry.getEffect())
                .collect(Collectors.joining("\n  //       | ")));buffer.append("\n");
        buffer.append(".(" + fileName + ":" + lineNum +"): " + sourceCode.get(lineNum.getLineNumber()-1).trim() + "\n");
        flowBefore = intra.getFlowBefore(unit);
      }
      lineNum = newLineNum;
    }
    buffer.append("  // - context: ");
    buffer.append(flowBefore.get().stream()
            .map(entry -> entry.getKey() + " & " + entry.getEffect())
            .collect(Collectors.joining("\n  //       | ")));
    buffer.append("\n");
    buffer.append(".(" + fileName + ":" + lineNum +"): " + sourceCode.get(lineNum.getLineNumber()-1).trim() + "\n");
    buffer.append("  // - type: ").append(effectType);
    buffer.append("\n");

    // Print the possibly problematic path if found
    if (!effectType.getType().getAggregateFinitary().accepted()) {
      if (!problematicPath.isEmpty()) {
        buffer.append("\n-----------------------------------\n");
        buffer.append("A finite, possibly problematic path\n");
        buffer.append("-----------------------------------\n");
        LineNumberTag l = (LineNumberTag) problematicPath.get(0).getUnit().getTag("LineNumberTag");
        Finitary effects = problematicPath.get(0).getEffect();
        for (int i = 1; i < problematicPath.size(); i++) {
          LineNumberTag newlineNum = (LineNumberTag) problematicPath.get(i).getUnit().getTag("LineNumberTag");
          // skip nodes with the same line number
          if (l != null && newlineNum !=null && l.getLineNumber() != newlineNum.getLineNumber()) {
            buffer.append(".(" + fileName + ":" + l +"): " + effects + "\n");
          }
          l = newlineNum;
          effects = problematicPath.get(i).getEffect();
        }
        buffer.append(".(" + fileName + ":" + l +"): " + effects + "\n");
      } else {
        buffer.append("\n--------------------------------\n");
        buffer.append("Fail to find a problematic path.\n");
        buffer.append("--------------------------------\n");
      }
    } else if (!infinitaryResults.get(new MethodVariable(entryPointKey)).getRightHandSide().getConstantTerm().accepted()) {
      if (!problematicPath.isEmpty()) {
        buffer.append("\n--------------------------------------\n");
        buffer.append("An infinite, possibly problematic path\n");
        buffer.append("--------------------------------------\n");
        LineNumberTag l = (LineNumberTag) problematicPath.get(0).getUnit().getTag("LineNumberTag");
        Finitary effects = problematicPath.get(0).getEffect();
        for (int i = 1; i < problematicPath.size(); i++) {
          LineNumberTag newlineNum = (LineNumberTag) problematicPath.get(i).getUnit().getTag("LineNumberTag");
          // skip nodes with the same line number
          if (l != null && newlineNum !=null && l.getLineNumber() != newlineNum.getLineNumber()) {
            buffer.append(".(" + fileName + ":" + l +"): " + effects + "\n");
          }
          l = newlineNum;
          effects = problematicPath.get(i).getEffect();
        }
        buffer.append(".(" + fileName + ":" + l +"): " + effects + "\n");
        buffer.append("loop...\n");
        buffer.append("The generated nonterminating effect is " + effects.omega() + "\n");
      } else {
        buffer.append("\n--------------------------------\n");
        buffer.append("Fail to find a problematic path.\n");
        buffer.append("--------------------------------\n");
      }
    }

    return buffer.toString();
  }

  private String dashedLine (int n) {
    return new String(new char[n]).replace("\0", "-") + "\n";
  }
}