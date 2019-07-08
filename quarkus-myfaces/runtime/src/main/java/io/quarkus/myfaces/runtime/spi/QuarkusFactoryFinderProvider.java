package io.quarkus.myfaces.runtime.spi;

import javax.enterprise.inject.spi.CDI;

import org.apache.myfaces.spi.FactoryFinderProvider;
import org.apache.myfaces.spi.FactoryFinderProviderFactory;

import io.quarkus.myfaces.runtime.QuarkusFactoryFinder;

public class QuarkusFactoryFinderProvider extends FactoryFinderProviderFactory {
    @Override
    public FactoryFinderProvider getFactoryFinderProvider() {
        return CDI.current().select(QuarkusFactoryFinder.class).get();
    }
}
