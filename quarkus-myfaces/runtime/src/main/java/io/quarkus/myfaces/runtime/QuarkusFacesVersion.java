package io.quarkus.myfaces.runtime;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;

import com.sun.faces.application.applicationimpl.Version;

@Alternative
@Priority(1)
public class QuarkusFacesVersion extends Version {

    @Override
    public boolean isJsf23() {
        return true;
    }

}
