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

import java.net.URL;

import org.apache.myfaces.view.facelets.impl.DefaultResourceResolver;

// prevents a NPE, see #3
public class QuarkusResourceResolver extends DefaultResourceResolver {
    @Override
    public URL resolveUrl(String resource) {
        URL resourceUrl = super.resolveUrl(resource);
        if (resourceUrl == null) {
            if (resource.equals("/")) {
                resource = "/index.xhtml";
            }
            resourceUrl = super.resolveUrl(resource);
        }
        return resourceUrl;
    }
}
