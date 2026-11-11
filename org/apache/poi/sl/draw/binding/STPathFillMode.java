package org.apache.poi.sl.draw.binding;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ST_PathFillMode")
@XmlEnum
public enum STPathFillMode {
   @XmlEnumValue("none")
   NONE("none"),
   @XmlEnumValue("norm")
   NORM("norm"),
   @XmlEnumValue("lighten")
   LIGHTEN("lighten"),
   @XmlEnumValue("lightenLess")
   LIGHTEN_LESS("lightenLess"),
   @XmlEnumValue("darken")
   DARKEN("darken"),
   @XmlEnumValue("darkenLess")
   DARKEN_LESS("darkenLess");

   private final String value;

   STPathFillMode(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static STPathFillMode fromValue(String v) {
      for (STPathFillMode c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
