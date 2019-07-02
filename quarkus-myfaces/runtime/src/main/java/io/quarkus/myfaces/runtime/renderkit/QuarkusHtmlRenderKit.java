package io.quarkus.myfaces.runtime.renderkit;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitWrapper;
import javax.faces.render.Renderer;

import org.apache.myfaces.config.element.ClientBehaviorRenderer;
import org.apache.myfaces.renderkit.html.HtmlRenderKitImpl;
import org.apache.myfaces.util.ClassUtils;

import io.quarkus.myfaces.runtime.MyFacesTemplate;

public class QuarkusHtmlRenderKit extends RenderKitWrapper {

    private static final Logger LOGGER = Logger.getLogger(QuarkusHtmlRenderKit.class.getName());

    private HtmlRenderKitImpl htmlRenderKit;

    @PostConstruct
    public void initRenderKit() { //TODO move to buildTime
        LOGGER.info("Initializing QuarkusHtmlRenderKit...");
        htmlRenderKit = new HtmlRenderKitImpl();

        MyFacesTemplate.COMPONENT_CLASSES
                .stream()
                .map(componentClass -> {
                    try {
                        return (UIComponent) componentClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalStateException("Could not instantiate renderer class: " + componentClass, e);
                    }
                })
                .filter(uiComponent -> uiComponent.getRendererType() != null)
                .forEach(addRenderer());

        MyFacesTemplate.CLIENT_BEHAVIOUR_CLASSES
                .stream()
                .map(behaviourClass -> {
                    try {
                        return (ClientBehaviorRenderer) behaviourClass.newInstance(); //FIXME this cast will fail
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalStateException("Could not instantiate client behaviour class: " + behaviourClass, e);
                    }
                })
                .forEach(addClientBehaviourRenderer());
    }

    private Consumer<UIComponent> addRenderer() {
        return uiComponent -> {
            LOGGER.info("Adding renderer for component: " + uiComponent + " family: " + uiComponent.getFamily()
                    + " rendererType: " + uiComponent.getRendererType());
            htmlRenderKit.addRenderer(uiComponent.getFamily(), uiComponent.getRendererType(),
                    getRenderer(uiComponent));
        };
    }

    private javax.faces.render.Renderer getRenderer(UIComponent component) {
        Renderer result = null;
        String rendererType = component.getRendererType();
        if (rendererType != null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            LOGGER.info("facesContext: " + facesContext);
            result = facesContext.getRenderKit().getRenderer(
                    component.getFamily(),
                    rendererType);
            if (null == result) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Can't get Renderer for type " + rendererType);
                }
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                String id = component.getId();
                id = (null != id) ? id : this.getClass().getName();
                LOGGER.fine("No renderer-type for component " + id);
            }
        }
        return result;
    }

    private Consumer<ClientBehaviorRenderer> addClientBehaviourRenderer() {
        return (clientBehaviorRenderer) -> {
            htmlRenderKit.addClientBehaviorRenderer(clientBehaviorRenderer.getRendererType(),
                    (javax.faces.render.ClientBehaviorRenderer) ClassUtils.newInstance(clientBehaviorRenderer.getRendererClass()));
        };
    }

    @Override
    public RenderKit getWrapped() {
        return htmlRenderKit;
    }

}
