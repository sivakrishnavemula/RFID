package org.apache.poi.hssf.eventusermodel;

import org.apache.poi.hssf.record.Record;

public abstract class AbortableHSSFListener implements HSSFListener {
   public void processRecord(Record record) {
   }

   public abstract short abortableProcessRecord(Record var1) throws HSSFUserException;
}
