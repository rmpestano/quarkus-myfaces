/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.myfaces.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.el.BeanELResolver;
import javax.faces.FactoryFinder;
import javax.faces.application.ProjectStage;
import javax.faces.application.StateManager;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.convert.FacesConverter;
import javax.faces.event.*;
import javax.faces.push.PushContext;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletsResourceResolver;
import javax.faces.webapp.FacesServlet;

import org.apache.el.ValueExpressionImpl;
import org.apache.myfaces.application.ApplicationFactoryImpl;
import org.apache.myfaces.application.ApplicationImpl;
import org.apache.myfaces.application.viewstate.StateUtils;
import org.apache.myfaces.cdi.FacesScoped;
import org.apache.myfaces.cdi.JsfApplicationArtifactHolder;
import org.apache.myfaces.cdi.JsfArtifactProducer;
import org.apache.myfaces.cdi.config.FacesConfigBeanHolder;
import org.apache.myfaces.cdi.model.FacesDataModelClassBeanHolder;
import org.apache.myfaces.cdi.util.BeanEntry;
import org.apache.myfaces.cdi.view.ViewScopeBeanHolder;
import org.apache.myfaces.cdi.view.ViewTransientScoped;
import org.apache.myfaces.component.search.SearchExpressionContextFactoryImpl;
import org.apache.myfaces.component.visit.VisitContextFactoryImpl;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.config.MyfacesConfig;
import org.apache.myfaces.config.annotation.CdiAnnotationProviderExtension;
import org.apache.myfaces.config.annotation.DefaultLifecycleProviderFactory;
import org.apache.myfaces.config.element.NamedEvent;
import org.apache.myfaces.context.ExceptionHandlerFactoryImpl;
import org.apache.myfaces.context.ExternalContextFactoryImpl;
import org.apache.myfaces.context.FacesContextFactoryImpl;
import org.apache.myfaces.context.PartialViewContextFactoryImpl;
import org.apache.myfaces.context.servlet.FacesContextImplBase;
import org.apache.myfaces.context.servlet.ServletFlashFactoryImpl;
import org.apache.myfaces.el.resolver.CompositeELResolver;
import org.apache.myfaces.flow.FlowHandlerFactoryImpl;
import org.apache.myfaces.flow.cdi.FlowBuilderFactoryBean;
import org.apache.myfaces.flow.cdi.FlowScopeBeanHolder;
import org.apache.myfaces.lifecycle.ClientWindowFactoryImpl;
import org.apache.myfaces.lifecycle.LifecycleFactoryImpl;
import org.apache.myfaces.lifecycle.RestoreViewSupport;
import org.apache.myfaces.push.cdi.*;
import org.apache.myfaces.renderkit.ErrorPageWriter;
import org.apache.myfaces.renderkit.RenderKitFactoryImpl;
import org.apache.myfaces.renderkit.html.HtmlRenderKitImpl;
import org.apache.myfaces.spi.FactoryFinderProviderFactory;
import org.apache.myfaces.spi.impl.DefaultWebConfigProviderFactory;
import org.apache.myfaces.util.ClassUtils;
import org.apache.myfaces.view.ViewDeclarationLanguageFactoryImpl;
import org.apache.myfaces.view.ViewScopeProxyMap;
import org.apache.myfaces.view.facelets.compiler.SAXCompiler;
import org.apache.myfaces.view.facelets.compiler.TagLibraryConfig;
import org.apache.myfaces.view.facelets.impl.FaceletCacheFactoryImpl;
import org.apache.myfaces.view.facelets.tag.MetaRulesetImpl;
import org.apache.myfaces.view.facelets.tag.jsf.TagHandlerDelegateFactoryImpl;
import org.apache.myfaces.webapp.FaceletsInitilializer;
import org.apache.myfaces.webapp.MyFacesContainerInitializer;
import org.apache.myfaces.webapp.StartupServletContextListener;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;
import com.sun.org.apache.xpath.internal.functions.FuncLocalPart;
import com.sun.org.apache.xpath.internal.functions.FuncNot;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.ContextRegistrarBuildItem;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.*;
import io.quarkus.myfaces.runtime.MyFacesTemplate;
import io.quarkus.myfaces.runtime.QuarkusApplicationFactory;
import io.quarkus.myfaces.runtime.QuarkusServletContextListener;
import io.quarkus.myfaces.runtime.exception.QuarkusExceptionHandlerFactory;
import io.quarkus.myfaces.runtime.scopes.QuarkusFacesScopeContext;
import io.quarkus.myfaces.runtime.scopes.QuarkusViewScopeContext;
import io.quarkus.myfaces.runtime.scopes.QuarkusViewTransientScopeContext;
import io.quarkus.myfaces.runtime.spi.QuarkusInjectionProvider;
import io.quarkus.myfaces.runtime.spi.QuarkusResourceResolver;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.undertow.deployment.ListenerBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

class MyFacesProcessor {

    private static final Logger LOGGER = Logger.getLogger(MyFacesTemplate.class.getName());

    private static final Class[] BEAN_CLASSES = {
            JsfApplicationArtifactHolder.class,
            JsfArtifactProducer.class,
            FacesConfigBeanHolder.class,
            FacesDataModelClassBeanHolder.class,
            ViewScopeBeanHolder.class,
            CdiAnnotationProviderExtension.class,
            PushContextFactoryBean.class,
            WebsocketChannelTokenBuilderBean.class,
            WebsocketSessionBean.class,
            WebsocketViewBean.class,
            WebsocketApplicationBean.class,
            FlowBuilderFactoryBean.class,
            FlowScopeBeanHolder.class
    };

    private static final String[] BEAN_DEFINING_ANNOTATION_CLASSES = {
            FacesComponent.class.getName(),
            FacesBehavior.class.getName(),
            FacesConverter.class.getName(),
            FacesValidator.class.getName(),
            FacesRenderer.class.getName(),
            NamedEvent.class.getName(),
            FacesBehaviorRenderer.class.getName(),
            FaceletsResourceResolver.class.getName()
    };

    private static final String[] FACES_FACTORIES = {
            QuarkusApplicationFactory.class.getName(),
            QuarkusExceptionHandlerFactory.class.getName(),
            ApplicationFactoryImpl.class.getName(),
            ExternalContextFactoryImpl.class.getName(),
            FacesContextFactoryImpl.class.getName(),
            LifecycleFactoryImpl.class.getName(),
            RenderKitFactoryImpl.class.getName(),
            PartialViewContextFactoryImpl.class.getName(),
            VisitContextFactoryImpl.class.getName(),
            ViewDeclarationLanguageFactoryImpl.class.getName(),
            ExceptionHandlerFactoryImpl.class.getName(),
            TagHandlerDelegateFactoryImpl.class.getName(),
            HtmlRenderKitImpl.class.getName(),
            SearchExpressionContextFactoryImpl.class.getName(),
            FlowHandlerFactoryImpl.class.getName(),
            ClientWindowFactoryImpl.class.getName(),
            ServletFlashFactoryImpl.class.getName(),
            FaceletCacheFactoryImpl.class.getName()
    };

    @BuildStep
    void buildFeature(BuildProducer<FeatureBuildItem> feature) throws IOException {
        feature.produce(new FeatureBuildItem("myfaces"));
    }

    @BuildStep
    void buildServlet(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ListenerBuildItem> listener) throws IOException {

        servlet.produce(ServletBuildItem.builder("Faces Servlet", FacesServlet.class.getName())
                .setLoadOnStartup(1)
                .addMapping("*.xhtml")
                .build());

        listener.produce(new ListenerBuildItem(QuarkusServletContextListener.class.getName()));
        listener.produce(new ListenerBuildItem(StartupServletContextListener.class.getName()));
    }

    @BuildStep
    void buildCdiBeans(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ListenerBuildItem> listener,
            BuildProducer<AdditionalBeanBuildItem> additionalBean,
            BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotation,
            BuildProducer<ContextRegistrarBuildItem> contextRegistrar) throws IOException {

        for (Class<?> clazz : BEAN_CLASSES) {
            additionalBean.produce(AdditionalBeanBuildItem.unremovableOf(clazz));
        }

        for (String clazz : BEAN_DEFINING_ANNOTATION_CLASSES) {
            beanDefiningAnnotation.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(clazz)));
        }

    }

    @BuildStep
    void buildCdiScopes(BuildProducer<ContextRegistrarBuildItem> contextRegistrar) throws IOException {

        contextRegistrar.produce(new ContextRegistrarBuildItem(new ContextRegistrar() {
            @Override
            public void register(ContextRegistrar.RegistrationContext registrationContext) {
                registrationContext.configure(ViewScoped.class).normal().contextClass(QuarkusViewScopeContext.class).done();
                registrationContext.configure(FacesScoped.class).normal().contextClass(QuarkusFacesScopeContext.class).done();
                registrationContext.configure(ViewTransientScoped.class).normal()
                        .contextClass(QuarkusViewTransientScopeContext.class).done();
            }
        }));
    }

    @BuildStep
    void buildInitParams(BuildProducer<ServletInitParamBuildItem> initParam) throws IOException {

        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.INJECTION_PROVIDER, QuarkusInjectionProvider.class.getName()));
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.FACES_INITIALIZER, FaceletsInitilializer.class.getName()));
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.SUPPORT_JSP, "false"));

        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.FACELETS_RESOURCE_RESOLVER, QuarkusResourceResolver.class.getName()));
    }

    @BuildStep
    void buildRecommendedInitParams(BuildProducer<ServletInitParamBuildItem> initParam) throws IOException {

        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.LOG_WEB_CONTEXT_PARAMS, "false"));
        initParam.produce(new ServletInitParamBuildItem(
                StateManager.STATE_SAVING_METHOD_PARAM_NAME, StateManager.STATE_SAVING_METHOD_SERVER));
        initParam.produce(new ServletInitParamBuildItem(
                StateManager.SERIALIZE_SERVER_STATE_PARAM_NAME, "false"));

        // perf
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.CHECK_ID_PRODUCTION_MODE, "false"));
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.EARLY_FLUSH_ENABLED, "true"));
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.CACHE_EL_EXPRESSIONS, "alwaysRecompile"));
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.COMPRESS_STATE_IN_SESSION, "false"));
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.RESOURCE_MAX_TIME_EXPIRES, "86400000")); // 1 day
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.RESOURCE_CACHE_LAST_MODIFIED, "true"));

        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.NUMBER_OF_VIEWS_IN_SESSION, "15"));
        initParam.produce(new ServletInitParamBuildItem(
                MyfacesConfig.NUMBER_OF_SEQUENTIAL_VIEWS_IN_SESSION, "3"));

        // primefaces perf
        initParam.produce(new ServletInitParamBuildItem(
                "primefaces.SUBMIT", "partial"));
        initParam.produce(new ServletInitParamBuildItem(
                "primefaces.MOVE_SCRIPTS_TO_BOTTOM", "true"));

        // user config
        Config config = ConfigProvider.getConfig();

        Optional<String> projectStage = resolveProjectStage(config);
        initParam.produce(new ServletInitParamBuildItem(ProjectStage.PROJECT_STAGE_PARAM_NAME, projectStage.get()));

        Optional<String> enableWebsocketsEndpoint = config.getOptionalValue(PushContext.ENABLE_WEBSOCKET_ENDPOINT_PARAM_NAME,
                String.class);
        if (enableWebsocketsEndpoint.isPresent()) {
            initParam.produce(new ServletInitParamBuildItem(PushContext.ENABLE_WEBSOCKET_ENDPOINT_PARAM_NAME,
                    enableWebsocketsEndpoint.get()));
        }
    }

    @BuildStep
    @Record(STATIC_INIT)
    void buildAnnotationProviderIntegration(MyFacesTemplate template, CombinedIndexBuildItem combinedIndex) throws IOException {

        for (String clazz : BEAN_DEFINING_ANNOTATION_CLASSES) {
            combinedIndex.getIndex()
                    .getAnnotations(DotName.createSimple(clazz))
                    .stream()
                    .forEach(annotation -> template.registerAnnotatedClass(annotation.name().toString(),
                            annotation.target().asClass().name().toString()));
        }
    }

    /**
     * Enables JNI support to avoid: Error: Unsupported method sun.font.SunLayoutEngine.nativeLayout
     *
     * @param extensionSslNativeSupport
     */
    @BuildStep
    @Record(STATIC_INIT)
    void enableSSlStep(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem("myfaces"));
    }

    @BuildStep(applicationArchiveMarkers = { "org/primefaces/component", "org/apache/myfaces/view", "javax/faces/component" })
    @Record(STATIC_INIT)
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {

        List<String> tagHandlers = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.faces.view.facelets.TagHandler"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> converterHandlers = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.faces.view.facelets.ConverterHandler"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> componentHandlers = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.faces.view.facelets.ComponentHandler"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> validatorHandlers = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.faces.view.facelets.ValidatorHandler"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> renderers = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.faces.render.Renderer"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> components = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.faces.component.UIComponent"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> valueExpressions = combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.el.ValueExpression"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> converters = combinedIndex.getIndex()
                .getAllKnownImplementors(DotName.createSimple("javax.faces.convert.Converter"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        List<String> primefacesWidgets = combinedIndex.getIndex()
                .getAllKnownImplementors(DotName.createSimple("org.primefaces.component.api.Widget"))
                .stream()
                .map(ClassInfo::toString)
                .collect(Collectors.toList());

        Set<String> collectedClassesForReflection = Stream
                .of(tagHandlers, converterHandlers, componentHandlers, validatorHandlers, renderers, primefacesWidgets,
                        components, converters, valueExpressions, Arrays.asList(FACES_FACTORIES))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        for (String className : collectedClassesForReflection) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, className));
        }

        //el resolvers requires method reflection
        combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple("javax.el.ELResolver"))
                .stream()
                .map(ClassInfo::toString)
                .forEach(className -> reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, className)));

        //class names build itens with limited reflection support
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                "org.primefaces.behavior.ajax.AjaxBehavior",
                "com.lowagie.text.pdf.MappedRandomAccessFile", "org.apache.myfaces.application_ApplicationUtils",
                "com.sun.el.util.ReflectionUtil", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
                "org.primefaces.util.MessageFactory",
                "javax.faces.component._DeltaStateHelper", "javax.faces.component._DeltaStateHelper$InternalMap"));

        //class names build items with method reflection support
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "org.primefaces.util.ComponentUtils",
                "org.primefaces.expression.SearchExpressionUtils", "org.primefaces.util.SecurityUtils",
                "org.primefaces.util.LangUtils",
                "io.quarkus.myfaces.showcase.view.LazyView", "io.quarkus.myfaces.showcase.view.LazyView_ClientProxy",
                "io.quarkus.myfaces.showcase.view.Car", "io.quarkus.myfaces.showcase.view.LazyCarDataModel",
                "io.quarkus.myfaces.showcase.view.CarService", "io.quarkus.myfaces.showcase.view.LazySorter"));

        //classes build items with limited reflection support
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, ExceptionQueuedEvent.class, DefaultWebConfigProviderFactory.class,
                        ErrorPageWriter.class, DocumentBuilderFactoryImpl.class, FuncLocalPart.class, FuncNot.class,
                        MyFacesContainerInitializer.class, DefaultLifecycleProviderFactory.class,
                        RestoreViewSupport.class, UIViewRoot.class, ExceptionQueuedEvent.class,
                        ExceptionQueuedEventContext.class,
                        PostAddToViewEvent.class, ComponentSystemEvent.class,
                        SystemEvent.class,
                        PreRenderComponentEvent.class, FacesConfigurator.class, FaceletsInitilializer.class,
                        TagLibraryConfig.class, String.class, FacesContextImplBase.class,
                        CompositeELResolver.class, javax.el.CompositeELResolver.class, ValueExpressionImpl.class,
                        com.sun.el.ValueExpressionImpl.class, ViewScopeProxyMap.class,
                        QuarkusResourceResolver.class, SAXCompiler.class, StateUtils.class,
                        ApplicationImpl.class));

        //classes build items with method reflection support
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, ClassUtils.class,
                FactoryFinderProviderFactory.class,
                FactoryFinder.class,
                BeanELResolver.class, PreDestroyApplicationEvent.class, BeanEntry.class, MetaRulesetImpl.class));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, FactoryFinderProviderFactory.class));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void runtimeReinit(BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinitProducer) {
        /*
         * runtimeReinitProducer
         * .produce(new RuntimeReinitializedClassBuildItem(Substitute_FactoryFinderProviderFactory.class.getName()));
         * runtimeReinitProducer.produce(new RuntimeReinitializedClassBuildItem(Substitute_FactoryFinder.class.getName()));
         */
    }

    @BuildStep
    void substrateResourceBuildItems(BuildProducer<SubstrateResourceBuildItem> substrateResourceProducer,
            BuildProducer<SubstrateResourceBundleBuildItem> resourceBundleBuildItem) {
        substrateResourceProducer
                .produce(new SubstrateResourceBuildItem("META-INF/maven/org.primefaces/primefaces/pom.properties"));
        substrateResourceProducer.produce(new SubstrateResourceBuildItem("META-INF/rsc/myfaces-dev-error.xml",
                "META-INF/rsc/myfaces-dev-debug.xml", "org/apache/myfaces/resource/default.dtd",
                "org/apache/myfaces/resource/datatypes.dtd", "META-INF/web-fragment.xml",
                "META-INF/resources/org/apache/myfaces/windowId/windowhandler.html",
                "org/apache/myfaces/resource/facelet-taglib_1_0.dtd", "org/apache/myfaces/resource/javaee_5.xsd",
                "org/apache/myfaces/resource/web-facelettaglibrary_2_0.xsd",
                "org/apache/myfaces/resource/XMLSchema.dtd", "org/apache/myfaces/resource/facesconfig_1_0.dtd",
                "org/apache/myfaces/resource/web-facesconfig_1_1.dtd",
                "org/apache/myfaces/resource/web-facesconfig_1_2.dtd", "org/apache/myfaces/resource/web-facesconfig_2_0.dtd",
                "org/apache/myfaces/resource/web-facesconfig_2_1.dtd",
                "org/apache/myfaces/resource/web-facesconfig_2_2.dtd", "org/apache/myfaces/resource/web-facesconfig_2_3.dtd",
                "org/apache/myfaces/resource/web-facesconfig_3_0.dtd",
                "org/apache/myfaces/resource/xml.xsd",
                "META-INF/rsc/myfaces-dev-error-include.xml",
                "META-INF/services/javax.servlet.ServletContainerInitializer"));

        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_ar"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_ca"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_cs"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_de"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_en"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_es"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_fr"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_it"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_ja"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_mt"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_nl"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_pl"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_pt_PR"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_ru"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_sk"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_zh_CN"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_zh_HK"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.faces.Messages_zh_TW"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("javax.el.PrivateMessages"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("org.primefaces.Messages"));
        resourceBundleBuildItem.produce(new SubstrateResourceBundleBuildItem("org.primefaces.Messages_en"));
    }

    private Optional<String> resolveProjectStage(Config config) {
        Optional<String> projectStage = config.getOptionalValue(ProjectStage.PROJECT_STAGE_PARAM_NAME, String.class);
        if (!projectStage.isPresent()) {
            projectStage = Optional.of(ProjectStage.Production.name());
            if (LaunchMode.DEVELOPMENT.getDefaultProfile().equals(ProfileManager.getActiveProfile())) {
                projectStage = Optional.of(ProjectStage.Development.name());
            } else if (LaunchMode.TEST.getDefaultProfile().equals(ProfileManager.getActiveProfile())) {
                projectStage = Optional.of(ProjectStage.SystemTest.name());
            }
        }
        return projectStage;
    }
}
