package org.apache.poi.hpsf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LittleEndianByteArrayInputStream;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class VariantSupport extends Variant {
   public static final int[] SUPPORTED_TYPES = new int[]{0, 2, 3, 20, 5, 64, 30, 31, 71, 11};
   private static final POILogger logger = POILogFactory.getLogger(VariantSupport.class);
   private static final int MAX_RECORD_LENGTH = 100000;
   private static boolean logUnsupportedTypes;
   private static List<Long> unsupportedMessage;
   private static final byte[] paddingBytes = new byte[3];

   public static void setLogUnsupportedTypes(boolean logUnsupportedTypes) {
      VariantSupport.logUnsupportedTypes = logUnsupportedTypes;
   }

   public static boolean isLogUnsupportedTypes() {
      return logUnsupportedTypes;
   }

   protected static void writeUnsupportedTypeMessage(UnsupportedVariantTypeException ex) {
      if (isLogUnsupportedTypes()) {
         if (unsupportedMessage == null) {
            unsupportedMessage = new LinkedList();
         }

         Long vt = ex.getVariantType();
         if (!unsupportedMessage.contains(vt)) {
            logger.log(7, new Object[]{ex.getMessage()});
            unsupportedMessage.add(vt);
         }
      }
   }

   public boolean isSupportedType(int variantType) {
      for (int st : SUPPORTED_TYPES) {
         if (variantType == st) {
            return true;
         }
      }

      return false;
   }

   public static Object read(byte[] src, int offset, int length, long type, int codepage) throws ReadingNotSupportedException, UnsupportedEncodingException {
      LittleEndianByteArrayInputStream lei = new LittleEndianByteArrayInputStream(src, offset);
      return read(lei, length, type, codepage);
   }

   public static Object read(LittleEndianByteArrayInputStream lei, int length, long type, int codepage) throws ReadingNotSupportedException, UnsupportedEncodingException {
      int offset = lei.getReadIndex();
      TypedPropertyValue typedPropertyValue = new TypedPropertyValue((int)type, null);

      try {
         typedPropertyValue.readValue(lei);
      } catch (UnsupportedOperationException exc) {
         try {
            byte[] v = IOUtils.toByteArray(lei, length, 100000);
            throw new ReadingNotSupportedException(type, v);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      switch ((int)type) {
         case 0:
         case 3:
         case 4:
         case 5:
         case 16:
         case 17:
         case 18:
         case 19:
         case 20:
         case 21:
            return typedPropertyValue.getValue();
         case 2:
            return ((Short)typedPropertyValue.getValue()).intValue();
         case 11:
            VariantBool bool = (VariantBool)typedPropertyValue.getValue();
            return bool.getValue();
         case 30:
            CodePageString cpString = (CodePageString)typedPropertyValue.getValue();
            return cpString.getJavaValue(codepage);
         case 31:
            UnicodeString uniString = (UnicodeString)typedPropertyValue.getValue();
            return uniString.toJavaString();
         case 64:
            Filetime filetime = (Filetime)typedPropertyValue.getValue();
            return filetime.getJavaValue();
         case 71:
            ClipboardData clipboardData = (ClipboardData)typedPropertyValue.getValue();
            return clipboardData.toByteArray();
         default:
            int unpadded = lei.getReadIndex() - offset;
            lei.setReadIndex(offset);
            byte[] v = IOUtils.safelyAllocate(unpadded, 100000);
            lei.readFully(v, 0, unpadded);
            throw new ReadingNotSupportedException(type, v);
      }
   }

   public static int write(OutputStream out, long type, Object value, int codepage) throws IOException, WritingNotSupportedException {
      int length = -1;
      switch ((int)type) {
         case 0:
            LittleEndian.putUInt(0L, out);
            length = 4;
            break;
         case 2:
            if (value instanceof Number) {
               LittleEndian.putShort(out, ((Number)value).shortValue());
               length = 2;
            }
            break;
         case 3:
            if (value instanceof Number) {
               LittleEndian.putInt(((Number)value).intValue(), out);
               length = 4;
            }
            break;
         case 4:
            if (value instanceof Number) {
               int floatBits = Float.floatToIntBits(((Number)value).floatValue());
               LittleEndian.putInt(floatBits, out);
               length = 4;
            }
            break;
         case 5:
            if (value instanceof Number) {
               LittleEndian.putDouble(((Number)value).doubleValue(), out);
               length = 8;
            }
            break;
         case 11:
            if (value instanceof Boolean) {
               int bb = (Boolean)value ? 255 : 0;
               out.write(bb);
               out.write(bb);
               length = 2;
            }
            break;
         case 18:
            if (value instanceof Number) {
               LittleEndian.putUShort(((Number)value).intValue(), out);
               length = 2;
            }
            break;
         case 19:
            if (value instanceof Number) {
               LittleEndian.putUInt(((Number)value).longValue(), out);
               length = 4;
            }
            break;
         case 20:
            if (value instanceof Number) {
               LittleEndian.putLong(((Number)value).longValue(), out);
               length = 8;
            }
            break;
         case 21:
            if (value instanceof Number) {
               BigInteger bi = value instanceof BigInteger ? (BigInteger)value : BigInteger.valueOf(((Number)value).longValue());
               if (bi.bitLength() > 64) {
                  throw new WritingNotSupportedException(type, value);
               }

               byte[] biBytesBE = bi.toByteArray();
               byte[] biBytesLE = new byte[8];
               int i = biBytesBE.length;

               for (byte b : biBytesBE) {
                  if (i <= 8) {
                     biBytesLE[i - 1] = b;
                  }

                  i--;
               }

               out.write(biBytesLE);
               length = 8;
            }
            break;
         case 30:
            if (value instanceof String) {
               CodePageString codePageString = new CodePageString();
               codePageString.setJavaValue((String)value, codepage);
               length = codePageString.write(out);
            }
            break;
         case 31:
            if (value instanceof String) {
               UnicodeString uniString = new UnicodeString();
               uniString.setJavaValue((String)value);
               length = uniString.write(out);
            }
            break;
         case 64:
            Filetime filetimeValue = value instanceof Date ? new Filetime((Date)value) : new Filetime();
            length = filetimeValue.write(out);
            break;
         case 71:
            if (value instanceof byte[]) {
               byte[] cf = (byte[])value;
               out.write(cf);
               length = cf.length;
            }
      }

      if (length == -1) {
         if (!(value instanceof byte[])) {
            throw new WritingNotSupportedException(type, value);
         }

         byte[] b = (byte[])value;
         out.write(b);
         length = b.length;
         writeUnsupportedTypeMessage(new WritingNotSupportedException(type, value));
      }

      int padding = 4 - (length & 3) & 3;
      out.write(paddingBytes, 0, padding);
      return length + padding;
   }
}
