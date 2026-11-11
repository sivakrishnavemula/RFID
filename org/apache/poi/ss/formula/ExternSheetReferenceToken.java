package org.apache.poi.ss.formula;

public interface ExternSheetReferenceToken {
   int getExternSheetIndex();

   String format2DRefAsString();
}
