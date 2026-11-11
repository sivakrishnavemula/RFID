package org.apache.poi.ddf;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

public class EscherBlipRecord extends EscherRecord {
   private static final int MAX_RECORD_LENGTH = 104857600;
   public static final short RECORD_ID_START = -4072;
   public static final short RECORD_ID_END = -3817;
   public static final String RECORD_DESCRIPTION = "msofbtBlip";
   private static final int HEADER_SIZE = 8;
   private byte[] field_pictureData;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesAfterHeader = this.readHeader(data, offset);
      int pos = offset + 8;
      this.field_pictureData = IOUtils.safelyAllocate(bytesAfterHeader, 104857600);
      System.arraycopy(data, pos, this.field_pictureData, 0, bytesAfterHeader);
      return bytesAfterHeader + 8;
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      System.arraycopy(this.field_pictureData, 0, data, offset + 4, this.field_pictureData.length);
      listener.afterRecordSerialize(offset + 4 + this.field_pictureData.length, this.getRecordId(), this.field_pictureData.length + 4, this);
      return this.field_pictureData.length + 4;
   }

   public int getRecordSize() {
      return this.field_pictureData.length + 8;
   }

   public String getRecordName() {
      return "Blip";
   }

   public byte[] getPicturedata() {
      return this.field_pictureData;
   }

   public void setPictureData(byte[] pictureData) {
      this.setPictureData(pictureData, 0, pictureData == null ? 0 : pictureData.length);
   }

   public void setPictureData(byte[] pictureData, int offset, int length) {
      if (pictureData != null && offset >= 0 && length >= 0 && pictureData.length >= offset + length) {
         this.field_pictureData = IOUtils.safelyAllocate(length, 104857600);
         System.arraycopy(pictureData, offset, this.field_pictureData, 0, length);
      } else {
         throw new IllegalArgumentException("picture data can't be null");
      }
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{{"Extra Data", this.getPicturedata()}};
   }
}
