package org.apache.poi.ddf;

import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.RecordFormatException;

public class EscherSpgrRecord extends EscherRecord {
   public static final short RECORD_ID = -4087;
   public static final String RECORD_DESCRIPTION = "MsofbtSpgr";
   private int field_1_rectX1;
   private int field_2_rectY1;
   private int field_3_rectX2;
   private int field_4_rectY2;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      int pos = offset + 8;
      int size = 0;
      this.field_1_rectX1 = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_2_rectY1 = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_3_rectX2 = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_4_rectY2 = LittleEndian.getInt(data, pos + size);
      size += 4;
      bytesRemaining -= size;
      if (bytesRemaining != 0) {
         throw new RecordFormatException("Expected no remaining bytes but got " + bytesRemaining);
      } else {
         return 8 + size + bytesRemaining;
      }
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      int remainingBytes = 16;
      LittleEndian.putInt(data, offset + 4, remainingBytes);
      LittleEndian.putInt(data, offset + 8, this.field_1_rectX1);
      LittleEndian.putInt(data, offset + 12, this.field_2_rectY1);
      LittleEndian.putInt(data, offset + 16, this.field_3_rectX2);
      LittleEndian.putInt(data, offset + 20, this.field_4_rectY2);
      listener.afterRecordSerialize(offset + this.getRecordSize(), this.getRecordId(), offset + this.getRecordSize(), this);
      return 24;
   }

   public int getRecordSize() {
      return 24;
   }

   public short getRecordId() {
      return -4087;
   }

   public String getRecordName() {
      return "Spgr";
   }

   public int getRectX1() {
      return this.field_1_rectX1;
   }

   public void setRectX1(int x1) {
      this.field_1_rectX1 = x1;
   }

   public int getRectY1() {
      return this.field_2_rectY1;
   }

   public void setRectY1(int y1) {
      this.field_2_rectY1 = y1;
   }

   public int getRectX2() {
      return this.field_3_rectX2;
   }

   public void setRectX2(int x2) {
      this.field_3_rectX2 = x2;
   }

   public int getRectY2() {
      return this.field_4_rectY2;
   }

   public void setRectY2(int rectY2) {
      this.field_4_rectY2 = rectY2;
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{
         {"RectX", this.field_1_rectX1}, {"RectY", this.field_2_rectY1}, {"RectWidth", this.field_3_rectX2}, {"RectHeight", this.field_4_rectY2}
      };
   }
}
