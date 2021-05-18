package guideforce.policy;

import soot.SootMethodRef;
import soot.jimple.AnyNewExpr;
import soot.jimple.StringConstant;
import guideforce.interproc.CallingContext;
import guideforce.interproc.Location;
import guideforce.regions.Region;

public interface Policy {

  AbstractDomain getAbstractDomain();

  // Defines the region for string constants.
  Region getRegion(CallingContext ctx, Location location, StringConstant v);

  // Defines the region for the objects created in a "new" expression.
  // Corresponds to phi in the paper.
  Region getRegion(CallingContext ctx, Location location, AnyNewExpr v);

  /**
   * Determines if method {@code method} is an intrinsic method and returns its typing information.
   * <p>
   * Note: The finitary analysis is currently limited wrt the treatment of constructors.
   * An intrinsic constructor must not be used in the body of a constructor of a subclass.
   * To be safe, define intrinsic constructors only for classes where the other constructors
   * do not have bodies (e.g. because they are library methods or because they do not exist).
   * <p>
   * This is what may happen:
   * <pre>
   * public A() {
   *   super(); // If this is an intrinsic constructor, then the region of 'this'
   *            // will be determined by the intrinsic information. But this information
   *            // is not propagated to the caller of the whole constructor for A
   *   this.f = ...; // Affects the region determined by the intrinsic operation.
   * }
   * ...
   * A x = new A(); // Region chosen by new statement
   * x.f = ....;    // Affects the region determined by new statement.
   * </pre>
   * Both field accesses should pertain to the same region, but may not.
   *
   * @param method A method reference.
   * @return Information about return type and effects if the method is an intrinsic,
   * {@code null} if the method reference is not an intrinsic.
   */
  Intrinsic getIntrinsicMethod(SootMethodRef method);
}
