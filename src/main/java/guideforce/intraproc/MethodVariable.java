package guideforce.intraproc;

import guideforce.interproc.MethodTable;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a variable for a method.
 */
@Immutable
public final class MethodVariable implements Variable {
  private final MethodTable.Key key;

  public MethodVariable(MethodTable.Key key) {
    this.key = key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MethodVariable that = (MethodVariable) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }

  @Override
  public String toString() {
    return "MethodVariable{" + key + '}';
  }
}
