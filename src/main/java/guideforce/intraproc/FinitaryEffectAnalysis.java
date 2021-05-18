package guideforce.intraproc;

import guideforce.interproc.*;
import guideforce.policy.AbstractDomain;
import guideforce.policy.AbstractDomain.Finitary;
import guideforce.policy.Intrinsic;
import guideforce.policy.Policy;
import guideforce.regions.AllocationSiteRegion;
import guideforce.regions.ExceptionRegion;
import guideforce.regions.Region;
import guideforce.regions.SpecialRegion;
import guideforce.types.Monad;
import guideforce.types.Regions;
import guideforce.types.Triple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FinitaryEffectAnalysis extends ForwardFlowAnalysis<Unit, FinitaryEffectFlow> {
  private final ClassTable tables;

  // what we analyze:
  private final Policy policy;
  private final MethodTable.Key currentKey;
  private final SootMethod currentMethod;

  private final Body body;

  private final Logger logger = LoggerFactory.getLogger(FinitaryEffectAnalysis.class);

  public FinitaryEffectAnalysis(Policy policy, ClassTable tables, MethodTable.Key currentKey,
                                UnitGraph g) {
    super(g);

    this.tables = tables;
    this.policy = policy;
    this.currentKey = currentKey;
    this.body = g.getBody();
    this.currentMethod = body.getMethod();

    doAnalysis();
  }

  public FinitaryEffectAnalysis(FinitaryEffectAnalysis fea) {
    super(fea.graph);
    this.tables = new ClassTable(fea.tables);
    this.policy = fea.policy;
    this.currentKey = fea.currentKey;
    this.body = fea.body;
    this.currentMethod = fea.currentMethod;
  }

  @Override
  protected void merge(FinitaryEffectFlow in1, FinitaryEffectFlow in2, FinitaryEffectFlow out) {
    FinitaryEffectFlow.merge(in1, in2, out);
  }

  @Override
  protected void copy(FinitaryEffectFlow from, FinitaryEffectFlow to) {
    FinitaryEffectFlow.copy(from, to);
  }

  @Override
  protected FinitaryEffectFlow newInitialFlow() {
    return FinitaryEffectFlow.empty(policy.getAbstractDomain());
  }

  @Override
  protected FinitaryEffectFlow entryInitialFlow() {
    return FinitaryEffectFlow.singleton(policy.getAbstractDomain());
  }

  /**
   * Computes the flow through the whole unit.
   */
  @Override
  protected Map<Unit,FinitaryEffectFlow> flowThrough(FinitaryEffectFlow in, Unit d) {
    logger.trace("analyzing unit: {}", d);
    logger.trace("in: {}", in);

    if (!(d instanceof Stmt))
      throw new RuntimeException("unhandled unit: " + d);

    Triple<Monad<Environment>, Monad<Region>, Monad<Region>> triple = in.get().tripleThen(abstractedStmtTriple((Stmt) d));
    FinitaryEffectFlow out = new FinitaryEffectFlow(policy.getAbstractDomain(), triple.getFirst());
    Monad<Region> ret = triple.getSecond();
    Monad<Region> exRet = triple.getThird();

//    // TODO: handle static initialization <clinit>
//    // Because static initializers can be executed at most once,
//    // we may extend FinitaryEffectFlow to track such information.
//    if (d instanceof AssignStmt) {
//      if(((AssignStmt) d).getRightOp() instanceof NewExpr) {
//        NewExpr e = (NewExpr) ((AssignStmt) d).getRightOp();
//        SootClass cl = e.getBaseType().getSootClass();
//        for (SootMethod clinit : EntryPoints.v().clinitsOf(cl)) {
//          // if clinit is not called yet, then analyze clinit and update out
//        }
//      }
//    }

    Map<Unit,FinitaryEffectFlow> outs = new HashMap<>();

    // out flow for each normal successor of d
    // TODO: if d would definitely throw an exception, how can we ignore the normal successors?
    for (Unit u : graph.getSuccsOf(d)) {
      logger.trace("out: {}\n", out);
      outs.put(u,out);
//      System.out.println("- out to " + u);
//      System.out.println("    " + out);
    }

    List<Trap> traps = TrapManager.getTrapsAt(d,body);

    FastHierarchy h = Scene.v().getOrMakeFastHierarchy();

    // Check if the exceptions of d may be caught
    for (Trap trap : traps) {
      // Get the exception class and variable of the handler
      SootClass caughtException = trap.getException();
      Unit u = trap.getHandlerUnit();
      assert (u instanceof IdentityStmt);
      Local l = (Local) ((IdentityStmt) u).getLeftOp();
      for (Region r : exRet.support()) {
        Finitary eff = exRet.get(r);
        boolean isCaught = false;
        boolean canRemove = false;
        if (r instanceof AllocationSiteRegion) { // the AllocationSiteRegion carries the class information of the exception
          SootClass thrownException = ((AllocationSiteRegion) r).getSootClass();
          if (h.isSubclass(thrownException, caughtException)) {
            isCaught = true;
            canRemove = true;
          }
        } else if (r instanceof ExceptionRegion) { // the region of the exception is specified for e.g. an intrinsic method
          SootClass thrownException = ((ExceptionRegion) r).getSootClass();
          if (h.isSubclass(thrownException, caughtException)) {
            isCaught = true;
            canRemove = true;
          }
        } else { // We don't know the exception class, and thus assume that it is caught by all traps.
          // TODO: Is this assumption ok?
          isCaught = true;
        }
        if (isCaught) { // The thrown exception of d may be caught
          // Extend the out flow with the exception variable
          Map<Environment, Finitary> choices = new HashMap<>();
          for (Environment env : in.get().support()) {
            Environment env1 = new Environment(env);
            env1.put(l, r);
            choices.put(env1, eff);
          }
          FinitaryEffectFlow exOut = new FinitaryEffectFlow(policy.getAbstractDomain(), new Monad<>(policy.getAbstractDomain(), choices));
          logger.trace("out: {}\n", exOut);
          for (Unit v : graph.getSuccsOf(u)) {
            outs.put(v, exOut);
//            System.out.println("- out to " + v);
//            System.out.println("    " + exOut);
          }
          if (canRemove) { // The exception is caught and thus its effect can be removed from exRet.
            exRet = exRet.remove(r);
          }
        }
      }
    }

    tables.ensurePresent(currentKey);
    tables.joinIfPresent(currentKey,
            new EffectType(ret, exRet, policy.getAbstractDomain().zeroInfinitary()));

    return outs;
  }

  public Function<Environment, Monad<Environment>> abstractedStmt(Stmt stmt) {
    return env -> {
      FlowThroughStmtVisitor visitor = new FlowThroughStmtVisitor(env);
      stmt.apply(visitor);
      return visitor.getResult();
    };
  }

//  public Function<Environment, Monad<Region>> abstractedStmtReturn(Stmt stmt) {
//    return env -> {
//      var visitor = new FlowThroughStmtVisitor(env);
//      stmt.apply(visitor);
//      return visitor.getReturn();
//    };
//  }

  public Function<Environment, Triple<Monad<Environment>, Monad<Region>, Monad<Region>>> abstractedStmtTriple(Stmt stmt) {
    return env -> {
      FlowThroughStmtVisitor visitor = new FlowThroughStmtVisitor(env);
      stmt.apply(visitor);
      return new Triple<>(visitor.getResult(), visitor.getReturn(), visitor.getExceptionalReturn());
    };
  }

//  public Function<Environment, Monad<Region>> abstractedStmtExceptionalReturn(Stmt stmt) {
//    return env -> {
//      var visitor = new FlowThroughStmtVisitor(env);
//      stmt.apply(visitor);
//      return visitor.getExceptionalReturn();
//    };
//  }

  /**
   * Compute the types of an effect-free Jimple value.
   * All values except new and invoke expressions are effect-free.
   */
  Monad<Region> getTypeOfPureValue(Environment env, Value v, Stmt stmt) {
    TypeOfValue sw = new TypeOfValue(env, stmt);
    v.apply(sw);
    assert (sw.getType() != null);
    // FIXME: assertion that it's pure
    return sw.getType();
  }

  /**
   * Compute types and effect of a Jimple value.
   */
  private EffectType getTypeOfValue(Environment env, Value v, Stmt stmt) {
    TypeOfValue sw = new TypeOfValue(env, stmt);
    v.apply(sw);
    assert (sw.getType() != null);
    // FIXME: assert (sw.getExceptionalType() != null)
    Monad<Region> exType = sw.getExceptionalType() == null ? Monad.empty(policy.getAbstractDomain()) : sw.getExceptionalType();
    return new EffectType(sw.getType(), exType, policy.getAbstractDomain().zeroInfinitary());
  }

  private boolean comparable(SootClass c1, SootClass c2) {
    if (c1.getName().equals("java.lang.Object") || (c2.getName().equals("java.lang.Object")))
      return true;

    FastHierarchy h = Scene.v().getOrMakeFastHierarchy();
    if (c1.isInterface() && h.getAllImplementersOfInterface(c1).contains(c2)) {
      return true;
    }
    if (c2.isInterface()) {
      return h.getAllImplementersOfInterface(c2).contains(c1);
    } else {
      return h.isSubclass(c1, c2) || h.isSubclass(c2, c1);
    }
  }

  public String annotatedMethod() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(currentMethod.getDeclaration()).append(" {\n");
    for (Unit u : this.graph) {
      // put some types information in comments
      Consumer<Value> typeOfValueComment = v -> {
        if (v instanceof Local) {
          Local x = (Local) v;
          buffer.append("  // ")
                  .append(x)
                  .append(": ")
                  .append(this.getFlowAfter(u).thenGet(x))
                  .append("\n");
        }
      };
      buffer.append("  ").append(u).append("\n");
      if (u instanceof AssignStmt) {
        typeOfValueComment.accept(((AssignStmt) u).getLeftOp());
      } else if (u instanceof IdentityStmt) {
        typeOfValueComment.accept(((IdentityStmt) u).getLeftOp());
      }
    }
    buffer.append("  // Return type: ")
            .append(this.tables.get(currentKey).getType())
            .append("\n")
            .append("}\n");
    return buffer.toString();
  }

  /**
   * Visitor to compute the flow through all possible Jimple statements
   */
  private class FlowThroughStmtVisitor extends AbstractStmtSwitch {
    private final AbstractDomain domain;
    private final Environment env;
    private Monad<Environment> out;
    private Monad<Region> ret, exRet;

    FlowThroughStmtVisitor(Environment env) {
      this.domain = policy.getAbstractDomain();
      this.env = env;
      this.out = Monad.pure(domain, env);
      this.ret = Monad.empty(domain);
      this.exRet = Monad.empty(domain);
    }

    public Monad<Environment> getResult() {
      return out;
    }

    public Monad<Region> getReturn() {
      return ret;
    }

    public Monad<Region> getExceptionalReturn() {
      return exRet;
    }

    private void execPut(Local l, Monad<Region> rt) {
      out = out.then(env ->
              rt.then(r -> {
                Environment env1 = new Environment(env);
                env1.put(l, r);
                return Monad.pure(domain, env1);
              })
      );
    }

    // Join all the effects of 't', and map each 'env' of 'out' to it
    private void execForEffect(Monad<Region> t) {
      out = out.then(t.getAggregateFinitary());
//      out = out.then(env0 -> t.map(r -> env0));
    }

    private void execReturn(Region r) {
      ret = out.map(env -> r);
    }

    /**
     * Analysis of an invoke statement.
     * <p>
     * The relevant Jimple grammar production is:
     * <pre>
     * invokeStm ::= invoke invokeExpr
     * invokeExpr ::= specialinvoke local.m(imm_1,...,imm_n)
     *              | interfaceinvoke local.m(imm_1,...,imm_n)
     *              | virtualinvoke local.m(imm_1,...,imm_n)
     *              | staticinvoke m(imm_1,...,imm_n)
     * </pre>
     */
    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
      InvokeExpr e = stmt.getInvokeExpr();
      SootMethodRef m = e.getMethodRef();

      // Intrinsic constructors specify type and effect in the policy.
      // Note that the type of the 'this'-variable may change when
      // such a constructor is called.
      if (m.getName().equals("<init>")) {
        Intrinsic i = policy.getIntrinsicMethod(m);
        // TODO: Verify this!
        if (i != null) {
          List<Monad<Region>> argTypes = e.getArgs().stream()
                  .map(arg -> getTypeOfPureValue(env, arg, stmt))
                  .collect(Collectors.toList());
          Local valThis = (Local) ((InstanceInvokeExpr) e).getBase();
          Monad<Region> t =
                  Monad.sequence(domain, argTypes).
                          then(args -> {
                            CallingContext newCtx = currentKey.getCallingContext().push(currentMethod, stmt);
                            MethodTable.Key key = new MethodTable.Key(m, newCtx, env.get(valThis), args);
                            tables.ensurePresent(key);
                            return i.getReturnType(env.get(valThis), args);});
          execPut(valThis, t);
//          execPut(valThis, t.join(Monad.pure(domain, env.get(valThis))));
          return;
        }
      }

      EffectType effectType = getTypeOfValue(env, e, stmt);
//      System.out.println("unit: " + stmt);
//      System.out.println("old:  " + out);
      execForEffect(effectType.getType());
//      System.out.println("new:  " + out);
      exRet = effectType.getExceptionalType();
    }

    /**
     * Analysis of an assignment statement.
     * <p>
     * The relevant Jimple grammar productions are:
     * <pre>
     *   assignStmt ::= local = rvalue;
     *                | field = imm;
     *                | local.field = imm;
     *                | local[imm] = imm;
     *       rvalue ::= concreteRef
     *                | imm
     *                | expr
     *  concreteRef ::= field
     *                | local.field
     *                | field[imm]
     * </pre>
     */
    @Override
    public void caseAssignStmt(AssignStmt stmt) {
      Value lv = stmt.getLeftOp();
      Value rv = stmt.getRightOp();

      // All right-hand sides are rvalues.
      EffectType effectType = getTypeOfValue(env, rv, stmt);

      logger.trace("lv := rv => getTypeRValue(rv) = {}", effectType);

      // TODO: rvalue may throw exceptions
      Monad<Region> rvType = effectType.getType();
      if (lv instanceof Local) {
        // case local = rvalue
        execPut((Local) lv, rvType);
      } else if (lv instanceof InstanceFieldRef) {
        // case field = imm
        InstanceFieldRef f = (InstanceFieldRef) lv;
        Local obj = (Local) f.getBase();
        Region r = env.get(obj);
        FieldTable.Key key = new FieldTable.Key(r, f.getField());
        Regions regions = Regions.fromSet(rvType.support());
        tables.joinIfPresent(key, regions);
        tables.putIfAbsent(key, regions);
      } else if (lv instanceof StaticFieldRef) {
        // case local.field = imm
        StaticFieldRef f = (StaticFieldRef) lv;
        FieldTable.Key key = new FieldTable.Key(SpecialRegion.STATIC_REGION, f.getField());
        Regions regions = Regions.fromSet(rvType.support());
        tables.joinIfPresent(key, regions);
        tables.putIfAbsent(key, regions);
      } else if (lv instanceof ArrayRef) {
        // case local[imm] = imm
        Local base = (Local) ((ArrayRef) lv).getBase();
        Region r = env.get(base);
        ArrayTable.Key key = new ArrayTable.Key(r);
        Regions regions = Regions.fromSet(rvType.support());
        tables.joinIfPresent(key, regions);
        tables.putIfAbsent(key, regions);
      } else {
        assert false;
        throw new RuntimeException("internal");
      }
    }

    /**
     * Analysis of an identity statement.
     * <p>
     * The relevant Jimple grammar productions are:
     * <pre>
     * identityStmt ::= local := @this: types;
     *                | local := @parameter_n: types;
     *                | local := @exception;
     * </pre>
     */
    @Override
    public void caseIdentityStmt(IdentityStmt stm) {
      Value lv = stm.getLeftOp();
      Value rv = stm.getRightOp();

      Monad<Region> t = getTypeOfPureValue(env, rv, stm);
      execPut((Local) lv, t);
    }

    @Override
    public void caseRetStmt(RetStmt stmt) {
      assert false;
    }

    /**
     * Analysis of a return statement.
     * <p>
     * The relevant Jimple grammar production is:
     * <pre>
     * returnStm ::= return imm
     * </pre>
     */
    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
      Value v = stmt.getOp();
      if (v instanceof Local) {
        execReturn(env.get(v));
      } else {
        assert (v instanceof Constant);
        execReturn(SpecialRegion.BASETYPE_REGION);
      }
    }

    /**
     * Analysis of a return statement.
     * <p>
     * The relevant Jimple grammar production is:
     * <pre>
     * returnStm ::= return
     * </pre>
     */
    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
      execReturn(SpecialRegion.BASETYPE_REGION);
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {
      // nothing to be done
    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
      // nothing to be done
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {
      // nothing to be done
    }

    /**
     * Analysis of a throw statement.
     * <p>
     * The relevant Jimple grammar production is:
     * <pre>
     * throwStm ::= throw imm
     * </pre>
     */
    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
      Value v = stmt.getOp();
      if (v instanceof Local) {
        exRet = out.map(env -> env.get(v));
      } else {
        assert (v instanceof Constant);
        // TODO: throw a constant exception?
      }
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
      // nothing to be done
    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
      // nothing to be done
    }

    @Override
    public void defaultCase(Object obj) {
      throw new RuntimeException("unhandled case");
    }
  }

  /**
   * Visitor to compute types and effect of all possible Jimple values
   */
  class TypeOfValue extends AbstractJimpleValueSwitch {
    private final Environment env;
    private final Stmt stm;

    private Monad<Region> type;
    private Monad<Region> exceptionalType;

    TypeOfValue(Environment env, Stmt stm) {
      this.env = env;
      this.stm = stm;
      this.type = null;
      this.exceptionalType = null;
    }

    Monad<Region> getType() {
      return type;
    }

    Monad<Region> getExceptionalType() {
      return exceptionalType;
    }

    void setType(Monad<Region> type) {
      this.type = type;
    }

    void setExceptionalType(Monad<Region> exceptionalType) {
      this.exceptionalType = exceptionalType;
    }

    @Override
    public void caseArrayRef(ArrayRef v) {
      Local b = (Local) v.getBase();
      ArrayType type = (ArrayType) v.getBase().getType();
      Region r = env.get(b);
      Monad<Region> rt = Monad.cases(policy.getAbstractDomain(),
              tables.get(new ArrayTable.Key(r), type).toSet());
      setType(rt);
    }

    @Override
    public void caseCastExpr(CastExpr v) {

      if (!(v.getType() instanceof RefType)) {
        throw new RuntimeException("not yet handled");
      }

      RefType toType = (RefType) v.getCastType();
      SootClass toClass = toType.getSootClass();

      Monad<Region> fromRefinedType = getTypeOfPureValue(env, v.getOp(), stm);
      // If the cast is from a non-class types, it will fail and
      // we use empty set of regions
      if (!(v.getType() instanceof RefType)) {
        // cast will fail at runtime; use empty set of regions
        setType(Monad.empty(policy.getAbstractDomain()));
        return;
      }
      RefType fromType = (RefType) v.getType();
      SootClass fromClass = fromType.getSootClass();

      // If the cast is between incomparable classes, it cannot be performed and
      // we use empty set of regions
      if (!comparable(fromClass, toClass)
              & !fromClass.toString().equals(toClass.toString())) { // class is not comparable to its mockup class
        setType(Monad.empty(policy.getAbstractDomain()));
        return;
      }

      setType(fromRefinedType);
    }

    @Override
    public void caseInstanceOfExpr(InstanceOfExpr v) {
      // result is boolean
      setType(Monad.pure(policy.getAbstractDomain(), SpecialRegion.BASETYPE_REGION));
    }

    @Override
    public void caseNewArrayExpr(NewArrayExpr v) {
      Location loc = new Location(currentMethod, stm);
      Region r = policy.getRegion(currentKey.getCallingContext(), loc, v);
      setType(Monad.pure(policy.getAbstractDomain(), r));
    }

    @Override
    public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
      // not handled
      // assert false;
      //throw new RuntimeException("Multidimentional arrays are not handled yet.");
      Location loc = new Location(currentMethod, stm);
      Region r = policy.getRegion(currentKey.getCallingContext(), loc, v);
      setType(Monad.pure(policy.getAbstractDomain(), r));
    }

    @Override
    public void caseNewExpr(NewExpr v) {
      Location loc = new Location(currentMethod, stm);
      Region r = policy.getRegion(currentKey.getCallingContext(), loc, v);
      assert tables.getTypePool().contains(v.getBaseType());
      setType(Monad.pure(policy.getAbstractDomain(), r));
    }

    /**
     * Common case for all invoke expressions.
     */
    private void caseInvoke(Region calleeType, SootMethodRef m, List<Value> args) {

      List<Monad<Region>> types = new LinkedList<>();
      types.add(Monad.pure(policy.getAbstractDomain(), calleeType));
      for (Value v : args) {
        v.apply(this); // arguments must be imm, no effect
        types.add(getType());
      }

      Monad<List<Region>> mTypes = Monad.sequence(policy.getAbstractDomain(), types);

      Triple result = mTypes.tripleThen(entry -> {
        Region r = entry.get(0);
        List<Region> argTypes = entry.subList(1, entry.size());

        CallingContext newCtx = currentKey.getCallingContext().push(currentMethod, stm);
        MethodTable.Key key = new MethodTable.Key(m, newCtx, r, argTypes);

        tables.ensurePresent(key);

        EffectType effectType = tables.get(key);

        assert (effectType != null);
        return new Triple(effectType.getType(), effectType.getExceptionalType(), null);
      });

//      Monad<Region> result =
//              mTypes.then(entry -> {
//                Region r = entry.get(0);
//                List<Region> argTypes = entry.subList(1, entry.size());
//
//                CallingContext newCtx = currentKey.getCallingContext().push(currentMethod, stm);
//                MethodTable.Key key = new MethodTable.Key(m, newCtx, r, argTypes);
//                tables.ensurePresent(key);
//
//                EffectType effectType = tables.get(key);
//                assert (effectType != null);
//                return effectType.getType();
//              });
//
//      Monad<Region> exResult =
//              mTypes.then(entry -> {
//                Region r = entry.get(0);
//                List<Region> argTypes = entry.subList(1, entry.size());
//
//                CallingContext newCtx = currentKey.getCallingContext().push(currentMethod, stm);
//                MethodTable.Key key = new MethodTable.Key(m, newCtx, r, argTypes);
//                tables.ensurePresent(key);
//
//                EffectType effectType = tables.get(key);
//                assert (effectType != null);
//                return effectType.getExceptionalType();
//              });

      setType((Monad<Region>) result.getFirst());
      setExceptionalType((Monad<Region>) result.getSecond());
    }

    @Override
    public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
      Local local = (Local) v.getBase();
//      System.out.println(v);
//      System.out.println(local);
//      System.out.println(env.get(local));
      caseInvoke(env.get(local), v.getMethodRef(), v.getArgs());
    }

    @Override
    public void caseSpecialInvokeExpr(SpecialInvokeExpr v) {
      Local local = (Local) v.getBase();
      caseInvoke(env.get(local), v.getMethodRef(), v.getArgs());
    }

    @Override
    public void caseStaticInvokeExpr(StaticInvokeExpr v) {
      caseInvoke(SpecialRegion.STATIC_REGION, v.getMethodRef(), v.getArgs());
    }

    @Override
    public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
      Local local = (Local) v.getBase();
      caseInvoke(env.get(local), v.getMethodRef(), v.getArgs());
    }

    @Override
    public void caseDynamicInvokeExpr(DynamicInvokeExpr v) {
      throw new RuntimeException("not handled");
    }

    @Override
    public void caseLengthExpr(LengthExpr v) {
      // expressions of base types are safe to ignore
      setType(Monad.pure(policy.getAbstractDomain(), SpecialRegion.BASETYPE_REGION));
    }

    @Override
    public void caseNegExpr(NegExpr v) {
      // expressions of base types are safe to ignore
      setType(Monad.pure(policy.getAbstractDomain(), SpecialRegion.BASETYPE_REGION));
    }

    @Override
    public void caseInstanceFieldRef(InstanceFieldRef v) {
      Local obj = (Local) v.getBase();
      Region r = env.get(obj);
      SootField f = v.getField();
      Regions regions = tables.get(new FieldTable.Key(r, f));
      Monad<Region> ty = Monad.cases(policy.getAbstractDomain(), regions.toSet());
      setType(ty);
    }

    @Override
    public void caseLocal(Local v) {
      setType(Monad.pure(policy.getAbstractDomain(), env.get(v)));
    }

    @Override
    public void caseParameterRef(ParameterRef v) {
      setType(Monad.pure(policy.getAbstractDomain(),
              currentKey.getArgumentTypes().get((v.getIndex()))));
    }

    @Override
    public void caseCaughtExceptionRef(CaughtExceptionRef v) {
      if (env.containsKey(v)) {
        Region r = env.get(v);
        setType(Monad.pure(policy.getAbstractDomain(), r));
      } else {
        // TODO: double check
        setType(Monad.empty(policy.getAbstractDomain()));
      }
//      Region r = env.getOrDefault(v , SpecialRegion.EXCEPTION_REGION);
//      setType(Monad.pure(policy.getAbstractDomain(), r));
    }

    @Override
    public void caseThisRef(ThisRef v) {
      setType(Monad.pure(policy.getAbstractDomain(), currentKey.getRegion()));
    }

    @Override
    public void caseStaticFieldRef(StaticFieldRef v) {
      SootField f = (v).getField();
      Regions res = tables.get(new FieldTable.Key(SpecialRegion.STATIC_REGION, f));
      if (res != null){
        setType(Monad.cases(policy.getAbstractDomain(), res.toSet()));
      } else {
        setType(Monad.pure(policy.getAbstractDomain(), SpecialRegion.UNKNOWN_REGION));
      }
    }

    @Override
    public void caseStringConstant(StringConstant v) {
      Location location = new Location(currentMethod, stm);
      Region region = policy.getRegion(currentKey.getCallingContext(), location, v);
      setType(Monad.pure(policy.getAbstractDomain(), region));
    }

    @Override
    public void defaultCase(Object v) {
      // only cases for constants and binary operations are not covered
      if (v instanceof Constant || v instanceof BinopExpr) {
        setType(Monad.pure(policy.getAbstractDomain(), SpecialRegion.BASETYPE_REGION));
      } else {
        assert false;
      }
    }
  }

  public class UnitAndEffect {
    final Unit u;
    final Finitary eff;

    public UnitAndEffect(Unit u, Finitary eff) {
      this.u = u;
      this.eff = eff;
    }

    public Unit getUnit() {
      return u;
    }

    public Finitary getEffect() {
      return eff;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      UnitAndEffect that = (UnitAndEffect) o;
      return Objects.equals(u, that.u) && Objects.equals(eff, that.eff);
    }

    @Override
    public int hashCode() {
      return Objects.hash(u, eff);
    }
  }

  public boolean searchCounterExample (List<UnitAndEffect> path) {
    BriefUnitGraph g = new BriefUnitGraph(body);
    for (Unit u : g.getHeads()) {
      return search(entryInitialFlow(), u, path);
    }
    return false;
  }

  private boolean search (FinitaryEffectFlow in, Unit curUnit, List<UnitAndEffect> path) {

    Map<Unit, FinitaryEffectFlow> afterFlows = flowThrough(in, curUnit);

    Finitary eff = afterFlows.values().stream()
            .map(f -> f.get().getAggregateFinitary())
            .reduce(policy.getAbstractDomain().zeroFinitary(), Finitary::join);

    if (afterFlows.isEmpty()) {
      eff = in.get().getAggregateFinitary();
    }

//    System.out.println(curUnit);
//    System.out.println(eff);

    // return false when entering a unproductive loop
    if (path.contains(new UnitAndEffect(curUnit, eff))) {
      return false;
    }

    path.add(new UnitAndEffect(curUnit, eff));

    // return true if it's the end of the graph and the effect is not accepted
    if (afterFlows.isEmpty() && !eff.accepted()) {
      return true;
    }

    for (Map.Entry<Unit, FinitaryEffectFlow> entry : afterFlows.entrySet()) {
      Unit u = entry.getKey();
      FinitaryEffectFlow flow = entry.getValue();
      if (search(flow, u, path)) {
        return true;
      }
    }

    // No extension of the path is possible.
    // Remove the last node in the path.
    path.remove(path.size() - 1);

    return false;
  }


  public boolean searchCounterExampleInf (List<UnitAndEffect> path) {
    BriefUnitGraph g = new BriefUnitGraph(body);
    for (Unit u : g.getHeads()) {
      return searchInf(entryInitialFlow(), u, path);
    }
    return false;
  }

  private boolean searchInf (FinitaryEffectFlow in, Unit curUnit, List<UnitAndEffect> path) {

    Map<Unit, FinitaryEffectFlow> afterFlows = flowThrough(in, curUnit);

    Finitary eff = afterFlows.values().stream()
            .map(f -> f.get().getAggregateFinitary())
            .reduce(policy.getAbstractDomain().zeroFinitary(), Finitary::join);

    // end of the graph
    if (afterFlows.isEmpty()) {
      return false;
    }

    // when entering a loop,
    // return true if the loop generates a unacceptable trace,
    // return false if the loop generates an acceptable trace
    if (path.contains(new UnitAndEffect(curUnit, eff))) {
      return !eff.omega().accepted();
    }

    path.add(new UnitAndEffect(curUnit, eff));

    for (Map.Entry<Unit, FinitaryEffectFlow> entry : afterFlows.entrySet()) {
      Unit u = entry.getKey();
      FinitaryEffectFlow flow = entry.getValue();
      if (searchInf(flow, u, path)) {
        return true;
      }
    }

    // No extension of the path is possible.
    // Remove the last node in the path.
    path.remove(path.size() - 1);

    return false;
  }
}