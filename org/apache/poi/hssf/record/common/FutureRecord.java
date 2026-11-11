package org.apache.poi.hssf.record.common;

import org.apache.poi.ss.util.CellRangeAddress;

public interface FutureRecord {
   short getFutureRecordType();

   FtrHeader getFutureHeader();

   CellRangeAddress getAssociatedRange();
}
