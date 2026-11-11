package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ChartLegend extends ManuallyPositionable {
   LegendPosition getPosition();

   void setPosition(LegendPosition var1);

   boolean isOverlay();

   void setOverlay(boolean var1);
}
