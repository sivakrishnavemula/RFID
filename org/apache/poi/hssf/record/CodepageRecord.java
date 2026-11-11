package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class CodepageRecord extends StandardRecord {
   public static final short sid = 66;
   private short field_1_codepage;
   public static final short CODEPAGE = 1200;

   public CodepageRecord() {
   }

   public CodepageRecord(RecordInputStream in) {
      this.field_1_codepage = in.readShort();
   }

   public void setCodepage(short cp) {
      this.field_1_codepage = cp;
   }

   public short getCodepage() {
      return this.field_1_codepage;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[CODEPAGE]\n");
      buffer.append("    .codepage        = ").append(Integer.toHexString(this.getCodepage())).append("\n");
      buffer.append("[/CODEPAGE]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.getCodepage());
   }

   protected int getDataSize() {
      return 2;
   }

   public short getSid() {
      return 66;
   }
}
