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

/*
 * public class QuarkusResourceResolver extends ResourceHandlerWrapper {
 * 
 * private static final Logger LOG = Logger.getLogger(QuarkusResourceResolver.class.getName());
 * 
 * private ResourceHandler wrapped;
 * 
 * public QuarkusResourceResolver(ResourceHandler wrapped) {
 * this.wrapped = wrapped;
 * }
 * 
 * 
 * @Override
 * public Resource createResource(String resourceName) {
 * return super.createResource(resourceName);
 * }
 * 
 * @Override
 * public ViewResource createViewResource(FacesContext context, String resourceName) {
 * return super.createViewResource(context, resourceName);
 * }
 * 
 * public URL resolveUrl(String path) {
 * ViewResource faceletResource = wrapped.createViewResource(FacesContext.getCurrentInstance(), path);
 * if (faceletResource != null) {
 * return faceletResource.getURL();
 * } else if(path.equals("/")) {
 * faceletResource = wrapped.createViewResource(FacesContext.getCurrentInstance(), "/index.xhtml");
 * }
 * return faceletResource.getURL();
 * }
 * }
 */
