package org.apache.poi.ss.formula.ptg;

import org.apache.poi.util.LittleEndianOutput;

public class UnknownPtg extends Ptg {
   private short size = 1;
   private final int _sid;

   public UnknownPtg(int sid) {
      this._sid = sid;
   }

   public boolean isBaseToken() {
      return true;
   }

   public void write(LittleEndianOutput out) {
      out.writeByte(this._sid);
   }

   public int getSize() {
      return this.size;
   }

   public String toFormulaString() {
      return "UNKNOWN";
   }

   public byte getDefaultOperandClass() {
      return 32;
   }
}
