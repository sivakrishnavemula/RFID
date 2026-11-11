package org.apache.poi.ss.usermodel.charts;

import java.util.List;
import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ScatterChartData extends ChartData {
   ScatterChartSeries addSerie(ChartDataSource<?> var1, ChartDataSource<? extends Number> var2);

   List<? extends ScatterChartSeries> getSeries();
}
