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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.spi.AnnotationProvider;

import io.quarkus.myfaces.runtime.MyFacesRecorder;

/**
 * AnnotationProvider which uses the collected classes from the Quarkus Deployment-time.
 */
public class QuarkusAnnotationProvider extends AnnotationProvider {

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(ExternalContext ctx) {
        return MyFacesRecorder.ANNOTATED_CLASSES;
    }

    @Override
    public Set<URL> getBaseUrls(ExternalContext ctx) throws IOException {
        return Collections.emptySet();
    }

}
