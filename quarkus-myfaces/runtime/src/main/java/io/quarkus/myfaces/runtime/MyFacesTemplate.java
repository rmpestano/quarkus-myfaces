package io.quarkus.myfaces.runtime;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.faces.component.UIComponent;

import org.apache.myfaces.config.element.ClientBehaviorRenderer;

import io.quarkus.runtime.annotations.Template;

@Template
public class MyFacesTemplate {

    public static final Map<Class<? extends Annotation>, Set<Class<?>>> ANNOTATED_CLASSES = new LinkedHashMap<>();
    public static final Map<String, List<String>> FACES_FACTORIES = new HashMap<>();

    public static final Set<Class<? extends UIComponent>> COMPONENT_CLASSES = new HashSet<>();

    public static final Set<Class<? extends ClientBehaviorRenderer>> CLIENT_BEHAVIOUR_CLASSES = new HashSet<>();

    @SuppressWarnings("unchecked") //cast to (Class<? extends Annotation>)
    public void registerAnnotatedClass(String annotationName, String clazzName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            Class<? extends Annotation> annotation = (Class<? extends Annotation>) cl.loadClass(annotationName);
            Class<?> clazz = cl.loadClass(clazzName);

            Set<Class<?>> classes = ANNOTATED_CLASSES.computeIfAbsent(annotation, $ -> new HashSet<>());
            classes.add(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public void registerFactory(String factory, String factoryImpl) {
        List<String> factories = FACES_FACTORIES.computeIfAbsent(factory, $ -> new ArrayList<>());
        factories.add(factoryImpl);
    }

    public void registerComponents(List<String> componentClasses) {

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            for (String rendererClass : componentClasses) {
                COMPONENT_CLASSES.add((Class<? extends UIComponent>) cl.loadClass(rendererClass));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public void registerClientBehaviour(List<String> clientBehaviourClasses) {

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            for (String clientBehaviourClass : clientBehaviourClasses) {
                CLIENT_BEHAVIOUR_CLASSES.add((Class<? extends ClientBehaviorRenderer>) cl.loadClass(clientBehaviourClass));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
