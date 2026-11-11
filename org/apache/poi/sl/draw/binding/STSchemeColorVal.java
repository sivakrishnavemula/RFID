package org.apache.poi.sl.draw.binding;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ST_SchemeColorVal")
@XmlEnum
public enum STSchemeColorVal {
   @XmlEnumValue("bg1")
   BG_1("bg1"),
   @XmlEnumValue("tx1")
   TX_1("tx1"),
   @XmlEnumValue("bg2")
   BG_2("bg2"),
   @XmlEnumValue("tx2")
   TX_2("tx2"),
   @XmlEnumValue("accent1")
   ACCENT_1("accent1"),
   @XmlEnumValue("accent2")
   ACCENT_2("accent2"),
   @XmlEnumValue("accent3")
   ACCENT_3("accent3"),
   @XmlEnumValue("accent4")
   ACCENT_4("accent4"),
   @XmlEnumValue("accent5")
   ACCENT_5("accent5"),
   @XmlEnumValue("accent6")
   ACCENT_6("accent6"),
   @XmlEnumValue("hlink")
   HLINK("hlink"),
   @XmlEnumValue("folHlink")
   FOL_HLINK("folHlink"),
   @XmlEnumValue("phClr")
   PH_CLR("phClr"),
   @XmlEnumValue("dk1")
   DK_1("dk1"),
   @XmlEnumValue("lt1")
   LT_1("lt1"),
   @XmlEnumValue("dk2")
   DK_2("dk2"),
   @XmlEnumValue("lt2")
   LT_2("lt2");

   private final String value;

   STSchemeColorVal(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static STSchemeColorVal fromValue(String v) {
      for (STSchemeColorVal c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
