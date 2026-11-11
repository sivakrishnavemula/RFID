package org.apache.poi.ss.formula.ptg;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.util.LittleEndianInput;

public final class AreaPtg extends Area2DPtgBase {
   public static final short sid = 37;

   public AreaPtg(
      int firstRow,
      int lastRow,
      int firstColumn,
      int lastColumn,
      boolean firstRowRelative,
      boolean lastRowRelative,
      boolean firstColRelative,
      boolean lastColRelative
   ) {
      super(firstRow, lastRow, firstColumn, lastColumn, firstRowRelative, lastRowRelative, firstColRelative, lastColRelative);
   }

   public AreaPtg(LittleEndianInput in) {
      super(in);
   }

   public AreaPtg(AreaReference arearef) {
      super(arearef);
   }

   protected byte getSid() {
      return 37;
   }
}
