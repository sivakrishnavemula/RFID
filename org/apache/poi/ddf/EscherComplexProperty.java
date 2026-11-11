package org.apache.poi.ddf;

import java.util.Arrays;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;

public class EscherComplexProperty extends EscherProperty {
   private byte[] _complexData;

   public EscherComplexProperty(short id, byte[] complexData) {
      super(id);
      if (complexData == null) {
         throw new IllegalArgumentException("complexData can't be null");
      }

      this._complexData = (byte[])complexData.clone();
   }

   public EscherComplexProperty(short propertyNumber, boolean isBlipId, byte[] complexData) {
      super(propertyNumber, true, isBlipId);
      if (complexData == null) {
         throw new IllegalArgumentException("complexData can't be null");
      }

      this._complexData = (byte[])complexData.clone();
   }

   public int serializeSimplePart(byte[] data, int pos) {
      LittleEndian.putShort(data, pos, this.getId());
      LittleEndian.putInt(data, pos + 2, this._complexData.length);
      return 6;
   }

   public int serializeComplexPart(byte[] data, int pos) {
      System.arraycopy(this._complexData, 0, data, pos, this._complexData.length);
      return this._complexData.length;
   }

   public byte[] getComplexData() {
      return this._complexData;
   }

   protected void setComplexData(byte[] _complexData) {
      this._complexData = _complexData;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && o instanceof EscherComplexProperty) {
         EscherComplexProperty escherComplexProperty = (EscherComplexProperty)o;
         return Arrays.equals(this._complexData, escherComplexProperty._complexData);
      } else {
         return false;
      }
   }

   public int getPropertySize() {
      return 6 + this._complexData.length;
   }

   public int hashCode() {
      return this.getId() * 11;
   }

   public String toString() {
      String dataStr = HexDump.toHex(this._complexData, 32);
      return "propNum: "
         + this.getPropertyNumber()
         + ", propName: "
         + EscherProperties.getPropertyName(this.getPropertyNumber())
         + ", complex: "
         + this.isComplex()
         + ", blipId: "
         + this.isBlipId()
         + ", data: "
         + System.getProperty("line.separator")
         + dataStr;
   }

   public String toXml(String tab) {
      return tab
         + "<"
         + this.getClass().getSimpleName()
         + " id=\"0x"
         + HexDump.toHex(this.getId())
         + "\" name=\""
         + this.getName()
         + "\" blipId=\""
         + this.isBlipId()
         + "\">\n"
         + tab
         + "</"
         + this.getClass().getSimpleName()
         + ">";
   }
}
