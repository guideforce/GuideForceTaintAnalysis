package guideforce.regions;

import java.util.Objects;

/**
 * The region of the arguments to the entry point of the analysis.
 */
public class InputRegion implements Region {
  private final int id;

  public InputRegion(int id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InputRegion that = (InputRegion) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "input" + id;
  }
}
