package io.quarkus.myfaces.runtime;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.FacesException;

import org.apache.myfaces.spi.FactoryFinderProvider;

@ApplicationScoped
public class QuarkusFactoryFinder implements FactoryFinderProvider {

    private static final Logger LOG = Logger.getLogger(QuarkusFactoryFinder.class.getName());

    @Override
    public Object getFactory(String factoryName) throws FacesException {
        Object o = null;
        try {
            o = Thread.currentThread().getContextClassLoader().loadClass(MyFacesTemplate.FACES_FACTORIES.get(factoryName))
                    .newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return o;
    }

    @Override
    public void releaseFactories() throws FacesException {
        MyFacesTemplate.FACES_FACTORIES.clear();
    }

    @Override
    public void setFactory(String factoryName, String implName) {
        MyFacesTemplate.FACES_FACTORIES.putIfAbsent(factoryName, implName);
    }
}
