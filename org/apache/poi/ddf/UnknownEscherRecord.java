package org.apache.poi.ddf;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

public final class UnknownEscherRecord extends EscherRecord implements Cloneable {
   private static final int MAX_RECORD_LENGTH = 100000000;
   private static final byte[] NO_BYTES = new byte[0];
   private byte[] thedata = NO_BYTES;
   private List<EscherRecord> _childRecords = new ArrayList();

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      int available = data.length - (offset + 8);
      if (bytesRemaining > available) {
         bytesRemaining = available;
      }

      if (!this.isContainerRecord()) {
         if (bytesRemaining < 0) {
            bytesRemaining = 0;
         }

         this.thedata = IOUtils.safelyAllocate(bytesRemaining, 100000000);
         System.arraycopy(data, offset + 8, this.thedata, 0, bytesRemaining);
         return bytesRemaining + 8;
      } else {
         int bytesWritten = 0;
         this.thedata = new byte[0];
         offset += 8;
         bytesWritten += 8;

         while (bytesRemaining > 0) {
            EscherRecord child = recordFactory.createRecord(data, offset);
            int childBytesWritten = child.fillFields(data, offset, recordFactory);
            bytesWritten += childBytesWritten;
            offset += childBytesWritten;
            bytesRemaining -= childBytesWritten;
            this.getChildRecords().add(child);
         }

         return bytesWritten;
      }
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      int remainingBytes = this.thedata.length;

      for (EscherRecord r : this._childRecords) {
         remainingBytes += r.getRecordSize();
      }

      LittleEndian.putInt(data, offset + 4, remainingBytes);
      System.arraycopy(this.thedata, 0, data, offset + 8, this.thedata.length);
      int pos = offset + 8 + this.thedata.length;

      for (EscherRecord r : this._childRecords) {
         pos += r.serialize(pos, data, listener);
      }

      listener.afterRecordSerialize(pos, this.getRecordId(), pos - offset, this);
      return pos - offset;
   }

   public byte[] getData() {
      return this.thedata;
   }

   public int getRecordSize() {
      return 8 + this.thedata.length;
   }

   public List<EscherRecord> getChildRecords() {
      return this._childRecords;
   }

   public void setChildRecords(List<EscherRecord> childRecords) {
      this._childRecords = childRecords;
   }

   public UnknownEscherRecord clone() {
      UnknownEscherRecord uer = new UnknownEscherRecord();
      uer.thedata = (byte[])this.thedata.clone();
      uer.setOptions(this.getOptions());
      uer.setRecordId(this.getRecordId());
      return uer;
   }

   public String getRecordName() {
      return "Unknown 0x" + HexDump.toHex(this.getRecordId());
   }

   public void addChildRecord(EscherRecord childRecord) {
      this.getChildRecords().add(childRecord);
   }

   protected Object[][] getAttributeMap() {
      int numCh = this.getChildRecords().size();
      List<Object> chLst = new ArrayList(numCh * 2 + 2);
      chLst.add("children");
      chLst.add(numCh);

      for (EscherRecord er : this._childRecords) {
         chLst.add(er.getRecordName());
         chLst.add(er);
      }

      return new Object[][]{{"isContainer", this.isContainerRecord()}, chLst.toArray(), {"Extra Data", this.thedata}};
   }
}
