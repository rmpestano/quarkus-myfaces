package io.quarkus.myfaces.runtime.graal;

import javax.enterprise.inject.spi.CDI;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.myfaces.runtime.renderkit.QuarkusHtmlRenderKit;

/**
 * replaces original getRenderKit because renderKits added at build time are empty in runtime on GraalVM
 * See original one here:
 * https://github.com/apache/myfaces/blob/cfa3c56b8e60d7db3e235b81131fcfa3e5d1326a/impl/src/main/java/org/apache/myfaces/renderkit/RenderKitFactoryImpl.java#L76
 */
@TargetClass(className = "org.apache.myfaces.renderkit.RenderKitFactoryImpl")
public final class Substutute_RenderKitFactoryImpl {

    @Substitute
    public RenderKit getRenderKit(FacesContext context, String renderKitId) throws FacesException {
        return CDI.current().select(QuarkusHtmlRenderKit.class).get();
    }
}
