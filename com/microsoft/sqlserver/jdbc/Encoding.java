package com.microsoft.sqlserver.jdbc;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

enum Encoding {
   UNICODE("UTF-16LE", true, false),
   CP437("Cp437", false, false),
   CP850("Cp850", false, false),
   CP874("MS874", true, true),
   CP932("MS932", true, false),
   CP936("MS936", true, false),
   CP949("MS949", true, false),
   CP950("MS950", true, false),
   CP1250("Cp1250", true, true),
   CP1251("Cp1251", true, true),
   CP1252("Cp1252", true, true),
   CP1253("Cp1253", true, true),
   CP1254("Cp1254", true, true),
   CP1255("Cp1255", true, true),
   CP1256("Cp1256", true, true),
   CP1257("Cp1257", true, true),
   CP1258("Cp1258", true, true);

   private final String charsetName;
   private final boolean supportsAsciiConversion;
   private final boolean hasAsciiCompatibleSBCS;
   private boolean jvmSupportConfirmed = false;

   Encoding(String var3, boolean var4, boolean var5) {
      this.charsetName = var3;
      this.supportsAsciiConversion = var4;
      this.hasAsciiCompatibleSBCS = var5;
   }

   final Encoding checkSupported() throws UnsupportedEncodingException {
      if (!this.jvmSupportConfirmed) {
         try {
            " ".getBytes(this.charsetName);
         } catch (UnsupportedEncodingException var4) {
            MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_codePageNotSupported"));
            Object[] var3 = new Object[]{this.charsetName};
            throw new UnsupportedEncodingException(var2.format(var3));
         }

         this.jvmSupportConfirmed = true;
      }

      return this;
   }

   final String charsetName() {
      return this.charsetName;
   }

   boolean supportsAsciiConversion() {
      return this.supportsAsciiConversion;
   }

   boolean hasAsciiCompatibleSBCS() {
      return this.hasAsciiCompatibleSBCS;
   }
}
