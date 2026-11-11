package org.apache.poi.hssf.record;

public final class HeaderRecord extends HeaderFooterBase implements Cloneable {
   public static final short sid = 20;

   public HeaderRecord(String text) {
      super(text);
   }

   public HeaderRecord(RecordInputStream in) {
      super(in);
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[HEADER]\n");
      buffer.append("    .header = ").append(this.getText()).append("\n");
      buffer.append("[/HEADER]\n");
      return buffer.toString();
   }

   public short getSid() {
      return 20;
   }

   public HeaderRecord clone() {
      return new HeaderRecord(this.getText());
   }
}
