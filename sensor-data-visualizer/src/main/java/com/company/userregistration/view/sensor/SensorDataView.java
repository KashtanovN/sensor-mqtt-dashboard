package com.company.userregistration.view.sensor;

import com.company.userregistration.entity.Sensor;
import com.company.userregistration.entity.SensorReading;
import com.company.userregistration.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.series.AbstractSeries;
import io.jmix.chartsflowui.kit.component.model.series.LineSeries;
import io.jmix.chartsflowui.kit.component.model.shared.AbstractTooltip;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View.BeforeShowEvent;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Route(value = "sensors/:sensorId/data", layout = MainView.class)
@ViewController("Sensor.data")
@ViewDescriptor("sensor-data-view.xml")
public class SensorDataView extends StandardView {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private CollectionLoader<SensorReading> readingsDl;

    @ViewComponent
    private Span sensorSummary;

    @ViewComponent
    private Chart temperatureChart;

    private UUID sensorId;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        sensorId = event.getRouteParameters().get("sensorId")
                .map(UUID::fromString)
                .orElse(null);
        if (sensorId == null) {
            sensorId = parseSensorIdFromPath(event);
        }
        if (sensorId != null && readingsDl != null) {
            readingsDl.setParameter("sensor", sensorId);
        }
        super.beforeEnter(event);
    }

    private static UUID parseSensorIdFromPath(BeforeEnterEvent event) {
        List<String> segments = event.getLocation().getSegments();
        if (segments.size() >= 3
                && "sensors".equalsIgnoreCase(segments.get(0))
                && "data".equalsIgnoreCase(segments.get(segments.size() - 1))) {
            try {
                return UUID.fromString(segments.get(1));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        if (sensorId == null) {
            notifications.create("Missing sensor id").show();
            UI.getCurrent().navigate(SensorListView.class);
            return;
        }

        Sensor sensor = dataManager.load(Sensor.class).id(sensorId).optional().orElse(null);
        if (sensor == null) {
            notifications.create("Sensor not found").show();
            UI.getCurrent().navigate(SensorListView.class);
            return;
        }

        sensorSummary.setText(sensor.getName() + " — " + sensor.getMqttTopic());

        configureTemperatureChart();

        readingsDl.setParameter("sensor", sensorId);
        readingsDl.load();
    }

    /**
     * Dense line series: ECharts only draws a subset of point symbols ("large" threshold), which looks random.
     * We hide default symbols and use an axis tooltip so the exact category (measuredAt) and value are always clear.
     */
    private void configureTemperatureChart() {
        if (temperatureChart == null) {
            return;
        }
        var seriesList = temperatureChart.getSeries();
        if (seriesList != null) {
            for (AbstractSeries<?> s : seriesList) {
                if (s instanceof LineSeries line) {
                    line.setShowSymbol(false);
                    break;
                }
            }
        }
        var axisPointer = new AbstractTooltip.AxisPointer()
                .withType(AbstractTooltip.AxisPointer.IndicatorType.LINE)
                .withSnap(true);
        var tooltip = new Tooltip()
                .withShow(true)
                .withTrigger(AbstractTooltip.Trigger.AXIS)
                .withAxisPointer(axisPointer)
                .withFormatterFunction(
                        "function (params) {\n"
                                + "  function pickNumber(v) {\n"
                                + "    if (v == null || v === '') return null;\n"
                                + "    if (typeof v === 'number' && !isNaN(v)) return v;\n"
                                + "    if (typeof v === 'string' && v !== '' && !isNaN(Number(v))) return Number(v);\n"
                                + "    if (Array.isArray(v)) return pickNumber(v[v.length - 1]);\n"
                                + "    if (typeof v === 'object') {\n"
                                + "      if (typeof v.value === 'number') return v.value;\n"
                                + "      if (typeof v.temperature === 'number') return v.temperature;\n"
                                + "      for (var k in v) {\n"
                                + "        if (Object.prototype.hasOwnProperty.call(v, k) && typeof v[k] === 'number') {\n"
                                + "          return v[k];\n"
                                + "        }\n"
                                + "      }\n"
                                + "    }\n"
                                + "    return null;\n"
                                + "  }\n"
                                + "  if (!params || !params.length) return '';\n"
                                + "  var p = params[0];\n"
                                + "  var time = (p.axisValueLabel != null && p.axisValueLabel !== '')\n"
                                + "      ? p.axisValueLabel\n"
                                + "      : (p.axisValue != null ? p.axisValue : (p.name || ''));\n"
                                + "  var val = pickNumber(p.value);\n"
                                + "  if (val == null) val = pickNumber(p.data);\n"
                                + "  if (val == null) val = '';\n"
                                + "  return time + '<br/>' + p.seriesName + ': ' + val + ' °C';\n"
                                + "}");
        temperatureChart.withTooltip(tooltip);
    }

    @Subscribe("refreshBtn")
    public void onRefreshBtnClick(final ClickEvent<?> event) {
        if (sensorId == null) {
            return;
        }
        readingsDl.setParameter("sensor", sensorId);
        readingsDl.load();
    }
}
