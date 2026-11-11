package com.microsoft.sqlserver.jdbc;

import java.util.logging.Level;
import java.util.logging.Logger;

abstract class TDSCommand {
   static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.TDS.Command");
   private final String logContext;
   private String traceID;
   private final TimeoutTimer timeoutTimer;
   private volatile TDSWriter tdsWriter;
   private volatile TDSReader tdsReader;
   private final Object interruptLock = new Object();
   private volatile boolean interruptsEnabled = false;
   private volatile boolean wasInterrupted = false;
   private volatile String interruptReason = null;
   private volatile boolean requestComplete;
   private volatile boolean attentionPending = false;
   private volatile boolean processedResponse;
   private volatile boolean readingResponse;
   private boolean interruptChecked = false;

   abstract boolean doExecute() throws SQLServerException;

   final String getLogContext() {
      return this.logContext;
   }

   public final String toString() {
      if (this.traceID == null) {
         this.traceID = "TDSCommand@" + Integer.toHexString(this.hashCode()) + " (" + this.logContext + ")";
      }

      return this.traceID;
   }

   final void log(Level var1, String var2) {
      logger.log(var1, this.toString() + ": " + var2);
   }

   private final boolean wasInterrupted() {
      return this.wasInterrupted;
   }

   boolean attentionPending() {
      return this.attentionPending;
   }

   final boolean readingResponse() {
      return this.readingResponse;
   }

   TDSCommand(String var1, int var2) {
      this.logContext = var1;
      this.timeoutTimer = var2 > 0 ? new TimeoutTimer(var2, this) : null;
   }

   boolean execute(TDSWriter var1, TDSReader var2) throws SQLServerException {
      this.tdsWriter = var1;
      this.tdsReader = var2;
      assert null != var2;

      try {
         return this.doExecute();
      } catch (SQLServerException var6) {
         SQLServerException var3 = var6;

         try {
            if (!this.requestComplete && !var2.getConnection().isClosed()) {
               this.interrupt(var3.getMessage());
               this.onRequestComplete();
               this.close();
            }
         } catch (SQLServerException var5) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.toString() + ": Ignoring error in sending attention: " + var5.getMessage());
            }
         }

         throw var6;
      }
   }

   void processResponse(TDSReader var1) throws SQLServerException {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + ": Processing response");
      }

      try {
         TDSParser.parse(var1, this.getLogContext());
      } catch (SQLServerException var3) {
         if (2 != var3.getDriverErrorCode()) {
            throw var3;
         }

         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + ": Ignoring error from database: " + var3.getMessage());
         }
      }
   }

   final void detach() throws SQLServerException {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this + ": detaching...");
      }

      while (this.tdsReader.readPacket()) {
      }

      assert !this.readingResponse;
   }

   final void close() {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this + ": closing...");
      }

      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this + ": processing response...");
      }

      while (!this.processedResponse) {
         try {
            this.processResponse(this.tdsReader);
         } catch (SQLServerException var4) {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this + ": close ignoring error processing response: " + var4.getMessage());
            }

            if (this.tdsReader.getConnection().isSessionUnAvailable()) {
               this.processedResponse = true;
               this.attentionPending = false;
            }
         }
      }

      if (this.attentionPending) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this + ": processing attention ack...");
         }

         try {
            TDSParser.parse(this.tdsReader, "attention ack");
         } catch (SQLServerException var3) {
            if (this.tdsReader.getConnection().isSessionUnAvailable()) {
               if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(this + ": giving up on attention ack after connection closed by exception: " + var3);
               }

               this.attentionPending = false;
            } else if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this + ": ignored exception: " + var3);
            }
         }

         if (this.attentionPending) {
            logger.severe(this + ": expected attn ack missing or not processed; terminating connection...");

            try {
               this.tdsReader.throwInvalidTDS();
            } catch (SQLServerException var2) {
               if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(this + ": ignored expected invalid TDS exception: " + var2);
               }

               assert this.tdsReader.getConnection().isSessionUnAvailable();
               this.attentionPending = false;
            }
         }
      }

      assert this.processedResponse && !this.attentionPending;
   }

   void interrupt(String var1) throws SQLServerException {
      synchronized (this.interruptLock) {
         if (this.interruptsEnabled && !this.wasInterrupted()) {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this + ": Raising interrupt for reason:" + var1);
            }

            this.wasInterrupted = true;
            this.interruptReason = var1;
            if (this.requestComplete) {
               this.attentionPending = this.tdsWriter.sendAttention();
            }
         }
      }
   }

   final void checkForInterrupt() throws SQLServerException {
      if (this.wasInterrupted() && !this.interruptChecked) {
         this.interruptChecked = true;
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this + ": throwing interrupt exception, reason: " + this.interruptReason);
         }

         throw new SQLServerException(this.interruptReason, SQLState.STATEMENT_CANCELED, DriverError.NOT_SET, null);
      }
   }

   final void onRequestComplete() throws SQLServerException {
      assert !this.requestComplete;
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this + ": request complete");
      }

      synchronized (this.interruptLock) {
         this.requestComplete = true;
         if (!this.interruptsEnabled) {
            assert !this.attentionPending;
            assert !this.processedResponse;
            assert !this.readingResponse;
            this.processedResponse = true;
         } else if (this.wasInterrupted()) {
            if (this.tdsWriter.isEOMSent()) {
               this.attentionPending = this.tdsWriter.sendAttention();
               this.readingResponse = this.attentionPending;
            } else {
               assert !this.attentionPending;
               this.readingResponse = this.tdsWriter.ignoreMessage();
            }

            this.processedResponse = !this.readingResponse;
         } else {
            assert !this.attentionPending;
            assert !this.processedResponse;
            this.readingResponse = true;
         }
      }
   }

   final void onResponseEOM() throws SQLServerException {
      boolean var1 = false;
      synchronized (this.interruptLock) {
         if (this.interruptsEnabled) {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this + ": disabling interrupts");
            }

            var1 = this.attentionPending;
            this.interruptsEnabled = false;
         }
      }

      if (var1) {
         this.tdsReader.readPacket();
      }

      this.readingResponse = false;
   }

   final void onTokenEOF() {
      this.processedResponse = true;
   }

   final void onAttentionAck() {
      assert this.attentionPending;
      this.attentionPending = false;
   }

   final TDSWriter startRequest(byte var1) throws SQLServerException {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this + ": starting request...");
      }

      try {
         this.tdsWriter.startMessage(this, var1);
      } catch (SQLServerException var5) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this + ": starting request: exception: " + var5.getMessage());
         }

         throw var5;
      }

      synchronized (this.interruptLock) {
         this.requestComplete = false;
         this.readingResponse = false;
         this.processedResponse = false;
         this.attentionPending = false;
         this.wasInterrupted = false;
         this.interruptReason = null;
         this.interruptsEnabled = true;
      }

      return this.tdsWriter;
   }

   final TDSReader startResponse() throws SQLServerException {
      return this.startResponse(false);
   }

   final TDSReader startResponse(boolean var1) throws SQLServerException {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this + ": finishing request");
      }

      try {
         this.tdsWriter.endMessage();
      } catch (SQLServerException var7) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this + ": finishing request: endMessage threw exception: " + var7.getMessage());
         }

         throw var7;
      }

      if (null != this.timeoutTimer) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + ": Starting timer...");
         }

         this.timeoutTimer.start();
      }

      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + ": Reading response...");
      }

      try {
         if (var1) {
            this.tdsReader.readPacket();
         } else {
            while (this.tdsReader.readPacket()) {
            }
         }
      } catch (SQLServerException var8) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + ": Exception reading response: " + var8.getMessage());
         }

         throw var8;
      } finally {
         if (null != this.timeoutTimer) {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this.toString() + ": Stopping timer...");
            }

            this.timeoutTimer.stop();
         }
      }

      return this.tdsReader;
   }
}
