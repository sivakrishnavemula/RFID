package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class MMSRecord extends StandardRecord {
   public static final short sid = 193;
   private byte field_1_addMenuCount;
   private byte field_2_delMenuCount;

   public MMSRecord() {
   }

   public MMSRecord(RecordInputStream in) {
      if (in.remaining() != 0) {
         this.field_1_addMenuCount = in.readByte();
         this.field_2_delMenuCount = in.readByte();
      }
   }

   public void setAddMenuCount(byte am) {
      this.field_1_addMenuCount = am;
   }

   public void setDelMenuCount(byte dm) {
      this.field_2_delMenuCount = dm;
   }

   public byte getAddMenuCount() {
      return this.field_1_addMenuCount;
   }

   public byte getDelMenuCount() {
      return this.field_2_delMenuCount;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[MMS]\n");
      buffer.append("    .addMenu        = ").append(Integer.toHexString(this.getAddMenuCount())).append("\n");
      buffer.append("    .delMenu        = ").append(Integer.toHexString(this.getDelMenuCount())).append("\n");
      buffer.append("[/MMS]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeByte(this.getAddMenuCount());
      out.writeByte(this.getDelMenuCount());
   }

   protected int getDataSize() {
      return 2;
   }

   public short getSid() {
      return 193;
   }
}
