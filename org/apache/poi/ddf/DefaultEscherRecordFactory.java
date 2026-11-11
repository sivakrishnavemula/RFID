package org.apache.poi.ddf;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.util.LittleEndian;

public class DefaultEscherRecordFactory implements EscherRecordFactory {
   private static Class<?>[] escherRecordClasses = new Class[]{
      EscherBSERecord.class,
      EscherOptRecord.class,
      EscherTertiaryOptRecord.class,
      EscherClientAnchorRecord.class,
      EscherDgRecord.class,
      EscherSpgrRecord.class,
      EscherSpRecord.class,
      EscherClientDataRecord.class,
      EscherDggRecord.class,
      EscherSplitMenuColorsRecord.class,
      EscherChildAnchorRecord.class,
      EscherTextboxRecord.class
   };
   private static Map<Short, Constructor<? extends EscherRecord>> recordsMap = recordsToMap(escherRecordClasses);

   public EscherRecord createRecord(byte[] data, int offset) {
      short options = LittleEndian.getShort(data, offset);
      short recordId = LittleEndian.getShort(data, offset + 2);
      if (isContainer(options, recordId)) {
         EscherContainerRecord r = new EscherContainerRecord();
         r.setRecordId(recordId);
         r.setOptions(options);
         return r;
      }

      if (recordId >= -4072 && recordId <= -3817) {
         EscherBlipRecord r;
         if (recordId == -4065 || recordId == -4067 || recordId == -4066) {
            r = new EscherBitmapBlip();
         } else if (recordId != -4070 && recordId != -4069 && recordId != -4068) {
            r = new EscherBlipRecord();
         } else {
            r = new EscherMetafileBlip();
         }

         r.setRecordId(recordId);
         r.setOptions(options);
         return r;
      } else {
         Constructor<? extends EscherRecord> recordConstructor = (Constructor<? extends EscherRecord>)recordsMap.get(recordId);
         if (recordConstructor == null) {
            return new UnknownEscherRecord();
         }

         EscherRecord escherRecord;
         try {
            escherRecord = (EscherRecord)recordConstructor.newInstance(new Object[0]);
         } catch (Exception e) {
            return new UnknownEscherRecord();
         }

         escherRecord.setRecordId(recordId);
         escherRecord.setOptions(options);
         return escherRecord;
      }
   }

   protected static Map<Short, Constructor<? extends EscherRecord>> recordsToMap(Class<?>[] recClasses) {
      Map<Short, Constructor<? extends EscherRecord>> result = new HashMap();
      Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

      for (Class<?> recClass : recClasses) {
         Class<? extends EscherRecord> recCls = (Class<? extends EscherRecord>)recClass;

         short sid;
         try {
            sid = recCls.getField("RECORD_ID").getShort(null);
         } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
         } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
         } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
         }

         Constructor<? extends EscherRecord> constructor;
         try {
            constructor = recCls.getConstructor(EMPTY_CLASS_ARRAY);
         } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
         }

         result.put(sid, constructor);
      }

      return result;
   }

   public static boolean isContainer(short options, short recordId) {
      if (recordId >= -4096 && recordId <= -4091) {
         return true;
      } else {
         return recordId == -4083 ? false : (options & 15) == 15;
      }
   }
}
