package org.apache.poi.hssf.record;

import java.util.List;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.NullEscherSerializationListener;
import org.apache.poi.util.LittleEndian;

public final class DrawingGroupRecord extends AbstractEscherHolderRecord {
   public static final short sid = 235;
   static final int MAX_RECORD_SIZE = 8228;
   private static final int MAX_DATA_SIZE = 8224;

   public DrawingGroupRecord() {
   }

   public DrawingGroupRecord(RecordInputStream in) {
      super(in);
   }

   protected String getRecordName() {
      return "MSODRAWINGGROUP";
   }

   public short getSid() {
      return 235;
   }

   public int serialize(int offset, byte[] data) {
      byte[] rawData = this.getRawData();
      if (this.getEscherRecords().size() == 0 && rawData != null) {
         return this.writeData(offset, data, rawData);
      }

      byte[] buffer = new byte[this.getRawDataSize()];
      int pos = 0;

      for (EscherRecord r : this.getEscherRecords()) {
         pos += r.serialize(pos, buffer, new NullEscherSerializationListener());
      }

      return this.writeData(offset, data, buffer);
   }

   public void processChildRecords() {
      this.convertRawBytesToEscherRecords();
   }

   public int getRecordSize() {
      return grossSizeFromDataSize(this.getRawDataSize());
   }

   private int getRawDataSize() {
      List<EscherRecord> escherRecords = this.getEscherRecords();
      byte[] rawData = this.getRawData();
      if (escherRecords.size() == 0 && rawData != null) {
         return rawData.length;
      }

      int size = 0;

      for (EscherRecord r : escherRecords) {
         size += r.getRecordSize();
      }

      return size;
   }

   static int grossSizeFromDataSize(int dataSize) {
      return dataSize + ((dataSize - 1) / 8224 + 1) * 4;
   }

   private int writeData(int offset, byte[] data, byte[] rawData) {
      int writtenActualData = 0;
      int writtenRawData = 0;

      while (writtenRawData < rawData.length) {
         int segmentLength = Math.min(rawData.length - writtenRawData, 8224);
         if (writtenRawData / 8224 >= 2) {
            this.writeContinueHeader(data, offset, segmentLength);
         } else {
            this.writeHeader(data, offset, segmentLength);
         }

         writtenActualData += 4;
         offset += 4;
         System.arraycopy(rawData, writtenRawData, data, offset, segmentLength);
         offset += segmentLength;
         writtenRawData += segmentLength;
         writtenActualData += segmentLength;
      }

      return writtenActualData;
   }

   private void writeHeader(byte[] data, int offset, int sizeExcludingHeader) {
      LittleEndian.putShort(data, 0 + offset, this.getSid());
      LittleEndian.putShort(data, 2 + offset, (short)sizeExcludingHeader);
   }

   private void writeContinueHeader(byte[] data, int offset, int sizeExcludingHeader) {
      LittleEndian.putShort(data, 0 + offset, (short)60);
      LittleEndian.putShort(data, 2 + offset, (short)sizeExcludingHeader);
   }
}
