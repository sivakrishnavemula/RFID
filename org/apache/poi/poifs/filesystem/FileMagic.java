package org.apache.poi.poifs.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LocaleUtil;

public enum FileMagic {
   OLE2(-2226271756974174256L),
   OOXML(POIFSConstants.OOXML_FILE_HEADER),
   XML(POIFSConstants.RAW_XML_FILE_HEADER),
   BIFF2(new byte[]{9, 0, 4, 0, 0, 0, 112, 0}),
   BIFF3(new byte[]{9, 2, 6, 0, 0, 0, 112, 0}),
   BIFF4(new byte[]{9, 4, 6, 0, 0, 0, 112, 0}, new byte[]{9, 4, 6, 0, 0, 0, 0, 1}),
   MSWRITE(new byte[]{49, -66, 0, 0}, new byte[]{50, -66, 0, 0}),
   RTF("{\\rtf"),
   PDF("%PDF"),
   HTML("<!DOCTYP".getBytes(StandardCharsets.UTF_8), "<html".getBytes(StandardCharsets.UTF_8)),
   WORD2(new byte[]{-37, -91, 45, 0}),
   UNKNOWN(new byte[0]);

   final byte[][] magic;

   FileMagic(long magic) {
      this.magic = new byte[1][8];
      LittleEndian.putLong(this.magic[0], 0, magic);
   }

   FileMagic(byte[]... magic) {
      this.magic = magic;
   }

   FileMagic(String magic) {
      this(magic.getBytes(LocaleUtil.CHARSET_1252));
   }

   public static FileMagic valueOf(byte[] magic) {
      for (FileMagic fm : values()) {
         int i = 0;
         boolean found = true;

         for (byte[] ma : fm.magic) {
            for (byte m : ma) {
               byte d = magic[i++];
               if (d != m && (m != 112 || d != 16 && d != 32 && d != 64)) {
                  found = false;
                  break;
               }
            }

            if (found) {
               return fm;
            }
         }
      }

      return UNKNOWN;
   }

   public static FileMagic valueOf(File inp) throws IOException {
      FileInputStream fis = new FileInputStream(inp);
      Throwable var2 = null;

      try {
         byte[] data = IOUtils.toByteArray(fis, 8);
         return valueOf(data);
      } catch (Throwable var13) {
         var2 = var13;
         throw var13;
      } finally {
         if (fis != null) {
            if (var2 != null) {
               try {
                  fis.close();
               } catch (Throwable var12) {
                  var2.addSuppressed(var12);
               }
            } else {
               fis.close();
            }
         }
      }
   }

   public static FileMagic valueOf(InputStream inp) throws IOException {
      if (!inp.markSupported()) {
         throw new IOException("getFileMagic() only operates on streams which support mark(int)");
      }

      byte[] data = IOUtils.peekFirst8Bytes(inp);
      return valueOf(data);
   }

   public static InputStream prepareToCheckMagic(InputStream stream) {
      return (InputStream)(stream.markSupported() ? stream : new BufferedInputStream(stream));
   }
}
