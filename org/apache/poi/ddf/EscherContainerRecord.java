package org.apache.poi.ddf;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public final class EscherContainerRecord extends EscherRecord implements Iterable<EscherRecord> {
   public static final short DGG_CONTAINER = -4096;
   public static final short BSTORE_CONTAINER = -4095;
   public static final short DG_CONTAINER = -4094;
   public static final short SPGR_CONTAINER = -4093;
   public static final short SP_CONTAINER = -4092;
   public static final short SOLVER_CONTAINER = -4091;
   private static final POILogger log = POILogFactory.getLogger(EscherContainerRecord.class);
   private int _remainingLength;
   private final List<EscherRecord> _childRecords = new ArrayList();

   public int fillFields(byte[] data, int pOffset, EscherRecordFactory recordFactory) {
      int bytesRemaining = this.readHeader(data, pOffset);
      int bytesWritten = 8;
      int offset = pOffset + 8;

      while (bytesRemaining > 0 && offset < data.length) {
         EscherRecord child = recordFactory.createRecord(data, offset);
         int childBytesWritten = child.fillFields(data, offset, recordFactory);
         bytesWritten += childBytesWritten;
         offset += childBytesWritten;
         bytesRemaining -= childBytesWritten;
         this.addChildRecord(child);
         if (offset >= data.length && bytesRemaining > 0) {
            this._remainingLength = bytesRemaining;
            if (log.check(5)) {
               log.log(5, new Object[]{"Not enough Escher data: " + bytesRemaining + " bytes remaining but no space left"});
            }
         }
      }

      return bytesWritten;
   }

   public int serialize(int offset, byte[] data, EscherSerializationListener listener) {
      listener.beforeRecordSerialize(offset, this.getRecordId(), this);
      LittleEndian.putShort(data, offset, this.getOptions());
      LittleEndian.putShort(data, offset + 2, this.getRecordId());
      int remainingBytes = 0;

      for (EscherRecord r : this) {
         remainingBytes += r.getRecordSize();
      }

      remainingBytes += this._remainingLength;
      LittleEndian.putInt(data, offset + 4, remainingBytes);
      int pos = offset + 8;

      for (EscherRecord r : this) {
         pos += r.serialize(pos, data, listener);
      }

      listener.afterRecordSerialize(pos, this.getRecordId(), pos - offset, this);
      return pos - offset;
   }

   public int getRecordSize() {
      int childRecordsSize = 0;

      for (EscherRecord r : this) {
         childRecordsSize += r.getRecordSize();
      }

      return 8 + childRecordsSize;
   }

   public boolean hasChildOfType(short recordId) {
      for (EscherRecord r : this) {
         if (r.getRecordId() == recordId) {
            return true;
         }
      }

      return false;
   }

   public EscherRecord getChild(int index) {
      return (EscherRecord)this._childRecords.get(index);
   }

   public List<EscherRecord> getChildRecords() {
      return new ArrayList(this._childRecords);
   }

   public Iterator<EscherRecord> iterator() {
      return Collections.unmodifiableList(this._childRecords).iterator();
   }

   public void setChildRecords(List<EscherRecord> childRecords) {
      if (childRecords == this._childRecords) {
         throw new IllegalStateException("Child records private data member has escaped");
      }

      this._childRecords.clear();
      this._childRecords.addAll(childRecords);
   }

   public boolean removeChildRecord(EscherRecord toBeRemoved) {
      return this._childRecords.remove(toBeRemoved);
   }

   public List<EscherContainerRecord> getChildContainers() {
      List<EscherContainerRecord> containers = new ArrayList();

      for (EscherRecord r : this) {
         if (r instanceof EscherContainerRecord) {
            containers.add((EscherContainerRecord)r);
         }
      }

      return containers;
   }

   public String getRecordName() {
      switch (this.getRecordId()) {
         case -4096:
            return "DggContainer";
         case -4095:
            return "BStoreContainer";
         case -4094:
            return "DgContainer";
         case -4093:
            return "SpgrContainer";
         case -4092:
            return "SpContainer";
         case -4091:
            return "SolverContainer";
         default:
            return "Container 0x" + HexDump.toHex(this.getRecordId());
      }
   }

   public void display(PrintWriter w, int indent) {
      super.display(w, indent);

      for (EscherRecord escherRecord : this) {
         escherRecord.display(w, indent + 1);
      }
   }

   public void addChildRecord(EscherRecord record) {
      this._childRecords.add(record);
   }

   public void addChildBefore(EscherRecord record, int insertBeforeRecordId) {
      int idx = 0;

      for (EscherRecord rec : this) {
         if (rec.getRecordId() == (short)insertBeforeRecordId) {
            break;
         }

         idx++;
      }

      this._childRecords.add(idx, record);
   }

   public <T extends EscherRecord> T getChildById(short recordId) {
      for (EscherRecord childRecord : this) {
         if (childRecord.getRecordId() == recordId) {
            return (T)childRecord;
         }
      }

      return null;
   }

   public void getRecordsById(short recordId, List<EscherRecord> out) {
      for (EscherRecord r : this) {
         if (r instanceof EscherContainerRecord) {
            EscherContainerRecord c = (EscherContainerRecord)r;
            c.getRecordsById(recordId, out);
         } else if (r.getRecordId() == recordId) {
            out.add(r);
         }
      }
   }

   protected Object[][] getAttributeMap() {
      List<Object> chList = new ArrayList(this._childRecords.size() * 2 + 2);
      chList.add("children");
      chList.add(this._childRecords.size());
      int count = 0;

      for (EscherRecord record : this) {
         chList.add("Child " + count);
         chList.add(record);
         count++;
      }

      return new Object[][]{{"isContainer", this.isContainerRecord()}, chList.toArray()};
   }
}
