package org.apache.poi.ss.formula.eval;

public final class BoolEval implements NumericValueEval, StringValueEval {
   private boolean _value;
   public static final BoolEval FALSE = new BoolEval(false);
   public static final BoolEval TRUE = new BoolEval(true);

   public static BoolEval valueOf(boolean b) {
      return b ? TRUE : FALSE;
   }

   private BoolEval(boolean value) {
      this._value = value;
   }

   public boolean getBooleanValue() {
      return this._value;
   }

   public double getNumberValue() {
      return this._value ? 1.0 : 0.0;
   }

   public String getStringValue() {
      return this._value ? "TRUE" : "FALSE";
   }

   public String toString() {
      return this.getClass().getName() + " [" + this.getStringValue() + "]";
   }
}
