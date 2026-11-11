package org.apache.poi.hssf.record;

public final class FooterRecord extends HeaderFooterBase implements Cloneable {
   public static final short sid = 21;

   public FooterRecord(String text) {
      super(text);
   }

   public FooterRecord(RecordInputStream in) {
      super(in);
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[FOOTER]\n");
      buffer.append("    .footer = ").append(this.getText()).append("\n");
      buffer.append("[/FOOTER]\n");
      return buffer.toString();
   }

   public short getSid() {
      return 21;
   }

   public FooterRecord clone() {
      return new FooterRecord(this.getText());
   }
}
