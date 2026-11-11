package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ChartDataFactory {
   ScatterChartData createScatterChartData();

   LineChartData createLineChartData();
}
