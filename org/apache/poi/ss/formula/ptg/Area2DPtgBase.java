package org.apache.poi.ss.formula.ptg;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

public abstract class Area2DPtgBase extends AreaPtgBase {
   private static final int SIZE = 9;

   protected Area2DPtgBase(
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

   protected Area2DPtgBase(AreaReference ar) {
      super(ar);
   }

   protected Area2DPtgBase(LittleEndianInput in) {
      this.readCoordinates(in);
   }

   protected abstract byte getSid();

   public final void write(LittleEndianOutput out) {
      out.writeByte(this.getSid() + this.getPtgClass());
      this.writeCoordinates(out);
   }

   public final int getSize() {
      return 9;
   }

   public final String toFormulaString() {
      return this.formatReferenceAsString();
   }

   public final String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(this.getClass().getName());
      sb.append(" [");
      sb.append(this.formatReferenceAsString());
      sb.append("]");
      return sb.toString();
   }
}
