package guideforce.regions;

import soot.SootMethodRef;

import javax.annotation.concurrent.Immutable;

@Immutable
public enum SpecialRegion implements Region {
  // special regions for library methods not in the built-in table
  // (and not having a mock-up implementation)
  UNKNOWN_REGION,
  // Entry point of program, i.e. the main function being analyzed
  ENTRYPOINT_REGION,
  // Exception region
  EXCEPTION_REGION,
  // special regions used for static fields and methods
  STATIC_REGION,
  // dummy regions used for all values of base type
  BASETYPE_REGION,
  // dummy regions used for all values of base type
  NULL_REGION;

  @Override
  public String toString() {
    switch (this) {
      case UNKNOWN_REGION:
        return "unknown";
      case ENTRYPOINT_REGION:
        return "entry_point";
      case EXCEPTION_REGION:
        return "exception";
      case STATIC_REGION:
        return "static";
      case BASETYPE_REGION:
        return "base";
      case NULL_REGION:
        return "null";
    }
    assert false;
    return null;
  }


  @Override
  public boolean impossible(SootMethodRef m) {
    return this == NULL_REGION;
  }
}