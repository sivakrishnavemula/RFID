package com.microsoft.sqlserver.jdbc;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SQLServerException extends SQLException {
   static final String EXCEPTION_XOPEN_CONNECTION_CANT_ESTABLISH = "08001";
   static final String EXCEPTION_XOPEN_CONNECTION_DOES_NOT_EXIST = "08003";
   static final String EXCEPTION_XOPEN_CONNECTION_FAILURE = "08006";
   static final String LOG_CLIENT_CONNECTION_ID_PREFIX = " ClientConnectionId:";
   static final int LOGON_FAILED = 18456;
   static final int PASSWORD_EXPIRED = 18488;
   static Logger exLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerException");
   static final int DRIVER_ERROR_NONE = 0;
   static final int DRIVER_ERROR_FROM_DATABASE = 2;
   static final int DRIVER_ERROR_IO_FAILED = 3;
   static final int DRIVER_ERROR_INVALID_TDS = 4;
   static final int DRIVER_ERROR_SSL_FAILED = 5;
   static final int DRIVER_ERROR_UNSUPPORTED_CONFIG = 6;
   static final int DRIVER_ERROR_INTERMITTENT_TLS_FAILED = 7;
   private int driverErrorCode = 0;

   final int getDriverErrorCode() {
      return this.driverErrorCode;
   }

   final void setDriverErrorCode(int var1) {
      this.driverErrorCode = var1;
   }

   private void logException(Object var1, String var2, boolean var3) {
      String var4 = "";
      if (var1 != null) {
         var4 = var1.toString();
      }

      if (exLogger.isLoggable(Level.FINE)) {
         exLogger.fine("*** SQLException:" + var4 + " " + this.toString() + " " + var2);
      }

      if (var3 && exLogger.isLoggable(Level.FINE)) {
         StringBuilder var5 = new StringBuilder(100);
         StackTraceElement[] var6 = this.getStackTrace();

         for (int var7 = 0; var7 < var6.length; var7++) {
            var5.append(var6[var7].toString());
         }

         Throwable var10 = this.getCause();
         if (var10 != null) {
            var5.append("\n caused by " + var10 + "\n");
            StackTraceElement[] var8 = var10.getStackTrace();

            for (int var9 = 0; var9 < var8.length; var9++) {
               var5.append(var8[var9].toString());
            }
         }

         exLogger.fine(var5.toString());
      }
   }

   static String getErrString(String var0) {
      return SQLServerResource.getResource(var0);
   }

   SQLServerException(String var1, SQLState var2, DriverError var3, Throwable var4) {
      this(var1, var2.getSQLStateCode(), var3.getErrorCode(), var4);
   }

   SQLServerException(String var1, String var2, int var3, Throwable var4) {
      super(var1, var2, var3);
      this.initCause(var4);
      this.logException(null, var1, true);
      ActivityCorrelator.setCurrentActivityIdSentFlag();
   }

   SQLServerException(String var1, Throwable var2) {
      super(var1);
      this.initCause(var2);
      this.logException(null, var1, true);
      ActivityCorrelator.setCurrentActivityIdSentFlag();
   }

   SQLServerException(Object var1, String var2, String var3, int var4, boolean var5) {
      super(var2, var3, var4);
      this.logException(var1, var2, var5);
      ActivityCorrelator.setCurrentActivityIdSentFlag();
   }

   SQLServerException(Object var1, String var2, String var3, StreamError var4, boolean var5) {
      super(var2, var3, var4.getErrorNumber());
      var2 = "Msg " + var4.getErrorNumber() + ", Level " + var4.getErrorSeverity() + ", State " + var4.getErrorState() + ", " + var2;
      this.logException(var1, var2, var5);
   }

   static void makeFromDriverError(SQLServerConnection var0, Object var1, String var2, String var3, boolean var4) throws SQLServerException {
      String var5 = "";
      if (var3 != null) {
         var5 = var3;
      }

      if (var0 == null || !var0.xopenStates) {
         var5 = mapFromXopen(var3);
      }

      SQLServerException var6 = new SQLServerException(var1, checkAndAppendClientConnId(var2, var0), var5, 0, var4);
      if (null != var3 && var3.equals("08006") && null != var0) {
         var0.notifyPooledConnection(var6);
         var0.close();
      }

      throw var6;
   }

   static void makeFromDatabaseError(SQLServerConnection var0, Object var1, String var2, StreamError var3, boolean var4) throws SQLServerException {
      String var5 = generateStateCode(var0, var3.getErrorNumber(), var3.getErrorState());
      SQLServerException var6 = new SQLServerException(var1, checkAndAppendClientConnId(var2, var0), var5, var3, var4);
      var6.setDriverErrorCode(2);
      if (var3.getErrorSeverity() >= 20 && null != var0) {
         var0.notifyPooledConnection(var6);
         var0.close();
      }

      throw var6;
   }

   static void ConvertConnectExceptionToSQLServerException(String var0, int var1, SQLServerConnection var2, Exception var3) throws SQLServerException {
      Exception var4 = var3;
      if (var4 != null) {
         MessageFormat var5 = new MessageFormat(getErrString("R_tcpOpenFailed"));
         Object[] var6 = new Object[]{var4.getMessage()};
         MessageFormat var7 = new MessageFormat(getErrString("R_tcpipConnectionFailed"));
         Object[] var8 = new Object[]{var0, Integer.toString(var1), var5.format(var6)};
         String var9 = var7.format(var8);
         makeFromDriverError(var2, var2, var9, "08001", false);
      }
   }

   static String mapFromXopen(String var0) {
      if (var0 == null) {
         return null;
      } else if (var0.equals("07009")) {
         return "S1093";
      } else if (var0.equals("08001")) {
         return "08S01";
      } else {
         return var0.equals("08006") ? "08S01" : "";
      }
   }

   static String generateStateCode(SQLServerConnection var0, int var1, int var2) {
      boolean var3 = var0 != null && var0.xopenStates;
      if (var3) {
         switch (var1) {
            case 207:
               return "42S22";
            case 208:
               return "42S02";
            case 2714:
               return "42S01";
            case 4060:
               return "08001";
            case 18456:
               return "08001";
            default:
               return "42000";
         }
      } else {
         switch (var1) {
            case 208:
               return "S0002";
            case 515:
            case 547:
               return "23000";
            case 1205:
               return "40001";
            case 2601:
               return "23000";
            case 2627:
               return "23000";
            case 2714:
               return "S0001";
            case 8152:
               return "22001";
            default:
               return "S000" + var2;
         }
      }
   }

   static String checkAndAppendClientConnId(String var0, SQLServerConnection var1) throws SQLServerException {
      if (null != var1 && var1.attachConnId()) {
         UUID var2 = var1.getClientConIdInternal();
         assert null != var2;
         StringBuilder var3 = new StringBuilder(var0);
         var3.append(" ClientConnectionId:");
         var3.append(var2.toString());
         return var3.toString();
      } else {
         return var0;
      }
   }
}
