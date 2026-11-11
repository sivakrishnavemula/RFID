package org.apache.poi.hssf.record;

import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;

public final class EndSubRecord extends SubRecord implements Cloneable {
   public static final short sid = 0;
   private static final int ENCODED_SIZE = 0;

   public EndSubRecord() {
   }

   public EndSubRecord(LittleEndianInput in, int size) {
      if ((size & 0xFF) != 0) {
         throw new RecordFormatException("Unexpected size (" + size + ")");
      }
   }

   public boolean isTerminating() {
      return true;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[ftEnd]\n");
      buffer.append("[/ftEnd]\n");
      return buffer.toString();
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(0);
      out.writeShort(0);
   }

   protected int getDataSize() {
      return 0;
   }

   public short getSid() {
      return 0;
   }

   public EndSubRecord clone() {
      return new EndSubRecord();
   }
}
