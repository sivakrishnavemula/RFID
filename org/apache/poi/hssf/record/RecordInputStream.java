package org.apache.poi.hssf.record;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.apache.poi.hssf.record.crypto.Biff8DecryptingStream;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianInputStream;
import org.apache.poi.util.RecordFormatException;

public final class RecordInputStream implements LittleEndianInput {
   public static final short MAX_RECORD_DATA_SIZE = 8224;
   private static final int INVALID_SID_VALUE = -1;
   private static final int MAX_RECORD_LENGTH = 100000;
   private static final int DATA_LEN_NEEDS_TO_BE_READ = -1;
   private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
   private final BiffHeaderInput _bhi;
   private final LittleEndianInput _dataInput;
   private int _currentSid;
   private int _currentDataLength;
   private int _nextSid;
   private int _currentDataOffset;
   private int _markedDataOffset;

   public RecordInputStream(InputStream in) throws RecordFormatException {
      this(in, null, 0);
   }

   public RecordInputStream(InputStream in, EncryptionInfo key, int initialOffset) throws RecordFormatException {
      if (key == null) {
         this._dataInput = getLEI(in);
         this._bhi = new RecordInputStream.SimpleHeaderInput(in);
      } else {
         Biff8DecryptingStream bds = new Biff8DecryptingStream(in, initialOffset, key);
         this._dataInput = bds;
         this._bhi = bds;
      }

      this._nextSid = this.readNextSid();
   }

   static LittleEndianInput getLEI(InputStream is) {
      return (LittleEndianInput)(is instanceof LittleEndianInput ? (LittleEndianInput)is : new LittleEndianInputStream(is));
   }

   public int available() {
      return this.remaining();
   }

   public int read(byte[] b, int off, int len) {
      int limit = Math.min(len, this.remaining());
      if (limit == 0) {
         return 0;
      }

      this.readFully(b, off, limit);
      return limit;
   }

   public short getSid() {
      return (short)this._currentSid;
   }

   public boolean hasNextRecord() throws RecordInputStream.LeftoverDataException {
      if (this._currentDataLength != -1 && this._currentDataLength != this._currentDataOffset) {
         throw new RecordInputStream.LeftoverDataException(this._currentSid, this.remaining());
      }

      if (this._currentDataLength != -1) {
         this._nextSid = this.readNextSid();
      }

      return this._nextSid != -1;
   }

   private int readNextSid() {
      int nAvailable = this._bhi.available();
      if (nAvailable < 4) {
         if (nAvailable > 0) {
         }

         return -1;
      } else {
         int result = this._bhi.readRecordSID();
         if (result == -1) {
            throw new RecordFormatException("Found invalid sid (" + result + ")");
         }

         this._currentDataLength = -1;
         return result;
      }
   }

   public void nextRecord() throws RecordFormatException {
      if (this._nextSid == -1) {
         throw new IllegalStateException("EOF - next record not available");
      }

      if (this._currentDataLength != -1) {
         throw new IllegalStateException("Cannot call nextRecord() without checking hasNextRecord() first");
      }

      this._currentSid = this._nextSid;
      this._currentDataOffset = 0;
      this._currentDataLength = this._bhi.readDataSize();
      if (this._currentDataLength > 8224) {
         throw new RecordFormatException("The content of an excel record cannot exceed 8224 bytes");
      }
   }

   private void checkRecordPosition(int requiredByteCount) {
      int nAvailable = this.remaining();
      if (nAvailable < requiredByteCount) {
         if (nAvailable == 0 && this.isContinueNext()) {
            this.nextRecord();
         } else {
            throw new RecordFormatException("Not enough data (" + nAvailable + ") to read requested (" + requiredByteCount + ") bytes");
         }
      }
   }

   public byte readByte() {
      this.checkRecordPosition(1);
      this._currentDataOffset++;
      return this._dataInput.readByte();
   }

   public short readShort() {
      this.checkRecordPosition(2);
      this._currentDataOffset += 2;
      return this._dataInput.readShort();
   }

   public int readInt() {
      this.checkRecordPosition(4);
      this._currentDataOffset += 4;
      return this._dataInput.readInt();
   }

   public long readLong() {
      this.checkRecordPosition(8);
      this._currentDataOffset += 8;
      return this._dataInput.readLong();
   }

   public int readUByte() {
      return this.readByte() & 0xFF;
   }

   public int readUShort() {
      this.checkRecordPosition(2);
      this._currentDataOffset += 2;
      return this._dataInput.readUShort();
   }

   public double readDouble() {
      long valueLongBits = this.readLong();
      double result = Double.longBitsToDouble(valueLongBits);
      if (Double.isNaN(result)) {
      }

      return result;
   }

   public void readPlain(byte[] buf, int off, int len) {
      this.readFully(buf, 0, buf.length, true);
   }

   public void readFully(byte[] buf) {
      this.readFully(buf, 0, buf.length, false);
   }

   public void readFully(byte[] buf, int off, int len) {
      this.readFully(buf, off, len, false);
   }

   protected void readFully(byte[] buf, int off, int len, boolean isPlain) {
      int origLen = len;
      if (buf == null) {
         throw new NullPointerException();
      }

      if (off >= 0 && len >= 0 && len <= buf.length - off) {
         while (len > 0) {
            int nextChunk = Math.min(this.available(), len);
            if (nextChunk == 0) {
               if (!this.hasNextRecord()) {
                  throw new RecordFormatException(
                     "Can't read the remaining " + len + " bytes of the requested " + origLen + " bytes. No further record exists."
                  );
               }

               this.nextRecord();
               nextChunk = Math.min(this.available(), len);
               assert nextChunk > 0;
            }

            this.checkRecordPosition(nextChunk);
            if (isPlain) {
               this._dataInput.readPlain(buf, off, nextChunk);
            } else {
               this._dataInput.readFully(buf, off, nextChunk);
            }

            this._currentDataOffset += nextChunk;
            off += nextChunk;
            len -= nextChunk;
         }
      } else {
         throw new IndexOutOfBoundsException();
      }
   }

   public String readString() {
      int requestedLength = this.readUShort();
      byte compressFlag = this.readByte();
      return this.readStringCommon(requestedLength, compressFlag == 0);
   }

   public String readUnicodeLEString(int requestedLength) {
      return this.readStringCommon(requestedLength, false);
   }

   public String readCompressedUnicode(int requestedLength) {
      return this.readStringCommon(requestedLength, true);
   }

   private String readStringCommon(int requestedLength, boolean pIsCompressedEncoding) {
      if (requestedLength >= 0 && requestedLength <= 1048576) {
         char[] buf = new char[requestedLength];
         boolean isCompressedEncoding = pIsCompressedEncoding;
         int curLen = 0;

         while (true) {
            int availableChars = isCompressedEncoding ? this.remaining() : this.remaining() / 2;
            if (requestedLength - curLen <= availableChars) {
               while (curLen < requestedLength) {
                  char ch;
                  if (isCompressedEncoding) {
                     ch = (char)this.readUByte();
                  } else {
                     ch = (char)this.readShort();
                  }

                  buf[curLen] = ch;
                  curLen++;
               }

               return new String(buf);
            }

            while (availableChars > 0) {
               char ch;
               if (isCompressedEncoding) {
                  ch = (char)this.readUByte();
               } else {
                  ch = (char)this.readShort();
               }

               buf[curLen] = ch;
               curLen++;
               availableChars--;
            }

            if (!this.isContinueNext()) {
               throw new RecordFormatException(
                  "Expected to find a ContinueRecord in order to read remaining " + (requestedLength - curLen) + " of " + requestedLength + " chars"
               );
            }

            if (this.remaining() != 0) {
               throw new RecordFormatException("Odd number of bytes(" + this.remaining() + ") left behind");
            }

            this.nextRecord();
            byte compressFlag = this.readByte();
            assert compressFlag == 0 || compressFlag == 1;
            isCompressedEncoding = compressFlag == 0;
         }
      } else {
         throw new IllegalArgumentException("Bad requested string length (" + requestedLength + ")");
      }
   }

   public byte[] readRemainder() {
      int size = this.remaining();
      if (size == 0) {
         return EMPTY_BYTE_ARRAY;
      }

      byte[] result = IOUtils.safelyAllocate(size, 100000);
      this.readFully(result);
      return result;
   }

   @Deprecated
   public byte[] readAllContinuedRemainder() {
      ByteArrayOutputStream out = new ByteArrayOutputStream(16448);

      while (true) {
         byte[] b = this.readRemainder();
         out.write(b, 0, b.length);
         if (!this.isContinueNext()) {
            return out.toByteArray();
         }

         this.nextRecord();
      }
   }

   public int remaining() {
      return this._currentDataLength == -1 ? 0 : this._currentDataLength - this._currentDataOffset;
   }

   private boolean isContinueNext() {
      if (this._currentDataLength != -1 && this._currentDataOffset != this._currentDataLength) {
         throw new IllegalStateException("Should never be called before end of current record");
      } else {
         return !this.hasNextRecord() ? false : this._nextSid == 60;
      }
   }

   public int getNextSid() {
      return this._nextSid;
   }

   @Internal
   public void mark(int readlimit) {
      ((InputStream)this._dataInput).mark(readlimit);
      this._markedDataOffset = this._currentDataOffset;
   }

   @Internal
   public void reset() throws IOException {
      ((InputStream)this._dataInput).reset();
      this._currentDataOffset = this._markedDataOffset;
   }

   public static final class LeftoverDataException extends RuntimeException {
      public LeftoverDataException(int sid, int remainingByteCount) {
         super(
            "Initialisation of record 0x"
               + Integer.toHexString(sid).toUpperCase(Locale.ROOT)
               + "("
               + getRecordName(sid)
               + ") left "
               + remainingByteCount
               + " bytes remaining still to be read."
         );
      }

      private static String getRecordName(int sid) {
         Class<? extends Record> recordClass = RecordFactory.getRecordClass(sid);
         return recordClass == null ? null : recordClass.getSimpleName();
      }
   }

   private static final class SimpleHeaderInput implements BiffHeaderInput {
      private final LittleEndianInput _lei;

      public SimpleHeaderInput(InputStream in) {
         this._lei = RecordInputStream.getLEI(in);
      }

      public int available() {
         return this._lei.available();
      }

      public int readDataSize() {
         return this._lei.readUShort();
      }

      public int readRecordSID() {
         return this._lei.readUShort();
      }
   }
}
