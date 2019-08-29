/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.myfaces.runtime.scopes;

import java.lang.annotation.Annotation;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.CDI;
import javax.faces.flow.FlowScoped;

import org.apache.myfaces.flow.cdi.FlowScopedContextImpl;

import io.quarkus.arc.InjectableContext;
import io.quarkus.myfaces.runtime.MyFacesRecorder;

public class QuarkusFlowScopedContext implements InjectableContext {

    private FlowScopedContextImpl wrapped;

    public QuarkusFlowScopedContext() {
    }

    public FlowScopedContextImpl getWrapped() {
        if (wrapped == null) {
            wrapped = new FlowScopedContextImpl(CDI.current().getBeanManager(), MyFacesRecorder.FLOW_REFERENCES);
        }
        return wrapped;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void destroy(Contextual<?> contextual) {
        getWrapped().destroy(contextual);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return FlowScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> cc) {
        return getWrapped().get(contextual, cc);
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return getWrapped().get(contextual);
    }

    @Override
    public boolean isActive() {
        return getWrapped().isActive();
    }

    @Override
    public InjectableContext.ContextState getState() {
        return null;
    }
}
