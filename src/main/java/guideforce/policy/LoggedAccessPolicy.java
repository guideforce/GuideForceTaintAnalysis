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
import soot.jimple.AnyNewExpr;
import soot.jimple.StringConstant;

import javax.annotation.Nonnull;
import java.util.List;

public class LoggedAccessPolicy implements Policy {
    private final AutomatonAbstractDomain abstractDomain;

    public LoggedAccessPolicy() {
        Automaton a = new Automaton();

        a.addAlphabetSymbol(Token.auth);
        a.addAlphabetSymbol(Token.access);
        a.addAlphabetSymbol(Token.log);

        a.addState("s0");
        a.addState("s1");

        a.addEdge("s0", "s0", Token.auth);
        a.addEdge("s0", "s0", Token.log);
        a.addEdge("s0", "s1", Token.access);
        a.addEdge("s1", "s1", Token.auth);
        a.addEdge("s1", "s1", Token.access);
        a.addEdge("s1", "s0", Token.log);

        a.setInitialState("s0");
        a.addFinalState("s0");

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
            case "<testcases.paperexamples.Server: boolean verifyAuthorization()>":
                return emitIntrinsic(method, Token.auth);
            case "<testcases.paperexamples.Server: void readSensitiveData()>":
                return emitIntrinsic(method, Token.access);
            case "<testcases.paperexamples.Server: void logAccess()>":
                return emitIntrinsic(method, Token.log);
            default:
                return null;
        }
    }

    private Intrinsic emitIntrinsic(SootMethodRef method, Token token) {
        return new Intrinsic() {
            @Nonnull
            @Override
            public Monad<Region> getReturnType(Region region, List<Region> argumentTypes) {
                return Monad.pure(abstractDomain, (Region) SpecialRegion.BASETYPE_REGION)
                        .then(abstractDomain.makeFinitary(abstractDomain.read(token)));
            }

            @Nonnull
            @Override
            public Monad<Region> getExceptionalType(Region region, List<Region> argumentTypes) {
                return Monad.empty(abstractDomain);
            }
        };
    }

    public enum Token {
        auth, access, log
    }
}
