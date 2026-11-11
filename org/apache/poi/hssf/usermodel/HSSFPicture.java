package org.apache.poi.hssf.usermodel;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherBlipRecord;
import org.apache.poi.ddf.EscherComplexProperty;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.ClientAnchor.AnchorType;
import org.apache.poi.ss.util.ImageUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.StringUtil;

public class HSSFPicture extends HSSFSimpleShape implements Picture {
   private static POILogger logger = POILogFactory.getLogger(HSSFPicture.class);
   public static final int PICTURE_TYPE_EMF = 2;
   public static final int PICTURE_TYPE_WMF = 3;
   public static final int PICTURE_TYPE_PICT = 4;
   public static final int PICTURE_TYPE_JPEG = 5;
   public static final int PICTURE_TYPE_PNG = 6;
   public static final int PICTURE_TYPE_DIB = 7;

   public HSSFPicture(EscherContainerRecord spContainer, ObjRecord objRecord) {
      super(spContainer, objRecord);
   }

   public HSSFPicture(HSSFShape parent, HSSFAnchor anchor) {
      super(parent, anchor);
      super.setShapeType(75);
      CommonObjectDataSubRecord cod = (CommonObjectDataSubRecord)this.getObjRecord().getSubRecords().get(0);
      cod.setObjectType((short)8);
   }

   public int getPictureIndex() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(260);
      return null == property ? -1 : property.getPropertyValue();
   }

   public void setPictureIndex(int pictureIndex) {
      this.setPropertyValue(new EscherSimpleProperty((short)260, false, true, pictureIndex));
   }

   protected EscherContainerRecord createSpContainer() {
      EscherContainerRecord spContainer = super.createSpContainer();
      EscherOptRecord opt = (EscherOptRecord)spContainer.getChildById((short)-4085);
      opt.removeEscherProperty(462);
      opt.removeEscherProperty(511);
      spContainer.removeChildRecord(spContainer.getChildById((short)-4083));
      return spContainer;
   }

   public void resize() {
      this.resize(Double.MAX_VALUE);
   }

   public void resize(double scale) {
      this.resize(scale, scale);
   }

   public void resize(double scaleX, double scaleY) {
      HSSFClientAnchor anchor = this.getClientAnchor();
      anchor.setAnchorType(AnchorType.MOVE_DONT_RESIZE);
      HSSFClientAnchor pref = this.getPreferredSize(scaleX, scaleY);
      int row2 = anchor.getRow1() + (pref.getRow2() - pref.getRow1());
      int col2 = anchor.getCol1() + (pref.getCol2() - pref.getCol1());
      anchor.setCol2((short)col2);
      anchor.setDx2(pref.getDx2());
      anchor.setRow2(row2);
      anchor.setDy2(pref.getDy2());
   }

   public HSSFClientAnchor getPreferredSize() {
      return this.getPreferredSize(1.0);
   }

   public HSSFClientAnchor getPreferredSize(double scale) {
      return this.getPreferredSize(scale, scale);
   }

   public HSSFClientAnchor getPreferredSize(double scaleX, double scaleY) {
      ImageUtils.setPreferredSize(this, scaleX, scaleY);
      return this.getClientAnchor();
   }

   public Dimension getImageDimension() {
      InternalWorkbook iwb = this.getPatriarch().getSheet().getWorkbook().getWorkbook();
      EscherBSERecord bse = iwb.getBSERecord(this.getPictureIndex());
      byte[] data = bse.getBlipRecord().getPicturedata();
      int type = bse.getBlipTypeWin32();
      return ImageUtils.getImageDimension(new ByteArrayInputStream(data), type);
   }

   public HSSFPictureData getPictureData() {
      int picIdx = this.getPictureIndex();
      if (picIdx == -1) {
         return null;
      }

      HSSFPatriarch patriarch = this.getPatriarch();

      for (HSSFShape parent = this.getParent(); patriarch == null && parent != null; parent = parent.getParent()) {
         patriarch = parent.getPatriarch();
      }

      if (patriarch == null) {
         throw new IllegalStateException("Could not find a patriarch for a HSSPicture");
      }

      InternalWorkbook iwb = patriarch.getSheet().getWorkbook().getWorkbook();
      EscherBSERecord bse = iwb.getBSERecord(picIdx);
      EscherBlipRecord blipRecord = bse.getBlipRecord();
      return new HSSFPictureData(blipRecord);
   }

   void afterInsert(HSSFPatriarch patriarch) {
      EscherAggregate agg = patriarch.getBoundAggregate();
      agg.associateShapeToObjRecord(this.getEscherContainer().getChildById((short)-4079), this.getObjRecord());
      if (this.getPictureIndex() != -1) {
         EscherBSERecord bse = patriarch.getSheet().getWorkbook().getWorkbook().getBSERecord(this.getPictureIndex());
         bse.setRef(bse.getRef() + 1);
      }
   }

   public String getFileName() {
      EscherComplexProperty propFile = (EscherComplexProperty)this.getOptRecord().lookup(261);
      return null == propFile ? "" : StringUtil.getFromUnicodeLE(propFile.getComplexData()).trim();
   }

   public void setFileName(String data) {
      byte[] bytes = StringUtil.getToUnicodeLE(data);
      EscherComplexProperty prop = new EscherComplexProperty((short)261, true, bytes);
      this.setPropertyValue(prop);
   }

   public void setShapeType(int shapeType) {
      throw new IllegalStateException("Shape type can not be changed in " + this.getClass().getSimpleName());
   }

   protected HSSFShape cloneShape() {
      EscherContainerRecord spContainer = new EscherContainerRecord();
      byte[] inSp = this.getEscherContainer().serialize();
      spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
      ObjRecord obj = (ObjRecord)this.getObjRecord().cloneViaReserialise();
      return new HSSFPicture(spContainer, obj);
   }

   public HSSFClientAnchor getClientAnchor() {
      HSSFAnchor a = this.getAnchor();
      return a instanceof HSSFClientAnchor ? (HSSFClientAnchor)a : null;
   }

   public HSSFSheet getSheet() {
      return this.getPatriarch().getSheet();
   }
}
