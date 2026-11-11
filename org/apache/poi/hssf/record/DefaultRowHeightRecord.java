package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianOutput;

public final class DefaultRowHeightRecord extends StandardRecord implements Cloneable {
   public static final short sid = 549;
   private short field_1_option_flags;
   private short field_2_row_height;
   public static final short DEFAULT_ROW_HEIGHT = 255;

   public DefaultRowHeightRecord() {
      this.field_1_option_flags = 0;
      this.field_2_row_height = 255;
   }

   public DefaultRowHeightRecord(RecordInputStream in) {
      this.field_1_option_flags = in.readShort();
      this.field_2_row_height = in.readShort();
   }

   public void setOptionFlags(short flags) {
      this.field_1_option_flags = flags;
   }

   public void setRowHeight(short height) {
      this.field_2_row_height = height;
   }

   public short getOptionFlags() {
      return this.field_1_option_flags;
   }

   public short getRowHeight() {
      return this.field_2_row_height;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[DEFAULTROWHEIGHT]\n");
      buffer.append("    .optionflags    = ").append(Integer.toHexString(this.getOptionFlags())).append("\n");
      buffer.append("    .rowheight      = ").append(Integer.toHexString(this.getRowHeight())).append("\n");
      buffer.append("[/DEFAULTROWHEIGHT]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.getOptionFlags());
      out.writeShort(this.getRowHeight());
   }

   protected int getDataSize() {
      return 4;
   }

   public short getSid() {
      return 549;
   }

   public DefaultRowHeightRecord clone() {
      DefaultRowHeightRecord rec = new DefaultRowHeightRecord();
      rec.field_1_option_flags = this.field_1_option_flags;
      rec.field_2_row_height = this.field_2_row_height;
      return rec;
   }
}
