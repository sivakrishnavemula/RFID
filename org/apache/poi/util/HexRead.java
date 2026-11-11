package org.apache.poi.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HexRead {
   public static byte[] readData(String filename) throws IOException {
      File file = new File(filename);
      InputStream stream = new FileInputStream(file);

      try {
         return readData(stream, -1);
      } finally {
         stream.close();
      }
   }

   public static byte[] readData(InputStream stream, String section) throws IOException {
      try {
         StringBuffer sectionText = new StringBuffer();
         boolean inSection = false;

         for (int c = stream.read(); c != -1; c = stream.read()) {
            switch (c) {
               case 10:
               case 13:
                  inSection = false;
                  sectionText = new StringBuffer();
                  break;
               case 91:
                  inSection = true;
                  break;
               case 93:
                  inSection = false;
                  if (sectionText.toString().equals(section)) {
                     return readData(stream, 91);
                  }

                  sectionText = new StringBuffer();
                  break;
               default:
                  if (inSection) {
                     sectionText.append((char)c);
                  }
            }
         }
      } finally {
         stream.close();
      }

      throw new IOException("Section '" + section + "' not found");
   }

   public static byte[] readData(String filename, String section) throws IOException {
      return readData(new FileInputStream(filename), section);
   }

   public static byte[] readData(InputStream stream, int eofChar) throws IOException {
      int characterCount = 0;
      byte b = 0;
      List<Byte> bytes = new ArrayList();
      char a = 'W';
      char A = '7';

      while (true) {
         int count = stream.read();
         int digitValue = -1;
         if (48 <= count && count <= 57) {
            digitValue = count - 48;
         } else if (65 <= count && count <= 70) {
            digitValue = count - 55;
         } else if (97 <= count && count <= 102) {
            digitValue = count - 87;
         } else if (35 == count) {
            readToEOL(stream);
         } else if (-1 == count || eofChar == count) {
            Byte[] polished = (Byte[])bytes.toArray(new Byte[bytes.size()]);
            byte[] rval = new byte[polished.length];

            for (int j = 0; j < polished.length; j++) {
               rval[j] = polished[j];
            }

            return rval;
         }

         if (digitValue != -1) {
            b = (byte)(b << 4);
            b = (byte)(b + (byte)digitValue);
            if (++characterCount == 2) {
               bytes.add(b);
               characterCount = 0;
               b = 0;
            }
         }
      }
   }

   public static byte[] readFromString(String data) {
      try {
         return readData(new ByteArrayInputStream(data.getBytes(StringUtil.UTF8)), -1);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private static void readToEOL(InputStream stream) throws IOException {
      int c = stream.read();

      while (c != -1 && c != 10 && c != 13) {
         c = stream.read();
      }
   }
}
