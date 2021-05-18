package guideforce.intraproc;

import guideforce.interproc.CallingContext;
import guideforce.interproc.MethodTable;
import guideforce.policy.AbstractDomain;
import guideforce.regions.AllocationSiteRegion;
import guideforce.regions.ExceptionRegion;
import guideforce.regions.Region;
import guideforce.regions.SpecialRegion;
import guideforce.types.Monad;
import guideforce.types.Triple;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.UnitGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class InfinitaryEffectAnalysis {
  private final AbstractDomain abstractDomain;
  private final MethodTable.Key currentKey;
  private final UnitGraph graph;
  private final FinitaryEffectAnalysis analysis;
  private final EquationSystem equations;

  public InfinitaryEffectAnalysis(AbstractDomain abstractDomain, MethodTable.Key currentKey,
                                  UnitGraph graph,
                                  FinitaryEffectAnalysis analysis) {
    this.abstractDomain = abstractDomain;
    this.currentKey = currentKey;
    this.graph = graph;
    this.analysis = analysis;

    this.equations = new EquationSystem();
    buildEquations();

    this.equations.solve();
  }

  public EffectTerm<Variable> getResult() {
    Unit entry = graph.getBody().getUnits().getFirst();
    Map<Local, Region> env = Collections.emptyMap();
    UnitVariable<Map<Local, Region>> var = new UnitVariable<>(entry, env);
    return equations.get(var).getRightHandSide();
  }

  private void buildEquations() {
    for (Unit d : graph) {
      analysis.getFlowBefore(d).get().stream()
              .map(Monad.Entry::getKey)
              .forEach(env -> buildEquation((Stmt) d, env));
    }
  }

  private void buildEquation(Stmt d, Environment env) {
    EffectTerm<Variable> out = new EffectTerm<>(abstractDomain);

    Triple<Monad<Environment>, Monad<Region>, Monad<Region>> triple = analysis.abstractedStmtTriple(d).apply(env);
    triple.getFirst().stream().forEach(entry -> {
      // put all terms from successor nodes
      for (Unit succ : graph.getSuccsOf(d)) {
        out.addMonomial(new UnitVariable<>(succ, entry.getKey()), entry.getEffect());
      }
      // put all terms from exception handler nodes
      List<Trap> traps = TrapManager.getTrapsAt(d,graph.getBody());
      FastHierarchy h = Scene.v().getOrMakeFastHierarchy();
      // Check if the exceptions of d may be caught
      for (Trap trap : traps) {
        // Get the exception class and variable of the handler
        SootClass caughtException = trap.getException();
        Unit u = trap.getHandlerUnit();
        assert (u instanceof IdentityStmt);
        Monad<Region> exRet = triple.getThird();
        Local l = (Local) ((IdentityStmt) u).getLeftOp();
        for (Map.Entry<Region, AbstractDomain.Finitary> en : exRet.getChoices().entrySet()) {
          Region r = en.getKey();
          boolean isCaught = false;
          if (r instanceof AllocationSiteRegion) { // the AllocationSiteRegion carries the class information of the exception
            SootClass thrownException = ((AllocationSiteRegion) r).getSootClass();
            if (h.isSubclass(thrownException, caughtException)) {
              isCaught = true;
            }
          } else if (r instanceof ExceptionRegion) { // the region of the exception is specified for e.g. an intrinsic method
            SootClass thrownException = ((ExceptionRegion) r).getSootClass();
            if (h.isSubclass(thrownException, caughtException)) {
              isCaught = true;
            }
          } else { // We don't know the exception class, and thus assume that it is caught by all traps.
            // TODO: Is this assumption ok?
            isCaught = true;
          }
          if (isCaught) { // The thrown exception of d may be caught
            Environment env1 = new Environment(entry.getKey());
            env1.put(l, r);
            for (Unit v : graph.getSuccsOf(u)) {
              out.addMonomial(new UnitVariable<>(v, env1), en.getValue());
            }
          }
        }
      }
    });

    // put all possible terms from method calls
    if (d.containsInvokeExpr()) {
      out.add(invokeTerm(d, env, d.getInvokeExpr()));
    }

    equations.put(new UnitVariable<>(d, env), out);
  }

  private EffectTerm<Variable> invokeTerm(Stmt stmt, Environment env, InvokeExpr e) {
    EffectTerm<Variable> p = new EffectTerm<>(abstractDomain);

    // region of method
    Region methodRegion = methodRegionFromInvokeExpr(env, e);

    // region of arguments
    List<Monad<Region>> argTypes = new ArrayList<>();
    for (Value arg : e.getArgs()) {
      argTypes.add(analysis.getTypeOfPureValue(env, arg, stmt));
    }
    Monad<List<Region>> argsType = Monad.sequence(abstractDomain, argTypes);

    // add variables for each invoked method
    CallingContext newCtx = currentKey.getCallingContext().push(graph.getBody().getMethod(), stmt);
    argsType.stream().forEach(entry -> {
      MethodTable.Key key =
              new MethodTable.Key(e.getMethodRef(), newCtx, methodRegion, entry.getKey());
      p.addMonomial(new MethodVariable(key), entry.getEffect());
    });

    return p;
  }

  private Region methodRegionFromInvokeExpr(Map<Local, Region> env, InvokeExpr invokeExpr) {
    if (invokeExpr instanceof StaticInvokeExpr) {
      return SpecialRegion.STATIC_REGION;
    } else {
      assert (invokeExpr instanceof InstanceInvokeExpr); // FIXME: dynamic invoke
      InstanceInvokeExpr ie = (InstanceInvokeExpr) invokeExpr;
      Local base = (Local) ie.getBase();
      return env.get(base);
    }
  }
}