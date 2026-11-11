package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ScatterChartSeries extends ChartSeries {
   ChartDataSource<?> getXValues();

   ChartDataSource<? extends Number> getYValues();
}
