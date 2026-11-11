package org.apache.poi.ss.formula.ptg;

import org.apache.poi.util.LittleEndianOutput;

public final class MissingArgPtg extends ScalarConstantPtg {
   private static final int SIZE = 1;
   public static final byte sid = 22;
   public static final Ptg instance = new MissingArgPtg();

   private MissingArgPtg() {
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(22 + this.getPtgClass());
   }

   public int getSize() {
      return 1;
   }

   public String toFormulaString() {
      return " ";
   }
}
