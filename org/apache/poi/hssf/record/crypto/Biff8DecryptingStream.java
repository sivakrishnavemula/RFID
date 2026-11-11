package org.apache.poi.hssf.record.crypto;

import java.io.InputStream;
import java.io.PushbackInputStream;
import org.apache.poi.hssf.record.BiffHeaderInput;
import org.apache.poi.poifs.crypt.ChunkedCipherInputStream;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.util.SuppressForbidden;

public final class Biff8DecryptingStream implements BiffHeaderInput, LittleEndianInput {
   public static final int RC4_REKEYING_INTERVAL = 1024;
   private static final int MAX_RECORD_LENGTH = 100000;
   private ChunkedCipherInputStream ccis;
   private final byte[] buffer = new byte[8];
   private boolean shouldSkipEncryptionOnCurrentRecord;

   public Biff8DecryptingStream(InputStream in, int initialOffset, EncryptionInfo info) throws RecordFormatException {
      try {
         byte[] initialBuf = IOUtils.safelyAllocate(initialOffset, 100000);
         InputStream stream;
         if (initialOffset == 0) {
            stream = in;
         } else {
            stream = new PushbackInputStream(in, initialOffset);
            ((PushbackInputStream)stream).unread(initialBuf);
         }

         Decryptor dec = info.getDecryptor();
         dec.setChunkSize(1024);
         this.ccis = (ChunkedCipherInputStream)dec.getDataStream(stream, Integer.MAX_VALUE, 0);
         if (initialOffset > 0) {
            this.ccis.readFully(initialBuf);
         }
      } catch (Exception e) {
         throw new RecordFormatException(e);
      }
   }

   @SuppressForbidden("just delegating")
   public int available() {
      return this.ccis.available();
   }

   public int readRecordSID() {
      this.readPlain(this.buffer, 0, 2);
      int sid = LittleEndian.getUShort(this.buffer, 0);
      this.shouldSkipEncryptionOnCurrentRecord = isNeverEncryptedRecord(sid);
      return sid;
   }

   public int readDataSize() {
      this.readPlain(this.buffer, 0, 2);
      int dataSize = LittleEndian.getUShort(this.buffer, 0);
      this.ccis.setNextRecordSize(dataSize);
      return dataSize;
   }

   public double readDouble() {
      long valueLongBits = this.readLong();
      double result = Double.longBitsToDouble(valueLongBits);
      if (Double.isNaN(result)) {
         throw new RuntimeException("Did not expect to read NaN");
      } else {
         return result;
      }
   }

   public void readFully(byte[] buf) {
      this.readFully(buf, 0, buf.length);
   }

   public void readFully(byte[] buf, int off, int len) {
      if (this.shouldSkipEncryptionOnCurrentRecord) {
         this.readPlain(buf, off, buf.length);
      } else {
         this.ccis.readFully(buf, off, len);
      }
   }

   public int readUByte() {
      return this.readByte() & 0xFF;
   }

   public byte readByte() {
      if (this.shouldSkipEncryptionOnCurrentRecord) {
         this.readPlain(this.buffer, 0, 1);
         return this.buffer[0];
      } else {
         return this.ccis.readByte();
      }
   }

   public int readUShort() {
      return this.readShort() & 65535;
   }

   public short readShort() {
      if (this.shouldSkipEncryptionOnCurrentRecord) {
         this.readPlain(this.buffer, 0, 2);
         return LittleEndian.getShort(this.buffer);
      } else {
         return this.ccis.readShort();
      }
   }

   public int readInt() {
      if (this.shouldSkipEncryptionOnCurrentRecord) {
         this.readPlain(this.buffer, 0, 4);
         return LittleEndian.getInt(this.buffer);
      } else {
         return this.ccis.readInt();
      }
   }

   public long readLong() {
      if (this.shouldSkipEncryptionOnCurrentRecord) {
         this.readPlain(this.buffer, 0, 8);
         return LittleEndian.getLong(this.buffer);
      } else {
         return this.ccis.readLong();
      }
   }

   public long getPosition() {
      return this.ccis.getPos();
   }

   public static boolean isNeverEncryptedRecord(int sid) {
      switch (sid) {
         case 47:
         case 225:
         case 2057:
            return true;
         default:
            return false;
      }
   }

   public void readPlain(byte[] b, int off, int len) {
      this.ccis.readPlain(b, off, len);
   }
}
