package com.microsoft.sqlserver.jdbc;

import java.util.logging.Logger;

final class AuthenticationJNI extends SSPIAuthentication {
   private static final int maximumpointersize = 128;
   private static boolean enabled = false;
   private static Logger authLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.AuthenticationJNI");
   private static int sspiBlobMaxlen = 0;
   private byte[] sniSec = new byte[128];
   private int[] sniSecLen = new int[]{0};
   private final String DNSName;
   private final int port;
   private SQLServerConnection con;
   private static final UnsatisfiedLinkError linkError;

   static int GetMaxSSPIBlobSize() {
      return sspiBlobMaxlen;
   }

   AuthenticationJNI(SQLServerConnection var1, String var2, int var3) throws SQLServerException {
      if (!enabled) {
         var1.terminate(0, SQLServerException.getErrString("R_notConfiguredForIntegrated"), linkError);
      }

      this.con = var1;
      this.DNSName = GetDNSName(var2);
      this.port = var3;
   }

   static FedAuthDllInfo getAccessTokenForWindowsIntegrated(String var0, String var1, String var2, String var3, long var4) throws DLLException {
      return ADALGetAccessTokenForWindowsIntegrated(var0, var1, var2, var3, var4, authLogger);
   }

   static FedAuthDllInfo getAccessToken(String var0, String var1, String var2, String var3, String var4, String var5, long var6) throws DLLException {
      return ADALGetAccessToken(var0, var1, var2, var3, var4, var5, var6, authLogger);
   }

   byte[] GenerateClientContext(byte[] var1, boolean[] var2) throws SQLServerException {
      int[] var4 = new int[]{GetMaxSSPIBlobSize()};
      byte[] var3 = new byte[var4[0]];
      assert this.DNSName != null;
      int var5 = SNISecGenClientContext(this.sniSec, this.sniSecLen, var1, var1.length, var3, var4, var2, this.DNSName, this.port, null, null, authLogger);
      if (var5 != 0) {
         authLogger.warning(this.toString() + " Authentication failed code : " + var5);
         this.con.terminate(0, SQLServerException.getErrString("R_integratedAuthenticationFailed"), linkError);
      }

      byte[] var6 = new byte[var4[0]];
      System.arraycopy(var3, 0, var6, 0, var4[0]);
      return var6;
   }

   int ReleaseClientContext() {
      int var1 = 0;
      if (this.sniSecLen[0] > 0) {
         var1 = SNISecReleaseClientContext(this.sniSec, this.sniSecLen[0], authLogger);
         this.sniSecLen[0] = 0;
      }

      return var1;
   }

   private static String GetDNSName(String var0) {
      String[] var1 = new String[1];
      if (GetDNSName(var0, var1, authLogger) != 0) {
         var1[0] = var0;
      }

      return var1[0];
   }

   private static native int SNISecGenClientContext(
      byte[] var0, int[] var1, byte[] var2, int var3, byte[] var4, int[] var5, boolean[] var6, String var7, int var8, String var9, String var10, Logger var11
   );

   private static native int SNISecReleaseClientContext(byte[] var0, int var1, Logger var2);

   private static native int SNISecInitPackage(int[] var0, Logger var1);

   private static native int SNISecTerminatePackage(Logger var0);

   private static native int SNIGetSID(byte[] var0, Logger var1);

   private static native boolean SNIIsEqualToCurrentSID(byte[] var0, Logger var1);

   private static native int GetDNSName(String var0, String[] var1, Logger var2);

   private static native FedAuthDllInfo ADALGetAccessTokenForWindowsIntegrated(String var0, String var1, String var2, String var3, long var4, Logger var6);

   private static native FedAuthDllInfo ADALGetAccessToken(String var0, String var1, String var2, String var3, String var4, String var5, long var6, Logger var8);

   static native byte[] DecryptColumnEncryptionKey(String var0, String var1, byte[] var2) throws DLLException;

   static {
      UnsatisfiedLinkError var0 = null;

      try {
         String var1 = "sqljdbc_auth";
         System.loadLibrary(var1);
         int[] var2 = new int[]{0};
         if (0 != SNISecInitPackage(var2, authLogger)) {
            throw new UnsatisfiedLinkError();
         }

         sspiBlobMaxlen = var2[0];
         enabled = true;
      } catch (UnsatisfiedLinkError var6) {
         var0 = var6;
         authLogger.warning("Failed to load the sqljdbc_auth.dll cause : " + var6.getMessage());
      } finally {
         linkError = var0;
      }
   }
}
