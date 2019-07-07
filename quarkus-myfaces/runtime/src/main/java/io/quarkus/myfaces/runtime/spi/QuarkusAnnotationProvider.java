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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import com.sun.faces.spi.AnnotationProvider;

import io.quarkus.myfaces.runtime.MyFacesTemplate;

/**
 * AnnotationProvider which uses the collected classes from the Quarkus Deployment-time.
 */
public class QuarkusAnnotationProvider extends AnnotationProvider {

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(Set<URI> urls) {
        return MyFacesTemplate.ANNOTATED_CLASSES;
    }
}
