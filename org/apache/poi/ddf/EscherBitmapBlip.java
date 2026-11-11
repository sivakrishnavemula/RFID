package org.apache.poi.ddf;

import org.apache.poi.util.LittleEndian;

public class EscherBitmapBlip extends EscherBlipRecord {
   public static final short RECORD_ID_JPEG = -4067;
   public static final short RECORD_ID_PNG = -4066;
   public static final short RECORD_ID_DIB = -4065;
   private static final int HEADER_SIZE = 8;
   private final byte[] field_1_UID = new byte[16];
   private byte field_2_marker = -1;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesAfterHeader = this.readHeader(data, offset);
      int pos = offset + 8;
      System.arraycopy(data, pos, this.field_1_UID, 0, 16);
      pos += 16;
      this.field_2_marker = data[pos];
      this.setPictureData(data, ++pos, bytesAfterHeader - 17);
      return bytesAfterHeader + 8;
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      LittleEndian.putInt(data, offset + 4, this.getRecordSize() - 8);
      int pos = offset + 8;
      System.arraycopy(this.field_1_UID, 0, data, pos, 16);
      data[pos + 16] = this.field_2_marker;
      byte[] pd = this.getPicturedata();
      System.arraycopy(pd, 0, data, pos + 17, pd.length);
      listener.afterRecordSerialize(offset + this.getRecordSize(), this.getRecordId(), this.getRecordSize(), this);
      return 25 + pd.length;
   }

   public int getRecordSize() {
      return 25 + this.getPicturedata().length;
   }

   public byte[] getUID() {
      return this.field_1_UID;
   }

   public void setUID(byte[] field_1_UID) {
      if (field_1_UID != null && field_1_UID.length == 16) {
         System.arraycopy(field_1_UID, 0, this.field_1_UID, 0, 16);
      } else {
         throw new IllegalArgumentException("field_1_UID must be byte[16]");
      }
   }

   public byte getMarker() {
      return this.field_2_marker;
   }

   public void setMarker(byte field_2_marker) {
      this.field_2_marker = field_2_marker;
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{{"Marker", this.field_2_marker}, {"Extra Data", this.getPicturedata()}};
   }
}
