package org.apache.poi.hssf.usermodel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherChildAnchorRecord;
import org.apache.poi.ddf.EscherClientAnchorRecord;
import org.apache.poi.ddf.EscherComplexProperty;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.ss.usermodel.Shape;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;

public abstract class HSSFShape implements Shape {
   private static final POILogger LOG = POILogFactory.getLogger(HSSFShape.class);
   public static final int LINEWIDTH_ONE_PT = 12700;
   public static final int LINEWIDTH_DEFAULT = 9525;
   public static final int LINESTYLE__COLOR_DEFAULT = 134217792;
   public static final int FILL__FILLCOLOR_DEFAULT = 134217737;
   public static final boolean NO_FILL_DEFAULT = true;
   public static final int LINESTYLE_SOLID = 0;
   public static final int LINESTYLE_DASHSYS = 1;
   public static final int LINESTYLE_DOTSYS = 2;
   public static final int LINESTYLE_DASHDOTSYS = 3;
   public static final int LINESTYLE_DASHDOTDOTSYS = 4;
   public static final int LINESTYLE_DOTGEL = 5;
   public static final int LINESTYLE_DASHGEL = 6;
   public static final int LINESTYLE_LONGDASHGEL = 7;
   public static final int LINESTYLE_DASHDOTGEL = 8;
   public static final int LINESTYLE_LONGDASHDOTGEL = 9;
   public static final int LINESTYLE_LONGDASHDOTDOTGEL = 10;
   public static final int LINESTYLE_NONE = -1;
   public static final int LINESTYLE_DEFAULT = -1;
   private HSSFShape parent;
   HSSFAnchor anchor;
   private HSSFPatriarch _patriarch;
   private final EscherContainerRecord _escherContainer;
   private final ObjRecord _objRecord;
   private final EscherOptRecord _optRecord;
   public static final int NO_FILLHITTEST_TRUE = 1114112;
   public static final int NO_FILLHITTEST_FALSE = 65536;

   public HSSFShape(EscherContainerRecord spContainer, ObjRecord objRecord) {
      this._escherContainer = spContainer;
      this._objRecord = objRecord;
      this._optRecord = (EscherOptRecord)spContainer.getChildById((short)-4085);
      this.anchor = HSSFAnchor.createAnchorFromEscher(spContainer);
   }

   public HSSFShape(HSSFShape parent, HSSFAnchor anchor) {
      this.parent = parent;
      this.anchor = anchor;
      this._escherContainer = this.createSpContainer();
      this._optRecord = (EscherOptRecord)this._escherContainer.getChildById((short)-4085);
      this._objRecord = this.createObjRecord();
   }

   protected abstract EscherContainerRecord createSpContainer();

   protected abstract ObjRecord createObjRecord();

   protected abstract void afterRemove(HSSFPatriarch var1);

   void setShapeId(int shapeId) {
      EscherSpRecord spRecord = (EscherSpRecord)this._escherContainer.getChildById((short)-4086);
      spRecord.setShapeId(shapeId);
      CommonObjectDataSubRecord cod = (CommonObjectDataSubRecord)this._objRecord.getSubRecords().get(0);
      cod.setObjectId((short)(shapeId % 1024));
   }

   int getShapeId() {
      return ((EscherSpRecord)this._escherContainer.getChildById((short)-4086)).getShapeId();
   }

   abstract void afterInsert(HSSFPatriarch var1);

   protected EscherContainerRecord getEscherContainer() {
      return this._escherContainer;
   }

   protected ObjRecord getObjRecord() {
      return this._objRecord;
   }

   public EscherOptRecord getOptRecord() {
      return this._optRecord;
   }

   public HSSFShape getParent() {
      return this.parent;
   }

   public HSSFAnchor getAnchor() {
      return this.anchor;
   }

   public void setAnchor(HSSFAnchor anchor) {
      int i = 0;
      int recordId = -1;
      if (this.parent == null) {
         if (anchor instanceof HSSFChildAnchor) {
            throw new IllegalArgumentException("Must use client anchors for shapes directly attached to sheet.");
         }

         EscherClientAnchorRecord anch = (EscherClientAnchorRecord)this._escherContainer.getChildById((short)-4080);
         if (null != anch) {
            for (int var5 = 0; var5 < this._escherContainer.getChildRecords().size(); var5++) {
               if (this._escherContainer.getChild(var5).getRecordId() == -4080 && var5 != this._escherContainer.getChildRecords().size() - 1) {
                  recordId = this._escherContainer.getChild(var5 + 1).getRecordId();
               }
            }

            this._escherContainer.removeChildRecord(anch);
         }
      } else {
         if (anchor instanceof HSSFClientAnchor) {
            throw new IllegalArgumentException("Must use child anchors for shapes attached to groups.");
         }

         EscherChildAnchorRecord anch = (EscherChildAnchorRecord)this._escherContainer.getChildById((short)-4081);
         if (null != anch) {
            for (int var6 = 0; var6 < this._escherContainer.getChildRecords().size(); var6++) {
               if (this._escherContainer.getChild(var6).getRecordId() == -4081 && var6 != this._escherContainer.getChildRecords().size() - 1) {
                  recordId = this._escherContainer.getChild(var6 + 1).getRecordId();
               }
            }

            this._escherContainer.removeChildRecord(anch);
         }
      }

      if (-1 == recordId) {
         this._escherContainer.addChildRecord(anchor.getEscherAnchor());
      } else {
         this._escherContainer.addChildBefore(anchor.getEscherAnchor(), recordId);
      }

      this.anchor = anchor;
   }

   public int getLineStyleColor() {
      EscherRGBProperty rgbProperty = (EscherRGBProperty)this._optRecord.lookup(448);
      return rgbProperty == null ? 134217792 : rgbProperty.getRgbColor();
   }

   public void setLineStyleColor(int lineStyleColor) {
      this.setPropertyValue(new EscherRGBProperty((short)448, lineStyleColor));
   }

   public void setLineStyleColor(int red, int green, int blue) {
      int lineStyleColor = blue << 16 | green << 8 | red;
      this.setPropertyValue(new EscherRGBProperty((short)448, lineStyleColor));
   }

   public int getFillColor() {
      EscherRGBProperty rgbProperty = (EscherRGBProperty)this._optRecord.lookup(385);
      return rgbProperty == null ? 134217737 : rgbProperty.getRgbColor();
   }

   public void setFillColor(int fillColor) {
      this.setPropertyValue(new EscherRGBProperty((short)385, fillColor));
   }

   public void setFillColor(int red, int green, int blue) {
      int fillColor = blue << 16 | green << 8 | red;
      this.setPropertyValue(new EscherRGBProperty((short)385, fillColor));
   }

   public int getLineWidth() {
      EscherSimpleProperty property = (EscherSimpleProperty)this._optRecord.lookup(459);
      return property == null ? 9525 : property.getPropertyValue();
   }

   public void setLineWidth(int lineWidth) {
      this.setPropertyValue(new EscherSimpleProperty((short)459, lineWidth));
   }

   public int getLineStyle() {
      EscherSimpleProperty property = (EscherSimpleProperty)this._optRecord.lookup(462);
      return null == property ? -1 : property.getPropertyValue();
   }

   public void setLineStyle(int lineStyle) {
      this.setPropertyValue(new EscherSimpleProperty((short)462, lineStyle));
      if (this.getLineStyle() != 0) {
         this.setPropertyValue(new EscherSimpleProperty((short)471, 0));
         if (this.getLineStyle() == -1) {
            this.setPropertyValue(new EscherBoolProperty((short)511, 524288));
         } else {
            this.setPropertyValue(new EscherBoolProperty((short)511, 524296));
         }
      }
   }

   public boolean isNoFill() {
      EscherBoolProperty property = (EscherBoolProperty)this._optRecord.lookup(447);
      return property == null ? true : property.getPropertyValue() == 1114112;
   }

   public void setNoFill(boolean noFill) {
      this.setPropertyValue(new EscherBoolProperty((short)447, noFill ? 1114112 : 65536));
   }

   protected void setPropertyValue(EscherProperty property) {
      this._optRecord.setEscherProperty(property);
   }

   public void setFlipVertical(boolean value) {
      EscherSpRecord sp = (EscherSpRecord)this.getEscherContainer().getChildById((short)-4086);
      if (value) {
         sp.setFlags(sp.getFlags() | 128);
      } else {
         sp.setFlags(sp.getFlags() & 2147483519);
      }
   }

   public void setFlipHorizontal(boolean value) {
      EscherSpRecord sp = (EscherSpRecord)this.getEscherContainer().getChildById((short)-4086);
      if (value) {
         sp.setFlags(sp.getFlags() | 64);
      } else {
         sp.setFlags(sp.getFlags() & 2147483583);
      }
   }

   public boolean isFlipVertical() {
      EscherSpRecord sp = (EscherSpRecord)this.getEscherContainer().getChildById((short)-4086);
      return (sp.getFlags() & 128) != 0;
   }

   public boolean isFlipHorizontal() {
      EscherSpRecord sp = (EscherSpRecord)this.getEscherContainer().getChildById((short)-4086);
      return (sp.getFlags() & 64) != 0;
   }

   public int getRotationDegree() {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(4);
      if (null == property) {
         return 0;
      }

      try {
         LittleEndian.putInt(property.getPropertyValue(), bos);
         return LittleEndian.getShort(bos.toByteArray(), 2);
      } catch (IOException e) {
         LOG.log(7, new Object[]{"can't determine rotation degree", e});
         return 0;
      }
   }

   public void setRotationDegree(short value) {
      this.setPropertyValue(new EscherSimpleProperty((short)4, value << 16));
   }

   public int countOfAllChildren() {
      return 1;
   }

   protected abstract HSSFShape cloneShape();

   protected void setPatriarch(HSSFPatriarch _patriarch) {
      this._patriarch = _patriarch;
   }

   public HSSFPatriarch getPatriarch() {
      return this._patriarch;
   }

   protected void setParent(HSSFShape parent) {
      this.parent = parent;
   }

   public String getShapeName() {
      EscherOptRecord eor = this.getOptRecord();
      if (eor == null) {
         return null;
      }

      EscherProperty ep = eor.lookup(896);
      return ep instanceof EscherComplexProperty ? StringUtil.getFromUnicodeLE(((EscherComplexProperty)ep).getComplexData()) : null;
   }
}
