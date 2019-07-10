package io.quarkus.myfaces.runtime.graal;

import java.util.logging.Logger;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.myfaces.runtime.MyFacesTemplate;

@TargetClass(className = "javax.faces.FactoryFinder")
public final class Subs_FactoryFinderProviderFactory {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Object factoryFinderProviderFactoryInstance = MyFacesTemplate.FACTORY_FINDER_PROVIDER_INSTANCE;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static boolean initialized = false;

    @Substitute
    private static void initializeFactoryFinderProviderFactory() {
        Logger log = Logger.getLogger(Subs_FactoryFinderProviderFactory.class.getName());
        log.info("graal initializeFactoryFinderProviderFactory");
        if (!initialized) {
            factoryFinderProviderFactoryInstance = MyFacesTemplate.FACTORY_FINDER_PROVIDER_INSTANCE;
            initialized = true;
            log.info("graal factoryFinderProviderFactoryInstance: " + factoryFinderProviderFactoryInstance);
        }
    }

    @Substitute
    public static Object getFactory(String factoryName) {
        Logger.getLogger(Subs_FactoryFinderProviderFactory.class.getName()).info("Graal getFactory " + factoryName);
        return MyFacesTemplate.FACTORY_FINDER_PROVIDER_INSTANCE.getFactoryFinderProvider().getFactory(factoryName);
    }

}
