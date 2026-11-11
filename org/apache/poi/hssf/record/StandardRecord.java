package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianByteArrayOutputStream;
import org.apache.poi.util.LittleEndianOutput;

public abstract class StandardRecord extends Record {
   protected abstract int getDataSize();

   public final int getRecordSize() {
      return 4 + this.getDataSize();
   }

   public final int serialize(int offset, byte[] data) {
      int dataSize = this.getDataSize();
      int recSize = 4 + dataSize;
      LittleEndianByteArrayOutputStream out = new LittleEndianByteArrayOutputStream(data, offset, recSize);
      out.writeShort(this.getSid());
      out.writeShort(dataSize);
      this.serialize(out);
      if (out.getWriteIndex() - offset != recSize) {
         throw new IllegalStateException(
            "Error in serialization of ("
               + this.getClass().getName()
               + "): Incorrect number of bytes written - expected "
               + recSize
               + " but got "
               + (out.getWriteIndex() - offset)
         );
      } else {
         return recSize;
      }
   }

   protected abstract void serialize(LittleEndianOutput var1);
}
