package org.apache.poi.ddf;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

public final class EscherBSERecord extends EscherRecord {
   private static final int MAX_RECORD_LENGTH = 100000;
   public static final short RECORD_ID = -4089;
   public static final String RECORD_DESCRIPTION = "MsofbtBSE";
   public static final byte BT_ERROR = 0;
   public static final byte BT_UNKNOWN = 1;
   public static final byte BT_EMF = 2;
   public static final byte BT_WMF = 3;
   public static final byte BT_PICT = 4;
   public static final byte BT_JPEG = 5;
   public static final byte BT_PNG = 6;
   public static final byte BT_DIB = 7;
   private byte field_1_blipTypeWin32;
   private byte field_2_blipTypeMacOS;
   private final byte[] field_3_uid = new byte[16];
   private short field_4_tag;
   private int field_5_size;
   private int field_6_ref;
   private int field_7_offset;
   private byte field_8_usage;
   private byte field_9_name;
   private byte field_10_unused2;
   private byte field_11_unused3;
   private EscherBlipRecord field_12_blipRecord;
   private byte[] _remainingData = new byte[0];

   public EscherBSERecord() {
      this.setRecordId((short)-4089);
   }

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      int pos = offset + 8;
      this.field_1_blipTypeWin32 = data[pos];
      this.field_2_blipTypeMacOS = data[pos + 1];
      System.arraycopy(data, pos + 2, this.field_3_uid, 0, 16);
      this.field_4_tag = LittleEndian.getShort(data, pos + 18);
      this.field_5_size = LittleEndian.getInt(data, pos + 20);
      this.field_6_ref = LittleEndian.getInt(data, pos + 24);
      this.field_7_offset = LittleEndian.getInt(data, pos + 28);
      this.field_8_usage = data[pos + 32];
      this.field_9_name = data[pos + 33];
      this.field_10_unused2 = data[pos + 34];
      this.field_11_unused3 = data[pos + 35];
      bytesRemaining -= 36;
      int bytesRead = 0;
      if (bytesRemaining > 0) {
         this.field_12_blipRecord = (EscherBlipRecord)recordFactory.createRecord(data, pos + 36);
         bytesRead = this.field_12_blipRecord.fillFields(data, pos + 36, recordFactory);
      }

      pos += 36 + bytesRead;
      bytesRemaining -= bytesRead;
      this._remainingData = IOUtils.safelyAllocate(bytesRemaining, 100000);
      System.arraycopy(data, pos, this._remainingData, 0, bytesRemaining);
      return bytesRemaining + 8 + 36 + (this.field_12_blipRecord == null ? 0 : this.field_12_blipRecord.getRecordSize());
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      if (this._remainingData == null) {
         this._remainingData = new byte[0];
      }

      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      int blipSize = this.field_12_blipRecord == null ? 0 : this.field_12_blipRecord.getRecordSize();
      int remainingBytes = this._remainingData.length + 36 + blipSize;
      LittleEndian.putInt(data, offset + 4, remainingBytes);
      data[offset + 8] = this.field_1_blipTypeWin32;
      data[offset + 9] = this.field_2_blipTypeMacOS;
      System.arraycopy(this.field_3_uid, 0, data, offset + 10, 16);
      LittleEndian.putShort(data, offset + 26, this.field_4_tag);
      LittleEndian.putInt(data, offset + 28, this.field_5_size);
      LittleEndian.putInt(data, offset + 32, this.field_6_ref);
      LittleEndian.putInt(data, offset + 36, this.field_7_offset);
      data[offset + 40] = this.field_8_usage;
      data[offset + 41] = this.field_9_name;
      data[offset + 42] = this.field_10_unused2;
      data[offset + 43] = this.field_11_unused3;
      int bytesWritten = 0;
      if (this.field_12_blipRecord != null) {
         bytesWritten = this.field_12_blipRecord.serialize(offset + 44, data, new NullEscherSerializationListener());
      }

      System.arraycopy(this._remainingData, 0, data, offset + 44 + bytesWritten, this._remainingData.length);
      int pos = offset + 8 + 36 + this._remainingData.length + bytesWritten;
      listener.afterRecordSerialize(pos, this.getRecordId(), pos - offset, this);
      return pos - offset;
   }

   public int getRecordSize() {
      int field_12_size = 0;
      if (this.field_12_blipRecord != null) {
         field_12_size = this.field_12_blipRecord.getRecordSize();
      }

      int remaining_size = 0;
      if (this._remainingData != null) {
         remaining_size = this._remainingData.length;
      }

      return 44 + field_12_size + remaining_size;
   }

   public String getRecordName() {
      return "BSE";
   }

   public byte getBlipTypeWin32() {
      return this.field_1_blipTypeWin32;
   }

   public void setBlipTypeWin32(byte blipTypeWin32) {
      this.field_1_blipTypeWin32 = blipTypeWin32;
   }

   public byte getBlipTypeMacOS() {
      return this.field_2_blipTypeMacOS;
   }

   public void setBlipTypeMacOS(byte blipTypeMacOS) {
      this.field_2_blipTypeMacOS = blipTypeMacOS;
   }

   public byte[] getUid() {
      return this.field_3_uid;
   }

   public void setUid(byte[] uid) {
      if (uid != null && uid.length == 16) {
         System.arraycopy(uid, 0, this.field_3_uid, 0, this.field_3_uid.length);
      } else {
         throw new IllegalArgumentException("uid must be byte[16]");
      }
   }

   public short getTag() {
      return this.field_4_tag;
   }

   public void setTag(short tag) {
      this.field_4_tag = tag;
   }

   public int getSize() {
      return this.field_5_size;
   }

   public void setSize(int size) {
      this.field_5_size = size;
   }

   public int getRef() {
      return this.field_6_ref;
   }

   public void setRef(int ref) {
      this.field_6_ref = ref;
   }

   public int getOffset() {
      return this.field_7_offset;
   }

   public void setOffset(int offset) {
      this.field_7_offset = offset;
   }

   public byte getUsage() {
      return this.field_8_usage;
   }

   public void setUsage(byte usage) {
      this.field_8_usage = usage;
   }

   public byte getName() {
      return this.field_9_name;
   }

   public void setName(byte name) {
      this.field_9_name = name;
   }

   public byte getUnused2() {
      return this.field_10_unused2;
   }

   public void setUnused2(byte unused2) {
      this.field_10_unused2 = unused2;
   }

   public byte getUnused3() {
      return this.field_11_unused3;
   }

   public void setUnused3(byte unused3) {
      this.field_11_unused3 = unused3;
   }

   public EscherBlipRecord getBlipRecord() {
      return this.field_12_blipRecord;
   }

   public void setBlipRecord(EscherBlipRecord blipRecord) {
      this.field_12_blipRecord = blipRecord;
   }

   public byte[] getRemainingData() {
      return this._remainingData;
   }

   public void setRemainingData(byte[] remainingData) {
      this._remainingData = remainingData == null ? new byte[0] : (byte[])remainingData.clone();
   }

   public static String getBlipType(byte b) {
      switch (b) {
         case 0:
            return " ERROR";
         case 1:
            return " UNKNOWN";
         case 2:
            return " EMF";
         case 3:
            return " WMF";
         case 4:
            return " PICT";
         case 5:
            return " JPEG";
         case 6:
            return " PNG";
         case 7:
            return " DIB";
         default:
            return b < 32 ? " NotKnown" : " Client";
      }
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{
         {"BlipTypeWin32", this.field_1_blipTypeWin32},
         {"BlipTypeMacOS", this.field_2_blipTypeMacOS},
         {"SUID", this.field_3_uid},
         {"Tag", this.field_4_tag},
         {"Size", this.field_5_size},
         {"Ref", this.field_6_ref},
         {"Offset", this.field_7_offset},
         {"Usage", this.field_8_usage},
         {"Name", this.field_9_name},
         {"Unused2", this.field_10_unused2},
         {"Unused3", this.field_11_unused3},
         {"Blip Record", this.field_12_blipRecord},
         {"Extra Data", this._remainingData}
      };
   }
}
