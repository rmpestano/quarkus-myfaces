package io.quarkus.myfaces.showcase.view;

import java.io.Serializable;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyDataModel;

@Named("dtLazyView")
@ViewScoped
public class LazyView implements Serializable {

    private static final Logger LOG = Logger.getLogger(LazyView.class.getName());

    private Car selectedCar;
    private LazyDataModel<Car> lazyModel;

    @Inject
    CarService service;

    @PostConstruct
    public void init() {
        LOG.info("PostConstruct");
        lazyModel = new LazyCarDataModel(service.createCars(200));
    }

    public LazyDataModel<Car> getLazyModel() {
        LOG.info("getLazyModel:"+lazyModel);
        LOG.info("lazyModel rowCount:"+lazyModel.getRowCount());
        return lazyModel;
    }

    public Car getSelectedCar() {
        return selectedCar;
    }

    public void setSelectedCar(Car selectedCar) {
        this.selectedCar = selectedCar;
    }

    public void setService(CarService service) {
        this.service = service;
    }

    public void onRowSelect(SelectEvent event) {
        FacesMessage msg = new FacesMessage("Car Selected", ((Car) event.getObject()).getId());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }
}
