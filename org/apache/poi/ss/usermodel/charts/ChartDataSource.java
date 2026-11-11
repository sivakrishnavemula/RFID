package org.apache.poi.ss.usermodel.charts;

import org.apache.poi.util.Removal;

@Deprecated
@Removal(version = "4.2")
public interface ChartDataSource<T> {
   int getPointCount();

   T getPointAt(int var1);

   boolean isReference();

   boolean isNumeric();

   String getFormulaString();
}
