package org.apache.poi.sl.draw.binding;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ST_BlackWhiteMode")
@XmlEnum
public enum STBlackWhiteMode {
   @XmlEnumValue("clr")
   CLR("clr"),
   @XmlEnumValue("auto")
   AUTO("auto"),
   @XmlEnumValue("gray")
   GRAY("gray"),
   @XmlEnumValue("ltGray")
   LT_GRAY("ltGray"),
   @XmlEnumValue("invGray")
   INV_GRAY("invGray"),
   @XmlEnumValue("grayWhite")
   GRAY_WHITE("grayWhite"),
   @XmlEnumValue("blackGray")
   BLACK_GRAY("blackGray"),
   @XmlEnumValue("blackWhite")
   BLACK_WHITE("blackWhite"),
   @XmlEnumValue("black")
   BLACK("black"),
   @XmlEnumValue("white")
   WHITE("white"),
   @XmlEnumValue("hidden")
   HIDDEN("hidden");

   private final String value;

   STBlackWhiteMode(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static STBlackWhiteMode fromValue(String v) {
      for (STBlackWhiteMode c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
