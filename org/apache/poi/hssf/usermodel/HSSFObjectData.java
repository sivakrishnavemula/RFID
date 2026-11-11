package org.apache.poi.hssf.usermodel;

import java.io.IOException;
import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.hssf.record.EmbeddedObjectRefSubRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.ss.usermodel.ObjectData;
import org.apache.poi.util.HexDump;

public final class HSSFObjectData extends HSSFPicture implements ObjectData {
   private final DirectoryEntry _root;

   public HSSFObjectData(EscherContainerRecord spContainer, ObjRecord objRecord, DirectoryEntry _root) {
      super(spContainer, objRecord);
      this._root = _root;
   }

   public String getOLE2ClassName() {
      return this.findObjectRecord().getOLEClassName();
   }

   public DirectoryEntry getDirectory() throws IOException {
      EmbeddedObjectRefSubRecord subRecord = this.findObjectRecord();
      int streamId = subRecord.getStreamId();
      String streamName = "MBD" + HexDump.toHex(streamId);
      Entry entry = this._root.getEntry(streamName);
      if (entry instanceof DirectoryEntry) {
         return (DirectoryEntry)entry;
      } else {
         throw new IOException("Stream " + streamName + " was not an OLE2 directory");
      }
   }

   public byte[] getObjectData() {
      return this.findObjectRecord().getObjectData();
   }

   public boolean hasDirectoryEntry() {
      EmbeddedObjectRefSubRecord subRecord = this.findObjectRecord();
      Integer streamId = subRecord.getStreamId();
      return streamId != null && streamId != 0;
   }

   protected EmbeddedObjectRefSubRecord findObjectRecord() {
      for (Object subRecord : this.getObjRecord().getSubRecords()) {
         if (subRecord instanceof EmbeddedObjectRefSubRecord) {
            return (EmbeddedObjectRefSubRecord)subRecord;
         }
      }

      throw new IllegalStateException("Object data does not contain a reference to an embedded object OLE2 directory");
   }

   protected EscherContainerRecord createSpContainer() {
      throw new IllegalStateException("HSSFObjectData cannot be created from scratch");
   }

   protected ObjRecord createObjRecord() {
      throw new IllegalStateException("HSSFObjectData cannot be created from scratch");
   }

   protected void afterRemove(HSSFPatriarch patriarch) {
      throw new IllegalStateException("HSSFObjectData cannot be created from scratch");
   }

   void afterInsert(HSSFPatriarch patriarch) {
      EscherAggregate agg = patriarch.getBoundAggregate();
      agg.associateShapeToObjRecord(this.getEscherContainer().getChildById((short)-4079), this.getObjRecord());
      EscherBSERecord bse = patriarch.getSheet().getWorkbook().getWorkbook().getBSERecord(this.getPictureIndex());
      bse.setRef(bse.getRef() + 1);
   }

   protected HSSFShape cloneShape() {
      EscherContainerRecord spContainer = new EscherContainerRecord();
      byte[] inSp = this.getEscherContainer().serialize();
      spContainer.fillFields(inSp, 0, new DefaultEscherRecordFactory());
      ObjRecord obj = (ObjRecord)this.getObjRecord().cloneViaReserialise();
      return new HSSFObjectData(spContainer, obj, this._root);
   }
}
