package org.apache.poi.ddf;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

public class EscherClientDataRecord extends EscherRecord {
   private static final int MAX_RECORD_LENGTH = 100000;
   public static final short RECORD_ID = -4079;
   public static final String RECORD_DESCRIPTION = "MsofbtClientData";
   private byte[] remainingData;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      int pos = offset + 8;
      this.remainingData = IOUtils.safelyAllocate(bytesRemaining, 100000);
      System.arraycopy(data, pos, this.remainingData, 0, bytesRemaining);
      return 8 + bytesRemaining;
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      if (this.remainingData == null) {
         this.remainingData = new byte[0];
      }

      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      LittleEndian.putInt(data, offset + 4, this.remainingData.length);
      System.arraycopy(this.remainingData, 0, data, offset + 8, this.remainingData.length);
      int pos = offset + 8 + this.remainingData.length;
      listener.afterRecordSerialize(pos, this.getRecordId(), pos - offset, this);
      return pos - offset;
   }

   public int getRecordSize() {
      return 8 + (this.remainingData == null ? 0 : this.remainingData.length);
   }

   public short getRecordId() {
      return -4079;
   }

   public String getRecordName() {
      return "ClientData";
   }

   public byte[] getRemainingData() {
      return this.remainingData;
   }

   public void setRemainingData(byte[] remainingData) {
      this.remainingData = remainingData == null ? new byte[0] : (byte[])remainingData.clone();
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{{"Extra Data", this.getRemainingData()}};
   }
}
