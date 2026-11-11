package org.apache.poi.sl.draw.binding;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ST_TextShapeType")
@XmlEnum
public enum STTextShapeType {
   @XmlEnumValue("textNoShape")
   TEXT_NO_SHAPE("textNoShape"),
   @XmlEnumValue("textPlain")
   TEXT_PLAIN("textPlain"),
   @XmlEnumValue("textStop")
   TEXT_STOP("textStop"),
   @XmlEnumValue("textTriangle")
   TEXT_TRIANGLE("textTriangle"),
   @XmlEnumValue("textTriangleInverted")
   TEXT_TRIANGLE_INVERTED("textTriangleInverted"),
   @XmlEnumValue("textChevron")
   TEXT_CHEVRON("textChevron"),
   @XmlEnumValue("textChevronInverted")
   TEXT_CHEVRON_INVERTED("textChevronInverted"),
   @XmlEnumValue("textRingInside")
   TEXT_RING_INSIDE("textRingInside"),
   @XmlEnumValue("textRingOutside")
   TEXT_RING_OUTSIDE("textRingOutside"),
   @XmlEnumValue("textArchUp")
   TEXT_ARCH_UP("textArchUp"),
   @XmlEnumValue("textArchDown")
   TEXT_ARCH_DOWN("textArchDown"),
   @XmlEnumValue("textCircle")
   TEXT_CIRCLE("textCircle"),
   @XmlEnumValue("textButton")
   TEXT_BUTTON("textButton"),
   @XmlEnumValue("textArchUpPour")
   TEXT_ARCH_UP_POUR("textArchUpPour"),
   @XmlEnumValue("textArchDownPour")
   TEXT_ARCH_DOWN_POUR("textArchDownPour"),
   @XmlEnumValue("textCirclePour")
   TEXT_CIRCLE_POUR("textCirclePour"),
   @XmlEnumValue("textButtonPour")
   TEXT_BUTTON_POUR("textButtonPour"),
   @XmlEnumValue("textCurveUp")
   TEXT_CURVE_UP("textCurveUp"),
   @XmlEnumValue("textCurveDown")
   TEXT_CURVE_DOWN("textCurveDown"),
   @XmlEnumValue("textCanUp")
   TEXT_CAN_UP("textCanUp"),
   @XmlEnumValue("textCanDown")
   TEXT_CAN_DOWN("textCanDown"),
   @XmlEnumValue("textWave1")
   TEXT_WAVE_1("textWave1"),
   @XmlEnumValue("textWave2")
   TEXT_WAVE_2("textWave2"),
   @XmlEnumValue("textDoubleWave1")
   TEXT_DOUBLE_WAVE_1("textDoubleWave1"),
   @XmlEnumValue("textWave4")
   TEXT_WAVE_4("textWave4"),
   @XmlEnumValue("textInflate")
   TEXT_INFLATE("textInflate"),
   @XmlEnumValue("textDeflate")
   TEXT_DEFLATE("textDeflate"),
   @XmlEnumValue("textInflateBottom")
   TEXT_INFLATE_BOTTOM("textInflateBottom"),
   @XmlEnumValue("textDeflateBottom")
   TEXT_DEFLATE_BOTTOM("textDeflateBottom"),
   @XmlEnumValue("textInflateTop")
   TEXT_INFLATE_TOP("textInflateTop"),
   @XmlEnumValue("textDeflateTop")
   TEXT_DEFLATE_TOP("textDeflateTop"),
   @XmlEnumValue("textDeflateInflate")
   TEXT_DEFLATE_INFLATE("textDeflateInflate"),
   @XmlEnumValue("textDeflateInflateDeflate")
   TEXT_DEFLATE_INFLATE_DEFLATE("textDeflateInflateDeflate"),
   @XmlEnumValue("textFadeRight")
   TEXT_FADE_RIGHT("textFadeRight"),
   @XmlEnumValue("textFadeLeft")
   TEXT_FADE_LEFT("textFadeLeft"),
   @XmlEnumValue("textFadeUp")
   TEXT_FADE_UP("textFadeUp"),
   @XmlEnumValue("textFadeDown")
   TEXT_FADE_DOWN("textFadeDown"),
   @XmlEnumValue("textSlantUp")
   TEXT_SLANT_UP("textSlantUp"),
   @XmlEnumValue("textSlantDown")
   TEXT_SLANT_DOWN("textSlantDown"),
   @XmlEnumValue("textCascadeUp")
   TEXT_CASCADE_UP("textCascadeUp"),
   @XmlEnumValue("textCascadeDown")
   TEXT_CASCADE_DOWN("textCascadeDown");

   private final String value;

   STTextShapeType(String v) {
      this.value = v;
   }

   public String value() {
      return this.value;
   }

   public static STTextShapeType fromValue(String v) {
      for (STTextShapeType c : values()) {
         if (c.value.equals(v)) {
            return c;
         }
      }

      throw new IllegalArgumentException(v);
   }
}
