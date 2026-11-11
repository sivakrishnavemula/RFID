package org.apache.poi.hssf.usermodel;

import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherTextboxRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.TextObjectRecord;

public class HSSFTextbox extends HSSFSimpleShape {
   public static final short OBJECT_TYPE_TEXT = 6;
   public static final short HORIZONTAL_ALIGNMENT_LEFT = 1;
   public static final short HORIZONTAL_ALIGNMENT_CENTERED = 2;
   public static final short HORIZONTAL_ALIGNMENT_RIGHT = 3;
   public static final short HORIZONTAL_ALIGNMENT_JUSTIFIED = 4;
   public static final short HORIZONTAL_ALIGNMENT_DISTRIBUTED = 7;
   public static final short VERTICAL_ALIGNMENT_TOP = 1;
   public static final short VERTICAL_ALIGNMENT_CENTER = 2;
   public static final short VERTICAL_ALIGNMENT_BOTTOM = 3;
   public static final short VERTICAL_ALIGNMENT_JUSTIFY = 4;
   public static final short VERTICAL_ALIGNMENT_DISTRIBUTED = 7;

   public HSSFTextbox(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord textObjectRecord) {
      super(spContainer, objRecord, textObjectRecord);
   }

   public HSSFTextbox(HSSFShape parent, HSSFAnchor anchor) {
      super(parent, anchor);
      this.setHorizontalAlignment((short)1);
      this.setVerticalAlignment((short)1);
      this.setString(new HSSFRichTextString(""));
   }

   protected ObjRecord createObjRecord() {
      ObjRecord obj = new ObjRecord();
      CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
      c.setObjectType((short)6);
      c.setLocked(true);
      c.setPrintable(true);
      c.setAutofill(true);
      c.setAutoline(true);
      EndSubRecord e = new EndSubRecord();
      obj.addSubRecord(c);
      obj.addSubRecord(e);
      return obj;
   }

   protected EscherContainerRecord createSpContainer() {
      EscherContainerRecord spContainer = new EscherContainerRecord();
      EscherSpRecord sp = new EscherSpRecord();
      EscherOptRecord opt = new EscherOptRecord();
      EscherClientDataRecord clientData = new EscherClientDataRecord();
      EscherTextboxRecord escherTextbox = new EscherTextboxRecord();
      spContainer.setRecordId((short)-4092);
      spContainer.setOptions((short)15);
      sp.setRecordId((short)-4086);
      sp.setOptions((short)3234);
      sp.setFlags(2560);
      opt.setRecordId((short)-4085);
      opt.addEscherProperty(new EscherSimpleProperty((short)128, 0));
      opt.addEscherProperty(new EscherSimpleProperty((short)133, 0));
      opt.addEscherProperty(new EscherSimpleProperty((short)135, 0));
      opt.addEscherProperty(new EscherSimpleProperty((short)959, 524288));
      opt.addEscherProperty(new EscherSimpleProperty((short)129, 0));
      opt.addEscherProperty(new EscherSimpleProperty((short)131, 0));
      opt.addEscherProperty(new EscherSimpleProperty((short)130, 0));
      opt.addEscherProperty(new EscherSimpleProperty((short)132, 0));
      opt.setEscherProperty(new EscherSimpleProperty((short)462, 0));
      opt.setEscherProperty(new EscherBoolProperty((short)511, 524296));
      opt.setEscherProperty(new EscherSimpleProperty((short)459, 9525));
      opt.setEscherProperty(new EscherRGBProperty((short)385, 134217737));
      opt.setEscherProperty(new EscherRGBProperty((short)448, 134217792));
      opt.setEscherProperty(new EscherBoolProperty((short)447, 65536));
      opt.setEscherProperty(new EscherBoolProperty((short)959, 524288));
      EscherRecord anchor = this.getAnchor().getEscherAnchor();
      clientData.setRecordId((short)-4079);
      clientData.setOptions((short)0);
      escherTextbox.setRecordId((short)-4083);
      escherTextbox.setOptions((short)0);
      spContainer.addChildRecord(sp);
      spContainer.addChildRecord(opt);
      spContainer.addChildRecord(anchor);
      spContainer.addChildRecord(clientData);
      spContainer.addChildRecord(escherTextbox);
      return spContainer;
   }

   void afterInsert(HSSFPatriarch patriarch) {
      EscherAggregate agg = patriarch.getBoundAggregate();
      agg.associateShapeToObjRecord(this.getEscherContainer().getChildById((short)-4079), this.getObjRecord());
      if (this.getTextObjectRecord() != null) {
         agg.associateShapeToObjRecord(this.getEscherContainer().getChildById((short)-4083), this.getTextObjectRecord());
      }
   }

   public int getMarginLeft() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(129);
      return property == null ? 0 : property.getPropertyValue();
   }

   public void setMarginLeft(int marginLeft) {
      this.setPropertyValue(new EscherSimpleProperty((short)129, marginLeft));
   }

   public int getMarginRight() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(131);
      return property == null ? 0 : property.getPropertyValue();
   }

   public void setMarginRight(int marginRight) {
      this.setPropertyValue(new EscherSimpleProperty((short)131, marginRight));
   }

   public int getMarginTop() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(130);
      return property == null ? 0 : property.getPropertyValue();
   }

   public void setMarginTop(int marginTop) {
      this.setPropertyValue(new EscherSimpleProperty((short)130, marginTop));
   }

   public int getMarginBottom() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(132);
      return property == null ? 0 : property.getPropertyValue();
   }

   public void setMarginBottom(int marginBottom) {
      this.setPropertyValue(new EscherSimpleProperty((short)132, marginBottom));
   }

   public short getHorizontalAlignment() {
      return (short)this.getTextObjectRecord().getHorizontalTextAlignment();
   }

   public void setHorizontalAlignment(short align) {
      this.getTextObjectRecord().setHorizontalTextAlignment(align);
   }

   public short getVerticalAlignment() {
      return (short)this.getTextObjectRecord().getVerticalTextAlignment();
   }

   public void setVerticalAlignment(short align) {
      this.getTextObjectRecord().setVerticalTextAlignment(align);
   }

   public void setShapeType(int shapeType) {
      throw new IllegalStateException("Shape type can not be changed in " + this.getClass().getSimpleName());
   }

   protected HSSFShape cloneShape() {
      TextObjectRecord txo = this.getTextObjectRecord() == null ? null : (TextObjectRecord)this.getTextObjectRecord().cloneViaReserialise();
      EscherContainerRecord spContainer = new EscherContainerRecord();
      byte[] inSp = this.getEscherContainer().serialize();
      spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
      ObjRecord obj = (ObjRecord)this.getObjRecord().cloneViaReserialise();
      return new HSSFTextbox(spContainer, obj, txo);
   }

   protected void afterRemove(HSSFPatriarch patriarch) {
      patriarch.getBoundAggregate().removeShapeToObjRecord(this.getEscherContainer().getChildById((short)-4079));
      patriarch.getBoundAggregate().removeShapeToObjRecord(this.getEscherContainer().getChildById((short)-4083));
   }
}
