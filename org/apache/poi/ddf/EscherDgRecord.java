package org.apache.poi.ddf;

import org.apache.poi.util.LittleEndian;

public class EscherDgRecord extends EscherRecord {
   public static final short RECORD_ID = -4088;
   public static final String RECORD_DESCRIPTION = "MsofbtDg";
   private int field_1_numShapes;
   private int field_2_lastMSOSPID;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      this.readHeader(data, offset);
      int pos = offset + 8;
      int size = 0;
      this.field_1_numShapes = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_2_lastMSOSPID = LittleEndian.getInt(data, pos + size);
      size += 4;
      return this.getRecordSize();
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      LittleEndian.putInt(data, offset + 4, 8);
      LittleEndian.putInt(data, offset + 8, this.field_1_numShapes);
      LittleEndian.putInt(data, offset + 12, this.field_2_lastMSOSPID);
      listener.afterRecordSerialize(offset + 16, this.getRecordId(), this.getRecordSize(), this);
      return this.getRecordSize();
   }

   public int getRecordSize() {
      return 16;
   }

   public short getRecordId() {
      return -4088;
   }

   public String getRecordName() {
      return "Dg";
   }

   public int getNumShapes() {
      return this.field_1_numShapes;
   }

   public void setNumShapes(int field_1_numShapes) {
      this.field_1_numShapes = field_1_numShapes;
   }

   public int getLastMSOSPID() {
      return this.field_2_lastMSOSPID;
   }

   public void setLastMSOSPID(int field_2_lastMSOSPID) {
      this.field_2_lastMSOSPID = field_2_lastMSOSPID;
   }

   public short getDrawingGroupId() {
      return (short)(this.getOptions() >> 4);
   }

   public void incrementShapeCount() {
      this.field_1_numShapes++;
   }

   protected Object[][] getAttributeMap() {
      return new Object[][]{{"NumShapes", this.field_1_numShapes}, {"LastMSOSPID", this.field_2_lastMSOSPID}};
   }
}
