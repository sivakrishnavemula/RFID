package com.microsoft.sqlserver.jdbc;

import java.text.MessageFormat;

class DLLException extends Exception {
   private int category = -9;
   private int status = -9;
   private int state = -9;
   private int errCode = -1;
   private String param1 = "";
   private String param2 = "";
   private String param3 = "";

   DLLException(String var1, int var2, int var3, int var4) {
      super(var1);
      this.category = var2;
      this.status = var3;
      this.state = var4;
   }

   DLLException(String var1, String var2, String var3, int var4) {
      this.errCode = var4;
      this.param1 = var1;
      this.param2 = var2;
      this.param3 = var3;
   }

   int GetCategory() {
      return this.category;
   }

   int GetStatus() {
      return this.status;
   }

   int GetState() {
      return this.state;
   }

   int GetErrCode() {
      return this.errCode;
   }

   String GetParam1() {
      return this.param1;
   }

   String GetParam2() {
      return this.param2;
   }

   String GetParam3() {
      return this.param3;
   }

   static void buildException(int var0, String var1, String var2, String var3) throws SQLServerException {
      String var4 = getErrMessage(var0);
      MessageFormat var5 = new MessageFormat(SQLServerException.getErrString(var4));
      Object[] var6 = new Object[]{null, null, null};
      buildMsgParams(var4, var6, var1, var2, var3);
      throw new SQLServerException(null, var5.format(var6), null, 0, false);
   }

   private static void buildMsgParams(String var0, Object[] var1, String var2, String var3, String var4) {
      if (var0.equalsIgnoreCase("R_AECertLocBad")) {
         var1[0] = var2;
         var1[1] = var2 + "/" + var3 + "/" + var4;
      } else if (var0.equalsIgnoreCase("R_AECertStoreBad")) {
         var1[0] = var3;
         var1[1] = var2 + "/" + var3 + "/" + var4;
      } else if (var0.equalsIgnoreCase("R_AECertHashEmpty")) {
         var1[0] = var2 + "/" + var3 + "/" + var4;
      } else {
         var1[0] = var2;
         var1[1] = var3;
         var1[2] = var4;
      }
   }

   private static String getErrMessage(int var0) {
      String var1 = null;
      switch (var0) {
         case 1:
            var1 = "R_AEKeypathEmpty";
            break;
         case 2:
            var1 = "R_EncryptedCEKNull";
            break;
         case 3:
            var1 = "R_NullKeyEncryptionAlgorithm";
            break;
         case 4:
            var1 = "R_AEWinApiErr";
            break;
         case 5:
            var1 = "R_AECertpathBad";
            break;
         case 6:
            var1 = "R_AECertLocBad";
            break;
         case 7:
            var1 = "R_AECertStoreBad";
            break;
         case 8:
            var1 = "R_AECertHashEmpty";
            break;
         case 9:
            var1 = "R_AECertNotFound";
            break;
         case 10:
            var1 = "R_AEMaloc";
            break;
         case 11:
            var1 = "R_EmptyEncryptedCEK";
            break;
         case 12:
            var1 = "R_InvalidKeyEncryptionAlgorithm";
            break;
         case 13:
            var1 = "R_AEKeypathLong";
            break;
         case 14:
            var1 = "R_InvalidEcryptionAlgorithmVersion";
            break;
         case 15:
            var1 = "R_AEECEKLenBad";
            break;
         case 16:
            var1 = "R_AEECEKSigLenBad";
            break;
         case 17:
            var1 = "R_InvalidCertificateSignature";
            break;
         default:
            var1 = "R_AEWinApiErr";
      }

      return var1;
   }
}
