package com.microsoft.sqlserver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

final class TDSReader {
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.TDS.Reader");
   private final String traceID;
   private final TDSChannel tdsChannel;
   private final SQLServerConnection con;
   private final TDSCommand command;
   private TDSPacket currentPacket = new TDSPacket(0);
   private TDSPacket lastPacket = this.currentPacket;
   private int payloadOffset = 0;
   private int packetNum = 0;
   private boolean isStreaming = true;
   private boolean useColumnEncryption = false;
   private boolean serverSupportsColumnEncryption = false;
   private final byte[] valueBytes = new byte[256];
   private static int lastReaderID = 0;
   private static final int[] SCALED_MULTIPLIERS = new int[]{10000000, 1000000, 100000, 10000, 1000, 100, 10, 1};
   static final String guidTemplate = "NNNNNNNN-NNNN-NNNN-NNNN-NNNNNNNNNNNN";

   public final String toString() {
      return this.traceID;
   }

   final TDSCommand getCommand() {
      assert null != this.command;
      return this.command;
   }

   final SQLServerConnection getConnection() {
      return this.con;
   }

   private static synchronized int nextReaderID() {
      return ++lastReaderID;
   }

   TDSReader(TDSChannel var1, SQLServerConnection var2, TDSCommand var3) {
      this.tdsChannel = var1;
      this.con = var2;
      this.command = var3;
      if (logger.isLoggable(Level.FINE)) {
         this.traceID = "TDSReader@" + nextReaderID() + " (" + var2.toString() + ")";
      } else {
         this.traceID = var2.toString();
      }

      if (var2.isColumnEncryptionSettingEnabled()) {
         this.useColumnEncryption = true;
      }

      this.serverSupportsColumnEncryption = var2.getServerSupportsColumnEncryption();
   }

   final boolean isColumnEncryptionSettingEnabled() {
      return this.useColumnEncryption;
   }

   final boolean getServerSupportsColumnEncryption() {
      return this.serverSupportsColumnEncryption;
   }

   final void throwInvalidTDS() throws SQLServerException {
      if (logger.isLoggable(Level.SEVERE)) {
         logger.severe(this.toString() + " got unexpected value in TDS response at offset:" + this.payloadOffset);
      }

      this.con.throwInvalidTDS();
   }

   final void throwInvalidTDSToken(String var1) throws SQLServerException {
      if (logger.isLoggable(Level.SEVERE)) {
         logger.severe(this.toString() + " got unexpected value in TDS response at offset:" + this.payloadOffset);
      }

      this.con.throwInvalidTDSToken(var1);
   }

   private final boolean ensurePayload() throws SQLServerException {
      if (this.payloadOffset == this.currentPacket.payloadLength && !this.nextPacket()) {
         return false;
      }

      assert this.payloadOffset < this.currentPacket.payloadLength;
      return true;
   }

   private final boolean nextPacket() throws SQLServerException {
      assert null != this.currentPacket;
      TDSPacket var1 = this.currentPacket;
      assert this.payloadOffset == var1.payloadLength;
      if (null == var1.next) {
         this.readPacket();
         if (null == var1.next) {
            return false;
         }
      }

      TDSPacket var2 = var1.next;
      if (this.isStreaming) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Moving to next packet -- unlinking consumed packet");
         }

         var1.next = null;
      }

      this.currentPacket = var2;
      this.payloadOffset = 0;
      return true;
   }

   final synchronized boolean readPacket() throws SQLServerException {
      if (null != this.command && !this.command.readingResponse()) {
         return false;
      }

      assert this.tdsChannel.numMsgsRcvd < this.tdsChannel.numMsgsSent : "numMsgsRcvd:"
         + this.tdsChannel.numMsgsRcvd
         + " should be less than numMsgsSent:"
         + this.tdsChannel.numMsgsSent;
      TDSPacket var1 = new TDSPacket(this.con.getTDSPacketSize());
      int var2 = 0;

      while (var2 < 8) {
         int var3 = this.tdsChannel.read(var1.header, var2, 8 - var2);
         if (var3 < 0) {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " Premature EOS in response. packetNum:" + this.packetNum + " headerBytesRead:" + var2);
            }

            this.con
               .terminate(
                  3,
                  0 == this.packetNum && 0 == var2
                     ? SQLServerException.getErrString("R_noServerResponse")
                     : SQLServerException.getErrString("R_truncatedServerResponse")
               );
         }

         var2 += var3;
      }

      var2 = Util.readUnsignedShortBigEndian(var1.header, 2);
      if (var2 < 8 || var2 > this.con.getTDSPacketSize()) {
         logger.warning(this.toString() + " TDS header contained invalid packet length:" + var2 + "; packet size:" + this.con.getTDSPacketSize());
         this.throwInvalidTDS();
      }

      var1.payloadLength = var2 - 8;
      this.tdsChannel.setSPID(Util.readUnsignedShortBigEndian(var1.header, 4));
      byte[] var7 = null;
      if (this.tdsChannel.isLoggingPackets()) {
         var7 = new byte[var2];
         System.arraycopy(var1.header, 0, var7, 0, 8);
      }

      int var4 = 0;

      while (var4 < var1.payloadLength) {
         int var5 = this.tdsChannel.read(var1.payload, var4, var1.payloadLength - var4);
         if (var5 < 0) {
            this.con.terminate(3, SQLServerException.getErrString("R_truncatedServerResponse"));
         }

         var4 += var5;
      }

      this.packetNum++;
      this.lastPacket.next = var1;
      this.lastPacket = var1;
      if (this.tdsChannel.isLoggingPackets()) {
         System.arraycopy(var1.payload, 0, var7, 8, var1.payloadLength);
         this.tdsChannel.logPacket(var7, 0, var2, this.toString() + " received Packet:" + this.packetNum + " (" + var1.payloadLength + " bytes)");
      }

      if (var1.isEOM()) {
         this.tdsChannel.numMsgsRcvd++;
         if (null != this.command) {
            this.command.onResponseEOM();
         }
      }

      return true;
   }

   final TDSReaderMark mark() {
      TDSReaderMark var1 = new TDSReaderMark(this.currentPacket, this.payloadOffset);
      this.isStreaming = false;
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + ": Buffering from: " + var1.toString());
      }

      return var1;
   }

   final void reset(TDSReaderMark var1) {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + ": Resetting to: " + var1.toString());
      }

      this.currentPacket = var1.packet;
      this.payloadOffset = var1.payloadOffset;
   }

   final void stream() {
      this.isStreaming = true;
   }

   final int available() {
      int var1 = this.currentPacket.payloadLength - this.payloadOffset;

      for (TDSPacket var2 = this.currentPacket.next; null != var2; var2 = var2.next) {
         var1 += var2.payloadLength;
      }

      return var1;
   }

   final int peekTokenType() throws SQLServerException {
      return !this.ensurePayload() ? -1 : this.currentPacket.payload[this.payloadOffset] & 0xFF;
   }

   final int readUnsignedByte() throws SQLServerException {
      if (!this.ensurePayload()) {
         this.throwInvalidTDS();
      }

      return this.currentPacket.payload[this.payloadOffset++] & 0xFF;
   }

   final short readShort() throws SQLServerException {
      if (this.payloadOffset + 2 <= this.currentPacket.payloadLength) {
         short var1 = Util.readShort(this.currentPacket.payload, this.payloadOffset);
         this.payloadOffset += 2;
         return var1;
      } else {
         return Util.readShort(this.readWrappedBytes(2), 0);
      }
   }

   final int readUnsignedShort() throws SQLServerException {
      if (this.payloadOffset + 2 <= this.currentPacket.payloadLength) {
         int var1 = Util.readUnsignedShort(this.currentPacket.payload, this.payloadOffset);
         this.payloadOffset += 2;
         return var1;
      } else {
         return Util.readUnsignedShort(this.readWrappedBytes(2), 0);
      }
   }

   final String readUnicodeString(int var1) throws SQLServerException {
      int var2 = 2 * var1;
      byte[] var3 = new byte[var2];
      this.readBytes(var3, 0, var2);
      return Util.readUnicodeString(var3, 0, var2, this.con);
   }

   final char readChar() throws SQLServerException {
      return (char)this.readShort();
   }

   final int readInt() throws SQLServerException {
      if (this.payloadOffset + 4 <= this.currentPacket.payloadLength) {
         int var1 = Util.readInt(this.currentPacket.payload, this.payloadOffset);
         this.payloadOffset += 4;
         return var1;
      } else {
         return Util.readInt(this.readWrappedBytes(4), 0);
      }
   }

   final int readIntBigEndian() throws SQLServerException {
      if (this.payloadOffset + 4 <= this.currentPacket.payloadLength) {
         int var1 = Util.readIntBigEndian(this.currentPacket.payload, this.payloadOffset);
         this.payloadOffset += 4;
         return var1;
      } else {
         return Util.readIntBigEndian(this.readWrappedBytes(4), 0);
      }
   }

   final long readUnsignedInt() throws SQLServerException {
      return this.readInt() & 4294967295L;
   }

   final long readLong() throws SQLServerException {
      if (this.payloadOffset + 8 <= this.currentPacket.payloadLength) {
         long var1 = Util.readLong(this.currentPacket.payload, this.payloadOffset);
         this.payloadOffset += 8;
         return var1;
      } else {
         return Util.readLong(this.readWrappedBytes(8), 0);
      }
   }

   final void readBytes(byte[] var1, int var2, int var3) throws SQLServerException {
      int var4 = 0;

      while (var4 < var3) {
         if (!this.ensurePayload()) {
            this.throwInvalidTDS();
         }

         int var5 = var3 - var4;
         if (var5 > this.currentPacket.payloadLength - this.payloadOffset) {
            var5 = this.currentPacket.payloadLength - this.payloadOffset;
         }

         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Reading " + var5 + " bytes from offset " + this.payloadOffset);
         }

         System.arraycopy(this.currentPacket.payload, this.payloadOffset, var1, var2 + var4, var5);
         var4 += var5;
         this.payloadOffset += var5;
      }
   }

   final byte[] readWrappedBytes(int var1) throws SQLServerException {
      assert var1 <= this.valueBytes.length;
      this.readBytes(this.valueBytes, 0, var1);
      return this.valueBytes;
   }

   final Object readDecimal(int var1, TypeInfo var2, JDBCType var3, StreamType var4) throws SQLServerException {
      if (var1 > this.valueBytes.length) {
         logger.warning(this.toString() + " Invalid value length:" + var1);
         this.throwInvalidTDS();
      }

      this.readBytes(this.valueBytes, 0, var1);
      return DDC.convertBigDecimalToObject(Util.readBigDecimal(this.valueBytes, var1, var2.getScale()), var3, var4);
   }

   final Object readMoney(int var1, JDBCType var2, StreamType var3) throws SQLServerException {
      BigInteger var4;
      switch (var1) {
         case 4:
            if (JDBCType.BINARY == var2) {
               byte[] var8 = new byte[4];
               Util.writeIntBigEndian(this.readInt(), var8, 0);
               return var8;
            }

            var4 = BigInteger.valueOf(this.readInt());
            break;
         case 8:
            int var5 = this.readInt();
            int var6 = this.readInt();
            if (JDBCType.BINARY == var2) {
               byte[] var7 = new byte[8];
               Util.writeIntBigEndian(var5, var7, 0);
               Util.writeIntBigEndian(var6, var7, 4);
               return var7;
            }

            var4 = BigInteger.valueOf((long)var5 << 32 | var6 & 4294967295L);
            break;
         default:
            this.throwInvalidTDS();
            return null;
      }

      return DDC.convertBigDecimalToObject(new BigDecimal(var4, 4), var2, var3);
   }

   final Object readReal(int var1, JDBCType var2, StreamType var3) throws SQLServerException {
      if (4 != var1) {
         this.throwInvalidTDS();
      }

      return DDC.convertFloatToObject(Float.intBitsToFloat(this.readInt()), var2, var3);
   }

   final Object readFloat(int var1, JDBCType var2, StreamType var3) throws SQLServerException {
      if (8 != var1) {
         this.throwInvalidTDS();
      }

      return DDC.convertDoubleToObject(Double.longBitsToDouble(this.readLong()), var2, var3);
   }

   final Object readDateTime(int var1, Calendar var2, JDBCType var3, StreamType var4) throws SQLServerException {
      int var5;
      int var7;
      switch (var1) {
         case 4:
            var5 = this.readUnsignedShort();
            int var9 = this.readUnsignedShort();
            if (JDBCType.BINARY == var3) {
               byte[] var10 = new byte[4];
               Util.writeShortBigEndian((short)var5, var10, 0);
               Util.writeShortBigEndian((short)var9, var10, 2);
               return var10;
            }

            var7 = var9 * 60 * 1000;
            break;
         case 8:
            var5 = this.readInt();
            int var6 = this.readInt();
            if (JDBCType.BINARY == var3) {
               byte[] var8 = new byte[8];
               Util.writeIntBigEndian(var5, var8, 0);
               Util.writeIntBigEndian(var6, var8, 4);
               return var8;
            }

            var7 = (var6 * 10 + 1) / 3;
            break;
         default:
            this.throwInvalidTDS();
            return null;
      }

      return DDC.convertTemporalToObject(var3, SSType.DATETIME, var2, var5, var7, 0);
   }

   final Object readDate(int var1, Calendar var2, JDBCType var3) throws SQLServerException {
      if (3 != var1) {
         this.throwInvalidTDS();
      }

      int var4 = this.readDaysIntoCE();
      return DDC.convertTemporalToObject(var3, SSType.DATE, var2, var4, 0L, 0);
   }

   final Object readTime(int var1, TypeInfo var2, Calendar var3, JDBCType var4) throws SQLServerException {
      if (TDS.timeValueLength(var2.getScale()) != var1) {
         this.throwInvalidTDS();
      }

      long var5 = this.readNanosSinceMidnight(var2.getScale());
      return DDC.convertTemporalToObject(var4, SSType.TIME, var3, 0, var5, var2.getScale());
   }

   final Object readDateTime2(int var1, TypeInfo var2, Calendar var3, JDBCType var4) throws SQLServerException {
      if (TDS.datetime2ValueLength(var2.getScale()) != var1) {
         this.throwInvalidTDS();
      }

      long var5 = this.readNanosSinceMidnight(var2.getScale());
      int var7 = this.readDaysIntoCE();
      return DDC.convertTemporalToObject(var4, SSType.DATETIME2, var3, var7, var5, var2.getScale());
   }

   final Object readDateTimeOffset(int var1, TypeInfo var2, JDBCType var3) throws SQLServerException {
      if (TDS.datetimeoffsetValueLength(var2.getScale()) != var1) {
         this.throwInvalidTDS();
      }

      long var4 = this.readNanosSinceMidnight(var2.getScale());
      int var6 = this.readDaysIntoCE();
      short var7 = this.readShort();
      return DDC.convertTemporalToObject(
         var3, SSType.DATETIMEOFFSET, new GregorianCalendar(new SimpleTimeZone(var7 * 60 * 1000, ""), Locale.US), var6, var4, var2.getScale()
      );
   }

   private int readDaysIntoCE() throws SQLServerException {
      byte[] var1 = new byte[3];
      this.readBytes(var1, 0, var1.length);
      int var2 = 0;

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2 |= (var1[var3] & 255) << 8 * var3;
      }

      if (var2 < 0) {
         this.throwInvalidTDS();
      }

      return var2;
   }

   private long readNanosSinceMidnight(int var1) throws SQLServerException {
      assert 0 <= var1 && var1 <= 7;
      byte[] var2 = new byte[TDS.nanosSinceMidnightLength(var1)];
      this.readBytes(var2, 0, var2.length);
      long var3 = 0L;

      for (int var5 = 0; var5 < var2.length; var5++) {
         var3 |= (var2[var5] & 255L) << 8 * var5;
      }

      var3 *= SCALED_MULTIPLIERS[var1];
      if (0L > var3 || var3 >= 864000000000L) {
         this.throwInvalidTDS();
      }

      return 100L * var3;
   }

   final Object readGUID(int var1, JDBCType var2, StreamType var3) throws SQLServerException {
      if (16 != var1) {
         this.throwInvalidTDS();
      }

      byte[] var4 = new byte[16];
      this.readBytes(var4, 0, 16);
      switch (var2) {
         case CHAR:
         case VARCHAR:
         case LONGVARCHAR:
         case GUID:
            StringBuilder var5 = new StringBuilder("NNNNNNNN-NNNN-NNNN-NNNN-NNNNNNNNNNNN".length());

            for (int var6 = 0; var6 < 4; var6++) {
               var5.append(Util.hexChars[(var4[3 - var6] & 240) >> 4]);
               var5.append(Util.hexChars[var4[3 - var6] & 15]);
            }

            var5.append('-');

            for (int var9 = 0; var9 < 2; var9++) {
               var5.append(Util.hexChars[(var4[5 - var9] & 240) >> 4]);
               var5.append(Util.hexChars[var4[5 - var9] & 15]);
            }

            var5.append('-');

            for (int var10 = 0; var10 < 2; var10++) {
               var5.append(Util.hexChars[(var4[7 - var10] & 240) >> 4]);
               var5.append(Util.hexChars[var4[7 - var10] & 15]);
            }

            var5.append('-');

            for (int var11 = 0; var11 < 2; var11++) {
               var5.append(Util.hexChars[(var4[8 + var11] & 240) >> 4]);
               var5.append(Util.hexChars[var4[8 + var11] & 15]);
            }

            var5.append('-');

            for (int var12 = 0; var12 < 6; var12++) {
               var5.append(Util.hexChars[(var4[10 + var12] & 240) >> 4]);
               var5.append(Util.hexChars[var4[10 + var12] & 15]);
            }

            try {
               return DDC.convertStringToObject(var5.toString(), Encoding.UNICODE.charsetName(), var2, var3);
            } catch (UnsupportedEncodingException var8) {
               MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_errorConvertingValue"));
               throw new SQLServerException(var7.format(new Object[]{"UNIQUEIDENTIFIER", var2}), null, 0, var8);
            }
         default:
            return StreamType.BINARY != var3 && StreamType.ASCII != var3 ? var4 : new ByteArrayInputStream(var4);
      }
   }

   final SQLIdentifier readSQLIdentifier() throws SQLServerException {
      int var1 = this.readUnsignedByte();
      if (1 > var1 || var1 > 4) {
         this.throwInvalidTDS();
      }

      String[] var2 = new String[var1];

      for (int var3 = 0; var3 < var1; var3++) {
         var2[var3] = this.readUnicodeString(this.readUnsignedShort());
      }

      SQLIdentifier var4 = new SQLIdentifier();
      var4.setObjectName(var2[var1 - 1]);
      if (var1 >= 2) {
         var4.setSchemaName(var2[var1 - 2]);
      }

      if (var1 >= 3) {
         var4.setDatabaseName(var2[var1 - 3]);
      }

      if (4 == var1) {
         var4.setServerName(var2[var1 - 4]);
      }

      return var4;
   }

   final SQLCollation readCollation() throws SQLServerException {
      SQLCollation var1 = null;

      try {
         var1 = new SQLCollation(this);
      } catch (UnsupportedEncodingException var3) {
         this.con.terminate(4, var3.getMessage(), var3);
      }

      return var1;
   }

   final void skip(int var1) throws SQLServerException {
      assert var1 >= 0;

      while (var1 > 0) {
         if (!this.ensurePayload()) {
            this.throwInvalidTDS();
         }

         int var2 = var1;
         if (var2 > this.currentPacket.payloadLength - this.payloadOffset) {
            var2 = this.currentPacket.payloadLength - this.payloadOffset;
         }

         var1 -= var2;
         this.payloadOffset += var2;
      }
   }

   final void TryProcessFeatureExtAck(boolean var1) throws SQLServerException {
      if (this.isColumnEncryptionSettingEnabled() && !var1) {
         throw new SQLServerException(this, SQLServerException.getErrString("R_AE_NotSupportedByServer"), null, 0, false);
      }
   }
}
