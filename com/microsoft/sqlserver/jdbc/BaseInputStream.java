package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class BaseInputStream extends InputStream {
   final boolean isAdaptive;
   final boolean isStreaming;
   private String parentLoggingInfo = "";
   private static int lastLoggingID = 0;
   static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.InputStream");
   private String traceID;
   int streamPos = 0;
   int markedStreamPos = 0;
   TDSReaderMark currentMark;
   private ServerDTVImpl dtv;
   TDSReader tdsReader;
   int readLimit = 0;
   boolean isReadLimitSet = false;

   abstract byte[] getBytes() throws SQLServerException;

   private static synchronized int nextLoggingID() {
      return ++lastLoggingID;
   }

   public final String toString() {
      if (this.traceID == null) {
         this.traceID = this.getClass().getName() + "ID:" + nextLoggingID();
      }

      return this.traceID;
   }

   final void setLoggingInfo(String var1) {
      this.parentLoggingInfo = var1;
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString());
      }
   }

   BaseInputStream(TDSReader var1, boolean var2, boolean var3, ServerDTVImpl var4) {
      this.tdsReader = var1;
      this.isAdaptive = var2;
      this.isStreaming = var3;
      if (var2) {
         this.clearCurrentMark();
      } else {
         this.currentMark = var1.mark();
      }

      this.dtv = var4;
   }

   final void clearCurrentMark() {
      this.currentMark = null;
      this.isReadLimitSet = false;
      if (this.isAdaptive && this.isStreaming) {
         this.tdsReader.stream();
      }
   }

   void closeHelper() throws IOException {
      if (this.isAdaptive && null != this.dtv) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + " closing the adaptive stream.");
         }

         this.dtv.setPositionAfterStreamed(this.tdsReader);
      }

      this.currentMark = null;
      this.tdsReader = null;
      this.dtv = null;
   }

   final void checkClosed() throws IOException {
      if (null == this.tdsReader) {
         throw new IOException(SQLServerException.getErrString("R_streamIsClosed"));
      }
   }

   public boolean markSupported() {
      return true;
   }

   void setReadLimit(int var1) {
      if (this.isAdaptive && var1 > 0) {
         this.readLimit = var1;
         this.isReadLimitSet = true;
      }
   }

   void resetHelper() throws IOException {
      this.checkClosed();
      if (null == this.currentMark) {
         throw new IOException(SQLServerException.getErrString("R_streamWasNotMarkedBefore"));
      }

      this.tdsReader.reset(this.currentMark);
   }
}
