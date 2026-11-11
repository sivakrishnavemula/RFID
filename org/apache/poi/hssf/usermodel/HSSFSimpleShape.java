package org.apache.poi.hssf.usermodel;

import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherShapePathProperty;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherTextboxRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.SimpleShape;

public class HSSFSimpleShape extends HSSFShape implements SimpleShape {
   public static final short OBJECT_TYPE_LINE = 20;
   public static final short OBJECT_TYPE_RECTANGLE = 1;
   public static final short OBJECT_TYPE_OVAL = 3;
   public static final short OBJECT_TYPE_ARC = 19;
   public static final short OBJECT_TYPE_PICTURE = 75;
   public static final short OBJECT_TYPE_COMBO_BOX = 201;
   public static final short OBJECT_TYPE_COMMENT = 202;
   public static final short OBJECT_TYPE_MICROSOFT_OFFICE_DRAWING = 30;
   public static final int WRAP_SQUARE = 0;
   public static final int WRAP_BY_POINTS = 1;
   public static final int WRAP_NONE = 2;
   private TextObjectRecord _textObjectRecord;

   public HSSFSimpleShape(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord textObjectRecord) {
      super(spContainer, objRecord);
      this._textObjectRecord = textObjectRecord;
   }

   public HSSFSimpleShape(EscherContainerRecord spContainer, ObjRecord objRecord) {
      super(spContainer, objRecord);
   }

   public HSSFSimpleShape(HSSFShape parent, HSSFAnchor anchor) {
      super(parent, anchor);
      this._textObjectRecord = this.createTextObjRecord();
   }

   protected TextObjectRecord getTextObjectRecord() {
      return this._textObjectRecord;
   }

   protected TextObjectRecord createTextObjRecord() {
      TextObjectRecord obj = new TextObjectRecord();
      obj.setHorizontalTextAlignment(2);
      obj.setVerticalTextAlignment(2);
      obj.setTextLocked(true);
      obj.setTextOrientation(0);
      obj.setStr(new HSSFRichTextString(""));
      return obj;
   }

   protected EscherContainerRecord createSpContainer() {
      EscherContainerRecord spContainer = new EscherContainerRecord();
      spContainer.setRecordId((short)-4092);
      spContainer.setOptions((short)15);
      EscherSpRecord sp = new EscherSpRecord();
      sp.setRecordId((short)-4086);
      sp.setFlags(2560);
      sp.setVersion((short)2);
      EscherClientDataRecord clientData = new EscherClientDataRecord();
      clientData.setRecordId((short)-4079);
      clientData.setOptions((short)0);
      EscherOptRecord optRecord = new EscherOptRecord();
      optRecord.setEscherProperty(new EscherSimpleProperty((short)462, 0));
      optRecord.setEscherProperty(new EscherBoolProperty((short)511, 524296));
      optRecord.setEscherProperty(new EscherRGBProperty((short)385, 134217737));
      optRecord.setEscherProperty(new EscherRGBProperty((short)448, 134217792));
      optRecord.setEscherProperty(new EscherBoolProperty((short)447, 65536));
      optRecord.setEscherProperty(new EscherBoolProperty((short)511, 524296));
      optRecord.setEscherProperty(new EscherShapePathProperty((short)324, 4));
      optRecord.setEscherProperty(new EscherBoolProperty((short)959, 524288));
      optRecord.setRecordId((short)-4085);
      EscherTextboxRecord escherTextbox = new EscherTextboxRecord();
      escherTextbox.setRecordId((short)-4083);
      escherTextbox.setOptions((short)0);
      spContainer.addChildRecord(sp);
      spContainer.addChildRecord(optRecord);
      spContainer.addChildRecord(this.getAnchor().getEscherAnchor());
      spContainer.addChildRecord(clientData);
      spContainer.addChildRecord(escherTextbox);
      return spContainer;
   }

   protected ObjRecord createObjRecord() {
      ObjRecord obj = new ObjRecord();
      CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
      c.setLocked(true);
      c.setPrintable(true);
      c.setAutofill(true);
      c.setAutoline(true);
      EndSubRecord e = new EndSubRecord();
      obj.addSubRecord(c);
      obj.addSubRecord(e);
      return obj;
   }

   protected void afterRemove(HSSFPatriarch patriarch) {
      patriarch.getBoundAggregate().removeShapeToObjRecord(this.getEscherContainer().getChildById((short)-4079));
      if (null != this.getEscherContainer().getChildById((short)-4083)) {
         patriarch.getBoundAggregate().removeShapeToObjRecord(this.getEscherContainer().getChildById((short)-4083));
      }
   }

   public HSSFRichTextString getString() {
      return this._textObjectRecord.getStr();
   }

   public void setString(RichTextString string) {
      if (this.getShapeType() != 0 && this.getShapeType() != 20) {
         HSSFRichTextString rtr = (HSSFRichTextString)string;
         if (rtr.numFormattingRuns() == 0) {
            rtr.applyFont((short)0);
         }

         TextObjectRecord txo = this.getOrCreateTextObjRecord();
         txo.setStr(rtr);
         if (string.getString() != null) {
            this.setPropertyValue(new EscherSimpleProperty((short)128, string.getString().hashCode()));
         }
      } else {
         throw new IllegalStateException("Cannot set text for shape type: " + this.getShapeType());
      }
   }

   void afterInsert(HSSFPatriarch patriarch) {
      EscherAggregate agg = patriarch.getBoundAggregate();
      agg.associateShapeToObjRecord(this.getEscherContainer().getChildById((short)-4079), this.getObjRecord());
      if (null != this.getTextObjectRecord()) {
         agg.associateShapeToObjRecord(this.getEscherContainer().getChildById((short)-4083), this.getTextObjectRecord());
      }
   }

   protected HSSFShape cloneShape() {
      TextObjectRecord txo = null;
      EscherContainerRecord spContainer = new EscherContainerRecord();
      byte[] inSp = this.getEscherContainer().serialize();
      spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
      ObjRecord obj = (ObjRecord)this.getObjRecord().cloneViaReserialise();
      if (this.getTextObjectRecord() != null && this.getString() != null && null != this.getString().getString()) {
         txo = (TextObjectRecord)this.getTextObjectRecord().cloneViaReserialise();
      }

      return new HSSFSimpleShape(spContainer, obj, txo);
   }

   public int getShapeType() {
      EscherSpRecord spRecord = (EscherSpRecord)this.getEscherContainer().getChildById((short)-4086);
      return spRecord.getShapeType();
   }

   public int getWrapText() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(133);
      return null == property ? 0 : property.getPropertyValue();
   }

   public void setWrapText(int value) {
      this.setPropertyValue(new EscherSimpleProperty((short)133, false, false, value));
   }

   public void setShapeType(int value) {
      CommonObjectDataSubRecord cod = (CommonObjectDataSubRecord)this.getObjRecord().getSubRecords().get(0);
      cod.setObjectType((short)30);
      EscherSpRecord spRecord = (EscherSpRecord)this.getEscherContainer().getChildById((short)-4086);
      spRecord.setShapeType((short)value);
   }

   private TextObjectRecord getOrCreateTextObjRecord() {
      if (this.getTextObjectRecord() == null) {
         this._textObjectRecord = this.createTextObjRecord();
      }

      EscherTextboxRecord escherTextbox = (EscherTextboxRecord)this.getEscherContainer().getChildById((short)-4083);
      if (null == escherTextbox) {
         escherTextbox = new EscherTextboxRecord();
         escherTextbox.setRecordId((short)-4083);
         escherTextbox.setOptions((short)0);
         this.getEscherContainer().addChildRecord(escherTextbox);
         this.getPatriarch().getBoundAggregate().associateShapeToObjRecord(escherTextbox, this._textObjectRecord);
      }

      return this._textObjectRecord;
   }

   public int getShapeId() {
      return super.getShapeId();
   }
}
