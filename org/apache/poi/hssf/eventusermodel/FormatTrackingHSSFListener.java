package org.apache.poi.hssf.eventusermodel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class FormatTrackingHSSFListener implements HSSFListener {
   private static final POILogger logger = POILogFactory.getLogger(FormatTrackingHSSFListener.class);
   private final HSSFListener _childListener;
   private final HSSFDataFormatter _formatter;
   private final NumberFormat _defaultFormat;
   private final Map<Integer, FormatRecord> _customFormatRecords = new HashMap();
   private final List<ExtendedFormatRecord> _xfRecords = new ArrayList();

   public FormatTrackingHSSFListener(HSSFListener childListener) {
      this(childListener, LocaleUtil.getUserLocale());
   }

   public FormatTrackingHSSFListener(HSSFListener childListener, Locale locale) {
      this._childListener = childListener;
      this._formatter = new HSSFDataFormatter(locale);
      this._defaultFormat = NumberFormat.getInstance(locale);
   }

   protected int getNumberOfCustomFormats() {
      return this._customFormatRecords.size();
   }

   protected int getNumberOfExtendedFormats() {
      return this._xfRecords.size();
   }

   public void processRecord(Record record) {
      this.processRecordInternally(record);
      this._childListener.processRecord(record);
   }

   public void processRecordInternally(Record record) {
      if (record instanceof FormatRecord) {
         FormatRecord fr = (FormatRecord)record;
         this._customFormatRecords.put(fr.getIndexCode(), fr);
      }

      if (record instanceof ExtendedFormatRecord) {
         ExtendedFormatRecord xr = (ExtendedFormatRecord)record;
         this._xfRecords.add(xr);
      }
   }

   public String formatNumberDateCell(CellValueRecordInterface cell) {
      double value;
      if (cell instanceof NumberRecord) {
         value = ((NumberRecord)cell).getValue();
      } else {
         if (!(cell instanceof FormulaRecord)) {
            throw new IllegalArgumentException("Unsupported CellValue Record passed in " + cell);
         }

         value = ((FormulaRecord)cell).getValue();
      }

      int formatIndex = this.getFormatIndex(cell);
      String formatString = this.getFormatString(cell);
      return formatString == null ? this._defaultFormat.format(value) : this._formatter.formatRawCellContents(value, formatIndex, formatString);
   }

   public String getFormatString(int formatIndex) {
      String format = null;
      if (formatIndex >= HSSFDataFormat.getNumberOfBuiltinBuiltinFormats()) {
         FormatRecord tfr = (FormatRecord)this._customFormatRecords.get(formatIndex);
         if (tfr == null) {
            logger.log(7, new Object[]{"Requested format at index " + formatIndex + ", but it wasn't found"});
         } else {
            format = tfr.getFormatString();
         }
      } else {
         format = HSSFDataFormat.getBuiltinFormat((short)formatIndex);
      }

      return format;
   }

   public String getFormatString(CellValueRecordInterface cell) {
      int formatIndex = this.getFormatIndex(cell);
      return formatIndex == -1 ? null : this.getFormatString(formatIndex);
   }

   public int getFormatIndex(CellValueRecordInterface cell) {
      ExtendedFormatRecord xfr = (ExtendedFormatRecord)this._xfRecords.get(cell.getXFIndex());
      if (xfr == null) {
         logger.log(7, new Object[]{"Cell " + cell.getRow() + "," + cell.getColumn() + " uses XF with index " + cell.getXFIndex() + ", but we don't have that"});
         return -1;
      } else {
         return xfr.getFormatIndex();
      }
   }
}
