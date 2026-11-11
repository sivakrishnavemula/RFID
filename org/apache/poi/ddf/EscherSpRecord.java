package org.apache.poi.ddf;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;

public class EscherSpRecord extends EscherRecord {
   public static final short RECORD_ID = -4086;
   public static final String RECORD_DESCRIPTION = "MsofbtSp";
   public static final int FLAG_GROUP = 1;
   public static final int FLAG_CHILD = 2;
   public static final int FLAG_PATRIARCH = 4;
   public static final int FLAG_DELETED = 8;
   public static final int FLAG_OLESHAPE = 16;
   public static final int FLAG_HAVEMASTER = 32;
   public static final int FLAG_FLIPHORIZ = 64;
   public static final int FLAG_FLIPVERT = 128;
   public static final int FLAG_CONNECTOR = 256;
   public static final int FLAG_HAVEANCHOR = 512;
   public static final int FLAG_BACKGROUND = 1024;
   public static final int FLAG_HASSHAPETYPE = 2048;
   private int field_1_shapeId;
   private int field_2_flags;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      this.readHeader(data, offset);
      int pos = offset + 8;
      int size = 0;
      this.field_1_shapeId = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_2_flags = LittleEndian.getInt(data, pos + size);
      size += 4;
      return this.getRecordSize();
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      int remainingBytes = 8;
      LittleEndian.putInt(data, offset + 4, remainingBytes);
      LittleEndian.putInt(data, offset + 8, this.field_1_shapeId);
      LittleEndian.putInt(data, offset + 12, this.field_2_flags);
      listener.afterRecordSerialize(offset + this.getRecordSize(), this.getRecordId(), this.getRecordSize(), this);
      return 16;
   }

   public int getRecordSize() {
      return 16;
   }

   public short getRecordId() {
      return -4086;
   }

   public String getRecordName() {
      return "Sp";
   }

   private String decodeFlags(int flags) {
      StringBuffer result = new StringBuffer();
      result.append((flags & 1) != 0 ? "|GROUP" : "");
      result.append((flags & 2) != 0 ? "|CHILD" : "");
      result.append((flags & 4) != 0 ? "|PATRIARCH" : "");
      result.append((flags & 8) != 0 ? "|DELETED" : "");
      result.append((flags & 16) != 0 ? "|OLESHAPE" : "");
      result.append((flags & 32) != 0 ? "|HAVEMASTER" : "");
      result.append((flags & 64) != 0 ? "|FLIPHORIZ" : "");
      result.append((flags & 128) != 0 ? "|FLIPVERT" : "");
      result.append((flags & 256) != 0 ? "|CONNECTOR" : "");
      result.append((flags & 512) != 0 ? "|HAVEANCHOR" : "");
      result.append((flags & 1024) != 0 ? "|BACKGROUND" : "");
      result.append((flags & 2048) != 0 ? "|HASSHAPETYPE" : "");
      if (result.length() > 0) {
         result.deleteCharAt(0);
      }

      return result.toString();
   }

   public int getShapeId() {
      return this.field_1_shapeId;
   }

   public void setShapeId(int field_1_shapeId) {
      this.field_1_shapeId = field_1_shapeId;
   }

   public int getFlags() {
      return this.field_2_flags;
   }

   public void setFlags(int field_2_flags) {
      this.field_2_flags = field_2_flags;
   }

   public short getShapeType() {
      return this.getInstance();
   }

   public void setShapeType(short value) {
      this.setInstance(value);
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{
         {"ShapeType", this.getShapeType()},
         {"ShapeId", this.field_1_shapeId},
         {"Flags", this.decodeFlags(this.field_2_flags) + " (0x" + HexDump.toHex(this.field_2_flags) + ")"}
      };
   }
}
