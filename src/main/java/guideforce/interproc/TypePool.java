package guideforce.interproc;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import guideforce.MockInfo;

import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.function.Consumer;

/**
 * The types pool represents all the types that are <b>relevant</b> for the analysis of a method.
 * <p>
 * It has the following properties:
 * <ul>
 * <li> It is closed under taking super-classes. If it contains {@code C}, then it also
 * contains {@code D} for any {@code D} that is a super-class or implemented interface of
 * {@code C}. </li>
 * <li> For any statement of the form {@code i: new C} that is reachable from
 * the entry method with calling context z, the pool contains the atomic types {@code C_TSA
 * .getRegion(z, i)} </li>
 * </ul>
 * <p>
 * In the second point, reachability has the following inductively defined meaning:
 * <ul>
 * <li> Any statement in the body of the main method is reachable. </li>
 * <li> If an invoke statement {@code x.m(...)} is reachable, where C is the types of x, then
 * the body of {@code m} in C and any relevant subclass D of C is also reachable. </li>
 * </ul>
 * <p>
 */
@Immutable
public final class TypePool {

  private final int maxContextDepth;
  private final Set<RefType> pool;

  /**
   * Initialises types pool for the given method as entry method.
   */
  TypePool(CFGCache cfgCache, int maxContextDepth, SootMethodRef entryMethod) {
    this.maxContextDepth = maxContextDepth;
    this.pool = new HashSet<>();
    this.initialize(cfgCache, entryMethod);
//    System.out.println(cfgCache);
  }

  public boolean contains(RefType atomic) {
    return pool.contains(atomic);
  }

  private void initialize(CFGCache cfgCache, SootMethodRef topMethodRef) {
    int oldSize;
    do {
      oldSize = this.pool.size();
      fillPool(cfgCache, topMethodRef);
      includeMocks();
      upwardClosure();
    } while (oldSize != this.pool.size());

  }

  /**
   * Include all mock classes
   */
  private void includeMocks() {
    // TODO: check for interfaces etc.
    MockInfo typeMap = new MockInfo();
    for (RefType type : new HashSet<>(pool)) {
      SootClass c = type.getSootClass();
      String mockClass = typeMap.getMockClassName(c.getName());
      if (mockClass != null) {
        pool.add(Scene.v().getRefType(mockClass));
      }
    }
  }

  /**
   * Closes pool under super classes to establish invariant.
   */
  private void upwardClosure() {
    boolean change = true;
    while (change) {
      change = false;

      for (RefType type : new HashSet<>(pool)) {
        // Add all super classes
        SootClass c = type.getSootClass();
        if (!c.equals(Scene.v().getSootClass("java.lang.Object"))) {
          if (!c.isInterface()) {
            change |= pool.add(c.getSuperclass().getType());
          }
          for (SootClass implementedInterfaces : c.getInterfaces()) {
            change |= pool.add(implementedInterfaces.getType());
          }
        }

      }
    }
  }

  /**
   * Computes the flow through the whole unit.
   */
  private void fillPool(CFGCache cfgCache, SootMethodRef entryPoint) {

    // TODO: is it right to add the declaring class into the type pool?
    pool.add(entryPoint.getDeclaringClass().getType());

    // put all classes that implement the arguments
    for (Type typ : entryPoint.getParameterTypes()) {
      if (typ instanceof RefType) {
        RefType rt = (RefType) typ;
        SootClass c = rt.getSootClass();
        for (SootClass leafSubClass : getLeafClasses(c)) {
          pool.add(leafSubClass.getType());
        }
      }
    }

    // put all classes that can be instantiated in the method body
    Set<ActivationFrame> done = new HashSet<>();
    Deque<ActivationFrame> todo = new LinkedList<>();

    todo.add(new ActivationFrame(new CallingContext(maxContextDepth), entryPoint));
    while (!todo.isEmpty()) {
      ActivationFrame next = todo.removeFirst();
      SootMethodRef methodRef = next.methodRef;
      CallingContext ctx = next.callingContext;

      if (done.contains(next)) {
        continue;
      }
      done.add(next);

      Body body = cfgCache.getOrCreate(methodRef);
      if (body != null) { // (method has a body)
        SootMethod m = body.getMethod();
        ExceptionalUnitGraph unitGraph = new ExceptionalUnitGraph(body);
        for (Unit u : unitGraph) {
          assert (u instanceof Stmt);
          u.apply(new StmtVisitor(m, ctx, todo::addFirst));
        }
      }
    }

  }

  private Set<SootClass> getLeafClasses(SootClass c) {
    Hierarchy h = Scene.v().getActiveHierarchy();
    Set<SootClass> res = new HashSet<>();
    if (c.isInterface()) {
      for (SootClass sub : h.getDirectSubinterfacesOf(c)) {
        res.addAll(getLeafClasses(sub));
      }
      for (SootClass sub : h.getDirectImplementersOf(c)) {
        res.addAll(getLeafClasses(sub));
      }
    } else {
      List<SootClass> subclasses = h.getDirectSubclassesOf(c);
      if (subclasses.isEmpty()) {
        res.add(c);
      } else {
        subclasses.forEach(sub -> res.addAll(getLeafClasses(sub)));
      }
    }
    return res;
  }

  /**
   * Visitor to compute the flow through all possible Jimple statements
   */
  private class StmtVisitor extends AbstractStmtSwitch {
    final Consumer<ActivationFrame> addCall;
    private final SootMethod method;
    private final CallingContext ctx;

    StmtVisitor(SootMethod method, CallingContext ctx, Consumer<ActivationFrame> addCall) {
      this.method = method;
      this.ctx = ctx;
      this.addCall = addCall;
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
      InvokeExpr e = stmt.getInvokeExpr();
      e.apply(new ValueVisitor(method, ctx, stmt, addCall));
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
      Value rv = stmt.getRightOp();
      rv.apply(new ValueVisitor(method, ctx, stmt, addCall));
    }

    @Override
    public void defaultCase(Object obj) {
      // nothing to do
    }
  }

  /**
   * Visitor to compute types and effect of all possible Jimple values
   */
  class ValueVisitor extends AbstractJimpleValueSwitch {

    final Consumer<ActivationFrame> addCall;
    private final SootMethod method;
    private final CallingContext ctx;
    private final Stmt stm;


    ValueVisitor(SootMethod method, CallingContext ctx, Stmt stm, Consumer<ActivationFrame> call) {
      this.method = method;
      this.ctx = ctx;
      this.stm = stm;
      this.addCall = call;
    }

    /**
     * Common case for all invoke expressions.
     */
    private void caseInvoke(SootMethodRef m) {
      FastHierarchy h = Scene.v().getOrMakeFastHierarchy();
      CallingContext newCtx = ctx.push(method, stm);
      for (RefType t : pool) {
        SootClass c = t.getSootClass();
        if (h.canStoreClass(c, m.getDeclaringClass())) {
          SootMethodRef mr = Scene.v().makeMethodRef(c, m.getName(), m.getParameterTypes(),
                  m.getReturnType(), m.isStatic());
          this.addCall.accept(new ActivationFrame(newCtx, mr));
        }
      }
      this.addCall.accept(new ActivationFrame(newCtx, m));
    }

    @Override
    public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
      caseInvoke(v.getMethodRef());
    }

    @Override
    public void caseSpecialInvokeExpr(SpecialInvokeExpr v) { caseInvoke(v.getMethodRef());
    }

    @Override
    public void caseStaticInvokeExpr(StaticInvokeExpr v) {
      pool.add(v.getMethodRef().getDeclaringClass().getType());
      caseInvoke(v.getMethodRef());
    }

    @Override
    public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
      caseInvoke(v.getMethodRef());
    }

    @Override
    public void caseDynamicInvokeExpr(DynamicInvokeExpr v) {
      v.getMethodRef().getDeclaringClass().getType();
      throw new RuntimeException("not handled");
    }

    @Override
    public void caseNewExpr(NewExpr v) {
      pool.add(v.getBaseType());
    }

    @Override
    public void defaultCase(Object v) {
      // nothing to do
    }
  }

  private static class ActivationFrame {
    private final SootMethodRef methodRef;
    private final CallingContext callingContext;

    public ActivationFrame(CallingContext callingContext, SootMethodRef methodRef) {
      this.methodRef = methodRef;
      this.callingContext = callingContext;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ActivationFrame that = (ActivationFrame) o;
      return Objects.equals(methodRef, that.methodRef) &&
              Objects.equals(callingContext, that.callingContext);
    }

    @Override
    public int hashCode() {
      return Objects.hash(methodRef, callingContext);
    }
  }

}
