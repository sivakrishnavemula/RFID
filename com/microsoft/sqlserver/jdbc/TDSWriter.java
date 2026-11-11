package com.microsoft.sqlserver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import microsoft.sql.DateTimeOffset;

final class TDSWriter {
   private static Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.TDS.Writer");
   private final String traceID;
   private final TDSChannel tdsChannel;
   private final SQLServerConnection con;
   private boolean dataIsLoggable = true;
   private TDSCommand command = null;
   private byte tdsMessageType;
   private volatile int sendResetConnection = 0;
   private int currentPacketSize = 0;
   private static final int TDS_PACKET_HEADER_SIZE = 8;
   private static final byte[] placeholderHeader = new byte[8];
   private byte[] valueBytes = new byte[256];
   private volatile int packetNum = 0;
   private static final int BYTES4 = 4;
   private static final int BYTES8 = 8;
   private static final int BYTES12 = 12;
   private static final int BYTES16 = 16;
   public static final int BIGDECIMAL_MAX_LENGTH = 17;
   private boolean isEOMSent = false;
   private ByteBuffer stagingBuffer;
   private ByteBuffer socketBuffer;
   private ByteBuffer logBuffer;
   private CryptoMetadata cryptoMeta = null;

   public final String toString() {
      return this.traceID;
   }

   void setDataLoggable(boolean var1) {
      this.dataIsLoggable = var1;
   }

   boolean isEOMSent() {
      return this.isEOMSent;
   }

   TDSWriter(TDSChannel var1, SQLServerConnection var2) {
      this.tdsChannel = var1;
      this.con = var2;
      this.traceID = "TDSWriter@" + Integer.toHexString(this.hashCode()) + " (" + var2.toString() + ")";
   }

   void preparePacket() throws SQLServerException {
      if (this.tdsChannel.isLoggingPackets()) {
         Arrays.fill(this.logBuffer.array(), (byte)-2);
         this.logBuffer.clear();
      }

      this.writeBytes(placeholderHeader);
   }

   void writeMessageHeader() throws SQLServerException {
      if (1 == this.tdsMessageType || 14 == this.tdsMessageType || 3 == this.tdsMessageType) {
         boolean var1 = false;
         byte var2 = 22;
         if ((1 == this.tdsMessageType || 3 == this.tdsMessageType)
            && this.con.isDenaliOrLater()
            && !ActivityCorrelator.getCurrent().IsSentToServer()
            && Util.IsActivityTraceOn()) {
            var1 = true;
            var2 += 26;
         }

         this.writeInt(var2);
         this.writeInt(18);
         this.writeShort((short)2);
         this.writeBytes(this.con.getTransactionDescriptor());
         this.writeInt(1);
         if (var1) {
            this.writeInt(26);
            this.writeTraceHeaderData();
            ActivityCorrelator.setCurrentActivityIdSentFlag();
         }
      }
   }

   void writeTraceHeaderData() throws SQLServerException {
      ActivityId var1 = ActivityCorrelator.getCurrent();
      byte[] var2 = Util.asGuidByteArray(var1.getId());
      long var3 = var1.getSequence();
      this.writeShort((short)3);
      this.writeBytes(var2, 0, var2.length);
      this.writeInt((int)var3);
      if (logger.isLoggable(Level.FINER)) {
         logger.finer("Send Trace Header - ActivityID: " + var1.toString());
      }
   }

   void startMessage(TDSCommand var1, byte var2) throws SQLServerException {
      this.command = var1;
      this.tdsMessageType = var2;
      this.packetNum = 0;
      this.isEOMSent = false;
      this.dataIsLoggable = true;
      int var3 = this.con.getTDSPacketSize();
      if (this.currentPacketSize != var3) {
         this.socketBuffer = ByteBuffer.allocate(var3).order(ByteOrder.LITTLE_ENDIAN);
         this.stagingBuffer = ByteBuffer.allocate(var3).order(ByteOrder.LITTLE_ENDIAN);
         this.logBuffer = ByteBuffer.allocate(var3).order(ByteOrder.LITTLE_ENDIAN);
         this.currentPacketSize = var3;
      }

      this.socketBuffer.position(this.socketBuffer.limit());
      this.stagingBuffer.clear();
      this.preparePacket();
      this.writeMessageHeader();
   }

   final void endMessage() throws SQLServerException {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + " Finishing TDS message");
      }

      this.writePacket(1);
   }

   final boolean ignoreMessage() throws SQLServerException {
      if (this.packetNum > 0) {
         assert !this.isEOMSent;
         if (logger.isLoggable(Level.FINER)) {
            logger.finest(this.toString() + " Finishing TDS message by sending ignore bit and end of message");
         }

         this.writePacket(3);
         return true;
      } else {
         return false;
      }
   }

   final void resetPooledConnection() {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + " resetPooledConnection");
      }

      this.sendResetConnection = 8;
   }

   void writeByte(byte var1) throws SQLServerException {
      if (this.stagingBuffer.remaining() >= 1) {
         this.stagingBuffer.put(var1);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.put(var1);
            } else {
               this.logBuffer.position(this.logBuffer.position() + 1);
            }
         }
      } else {
         this.valueBytes[0] = var1;
         this.writeWrappedBytes(this.valueBytes, 1);
      }
   }

   void writeChar(char var1) throws SQLServerException {
      if (this.stagingBuffer.remaining() >= 2) {
         this.stagingBuffer.putChar(var1);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.putChar(var1);
            } else {
               this.logBuffer.position(this.logBuffer.position() + 2);
            }
         }
      } else {
         Util.writeShort((short)var1, this.valueBytes, 0);
         this.writeWrappedBytes(this.valueBytes, 2);
      }
   }

   void writeShort(short var1) throws SQLServerException {
      if (this.stagingBuffer.remaining() >= 2) {
         this.stagingBuffer.putShort(var1);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.putShort(var1);
            } else {
               this.logBuffer.position(this.logBuffer.position() + 2);
            }
         }
      } else {
         Util.writeShort(var1, this.valueBytes, 0);
         this.writeWrappedBytes(this.valueBytes, 2);
      }
   }

   void writeInt(int var1) throws SQLServerException {
      if (this.stagingBuffer.remaining() >= 4) {
         this.stagingBuffer.putInt(var1);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.putInt(var1);
            } else {
               this.logBuffer.position(this.logBuffer.position() + 4);
            }
         }
      } else {
         Util.writeInt(var1, this.valueBytes, 0);
         this.writeWrappedBytes(this.valueBytes, 4);
      }
   }

   void writeReal(Float var1) throws SQLServerException {
      this.writeInt(Float.floatToRawIntBits(var1));
   }

   void writeDouble(double var1) throws SQLServerException {
      if (this.stagingBuffer.remaining() >= 8) {
         this.stagingBuffer.putDouble(var1);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.putDouble(var1);
            } else {
               this.logBuffer.position(this.logBuffer.position() + 8);
            }
         }
      } else {
         long var3 = Double.doubleToLongBits(var1);
         long var5 = 255L;
         byte var7 = 0;

         for (int var8 = 0; var8 < 8; var8++) {
            this.writeByte((byte)((var3 & var5) >> var7));
            var7 += 8;
            var5 <<= 8;
         }
      }
   }

   void writeBigDecimal(BigDecimal var1, int var2, int var3) throws SQLServerException {
      boolean var4 = var1.signum() < 0;
      BigInteger var5 = var1.unscaledValue();
      if (var4) {
         var5 = var5.negate();
      }

      if (9 >= var3) {
         this.writeByte((byte)5);
         this.writeByte((byte)(var4 ? 0 : 1));
         this.writeInt(var5.intValue());
      } else if (19 >= var3) {
         this.writeByte((byte)9);
         this.writeByte((byte)(var4 ? 0 : 1));
         this.writeLong(var5.longValue());
      } else {
         byte var6;
         if (28 >= var3) {
            var6 = 12;
         } else {
            var6 = 16;
         }

         this.writeByte((byte)(var6 + 1));
         this.writeByte((byte)(var4 ? 0 : 1));
         byte[] var7 = var5.toByteArray();
         if (var7.length > var6) {
            MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
            Object[] var13 = new Object[]{JDBCType.of(var2)};
            throw new SQLServerException(var12.format(var13), SQLState.DATA_EXCEPTION_LENGTH_MISMATCH, DriverError.NOT_SET, null);
         }

         byte[] var8 = new byte[var6];
         int var9 = var6 - var7.length;
         int var10 = 0;
         int var11 = var7.length - 1;

         while (var10 < var7.length) {
            var8[var10++] = var7[var11--];
         }

         while (var10 < var9) {
            var8[var10] = 0;
            var10++;
         }

         this.writeBytes(var8);
      }
   }

   void writeSmalldatetime(String var1) throws SQLServerException {
      GregorianCalendar var2 = this.initializeCalender(TimeZone.getDefault());
      long var3 = 0L;
      Timestamp var5 = Timestamp.valueOf(var1);
      var3 = var5.getTime();
      var2.setTimeInMillis(var3);
      int var6 = DDC.daysSinceBaseDate(var2.get(1), var2.get(6), 1900);
      int var7 = 1000 * var2.get(13) + 60000 * var2.get(12) + 3600000 * var2.get(11);
      if (86399999 <= var7) {
         var6++;
         var7 = 0;
      }

      this.writeShort((short)var6);
      int var8 = var7 / 1000;
      int var9 = var8 / 60;
      var9 = var8 % 60 > 29.998 ? var9 + 1 : var9;
      this.writeShort((short)var9);
   }

   void writeDatetime(String var1) throws SQLServerException {
      GregorianCalendar var2 = this.initializeCalender(TimeZone.getDefault());
      long var3 = 0L;
      int var5 = 0;
      Timestamp var6 = Timestamp.valueOf(var1);
      var3 = var6.getTime();
      var5 = var6.getNanos();
      var2.setTimeInMillis(var3);
      int var7 = DDC.daysSinceBaseDate(var2.get(1), var2.get(6), 1900);
      int var8 = (var5 + 500000) / 1000000 + 1000 * var2.get(13) + 60000 * var2.get(12) + 3600000 * var2.get(11);
      if (86399999 <= var8) {
         var7++;
         var8 = 0;
      }

      if (var7 >= DDC.daysSinceBaseDate(1753, 1, 1900) && var7 < DDC.daysSinceBaseDate(10000, 1, 1900)) {
         this.writeInt(var7);
         this.writeInt((3 * var8 + 5) / 10);
      } else {
         MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
         Object[] var10 = new Object[]{SSType.DATETIME};
         throw new SQLServerException(var9.format(var10), SQLState.DATA_EXCEPTION_DATETIME_FIELD_OVERFLOW, DriverError.NOT_SET, null);
      }
   }

   void writeDate(String var1) throws SQLServerException {
      GregorianCalendar var2 = this.initializeCalender(TimeZone.getDefault());
      long var3 = 0L;
      Date var5 = Date.valueOf(var1);
      var3 = var5.getTime();
      var2.setTimeInMillis(var3);
      this.writeScaledTemporal(var2, 0, 0, SSType.DATE);
   }

   void writeTime(Timestamp var1, int var2) throws SQLServerException {
      GregorianCalendar var3 = this.initializeCalender(TimeZone.getDefault());
      long var4 = 0L;
      int var6 = 0;
      var4 = var1.getTime();
      var6 = var1.getNanos();
      var3.setTimeInMillis(var4);
      this.writeScaledTemporal(var3, var6, var2, SSType.TIME);
   }

   void writeDateTimeOffset(Object var1, int var2, SSType var3) throws SQLServerException {
      GregorianCalendar var4 = null;
      TimeZone var5 = TimeZone.getDefault();
      long var6 = 0L;
      int var8 = 0;
      int var9 = 0;
      DateTimeOffset var10 = (DateTimeOffset)var1;
      var6 = var10.getTimestamp().getTime();
      var8 = var10.getTimestamp().getNanos();
      var9 = var10.getMinutesOffset();
      var5 = (TimeZone)(SSType.DATETIMEOFFSET == var3 ? UTC.timeZone : new SimpleTimeZone(var9 * 60 * 1000, ""));
      var4 = new GregorianCalendar(var5, Locale.US);
      var4.setLenient(true);
      var4.clear();
      var4.setTimeInMillis(var6);
      this.writeScaledTemporal(var4, var8, var2, SSType.DATETIMEOFFSET);
      this.writeShort((short)var9);
   }

   void writeOffsetDateTimeWithTimezone(OffsetDateTime var1, int var2) throws SQLServerException {
      GregorianCalendar var3 = null;
      long var5 = 0L;
      int var7 = 0;
      int var8 = 0;

      try {
         var8 = var1.getOffset().getTotalSeconds() / 60;
      } catch (Exception var12) {
         throw new SQLServerException(SQLServerException.getErrString("R_zoneOffsetError"), null, 0, null);
      }

      var7 = var1.getNano();

      for (int var9 = 9 - String.valueOf(var7).length(); var9 > 0; var9--) {
         var7 *= 10;
      }

      TimeZone var4 = UTC.timeZone;
      String var10 = String.format("%04d", new Object[]{var1.getYear()})
         + '-'
         + var1.getMonthValue()
         + '-'
         + var1.getDayOfMonth()
         + ' '
         + var1.getHour()
         + ':'
         + var1.getMinute()
         + ':'
         + var1.getSecond();
      var5 = Timestamp.valueOf(var10).getTime();
      var3 = this.initializeCalender(var4);
      var3.setTimeInMillis(var5);
      int var11 = TimeZone.getDefault().getRawOffset() / 60000;
      if (TimeZone.getDefault().inDaylightTime(var3.getTime())) {
         var11 += TimeZone.getDefault().getDSTSavings() / 60000;
      }

      var11 += var11 < 0 ? var8 * -1 : var8;
      var3.add(12, var11);
      this.writeScaledTemporal(var3, var7, var2, SSType.DATETIMEOFFSET);
      this.writeShort((short)var8);
   }

   void writeOffsetTimeWithTimezone(OffsetTime var1, int var2) throws SQLServerException {
      GregorianCalendar var3 = null;
      long var5 = 0L;
      int var7 = 0;
      int var8 = 0;

      try {
         var8 = var1.getOffset().getTotalSeconds() / 60;
      } catch (Exception var12) {
         throw new SQLServerException(SQLServerException.getErrString("R_zoneOffsetError"), null, 0, null);
      }

      var7 = var1.getNano();

      for (int var9 = 9 - String.valueOf(var7).length(); var9 > 0; var9--) {
         var7 *= 10;
      }

      TimeZone var4 = UTC.timeZone;
      String var10 = "1900-01-01 " + var1.getHour() + ':' + var1.getMinute() + ':' + var1.getSecond();
      var5 = Timestamp.valueOf(var10).getTime();
      var3 = this.initializeCalender(var4);
      var3.setTimeInMillis(var5);
      int var11 = TimeZone.getDefault().getRawOffset() / 60000;
      if (TimeZone.getDefault().inDaylightTime(var3.getTime())) {
         var11 += TimeZone.getDefault().getDSTSavings() / 60000;
      }

      var11 += var11 < 0 ? var8 * -1 : var8;
      var3.add(12, var11);
      this.writeScaledTemporal(var3, var7, var2, SSType.DATETIMEOFFSET);
      this.writeShort((short)var8);
   }

   void writeLong(long var1) throws SQLServerException {
      if (this.stagingBuffer.remaining() >= 8) {
         this.stagingBuffer.putLong(var1);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.putLong(var1);
            } else {
               this.logBuffer.position(this.logBuffer.position() + 8);
            }
         }
      } else {
         this.valueBytes[0] = (byte)(var1 >> 0 & 255L);
         this.valueBytes[1] = (byte)(var1 >> 8 & 255L);
         this.valueBytes[2] = (byte)(var1 >> 16 & 255L);
         this.valueBytes[3] = (byte)(var1 >> 24 & 255L);
         this.valueBytes[4] = (byte)(var1 >> 32 & 255L);
         this.valueBytes[5] = (byte)(var1 >> 40 & 255L);
         this.valueBytes[6] = (byte)(var1 >> 48 & 255L);
         this.valueBytes[7] = (byte)(var1 >> 56 & 255L);
         this.writeWrappedBytes(this.valueBytes, 8);
      }
   }

   void writeBytes(byte[] var1) throws SQLServerException {
      this.writeBytes(var1, 0, var1.length);
   }

   void writeBytes(byte[] var1, int var2, int var3) throws SQLServerException {
      assert var3 <= var1.length;
      int var4 = 0;
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + " Writing " + var3 + " bytes");
      }

      int var5;
      while ((var5 = var3 - var4) > 0) {
         if (0 == this.stagingBuffer.remaining()) {
            this.writePacket(0);
         }

         if (var5 > this.stagingBuffer.remaining()) {
            var5 = this.stagingBuffer.remaining();
         }

         this.stagingBuffer.put(var1, var2 + var4, var5);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.put(var1, var2 + var4, var5);
            } else {
               this.logBuffer.position(this.logBuffer.position() + var5);
            }
         }

         var4 += var5;
      }
   }

   void writeWrappedBytes(byte[] var1, int var2) throws SQLServerException {
      assert var2 <= var1.length;
      assert this.stagingBuffer.remaining() < var2;
      assert var2 <= this.stagingBuffer.capacity();
      int var3 = this.stagingBuffer.remaining();
      if (var3 > 0) {
         this.stagingBuffer.put(var1, 0, var3);
         if (this.tdsChannel.isLoggingPackets()) {
            if (this.dataIsLoggable) {
               this.logBuffer.put(var1, 0, var3);
            } else {
               this.logBuffer.position(this.logBuffer.position() + var3);
            }
         }
      }

      this.writePacket(0);
      this.stagingBuffer.put(var1, var3, var2 - var3);
      if (this.tdsChannel.isLoggingPackets()) {
         if (this.dataIsLoggable) {
            this.logBuffer.put(var1, var3, var2 - var3);
         } else {
            this.logBuffer.position(this.logBuffer.position() + var3);
         }
      }
   }

   void writeString(String var1) throws SQLServerException {
      int var2 = 0;
      int var3 = var1.length();

      while (var2 < var3) {
         int var4 = 2 * (var3 - var2);
         if (var4 > this.valueBytes.length) {
            var4 = this.valueBytes.length;
         }

         int var5 = 0;

         while (var5 < var4) {
            char var6 = var1.charAt(var2++);
            this.valueBytes[var5++] = (byte)(var6 >> 0 & 0xFF);
            this.valueBytes[var5++] = (byte)(var6 >> '\b' & 0xFF);
         }

         this.writeBytes(this.valueBytes, 0, var5);
      }
   }

   void writeStream(InputStream var1, long var2, boolean var4) throws SQLServerException {
      assert -1L == var2 || var2 >= 0L;
      long var5 = 0L;
      byte[] var7 = new byte[4 * this.currentPacketSize];
      int var8 = 0;

      int var9;
      do {
         for (var9 = 0; -1 != var8 && var9 < var7.length; var9 += var8) {
            try {
               var8 = var1.read(var7, var9, var7.length - var9);
            } catch (IOException var13) {
               MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
               Object[] var12 = new Object[]{var13.toString()};
               this.error(var11.format(var12), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET);
            }

            if (-1 == var8) {
               break;
            }

            if (var8 < 0 || var8 > var7.length - var9) {
               MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
               Object[] var15 = new Object[]{SQLServerException.getErrString("R_streamReadReturnedInvalidValue")};
               this.error(var10.format(var15), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET);
            }
         }

         if (var4) {
            this.writeInt(var9);
         }

         this.writeBytes(var7, 0, var9);
         var5 += var9;
      } while (-1 != var8 || var9 > 0);

      if (-1L != var2 && var5 != var2) {
         MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_mismatchedStreamLength"));
         Object[] var16 = new Object[]{var2, var5};
         this.error(var14.format(var16), SQLState.DATA_EXCEPTION_LENGTH_MISMATCH, DriverError.NOT_SET);
      }
   }

   void writeNonUnicodeReader(Reader var1, long var2, boolean var4, String var5) throws SQLServerException {
      assert -1L == var2 || var2 >= 0L;
      long var6 = 0L;
      char[] var8 = new char[this.currentPacketSize];
      byte[] var9 = new byte[this.currentPacketSize];
      int var10 = 0;

      int var11;
      do {
         for (var11 = 0; -1 != var10 && var11 < var8.length; var11 += var10) {
            try {
               var10 = var1.read(var8, var11, var8.length - var11);
            } catch (IOException var17) {
               MessageFormat var15 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
               Object[] var16 = new Object[]{var17.toString()};
               this.error(var15.format(var16), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET);
            }

            if (-1 == var10) {
               break;
            }

            if (var10 < 0 || var10 > var8.length - var11) {
               MessageFormat var14 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
               Object[] var22 = new Object[]{SQLServerException.getErrString("R_streamReadReturnedInvalidValue")};
               this.error(var14.format(var22), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET);
            }
         }

         if (var4) {
            int var12 = var11;
            if (0 != var11) {
               var12 = var11 / 2;
            }

            String var13 = new String(var8);
            byte[] var20 = ParameterUtils.HexToBin(var13.trim());
            this.writeInt(var12);
            this.writeBytes(var20, 0, var12);
         } else {
            this.writeInt(var11);

            for (int var19 = 0; var19 < var11; var19++) {
               try {
                  if (null == var5) {
                     var9[var19] = (byte)(var8[var19] & 0xFF);
                  } else {
                     var9[var19] = new String(var8[var19] + "").getBytes(var5)[0];
                  }
               } catch (UnsupportedEncodingException var18) {
                  throw new SQLServerException(SQLServerException.getErrString("R_encodingErrorWritingTDS"), var18);
               }
            }

            this.writeBytes(var9, 0, var11);
         }

         var6 += var11;
      } while (-1 != var10 || var11 > 0);

      if (-1L != var2 && var6 != var2) {
         MessageFormat var21 = new MessageFormat(SQLServerException.getErrString("R_mismatchedStreamLength"));
         Object[] var23 = new Object[]{var2, var6};
         this.error(var21.format(var23), SQLState.DATA_EXCEPTION_LENGTH_MISMATCH, DriverError.NOT_SET);
      }
   }

   void writeReader(Reader var1, long var2, boolean var4) throws SQLServerException {
      assert -1L == var2 || var2 >= 0L;
      long var5 = 0L;
      char[] var7 = new char[2 * this.currentPacketSize];
      byte[] var8 = new byte[4 * this.currentPacketSize];
      int var9 = 0;

      int var10;
      do {
         for (var10 = 0; -1 != var9 && var10 < var7.length; var10 += var9) {
            try {
               var9 = var1.read(var7, var10, var7.length - var10);
            } catch (IOException var14) {
               MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
               Object[] var13 = new Object[]{var14.toString()};
               this.error(var12.format(var13), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET);
            }

            if (-1 == var9) {
               break;
            }

            if (var9 < 0 || var9 > var7.length - var10) {
               MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
               Object[] var17 = new Object[]{SQLServerException.getErrString("R_streamReadReturnedInvalidValue")};
               this.error(var11.format(var17), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET);
            }
         }

         if (var4) {
            this.writeInt(2 * var10);
         }

         for (int var15 = 0; var15 < var10; var15++) {
            var8[2 * var15] = (byte)(var7[var15] >> 0 & 0xFF);
            var8[2 * var15 + 1] = (byte)(var7[var15] >> '\b' & 0xFF);
         }

         this.writeBytes(var8, 0, 2 * var10);
         var5 += var10;
      } while (-1 != var9 || var10 > 0);

      if (-1L != var2 && var5 != var2) {
         MessageFormat var16 = new MessageFormat(SQLServerException.getErrString("R_mismatchedStreamLength"));
         Object[] var18 = new Object[]{var2, var5};
         this.error(var16.format(var18), SQLState.DATA_EXCEPTION_LENGTH_MISMATCH, DriverError.NOT_SET);
      }
   }

   GregorianCalendar initializeCalender(TimeZone var1) {
      GregorianCalendar var2 = null;
      var2 = new GregorianCalendar(var1, Locale.US);
      var2.setLenient(true);
      var2.clear();
      return var2;
   }

   final void error(String var1, SQLState var2, DriverError var3) throws SQLServerException {
      assert null != this.command;
      this.command.interrupt(var1);
      throw new SQLServerException(var1, var2, var3, null);
   }

   final boolean sendAttention() throws SQLServerException {
      if (this.packetNum > 0) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this + ": sending attention...");
         }

         this.tdsChannel.numMsgsSent++;
         this.startMessage(this.command, (byte)6);
         this.endMessage();
         return true;
      } else {
         return false;
      }
   }

   private final void writePacket(int var1) throws SQLServerException {
      boolean var2 = 1 == (1 & var1);
      boolean var3 = 6 == this.tdsMessageType || (var1 & 2) == 2;
      if (null != this.command && !var3) {
         this.command.checkForInterrupt();
      }

      this.writePacketHeader(var1 | this.sendResetConnection);
      this.sendResetConnection = 0;
      this.flush(var2);
      if (var2) {
         this.flush(var2);
         this.isEOMSent = true;
         this.tdsChannel.numMsgsSent++;
      }

      if (16 == this.tdsMessageType && 1 == this.packetNum && 0 == this.con.getNegotiatedEncryptionLevel()) {
         this.tdsChannel.disableSSL();
      }

      if (null != this.command && !var3 && var2) {
         this.command.onRequestComplete();
      }
   }

   private final void writePacketHeader(int var1) {
      int var2 = this.stagingBuffer.position();
      this.packetNum++;
      this.stagingBuffer.put(0, this.tdsMessageType);
      this.stagingBuffer.put(1, (byte)var1);
      this.stagingBuffer.put(2, (byte)(var2 >> 8 & 0xFF));
      this.stagingBuffer.put(3, (byte)(var2 >> 0 & 0xFF));
      this.stagingBuffer.put(4, (byte)(this.tdsChannel.getSPID() >> 8 & 0xFF));
      this.stagingBuffer.put(5, (byte)(this.tdsChannel.getSPID() >> 0 & 0xFF));
      this.stagingBuffer.put(6, (byte)(this.packetNum % 256));
      this.stagingBuffer.put(7, (byte)0);
      if (this.tdsChannel.isLoggingPackets()) {
         this.logBuffer.put(0, this.tdsMessageType);
         this.logBuffer.put(1, (byte)var1);
         this.logBuffer.put(2, (byte)(var2 >> 8 & 0xFF));
         this.logBuffer.put(3, (byte)(var2 >> 0 & 0xFF));
         this.logBuffer.put(4, (byte)(this.tdsChannel.getSPID() >> 8 & 0xFF));
         this.logBuffer.put(5, (byte)(this.tdsChannel.getSPID() >> 0 & 0xFF));
         this.logBuffer.put(6, (byte)(this.packetNum % 256));
         this.logBuffer.put(7, (byte)0);
      }
   }

   void flush(boolean var1) throws SQLServerException {
      this.tdsChannel.write(this.socketBuffer.array(), this.socketBuffer.position(), this.socketBuffer.remaining());
      this.socketBuffer.position(this.socketBuffer.limit());
      if (this.stagingBuffer.position() >= 8) {
         ByteBuffer var2 = this.stagingBuffer;
         this.stagingBuffer = this.socketBuffer;
         this.socketBuffer = var2;
         this.socketBuffer.flip();
         this.stagingBuffer.clear();
         if (this.tdsChannel.isLoggingPackets()) {
            this.tdsChannel
               .logPacket(this.logBuffer.array(), 0, this.socketBuffer.limit(), this.toString() + " sending packet (" + this.socketBuffer.limit() + " bytes)");
         }

         if (!var1) {
            this.preparePacket();
         }

         this.tdsChannel.write(this.socketBuffer.array(), this.socketBuffer.position(), this.socketBuffer.remaining());
         this.socketBuffer.position(this.socketBuffer.limit());
      }
   }

   void writeRPCNameValType(String var1, boolean var2, TDSType var3) throws SQLServerException {
      int var4 = 0;
      if (null != var1) {
         var4 = var1.length() + 1;
      }

      this.writeByte((byte)var4);
      if (var4 > 0) {
         this.writeChar('@');
         this.writeString(var1);
      }

      if (null != this.cryptoMeta) {
         this.writeByte((byte)(var2 ? 9 : 8));
      } else {
         this.writeByte((byte)(var2 ? 1 : 0));
      }

      this.writeByte(var3.byteValue());
   }

   void writeRPCBit(String var1, Boolean var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.BITN);
      this.writeByte((byte)1);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)1);
         this.writeByte((byte)(var2 ? 1 : 0));
      }
   }

   void writeRPCByte(String var1, Byte var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.INTN);
      this.writeByte((byte)1);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)1);
         this.writeByte(var2);
      }
   }

   void writeRPCShort(String var1, Short var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.INTN);
      this.writeByte((byte)2);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)2);
         this.writeShort(var2);
      }
   }

   void writeRPCInt(String var1, Integer var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.INTN);
      this.writeByte((byte)4);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)4);
         this.writeInt(var2);
      }
   }

   void writeRPCLong(String var1, Long var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.INTN);
      this.writeByte((byte)8);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)8);
         this.writeLong(var2);
      }
   }

   void writeRPCReal(String var1, Float var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.FLOATN);
      if (null == var2) {
         this.writeByte((byte)4);
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)4);
         this.writeByte((byte)4);
         this.writeInt(Float.floatToRawIntBits(var2));
      }
   }

   void writeRPCDouble(String var1, Double var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.FLOATN);
      byte var4 = 8;
      this.writeByte((byte)var4);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)var4);
         long var5 = Double.doubleToLongBits(var2);
         long var7 = 255L;
         byte var9 = 0;

         for (int var10 = 0; var10 < 8; var10++) {
            this.writeByte((byte)((var5 & var7) >> var9));
            var9 += 8;
            var7 <<= 8;
         }
      }
   }

   void writeRPCBigDecimal(String var1, BigDecimal var2, int var3, boolean var4) throws SQLServerException {
      this.writeRPCNameValType(var1, var4, TDSType.DECIMALN);
      this.writeByte((byte)17);
      this.writeByte((byte)38);
      byte[] var5 = DDC.convertBigDecimalToBytes(var2, var3);
      this.writeBytes(var5, 0, var5.length);
   }

   void writeVMaxHeader(long var1, boolean var3, SQLCollation var4) throws SQLServerException {
      this.writeShort((short)-1);
      if (null != var4) {
         var4.writeCollation(this);
      }

      if (var3) {
         this.writeLong(-1L);
      } else if (-1L == var1) {
         this.writeLong(-2L);
      } else {
         this.writeLong(var1);
      }
   }

   void writeRPCStringUnicode(String var1) throws SQLServerException {
      this.writeRPCStringUnicode(null, var1, false, null);
   }

   void writeRPCStringUnicode(String var1, String var2, boolean var3, SQLCollation var4) throws SQLServerException {
      boolean var5 = var2 == null;
      int var6 = var5 ? 0 : 2 * var2.length();
      boolean var7 = var6 <= 8000;
      if (null == var4) {
         var4 = this.con.getDatabaseCollation();
      }

      boolean var8 = !var7 || var3;
      if (var8) {
         this.writeRPCNameValType(var1, var3, TDSType.NVARCHAR);
         this.writeVMaxHeader(var6, var5, var4);
         if (!var5) {
            if (var6 > 0) {
               this.writeInt(var6);
               this.writeString(var2);
            }

            this.writeInt(0);
         }
      } else {
         if (var7) {
            this.writeRPCNameValType(var1, var3, TDSType.NVARCHAR);
            this.writeShort((short)8000);
         } else {
            this.writeRPCNameValType(var1, var3, TDSType.NTEXT);
            this.writeInt(Integer.MAX_VALUE);
         }

         var4.writeCollation(this);
         if (var5) {
            this.writeShort((short)-1);
         } else {
            if (var7) {
               this.writeShort((short)var6);
            } else {
               this.writeInt(var6);
            }

            if (0 != var6) {
               this.writeString(var2);
            }
         }
      }
   }

   void writeTVP(TVP var1) throws SQLServerException {
      if (!var1.isNull()) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)2);
      }

      this.writeByte((byte)-13);
      if (null != var1.getDbNameTVP()) {
         this.writeByte((byte)var1.getDbNameTVP().length());
         this.writeString(var1.getDbNameTVP());
      } else {
         this.writeByte((byte)0);
      }

      if (null != var1.getOwningSchemaNameTVP()) {
         this.writeByte((byte)var1.getOwningSchemaNameTVP().length());
         this.writeString(var1.getOwningSchemaNameTVP());
      } else {
         this.writeByte((byte)0);
      }

      if (null != var1.getTVPName()) {
         this.writeByte((byte)var1.getTVPName().length());
         this.writeString(var1.getTVPName());
      } else {
         this.writeByte((byte)0);
      }

      if (!var1.isNull()) {
         this.writeTVPColumnMetaData(var1);
         this.writeTvpOrderUnique(var1);
      } else {
         this.writeShort((short)-1);
      }

      this.writeByte((byte)0);

      try {
         this.writeTVPRows(var1);
      } catch (NumberFormatException var3) {
         throw new SQLServerException(SQLServerException.getErrString("R_TVPInvalidColumnValue"), var3);
      } catch (ClassCastException var4) {
         throw new SQLServerException(SQLServerException.getErrString("R_TVPInvalidColumnValue"), var4);
      }
   }

   void writeTVPRows(TVP var1) throws SQLServerException {
      if (!var1.isNull()) {
         Map var5 = var1.getColumnMetadata();

         while (var1.next()) {
            Object[] var7 = var1.getRowData();
            this.writeByte((byte)1);
            Iterator var6 = var5.entrySet().iterator();
            int var8 = 0;

            while (var6.hasNext()) {
               Entry var9 = (Entry)var6.next();
               if (((SQLServerMetaData)var9.getValue()).useServerDefault) {
                  var8++;
               } else {
                  JDBCType var10 = JDBCType.of(((SQLServerMetaData)var9.getValue()).javaSqlType);
                  String var11 = null;
                  Object var12 = null;
                  if (null != var7 && var7.length > var8) {
                     var12 = var7[var8];
                     if (null != var12) {
                        var11 = String.valueOf(var12);
                     }
                  }

                  switch (var10) {
                     case BIGINT:
                        if (null == var11) {
                           this.writeByte((byte)0);
                        } else {
                           this.writeByte((byte)8);
                           this.writeLong(Long.valueOf(var11));
                        }
                        break;
                     case BIT:
                        if (null == var11) {
                           this.writeByte((byte)0);
                        } else {
                           this.writeByte((byte)1);
                           this.writeByte((byte)(Boolean.valueOf(var11) ? 1 : 0));
                        }
                        break;
                     case INTEGER:
                        if (null == var11) {
                           this.writeByte((byte)0);
                        } else {
                           this.writeByte((byte)4);
                           this.writeInt(Integer.valueOf(var11));
                        }
                        break;
                     case SMALLINT:
                     case TINYINT:
                        if (null == var11) {
                           this.writeByte((byte)0);
                        } else {
                           this.writeByte((byte)2);
                           this.writeShort(Short.valueOf(var11));
                        }
                        break;
                     case DECIMAL:
                     case NUMERIC:
                        if (null == var11) {
                           this.writeByte((byte)0);
                        } else {
                           this.writeByte((byte)17);
                           BigDecimal var22 = new BigDecimal(var11);
                           var22 = var22.setScale(((SQLServerMetaData)var9.getValue()).scale, RoundingMode.HALF_UP);
                           byte[] var14 = DDC.convertBigDecimalToBytes(var22, var22.scale());
                           byte[] var24 = new byte[17];
                           System.arraycopy(var14, 2, var24, 0, var14.length - 2);
                           this.writeBytes(var24);
                        }
                        break;
                     case DOUBLE:
                        if (null == var11) {
                           this.writeByte((byte)0);
                        } else {
                           this.writeByte((byte)8);
                           long var13 = Double.doubleToLongBits(Double.valueOf(var11));
                           long var15 = 255L;
                           byte var17 = 0;

                           for (int var18 = 0; var18 < 8; var18++) {
                              this.writeByte((byte)((var13 & var15) >> var17));
                              var17 += 8;
                              var15 <<= 8;
                           }
                        }
                        break;
                     case FLOAT:
                     case REAL:
                        if (null == var11) {
                           this.writeByte((byte)0);
                        } else {
                           this.writeByte((byte)4);
                           this.writeInt(Float.floatToRawIntBits(Float.valueOf(var11)));
                        }
                        break;
                     case DATE:
                     case TIME:
                     case TIMESTAMP:
                     case DATETIMEOFFSET:
                     case TIMESTAMP_WITH_TIMEZONE:
                     case TIME_WITH_TIMEZONE:
                     case CHAR:
                     case VARCHAR:
                     case NCHAR:
                     case NVARCHAR:
                        boolean var19 = 2 * ((SQLServerMetaData)var9.getValue()).precision <= 8000;
                        boolean var20 = null == var11;
                        int var21 = var20 ? 0 : var11.length() * 2;
                        if (!var19) {
                           if (var20) {
                              this.writeLong(-1L);
                           } else if (-1 == var21) {
                              this.writeLong(-2L);
                           } else {
                              this.writeLong(var21);
                           }

                           if (!var20) {
                              if (var21 > 0) {
                                 this.writeInt(var21);
                                 this.writeString(var11);
                              }

                              this.writeInt(0);
                           }
                        } else if (var20) {
                           this.writeShort((short)-1);
                        } else {
                           this.writeShort((short)var21);
                           this.writeString(var11);
                        }
                        break;
                     case BINARY:
                     case VARBINARY:
                        boolean var2 = ((SQLServerMetaData)var9.getValue()).precision <= 8000;
                        boolean var3 = null == var12;
                        int var4;
                        if (var12 instanceof String) {
                           var4 = var3 ? 0 : toByteArray(var12.toString()).length;
                        } else {
                           var4 = var3 ? 0 : ((byte[])var12).length;
                        }

                        if (!var2) {
                           if (var3) {
                              this.writeLong(-1L);
                           } else if (-1 == var4) {
                              this.writeLong(-2L);
                           } else {
                              this.writeLong(var4);
                           }

                           if (!var3) {
                              if (var4 > 0) {
                                 this.writeInt(var4);
                                 if (var12 instanceof String) {
                                    this.writeBytes(toByteArray(var12.toString()));
                                 } else {
                                    this.writeBytes((byte[])var12);
                                 }
                              }

                              this.writeInt(0);
                           }
                        } else if (var3) {
                           this.writeShort((short)-1);
                        } else {
                           this.writeShort((short)var4);
                           if (var12 instanceof String) {
                              this.writeBytes(toByteArray(var12.toString()));
                           } else {
                              this.writeBytes((byte[])var12);
                           }
                        }
                        break;
                     default:
                        assert false : "Unexpected JDBC type " + var10.toString();
                  }

                  var8++;
               }
            }
         }
      }

      this.writeByte((byte)0);
   }

   private static byte[] toByteArray(String var0) {
      return DatatypeConverter.parseHexBinary(var0);
   }

   void writeTVPColumnMetaData(TVP var1) throws SQLServerException {
      this.writeShort((short)var1.getTVPColumnCount());
      Map var3 = var1.getColumnMetadata();

      for (Entry var5 : var3.entrySet()) {
         JDBCType var6 = JDBCType.of(((SQLServerMetaData)var5.getValue()).javaSqlType);
         boolean var7 = ((SQLServerMetaData)var5.getValue()).useServerDefault;
         this.writeInt(0);
         short var8 = 1;
         if (var7) {
            var8 = (short)(var8 | 512);
         }

         this.writeShort(var8);
         switch (var6) {
            case BIGINT:
               this.writeByte(TDSType.INTN.byteValue());
               this.writeByte((byte)8);
               break;
            case BIT:
               this.writeByte(TDSType.BITN.byteValue());
               this.writeByte((byte)1);
               break;
            case INTEGER:
               this.writeByte(TDSType.INTN.byteValue());
               this.writeByte((byte)4);
               break;
            case SMALLINT:
            case TINYINT:
               this.writeByte(TDSType.INTN.byteValue());
               this.writeByte((byte)2);
               break;
            case DECIMAL:
            case NUMERIC:
               this.writeByte(TDSType.NUMERICN.byteValue());
               this.writeByte((byte)17);
               this.writeByte((byte)((SQLServerMetaData)var5.getValue()).precision);
               this.writeByte((byte)((SQLServerMetaData)var5.getValue()).scale);
               break;
            case DOUBLE:
               this.writeByte(TDSType.FLOATN.byteValue());
               this.writeByte((byte)8);
               break;
            case FLOAT:
            case REAL:
               this.writeByte(TDSType.FLOATN.byteValue());
               this.writeByte((byte)4);
               break;
            case DATE:
            case TIME:
            case TIMESTAMP:
            case DATETIMEOFFSET:
            case TIMESTAMP_WITH_TIMEZONE:
            case TIME_WITH_TIMEZONE:
            case CHAR:
            case VARCHAR:
            case NCHAR:
            case NVARCHAR:
               this.writeByte(TDSType.NVARCHAR.byteValue());
               boolean var9 = 2 * ((SQLServerMetaData)var5.getValue()).precision <= 8000;
               if (!var9) {
                  this.writeShort((short)-1);
                  this.con.getDatabaseCollation().writeCollation(this);
               } else {
                  this.writeShort((short)8000);
                  this.con.getDatabaseCollation().writeCollation(this);
               }
               break;
            case BINARY:
            case VARBINARY:
               this.writeByte(TDSType.BIGVARBINARY.byteValue());
               boolean var2 = ((SQLServerMetaData)var5.getValue()).precision <= 8000;
               if (!var2) {
                  this.writeShort((short)-1);
               } else {
                  this.writeShort((short)8000);
               }
               break;
            default:
               assert false : "Unexpected JDBC type " + var6.toString();
         }

         this.writeByte((byte)0);
      }
   }

   void writeTvpOrderUnique(TVP var1) throws SQLServerException {
      Map var2 = var1.getColumnMetadata();
      Iterator var3 = var2.entrySet().iterator();
      LinkedList var4 = new LinkedList();

      while (var3.hasNext()) {
         byte var5 = 0;
         Entry var6 = (Entry)var3.next();
         SQLServerMetaData var7 = (SQLServerMetaData)var6.getValue();
         if (SQLServerSortOrder.Ascending == var7.sortOrder) {
            var5 = 1;
         } else if (SQLServerSortOrder.Descending == var7.sortOrder) {
            var5 = 2;
         }

         if (var7.isUniqueKey) {
            var5 = (byte)(var5 | 4);
         }

         if (0 != var5) {
            var4.add(new TDSWriter.TdsOrderUnique((Integer)var6.getKey(), var5));
         }
      }

      if (!var4.isEmpty()) {
         this.writeByte((byte)16);
         this.writeShort((short)var4.size());

         for (TDSWriter.TdsOrderUnique var9 : var4) {
            this.writeShort((short)(var9.columnOrdinal + 1));
            this.writeByte(var9.flags);
         }
      }
   }

   void setCryptoMetaData(CryptoMetadata var1) {
      this.cryptoMeta = var1;
   }

   CryptoMetadata getCryptoMetaData() {
      return this.cryptoMeta;
   }

   void writeEncryptedRPCByteArray(byte[] var1) throws SQLServerException {
      boolean var2 = var1 == null;
      long var3 = var2 ? 0L : var1.length;
      boolean var5 = var3 <= 8000L;
      boolean var6 = !var5 && var3 <= 2147483647L;
      if (var5) {
         this.writeShort((short)8000);
      } else if (var6) {
         this.writeShort((short)-1);
      } else {
         this.writeInt(Integer.MAX_VALUE);
      }

      if (var2) {
         this.writeShort((short)-1);
      } else {
         if (var5) {
            this.writeShort((short)var3);
         } else if (var6) {
            this.writeLong(var3);
         } else {
            this.writeInt((int)var3);
         }

         if (0L != var3) {
            if (var6) {
               this.writeInt((int)var3);
            }

            this.writeBytes(var1);
         }

         if (var6) {
            this.writeInt(0);
         }
      }
   }

   void writeCryptoMetaData() throws SQLServerException {
      this.writeByte(this.cryptoMeta.cipherAlgorithmId);
      this.writeByte(this.cryptoMeta.encryptionType.getValue());
      this.writeInt(((EncryptionKeyInfo)this.cryptoMeta.cekTableEntry.getColumnEncryptionKeyValues().get(0)).databaseId);
      this.writeInt(((EncryptionKeyInfo)this.cryptoMeta.cekTableEntry.getColumnEncryptionKeyValues().get(0)).cekId);
      this.writeInt(((EncryptionKeyInfo)this.cryptoMeta.cekTableEntry.getColumnEncryptionKeyValues().get(0)).cekVersion);
      this.writeBytes(((EncryptionKeyInfo)this.cryptoMeta.cekTableEntry.getColumnEncryptionKeyValues().get(0)).cekMdVersion);
      this.writeByte(this.cryptoMeta.normalizationRuleVersion);
   }

   void writeRPCByteArray(String var1, byte[] var2, boolean var3, JDBCType var4, SQLCollation var5) throws SQLServerException {
      boolean var6 = var2 == null;
      int var7 = var6 ? 0 : var2.length;
      boolean var8 = var7 <= 8000;
      boolean var9 = !var8 || var3;
      TDSType var10;
      if (null != this.cryptoMeta) {
         var10 = !var8 && !var9 ? TDSType.IMAGE : TDSType.BIGVARBINARY;
         var5 = null;
      } else {
         switch (var4) {
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case CLOB:
               var10 = !var8 && !var9 ? TDSType.TEXT : TDSType.BIGVARCHAR;
               if (null == var5) {
                  var5 = this.con.getDatabaseCollation();
               }
               break;
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
            case NCLOB:
               var10 = !var8 && !var9 ? TDSType.NTEXT : TDSType.NVARCHAR;
               if (null == var5) {
                  var5 = this.con.getDatabaseCollation();
               }
               break;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BLOB:
            default:
               var10 = !var8 && !var9 ? TDSType.IMAGE : TDSType.BIGVARBINARY;
               var5 = null;
         }
      }

      this.writeRPCNameValType(var1, var3, var10);
      if (var9) {
         this.writeVMaxHeader(var7, var6, var5);
         if (!var6) {
            if (var7 > 0) {
               this.writeInt(var7);
               this.writeBytes(var2);
            }

            this.writeInt(0);
         }
      } else {
         if (var8) {
            this.writeShort((short)8000);
         } else {
            this.writeInt(Integer.MAX_VALUE);
         }

         if (null != var5) {
            var5.writeCollation(this);
         }

         if (var6) {
            this.writeShort((short)-1);
         } else {
            if (var8) {
               this.writeShort((short)var7);
            } else {
               this.writeInt(var7);
            }

            if (0 != var7) {
               this.writeBytes(var2);
            }
         }
      }
   }

   void writeRPCDateTime(String var1, GregorianCalendar var2, int var3, boolean var4) throws SQLServerException {
      assert var3 >= 0 && var3 < 1000000000 : "Invalid subNanoSeconds value: " + var3;
      assert var2 != null || var2 == null && var3 == 0 : "Invalid subNanoSeconds value when calendar is null: " + var3;
      this.writeRPCNameValType(var1, var4, TDSType.DATETIMEN);
      this.writeByte((byte)8);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)8);
         int var5 = DDC.daysSinceBaseDate(var2.get(1), var2.get(6), 1900);
         int var6 = (var3 + 500000) / 1000000 + 1000 * var2.get(13) + 60000 * var2.get(12) + 3600000 * var2.get(11);
         if (var6 >= 86399999) {
            var5++;
            var6 = 0;
         }

         if (var5 >= DDC.daysSinceBaseDate(1753, 1, 1900) && var5 < DDC.daysSinceBaseDate(10000, 1, 1900)) {
            this.writeInt(var5);
            this.writeInt((3 * var6 + 5) / 10);
         } else {
            MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
            Object[] var8 = new Object[]{SSType.DATETIME};
            throw new SQLServerException(var7.format(var8), SQLState.DATA_EXCEPTION_DATETIME_FIELD_OVERFLOW, DriverError.NOT_SET, null);
         }
      }
   }

   void writeRPCTime(String var1, GregorianCalendar var2, int var3, int var4, boolean var5) throws SQLServerException {
      this.writeRPCNameValType(var1, var5, TDSType.TIMEN);
      this.writeByte((byte)var4);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)TDS.timeValueLength(var4));
         this.writeScaledTemporal(var2, var3, var4, SSType.TIME);
      }
   }

   void writeRPCDate(String var1, GregorianCalendar var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.DATEN);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)3);
         this.writeScaledTemporal(var2, 0, 0, SSType.DATE);
      }
   }

   void writeEncryptedRPCTime(String var1, GregorianCalendar var2, int var3, int var4, boolean var5) throws SQLServerException {
      if (this.con.getSendTimeAsDatetime()) {
         throw new SQLServerException(SQLServerException.getErrString("R_sendTimeAsDateTimeForAE"), null);
      }

      this.writeRPCNameValType(var1, var5, TDSType.BIGVARBINARY);
      if (null == var2) {
         this.writeEncryptedRPCByteArray(null);
      } else {
         this.writeEncryptedRPCByteArray(this.writeEncryptedScaledTemporal(var2, var3, var4, SSType.TIME, (short)0));
      }

      this.writeByte(TDSType.TIMEN.byteValue());
      this.writeByte((byte)var4);
      this.writeCryptoMetaData();
   }

   void writeEncryptedRPCDate(String var1, GregorianCalendar var2, boolean var3) throws SQLServerException {
      this.writeRPCNameValType(var1, var3, TDSType.BIGVARBINARY);
      if (null == var2) {
         this.writeEncryptedRPCByteArray(null);
      } else {
         this.writeEncryptedRPCByteArray(this.writeEncryptedScaledTemporal(var2, 0, 0, SSType.DATE, (short)0));
      }

      this.writeByte(TDSType.DATEN.byteValue());
      this.writeCryptoMetaData();
   }

   void writeEncryptedRPCDateTime(String var1, GregorianCalendar var2, int var3, boolean var4, JDBCType var5) throws SQLServerException {
      assert var3 >= 0 && var3 < 1000000000 : "Invalid subNanoSeconds value: " + var3;
      assert var2 != null || var2 == null && var3 == 0 : "Invalid subNanoSeconds value when calendar is null: " + var3;
      this.writeRPCNameValType(var1, var4, TDSType.BIGVARBINARY);
      if (null == var2) {
         this.writeEncryptedRPCByteArray(null);
      } else {
         this.writeEncryptedRPCByteArray(this.getEncryptedDateTimeAsBytes(var2, var3, var5));
      }

      if (JDBCType.SMALLDATETIME == var5) {
         this.writeByte(TDSType.DATETIMEN.byteValue());
         this.writeByte((byte)4);
      } else {
         this.writeByte(TDSType.DATETIMEN.byteValue());
         this.writeByte((byte)8);
      }

      this.writeCryptoMetaData();
   }

   byte[] getEncryptedDateTimeAsBytes(GregorianCalendar var1, int var2, JDBCType var3) throws SQLServerException {
      int var4 = DDC.daysSinceBaseDate(var1.get(1), var1.get(6), 1900);
      int var5 = (var2 + 500000) / 1000000 + 1000 * var1.get(13) + 60000 * var1.get(12) + 3600000 * var1.get(11);
      if (var5 >= 86399999) {
         var4++;
         var5 = 0;
      }

      if (var4 < DDC.daysSinceBaseDate(1753, 1, 1900) || var4 >= DDC.daysSinceBaseDate(10000, 1, 1900)) {
         MessageFormat var12 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
         Object[] var15 = new Object[]{SSType.DATETIME};
         throw new SQLServerException(var12.format(var15), SQLState.DATA_EXCEPTION_DATETIME_FIELD_OVERFLOW, DriverError.NOT_SET, null);
      } else if (JDBCType.SMALLDATETIME == var3) {
         int var11 = var5 / 1000;
         int var13 = var11 / 60;
         var13 = var11 % 60 > 29.998 ? var13 + 1 : var13;
         ByteBuffer var16 = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
         var16.putShort((short)var4);
         ByteBuffer var9 = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
         var9.putShort((short)var13);
         byte[] var10 = new byte[4];
         System.arraycopy(var16.array(), 0, var10, 0, 2);
         System.arraycopy(var9.array(), 0, var10, 2, 2);
         return SQLServerSecurityUtility.encryptWithKey(var10, this.cryptoMeta, this.con);
      } else if (JDBCType.DATETIME == var3) {
         ByteBuffer var6 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
         var6.putInt(var4);
         ByteBuffer var7 = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
         var7.putInt((3 * var5 + 5) / 10);
         byte[] var8 = new byte[8];
         System.arraycopy(var6.array(), 0, var8, 0, 4);
         System.arraycopy(var7.array(), 0, var8, 4, 4);
         return SQLServerSecurityUtility.encryptWithKey(var8, this.cryptoMeta, this.con);
      } else {
         assert false : "Unexpected JDBCType type " + var3;
         return null;
      }
   }

   void writeEncryptedRPCDateTime2(String var1, GregorianCalendar var2, int var3, int var4, boolean var5) throws SQLServerException {
      this.writeRPCNameValType(var1, var5, TDSType.BIGVARBINARY);
      if (null == var2) {
         this.writeEncryptedRPCByteArray(null);
      } else {
         this.writeEncryptedRPCByteArray(this.writeEncryptedScaledTemporal(var2, var3, var4, SSType.DATETIME2, (short)0));
      }

      this.writeByte(TDSType.DATETIME2N.byteValue());
      this.writeByte((byte)var4);
      this.writeCryptoMetaData();
   }

   void writeEncryptedRPCDateTimeOffset(String var1, GregorianCalendar var2, int var3, int var4, int var5, boolean var6) throws SQLServerException {
      this.writeRPCNameValType(var1, var6, TDSType.BIGVARBINARY);
      if (null == var2) {
         this.writeEncryptedRPCByteArray(null);
      } else {
         assert 0 == var2.get(15);
         this.writeEncryptedRPCByteArray(this.writeEncryptedScaledTemporal(var2, var4, var5, SSType.DATETIMEOFFSET, (short)var3));
      }

      this.writeByte(TDSType.DATETIMEOFFSETN.byteValue());
      this.writeByte((byte)var5);
      this.writeCryptoMetaData();
   }

   void writeRPCDateTime2(String var1, GregorianCalendar var2, int var3, int var4, boolean var5) throws SQLServerException {
      this.writeRPCNameValType(var1, var5, TDSType.DATETIME2N);
      this.writeByte((byte)var4);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         this.writeByte((byte)TDS.datetime2ValueLength(var4));
         this.writeScaledTemporal(var2, var3, var4, SSType.DATETIME2);
      }
   }

   void writeRPCDateTimeOffset(String var1, GregorianCalendar var2, int var3, int var4, int var5, boolean var6) throws SQLServerException {
      this.writeRPCNameValType(var1, var6, TDSType.DATETIMEOFFSETN);
      this.writeByte((byte)var5);
      if (null == var2) {
         this.writeByte((byte)0);
      } else {
         assert 0 == var2.get(15);
         this.writeByte((byte)TDS.datetimeoffsetValueLength(var5));
         this.writeScaledTemporal(var2, var4, var5, SSType.DATETIMEOFFSET);
         this.writeShort((short)var3);
      }
   }

   private int getRoundedSubSecondNanos(int var1) {
      return (var1 + Nanos.PER_MAX_SCALE_INTERVAL / 2) / Nanos.PER_MAX_SCALE_INTERVAL * Nanos.PER_MAX_SCALE_INTERVAL;
   }

   private void writeScaledTemporal(GregorianCalendar var1, int var2, int var3, SSType var4) throws SQLServerException {
      assert this.con.isKatmaiOrLater();
      assert SSType.DATE == var4 || SSType.TIME == var4 || SSType.DATETIME2 == var4 || SSType.DATETIMEOFFSET == var4 : "Unexpected SSType: " + var4;
      if (SSType.TIME == var4 || SSType.DATETIME2 == var4 || SSType.DATETIMEOFFSET == var4) {
         assert var2 >= 0;
         assert var2 < 1000000000;
         assert var3 >= 0;
         assert var3 <= 7;
         int var5 = var1.get(13) + 60 * var1.get(12) + 3600 * var1.get(11);
         long var6 = Nanos.PER_MAX_SCALE_INTERVAL * (long)Math.pow(10.0, 7 - var3);
         long var8 = (1000000000L * var5 + this.getRoundedSubSecondNanos(var2) + var6 / 2L) / var6;
         if (86400000000000L / var6 == var8) {
            if (SSType.TIME == var4) {
               var8--;
            } else {
               assert SSType.DATETIME2 == var4 || SSType.DATETIMEOFFSET == var4 : "Unexpected SSType: " + var4;
               var1.add(13, 1);
               if (var1.get(1) <= 9999) {
                  var8 = 0L;
               } else {
                  var1.add(13, -1);
                  var8--;
               }
            }
         }

         int var10 = TDS.nanosSinceMidnightLength(var3);
         byte[] var11 = this.scaledNanosToEncodedBytes(var8, var10);
         this.writeBytes(var11);
      }

      if (SSType.DATE == var4 || SSType.DATETIME2 == var4 || SSType.DATETIMEOFFSET == var4) {
         if (var1.getTimeInMillis() < GregorianChange.STANDARD_CHANGE_DATE.getTime() || var1.getActualMaximum(6) < 365) {
            int var12 = var1.get(1);
            int var14 = var1.get(2);
            int var7 = var1.get(5);
            var1.setGregorianChange(GregorianChange.PURE_CHANGE_DATE);
            var1.set(var12, var14, var7);
         }

         int var13 = DDC.daysSinceBaseDate(var1.get(1), var1.get(6), 1);
         if (var13 < 0 || var13 >= DDC.daysSinceBaseDate(10000, 1, 1)) {
            MessageFormat var16 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
            Object[] var17 = new Object[]{var4};
            throw new SQLServerException(var16.format(var17), SQLState.DATA_EXCEPTION_DATETIME_FIELD_OVERFLOW, DriverError.NOT_SET, null);
         }

         byte[] var15 = new byte[]{(byte)(var13 >> 0 & 0xFF), (byte)(var13 >> 8 & 0xFF), (byte)(var13 >> 16 & 0xFF)};
         this.writeBytes(var15);
      }
   }

   byte[] writeEncryptedScaledTemporal(GregorianCalendar var1, int var2, int var3, SSType var4, short var5) throws SQLServerException {
      assert this.con.isKatmaiOrLater();
      assert SSType.DATE == var4 || SSType.TIME == var4 || SSType.DATETIME2 == var4 || SSType.DATETIMEOFFSET == var4 : "Unexpected SSType: " + var4;
      byte[] var6 = null;
      int var7 = 0;
      long var8 = 0L;
      long var10 = 0L;
      if (SSType.TIME == var4 || SSType.DATETIME2 == var4 || SSType.DATETIMEOFFSET == var4) {
         assert var2 >= 0;
         assert var2 < 1000000000;
         assert var3 >= 0;
         assert var3 <= 7;
         var7 = var1.get(13) + 60 * var1.get(12) + 3600 * var1.get(11);
         var8 = Nanos.PER_MAX_SCALE_INTERVAL * (long)Math.pow(10.0, 7 - var3);
         var10 = (1000000000L * var7 + this.getRoundedSubSecondNanos(var2) + var8 / 2L) / var8 * var8 / 100L;
         if (SSType.TIME == var4 && 864000000000L <= var10) {
            var10 = (1000000000L * var7 + this.getRoundedSubSecondNanos(var2)) / var8 * var8 / 100L;
         }

         if (86400000000000L / var8 == var10) {
            if (SSType.TIME == var4) {
               var10--;
            } else {
               assert SSType.DATETIME2 == var4 || SSType.DATETIMEOFFSET == var4 : "Unexpected SSType: " + var4;
               var1.add(13, 1);
               if (var1.get(1) <= 9999) {
                  var10 = 0L;
               } else {
                  var1.add(13, -1);
                  var10--;
               }
            }
         }

         int var12 = TDS.nanosSinceMidnightLength(7);
         byte[] var13 = this.scaledNanosToEncodedBytes(var10, var12);
         if (SSType.TIME == var4) {
            return SQLServerSecurityUtility.encryptWithKey(var13, this.cryptoMeta, this.con);
         }

         if (SSType.DATETIME2 == var4) {
            var6 = new byte[var12 + 3];
            System.arraycopy(var13, 0, var6, 0, var13.length);
         } else if (SSType.DATETIMEOFFSET == var4) {
            var6 = new byte[var12 + 5];
            System.arraycopy(var13, 0, var6, 0, var13.length);
         }
      }

      if (SSType.DATE != var4 && SSType.DATETIME2 != var4 && SSType.DATETIMEOFFSET != var4) {
         MessageFormat var21 = new MessageFormat(SQLServerException.getErrString("R_unknownSSType"));
         Object[] var25 = new Object[]{var4};
         SQLServerException.makeFromDriverError(null, null, var21.format(var25), null, true);
         return null;
      }

      if (var1.getTimeInMillis() < GregorianChange.STANDARD_CHANGE_DATE.getTime() || var1.getActualMaximum(6) < 365) {
         int var19 = var1.get(1);
         int var22 = var1.get(2);
         int var14 = var1.get(5);
         var1.setGregorianChange(GregorianChange.PURE_CHANGE_DATE);
         var1.set(var19, var22, var14);
      }

      int var20 = DDC.daysSinceBaseDate(var1.get(1), var1.get(6), 1);
      if (var20 >= 0 && var20 < DDC.daysSinceBaseDate(10000, 1, 1)) {
         byte[] var24 = new byte[]{(byte)(var20 >> 0 & 0xFF), (byte)(var20 >> 8 & 0xFF), (byte)(var20 >> 16 & 0xFF)};
         byte[] var27;
         if (SSType.DATE == var4) {
            var27 = SQLServerSecurityUtility.encryptWithKey(var24, this.cryptoMeta, this.con);
         } else if (SSType.DATETIME2 == var4) {
            if (3652058 == var20 && 864000000000L == var10) {
               var10 = (1000000000L * var7 + this.getRoundedSubSecondNanos(var2)) / var8 * var8 / 100L;
               int var15 = TDS.nanosSinceMidnightLength(7);
               byte[] var16 = this.scaledNanosToEncodedBytes(var10, var15);
               var6 = new byte[var15 + 3];
               System.arraycopy(var16, 0, var6, 0, var16.length);
            }

            System.arraycopy(var24, 0, var6, var6.length - 3, 3);
            var27 = SQLServerSecurityUtility.encryptWithKey(var6, this.cryptoMeta, this.con);
         } else {
            if (3652058 == var20 && 864000000000L == var10) {
               var10 = (1000000000L * var7 + this.getRoundedSubSecondNanos(var2)) / var8 * var8 / 100L;
               int var28 = TDS.nanosSinceMidnightLength(7);
               byte[] var29 = this.scaledNanosToEncodedBytes(var10, var28);
               var6 = new byte[var28 + 5];
               System.arraycopy(var29, 0, var6, 0, var29.length);
            }

            System.arraycopy(var24, 0, var6, var6.length - 5, 3);
            System.arraycopy(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(var5).array(), 0, var6, var6.length - 2, 2);
            var27 = SQLServerSecurityUtility.encryptWithKey(var6, this.cryptoMeta, this.con);
         }

         return var27;
      } else {
         MessageFormat var23 = new MessageFormat(SQLServerException.getErrString("R_valueOutOfRange"));
         Object[] var26 = new Object[]{var4};
         throw new SQLServerException(var23.format(var26), SQLState.DATA_EXCEPTION_DATETIME_FIELD_OVERFLOW, DriverError.NOT_SET, null);
      }
   }

   private byte[] scaledNanosToEncodedBytes(long var1, int var3) {
      byte[] var4 = new byte[var3];

      for (int var5 = 0; var5 < var3; var5++) {
         var4[var5] = (byte)(var1 >> 8 * var5 & 255L);
      }

      return var4;
   }

   void writeRPCInputStream(String var1, InputStream var2, long var3, boolean var5, JDBCType var6, SQLCollation var7) throws SQLServerException {
      assert null != var2;
      assert -1L == var3 || var3 >= 0L;
      boolean var8 = -1L == var3 || var3 > 8000L;
      if (var8) {
         assert -1L == var3 || var3 <= 2147483647L;
         this.writeRPCNameValType(var1, var5, var6.isTextual() ? TDSType.BIGVARCHAR : TDSType.BIGVARBINARY);
         this.writeVMaxHeader(var3, false, var6.isTextual() ? var7 : null);
      } else {
         if (-1L == var3) {
            ByteArrayOutputStream var9 = new ByteArrayOutputStream(8000);
            var3 = 0L;
            long var10 = 65535L * this.con.getTDSPacketSize();

            try {
               byte[] var12 = new byte[8000];

               int var13;
               while (var3 < var10 && -1 != (var13 = var2.read(var12, 0, var12.length))) {
                  var9.write(var12);
                  var3 += var13;
               }
            } catch (IOException var14) {
               throw new SQLServerException(var14.getMessage(), SQLState.DATA_EXCEPTION_NOT_SPECIFIC, DriverError.NOT_SET, var14);
            }

            if (var3 >= var10) {
               MessageFormat var16 = new MessageFormat(SQLServerException.getErrString("R_invalidLength"));
               Object[] var17 = new Object[]{var3};
               SQLServerException.makeFromDriverError(null, null, var16.format(var17), "", true);
            }

            assert var3 <= 2147483647L;
            var2 = new ByteArrayInputStream(var9.toByteArray(), 0, (int)var3);
         }

         assert 0L <= var3 && var3 <= 2147483647L;
         boolean var15 = var3 <= 8000L;
         this.writeRPCNameValType(var1, var5, var6.isTextual() ? (var15 ? TDSType.BIGVARCHAR : TDSType.TEXT) : (var15 ? TDSType.BIGVARBINARY : TDSType.IMAGE));
         if (var15) {
            this.writeShort((short)8000);
            if (var6.isTextual()) {
               var7.writeCollation(this);
            }

            this.writeShort((short)var3);
         } else {
            this.writeInt(Integer.MAX_VALUE);
            if (var6.isTextual()) {
               var7.writeCollation(this);
            }

            this.writeInt((int)var3);
         }
      }

      this.writeStream((InputStream)var2, var3, var8);
   }

   void writeRPCXML(String var1, InputStream var2, long var3, boolean var5) throws SQLServerException {
      assert -1L == var3 || var3 >= 0L;
      assert -1L == var3 || var3 <= 2147483647L;
      this.writeRPCNameValType(var1, var5, TDSType.XML);
      this.writeByte((byte)0);
      if (null == var2) {
         this.writeLong(-1L);
      } else if (-1L == var3) {
         this.writeLong(-2L);
      } else {
         this.writeLong(var3);
      }

      if (null != var2) {
         this.writeStream(var2, var3, true);
      }
   }

   void writeRPCReaderUnicode(String var1, Reader var2, long var3, boolean var5, SQLCollation var6) throws SQLServerException {
      assert null != var2;
      assert -1L == var3 || var3 >= 0L;
      if (null == var6) {
         var6 = this.con.getDatabaseCollation();
      }

      boolean var7 = -1L == var3 || var3 > 4000L;
      if (var7) {
         assert -1L == var3 || var3 <= 1073741823L;
         this.writeRPCNameValType(var1, var5, TDSType.NVARCHAR);
         this.writeVMaxHeader(-1L == var3 ? -1L : 2L * var3, false, var6);
      } else {
         assert 0L <= var3 && var3 <= 1073741823L;
         boolean var8 = var3 <= 4000L;
         this.writeRPCNameValType(var1, var5, var8 ? TDSType.NVARCHAR : TDSType.NTEXT);
         if (var8) {
            this.writeShort((short)8000);
            var6.writeCollation(this);
            this.writeShort((short)(2L * var3));
         } else {
            this.writeInt(1073741823);
            var6.writeCollation(this);
            this.writeInt((int)(2L * var3));
         }
      }

      this.writeReader(var2, var3, var7);
   }

   private class TdsOrderUnique {
      int columnOrdinal;
      byte flags;

      TdsOrderUnique(int var2, byte var3) {
         this.columnOrdinal = var2;
         this.flags = var3;
      }
   }
}
