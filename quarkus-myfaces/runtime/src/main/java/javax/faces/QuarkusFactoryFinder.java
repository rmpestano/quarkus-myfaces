package javax.faces;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import org.apache.myfaces.spi.FactoryFinderProvider;

/**
 * A copy of MyFaces FactoryFinder without the multiple classloader because of issue:
 * https://github.com/oracle/graal/issues/1457
 *
 */
public class QuarkusFactoryFinder implements FactoryFinderProvider {

    private static final Logger LOGGER = Logger.getLogger(QuarkusFactoryFinder.class.getName());

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
    private static Map<String, List<String>> registeredFactoryNames = new HashMap<>();

    /**
     * Maps from classLoader to another map, the container (i.e. Tomcat) will create a class loader for each web app
     * that it controls (typically anyway) and that class loader is used as the key.
     *
     * The secondary map maps the factory name (i.e. FactoryFinder.APPLICATION_FACTORY) to actual instances that are
     * created via getFactory. The instances will be of the class specified in the setFactory method for the factory
     * name, i.e. FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY, MyFactory.class).
     */
    private static Map<String, Object> factories = new HashMap<>();

    private static final Set<String> VALID_FACTORY_NAMES = new HashSet<String>();
    private static final Map<String, Class<?>> ABSTRACT_FACTORY_CLASSES = new HashMap<String, Class<?>>();

    private static final String INJECTION_PROVIDER_INSTANCE = "oam.spi.INJECTION_PROVIDER_KEY";
    private static final String INJECTED_BEAN_STORAGE_KEY = "org.apache.myfaces.spi.BEAN_ENTRY_STORAGE";
    private static final String BEAN_ENTRY_CLASS_NAME = "org.apache.myfaces.cdi.util.BeanEntry";

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

    }

    @Override
    public Object getFactory(String factoryName) throws FacesException {
        // This code must be synchronized because this could cause a problem when
        // using update feature each time of myfaces (org.apache.myfaces.CONFIG_REFRESH_PERIOD)
        // In this moment, a concurrency problem could happen
        Map<String, List<String>> factoryClassNames = null;

        synchronized (registeredFactoryNames) {
            LOGGER.info("registeredFactoryNames: " + registeredFactoryNames);
            factoryClassNames = registeredFactoryNames;

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

            if (!factoryClassNames.containsKey(factoryName)) {
                throw new IllegalArgumentException("no factory " + factoryName + " configured for this application.");
            }

        }

        List beanEntryStorage;

        synchronized (factoryClassNames) {
            beanEntryStorage = (List) factories.computeIfAbsent(INJECTED_BEAN_STORAGE_KEY,
                    k -> new CopyOnWriteArrayList());
        }

        List<String> classNames;
        Object factory;
        Object injectionProvider;
        synchronized (factoryClassNames) {
            factory = factories.get(factoryName);
            if (factory != null) {
                return factory;
            }

            classNames = factoryClassNames.get(factoryName);

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
                classNames.iterator(), injectionProvider, beanEntryStorage);

        synchronized (factoryClassNames) {
            // check if someone else already installed the factory
            if (factories.get(factoryName) == null) {
                factories.put(factoryName, factory);
            }
        }

        return factory;
    }

    private Object newFactoryInstance(Class<?> interfaceClass, Iterator<String> classNamesIterator,
            Object injectionProvider,
            List injectedBeanStorage) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Object current = null;

            while (classNamesIterator.hasNext()) {
                String implClassName = classNamesIterator.next();
                Class<?> implClass = null;
                try {
                    implClass = classLoader.loadClass(implClassName);
                } catch (ClassNotFoundException e) {
                    implClass = getClass().getClassLoader().loadClass(implClassName);
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

    private void injectAndPostConstruct(Object injectionProvider, Object instance, List injectedBeanStorage) {
        if (injectionProvider != null) {
            try {
                Object creationMetaData = _FactoryFinderProviderFactory.INJECTION_PROVIDER_INJECT_METHOD.invoke(
                        injectionProvider, instance);

                addBeanEntry(instance, creationMetaData, injectedBeanStorage);

                _FactoryFinderProviderFactory.INJECTION_PROVIDER_POST_CONSTRUCT_METHOD.invoke(
                        injectionProvider, instance, creationMetaData);
            } catch (Exception ex) {
                throw new FacesException(ex);
            }
        }
    }

    private void preDestroy(Object injectionProvider, Object beanEntry) {
        if (injectionProvider != null) {
            try {
                _FactoryFinderProviderFactory.INJECTION_PROVIDER_PRE_DESTROY_METHOD.invoke(
                        injectionProvider, getInstance(beanEntry), getCreationMetaData(beanEntry));
            } catch (Exception ex) {
                throw new FacesException(ex);
            }
        }
    }

    private Object getInstance(Object beanEntry) {
        try {
            Method getterMethod = getMethod(beanEntry, "getInstance");
            return getterMethod.invoke(beanEntry);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Object getCreationMetaData(Object beanEntry) {
        try {
            Method getterMethod = getMethod(beanEntry, "getCreationMetaData");
            return getterMethod.invoke(beanEntry);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Method getMethod(Object beanEntry, String methodName) throws NoSuchMethodException {
        return beanEntry.getClass().getDeclaredMethod(methodName);
    }

    private void addBeanEntry(Object instance, Object creationMetaData, List injectedBeanStorage) {
        try {
            Class<?> beanEntryClass = _FactoryFinderProviderFactory.classForName(BEAN_ENTRY_CLASS_NAME);
            Constructor beanEntryConstructor = beanEntryClass.getDeclaredConstructor(Object.class, Object.class);

            Object result = beanEntryConstructor.newInstance(instance, creationMetaData);
            injectedBeanStorage.add(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getInjectionProvider() {
        try {
            // Remember the first call in a webapp over FactoryFinder.getFactory(...) comes in the
            // initialization block, so there is a startup FacesContext active and
            // also a valid startup ExternalContext. Note after that, we need to cache
            // the injection provider for the classloader, because in a normal
            // request there is no active FacesContext in the moment and this call will
            // surely fail.
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                Object injectionProviderFactory = _FactoryFinderProviderFactory.INJECTION_PROVIDER_FACTORY_GET_INSTANCE_METHOD
                        .invoke(_FactoryFinderProviderFactory.INJECTION_PROVIDER_CLASS);
                Object injectionProvider = _FactoryFinderProviderFactory.INJECTION_PROVIDER_FACTORY_GET_INJECTION_PROVIDER_METHOD
                        .invoke(injectionProviderFactory, facesContext.getExternalContext());
                return injectionProvider;
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public void releaseFactories() throws FacesException {
        // This code must be synchronized
        synchronized (registeredFactoryNames) {
            registeredFactoryNames.clear();
        }

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

    @Override
    public void setFactory(String factoryName, String implName) {
        List<String> factoryClassNames = null;
        synchronized (registeredFactoryNames) {
            if (factories != null && factories.containsKey(factoryName)) {
                // Javadoc says ... This method has no effect if getFactory() has already been
                // called looking for a factory for this factoryName.
                return;
            }

            factoryClassNames = registeredFactoryNames.computeIfAbsent(factoryName,
                    k -> new ArrayList<>());
        }

        synchronized (factoryClassNames) {
            factoryClassNames.add(implName);
        }
    }
}
