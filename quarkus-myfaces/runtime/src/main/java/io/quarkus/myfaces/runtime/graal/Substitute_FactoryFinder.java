package io.quarkus.myfaces.runtime.graal;

import java.util.List;
import java.util.Map;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.myfaces.runtime.MyFacesTemplate;

/**
 * Resolves myFaces factories in extension see
 * {@link io.quarkus.myfaces.deployment.MyFacesProcessor#registerForReflection(io.quarkus.deployment.annotations.BuildProducer, io.quarkus.deployment.builditem.CombinedIndexBuildItem)}
 * due to this issue: https://github.com/oracle/graal/issues/1457
 */
@TargetClass(className = "javax.faces.FactoryFinder")
public final class Substitute_FactoryFinder {

    @Substitute
    private static Map<String, List<String>> getFactoryClassNames(ClassLoader classLoader) {
        return MyFacesTemplate.FACES_FACTORIES;
    }
}
