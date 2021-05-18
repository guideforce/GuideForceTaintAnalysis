package guideforce.policy;

import guideforce.interproc.CallingContext;
import guideforce.interproc.Location;
import guideforce.policy.automata.Automaton;
import guideforce.policy.automata.AutomatonAbstractDomain;
import guideforce.regions.AllocationSiteRegion;
import guideforce.regions.Region;
import guideforce.regions.SpecialRegion;
import guideforce.types.Monad;
import soot.SootMethodRef;
import soot.VoidType;
import soot.jimple.AnyNewExpr;
import soot.jimple.StringConstant;

import javax.annotation.Nonnull;
import java.util.List;

public class AStarBStar implements Policy {
    private final AutomatonAbstractDomain abstractDomain;

    public AStarBStar() {
        Automaton a = new Automaton();

        a.addAlphabetSymbol(Token.A);
        a.addAlphabetSymbol(Token.B);

        a.addState("0");
        a.addState("1");

        a.addEdge("0","0",Token.A);
        a.addEdge("0","1",Token.B);
        a.addEdge("1","1",Token.B);

        a.setInitialState("0");
        a.addFinalState("0");
        a.addFinalState("1");

        this.abstractDomain = new AutomatonAbstractDomain(a);
    }

    @Override
    public AbstractDomain getAbstractDomain() {
        return this.abstractDomain;
    }

    @Override
    public Region getRegion(CallingContext ctx, Location location, StringConstant v) {
        return new AllocationSiteRegion(v, ctx, location);
    }

    @Override
    public Region getRegion(CallingContext ctx, Location location, AnyNewExpr v) {
        return new AllocationSiteRegion(v, ctx, location);
    }

    @Override
    public Intrinsic getIntrinsicMethod(SootMethodRef method) {
        switch (method.getSignature()) {
            case "<ourlib.nonapp.TaintAPI: void emitA()>":
                return emitIntrinsic(method, Token.A);
            case "<ourlib.nonapp.TaintAPI: void emitB()>":
                return emitIntrinsic(method, Token.B);
            default:
                return null;
        }
    }

    private Intrinsic emitIntrinsic(SootMethodRef method, Token token) {
        return new Intrinsic() {
            @Nonnull
            @Override
            public Monad<Region> getReturnType(Region region, List<Region> argumentTypes) {
                assert method.getReturnType().equals(VoidType.v());
                return Monad.pure(abstractDomain, (Region) SpecialRegion.BASETYPE_REGION)
                        .then(getEffect(region, argumentTypes));
            }

            @Nonnull
            @Override
            public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
                return Monad.empty(abstractDomain);
            }

            @Nonnull
            public AbstractDomain.Finitary getEffect(Region region,
                                                     List<Region> argumentTypes) {
                return abstractDomain.makeFinitary(abstractDomain.read(token));
            }
        };
    }

    public enum Token {
        A, B
    }
}
