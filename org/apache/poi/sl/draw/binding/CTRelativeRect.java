package org.apache.poi.sl.draw.binding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CT_RelativeRect")
public class CTRelativeRect {
   @XmlAttribute(name = "l")
   protected Integer l;
   @XmlAttribute(name = "t")
   protected Integer t;
   @XmlAttribute(name = "r")
   protected Integer r;
   @XmlAttribute(name = "b")
   protected Integer b;

   public int getL() {
      return this.l == null ? 0 : this.l;
   }

   public void setL(int value) {
      this.l = value;
   }

   public boolean isSetL() {
      return this.l != null;
   }

   public void unsetL() {
      this.l = null;
   }

   public int getT() {
      return this.t == null ? 0 : this.t;
   }

   public void setT(int value) {
      this.t = value;
   }

   public boolean isSetT() {
      return this.t != null;
   }

   public void unsetT() {
      this.t = null;
   }

   public int getR() {
      return this.r == null ? 0 : this.r;
   }

   public void setR(int value) {
      this.r = value;
   }

   public boolean isSetR() {
      return this.r != null;
   }

   public void unsetR() {
      this.r = null;
   }

   public int getB() {
      return this.b == null ? 0 : this.b;
   }

   public void setB(int value) {
      this.b = value;
   }

   public boolean isSetB() {
      return this.b != null;
   }

   public void unsetB() {
      this.b = null;
   }
}
