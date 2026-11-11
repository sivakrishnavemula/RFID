package org.apache.poi.ddf;

import org.apache.poi.util.HexDump;

public class EscherRGBProperty extends EscherSimpleProperty {
   public EscherRGBProperty(short propertyNumber, int rgbColor) {
      super(propertyNumber, rgbColor);
   }

   public int getRgbColor() {
      return this.getPropertyValue();
   }

   public byte getRed() {
      return (byte)(this.getRgbColor() & 0xFF);
   }

   public byte getGreen() {
      return (byte)(this.getRgbColor() >> 8 & 0xFF);
   }

   public byte getBlue() {
      return (byte)(this.getRgbColor() >> 16 & 0xFF);
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
         .append("\" blipId=\"")
         .append(this.isBlipId())
         .append("\" value=\"0x")
         .append(HexDump.toHex(this.getRgbColor()))
         .append("\"/>");
      return builder.toString();
   }
}
