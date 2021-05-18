package guideforce.interproc;

// TODO: check defaultTypeAndEffects

import guideforce.MockInfo;
import guideforce.policy.AbstractDomain;
import guideforce.policy.AbstractDomain.Infinitary;
import guideforce.policy.Intrinsic;
import guideforce.policy.Policy;
import guideforce.regions.AllocationSiteRegion;
import guideforce.regions.Region;
import guideforce.regions.SpecialRegion;
import guideforce.types.Monad;
import guideforce.types.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Encapsulates method and field tables for type checking.
 *
 * <p>
 * This class enforces well-formedness invariants for the tables with the following consequences:
 * <ul>
 * <li> If the method table contains an entry (C, m, r, z), then it also contains an entry
 * (D, m, r, z) for any relevant class D such that a call to C.m could invoke the code of D.m.
 * This could happen because of inheritance (C does not implement method m, but inherits it from D)
 * or because of virtual calls (D is a sub-class of C that overrides method m).
 * <li> Entries in the method table without a body contain a conservative approximation and need
 * not be analysed.
 * </ul>
 */
public final class ClassTable {
  private final Policy policy;
  private final MethodTable mTable;
  private final FieldTable fTable;
  private final ArrayTable arrayTable;
  private final TypePool typePool;
  private final CFGCache cfgCache;
  private final MockInfo typeMap;

  private final Logger logger = LoggerFactory.getLogger(ClassTable.class);

  ClassTable(Policy policy, int maxContextDepth, SootMethod entryPoint) {
    this.policy = policy;
    this.mTable = new MethodTable();
    this.fTable = new FieldTable();
    this.arrayTable = new ArrayTable();
    this.typeMap = new MockInfo();
    this.cfgCache = new CFGCache(this.typeMap);
    this.typePool = new TypePool(this.cfgCache, maxContextDepth, entryPoint.makeRef());
  }

  public ClassTable(ClassTable state) {
    this.policy = state.policy;
    this.mTable = new MethodTable(state.mTable);
    this.fTable = new FieldTable(state.fTable);
    this.arrayTable = new ArrayTable(state.arrayTable);
    this.typePool = state.typePool;
    this.cfgCache = state.cfgCache;
    this.typeMap = new MockInfo();
  }

  public TypePool getTypePool() {
    return typePool;
  }

  /**
   * Gives access to the method table, e.g. for iteration.
   * The returned table cannot be modified.
   */
  Map<MethodTable.Key, EffectType> getMethodTable() {
    return Collections.unmodifiableMap(mTable);
  }

  private enum MethodKind {
    APPLICATION_METHOD,
    INTRINSIC,
    EMPTY_DEFAULT_CONSTRUCTOR,
    MOCKED_LIBRARY_METHOD, // Library method for which we have a body (because there is a mock
    // class)
    OPAQUE_LIBRARY_METHOD
  }

  private MethodKind getKind(MethodTable.Key key) {

    // Intrinsic methods are specified by the policy
    if (policy.getIntrinsicMethod(key.getMethodRef()) != null) {
      return MethodKind.INTRINSIC;
    }

    // library methods are opaque
    boolean library = key.getMethodRef().getDeclaringClass().isLibraryClass();
    if (library) {

      // Default constructor
      SootClass objectClass = Scene.v().getSootClass("java.lang.Object");
      SootMethodRef objectConstructor = Scene.v().makeConstructorRef(objectClass,
              Collections.emptyList());
      if (key.getMethodRef().getSignature().equals(objectConstructor.getSignature())) {
        return MethodKind.EMPTY_DEFAULT_CONSTRUCTOR;
      }

      // Mocked methods
      SootMethodRef method = key.getMethodRef();
      SootMethodRef mockedMethod = typeMap.mockMethodRef(method);
      if (method != mockedMethod && cfgCache.getOrCreate(mockedMethod) != null) {
        return MethodKind.MOCKED_LIBRARY_METHOD;
      }

      // Anything else is treated conservatively
      return MethodKind.OPAQUE_LIBRARY_METHOD;
    }

    boolean hasBody = cfgCache.getOrCreate(key.getMethodRef()) != null;
    // default construct without body
    if (!hasBody && key.getMethodRef().getName().equals("<init>")) {
      return MethodKind.EMPTY_DEFAULT_CONSTRUCTOR;
    }
    return MethodKind.APPLICATION_METHOD;
//    throw new RuntimeException("Unexpected kind of method " + key.getMethodRef());
  }

  Body getBody(MethodTable.Key key) {
    MethodKind kind = getKind(key);
    if (kind == MethodKind.APPLICATION_METHOD || kind == MethodKind.MOCKED_LIBRARY_METHOD) {
      return cfgCache.getOrCreate(key.getMethodRef());
    } else {
      return null;
    }
  }

  /**
   * Returns an entry from the method table
   *
   * @param key entry key
   * @return Entry at {@code key} or {@code null} if none exists.
   */
  public EffectType get(MethodTable.Key key) {
    return mTable.get(key);
  }

  /**
   * Joins the entry at key {@code key} with the types-and-effect {@code te},
   * if the table has an entry for that key. It does nothing if no key is
   * present.
   *
   * <p>
   * To maintain the method table invariant, all existing entries of superclasses
   * are also joined with {@code te}.
   *
   * @param key entry key
   * @param te  types-and-effect to be joined
   */
  public void joinIfPresent(MethodTable.Key key, EffectType te) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(te);

    mTable.computeIfPresent(key, (k, v) -> v.join(te));
    // possible optimisation: if the join does not increase the entry, we can save the following
    // work

    // We need to also join any possible entries for superclasses to
    // maintain the subtyping invariant.
    SootMethodRef m = key.getMethodRef();
    SootClass c = m.getDeclaringClass();
    Consumer<SootClass> joinAt = mr -> {
      SootMethodRef newRef = Scene.v().makeMethodRef(mr, m.getName(), m.getParameterTypes(),
              m.getReturnType(), m.isStatic());
      MethodTable.Key newKey = key.withMethodRef(newRef);
      mTable.computeIfPresent(newKey, (k, v) -> v.join(te));
    };

    // update entries of all classes that inherit the method
    Hierarchy h = Scene.v().getActiveHierarchy();
    Deque<SootClass> queue = new LinkedList<>(c.isInterface() ? h.getDirectImplementersOf(c) :
            h.getDirectSubclassesOf(c));
    while (!queue.isEmpty()) {
      SootClass d = queue.pop();
      if (!typePool.contains(d.getType())) {
        continue;
      }
      if (!d.declaresMethod(m.getSubSignature())) {
        joinAt.accept(d);
        queue.addAll(d.isInterface() ? h.getDirectImplementersOf(d) : h.getDirectSubclassesOf(d));
      }
    }

    // update the entry of any superclass entry to ensure invariant
    while (!c.isInterface() && c.hasSuperclass()) { // && !c.isLibraryClass() && c.hasSuperclass()) {
      queue.addAll(c.getInterfaces());
      c = c.getSuperclass();
      joinAt.accept(c);
    }
    queue.addAll(c.getInterfaces());
    while (!queue.isEmpty()) {
      SootClass d = queue.pop();
      joinAt.accept(d);
      queue.addAll(d.getInterfaces());
    }
  }

  /**
   * Ensures that the method table has an entry for {@code key}.
   * If it does, then the method has no effect. Otherwise, the
   * default types (with the least possible effects) is inserted.
   * To maintain well-formedness, an entry for all <b>relevant</b> subclasses
   * are added.
   *
   * @param key entry key
   */
  public void ensurePresent(MethodTable.Key key) {
    Objects.requireNonNull(key);
    if (mTable.containsKey(key)) {
      return;
    }

    // We need to ensure that any possible implementation is included.
    if (getKind(key) != MethodKind.OPAQUE_LIBRARY_METHOD) {
      SootMethod method = key.getMethodRef().tryResolve();
      if (method != null) {
        key = key.withMethodRef(method.makeRef());
      } else {
        logger.error("cannot resolve method " + key.getMethodRef() + " all bets are off");
      }
    }

    mTable.put(key, defaultTypeAndEffects(key));

    // If the method is a constructor, then we do not need to close under subtyping
    if (key.getMethodRef().getName().equals("<init>")) {
      return;
    }

    // The default type is such that at a superclass a method can only have more effects (if
    // the superclass is a library class and we make a conservative assumption).

    // Add all subclasses of c to the method table to maintain invariant.
    // TODO: if c = Object then we visit the whole hierarchy!
    SootClass c = key.getMethodRef().getDeclaringClass();
    Hierarchy h = Scene.v().getActiveHierarchy();
    List<SootClass> all = new LinkedList<>();

    if (c.isInterface()) {
      all.addAll(h.getImplementersOf(c));
      all.addAll(h.getSubinterfacesOf(c));
    } else {
      all.addAll(h.getSubclassesOf(c));
    }

    for (SootClass subC : all) {
      RefType t = subC.getType();
      if (!typePool.contains(t)) {
        logger.trace("typePool does not contain " + t);
        continue;
      }
      SootMethodRef mRef = key.getMethodRef();
      SootMethodRef newRef = Scene.v().makeMethodRef(subC,
              mRef.getName(), mRef.getParameterTypes(), mRef.getReturnType(),
              mRef.isStatic());
      MethodTable.Key newKey = key.withMethodRef(newRef);
      mTable.putIfAbsent(newKey, defaultTypeAndEffects(newKey));
    }
  }

  /**
   * The default types-and-effect for the key. Of all the possible
   * entries for this key, the default entry is less-or-equal.
   */
  private EffectType defaultTypeAndEffects(MethodTable.Key key) {
    // methods that will be analysed can get the bottom annotation
    Supplier<EffectType> bottom = () -> {
      AbstractDomain abstractDomain = policy.getAbstractDomain();
      Infinitary infinitary = abstractDomain.zeroInfinitary();
      Monad<Region> refType = Monad.empty(abstractDomain);
      Monad<Region> exType = Monad.empty(abstractDomain);
      return new EffectType(refType, exType, infinitary);
    };
    // unknown methods get a conservative approximation
    Supplier<EffectType> unknown = () -> {
      AbstractDomain abstractDomain = policy.getAbstractDomain();
      Infinitary infinitary = abstractDomain.zeroInfinitary();
      Monad<Region> refType =
              (key.getMethodRef().getReturnType() instanceof RefType) ?
                      Monad.pure(abstractDomain, SpecialRegion.UNKNOWN_REGION) :
                      Monad.pure(abstractDomain, SpecialRegion.BASETYPE_REGION);
      Monad<Region> exType = Monad.empty(abstractDomain);
      return new EffectType(refType, exType, infinitary);
    };

    if (key.getRegion().impossible(key.getMethodRef())) {
      return bottom.get();
    }

    if (key.getRegion() == SpecialRegion.NULL_REGION) {
      return bottom.get();
    }

    switch (getKind(key)) {
      case INTRINSIC: {
        Intrinsic i = policy.getIntrinsicMethod(key.getMethodRef());
        Monad<Region> refType = i.getReturnType(key.getRegion(), key.getArgumentTypes());
        Monad<Region> exType = i.getExceptionalType(key.getRegion(), key.getArgumentTypes());
        Infinitary infinitary = policy.getAbstractDomain().zeroInfinitary();
        return new EffectType(refType, exType, infinitary);
      }
      case EMPTY_DEFAULT_CONSTRUCTOR: {
        // TODO: check bytecode semantics!
        AbstractDomain abstractDomain = policy.getAbstractDomain();
        Infinitary infinitary = abstractDomain.zeroInfinitary();
        // retType must be void;
        Monad<Region> refType = Monad.pure(abstractDomain, SpecialRegion.BASETYPE_REGION);
        Monad<Region> exType = Monad.empty(abstractDomain);
        return new EffectType(refType, exType, infinitary);
      }
      case OPAQUE_LIBRARY_METHOD:
//        if (!key.getMethodRef().getName().equals("<init>") &&
////                (key.getRegion() instanceof AllocationSiteRegion ||
//                        (key.getRegion() == SpecialRegion.ENTRYPOINT_REGION)) { // this line may be removed
//          // If we know where the object was created, then the implementation of the method is
//          // actually known and will
//          // be analysed. Constructors ar an exception.
//          return bottom.get();
//        } else {
          // Conservative assumption.
          return unknown.get();
//        }
      case MOCKED_LIBRARY_METHOD:
        // we have a method body
        return bottom.get();
      case APPLICATION_METHOD: {
        if (key.getMethodRef().isStatic() ||
                key.getRegion() instanceof AllocationSiteRegion || (key.getRegion() == SpecialRegion.ENTRYPOINT_REGION)) {
          // If we're sure that the class comes from the application, then it will be analysed.
          return bottom.get();
        } else {
          // The actual class may be an unknown class that may not be known in the application.
          // Need to assume that it's external code.
          // REVIEW: final classes may have more information
          return unknown.get();
        }
      }
    }
    throw new RuntimeException("non-exhaustive cases");
  }

  /**
   * Returns a field entry.
   */
  public Regions get(FieldTable.Key key) {
    Regions refinedType = fTable.get(key);

    if (refinedType == null) {
      // Special case: fields of objects in unknown region are assumed to be possibly
      // initialised to an unknown.
      if (key.getRegion() == SpecialRegion.UNKNOWN_REGION && key.getField().isDeclared()) {
        return Regions.singleton(SpecialRegion.UNKNOWN_REGION);
      } else if (key.getField().getType() instanceof RefType) {
        return Regions.singleton(SpecialRegion.NULL_REGION);
      } else {
        return Regions.singleton(SpecialRegion.BASETYPE_REGION);
      }
    }

    return refinedType;
  }

  /**
   * Inserts an entry in the table if there is no existing entry for the key, otherwise does
   * nothing.
   *
   * @param key entry key
   * @param typ types to be inserted
   */
  public void putIfAbsent(FieldTable.Key key, Regions typ) {
    fTable.putIfAbsent(key, typ);
  }

  /**
   * Updates the field table by joining the entry at key {@code key} with types {@code key}.
   * If there is no such entry, then the method has no effect.
   *
   * @param key entry key
   * @param typ types to be joined
   */
  public void joinIfPresent(FieldTable.Key key, Regions typ) {
    fTable.computeIfPresent(key, (k1, v) -> v.join(typ));
  }

  /**
   * Returns an array entry.
   */
  public Regions get(ArrayTable.Key key, ArrayType arrayType) {

    Regions refinedType = arrayTable.get(key);

    if (refinedType == null) {
      // Special case: fields of objects in unknown region are assumed to be possibly
      // initialised to an unknown.
      if (key.getRegion() == SpecialRegion.UNKNOWN_REGION) {
        return Regions.singleton(SpecialRegion.UNKNOWN_REGION);
      } else if (arrayType.getArrayElementType() instanceof RefType) {
        return Regions.singleton(SpecialRegion.NULL_REGION);
      } else {
        return Regions.singleton(SpecialRegion.BASETYPE_REGION);
      }
    }

    return refinedType;
  }

  /**
   * Inserts an entry in the table if there is no existing entry for the key, otherwise does
   * nothing.
   *
   * @param key entry key
   * @param typ types to be inserted
   */
  public void putIfAbsent(ArrayTable.Key key, Regions typ) {
    arrayTable.putIfAbsent(key, typ);
  }

  /**
   * Updates the field table by joining the entry at key {@code key} with types {@code key}.
   * If there is no such entry, then the method has no effect.
   *
   * @param key entry key
   * @param typ types to be joined
   */
  public void joinIfPresent(ArrayTable.Key key, Regions typ) {
    arrayTable.computeIfPresent(key, (k1, v) -> v.join(typ));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClassTable that = (ClassTable) o;
    return mTable.equals(that.mTable) &&
            fTable.equals(that.fTable) &&
            arrayTable.equals(that.arrayTable) &&
            typePool.equals(that.typePool);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mTable, fTable, arrayTable, typePool);
  }
}