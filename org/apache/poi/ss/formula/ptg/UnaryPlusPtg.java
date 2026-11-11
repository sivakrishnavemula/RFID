package org.apache.poi.ss.formula.ptg;

public final class UnaryPlusPtg extends ValueOperatorPtg {
   public static final byte sid = 18;
   private static final String ADD = "+";
   public static final ValueOperatorPtg instance = new UnaryPlusPtg();

   private UnaryPlusPtg() {
   }

   protected byte getSid() {
      return 18;
   }

   public int getNumberOfOperands() {
      return 1;
   }

   public String toFormulaString(String[] operands) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("+");
      buffer.append(operands[0]);
      return buffer.toString();
   }
}
