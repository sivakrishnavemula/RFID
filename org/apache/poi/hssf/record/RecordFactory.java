package org.apache.poi.hssf.record;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.record.chart.BeginRecord;
import org.apache.poi.hssf.record.chart.CatLabRecord;
import org.apache.poi.hssf.record.chart.ChartEndBlockRecord;
import org.apache.poi.hssf.record.chart.ChartEndObjectRecord;
import org.apache.poi.hssf.record.chart.ChartFRTInfoRecord;
import org.apache.poi.hssf.record.chart.ChartRecord;
import org.apache.poi.hssf.record.chart.ChartStartBlockRecord;
import org.apache.poi.hssf.record.chart.ChartStartObjectRecord;
import org.apache.poi.hssf.record.chart.ChartTitleFormatRecord;
import org.apache.poi.hssf.record.chart.DataFormatRecord;
import org.apache.poi.hssf.record.chart.EndRecord;
import org.apache.poi.hssf.record.chart.LegendRecord;
import org.apache.poi.hssf.record.chart.LinkedDataRecord;
import org.apache.poi.hssf.record.chart.SeriesRecord;
import org.apache.poi.hssf.record.chart.SeriesTextRecord;
import org.apache.poi.hssf.record.chart.SeriesToChartGroupRecord;
import org.apache.poi.hssf.record.chart.ValueRangeRecord;
import org.apache.poi.hssf.record.pivottable.DataItemRecord;
import org.apache.poi.hssf.record.pivottable.ExtendedPivotTableViewFieldsRecord;
import org.apache.poi.hssf.record.pivottable.PageItemRecord;
import org.apache.poi.hssf.record.pivottable.StreamIDRecord;
import org.apache.poi.hssf.record.pivottable.ViewDefinitionRecord;
import org.apache.poi.hssf.record.pivottable.ViewFieldsRecord;
import org.apache.poi.hssf.record.pivottable.ViewSourceRecord;
import org.apache.poi.util.RecordFormatException;

public final class RecordFactory {
   private static final int NUM_RECORDS = 512;
   private static final Class<?>[] CONSTRUCTOR_ARGS = new Class[]{RecordInputStream.class};
   private static final Class<? extends Record>[] recordClasses = new Class[]{
      ArrayRecord.class,
      AutoFilterInfoRecord.class,
      BackupRecord.class,
      BlankRecord.class,
      BOFRecord.class,
      BookBoolRecord.class,
      BoolErrRecord.class,
      BottomMarginRecord.class,
      BoundSheetRecord.class,
      CalcCountRecord.class,
      CalcModeRecord.class,
      CFHeaderRecord.class,
      CFHeader12Record.class,
      CFRuleRecord.class,
      CFRule12Record.class,
      ChartRecord.class,
      ChartTitleFormatRecord.class,
      CodepageRecord.class,
      ColumnInfoRecord.class,
      ContinueRecord.class,
      CountryRecord.class,
      CRNCountRecord.class,
      CRNRecord.class,
      DateWindow1904Record.class,
      DBCellRecord.class,
      DConRefRecord.class,
      DefaultColWidthRecord.class,
      DefaultRowHeightRecord.class,
      DeltaRecord.class,
      DimensionsRecord.class,
      DrawingGroupRecord.class,
      DrawingRecord.class,
      DrawingSelectionRecord.class,
      DSFRecord.class,
      DVALRecord.class,
      DVRecord.class,
      EOFRecord.class,
      ExtendedFormatRecord.class,
      ExternalNameRecord.class,
      ExternSheetRecord.class,
      ExtSSTRecord.class,
      FeatRecord.class,
      FeatHdrRecord.class,
      FilePassRecord.class,
      FileSharingRecord.class,
      FnGroupCountRecord.class,
      FontRecord.class,
      FooterRecord.class,
      FormatRecord.class,
      FormulaRecord.class,
      GridsetRecord.class,
      GutsRecord.class,
      HCenterRecord.class,
      HeaderRecord.class,
      HeaderFooterRecord.class,
      HideObjRecord.class,
      HorizontalPageBreakRecord.class,
      HyperlinkRecord.class,
      IndexRecord.class,
      InterfaceEndRecord.class,
      InterfaceHdrRecord.class,
      IterationRecord.class,
      LabelRecord.class,
      LabelSSTRecord.class,
      LeftMarginRecord.class,
      LegendRecord.class,
      MergeCellsRecord.class,
      MMSRecord.class,
      MulBlankRecord.class,
      MulRKRecord.class,
      NameRecord.class,
      NameCommentRecord.class,
      NoteRecord.class,
      NumberRecord.class,
      ObjectProtectRecord.class,
      ObjRecord.class,
      PaletteRecord.class,
      PaneRecord.class,
      PasswordRecord.class,
      PasswordRev4Record.class,
      PrecisionRecord.class,
      PrintGridlinesRecord.class,
      PrintHeadersRecord.class,
      PrintSetupRecord.class,
      ProtectionRev4Record.class,
      ProtectRecord.class,
      RecalcIdRecord.class,
      RefModeRecord.class,
      RefreshAllRecord.class,
      RightMarginRecord.class,
      RKRecord.class,
      RowRecord.class,
      SaveRecalcRecord.class,
      ScenarioProtectRecord.class,
      SelectionRecord.class,
      SeriesRecord.class,
      SeriesTextRecord.class,
      SharedFormulaRecord.class,
      SSTRecord.class,
      StringRecord.class,
      StyleRecord.class,
      SupBookRecord.class,
      TabIdRecord.class,
      TableRecord.class,
      TableStylesRecord.class,
      TextObjectRecord.class,
      TopMarginRecord.class,
      UncalcedRecord.class,
      UseSelFSRecord.class,
      UserSViewBegin.class,
      UserSViewEnd.class,
      ValueRangeRecord.class,
      VCenterRecord.class,
      VerticalPageBreakRecord.class,
      WindowOneRecord.class,
      WindowProtectRecord.class,
      WindowTwoRecord.class,
      WriteAccessRecord.class,
      WriteProtectRecord.class,
      WSBoolRecord.class,
      BeginRecord.class,
      ChartFRTInfoRecord.class,
      ChartStartBlockRecord.class,
      ChartEndBlockRecord.class,
      ChartStartObjectRecord.class,
      ChartEndObjectRecord.class,
      CatLabRecord.class,
      DataFormatRecord.class,
      EndRecord.class,
      LinkedDataRecord.class,
      SeriesToChartGroupRecord.class,
      DataItemRecord.class,
      ExtendedPivotTableViewFieldsRecord.class,
      PageItemRecord.class,
      StreamIDRecord.class,
      ViewDefinitionRecord.class,
      ViewFieldsRecord.class,
      ViewSourceRecord.class
   };
   private static final Map<Integer, RecordFactory.I_RecordCreator> _recordCreatorsById = recordsToMap(recordClasses);
   private static short[] _allKnownRecordSIDs;

   public static Class<? extends Record> getRecordClass(int sid) {
      RecordFactory.I_RecordCreator rc = (RecordFactory.I_RecordCreator)_recordCreatorsById.get(sid);
      return rc == null ? null : rc.getRecordClass();
   }

   public static Record[] createRecord(RecordInputStream in) {
      Record record = createSingleRecord(in);
      if (record instanceof DBCellRecord) {
         return new Record[]{null};
      } else if (record instanceof RKRecord) {
         return new Record[]{convertToNumberRecord((RKRecord)record)};
      } else {
         return (Record[])(record instanceof MulRKRecord ? convertRKRecords((MulRKRecord)record) : new Record[]{record});
      }
   }

   public static Record createSingleRecord(RecordInputStream in) {
      RecordFactory.I_RecordCreator constructor = (RecordFactory.I_RecordCreator)_recordCreatorsById.get(Integer.valueOf(in.getSid()));
      return (Record)(constructor == null ? new UnknownRecord(in) : constructor.create(in));
   }

   public static NumberRecord convertToNumberRecord(RKRecord rk) {
      NumberRecord num = new NumberRecord();
      num.setColumn(rk.getColumn());
      num.setRow(rk.getRow());
      num.setXFIndex(rk.getXFIndex());
      num.setValue(rk.getRKNumber());
      return num;
   }

   public static NumberRecord[] convertRKRecords(MulRKRecord mrk) {
      NumberRecord[] mulRecs = new NumberRecord[mrk.getNumColumns()];

      for (int k = 0; k < mrk.getNumColumns(); k++) {
         NumberRecord nr = new NumberRecord();
         nr.setColumn((short)(k + mrk.getFirstColumn()));
         nr.setRow(mrk.getRow());
         nr.setXFIndex(mrk.getXFAt(k));
         nr.setValue(mrk.getRKNumberAt(k));
         mulRecs[k] = nr;
      }

      return mulRecs;
   }

   public static BlankRecord[] convertBlankRecords(MulBlankRecord mbk) {
      BlankRecord[] mulRecs = new BlankRecord[mbk.getNumColumns()];

      for (int k = 0; k < mbk.getNumColumns(); k++) {
         BlankRecord br = new BlankRecord();
         br.setColumn((short)(k + mbk.getFirstColumn()));
         br.setRow(mbk.getRow());
         br.setXFIndex(mbk.getXFAt(k));
         mulRecs[k] = br;
      }

      return mulRecs;
   }

   public static short[] getAllKnownRecordSIDs() {
      if (_allKnownRecordSIDs == null) {
         short[] results = new short[_recordCreatorsById.size()];
         int i = 0;

         for (Integer sid : _recordCreatorsById.keySet()) {
            results[i++] = sid.shortValue();
         }

         Arrays.sort(results);
         _allKnownRecordSIDs = results;
      }

      return (short[])_allKnownRecordSIDs.clone();
   }

   private static Map<Integer, RecordFactory.I_RecordCreator> recordsToMap(Class<? extends Record>[] records) {
      Map<Integer, RecordFactory.I_RecordCreator> result = new HashMap();
      Set<Class<?>> uniqueRecClasses = new HashSet(records.length * 3 / 2);

      for (Class<? extends Record> recClass : records) {
         if (!Record.class.isAssignableFrom(recClass)) {
            throw new RuntimeException("Invalid record sub-class (" + recClass.getName() + ")");
         }

         if (Modifier.isAbstract(recClass.getModifiers())) {
            throw new RuntimeException("Invalid record class (" + recClass.getName() + ") - must not be abstract");
         }

         if (!uniqueRecClasses.add(recClass)) {
            throw new RuntimeException("duplicate record class (" + recClass.getName() + ")");
         }

         int sid;
         try {
            sid = recClass.getField("sid").getShort(null);
         } catch (Exception illegalArgumentException) {
            throw new RecordFormatException("Unable to determine record types");
         }

         Integer key = sid;
         if (result.containsKey(key)) {
            Class<?> prevClass = ((RecordFactory.I_RecordCreator)result.get(key)).getRecordClass();
            throw new RuntimeException(
               "duplicate record sid 0x"
                  + Integer.toHexString(sid).toUpperCase(Locale.ROOT)
                  + " for classes ("
                  + recClass.getName()
                  + ") and ("
                  + prevClass.getName()
                  + ")"
            );
         }

         result.put(key, getRecordCreator(recClass));
      }

      return result;
   }

   private static RecordFactory.I_RecordCreator getRecordCreator(Class<? extends Record> recClass) {
      try {
         Constructor<? extends Record> constructor = recClass.getConstructor(CONSTRUCTOR_ARGS);
         return new RecordFactory.ReflectionConstructorRecordCreator(constructor);
      } catch (NoSuchMethodException var3) {
         try {
            Method m = recClass.getDeclaredMethod("create", CONSTRUCTOR_ARGS);
            return new RecordFactory.ReflectionMethodRecordCreator(m);
         } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find constructor or create method for (" + recClass.getName() + ").");
         }
      }
   }

   public static List<Record> createRecords(InputStream in) throws RecordFormatException {
      List<Record> records = new ArrayList(512);
      RecordFactoryInputStream recStream = new RecordFactoryInputStream(in, true);

      Record record;
      while ((record = recStream.nextRecord()) != null) {
         records.add(record);
      }

      return records;
   }

   private interface I_RecordCreator {
      Record create(RecordInputStream var1);

      Class<? extends Record> getRecordClass();
   }

   private static final class ReflectionConstructorRecordCreator implements RecordFactory.I_RecordCreator {
      private final Constructor<? extends Record> _c;

      public ReflectionConstructorRecordCreator(Constructor<? extends Record> c) {
         this._c = c;
      }

      @Override
      public Record create(RecordInputStream in) {
         Object[] args = new Object[]{in};

         try {
            return (Record)this._c.newInstance(args);
         } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
         } catch (InstantiationException e) {
            throw new RuntimeException(e);
         } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
         } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof RecordFormatException) {
               throw (RecordFormatException)t;
            } else if (t instanceof EncryptedDocumentException) {
               throw (EncryptedDocumentException)t;
            } else {
               throw new RecordFormatException("Unable to construct record instance", t);
            }
         }
      }

      @Override
      public Class<? extends Record> getRecordClass() {
         return this._c.getDeclaringClass();
      }
   }

   private static final class ReflectionMethodRecordCreator implements RecordFactory.I_RecordCreator {
      private final Method _m;

      public ReflectionMethodRecordCreator(Method m) {
         this._m = m;
      }

      @Override
      public Record create(RecordInputStream in) {
         Object[] args = new Object[]{in};

         try {
            return (Record)this._m.invoke(null, args);
         } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
         } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
         } catch (InvocationTargetException e) {
            throw new RecordFormatException("Unable to construct record instance", e.getTargetException());
         }
      }

      @Override
      public Class<? extends Record> getRecordClass() {
         return this._m.getDeclaringClass();
      }
   }
}
