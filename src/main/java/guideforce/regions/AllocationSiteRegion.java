package guideforce.regions;

import soot.*;
import guideforce.interproc.CallingContext;
import guideforce.interproc.Location;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@Immutable
public final class AllocationSiteRegion implements Region {
  @Nullable
  private final SootClass sootClass;
  @Nonnull
  private final CallingContext callingContext;
  @Nonnull
  private final Location location;

  @Nullable
  public SootClass getSootClass() {
    return sootClass;
  }

  /**
   * Construct region for objects by allocation site.
   *
   * @param allocationSite The expression of the allocation site.
   * @param callingContext Calling context of the allocation site.
   * @param location       Location if allocation site in source file
   */
  public AllocationSiteRegion(Value allocationSite, CallingContext callingContext,
                              Location location) {
    if (allocationSite.getType() instanceof RefType) {
      this.sootClass = ((RefType) allocationSite.getType()).getSootClass();
    } else {
      // treat other expressions (e.g. new C[3]) conservatively
      this.sootClass = null;
    }
    this.callingContext = Objects.requireNonNull(callingContext);
    this.location = Objects.requireNonNull(location);
  }

  @Override
  public boolean impossible(SootMethodRef m) {
    // Actual class is not known.
    if (sootClass == null) {
      return false;
    }
    // Constructors of super-classes can be executed. Make a conservative assumption.
    if (m.getName().equals("<init>")) {
      return false;
    }
    // Static methods from other classes may be executed.
    if (m.isStatic()) {
      return false;
    }
    // Non-static method can also be executed if they are inherited, i.e. the class in
    // {@code sootClass} inherits the method in {@code m}.
    // The following code checks if the class in {@code sootClass} inherits the method in {@code m}
    // without overriding it.
    FastHierarchy h = Scene.v().getFastHierarchy();
    if (h.isSubclass(sootClass, m.getDeclaringClass())) {
      // Resolve the method reference at the subclass
      SootMethodRef mAtSootClassRef = Scene.v().makeMethodRef(sootClass, m.getName(),
              m.getParameterTypes(), m.getReturnType(), m.isStatic());
      SootMethod inheritedMethod = mAtSootClassRef.tryResolve();
      if (inheritedMethod != null) {
        // The given method reference can only be executed if it is the one that is inherited.
        return !inheritedMethod.getDeclaringClass().equals(m.getDeclaringClass());
      }
    }

    return true;
  }

  @Override
  public String toString() {
    String ctx = callingContext.toString();
    return "<created at " + location + (!ctx.trim().isEmpty() ? " " + ctx : "") + ">";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AllocationSiteRegion allocationSiteRegion = (AllocationSiteRegion) o;
    return location.equals(allocationSiteRegion.location) &&
            callingContext.equals(allocationSiteRegion.callingContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(callingContext, location);
  }
}
