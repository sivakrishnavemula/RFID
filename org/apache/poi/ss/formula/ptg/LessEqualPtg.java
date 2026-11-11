package org.apache.poi.ss.formula.ptg;

public final class LessEqualPtg extends ValueOperatorPtg {
   public static final byte sid = 10;
   public static final ValueOperatorPtg instance = new LessEqualPtg();

   private LessEqualPtg() {
   }

   protected byte getSid() {
      return 10;
   }

   public int getNumberOfOperands() {
      return 2;
   }

   public String toFormulaString(String[] operands) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(operands[0]);
      buffer.append("<=");
      buffer.append(operands[1]);
      return buffer.toString();
   }
}
