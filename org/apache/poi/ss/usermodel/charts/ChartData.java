package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ChartData {
   void fillChart(Chart var1, ChartAxis... var2);
}
