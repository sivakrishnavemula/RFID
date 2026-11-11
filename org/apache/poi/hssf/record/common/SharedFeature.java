package org.apache.poi.hssf.record.common;

import org.apache.poi.util.LittleEndianOutput;

public interface SharedFeature {
   String toString();

   void serialize(LittleEndianOutput var1);

   int getDataSize();
}
