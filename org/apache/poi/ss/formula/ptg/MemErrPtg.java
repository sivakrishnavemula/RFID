package org.apache.poi.ss.formula.ptg;

import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

public final class MemErrPtg extends OperandPtg {
   public static final short sid = 39;
   private static final int SIZE = 7;
   private int field_1_reserved;
   private short field_2_subex_len;

   public MemErrPtg(LittleEndianInput in) {
      this.field_1_reserved = in.readInt();
      this.field_2_subex_len = in.readShort();
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(39 + this.getPtgClass());
      out.writeInt(this.field_1_reserved);
      out.writeShort(this.field_2_subex_len);
   }

   public int getSize() {
      return 7;
   }

   public String toFormulaString() {
      return "ERR#";
   }

   public byte getDefaultOperandClass() {
      return 32;
   }
}
