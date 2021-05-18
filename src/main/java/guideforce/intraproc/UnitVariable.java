package guideforce.intraproc;

import soot.Unit;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a variable for a soot unit, such as the statements in an analyzed method.
 */
@Immutable
public final class UnitVariable<A> implements Variable {
  private final Unit unit;
  private final A point;

  UnitVariable(Unit unit, A point) {
    this.unit = unit;
    this.point = point;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UnitVariable that = (UnitVariable) o;
    return Objects.equals(unit, that.unit) &&
            Objects.equals(point, that.point);
  }

  @Override
  public int hashCode() {
    return Objects.hash(unit, point);
  }

  @Override
  public String toString() {
    return "UnitVariable{" + "unit=" + unit + "(" + point + ") " + '}';
  }
}
