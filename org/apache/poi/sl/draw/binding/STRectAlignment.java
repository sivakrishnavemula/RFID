package org.apache.poi.sl.draw.binding;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ST_RectAlignment")
@XmlEnum
public enum STRectAlignment {
   @XmlEnumValue("tl")
   TL("tl"),
   @XmlEnumValue("t")
   T("t"),
   @XmlEnumValue("tr")
   TR("tr"),
   @XmlEnumValue("l")
   L("l"),
   @XmlEnumValue("ctr")
   CTR("ctr"),
   @XmlEnumValue("r")
   R("r"),
   @XmlEnumValue("bl")
   BL("bl"),
   @XmlEnumValue("b")
   B("b"),
   @XmlEnumValue("br")
   BR("br");

   private final String value;

   STRectAlignment(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static STRectAlignment fromValue(String v) {
      for (STRectAlignment c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
