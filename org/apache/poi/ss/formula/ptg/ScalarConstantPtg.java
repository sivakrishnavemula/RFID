package org.apache.poi.ss.formula.ptg;

public abstract class ScalarConstantPtg extends Ptg {
   public final boolean isBaseToken() {
      return true;
   }

   public final byte getDefaultOperandClass() {
      return 32;
   }

   public final String toString() {
      StringBuffer sb = new StringBuffer(64);
      sb.append(this.getClass().getName()).append(" [");
      sb.append(this.toFormulaString());
      sb.append("]");
      return sb.toString();
   }
}
