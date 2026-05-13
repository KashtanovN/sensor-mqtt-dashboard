package com.company.userregistration.view.sensor;

import com.company.userregistration.entity.Sensor;
import com.company.userregistration.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Route(value = "sensors", layout = MainView.class)
@ViewController("Sensor.list")
@ViewDescriptor("sensor-list-view.xml")
@LookupComponent("sensorsDataGrid")
public class SensorListView extends StandardListView<Sensor> {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private MessageBundle messageBundle;

    @ViewComponent
    private DataGrid<Sensor> sensorsDataGrid;

    @Subscribe("openChartBtn")
    public void onOpenChartBtnClick(final ClickEvent<JmixButton> event) {
        Sensor sensor = sensorsDataGrid.getSingleSelectedItem();
        if (sensor == null && sensorsDataGrid.getSelectedItems().size() == 1) {
            sensor = sensorsDataGrid.getSelectedItems().iterator().next();
        }
        if (sensor == null) {
            List<Sensor> sensors = dataManager.load(Sensor.class).all().maxResults(2).list();
            if (sensors.size() == 1) {
                sensor = sensors.get(0);
            }
        }
        if (sensor == null) {
            notifications.create(messageBundle.getMessage("selectSensorFirst")).show();
            return;
        }

        UI.getCurrent().navigate(SensorDataView.class,
                new RouteParameters(Map.of("sensorId", sensor.getId().toString())));
    }
}
