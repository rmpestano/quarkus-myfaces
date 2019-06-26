package io.quarkus.myfaces.runtime.graal;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "javax.faces._FactoryFinderProviderFactory")
@Substitute
public final class Substitute_FactoryFinderProviderFactory {
    public static final String FACTORY_FINDER_PROVIDER_FACTORY_CLASS_NAME = "org.apache.myfaces.spi" +
            ".FactoryFinderProviderFactory";

    public static final String FACTORY_FINDER_PROVIDER_CLASS_NAME = "org.apache.myfaces.spi.FactoryFinderProvider";

    public static final String INJECTION_PROVIDER_FACTORY_CLASS_NAME = "org.apache.myfaces.spi.InjectionProviderFactory";

    public static final String INJECTION_PROVIDER_CLASS_NAME = "org.apache.myfaces.spi.InjectionProvider";

    public static final Class<?> FACTORY_FINDER_PROVIDER_FACTORY_CLASS;

    public static final Method FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD;

    public static final Method FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD;
    public static final Class<?> FACTORY_FINDER_PROVIDER_CLASS;
    public static final Method FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD;
    public static final Method FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD;
    public static final Method FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD;

    public static final Class<?> INJECTION_PROVIDER_FACTORY_CLASS;
    public static final Method INJECTION_PROVIDER_FACTORY_GET_INSTANCE_METHOD;
    public static final Method INJECTION_PROVIDER_FACTORY_GET_INJECTION_PROVIDER_METHOD;
    public static final Class<?> INJECTION_PROVIDER_CLASS;
    public static final Method INJECTION_PROVIDER_INJECT_METHOD;
    public static final Method INJECTION_PROVIDER_POST_CONSTRUCT_METHOD;
    public static final Method INJECTION_PROVIDER_PRE_DESTROY_METHOD;

    static {
        Class factoryFinderFactoryClass = null;
        Method factoryFinderproviderFactoryGetMethod = null;
        Method factoryFinderproviderFactoryGetFactoryFinderMethod = null;
        Class<?> factoryFinderProviderClass = null;

        Method factoryFinderProviderGetFactoryMethod = null;
        Method factoryFinderProviderSetFactoryMethod = null;
        Method factoryFinderProviderReleaseFactoriesMethod = null;

        Class injectionProviderFactoryClass = null;
        Method injectionProviderFactoryGetInstanceMethod = null;
        Method injectionProviderFactoryGetInjectionProviderMethod = null;
        Class injectionProviderClass = null;
        Method injectionProviderInjectMethod = null;
        Method injectionProviderPostConstructMethod = null;
        Method injectionProviderPreDestroyMethod = null;

        try {
            factoryFinderFactoryClass = classForName(FACTORY_FINDER_PROVIDER_FACTORY_CLASS_NAME);
            if (factoryFinderFactoryClass != null) {
                factoryFinderproviderFactoryGetMethod = factoryFinderFactoryClass.getMethod("getInstance", null);
                factoryFinderproviderFactoryGetFactoryFinderMethod = factoryFinderFactoryClass
                        .getMethod("getFactoryFinderProvider", null);
            }

            factoryFinderProviderClass = classForName(FACTORY_FINDER_PROVIDER_CLASS_NAME);
            if (factoryFinderProviderClass != null) {
                factoryFinderProviderGetFactoryMethod = factoryFinderProviderClass.getMethod("getFactory",
                        new Class[] { String.class });
                factoryFinderProviderSetFactoryMethod = factoryFinderProviderClass.getMethod("setFactory",
                        new Class[] { String.class, String.class });
                factoryFinderProviderReleaseFactoriesMethod = factoryFinderProviderClass.getMethod("releaseFactories", null);
            }

            injectionProviderFactoryClass = classForName(INJECTION_PROVIDER_FACTORY_CLASS_NAME);

            if (injectionProviderFactoryClass != null) {
                injectionProviderFactoryGetInstanceMethod = injectionProviderFactoryClass
                        .getMethod("getInjectionProviderFactory", null);
                injectionProviderFactoryGetInjectionProviderMethod = injectionProviderFactoryClass
                        .getMethod("getInjectionProvider", ExternalContext.class);
            }

            injectionProviderClass = classForName(INJECTION_PROVIDER_CLASS_NAME);

            if (injectionProviderClass != null) {
                injectionProviderInjectMethod = injectionProviderClass.getMethod("inject", Object.class);
                injectionProviderPostConstructMethod = injectionProviderClass.getMethod("postConstruct", Object.class,
                        Object.class);
                injectionProviderPreDestroyMethod = injectionProviderClass.getMethod("preDestroy", Object.class, Object.class);
            }
        } catch (Exception e) {
            // no op
        }

        FACTORY_FINDER_PROVIDER_FACTORY_CLASS = factoryFinderFactoryClass;
        FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD = factoryFinderproviderFactoryGetMethod;
        FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD = factoryFinderproviderFactoryGetFactoryFinderMethod;
        FACTORY_FINDER_PROVIDER_CLASS = factoryFinderProviderClass;

        FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD = factoryFinderProviderGetFactoryMethod;
        FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD = factoryFinderProviderSetFactoryMethod;
        FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD = factoryFinderProviderReleaseFactoriesMethod;

        INJECTION_PROVIDER_FACTORY_CLASS = injectionProviderFactoryClass;
        INJECTION_PROVIDER_FACTORY_GET_INSTANCE_METHOD = injectionProviderFactoryGetInstanceMethod;
        INJECTION_PROVIDER_FACTORY_GET_INJECTION_PROVIDER_METHOD = injectionProviderFactoryGetInjectionProviderMethod;
        INJECTION_PROVIDER_CLASS = injectionProviderClass;
        INJECTION_PROVIDER_INJECT_METHOD = injectionProviderInjectMethod;
        INJECTION_PROVIDER_POST_CONSTRUCT_METHOD = injectionProviderPostConstructMethod;
        INJECTION_PROVIDER_PRE_DESTROY_METHOD = injectionProviderPreDestroyMethod;
    }

    @Substitute
    public static Object getInstance() {
        if (FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD != null) {
            try {
                return FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD.invoke(FACTORY_FINDER_PROVIDER_FACTORY_CLASS, null);
            } catch (Exception e) {
                //No op
                Logger log = Logger.getLogger(Substitute_FactoryFinderProviderFactory.class.getName());
                if (log.isLoggable(Level.WARNING)) {
                    log.log(Level.WARNING, "Cannot retrieve current FactoryFinder instance from " +
                            "FactoryFinderProviderFactory." +
                            " Default strategy using thread context class loader will be used.", e);
                }
            }
        }
        return null;
    }

    // ~ Methods Copied from _ClassUtils
    // ------------------------------------------------------------------------------------

    /**
     * Tries a Class.loadClass with the context class loader of the current thread first and automatically falls back
     * to
     * the ClassUtils class loader (i.e. the loader of the myfaces.jar lib) if necessary.
     *
     * @param type fully qualified name of a non-primitive non-array class
     * @return the corresponding Class
     * @throws NullPointerException if type is null
     * @throws ClassNotFoundException
     */
    @Substitute
    public static Class<?> classForName(String type) throws ClassNotFoundException {
        if (type == null) {
            throw new NullPointerException("type");
        }
        try {
            // Try WebApp ClassLoader first
            return Class.forName(type, false, // do not initialize for faster startup
                    getContextClassLoader());
        } catch (ClassNotFoundException ignore) {
            // fallback: Try ClassLoader for ClassUtils (i.e. the myfaces.jar lib)
            return Class.forName(type, false, // do not initialize for faster startup
                    Thread.currentThread().getContextClassLoader());
        }
    }

    /**
     * Gets the ClassLoader associated with the current thread. Returns the class loader associated with the specified
     * default object if no context loader is associated with the current thread.
     *
     * @return ClassLoader
     */
    @Substitute
    protected static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() != null) {
            try {
                return (ClassLoader) AccessController.doPrivileged(
                        (PrivilegedExceptionAction) () -> Thread.currentThread().getContextClassLoader());
            } catch (PrivilegedActionException pae) {
                throw new FacesException(pae);
            }
        } else {
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
