package org.apache.poi.ddf;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.RecordFormatException;

public final class EscherTextboxRecord extends EscherRecord implements Cloneable {
   private static final int MAX_RECORD_LENGTH = 100000;
   public static final short RECORD_ID = -4083;
   public static final String RECORD_DESCRIPTION = "msofbtClientTextbox";
   private static final byte[] NO_BYTES = new byte[0];
   private byte[] thedata = NO_BYTES;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      this.thedata = IOUtils.safelyAllocate(bytesRemaining, 100000);
      System.arraycopy(data, offset + 8, this.thedata, 0, bytesRemaining);
      return bytesRemaining + 8;
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      int remainingBytes = this.thedata.length;
      LittleEndian.putInt(data, offset + 4, remainingBytes);
      System.arraycopy(this.thedata, 0, data, offset + 8, this.thedata.length);
      int pos = offset + 8 + this.thedata.length;
      listener.afterRecordSerialize(pos, this.getRecordId(), pos - offset, this);
      int size = pos - offset;
      if (size != this.getRecordSize()) {
         throw new RecordFormatException(size + " bytes written but getRecordSize() reports " + this.getRecordSize());
      } else {
         return size;
      }
   }

   public byte[] getData() {
      return this.thedata;
   }

   public void setData(byte[] b, int start, int length) {
      this.thedata = IOUtils.safelyAllocate(length, 100000);
      System.arraycopy(b, start, this.thedata, 0, length);
   }

   public void setData(byte[] b) {
      this.setData(b, 0, b.length);
   }

   public int getRecordSize() {
      return 8 + this.thedata.length;
   }

   public EscherTextboxRecord clone() {
      EscherTextboxRecord etr = new EscherTextboxRecord();
      etr.setOptions(this.getOptions());
      etr.setRecordId(this.getRecordId());
      etr.thedata = (byte[])this.thedata.clone();
      return etr;
   }

   public String getRecordName() {
      return "ClientTextbox";
   }

   protected Object[][] getAttributeMap() {
      int numCh = this.getChildRecords().size();
      List<Object> chLst = new ArrayList(numCh * 2 + 2);
      chLst.add("children");
      chLst.add(numCh);

      for (EscherRecord er : this.getChildRecords()) {
         chLst.add(er.getRecordName());
         chLst.add(er);
      }

      return new Object[][]{{"isContainer", this.isContainerRecord()}, chLst.toArray(), {"Extra Data", this.thedata}};
   }
}
