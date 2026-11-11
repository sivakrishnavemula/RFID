package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;

enum TDSType {
   BIT1(50),
   INT8(127),
   INT4(56),
   INT2(52),
   INT1(48),
   FLOAT4(59),
   FLOAT8(62),
   DATETIME4(58),
   DATETIME8(61),
   MONEY4(122),
   MONEY8(60),
   BITN(104),
   INTN(38),
   DECIMALN(106),
   NUMERICN(108),
   FLOATN(109),
   MONEYN(110),
   DATETIMEN(111),
   GUID(36),
   DATEN(40),
   TIMEN(41),
   DATETIME2N(42),
   DATETIMEOFFSETN(43),
   BIGCHAR(175),
   BIGVARCHAR(167),
   BIGBINARY(173),
   BIGVARBINARY(165),
   NCHAR(239),
   NVARCHAR(231),
   IMAGE(34),
   TEXT(35),
   NTEXT(99),
   UDT(240),
   XML(241),
   SQL_VARIANT(98);

   private final int intValue;
   private static final int MAXELEMENTS = 256;
   private static final TDSType[] valuesTypes = new TDSType[256];

   byte byteValue() {
      return (byte)this.intValue;
   }

   TDSType(int var3) {
      this.intValue = var3;
   }

   static TDSType valueOf(int var0) throws IllegalArgumentException {
      TDSType var1;
      if (0 <= var0 && var0 < valuesTypes.length && null != (var1 = valuesTypes[var0])) {
         return var1;
      }

      MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_unknownSSType"));
      Object[] var3 = new Object[]{new Integer(var0)};
      throw new IllegalArgumentException(var2.format(var3));
   }

   static {
      for (TDSType var3 : values()) {
         valuesTypes[var3.intValue] = var3;
      }
   }
}
