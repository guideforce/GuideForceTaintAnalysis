package guideforce.interproc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.NumberedString;
import guideforce.MockInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache to avoid repeated construction of control flow graphs.
 */
class CFGCache {
    private final Map<SootMethodRef, Body> bodies;
    private final MockInfo typeMap;

    private final Logger logger = LoggerFactory.getLogger(CFGCache.class);

    CFGCache(MockInfo typeMap) {
        bodies = new HashMap<>();
        this.typeMap = typeMap;
    }

    Body getOrCreate(SootMethodRef mRef0) {
        SootMethodRef mRef = typeMap.mockMethodRef(mRef0);
        if (mRef != mRef0) {
            logger.info("Using mocked method " + mRef + " instead of " + mRef0);
        }

        SootClass c = mRef.getDeclaringClass();
        Body body = bodies.get(mRef);
        if (body == null) {
            NumberedString sig = mRef.getSubSignature();
            if (!c.isInterface() && c.isApplicationClass() && c.declaresMethod(sig)) {
                SootMethod m = c.getMethod(sig);
                if (m.isConcrete()) {
                    body = m.retrieveActiveBody();
                    bodies.put(mRef, body);
                }
            }
        }
        return body;
    }

    @Override
    public String toString() {
        return "CFGCache{" +
                "bodies=" + bodies +
                ", typeMap=" + typeMap +
                '}';
    }
}
