package org.apache.poi.ss.formula.ptg;

public final class SubtractPtg extends ValueOperatorPtg {
   public static final byte sid = 4;
   public static final ValueOperatorPtg instance = new SubtractPtg();

   private SubtractPtg() {
   }

   protected byte getSid() {
      return 4;
   }

   public int getNumberOfOperands() {
      return 2;
   }

   public String toFormulaString(String[] operands) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(operands[0]);
      buffer.append("-");
      buffer.append(operands[1]);
      return buffer.toString();
   }
}
