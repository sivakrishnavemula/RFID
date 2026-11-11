package org.apache.poi.hssf.record;

public interface BiffHeaderInput {
   int readRecordSID();

   int readDataSize();

   int available();
}
