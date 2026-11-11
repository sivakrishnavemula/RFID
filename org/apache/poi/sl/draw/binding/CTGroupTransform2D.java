package org.apache.poi.sl.draw.binding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CT_GroupTransform2D", propOrder = {"off", "ext", "chOff", "chExt"})
public class CTGroupTransform2D {
   protected CTPoint2D off;
   protected CTPositiveSize2D ext;
   protected CTPoint2D chOff;
   protected CTPositiveSize2D chExt;
   @XmlAttribute(name = "rot")
   protected Integer rot;
   @XmlAttribute(name = "flipH")
   protected Boolean flipH;
   @XmlAttribute(name = "flipV")
   protected Boolean flipV;

   public CTPoint2D getOff() {
      return this.off;
   }

   public void setOff(CTPoint2D value) {
      this.off = value;
   }

   public boolean isSetOff() {
      return this.off != null;
   }

   public CTPositiveSize2D getExt() {
      return this.ext;
   }

   public void setExt(CTPositiveSize2D value) {
      this.ext = value;
   }

   public boolean isSetExt() {
      return this.ext != null;
   }

   public CTPoint2D getChOff() {
      return this.chOff;
   }

   public void setChOff(CTPoint2D value) {
      this.chOff = value;
   }

   public boolean isSetChOff() {
      return this.chOff != null;
   }

   public CTPositiveSize2D getChExt() {
      return this.chExt;
   }

   public void setChExt(CTPositiveSize2D value) {
      this.chExt = value;
   }

   public boolean isSetChExt() {
      return this.chExt != null;
   }

   public int getRot() {
      return this.rot == null ? 0 : this.rot;
   }

   public void setRot(int value) {
      this.rot = value;
   }

   public boolean isSetRot() {
      return this.rot != null;
   }

   public void unsetRot() {
      this.rot = null;
   }

   public boolean isFlipH() {
      return this.flipH == null ? false : this.flipH;
   }

   public void setFlipH(boolean value) {
      this.flipH = value;
   }

   public boolean isSetFlipH() {
      return this.flipH != null;
   }

   public void unsetFlipH() {
      this.flipH = null;
   }

   public boolean isFlipV() {
      return this.flipV == null ? false : this.flipV;
   }

   public void setFlipV(boolean value) {
      this.flipV = value;
   }

   public boolean isSetFlipV() {
      return this.flipV != null;
   }

   public void unsetFlipV() {
      this.flipV = null;
   }
}
