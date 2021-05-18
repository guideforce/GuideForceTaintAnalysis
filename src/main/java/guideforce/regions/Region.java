package guideforce.regions;

import soot.SootMethodRef;

import javax.annotation.concurrent.Immutable;

@Immutable
public interface Region {

  /**
   * Returns whether it's possible that the given method has a body that is executed on an object
   * in this region.
   * <p>
   * For example, a region may only contain objects of a certain particular class. In this case,
   * only methods from this class or inherited methods may be executed. All other methods are
   * impossible and can be ignored in the analysis.
   * <p>
   * The default implementation makes a conservative assumption.
   *
   * @param m A method reference.
   * @return {@code true} if method may never executed on any object in this region.
   */
  default boolean impossible(SootMethodRef m) {
    return false;
  }
}
