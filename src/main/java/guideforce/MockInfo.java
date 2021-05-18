package guideforce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.tagkit.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Information about which classes have mock code.
 */
public class MockInfo {
    private final Map<String, String> mockClass;

    private final Logger logger = LoggerFactory.getLogger(MockInfo.class);

    public MockInfo() {
        this.mockClass = new HashMap<>();

        for (SootClass appClass : Scene.v().getApplicationClasses()) {
            List<String> mocks = replacesAnnotation(appClass);
            if (mocks != null) {
                for (String oldClass : mocks) {
                    logger.info("Using mock class {} for {}.", appClass.getName(), oldClass);
                    String previousMock = mockClass.put(oldClass, appClass.getName());
                    if (previousMock != null) {
                        System.err.println("Warning: Class " + oldClass + " has two mock classes " + previousMock
                                + " and " + appClass.getName() + ". " + " Using " + appClass.getName());
                    }
                }
            }
        }
    }

    private static List<String> replacesAnnotation(SootClass c) {
        VisibilityAnnotationTag tag = (VisibilityAnnotationTag) c.getTag("VisibilityAnnotationTag");
        if (tag == null) {
            return null;
        }

        for (AnnotationTag annotation : tag.getAnnotations()) {

            if (!annotation.getType().equals("Lmockup/Replaces;")) {
                continue;
            }
            for (AnnotationElem elem : annotation.getElems()) {
                if (!(elem instanceof AnnotationArrayElem) || !elem.getName().equals("value")) {
                    continue;
                }
                return ((AnnotationArrayElem) elem).getValues().stream().filter(AnnotationStringElem.class::isInstance)
                        .map(s -> ((AnnotationStringElem) s).getValue()).collect(Collectors.toList());
            }
        }
        return null;
    }

    /**
     * Returns the set of classes that have mockup code.
     */
    public Set<String> getMockedClasses() {
        return Collections.unmodifiableSet(mockClass.keySet());
    }

    /**
     * Returns the name of a mockup class that replaces the given class.
     *
     * @param className A class name
     * @return The name of the mockup class that replaces {@code className} in the
     *         analysis, {@code null} if none.
     */
    public String getMockClassName(String className) {
        return mockClass.get(className);
    }

    /**
     * Returns a reference to the method that mocks the given method, if it exists.
     * Otherwise returns the given method reference.
     *
     * @param m Method reference
     * @return Method reference of a mock method for {@code m}, if such a mock
     *         method exists, {@code m} otherwise.
     */
    public SootMethodRef mockMethodRef(SootMethodRef m) {
        String mockClass = getMockClassName(m.getDeclaringClass().getName());
        if (mockClass != null && Scene.v().getSootClass(mockClass).declaresMethod(m.getName(), m.getParameterTypes())) {
            SootMethod mockCandidate = Scene.v().getSootClass(mockClass).getMethod(m.getName(), m.getParameterTypes());
            // TODO: check that return types match
            return mockCandidate.makeRef();
        } else {
            return m;
        }
    }
}
