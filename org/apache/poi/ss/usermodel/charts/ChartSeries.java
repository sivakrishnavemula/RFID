package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ChartSeries {
   void setTitle(String var1);

   void setTitle(CellReference var1);

   String getTitleString();

   CellReference getTitleCellReference();

   TitleType getTitleType();
}
