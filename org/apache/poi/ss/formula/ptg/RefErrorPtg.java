package org.apache.poi.ss.formula.ptg;

import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

public final class RefErrorPtg extends OperandPtg {
   private static final int SIZE = 5;
   public static final byte sid = 42;
   private int field_1_reserved;

   public RefErrorPtg() {
      this.field_1_reserved = 0;
   }

   public RefErrorPtg(LittleEndianInput in) {
      this.field_1_reserved = in.readInt();
   }

   public String toString() {
      return this.getClass().getName();
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(42 + this.getPtgClass());
      out.writeInt(this.field_1_reserved);
   }

   public int getSize() {
      return 5;
   }

   public String toFormulaString() {
      return FormulaError.REF.getString();
   }

   public byte getDefaultOperandClass() {
      return 0;
   }
}
