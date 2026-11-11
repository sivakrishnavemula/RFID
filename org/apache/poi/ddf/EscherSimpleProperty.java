package org.apache.poi.ddf;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;

public class EscherSimpleProperty extends EscherProperty {
   private int propertyValue;

   public EscherSimpleProperty(short id, int propertyValue) {
      super(id);
      this.propertyValue = propertyValue;
   }

   public EscherSimpleProperty(short propertyNumber, boolean isComplex, boolean isBlipId, int propertyValue) {
      super(propertyNumber, isComplex, isBlipId);
      this.propertyValue = propertyValue;
   }

   public int serializeSimplePart(byte[] data, int offset) {
      LittleEndian.putShort(data, offset, this.getId());
      LittleEndian.putInt(data, offset + 2, this.propertyValue);
      return 6;
   }

   public int serializeComplexPart(byte[] data, int pos) {
      return 0;
   }

   public int getPropertyValue() {
      return this.propertyValue;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof EscherSimpleProperty)) {
         return false;
      }

      EscherSimpleProperty escherSimpleProperty = (EscherSimpleProperty)o;
      return this.propertyValue != escherSimpleProperty.propertyValue ? false : this.getId() == escherSimpleProperty.getId();
   }

   public int hashCode() {
      return this.propertyValue;
   }

   public String toString() {
      return "propNum: "
         + this.getPropertyNumber()
         + ", RAW: 0x"
         + HexDump.toHex(this.getId())
         + ", propName: "
         + EscherProperties.getPropertyName(this.getPropertyNumber())
         + ", complex: "
         + this.isComplex()
         + ", blipId: "
         + this.isBlipId()
         + ", value: "
         + this.propertyValue
         + " (0x"
         + HexDump.toHex(this.propertyValue)
         + ")";
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
         .append("\" complex=\"")
         .append(this.isComplex())
         .append("\" value=\"")
         .append("0x")
         .append(HexDump.toHex(this.propertyValue))
         .append("\"/>");
      return builder.toString();
   }
}
