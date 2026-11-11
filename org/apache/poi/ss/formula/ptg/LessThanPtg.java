package org.apache.poi.ss.formula.ptg;

public final class LessThanPtg extends ValueOperatorPtg {
   public static final byte sid = 9;
   private static final String LESSTHAN = "<";
   public static final ValueOperatorPtg instance = new LessThanPtg();

   private LessThanPtg() {
   }

   protected byte getSid() {
      return 9;
   }

   public int getNumberOfOperands() {
      return 2;
   }

   public String toFormulaString(String[] operands) {
      StringBuffer buffer = new StringBuffer();
      buffer.append(operands[0]);
      buffer.append("<");
      buffer.append(operands[1]);
      return buffer.toString();
   }
}
