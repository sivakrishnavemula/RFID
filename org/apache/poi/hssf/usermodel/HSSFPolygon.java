package org.apache.poi.hssf.usermodel;

import org.apache.poi.ddf.EscherArrayProperty;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherClientDataRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherShapePathProperty;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EndSubRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class HSSFPolygon extends HSSFSimpleShape {
   private static POILogger logger = POILogFactory.getLogger(HSSFPolygon.class);
   public static final short OBJECT_TYPE_MICROSOFT_OFFICE_DRAWING = 30;

   public HSSFPolygon(EscherContainerRecord spContainer, ObjRecord objRecord, TextObjectRecord _textObjectRecord) {
      super(spContainer, objRecord, _textObjectRecord);
   }

   public HSSFPolygon(EscherContainerRecord spContainer, ObjRecord objRecord) {
      super(spContainer, objRecord);
   }

   HSSFPolygon(HSSFShape parent, HSSFAnchor anchor) {
      super(parent, anchor);
   }

   protected TextObjectRecord createTextObjRecord() {
      return null;
   }

   protected EscherContainerRecord createSpContainer() {
      EscherContainerRecord spContainer = new EscherContainerRecord();
      EscherSpRecord sp = new EscherSpRecord();
      EscherOptRecord opt = new EscherOptRecord();
      EscherClientDataRecord clientData = new EscherClientDataRecord();
      spContainer.setRecordId((short)-4092);
      spContainer.setOptions((short)15);
      sp.setRecordId((short)-4086);
      sp.setOptions((short)2);
      if (this.getParent() == null) {
         sp.setFlags(2560);
      } else {
         sp.setFlags(2562);
      }

      opt.setRecordId((short)-4085);
      opt.setEscherProperty(new EscherSimpleProperty((short)4, false, false, 0));
      opt.setEscherProperty(new EscherSimpleProperty((short)322, false, false, 100));
      opt.setEscherProperty(new EscherSimpleProperty((short)323, false, false, 100));
      opt.setEscherProperty(new EscherShapePathProperty((short)324, 4));
      opt.setEscherProperty(new EscherSimpleProperty((short)383, false, false, 65537));
      opt.setEscherProperty(new EscherSimpleProperty((short)464, false, false, 0));
      opt.setEscherProperty(new EscherSimpleProperty((short)465, false, false, 0));
      opt.setEscherProperty(new EscherSimpleProperty((short)471, false, false, 0));
      opt.setEscherProperty(new EscherSimpleProperty((short)462, 0));
      opt.setEscherProperty(new EscherBoolProperty((short)511, 524296));
      opt.setEscherProperty(new EscherSimpleProperty((short)459, 9525));
      opt.setEscherProperty(new EscherRGBProperty((short)385, 134217737));
      opt.setEscherProperty(new EscherRGBProperty((short)448, 134217792));
      opt.setEscherProperty(new EscherBoolProperty((short)447, 1));
      opt.setEscherProperty(new EscherBoolProperty((short)959, 524288));
      EscherRecord anchor = this.getAnchor().getEscherAnchor();
      clientData.setRecordId((short)-4079);
      clientData.setOptions((short)0);
      spContainer.addChildRecord(sp);
      spContainer.addChildRecord(opt);
      spContainer.addChildRecord(anchor);
      spContainer.addChildRecord(clientData);
      return spContainer;
   }

   protected ObjRecord createObjRecord() {
      ObjRecord obj = new ObjRecord();
      CommonObjectDataSubRecord c = new CommonObjectDataSubRecord();
      c.setObjectType((short)30);
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
   }

   public int[] getXPoints() {
      EscherArrayProperty verticesProp = (EscherArrayProperty)this.getOptRecord().lookup(325);
      if (null == verticesProp) {
         return new int[0];
      }

      int[] array = new int[verticesProp.getNumberOfElementsInArray() - 1];

      for (int i = 0; i < verticesProp.getNumberOfElementsInArray() - 1; i++) {
         byte[] property = verticesProp.getElement(i);
         short x = LittleEndian.getShort(property, 0);
         array[i] = x;
      }

      return array;
   }

   public int[] getYPoints() {
      EscherArrayProperty verticesProp = (EscherArrayProperty)this.getOptRecord().lookup(325);
      if (null == verticesProp) {
         return new int[0];
      }

      int[] array = new int[verticesProp.getNumberOfElementsInArray() - 1];

      for (int i = 0; i < verticesProp.getNumberOfElementsInArray() - 1; i++) {
         byte[] property = verticesProp.getElement(i);
         short x = LittleEndian.getShort(property, 2);
         array[i] = x;
      }

      return array;
   }

   public void setPoints(int[] xPoints, int[] yPoints) {
      if (xPoints.length != yPoints.length) {
         logger.log(7, new Object[]{"xPoint.length must be equal to yPoints.length"});
      } else {
         if (xPoints.length == 0) {
            logger.log(7, new Object[]{"HSSFPolygon must have at least one point"});
         }

         EscherArrayProperty verticesProp = new EscherArrayProperty((short)325, false, new byte[0]);
         verticesProp.setNumberOfElementsInArray(xPoints.length + 1);
         verticesProp.setNumberOfElementsInMemory(xPoints.length + 1);
         verticesProp.setSizeOfElements(65520);

         for (int i = 0; i < xPoints.length; i++) {
            byte[] data = new byte[4];
            LittleEndian.putShort(data, 0, (short)xPoints[i]);
            LittleEndian.putShort(data, 2, (short)yPoints[i]);
            verticesProp.setElement(i, data);
         }

         int point = xPoints.length;
         byte[] data = new byte[4];
         LittleEndian.putShort(data, 0, (short)xPoints[0]);
         LittleEndian.putShort(data, 2, (short)yPoints[0]);
         verticesProp.setElement(point, data);
         this.setPropertyValue(verticesProp);
         EscherArrayProperty segmentsProp = new EscherArrayProperty((short)326, false, null);
         segmentsProp.setSizeOfElements(2);
         segmentsProp.setNumberOfElementsInArray(xPoints.length * 2 + 4);
         segmentsProp.setNumberOfElementsInMemory(xPoints.length * 2 + 4);
         segmentsProp.setElement(0, new byte[]{0, 64});
         segmentsProp.setElement(1, new byte[]{0, -84});

         for (int i = 0; i < xPoints.length; i++) {
            segmentsProp.setElement(2 + i * 2, new byte[]{1, 0});
            segmentsProp.setElement(3 + i * 2, new byte[]{0, -84});
         }

         segmentsProp.setElement(segmentsProp.getNumberOfElementsInArray() - 2, new byte[]{1, 96});
         segmentsProp.setElement(segmentsProp.getNumberOfElementsInArray() - 1, new byte[]{0, -128});
         this.setPropertyValue(segmentsProp);
      }
   }

   public void setPolygonDrawArea(int width, int height) {
      this.setPropertyValue(new EscherSimpleProperty((short)322, width));
      this.setPropertyValue(new EscherSimpleProperty((short)323, height));
   }

   public int getDrawAreaWidth() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(322);
      return property == null ? 100 : property.getPropertyValue();
   }

   public int getDrawAreaHeight() {
      EscherSimpleProperty property = (EscherSimpleProperty)this.getOptRecord().lookup(323);
      return property == null ? 100 : property.getPropertyValue();
   }
}
