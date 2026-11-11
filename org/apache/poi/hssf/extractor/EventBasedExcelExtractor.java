package org.apache.poi.hssf.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.POIDocument;
import org.apache.poi.extractor.POIOLE2TextExtractor;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.extractor.ExcelExtractor;

public class EventBasedExcelExtractor extends POIOLE2TextExtractor implements ExcelExtractor {
   private DirectoryNode _dir;
   boolean _includeSheetNames = true;
   boolean _formulasNotResults;

   public EventBasedExcelExtractor(DirectoryNode dir) {
      super((POIDocument)null);
      this._dir = dir;
   }

   public EventBasedExcelExtractor(POIFSFileSystem fs) {
      this(fs.getRoot());
      super.setFilesystem(fs);
   }

   public DocumentSummaryInformation getDocSummaryInformation() {
      throw new IllegalStateException("Metadata extraction not supported in streaming mode, please use ExcelExtractor");
   }

   public SummaryInformation getSummaryInformation() {
      throw new IllegalStateException("Metadata extraction not supported in streaming mode, please use ExcelExtractor");
   }

   public void setIncludeCellComments(boolean includeComments) {
      throw new IllegalStateException("Comment extraction not supported in streaming mode, please use ExcelExtractor");
   }

   public void setIncludeHeadersFooters(boolean includeHeadersFooters) {
      throw new IllegalStateException("Header/Footer extraction not supported in streaming mode, please use ExcelExtractor");
   }

   public void setIncludeSheetNames(boolean includeSheetNames) {
      this._includeSheetNames = includeSheetNames;
   }

   public void setFormulasNotResults(boolean formulasNotResults) {
      this._formulasNotResults = formulasNotResults;
   }

   public String getText() {
      try {
         EventBasedExcelExtractor.TextListener tl = this.triggerExtraction();
         String text = tl._text.toString();
         if (!text.endsWith("\n")) {
            text = text + "\n";
         }

         return text;
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private EventBasedExcelExtractor.TextListener triggerExtraction() throws IOException {
      EventBasedExcelExtractor.TextListener tl = new EventBasedExcelExtractor.TextListener();
      FormatTrackingHSSFListener ft = new FormatTrackingHSSFListener(tl);
      tl._ft = ft;
      HSSFEventFactory factory = new HSSFEventFactory();
      HSSFRequest request = new HSSFRequest();
      request.addListenerForAllRecords(ft);
      factory.processWorkbookEvents(request, this._dir);
      return tl;
   }

   private class TextListener implements HSSFListener {
      FormatTrackingHSSFListener _ft;
      private SSTRecord sstRecord;
      private final List<String> sheetNames;
      final StringBuffer _text = new StringBuffer();
      private int sheetNum = -1;
      private int rowNum;
      private boolean outputNextStringValue;
      private int nextRow = -1;

      public TextListener() {
         this.sheetNames = new ArrayList();
      }

      public void processRecord(Record record) {
         String thisText = null;
         int thisRow = -1;
         switch (record.getSid()) {
            case 6:
               FormulaRecord frec = (FormulaRecord)record;
               thisRow = frec.getRow();
               if (EventBasedExcelExtractor.this._formulasNotResults) {
                  thisText = HSSFFormulaParser.toFormulaString(null, frec.getParsedExpression());
               } else if (frec.hasCachedResultString()) {
                  this.outputNextStringValue = true;
                  this.nextRow = frec.getRow();
               } else {
                  thisText = this._ft.formatNumberDateCell(frec);
               }
               break;
            case 28:
               NoteRecord nrec = (NoteRecord)record;
               thisRow = nrec.getRow();
               break;
            case 133:
               BoundSheetRecord sr = (BoundSheetRecord)record;
               this.sheetNames.add(sr.getSheetname());
               break;
            case 252:
               this.sstRecord = (SSTRecord)record;
               break;
            case 253:
               LabelSSTRecord lsrec = (LabelSSTRecord)record;
               thisRow = lsrec.getRow();
               if (this.sstRecord == null) {
                  throw new IllegalStateException("No SST record found");
               }

               thisText = this.sstRecord.getString(lsrec.getSSTIndex()).toString();
               break;
            case 515:
               NumberRecord numrec = (NumberRecord)record;
               thisRow = numrec.getRow();
               thisText = this._ft.formatNumberDateCell(numrec);
               break;
            case 516:
               LabelRecord lrec = (LabelRecord)record;
               thisRow = lrec.getRow();
               thisText = lrec.getValue();
               break;
            case 519:
               if (this.outputNextStringValue) {
                  StringRecord srec = (StringRecord)record;
                  thisText = srec.getString();
                  thisRow = this.nextRow;
                  this.outputNextStringValue = false;
               }
               break;
            case 2057:
               BOFRecord bof = (BOFRecord)record;
               if (bof.getType() == 16) {
                  this.sheetNum++;
                  this.rowNum = -1;
                  if (EventBasedExcelExtractor.this._includeSheetNames) {
                     if (this._text.length() > 0) {
                        this._text.append("\n");
                     }

                     this._text.append((String)this.sheetNames.get(this.sheetNum));
                  }
               }
         }

         if (thisText != null) {
            if (thisRow != this.rowNum) {
               this.rowNum = thisRow;
               if (this._text.length() > 0) {
                  this._text.append("\n");
               }
            } else {
               this._text.append("\t");
            }

            this._text.append(thisText);
         }
      }
   }
}
