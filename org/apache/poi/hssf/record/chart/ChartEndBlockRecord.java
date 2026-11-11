package org.apache.poi.hssf.record.chart;

import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

public final class ChartEndBlockRecord extends StandardRecord implements Cloneable {
   public static final short sid = 2131;
   private short rt;
   private short grbitFrt;
   private short iObjectKind;
   private byte[] unused;

   public ChartEndBlockRecord() {
   }

   public ChartEndBlockRecord(RecordInputStream in) {
      this.rt = in.readShort();
      this.grbitFrt = in.readShort();
      this.iObjectKind = in.readShort();
      if (in.available() == 0) {
         this.unused = new byte[0];
      } else {
         this.unused = new byte[6];
         in.readFully(this.unused);
      }
   }

   protected int getDataSize() {
      return 6 + this.unused.length;
   }

   public short getSid() {
      return 2131;
   }

   public void serialize(LittleEndianOutput out) {
      out.writeShort(this.rt);
      out.writeShort(this.grbitFrt);
      out.writeShort(this.iObjectKind);
      out.write(this.unused);
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[ENDBLOCK]\n");
      buffer.append("    .rt         =").append(HexDump.shortToHex(this.rt)).append('\n');
      buffer.append("    .grbitFrt   =").append(HexDump.shortToHex(this.grbitFrt)).append('\n');
      buffer.append("    .iObjectKind=").append(HexDump.shortToHex(this.iObjectKind)).append('\n');
      buffer.append("    .unused     =").append(HexDump.toHex(this.unused)).append('\n');
      buffer.append("[/ENDBLOCK]\n");
      return buffer.toString();
   }

   public ChartEndBlockRecord clone() {
      ChartEndBlockRecord record = new ChartEndBlockRecord();
      record.rt = this.rt;
      record.grbitFrt = this.grbitFrt;
      record.iObjectKind = this.iObjectKind;
      record.unused = (byte[])this.unused.clone();
      return record;
   }
}
