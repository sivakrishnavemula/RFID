package org.apache.poi.ddf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.RecordFormatException;

public final class EscherDggRecord extends EscherRecord {
   public static final short RECORD_ID = -4090;
   public static final String RECORD_DESCRIPTION = "MsofbtDgg";
   private int field_1_shapeIdMax;
   private int field_3_numShapesSaved;
   private int field_4_drawingsSaved;
   private final List<EscherDggRecord.FileIdCluster> field_5_fileIdClusters = new ArrayList();
   private int maxDgId;

   public int fillFields(byte[] data, int offset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, offset);
      int pos = offset + 8;
      int size = 0;
      this.field_1_shapeIdMax = LittleEndian.getInt(data, pos + size);
      size += 4;
      size += 4;
      this.field_3_numShapesSaved = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_4_drawingsSaved = LittleEndian.getInt(data, pos + size);
      size += 4;
      this.field_5_fileIdClusters.clear();
      int numIdClusters = (bytesRemaining - size) / 8;

      for (int i = 0; i < numIdClusters; i++) {
         int drawingGroupId = LittleEndian.getInt(data, pos + size);
         int numShapeIdsUsed = LittleEndian.getInt(data, pos + size + 4);
         EscherDggRecord.FileIdCluster fic = new EscherDggRecord.FileIdCluster(drawingGroupId, numShapeIdsUsed);
         this.field_5_fileIdClusters.add(fic);
         this.maxDgId = Math.max(this.maxDgId, drawingGroupId);
         size += 8;
      }

      bytesRemaining -= size;
      if (bytesRemaining != 0) {
         throw new RecordFormatException("Expecting no remaining data but got " + bytesRemaining + " byte(s).");
      } else {
         return 8 + size;
      }
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      int pos = offset;
      LittleEndian.putShort(data, pos, this.getOptions());
      pos += 2;
      LittleEndian.putShort(data, pos, this.getRecordId());
      pos += 2;
      int remainingBytes = this.getRecordSize() - 8;
      LittleEndian.putInt(data, pos, remainingBytes);
      pos += 4;
      LittleEndian.putInt(data, pos, this.field_1_shapeIdMax);
      pos += 4;
      LittleEndian.putInt(data, pos, this.getNumIdClusters());
      pos += 4;
      LittleEndian.putInt(data, pos, this.field_3_numShapesSaved);
      pos += 4;
      LittleEndian.putInt(data, pos, this.field_4_drawingsSaved);
      pos += 4;

      for (EscherDggRecord.FileIdCluster fic : this.field_5_fileIdClusters) {
         LittleEndian.putInt(data, pos, fic.getDrawingGroupId());
         pos += 4;
         LittleEndian.putInt(data, pos, fic.getNumShapeIdsUsed());
         pos += 4;
      }

      listener.afterRecordSerialize(pos, this.getRecordId(), this.getRecordSize(), this);
      return this.getRecordSize();
   }

   public int getRecordSize() {
      return 24 + 8 * this.field_5_fileIdClusters.size();
   }

   public short getRecordId() {
      return -4090;
   }

   public String getRecordName() {
      return "Dgg";
   }

   public int getShapeIdMax() {
      return this.field_1_shapeIdMax;
   }

   public void setShapeIdMax(int shapeIdMax) {
      this.field_1_shapeIdMax = shapeIdMax;
   }

   public int getNumIdClusters() {
      return this.field_5_fileIdClusters.isEmpty() ? 0 : this.field_5_fileIdClusters.size() + 1;
   }

   public int getNumShapesSaved() {
      return this.field_3_numShapesSaved;
   }

   public void setNumShapesSaved(int numShapesSaved) {
      this.field_3_numShapesSaved = numShapesSaved;
   }

   public int getDrawingsSaved() {
      return this.field_4_drawingsSaved;
   }

   public void setDrawingsSaved(int drawingsSaved) {
      this.field_4_drawingsSaved = drawingsSaved;
   }

   public int getMaxDrawingGroupId() {
      return this.maxDgId;
   }

   public EscherDggRecord.FileIdCluster[] getFileIdClusters() {
      return (EscherDggRecord.FileIdCluster[])this.field_5_fileIdClusters.toArray(new EscherDggRecord.FileIdCluster[this.field_5_fileIdClusters.size()]);
   }

   public void setFileIdClusters(EscherDggRecord.FileIdCluster[] fileIdClusters) {
      this.field_5_fileIdClusters.clear();
      if (fileIdClusters != null) {
         this.field_5_fileIdClusters.addAll(Arrays.asList(fileIdClusters));
      }
   }

   public EscherDggRecord.FileIdCluster addCluster(int dgId, int numShapedUsed) {
      return this.addCluster(dgId, numShapedUsed, true);
   }

   public EscherDggRecord.FileIdCluster addCluster(int dgId, int numShapedUsed, boolean sort) {
      EscherDggRecord.FileIdCluster ficNew = new EscherDggRecord.FileIdCluster(dgId, numShapedUsed);
      this.field_5_fileIdClusters.add(ficNew);
      this.maxDgId = Math.min(this.maxDgId, dgId);
      if (sort) {
         this.sortCluster();
      }

      return ficNew;
   }

   private void sortCluster() {
      this.field_5_fileIdClusters.sort(new Comparator<EscherDggRecord.FileIdCluster>() {
         public int compare(EscherDggRecord.FileIdCluster f1, EscherDggRecord.FileIdCluster f2) {
            int dgDif = f1.getDrawingGroupId() - f2.getDrawingGroupId();
            int cntDif = f2.getNumShapeIdsUsed() - f1.getNumShapeIdsUsed();
            return dgDif != 0 ? dgDif : cntDif;
         }
      });
   }

   public short findNewDrawingGroupId() {
      BitSet bs = new BitSet();
      bs.set(0);

      for (EscherDggRecord.FileIdCluster fic : this.field_5_fileIdClusters) {
         bs.set(fic.getDrawingGroupId());
      }

      return (short)bs.nextClearBit(0);
   }

   public int allocateShapeId(EscherDgRecord dg, boolean sort) {
      short drawingGroupId = dg.getDrawingGroupId();
      this.field_3_numShapesSaved++;
      EscherDggRecord.FileIdCluster ficAdd = null;
      int index = 1;

      for (EscherDggRecord.FileIdCluster fic : this.field_5_fileIdClusters) {
         if (fic.getDrawingGroupId() == drawingGroupId && fic.getNumShapeIdsUsed() < 1024) {
            ficAdd = fic;
            break;
         }

         index++;
      }

      if (ficAdd == null) {
         ficAdd = this.addCluster(drawingGroupId, 0, sort);
         this.maxDgId = Math.max(this.maxDgId, drawingGroupId);
      }

      int shapeId = index * 1024 + ficAdd.getNumShapeIdsUsed();
      ficAdd.incrementUsedShapeId();
      dg.setNumShapes(dg.getNumShapes() + 1);
      dg.setLastMSOSPID(shapeId);
      this.field_1_shapeIdMax = Math.max(this.field_1_shapeIdMax, shapeId + 1);
      return shapeId;
   }

   protected Object[][] getAttributeMap() {
      List<Object> fldIds = new ArrayList();
      fldIds.add("FileId Clusters");
      fldIds.add(this.field_5_fileIdClusters.size());

      for (EscherDggRecord.FileIdCluster fic : this.field_5_fileIdClusters) {
         fldIds.add("Group" + fic.field_1_drawingGroupId);
         fldIds.add(fic.field_2_numShapeIdsUsed);
      }

      return new Object[][]{
         {"ShapeIdMax", this.field_1_shapeIdMax},
         {"NumIdClusters", this.getNumIdClusters()},
         {"NumShapesSaved", this.field_3_numShapesSaved},
         {"DrawingsSaved", this.field_4_drawingsSaved},
         fldIds.toArray()
      };
   }

   public static class FileIdCluster {
      private int field_1_drawingGroupId;
      private int field_2_numShapeIdsUsed;

      public FileIdCluster(int drawingGroupId, int numShapeIdsUsed) {
         this.field_1_drawingGroupId = drawingGroupId;
         this.field_2_numShapeIdsUsed = numShapeIdsUsed;
      }

      public int getDrawingGroupId() {
         return this.field_1_drawingGroupId;
      }

      public int getNumShapeIdsUsed() {
         return this.field_2_numShapeIdsUsed;
      }

      private void incrementUsedShapeId() {
         this.field_2_numShapeIdsUsed++;
      }
   }
}
