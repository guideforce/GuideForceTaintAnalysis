package guideforce.interproc;

import guideforce.regions.Region;
import guideforce.types.Regions;

import javax.annotation.concurrent.Immutable;
import java.util.HashMap;
import java.util.Objects;

/**
 * Data structure for the array table.
 * <p>
 * This class is meant purely for data representation.
 * It does not enforce any well-formedness invariants.
 */
public final class ArrayTable extends HashMap<ArrayTable.Key, Regions> {

  ArrayTable() {
    super();
  }

  ArrayTable(ArrayTable other) {
    super(other);
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    for (Entry<Key, Regions> entry : entrySet()) {
      buffer.append(" ").append(entry.getKey().getRegion())
              .append(",").append(entry.getValue())
              .append("\n");
    }
    return buffer.toString();
  }

  @Immutable
  public static final class Key {
    private final Region region;

    public Key(Region region) {
      this.region = Objects.requireNonNull(region);
    }

    Region getRegion() {
      return region;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Key key = (Key) o;
      return region.equals(key.region);
    }

    @Override
    public int hashCode() {
      return Objects.hash(region);
    }

    @Override
    public String toString() {
      return "Key{" + "regions=" + region + '}';
    }
  }
}
