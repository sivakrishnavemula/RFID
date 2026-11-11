package org.apache.poi.ss.formula.ptg;

import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

public final class BoolPtg extends ScalarConstantPtg {
   public static final int SIZE = 2;
   public static final byte sid = 29;
   private static final BoolPtg FALSE = new BoolPtg(false);
   private static final BoolPtg TRUE = new BoolPtg(true);
   private final boolean _value;

   private BoolPtg(boolean b) {
      this._value = b;
   }

   public static BoolPtg valueOf(boolean b) {
      return b ? TRUE : FALSE;
   }

   public static BoolPtg read(LittleEndianInput in) {
      return valueOf(in.readByte() == 1);
   }

   public boolean getValue() {
      return this._value;
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(29 + this.getPtgClass());
      out.writeByte(this._value ? 1 : 0);
   }

   public int getSize() {
      return 2;
   }

   public String toFormulaString() {
      return this._value ? "TRUE" : "FALSE";
   }
}
