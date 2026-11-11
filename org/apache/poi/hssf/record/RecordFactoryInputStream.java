package org.apache.poi.hssf.record;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.util.RecordFormatException;

public final class RecordFactoryInputStream {
   private final RecordInputStream _recStream;
   private final boolean _shouldIncludeContinueRecords;
   private Record[] _unreadRecordBuffer;
   private int _unreadRecordIndex = -1;
   private Record _lastRecord;
   private DrawingRecord _lastDrawingRecord = new DrawingRecord();
   private int _bofDepth;
   private boolean _lastRecordWasEOFLevelZero;

   public RecordFactoryInputStream(InputStream in, boolean shouldIncludeContinueRecords) {
      RecordInputStream rs = new RecordInputStream(in);
      List<Record> records = new ArrayList();
      RecordFactoryInputStream.StreamEncryptionInfo sei = new RecordFactoryInputStream.StreamEncryptionInfo(rs, records);
      if (sei.hasEncryption()) {
         rs = sei.createDecryptingStream(in);
      }

      if (!records.isEmpty()) {
         this._unreadRecordBuffer = new Record[records.size()];
         records.toArray(this._unreadRecordBuffer);
         this._unreadRecordIndex = 0;
      }

      this._recStream = rs;
      this._shouldIncludeContinueRecords = shouldIncludeContinueRecords;
      this._lastRecord = sei.getLastRecord();
      this._bofDepth = sei.hasBOFRecord() ? 1 : 0;
      this._lastRecordWasEOFLevelZero = false;
   }

   public Record nextRecord() {
      Record r = this.getNextUnreadRecord();
      if (r != null) {
         return r;
      }

      while (this._recStream.hasNextRecord()) {
         if (this._lastRecordWasEOFLevelZero && this._recStream.getNextSid() != 2057) {
            return null;
         }

         this._recStream.nextRecord();
         r = this.readNextRecord();
         if (r != null) {
            return r;
         }
      }

      return null;
   }

   private Record getNextUnreadRecord() {
      if (this._unreadRecordBuffer != null) {
         int ix = this._unreadRecordIndex;
         if (ix < this._unreadRecordBuffer.length) {
            Record result = this._unreadRecordBuffer[ix];
            this._unreadRecordIndex = ix + 1;
            return result;
         }

         this._unreadRecordIndex = -1;
         this._unreadRecordBuffer = null;
      }

      return null;
   }

   private Record readNextRecord() {
      Record record = RecordFactory.createSingleRecord(this._recStream);
      this._lastRecordWasEOFLevelZero = false;
      if (record instanceof BOFRecord) {
         this._bofDepth++;
         return record;
      }

      if (record instanceof EOFRecord) {
         this._bofDepth--;
         if (this._bofDepth < 1) {
            this._lastRecordWasEOFLevelZero = true;
         }

         return record;
      } else {
         if (record instanceof DBCellRecord) {
            return null;
         }

         if (record instanceof RKRecord) {
            return RecordFactory.convertToNumberRecord((RKRecord)record);
         }

         if (record instanceof MulRKRecord) {
            Record[] records = RecordFactory.convertRKRecords((MulRKRecord)record);
            this._unreadRecordBuffer = records;
            this._unreadRecordIndex = 1;
            return records[0];
         }

         if (record.getSid() == 235 && this._lastRecord instanceof DrawingGroupRecord) {
            DrawingGroupRecord lastDGRecord = (DrawingGroupRecord)this._lastRecord;
            lastDGRecord.join((AbstractEscherHolderRecord)record);
            return null;
         }

         if (record.getSid() == 60) {
            ContinueRecord contRec = (ContinueRecord)record;
            if (this._lastRecord instanceof ObjRecord || this._lastRecord instanceof TextObjectRecord) {
               this._lastDrawingRecord.processContinueRecord(contRec.getData());
               return this._shouldIncludeContinueRecords ? record : null;
            } else if (this._lastRecord instanceof DrawingGroupRecord) {
               ((DrawingGroupRecord)this._lastRecord).processContinueRecord(contRec.getData());
               return null;
            } else if (this._lastRecord instanceof DrawingRecord) {
               return contRec;
            } else if (this._lastRecord instanceof UnknownRecord) {
               return record;
            } else if (this._lastRecord instanceof EOFRecord) {
               return record;
            } else {
               throw new RecordFormatException("Unhandled Continue Record followining " + this._lastRecord.getClass());
            }
         } else {
            this._lastRecord = record;
            if (record instanceof DrawingRecord) {
               this._lastDrawingRecord = (DrawingRecord)record;
            }

            return record;
         }
      }
   }

   private static final class StreamEncryptionInfo {
      private final int _initialRecordsSize;
      private final FilePassRecord _filePassRec;
      private final Record _lastRecord;
      private final boolean _hasBOFRecord;

      public StreamEncryptionInfo(RecordInputStream rs, List<Record> outputRecs) {
         rs.nextRecord();
         int recSize = 4 + rs.remaining();
         Record rec = RecordFactory.createSingleRecord(rs);
         outputRecs.add(rec);
         FilePassRecord fpr = null;
         if (rec instanceof BOFRecord) {
            this._hasBOFRecord = true;
            if (rs.hasNextRecord()) {
               rs.nextRecord();
               rec = RecordFactory.createSingleRecord(rs);
               recSize += rec.getRecordSize();
               outputRecs.add(rec);
               if (rec instanceof WriteProtectRecord && rs.hasNextRecord()) {
                  rs.nextRecord();
                  rec = RecordFactory.createSingleRecord(rs);
                  recSize += rec.getRecordSize();
                  outputRecs.add(rec);
               }

               if (rec instanceof FilePassRecord) {
                  fpr = (FilePassRecord)rec;
               }

               if (rec instanceof EOFRecord) {
                  throw new IllegalStateException("Nothing between BOF and EOF");
               }
            }
         } else {
            this._hasBOFRecord = false;
         }

         this._initialRecordsSize = recSize;
         this._filePassRec = fpr;
         this._lastRecord = rec;
      }

      public RecordInputStream createDecryptingStream(InputStream original) {
         String userPassword = Biff8EncryptionKey.getCurrentUserPassword();
         if (userPassword == null) {
            userPassword = "VelvetSweatshop";
         }

         EncryptionInfo info = this._filePassRec.getEncryptionInfo();

         try {
            if (!info.getDecryptor().verifyPassword(userPassword)) {
               throw new EncryptedDocumentException(
                  ("VelvetSweatshop".equals(userPassword) ? "Default" : "Supplied") + " password is invalid for salt/verifier/verifierHash"
               );
            }
         } catch (GeneralSecurityException e) {
            throw new EncryptedDocumentException(e);
         }

         return new RecordInputStream(original, info, this._initialRecordsSize);
      }

      public boolean hasEncryption() {
         return this._filePassRec != null;
      }

      public Record getLastRecord() {
         return this._lastRecord;
      }

      public boolean hasBOFRecord() {
         return this._hasBOFRecord;
      }
   }
}
