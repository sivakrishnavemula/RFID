package org.apache.poi.hssf.extractor;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.CodepageRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.OldFormulaRecord;
import org.apache.poi.hssf.record.OldLabelRecord;
import org.apache.poi.hssf.record.OldSheetRecord;
import org.apache.poi.hssf.record.OldStringRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentNode;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.poifs.filesystem.NotOLE2FileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.util.IOUtils;

public class OldExcelExtractor implements Closeable {
   private static final int FILE_PASS_RECORD_SID = 47;
   private static final int MAX_RECORD_LENGTH = 100000;
   private RecordInputStream ris;
   private Closeable toClose;
   private int biffVersion;
   private int fileType;

   public OldExcelExtractor(InputStream input) throws IOException {
      this.open(input);
   }

   public OldExcelExtractor(File f) throws IOException {
      POIFSFileSystem poifs = null;

      try {
         poifs = new POIFSFileSystem(f);
         this.open(poifs);
         this.toClose = poifs;
         return;
      } catch (OldExcelFormatException | NotOLE2FileException var9) {
      } finally {
         if (this.toClose == null) {
            IOUtils.closeQuietly(poifs);
         }
      }

      FileInputStream biffStream = new FileInputStream(f);

      try {
         this.open(biffStream);
      } catch (IOException | RuntimeException e) {
         biffStream.close();
         throw e;
      }
   }

   public OldExcelExtractor(POIFSFileSystem fs) throws IOException {
      this.open(fs);
   }

   public OldExcelExtractor(DirectoryNode directory) throws IOException {
      this.open(directory);
   }

   private void open(InputStream biffStream) throws IOException {
      BufferedInputStream bis = biffStream instanceof BufferedInputStream ? (BufferedInputStream)biffStream : new BufferedInputStream(biffStream, 8);
      if (FileMagic.valueOf(bis) == FileMagic.OLE2) {
         POIFSFileSystem poifs = new POIFSFileSystem(bis);

         try {
            this.open(poifs);
            this.toClose = poifs;
         } finally {
            if (this.toClose == null) {
               poifs.close();
            }
         }
      } else {
         this.ris = new RecordInputStream(bis);
         this.toClose = bis;
         this.prepare();
      }
   }

   private void open(POIFSFileSystem fs) throws IOException {
      this.open(fs.getRoot());
   }

   private void open(DirectoryNode directory) throws IOException {
      DocumentNode book;
      try {
         book = (DocumentNode)directory.getEntry("Book");
      } catch (FileNotFoundException e) {
         book = (DocumentNode)directory.getEntry(InternalWorkbook.WORKBOOK_DIR_ENTRY_NAMES[0]);
      }

      if (book == null) {
         throw new IOException("No Excel 5/95 Book stream found");
      }

      this.ris = new RecordInputStream(directory.createDocumentInputStream(book));
      this.prepare();
   }

   public static void main(String[] args) throws IOException {
      if (args.length < 1) {
         System.err.println("Use:");
         System.err.println("   OldExcelExtractor <filename>");
         System.exit(1);
      }

      OldExcelExtractor extractor = new OldExcelExtractor(new File(args[0]));
      System.out.println(extractor.getText());
      extractor.close();
   }

   private void prepare() {
      if (!this.ris.hasNextRecord()) {
         throw new IllegalArgumentException("File contains no records!");
      }

      this.ris.nextRecord();
      int bofSid = this.ris.getSid();
      switch (bofSid) {
         case 9:
            this.biffVersion = 2;
            break;
         case 521:
            this.biffVersion = 3;
            break;
         case 1033:
            this.biffVersion = 4;
            break;
         case 2057:
            this.biffVersion = 5;
            break;
         default:
            throw new IllegalArgumentException("File does not begin with a BOF, found sid of " + bofSid);
      }

      BOFRecord bof = new BOFRecord(this.ris);
      this.fileType = bof.getType();
   }

   public int getBiffVersion() {
      return this.biffVersion;
   }

   public int getFileType() {
      return this.fileType;
   }

   public String getText() {
      StringBuffer text = new StringBuffer();
      CodepageRecord codepage = null;

      while (this.ris.hasNextRecord()) {
         int sid = this.ris.getNextSid();
         this.ris.nextRecord();
         switch (sid) {
            case 4:
            case 516:
               OldLabelRecord lr = new OldLabelRecord(this.ris);
               lr.setCodePage(codepage);
               text.append(lr.getValue());
               text.append('\n');
               break;
            case 6:
            case 518:
            case 1030:
               if (this.biffVersion == 5) {
                  FormulaRecord fr = new FormulaRecord(this.ris);
                  if (fr.getCachedResultType() == CellType.NUMERIC.getCode()) {
                     this.handleNumericCell(text, fr.getValue());
                  }
               } else {
                  OldFormulaRecord fr = new OldFormulaRecord(this.ris);
                  if (fr.getCachedResultType() == CellType.NUMERIC.getCode()) {
                     this.handleNumericCell(text, fr.getValue());
                  }
               }
               break;
            case 7:
            case 519:
               OldStringRecord sr = new OldStringRecord(this.ris);
               sr.setCodePage(codepage);
               text.append(sr.getString());
               text.append('\n');
               break;
            case 47:
               throw new EncryptedDocumentException("Encryption not supported for Old Excel files");
            case 66:
               codepage = new CodepageRecord(this.ris);
               break;
            case 133:
               OldSheetRecord shr = new OldSheetRecord(this.ris);
               shr.setCodePage(codepage);
               text.append("Sheet: ");
               text.append(shr.getSheetname());
               text.append('\n');
               break;
            case 515:
               NumberRecord nr = new NumberRecord(this.ris);
               this.handleNumericCell(text, nr.getValue());
               break;
            case 638:
               RKRecord rr = new RKRecord(this.ris);
               this.handleNumericCell(text, rr.getRKNumber());
               break;
            default:
               this.ris.readFully(IOUtils.safelyAllocate(this.ris.remaining(), 100000));
         }
      }

      this.close();
      this.ris = null;
      return text.toString();
   }

   public void close() {
      if (this.toClose != null) {
         IOUtils.closeQuietly(this.toClose);
         this.toClose = null;
      }
   }

   protected void handleNumericCell(StringBuffer text, double value) {
      text.append(value);
      text.append('\n');
   }
}
