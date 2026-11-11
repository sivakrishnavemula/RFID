package org.apache.poi.hssf.record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.poi.util.LittleEndianOutput;

public abstract class PageBreakRecord extends StandardRecord {
   private static final int[] EMPTY_INT_ARRAY = new int[0];
   private List<PageBreakRecord.Break> _breaks;
   private Map<Integer, PageBreakRecord.Break> _breakMap;

   protected PageBreakRecord() {
      this._breaks = new ArrayList();
      this._breakMap = new HashMap();
   }

   public PageBreakRecord(RecordInputStream in) {
      int nBreaks = in.readShort();
      this._breaks = new ArrayList(nBreaks + 2);
      this._breakMap = new HashMap();

      for (int k = 0; k < nBreaks; k++) {
         PageBreakRecord.Break br = new PageBreakRecord.Break(in);
         this._breaks.add(br);
         this._breakMap.put(br.main, br);
      }
   }

   public boolean isEmpty() {
      return this._breaks.isEmpty();
   }

   protected int getDataSize() {
      return 2 + this._breaks.size() * 6;
   }

   public final void serialize(LittleEndianOutput out) {
      int nBreaks = this._breaks.size();
      out.writeShort(nBreaks);

      for (int i = 0; i < nBreaks; i++) {
         ((PageBreakRecord.Break)this._breaks.get(i)).serialize(out);
      }
   }

   public int getNumBreaks() {
      return this._breaks.size();
   }

   public final Iterator<PageBreakRecord.Break> getBreaksIterator() {
      return this._breaks.iterator();
   }

   public String toString() {
      StringBuffer retval = new StringBuffer();
      String label;
      String mainLabel;
      String subLabel;
      if (this.getSid() == 27) {
         label = "HORIZONTALPAGEBREAK";
         mainLabel = "row";
         subLabel = "col";
      } else {
         label = "VERTICALPAGEBREAK";
         mainLabel = "column";
         subLabel = "row";
      }

      retval.append("[" + label + "]").append("\n");
      retval.append("     .sid        =").append(this.getSid()).append("\n");
      retval.append("     .numbreaks =").append(this.getNumBreaks()).append("\n");
      Iterator<PageBreakRecord.Break> iterator = this.getBreaksIterator();

      for (int k = 0; k < this.getNumBreaks(); k++) {
         PageBreakRecord.Break region = (PageBreakRecord.Break)iterator.next();
         retval.append("     .").append(mainLabel).append(" (zero-based) =").append(region.main).append("\n");
         retval.append("     .").append(subLabel).append("From    =").append(region.subFrom).append("\n");
         retval.append("     .").append(subLabel).append("To      =").append(region.subTo).append("\n");
      }

      retval.append("[" + label + "]").append("\n");
      return retval.toString();
   }

   public void addBreak(int main, int subFrom, int subTo) {
      Integer key = main;
      PageBreakRecord.Break region = (PageBreakRecord.Break)this._breakMap.get(key);
      if (region == null) {
         region = new PageBreakRecord.Break(main, subFrom, subTo);
         this._breakMap.put(key, region);
         this._breaks.add(region);
      } else {
         region.main = main;
         region.subFrom = subFrom;
         region.subTo = subTo;
      }
   }

   public final void removeBreak(int main) {
      Integer rowKey = main;
      PageBreakRecord.Break region = (PageBreakRecord.Break)this._breakMap.get(rowKey);
      this._breaks.remove(region);
      this._breakMap.remove(rowKey);
   }

   public final PageBreakRecord.Break getBreak(int main) {
      Integer rowKey = main;
      return (PageBreakRecord.Break)this._breakMap.get(rowKey);
   }

   public final int[] getBreaks() {
      int count = this.getNumBreaks();
      if (count < 1) {
         return EMPTY_INT_ARRAY;
      }

      int[] result = new int[count];

      for (int i = 0; i < count; i++) {
         PageBreakRecord.Break breakItem = (PageBreakRecord.Break)this._breaks.get(i);
         result[i] = breakItem.main;
      }

      return result;
   }

   public static final class Break {
      public static final int ENCODED_SIZE = 6;
      public int main;
      public int subFrom;
      public int subTo;

      public Break(int main, int subFrom, int subTo) {
         this.main = main;
         this.subFrom = subFrom;
         this.subTo = subTo;
      }

      public Break(RecordInputStream in) {
         this.main = in.readUShort() - 1;
         this.subFrom = in.readUShort();
         this.subTo = in.readUShort();
      }

      public void serialize(LittleEndianOutput out) {
         out.writeShort(this.main + 1);
         out.writeShort(this.subFrom);
         out.writeShort(this.subTo);
      }
   }
}
