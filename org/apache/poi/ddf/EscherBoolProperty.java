package org.apache.poi.ddf;

import org.apache.poi.util.HexDump;

public class EscherBoolProperty extends EscherSimpleProperty {
   public EscherBoolProperty(short propertyNumber, int value) {
      super(propertyNumber, value);
   }

   public boolean isTrue() {
      return this.getPropertyValue() != 0;
   }

   public String toXml(String tab) {
      StringBuilder builder = new StringBuilder();
      builder.append(tab)
         .append("<")
         .append(this.getClass().getSimpleName())
         .append(" id=\"0x")
         .append(HexDump.toHex(this.getId()))
         .append("\" name=\"")
         .append(this.getName())
         .append("\" simpleValue=\"")
         .append(this.getPropertyValue())
         .append("\" blipId=\"")
         .append(this.isBlipId())
         .append("\" value=\"")
         .append(this.isTrue())
         .append("\"")
         .append("/>");
      return builder.toString();
   }
}
