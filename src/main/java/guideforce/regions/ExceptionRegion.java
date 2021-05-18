package guideforce.regions;

import soot.Scene;
import soot.SootClass;

import javax.annotation.Nullable;
import java.util.Objects;

public class ExceptionRegion implements Region {

    @Nullable
    private final SootClass sootClass;

    public ExceptionRegion() {
        this.sootClass = Scene.v().getSootClass("java.lang.Exception");
    }

    public ExceptionRegion(SootClass sootClass) {
        this.sootClass = sootClass;
    }

    @Nullable
    public SootClass getSootClass() {
        return sootClass;
    }

    @Override
    public String toString() {
        return sootClass.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExceptionRegion that = (ExceptionRegion) o;
        return Objects.equals(sootClass, that.sootClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sootClass);
    }
}
