package org.apache.poi.hssf.usermodel;

import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NoteStructureSubRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.util.CellAddress;

public class HSSFComment extends HSSFTextbox implements Comment {
   private static final int FILL_TYPE_SOLID = 0;
   private static final int FILL_TYPE_PICTURE = 3;
   private static final int GROUP_SHAPE_PROPERTY_DEFAULT_VALUE = 655362;
   private static final int GROUP_SHAPE_HIDDEN_MASK = 16777218;
   private static final int GROUP_SHAPE_NOT_HIDDEN_MASK = -16777219;
   private final NoteRecord _note;

   public HSSFComment(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord textObjectRecord, NoteRecord note) {
      super(spContainer, objRecord, textObjectRecord);
      this._note = note;
   }

   public HSSFComment(HSSFShape parent, HSSFAnchor anchor) {
      this(parent, anchor, createNoteRecord());
   }

   private HSSFComment(HSSFShape parent, HSSFAnchor anchor, NoteRecord note) {
      super(parent, anchor);
      this._note = note;
      this.setFillColor(134217808);
      this.setVisible(false);
      this.setAuthor("");
      CommonObjectDataSubRecord cod = (CommonObjectDataSubRecord)this.getObjRecord().getSubRecords().get(0);
      cod.setObjectType((short)25);
   }

   protected HSSFComment(NoteRecord note, TextObjectRecord txo) {
      this(null, new HSSFClientAnchor(), note);
   }

   void afterInsert(HSSFPatriarch patriarch) {
      super.afterInsert(patriarch);
      patriarch.getBoundAggregate().addTailRecord(this.getNoteRecord());
   }

   protected EscherContainerRecord createSpContainer() {
      EscherContainerRecord spContainer = super.createSpContainer();
      EscherOptRecord opt = (EscherOptRecord)spContainer.getChildById((short)-4085);
      opt.removeEscherProperty(129);
      opt.removeEscherProperty(131);
      opt.removeEscherProperty(130);
      opt.removeEscherProperty(132);
      opt.setEscherProperty(new EscherSimpleProperty((short)959, false, false, 655362));
      return spContainer;
   }

   protected ObjRecord createObjRecord() {
      ObjRecord obj = new ObjRecord();
      CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
      c.setObjectType((short)202);
      c.setLocked(true);
      c.setPrintable(true);
      c.setAutofill(false);
      c.setAutoline(true);
      NoteStructureSubRecord u = new NoteStructureSubRecord();
      EndSubRecord e = new EndSubRecord();
      obj.addSubRecord(c);
      obj.addSubRecord(u);
      obj.addSubRecord(e);
      return obj;
   }

   private static NoteRecord createNoteRecord() {
      NoteRecord note = new NoteRecord();
      note.setFlags((short)0);
      note.setAuthor("");
      return note;
   }

   void setShapeId(int shapeId) {
      if (shapeId > 65535) {
         throw new IllegalArgumentException("Cannot add more than 65535 shapes");
      }

      super.setShapeId(shapeId);
      CommonObjectDataSubRecord cod = (CommonObjectDataSubRecord)this.getObjRecord().getSubRecords().get(0);
      cod.setObjectId(shapeId);
      this._note.setShapeId(shapeId);
   }

   public void setVisible(boolean visible) {
      this._note.setFlags((short)(visible ? 2 : 0));
      this.setHidden(!visible);
   }

   public boolean isVisible() {
      return this._note.getFlags() == 2;
   }

   public CellAddress getAddress() {
      return new CellAddress(this.getRow(), this.getColumn());
   }

   public void setAddress(CellAddress address) {
      this.setRow(address.getRow());
      this.setColumn(address.getColumn());
   }

   public void setAddress(int row, int col) {
      this.setRow(row);
      this.setColumn(col);
   }

   public int getRow() {
      return this._note.getRow();
   }

   public void setRow(int row) {
      this._note.setRow(row);
   }

   public int getColumn() {
      return this._note.getColumn();
   }

   public void setColumn(int col) {
      this._note.setColumn(col);
   }

   public String getAuthor() {
      return this._note.getAuthor();
   }

   public void setAuthor(String author) {
      if (this._note != null) {
         this._note.setAuthor(author);
      }
   }

   protected NoteRecord getNoteRecord() {
      return this._note;
   }

   public boolean hasPosition() {
      return this._note == null ? false : this.getColumn() >= 0 && this.getRow() >= 0;
   }

   public ClientAnchor getClientAnchor() {
      HSSFAnchor ha = super.getAnchor();
      if (ha instanceof ClientAnchor) {
         return (ClientAnchor)ha;
      } else {
         throw new IllegalStateException("Anchor can not be changed in " + ClientAnchor.class.getSimpleName());
      }
   }

   public void setShapeType(int shapeType) {
      throw new IllegalStateException("Shape type can not be changed in " + this.getClass().getSimpleName());
   }

   public void afterRemove(HSSFPatriarch patriarch) {
      super.afterRemove(patriarch);
      patriarch.getBoundAggregate().removeTailRecord(this.getNoteRecord());
   }

   protected HSSFShape cloneShape() {
      TextObjectRecord txo = (TextObjectRecord)this.getTextObjectRecord().cloneViaReserialise();
      EscherContainerRecord spContainer = new EscherContainerRecord();
      byte[] inSp = this.getEscherContainer().serialize();
      spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
      ObjRecord obj = (ObjRecord)this.getObjRecord().cloneViaReserialise();
      NoteRecord note = (NoteRecord)this.getNoteRecord().cloneViaReserialise();
      return new HSSFComment(spContainer, obj, txo, note);
   }

   public void setBackgroundImage(int pictureIndex) {
      this.setPropertyValue(new EscherSimpleProperty((short)390, false, true, pictureIndex));
      this.setPropertyValue(new EscherSimpleProperty((short)384, false, false, 3));
      EscherBSERecord bse = this.getPatriarch().getSheet().getWorkbook().getWorkbook().getBSERecord(pictureIndex);
      bse.setRef(bse.getRef() + 1);
   }

   public void resetBackgroundImage() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(390);
      if (null != property) {
         EscherBSERecord bse = this.getPatriarch().getSheet().getWorkbook().getWorkbook().getBSERecord(property.getPropertyValue());
         bse.setRef(bse.getRef() - 1);
         this.getOptRecord().removeEscherProperty(390);
      }

      this.setPropertyValue(new EscherSimpleProperty((short)384, false, false, 0));
   }

   public int getBackgroundImageId() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(390);
      return property == null ? 0 : property.getPropertyValue();
   }

   private void setHidden(boolean value) {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(959);
      if (value) {
         this.setPropertyValue(new EscherSimpleProperty((short)959, false, false, property.getPropertyValue() | 16777218));
      } else {
         this.setPropertyValue(new EscherSimpleProperty((short)959, false, false, property.getPropertyValue() & -16777219));
      }
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof HSSFComment)) {
         return false;
      }

      HSSFComment other = (HSSFComment)obj;
      return this.getNoteRecord().equals(other.getNoteRecord());
   }

   public int hashCode() {
      return (this.getRow() * 17 + this.getColumn()) * 31;
   }
}
