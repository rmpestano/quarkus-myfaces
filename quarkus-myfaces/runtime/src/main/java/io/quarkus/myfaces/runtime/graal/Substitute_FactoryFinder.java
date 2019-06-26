package io.quarkus.myfaces.runtime.graal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.FactoryFinder;
import javax.faces.application.ApplicationFactory;
import javax.faces.component.search.SearchExpressionContextFactory;
import javax.faces.component.visit.VisitContextFactory;
import javax.faces.context.*;
import javax.faces.flow.FlowHandlerFactory;
import javax.faces.lifecycle.ClientWindowFactory;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.render.RenderKitFactory;
import javax.faces.view.ViewDeclarationLanguageFactory;
import javax.faces.view.facelets.FaceletCacheFactory;
import javax.faces.view.facelets.TagHandlerDelegateFactory;

/**
 * A simplified FactoryFinder, it doesn't support multiple classloaders as the original one:
 * https://github.com/apache/myfaces/blob/cfa3c56b8e60d7db3e235b81131fcfa3e5d1326a/api/src/main/java/javax/faces/FactoryFinder.java#L79
 */
//@TargetClass(className = "javax.faces.FactoryFinder")
//@Substitute
public final class Substitute_FactoryFinder {

    public static final String APPLICATION_FACTORY = "javax.faces.application.ApplicationFactory";
    public static final String EXCEPTION_HANDLER_FACTORY = "javax.faces.context.ExceptionHandlerFactory";
    public static final String EXTERNAL_CONTEXT_FACTORY = "javax.faces.context.ExternalContextFactory";
    public static final String FACES_CONTEXT_FACTORY = "javax.faces.context.FacesContextFactory";
    public static final String LIFECYCLE_FACTORY = "javax.faces.lifecycle.LifecycleFactory";
    public static final String PARTIAL_VIEW_CONTEXT_FACTORY = "javax.faces.context.PartialViewContextFactory";
    public static final String RENDER_KIT_FACTORY = "javax.faces.render.RenderKitFactory";
    public static final String TAG_HANDLER_DELEGATE_FACTORY = "javax.faces.view.facelets.TagHandlerDelegateFactory";
    public static final String VIEW_DECLARATION_LANGUAGE_FACTORY = "javax.faces.view.ViewDeclarationLanguageFactory";
    public static final String VISIT_CONTEXT_FACTORY = "javax.faces.component.visit.VisitContextFactory";
    public static final String FACELET_CACHE_FACTORY = "javax.faces.view.facelets.FaceletCacheFactory";
    public static final String FLASH_FACTORY = "javax.faces.context.FlashFactory";
    public static final String FLOW_HANDLER_FACTORY = "javax.faces.flow.FlowHandlerFactory";
    public static final String CLIENT_WINDOW_FACTORY = "javax.faces.lifecycle.ClientWindowFactory";
    public static final String SEARCH_EXPRESSION_CONTEXT_FACTORY = "javax.faces.component.search.SearchExpressionContextFactory";

    /**
     * used as a monitor for itself and _factories. Maps in this map are used as monitors for themselves and the
     * corresponding maps in _factories.
     */
    private static Map<String, List<String>> registeredFactoryNames = new ConcurrentHashMap<>();

    /**
     * Maps from classLoader to another map, the container (i.e. Tomcat) will create a class loader for each web app
     * that it controls (typically anyway) and that class loader is used as the key.
     *
     * The secondary map maps the factory name (i.e. FactoryFinder.APPLICATION_FACTORY) to actual instances that are
     * created via getFactory. The instances will be of the class specified in the setFactory method for the factory
     * name, i.e. FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY, MyFactory.class).
     */
    private static Map<String, Object> factories = new ConcurrentHashMap<>();

    private static final Set<String> VALID_FACTORY_NAMES = new HashSet<String>();
    private static final Map<String, Class<?>> ABSTRACT_FACTORY_CLASSES = new HashMap<String, Class<?>>();
    private static final ClassLoader MYFACES_CLASSLOADER;

    private static final String INJECTION_PROVIDER_INSTANCE = "oam.spi.INJECTION_PROVIDER_KEY";
    private static final String INJECTED_BEAN_STORAGE_KEY = "org.apache.myfaces.spi.BEAN_ENTRY_STORAGE";
    private static final String BEAN_ENTRY_CLASS_NAME = "org.apache.myfaces.cdi.util.BeanEntry";

    private static final Logger LOGGER = Logger.getLogger(FactoryFinder.class.getName());

    static {
        VALID_FACTORY_NAMES.add(APPLICATION_FACTORY);
        VALID_FACTORY_NAMES.add(EXCEPTION_HANDLER_FACTORY);
        VALID_FACTORY_NAMES.add(EXTERNAL_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(FACES_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(LIFECYCLE_FACTORY);
        VALID_FACTORY_NAMES.add(PARTIAL_VIEW_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(RENDER_KIT_FACTORY);
        VALID_FACTORY_NAMES.add(TAG_HANDLER_DELEGATE_FACTORY);
        VALID_FACTORY_NAMES.add(VIEW_DECLARATION_LANGUAGE_FACTORY);
        VALID_FACTORY_NAMES.add(VISIT_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(FACELET_CACHE_FACTORY);
        VALID_FACTORY_NAMES.add(FLASH_FACTORY);
        VALID_FACTORY_NAMES.add(FLOW_HANDLER_FACTORY);
        VALID_FACTORY_NAMES.add(CLIENT_WINDOW_FACTORY);
        VALID_FACTORY_NAMES.add(SEARCH_EXPRESSION_CONTEXT_FACTORY);

        ABSTRACT_FACTORY_CLASSES.put(APPLICATION_FACTORY, ApplicationFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(EXCEPTION_HANDLER_FACTORY, ExceptionHandlerFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(EXTERNAL_CONTEXT_FACTORY, ExternalContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FACES_CONTEXT_FACTORY, FacesContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(LIFECYCLE_FACTORY, LifecycleFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(PARTIAL_VIEW_CONTEXT_FACTORY, PartialViewContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(RENDER_KIT_FACTORY, RenderKitFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(TAG_HANDLER_DELEGATE_FACTORY, TagHandlerDelegateFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(VIEW_DECLARATION_LANGUAGE_FACTORY, ViewDeclarationLanguageFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(VISIT_CONTEXT_FACTORY, VisitContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FACELET_CACHE_FACTORY, FaceletCacheFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FLASH_FACTORY, FlashFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FLOW_HANDLER_FACTORY, FlowHandlerFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(CLIENT_WINDOW_FACTORY, ClientWindowFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(SEARCH_EXPRESSION_CONTEXT_FACTORY, SearchExpressionContextFactory.class);
        try {
            ClassLoader classLoader;
            if (System.getSecurityManager() != null) {
                classLoader = (ClassLoader) AccessController.doPrivileged(
                        (PrivilegedExceptionAction) () -> FactoryFinder.class.getClassLoader());
            } else {
                classLoader = FactoryFinder.class.getClassLoader();
            }

            if (classLoader == null) {
                throw new FacesException("jsf api class loader cannot be identified", null);
            }
            MYFACES_CLASSLOADER = Thread.currentThread().getContextClassLoader();
        } catch (Exception e) {
            throw new FacesException("jsf api class loader cannot be identified", e);
        }
    }

    // ~ Start FactoryFinderProvider Support

    private static Object factoryFinderProviderFactoryInstance;

    private static volatile boolean initialized = false;

    private static void initializeFactoryFinderProviderFactory() {
        if (!initialized) {
            factoryFinderProviderFactoryInstance = Substitute_FactoryFinderProviderFactory.getInstance();
            initialized = true;
        }
    }

    // ~ End FactoryFinderProvider Support

    // avoid instantiation
    Substitute_FactoryFinder() {
    }

    /**
     * <p>
     * Create (if necessary) and return a per-web-application instance of the appropriate implementation class for the
     * specified JavaServer Faces factory class, based on the discovery algorithm described in the class description.
     * </p>
     *
     * <p>
     * The standard factories and wrappers in JSF all implement the interface {@link FacesWrapper}. If the returned
     * <code>Object</code> is an implementation of one of the standard factories, it must be legal to cast it to an
     * instance of <code>FacesWrapper</code> and call {@link FacesWrapper#getWrapped()} on the instance.
     * </p>
     *
     * @param factoryName
     *        Fully qualified name of the JavaServer Faces factory for which an implementation instance is requested
     *
     * @return A per-web-application instance of the appropriate implementation class for the specified JavaServer Faces
     *         factory class
     *
     * @throws FacesException
     *         if the web application class loader cannot be identified
     * @throws FacesException
     *         if an instance of the configured factory implementation class cannot be loaded
     * @throws FacesException
     *         if an instance of the configured factory implementation class cannot be instantiated
     * @throws IllegalArgumentException
     *         if <code>factoryname</code> does not identify a standard JavaServer Faces factory name
     * @throws IllegalStateException
     *         if there is no configured factory implementation class for the specified factory name
     * @throws NullPointerException
     *         if <code>factoryname</code> is null
     */
    //@Substitute
    public static Object getFactory(String factoryName) throws FacesException {
        if (factoryName == null) {
            throw new NullPointerException("factoryName may not be null");
        }

        initializeFactoryFinderProviderFactory();

        if (factoryFinderProviderFactoryInstance == null) {
            // Do the typical stuff
            return _getFactory(factoryName);
        } else {
            try {
                //Obtain the FactoryFinderProvider instance for this context.
                Object ffp = Substitute_FactoryFinderProviderFactory.FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD
                        .invoke(factoryFinderProviderFactoryInstance, null);

                //Call getFactory method and pass the params
                return Substitute_FactoryFinderProviderFactory.FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD.invoke(ffp,
                        factoryName);
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getCause();
                if (targetException instanceof NullPointerException) {
                    throw (NullPointerException) targetException;
                } else if (targetException instanceof FacesException) {
                    throw (FacesException) targetException;
                } else if (targetException instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) targetException;
                } else if (targetException instanceof IllegalStateException) {
                    throw (IllegalStateException) targetException;
                } else if (targetException == null) {
                    throw new FacesException(e);
                } else {
                    throw new FacesException(targetException);
                }
            } catch (Exception e) {
                //No Op
                throw new FacesException(e);
            }
        }
    }

    private static Object _getFactory(String factoryName) throws FacesException {
        ClassLoader classLoader = getClassLoader();

        // This code must be synchronized because this could cause a problem when
        // using update feature each time of myfaces (org.apache.myfaces.CONFIG_REFRESH_PERIOD)
        // In this moment, a concurrency problem could happen
        List<String> factoryClassNames = null;

        factoryClassNames = registeredFactoryNames.get(factoryName);

        if (factoryClassNames == null) {
            String message = "No Factories configured for this Application. This happens if the faces-initialization "
                    + "does not work at all - make sure that you properly include all configuration "
                    + "settings necessary for a basic faces application "
                    + "and that all the necessary libs are included. Also check the logging output of your "
                    + "web application and your container for any exceptions!"
                    + "\nIf you did that and find nothing, the mistake might be due to the fact "
                    + "that you use some special web-containers which "
                    + "do not support registering context-listeners via TLD files and "
                    + "a context listener is not setup in your web.xml.\n"
                    + "A typical config looks like this;\n<listener>\n"
                    + "  <listener-class>org.apache.myfaces.webapp.StartupServletContextListener</listener-class>\n"
                    + "</listener>\n";
            throw new IllegalStateException(message);
        }

        List beanEntryStorage;

        synchronized (factoryClassNames) {
            beanEntryStorage = (List) factories.computeIfAbsent(INJECTED_BEAN_STORAGE_KEY,
                    k -> new CopyOnWriteArrayList());
        }

        Object factory;
        Object injectionProvider;
        synchronized (factoryClassNames) {
            factory = factories.get(factoryName);
            if (factory != null) {
                return factory;
            }

            injectionProvider = factories.get(INJECTION_PROVIDER_INSTANCE);
        }

        if (injectionProvider == null) {
            injectionProvider = getInjectionProvider();
            synchronized (factoryClassNames) {
                factories.put(INJECTION_PROVIDER_INSTANCE, injectionProvider);
            }
        }

        // release lock while calling out
        factory = newFactoryInstance(ABSTRACT_FACTORY_CLASSES.get(factoryName),
                factoryClassNames.iterator(), classLoader, injectionProvider, beanEntryStorage);

        synchronized (factoryClassNames) {
            // check if someone else already installed the factory
            if (factories.get(factoryName) == null) {
                factories.put(factoryName, factory);
            }
        }

        return factory;
    }

    private static Object getInjectionProvider() {
        try {
            // Remember the first call in a webapp over FactoryFinder.getFactory(...) comes in the
            // initialization block, so there is a startup FacesContext active and
            // also a valid startup ExternalContext. Note after that, we need to cache
            // the injection provider for the classloader, because in a normal
            // request there is no active FacesContext in the moment and this call will
            // surely fail.
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                Object injectionProviderFactory = Substitute_FactoryFinderProviderFactory.INJECTION_PROVIDER_FACTORY_GET_INSTANCE_METHOD
                        .invoke(Substitute_FactoryFinderProviderFactory.INJECTION_PROVIDER_CLASS);
                Object injectionProvider = Substitute_FactoryFinderProviderFactory.INJECTION_PROVIDER_FACTORY_GET_INJECTION_PROVIDER_METHOD
                        .invoke(injectionProviderFactory, facesContext.getExternalContext());
                return injectionProvider;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static void injectAndPostConstruct(Object injectionProvider, Object instance, List injectedBeanStorage) {
        if (injectionProvider != null) {
            try {
                Object creationMetaData = Substitute_FactoryFinderProviderFactory.INJECTION_PROVIDER_INJECT_METHOD.invoke(
                        injectionProvider, instance);

                addBeanEntry(instance, creationMetaData, injectedBeanStorage);

                Substitute_FactoryFinderProviderFactory.INJECTION_PROVIDER_POST_CONSTRUCT_METHOD.invoke(
                        injectionProvider, instance, creationMetaData);
            } catch (Exception ex) {
                throw new FacesException(ex);
            }
        }
    }

    private static void preDestroy(Object injectionProvider, Object beanEntry) {
        if (injectionProvider != null) {
            try {
                Substitute_FactoryFinderProviderFactory.INJECTION_PROVIDER_PRE_DESTROY_METHOD.invoke(
                        injectionProvider, getInstance(beanEntry), getCreationMetaData(beanEntry));
            } catch (Exception ex) {
                throw new FacesException(ex);
            }
        }
    }

    private static Object getInstance(Object beanEntry) {
        try {
            Method getterMethod = getMethod(beanEntry, "getInstance");
            return getterMethod.invoke(beanEntry);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object getCreationMetaData(Object beanEntry) {
        try {
            Method getterMethod = getMethod(beanEntry, "getCreationMetaData");
            return getterMethod.invoke(beanEntry);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Method getMethod(Object beanEntry, String methodName) throws NoSuchMethodException {
        return beanEntry.getClass().getDeclaredMethod(methodName);
    }

    private static void addBeanEntry(Object instance, Object creationMetaData, List injectedBeanStorage) {
        try {
            Class<?> beanEntryClass = Substitute_FactoryFinderProviderFactory.classForName(BEAN_ENTRY_CLASS_NAME);
            Constructor beanEntryConstructor = beanEntryClass.getDeclaredConstructor(Object.class, Object.class);

            Object result = beanEntryConstructor.newInstance(instance, creationMetaData);
            injectedBeanStorage.add(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object newFactoryInstance(Class<?> interfaceClass, Iterator<String> classNamesIterator,
            ClassLoader classLoader, Object injectionProvider,
            List injectedBeanStorage) {
        try {
            Object current = null;

            while (classNamesIterator.hasNext()) {
                String implClassName = classNamesIterator.next();
                Class<?> implClass = null;
                try {
                    implClass = classLoader.loadClass(implClassName);
                } catch (ClassNotFoundException e) {
                    implClass = MYFACES_CLASSLOADER.loadClass(implClassName);
                }

                // check, if class is of expected interface type
                if (!interfaceClass.isAssignableFrom(implClass)) {
                    throw new IllegalArgumentException("Class " + implClassName + " is no " + interfaceClass.getName());
                }

                if (current == null) {
                    // nothing to decorate
                    current = implClass.newInstance();
                    injectAndPostConstruct(injectionProvider, current, injectedBeanStorage);
                } else {
                    // let's check if class supports the decorator pattern
                    try {
                        Constructor<?> delegationConstructor = implClass.getConstructor(new Class[] { interfaceClass });
                        // impl class supports decorator pattern,
                        try {
                            // create new decorator wrapping current
                            current = delegationConstructor.newInstance(new Object[] { current });
                            injectAndPostConstruct(injectionProvider, current, injectedBeanStorage);
                        } catch (InstantiationException e) {
                            throw new FacesException(e);
                        } catch (IllegalAccessException e) {
                            throw new FacesException(e);
                        } catch (InvocationTargetException e) {
                            throw new FacesException(e);
                        }
                    } catch (NoSuchMethodException e) {
                        // no decorator pattern support
                        current = implClass.newInstance();
                        injectAndPostConstruct(injectionProvider, current, injectedBeanStorage);
                    }
                }
            }

            return current;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new FacesException(e);
        }
    }

    //@Substitute
    public static void setFactory(String factoryName, String implName) {
        if (factoryName == null) {
            throw new NullPointerException("factoryName may not be null");
        }

        initializeFactoryFinderProviderFactory();

        if (factoryFinderProviderFactoryInstance == null) {
            // Do the typical stuff
            _setFactory(factoryName, implName);
        } else {
            try {
                //Obtain the FactoryFinderProvider instance for this context.
                Object ffp = Substitute_FactoryFinderProviderFactory.FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD
                        .invoke(factoryFinderProviderFactoryInstance, null);

                //Call getFactory method and pass the params
                Substitute_FactoryFinderProviderFactory.FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD.invoke(ffp, factoryName,
                        implName);
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getCause();
                if (targetException instanceof NullPointerException) {
                    throw (NullPointerException) targetException;
                } else if (targetException instanceof FacesException) {
                    throw (FacesException) targetException;
                } else if (targetException instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) targetException;
                } else if (targetException == null) {
                    throw new FacesException(e);
                } else {
                    throw new FacesException(targetException);
                }
            } catch (Exception e) {
                //No Op
                throw new FacesException(e);
            }

        }
    }

    private static void _setFactory(String factoryName, String implName) {
        checkFactoryName(factoryName);

        if (factories != null && factories.containsKey(factoryName)) {
            // Javadoc says ... This method has no effect if getFactory() has already been
            // called looking for a factory for this factoryName.
            return;
        }
        List<String> classNameList = registeredFactoryNames.computeIfAbsent(factoryName, k -> new ArrayList<>());
        classNameList.add(implName);
    }

    //@Substitute
    public static void releaseFactories() throws FacesException {
        initializeFactoryFinderProviderFactory();

        if (factoryFinderProviderFactoryInstance == null) {
            // Do the typical stuff
            _releaseFactories();
        } else {
            try {
                //Obtain the FactoryFinderProvider instance for this context.
                Object ffp = Substitute_FactoryFinderProviderFactory.FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD
                        .invoke(factoryFinderProviderFactoryInstance, null);

                //Call getFactory method and pass the params
                Substitute_FactoryFinderProviderFactory.FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD.invoke(ffp, null);
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getCause();
                if (targetException instanceof FacesException) {
                    throw (FacesException) targetException;
                } else if (targetException == null) {
                    throw new FacesException(e);
                } else {
                    throw new FacesException(targetException);
                }
            } catch (Exception e) {
                //No Op
                throw new FacesException(e);
            }

        }
    }

    private static void _releaseFactories() throws FacesException {
        registeredFactoryNames.clear();

        if (factories != null) {
            Object injectionProvider = factories.remove(INJECTION_PROVIDER_INSTANCE);
            if (injectionProvider != null) {
                List injectedBeanStorage = (List) factories.get(INJECTED_BEAN_STORAGE_KEY);

                FacesException firstException = null;
                for (Object entry : injectedBeanStorage) {
                    try {
                        preDestroy(injectionProvider, entry);
                    } catch (FacesException e) {
                        LOGGER.log(Level.SEVERE, "#preDestroy failed", e);

                        if (firstException == null) {
                            firstException = e; //all preDestroy callbacks need to get invoked
                        }
                    }
                }
                injectedBeanStorage.clear();

                if (firstException != null) {
                    throw firstException;
                }
            }
        }
    }

    private static void checkFactoryName(String factoryName) {
        if (!VALID_FACTORY_NAMES.contains(factoryName)) {
            throw new IllegalArgumentException("factoryName '" + factoryName + '\'');
        }
    }

    private static ClassLoader getClassLoader() {
        try {
            Logger log = Logger.getLogger(FactoryFinder.class.getName());
            ClassLoader classLoader = null;
            if (System.getSecurityManager() != null) {
                classLoader = (ClassLoader) AccessController.doPrivileged(
                        (PrivilegedExceptionAction) () -> Thread.currentThread().getContextClassLoader());
            } else {
                classLoader = Thread.currentThread().getContextClassLoader();
            }

            if (classLoader == null) {
                throw new FacesException("web application class loader cannot be identified", null);
            }
            return classLoader;
        } catch (Exception e) {
            throw new FacesException("web application class loader cannot be identified", e);
        }
    }
}
