package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ValueAxis extends ChartAxis {
   AxisCrossBetween getCrossBetween();

   void setCrossBetween(AxisCrossBetween var1);
}
