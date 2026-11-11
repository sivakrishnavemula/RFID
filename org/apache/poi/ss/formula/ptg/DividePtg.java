package org.apache.poi.ss.formula.ptg;

public final class DividePtg extends ValueOperatorPtg {
   public static final byte sid = 6;
   public static final ValueOperatorPtg instance = new DividePtg();

   private DividePtg() {
   }

   protected byte getSid() {
      return 6;
   }

   public int getNumberOfOperands() {
      return 2;
   }

   public String toFormulaString(String[] operands) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(operands[0]);
      buffer.append("/");
      buffer.append(operands[1]);
      return buffer.toString();
   }
}
