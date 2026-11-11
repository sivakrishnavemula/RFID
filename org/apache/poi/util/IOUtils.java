package org.apache.poi.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.poi.EmptyFileException;
import org.apache.poi.POIDocument;
import org.apache.poi.ss.usermodel.Workbook;

public final class IOUtils {
   private static final POILogger logger = POILogFactory.getLogger(IOUtils.class);
   private static final int SKIP_BUFFER_SIZE = 2048;
   private static int BYTE_ARRAY_MAX_OVERRIDE = -1;
   private static byte[] SKIP_BYTE_BUFFER;

   private IOUtils() {
   }

   public static void setByteArrayMaxOverride(int maxOverride) {
      BYTE_ARRAY_MAX_OVERRIDE = maxOverride;
   }

   public static byte[] peekFirst8Bytes(InputStream stream) throws IOException, EmptyFileException {
      return peekFirstNBytes(stream, 8);
   }

   public static byte[] peekFirstNBytes(InputStream stream, int limit) throws IOException, EmptyFileException {
      stream.mark(limit);
      ByteArrayOutputStream bos = new ByteArrayOutputStream(limit);
      copy(new BoundedInputStream(stream, limit), bos);
      int readBytes = bos.size();
      if (readBytes == 0) {
         throw new EmptyFileException();
      }

      if (readBytes < limit) {
         bos.write(new byte[limit - readBytes]);
      }

      byte[] peekedBytes = bos.toByteArray();
      if (stream instanceof PushbackInputStream) {
         PushbackInputStream pin = (PushbackInputStream)stream;
         pin.unread(peekedBytes, 0, readBytes);
      } else {
         stream.reset();
      }

      return peekedBytes;
   }

   public static byte[] toByteArray(InputStream stream) throws IOException {
      return toByteArray(stream, Integer.MAX_VALUE);
   }

   public static byte[] toByteArray(InputStream stream, int length) throws IOException {
      return toByteArray(stream, length, Integer.MAX_VALUE);
   }

   public static byte[] toByteArray(InputStream stream, long length, int maxLength) throws IOException {
      if (length < 0L || maxLength < 0L) {
         throw new RecordFormatException("Can't allocate an array of length < 0");
      }

      if (length > 2147483647L) {
         throw new RecordFormatException("Can't allocate an array > 2147483647");
      }

      checkLength(length, maxLength);
      int len = Math.min((int)length, maxLength);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(len == Integer.MAX_VALUE ? 4096 : len);
      byte[] buffer = new byte[4096];
      int totalBytes = 0;

      int readBytes;
      do {
         readBytes = stream.read(buffer, 0, Math.min(buffer.length, len - totalBytes));
         totalBytes += Math.max(readBytes, 0);
         if (readBytes > 0) {
            baos.write(buffer, 0, readBytes);
         }
      } while (totalBytes < len && readBytes > -1);

      if (maxLength != Integer.MAX_VALUE && totalBytes == maxLength) {
         throw new IOException("MaxLength (" + maxLength + ") reached - stream seems to be invalid.");
      } else if (len != Integer.MAX_VALUE && totalBytes < len) {
         throw new EOFException("unexpected EOF - expected len: " + len + " - actual len: " + totalBytes);
      } else {
         return baos.toByteArray();
      }
   }

   private static void checkLength(long length, int maxLength) {
      if (BYTE_ARRAY_MAX_OVERRIDE > 0) {
         if (length > BYTE_ARRAY_MAX_OVERRIDE) {
            throwRFE(length, BYTE_ARRAY_MAX_OVERRIDE);
         }
      } else if (length > maxLength) {
         throwRFE(length, maxLength);
      }
   }

   public static byte[] toByteArray(ByteBuffer buffer, int length) {
      if (buffer.hasArray() && buffer.arrayOffset() == 0) {
         return buffer.array();
      }

      byte[] data = new byte[length];
      buffer.get(data);
      return data;
   }

   public static int readFully(InputStream in, byte[] b) throws IOException {
      return readFully(in, b, 0, b.length);
   }

   public static int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
      int total = 0;

      do {
         int got = in.read(b, off + total, len - total);
         if (got < 0) {
            return total == 0 ? -1 : total;
         }

         total += got;
      } while (total != len);

      return total;
   }

   public static int readFully(ReadableByteChannel channel, ByteBuffer b) throws IOException {
      int total = 0;

      do {
         int got = channel.read(b);
         if (got < 0) {
            return total == 0 ? -1 : total;
         }

         total += got;
      } while (total != b.capacity() && b.position() != b.capacity());

      return total;
   }

   @Deprecated
   @Removal(version = "4.2")
   public static void write(POIDocument doc, OutputStream out) throws IOException {
      try {
         doc.write(out);
      } finally {
         closeQuietly(out);
      }
   }

   @Deprecated
   @Removal(version = "4.2")
   public static void write(Workbook doc, OutputStream out) throws IOException {
      try {
         doc.write(out);
      } finally {
         closeQuietly(out);
      }
   }

   @Deprecated
   @Removal(version = "4.2")
   public static void writeAndClose(POIDocument doc, OutputStream out) throws IOException {
      try {
         write(doc, out);
      } finally {
         closeQuietly(doc);
      }
   }

   @Deprecated
   @Removal(version = "4.2")
   public static void writeAndClose(POIDocument doc, File out) throws IOException {
      try {
         doc.write(out);
      } finally {
         closeQuietly(doc);
      }
   }

   @Deprecated
   @Removal(version = "4.2")
   public static void writeAndClose(POIDocument doc) throws IOException {
      try {
         doc.write();
      } finally {
         closeQuietly(doc);
      }
   }

   @Deprecated
   @Removal(version = "4.2")
   public static void writeAndClose(Workbook doc, OutputStream out) throws IOException {
      try {
         doc.write(out);
      } finally {
         closeQuietly(doc);
      }
   }

   public static long copy(InputStream inp, OutputStream out) throws IOException {
      byte[] buff = new byte[4096];
      long totalCount = 0L;

      int count;
      while ((count = inp.read(buff)) != -1) {
         if (count > 0) {
            out.write(buff, 0, count);
         }

         totalCount += count;
      }

      return totalCount;
   }

   public static long copy(InputStream srcStream, File destFile) throws IOException {
      File destDirectory = destFile.getParentFile();
      if (!destDirectory.exists() && !destDirectory.mkdirs()) {
         throw new RuntimeException("Can't create destination directory: " + destDirectory);
      }

      OutputStream destStream = new FileOutputStream(destFile);
      Throwable var4 = null;

      try {
         return copy(srcStream, destStream);
      } catch (Throwable var15) {
         var4 = var15;
         throw var15;
      } finally {
         if (destStream != null) {
            if (var4 != null) {
               try {
                  destStream.close();
               } catch (Throwable var14) {
                  var4.addSuppressed(var14);
               }
            } else {
               destStream.close();
            }
         }
      }
   }

   public static long calculateChecksum(byte[] data) {
      Checksum sum = new CRC32();
      sum.update(data, 0, data.length);
      return sum.getValue();
   }

   public static long calculateChecksum(InputStream stream) throws IOException {
      Checksum sum = new CRC32();
      byte[] buf = new byte[4096];

      int count;
      while ((count = stream.read(buf)) != -1) {
         if (count > 0) {
            sum.update(buf, 0, count);
         }
      }

      return sum.getValue();
   }

   public static void closeQuietly(Closeable closeable) {
      if (closeable != null) {
         try {
            closeable.close();
         } catch (Exception exc) {
            logger.log(7, new Object[]{"Unable to close resource: " + exc, exc});
         }
      }
   }

   public static long skipFully(InputStream input, long toSkip) throws IOException {
      if (toSkip < 0L) {
         throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
      }

      if (toSkip == 0L) {
         return 0L;
      }

      if (SKIP_BYTE_BUFFER == null) {
         SKIP_BYTE_BUFFER = new byte[2048];
      }

      long remain = toSkip;

      while (remain > 0L) {
         long n = input.read(SKIP_BYTE_BUFFER, 0, (int)Math.min(remain, 2048L));
         if (n < 0L) {
            break;
         }

         remain -= n;
      }

      return toSkip == remain ? -1L : toSkip - remain;
   }

   public static byte[] safelyAllocate(long length, int maxLength) {
      if (length < 0L) {
         throw new RecordFormatException("Can't allocate an array of length < 0");
      }

      if (length > 2147483647L) {
         throw new RecordFormatException("Can't allocate an array > 2147483647");
      }

      checkLength(length, maxLength);
      return new byte[(int)length];
   }

   private static void throwRFE(long length, int maxLength) {
      throw new RecordFormatException(
         "Tried to allocate an array of length "
            + length
            + ", but "
            + maxLength
            + " is the maximum for this record type.\nIf the file is not corrupt, please open an issue on bugzilla to request \nincreasing the maximum allowable size for this record type.\nAs a temporary workaround, consider setting a higher override value with IOUtils.setByteArrayMaxOverride()"
      );
   }
}
