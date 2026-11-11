package org.apache.poi.ss.formula.ptg;

public interface Pxg {
   int getExternalWorkbookNumber();

   String getSheetName();

   void setSheetName(String var1);

   String toFormulaString();
}
