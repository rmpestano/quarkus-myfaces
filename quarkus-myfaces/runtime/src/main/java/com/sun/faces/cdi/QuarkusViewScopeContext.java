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
package com.sun.faces.cdi;

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableContext;

public class QuarkusViewScopeContext implements InjectableContext {

    private ViewScopeContext viewScopeContext = new ViewScopeContext();

    public QuarkusViewScopeContext() {
    }

    @Override
    public Collection<ContextInstanceHandle<?>> getAll() {
        return null;
    }

    @Override
    public void destroy() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ViewScopeManager.getInstance(facesContext).getContextManager().clear(facesContext);
    }

    @Override
    public void destroy(Contextual contextual) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ViewScopeContextManager contextManager = ViewScopeManager.getInstance(facesContext).getContextManager();
        ViewScopeContextObject viewScopeObject = contextManager.getViewScopeObject(facesContext, contextual);
        Object instance = viewScopeContext.get(contextual);
        contextual.destroy(instance, viewScopeObject.getCreationalContext());
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ViewScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> cc) {
        return viewScopeContext.get(contextual, cc);
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return viewScopeContext.get(contextual);
    }

    @Override
    public boolean isActive() {
        return viewScopeContext.isActive();
    }

}
