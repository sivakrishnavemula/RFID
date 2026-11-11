package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ChartAxisFactory {
   ValueAxis createValueAxis(AxisPosition var1);

   ChartAxis createCategoryAxis(AxisPosition var1);

   ChartAxis createDateAxis(AxisPosition var1);
}
