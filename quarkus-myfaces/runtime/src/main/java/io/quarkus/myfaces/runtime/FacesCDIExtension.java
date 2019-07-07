package io.quarkus.myfaces.runtime;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

import com.sun.faces.cdi.CdiExtension;

@Alternative
@Priority(1)
@Singleton
public class FacesCDIExtension extends CdiExtension {

    @Override
    public boolean isAddBeansForJSFImplicitObjects() {
        return false;
    }

}
