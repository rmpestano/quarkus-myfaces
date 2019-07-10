package io.quarkus.myfaces.runtime.spi;

import javax.faces.QuarkusFactoryFinder;

import org.apache.myfaces.spi.FactoryFinderProvider;
import org.apache.myfaces.spi.FactoryFinderProviderFactory;

public class QuarkusFactoryFinderProvider extends FactoryFinderProviderFactory {

    private static final QuarkusFactoryFinder quarkusFactoryFinder = new QuarkusFactoryFinder();

    @Override
    public FactoryFinderProvider getFactoryFinderProvider() {
        return quarkusFactoryFinder;
    }
}
