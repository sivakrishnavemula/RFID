package org.apache.poi.ss.formula.ptg;

import org.apache.poi.util.LittleEndianOutput;

public final class ParenthesisPtg extends ControlPtg {
   private static final int SIZE = 1;
   public static final byte sid = 21;
   public static final ControlPtg instance = new ParenthesisPtg();

   private ParenthesisPtg() {
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(21 + this.getPtgClass());
   }

   public int getSize() {
      return 1;
   }

   public String toFormulaString() {
      return "()";
   }

   public String toFormulaString(String[] operands) {
      return "(" + operands[0] + ")";
   }
}
