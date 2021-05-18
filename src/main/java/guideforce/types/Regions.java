package guideforce.types;

import guideforce.regions.Region;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Immutable
public final class Regions {
  @Nonnull
  private final Set<Region> regions;

  private Regions(Set<Region> regions) {
    this.regions = regions;
  }

  public static Regions singleton(Region r) {
    return new Regions(Collections.singleton(Objects.requireNonNull(r)));
  }

  public static Regions fromSet(Set<Region> rs) {
    return new Regions(new HashSet<>(Objects.requireNonNull(rs)));
  }

  public Set<Region> toSet() {
    return Collections.unmodifiableSet(regions);
  }

  @Override
  public String toString() {
    return "{" +
            regions.stream().map(Region::toString).collect(Collectors.joining(", ")) +
            "}";
  }

  public Regions join(Regions other) {
    Set<Region> result = new HashSet<>(this.regions);
    result.addAll(other.regions);
    return new Regions(result);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Regions that = (Regions) o;
    return regions.equals(that.regions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(regions);
  }
}
