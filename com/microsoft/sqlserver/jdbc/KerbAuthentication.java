package com.microsoft.sqlserver.jdbc;

import java.net.IDN;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

final class KerbAuthentication extends SSPIAuthentication {
   private static final String CONFIGNAME = "SQLJDBCDriver";
   private static final Logger authLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.KerbAuthentication");
   private final SQLServerConnection con;
   private final String spn;
   private final GSSManager manager = GSSManager.getInstance();
   private LoginContext lc = null;
   private GSSCredential peerCredentials = null;
   private GSSContext peerContext = null;

   private void intAuthInit() throws SQLServerException {
      try {
         Oid var1 = new Oid("1.2.840.113554.1.2.2");
         Subject var2 = null;

         try {
            AccessControlContext var3 = AccessController.getContext();
            var2 = Subject.getSubject(var3);
            if (null == var2) {
               this.lc = new LoginContext("SQLJDBCDriver");
               this.lc.login();
               var2 = this.lc.getSubject();
            }
         } catch (LoginException var4) {
            this.con.terminate(0, SQLServerException.getErrString("R_integratedAuthenticationFailed"), var4);
         }

         GSSName var7 = this.manager.createName(this.spn, null);
         if (authLogger.isLoggable(Level.FINER)) {
            authLogger.finer(this.toString() + " Getting client credentials");
         }

         this.peerCredentials = getClientCredential(var2, this.manager, var1);
         if (authLogger.isLoggable(Level.FINER)) {
            authLogger.finer(this.toString() + " creating security context");
         }

         this.peerContext = this.manager.createContext(var7, var1, this.peerCredentials, 0);
         this.peerContext.requestCredDeleg(true);
         this.peerContext.requestMutualAuth(true);
         this.peerContext.requestInteg(true);
      } catch (GSSException var5) {
         authLogger.finer(this.toString() + "initAuthInit failed GSSException:-" + var5);
         this.con.terminate(0, SQLServerException.getErrString("R_integratedAuthenticationFailed"), var5);
      } catch (PrivilegedActionException var6) {
         authLogger.finer(this.toString() + "initAuthInit failed privileged exception:-" + var6);
         this.con.terminate(0, SQLServerException.getErrString("R_integratedAuthenticationFailed"), var6);
      }
   }

   private static GSSCredential getClientCredential(Subject var0, final GSSManager var1, final Oid var2) throws PrivilegedActionException {
      PrivilegedExceptionAction var3 = new PrivilegedExceptionAction<GSSCredential>() {
         public GSSCredential run() throws GSSException {
            return var1.createCredential(null, 0, var2, 1);
         }
      };
      Object var4 = Subject.doAs(var0, var3);
      return (GSSCredential)var4;
   }

   private byte[] intAuthHandShake(byte[] var1, boolean[] var2) throws SQLServerException {
      try {
         if (authLogger.isLoggable(Level.FINER)) {
            authLogger.finer(this.toString() + " Sending token to server over secure context");
         }

         byte[] var3 = this.peerContext.initSecContext(var1, 0, var1.length);
         if (this.peerContext.isEstablished()) {
            var2[0] = true;
            if (authLogger.isLoggable(Level.FINER)) {
               authLogger.finer(this.toString() + "Authentication done.");
            }
         } else if (null == var3) {
            authLogger.info(this.toString() + "byteToken is null in initSecContext.");
            this.con.terminate(0, SQLServerException.getErrString("R_integratedAuthenticationFailed"));
         }

         return var3;
      } catch (GSSException var4) {
         authLogger.finer(this.toString() + "initSecContext Failed :-" + var4);
         this.con.terminate(0, SQLServerException.getErrString("R_integratedAuthenticationFailed"), var4);
         return null;
      }
   }

   private String makeSpn(String var1, int var2) throws SQLServerException {
      if (authLogger.isLoggable(Level.FINER)) {
         authLogger.finer(this.toString() + " Server: " + var1 + " port: " + var2);
      }

      StringBuilder var3 = new StringBuilder("MSSQLSvc/");
      if (this.con.serverNameAsACE()) {
         var3.append(IDN.toASCII(var1));
      } else {
         var3.append(var1);
      }

      var3.append(":");
      var3.append(var2);
      String var4 = var3.toString();
      if (authLogger.isLoggable(Level.FINER)) {
         authLogger.finer(this.toString() + " SPN: " + var4);
      }

      return var4;
   }

   KerbAuthentication(SQLServerConnection var1, String var2, int var3) throws SQLServerException {
      this.con = var1;
      String var4 = var1.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.SERVER_SPN.toString());
      if (null != var4) {
         if (var1.serverNameAsACE()) {
            int var5 = var4.indexOf("/");
            this.spn = var4.substring(0, var5 + 1) + IDN.toASCII(var4.substring(var5 + 1));
         } else {
            this.spn = var4;
         }
      } else {
         this.spn = this.makeSpn(var2, var3);
      }
   }

   byte[] GenerateClientContext(byte[] var1, boolean[] var2) throws SQLServerException {
      if (null == this.peerContext) {
         this.intAuthInit();
      }

      return this.intAuthHandShake(var1, var2);
   }

   int ReleaseClientContext() throws SQLServerException {
      try {
         if (null != this.peerCredentials) {
            this.peerCredentials.dispose();
         }

         if (null != this.peerContext) {
            this.peerContext.dispose();
         }

         if (null != this.lc) {
            this.lc.logout();
         }
      } catch (LoginException var2) {
         authLogger.fine(this.toString() + " Release of the credentials failed LoginException: " + var2);
      } catch (GSSException var3) {
         authLogger.fine(this.toString() + " Release of the credentials failed GSSException: " + var3);
      }

      return 0;
   }

   static {
      new KerbAuthentication$1SQLJDBCDriverConfig();
   }
}
