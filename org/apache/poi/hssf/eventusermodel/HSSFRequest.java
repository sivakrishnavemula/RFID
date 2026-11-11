package org.apache.poi.hssf.eventusermodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordFactory;

public class HSSFRequest {
   private final Map<Short, List<HSSFListener>> _records = new HashMap(50);

   public void addListener(HSSFListener lsnr, short sid) {
      List<HSSFListener> list = (List<HSSFListener>)this._records.get(sid);
      if (list == null) {
         list = new ArrayList(1);
         this._records.put(sid, list);
      }

      list.add(lsnr);
   }

   public void addListenerForAllRecords(HSSFListener lsnr) {
      short[] rectypes = RecordFactory.getAllKnownRecordSIDs();

      for (short rectype : rectypes) {
         this.addListener(lsnr, rectype);
      }
   }

   protected short processRecord(Record rec) throws HSSFUserException {
      List<HSSFListener> listeners = (List<HSSFListener>)this._records.get(rec.getSid());
      short userCode = 0;
      if (listeners != null) {
         for (int k = 0; k < listeners.size(); k++) {
            Object listenObj = listeners.get(k);
            if (listenObj instanceof AbortableHSSFListener) {
               AbortableHSSFListener listener = (AbortableHSSFListener)listenObj;
               userCode = listener.abortableProcessRecord(rec);
               if (userCode != 0) {
                  break;
               }
            } else {
               HSSFListener listener = (HSSFListener)listenObj;
               listener.processRecord(rec);
            }
         }
      }

      return userCode;
   }
}
