package guideforce.interproc;

import guideforce.regions.Region;
import soot.SootMethodRef;

import javax.annotation.concurrent.Immutable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data structure for method table.
 *
 * <p>
 * This class is meant purely for data representation.
 * It does not enforce any well-formedness invariants.
 */
public final class MethodTable extends HashMap<MethodTable.Key, EffectType> {

  MethodTable() {
    super();
  }

  MethodTable(MethodTable other) {
    super(other);
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    for (Map.Entry<Key, EffectType> entry : entrySet()) {
      buffer.append("(").append(entry.getKey().getMethodRef())
              .append(",").append(entry.getKey().getCallingContext())
              .append(",").append(entry.getKey().getRegion())
              .append(",").append(entry.getKey().getArgumentTypes())
              .append(",").append(entry.getValue())
              .append(")\n");
    }
    return buffer.toString();

  }

  /**
   * Represents a tuple (m, z, C_r, args).
   * <p>
   * The class C is available from m.getDefiningClass().
   */
  @Immutable
  public static final class Key {
    // only the signature of {@code method} counts towards equality
    private final SootMethodRef method;
    private final CallingContext callingContext;
    private final Region region;
    private final List<Region> argumentTypes;
    private final String methodSignature; // cached, for faster hashing

    public Key(SootMethodRef method, CallingContext callingContext, Region region,
               List<Region> argumentTypes) {
      this.method = Objects.requireNonNull(method);
      this.callingContext = Objects.requireNonNull(callingContext);
      this.region = Objects.requireNonNull(region);
      this.argumentTypes = Objects.requireNonNull(argumentTypes);
      this.methodSignature = this.method.getSignature();
    }

    public SootMethodRef getMethodRef() {
      return method;
    }

    public CallingContext getCallingContext() {
      return callingContext;
    }

    public Region getRegion() {
      return region;
    }

    public List<Region> getArgumentTypes() {
      return argumentTypes;
    }

    public Key withMethodRef(SootMethodRef methodRef) {
      return new Key(methodRef, this.callingContext, this.region, this.argumentTypes);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Key key = (Key) o;
      return methodSignature.equals(key.methodSignature) &&
              callingContext.equals(key.callingContext) &&
              region.equals(key.region) &&
              argumentTypes.equals(key.argumentTypes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(methodSignature, method.getDeclaringClass(), callingContext, region,
              argumentTypes);
    }

    @Override
    public String toString() {
      return "Key{" + "method=" + method +
              ", callingContext=" + callingContext +
              ", region=" + region +
              ", argumentTypes=" + argumentTypes +
              '}';
    }
  }
}
