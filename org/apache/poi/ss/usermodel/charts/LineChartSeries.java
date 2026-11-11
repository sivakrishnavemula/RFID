package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface LineChartSeries extends ChartSeries {
   ChartDataSource<?> getCategoryAxisData();

   ChartDataSource<? extends Number> getValues();
}
