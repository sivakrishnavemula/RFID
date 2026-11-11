package org.apache.poi.ddf;

import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.RecordFormatException;

public class EscherSplitMenuColorsRecord extends EscherRecord {
   public static final short RECORD_ID = -3810;
   public static final String RECORD_DESCRIPTION = "MsofbtSplitMenuColors";
   private int field_1_color1;
   private int field_2_color2;
   private int field_3_color3;
   private int field_4_color4;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      int pos = offset + 8;
      int size = 0;
      this.field_1_color1 = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_2_color2 = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_3_color3 = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_4_color4 = LittleEndian.getInt(data, pos + size);
      size += 4;
      bytesRemaining -= size;
      if (bytesRemaining != 0) {
         throw new RecordFormatException("Expecting no remaining data but got " + bytesRemaining + " byte(s).");
      } else {
         return 8 + size + bytesRemaining;
      }
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      int pos = offset;
      LittleEndian.putShort(data, pos, this.getOptions());
      pos += 2;
      LittleEndian.putShort(data, pos, this.getRecordId());
      pos += 2;
      int remainingBytes = this.getRecordSize() - 8;
      LittleEndian.putInt(data, pos, remainingBytes);
      pos += 4;
      LittleEndian.putInt(data, pos, this.field_1_color1);
      pos += 4;
      LittleEndian.putInt(data, pos, this.field_2_color2);
      pos += 4;
      LittleEndian.putInt(data, pos, this.field_3_color3);
      pos += 4;
      LittleEndian.putInt(data, pos, this.field_4_color4);
      pos += 4;
      listener.afterRecordSerialize(pos, this.getRecordId(), pos - offset, this);
      return this.getRecordSize();
   }

   public int getRecordSize() {
      return 24;
   }

   public short getRecordId() {
      return -3810;
   }

   public String getRecordName() {
      return "SplitMenuColors";
   }

   public int getColor1() {
      return this.field_1_color1;
   }

   public void setColor1(int field_1_color1) {
      this.field_1_color1 = field_1_color1;
   }

   public int getColor2() {
      return this.field_2_color2;
   }

   public void setColor2(int field_2_color2) {
      this.field_2_color2 = field_2_color2;
   }

   public int getColor3() {
      return this.field_3_color3;
   }

   public void setColor3(int field_3_color3) {
      this.field_3_color3 = field_3_color3;
   }

   public int getColor4() {
      return this.field_4_color4;
   }

   public void setColor4(int field_4_color4) {
      this.field_4_color4 = field_4_color4;
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{{"Color1", this.field_1_color1}, {"Color2", this.field_2_color2}, {"Color3", this.field_3_color3}, {"Color4", this.field_4_color4}};
   }
}
