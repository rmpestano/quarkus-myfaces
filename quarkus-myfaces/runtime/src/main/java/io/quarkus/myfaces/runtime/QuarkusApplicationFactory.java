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
package io.quarkus.myfaces.runtime;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;

public class QuarkusApplicationFactory extends ApplicationFactory {

    private Application application;

    public QuarkusApplicationFactory(ApplicationFactory delegate) {
        super(delegate);
    }

    @Override
    public Application getApplication() {
        if (application == null) {
            application = new QuarkusApplication(getWrapped().getApplication());
        }

        return application;
    }

    @Override
    public void setApplication(Application application) {
        this.application = new QuarkusApplication(application);
        getWrapped().setApplication(application);
    }

}
