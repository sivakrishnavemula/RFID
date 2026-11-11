package com.microsoft.sqlserver.jdbc;

import java.util.logging.Level;
import java.util.logging.Logger;

final class TDSParser {
   private static Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.TDS.TOKEN");

   static void parse(TDSReader var0, String var1) throws SQLServerException {
      parse(var0, new TDSTokenHandler(var1));
   }

   static void parse(TDSReader var0, TDSTokenHandler var1) throws SQLServerException {
      boolean var2 = logger.isLoggable(Level.FINEST);
      boolean var3 = true;
      boolean var4 = false;
      boolean var5 = false;

      while (var3) {
         int var6 = var0.peekTokenType();
         if (var2) {
            logger.finest(var0.toString() + ": " + var1.logContext + ": Processing " + (-1 == var6 ? "EOF" : TDS.getTokenName(var6)));
         }

         switch (var6) {
            case -1:
               var0.getCommand().onTokenEOF();
               var1.onEOF(var0);
               var3 = false;
               break;
            case 121:
               var3 = var1.onRetStatus(var0);
               break;
            case 129:
               var3 = var1.onColMetaData(var0);
               break;
            case 164:
               var3 = var1.onTabName(var0);
               break;
            case 165:
               var3 = var1.onColInfo(var0);
               break;
            case 169:
               var3 = var1.onOrder(var0);
               break;
            case 170:
               var3 = var1.onError(var0);
               break;
            case 171:
               var3 = var1.onInfo(var0);
               break;
            case 172:
               var3 = var1.onRetValue(var0);
               break;
            case 173:
               var4 = true;
               var3 = var1.onLoginAck(var0);
               break;
            case 174:
               var5 = true;
               var0.getConnection().processFeatureExtAck(var0);
               var3 = true;
               break;
            case 209:
               var3 = var1.onRow(var0);
               break;
            case 210:
               var3 = var1.onNBCRow(var0);
               break;
            case 227:
               var3 = var1.onEnvChange(var0);
               break;
            case 237:
               var3 = var1.onSSPI(var0);
               break;
            case 238:
               var3 = var1.onFedAuthInfo(var0);
               break;
            case 253:
            case 254:
            case 255:
               var0.getCommand().checkForInterrupt();
               var3 = var1.onDone(var0);
               break;
            default:
               throwUnexpectedTokenException(var0, var1.logContext);
         }
      }

      if (var4 && !var5) {
         var0.TryProcessFeatureExtAck(var5);
      }
   }

   static void throwUnexpectedTokenException(TDSReader var0, String var1) throws SQLServerException {
      if (logger.isLoggable(Level.SEVERE)) {
         logger.severe(var0.toString() + ": " + var1 + ": Encountered unexpected " + TDS.getTokenName(var0.peekTokenType()));
      }

      var0.throwInvalidTDSToken(TDS.getTokenName(var0.peekTokenType()));
   }

   static void ignoreLengthPrefixedToken(TDSReader var0) throws SQLServerException {
      var0.readUnsignedByte();
      int var1 = var0.readUnsignedShort();
      byte[] var2 = new byte[var1];
      var0.readBytes(var2, 0, var1);
   }
}
