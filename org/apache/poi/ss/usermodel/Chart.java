package org.apache.poi.ss.usermodel;

import java.util.List;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.ss.usermodel.charts.ChartAxisFactory;
import org.apache.poi.ss.usermodel.charts.ChartData;
import org.apache.poi.ss.usermodel.charts.ChartDataFactory;
import org.apache.poi.ss.usermodel.charts.ChartLegend;
import org.apache.poi.ss.usermodel.charts.ManuallyPositionable;
import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface Chart extends ManuallyPositionable {
   ChartDataFactory getChartDataFactory();

   ChartAxisFactory getChartAxisFactory();

   ChartLegend getOrCreateLegend();

   void deleteLegend();

   List<? extends ChartAxis> getAxis();

   void plot(ChartData var1, ChartAxis... var2);
}
