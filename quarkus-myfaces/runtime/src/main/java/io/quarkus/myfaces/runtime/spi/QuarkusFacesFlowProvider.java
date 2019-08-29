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
package io.quarkus.myfaces.runtime.spi;

import java.util.Iterator;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import javax.faces.flow.builder.FlowDefinition;

import org.apache.myfaces.flow.cdi.DefaultCDIFacesFlowProvider;

public class QuarkusFacesFlowProvider extends DefaultCDIFacesFlowProvider {

    @Override
    public Iterator<Flow> getAnnotatedFlows(FacesContext facesContext) {
        return CDI.current().select(Flow.class, new AnnotationLiteral<FlowDefinition>() {
        }).stream().iterator();
    }
}
