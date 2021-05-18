package guideforce.regions;

import guideforce.policy.AbstractDomain;

import java.util.Objects;

public class MonoidRegion implements Region {
  private final AbstractDomain abstractDomain;
  private final int value;

  public MonoidRegion(AbstractDomain abstractDomain, int value) {
    this.abstractDomain = Objects.requireNonNull(abstractDomain);
    this.value = value;
  }

  public AbstractDomain.Finitary asFinitary() {
    return abstractDomain.makeFinitary(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonoidRegion that = (MonoidRegion) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return abstractDomain.monoidElementToString(value);
  }
}
