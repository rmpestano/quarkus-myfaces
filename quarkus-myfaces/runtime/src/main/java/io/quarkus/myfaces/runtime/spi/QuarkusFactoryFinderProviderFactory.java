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

import org.apache.myfaces.spi.FactoryFinderProvider;
import org.apache.myfaces.spi.FactoryFinderProviderFactory;

public class QuarkusFactoryFinderProviderFactory extends FactoryFinderProviderFactory {

    private static final QuarkusFactoryFinderProvider FACTORY_FINDER_PROVIDER = new QuarkusFactoryFinderProvider();

    @Override
    public FactoryFinderProvider getFactoryFinderProvider() {
        return FACTORY_FINDER_PROVIDER;
    }

}
