package com.microsoft.sqlserver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

final class TDSChannel {
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.TDS.Channel");
   private final String traceID;
   private final SQLServerConnection con;
   private final TDSWriter tdsWriter;
   private Socket tcpSocket;
   private SSLSocket sslSocket;
   private Socket channelSocket;
   TDSChannel.ProxySocket proxySocket = null;
   private InputStream tcpInputStream;
   private OutputStream tcpOutputStream;
   private InputStream inputStream;
   private OutputStream outputStream;
   private static Logger packetLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.TDS.DATA");
   private final boolean isLoggingPackets = packetLogger.isLoggable(Level.FINEST);
   int numMsgsSent = 0;
   int numMsgsRcvd = 0;
   private int spid = 0;
   private static final String SEPARATOR = System.getProperty("file.separator");
   private static final String JAVA_HOME = System.getProperty("java.home");
   private static final String JAVA_SECURITY = JAVA_HOME + SEPARATOR + "lib" + SEPARATOR + "security";
   private static final String JSSECACERTS = JAVA_SECURITY + SEPARATOR + "jssecacerts";
   private static final String CACERTS = JAVA_SECURITY + SEPARATOR + "cacerts";

   final Logger getLogger() {
      return logger;
   }

   public final String toString() {
      return this.traceID;
   }

   final TDSWriter getWriter() {
      return this.tdsWriter;
   }

   final TDSReader getReader(TDSCommand var1) {
      return new TDSReader(this, this.con, var1);
   }

   final boolean isLoggingPackets() {
      return this.isLoggingPackets;
   }

   void setSPID(int var1) {
      this.spid = var1;
   }

   int getSPID() {
      return this.spid;
   }

   void resetPooledConnection() {
      this.tdsWriter.resetPooledConnection();
   }

   TDSChannel(SQLServerConnection var1) {
      this.con = var1;
      this.traceID = "TDSChannel (" + var1.toString() + ")";
      this.tcpSocket = null;
      this.sslSocket = null;
      this.channelSocket = null;
      this.tcpInputStream = null;
      this.tcpOutputStream = null;
      this.inputStream = null;
      this.outputStream = null;
      this.tdsWriter = new TDSWriter(this, var1);
   }

   final void open(String var1, int var2, int var3, boolean var4, boolean var5, boolean var6, int var7) throws SQLServerException {
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + ": Opening TCP socket...");
      }

      SocketFinder var8 = new SocketFinder(this.traceID, this.con);
      this.channelSocket = this.tcpSocket = var8.findSocket(var1, var2, var3, var4, var5, var6, var7);

      try {
         this.tcpSocket.setTcpNoDelay(true);
         this.tcpSocket.setKeepAlive(true);
         this.inputStream = this.tcpInputStream = this.tcpSocket.getInputStream();
         this.outputStream = this.tcpOutputStream = this.tcpSocket.getOutputStream();
      } catch (IOException var10) {
         SQLServerException.ConvertConnectExceptionToSQLServerException(var1, var2, this.con, var10);
      }
   }

   void disableSSL() {
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " Disabling SSL...");
      }

      ByteArrayInputStream var1 = new ByteArrayInputStream(new byte[0]);

      try {
         var1.close();
      } catch (IOException var6) {
         logger.fine("Ignored error closing InputStream: " + var6.getMessage());
      }

      ByteArrayOutputStream var2 = new ByteArrayOutputStream();

      try {
         var2.close();
      } catch (IOException var5) {
         logger.fine("Ignored error closing OutputStream: " + var5.getMessage());
      }

      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.toString() + " Rewiring proxy streams for SSL socket close");
      }

      this.proxySocket.setStreams(var1, var2);

      try {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + " Closing SSL socket");
         }

         this.sslSocket.close();
      } catch (IOException var4) {
         logger.fine("Ignored error closing SSLSocket: " + var4.getMessage());
      }

      this.proxySocket = null;
      this.inputStream = this.tcpInputStream;
      this.outputStream = this.tcpOutputStream;
      this.channelSocket = this.tcpSocket;
      this.sslSocket = null;
      if (logger.isLoggable(Level.FINER)) {
         logger.finer(this.toString() + " SSL disabled");
      }
   }

   void enableSSL(String var1, int var2) throws SQLServerException {
      Provider var3 = null;
      Provider var4 = null;
      Provider var5 = null;
      String var6 = null;
      TDSChannel.SSLHandhsakeState var7 = TDSChannel.SSLHandhsakeState.SSL_HANDHSAKE_NOT_STARTED;

      try {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + " Enabling SSL...");
         }

         String var8 = this.con.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.TRUST_STORE.toString());
         String var24 = this.con.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.TRUST_STORE_PASSWORD.toString());
         String var25 = this.con.activeConnectionProperties.getProperty(SQLServerDriverStringProperty.HOSTNAME_IN_CERTIFICATE.toString());
         assert 0 == this.con.getRequestedEncryptionLevel() || 1 == this.con.getRequestedEncryptionLevel();
         assert 0 == this.con.getNegotiatedEncryptionLevel() || 1 == this.con.getNegotiatedEncryptionLevel() || 3 == this.con.getNegotiatedEncryptionLevel();
         TrustManager[] var26 = null;
         if (0 != this.con.getRequestedEncryptionLevel() && (1 != this.con.getRequestedEncryptionLevel() || !this.con.trustServerCertificate())) {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " SSL handshake will validate server certificate");
            }

            KeyStore var12 = null;
            if (null == var8 && null == var24) {
               if (logger.isLoggable(Level.FINER)) {
                  logger.finer(this.toString() + " Using system default trust store and password");
               }
            } else {
               if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(this.toString() + " Finding key store interface");
               }

               var12 = KeyStore.getInstance("JKS");
               var5 = var12.getProvider();
               InputStream var13 = this.loadTrustStore(var8);
               if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(this.toString() + " Loading key store");
               }

               try {
                  var12.load(var13, null == var24 ? null : var24.toCharArray());
               } finally {
                  this.con.activeConnectionProperties.remove(SQLServerDriverStringProperty.TRUST_STORE_PASSWORD.toString());
                  if (null != var13) {
                     try {
                        var13.close();
                     } catch (IOException var21) {
                        if (logger.isLoggable(Level.FINE)) {
                           logger.fine(this.toString() + " Ignoring error closing trust material InputStream...");
                        }
                     }
                  }
               }
            }

            TrustManagerFactory var31 = null;
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this.toString() + " Locating X.509 trust manager factory");
            }

            var6 = TrustManagerFactory.getDefaultAlgorithm();
            var31 = TrustManagerFactory.getInstance(var6);
            var3 = var31.getProvider();
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this.toString() + " Getting trust manager");
            }

            var31.init(var12);
            var26 = var31.getTrustManagers();
            if (null != var25) {
               var26 = new TrustManager[]{new TDSChannel.HostNameOverrideX509TrustManager(this, (X509TrustManager)var26[0], var25)};
            } else {
               var26 = new TrustManager[]{new TDSChannel.HostNameOverrideX509TrustManager(this, (X509TrustManager)var26[0], var1)};
            }
         } else {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " SSL handshake will trust any certificate");
            }

            var26 = new TrustManager[]{new TDSChannel.PermissiveX509TrustManager(this)};
         }

         SSLContext var29 = null;
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Getting TLS or better SSL context");
         }

         var29 = SSLContext.getInstance("TLS");
         var4 = var29.getProvider();
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Initializing SSL context");
         }

         var29.init(null, var26, null);
         this.proxySocket = new TDSChannel.ProxySocket(this);
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Creating SSL socket");
         }

         this.sslSocket = (SSLSocket)var29.getSocketFactory().createSocket(this.proxySocket, var1, var2, false);
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + " Starting SSL handshake");
         }

         var7 = TDSChannel.SSLHandhsakeState.SSL_HANDHSAKE_STARTED;
         this.sslSocket.startHandshake();
         var7 = TDSChannel.SSLHandhsakeState.SSL_HANDHSAKE_COMPLETE;
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Rewiring proxy streams after handshake");
         }

         this.proxySocket.setStreams(this.inputStream, this.outputStream);
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Getting SSL InputStream");
         }

         this.inputStream = this.sslSocket.getInputStream();
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + " Getting SSL OutputStream");
         }

         this.outputStream = this.sslSocket.getOutputStream();
         this.channelSocket = this.sslSocket;
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + " SSL enabled");
         }
      } catch (Exception var23) {
         if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, var23.getMessage(), var23);
         }

         if (logger.isLoggable(Level.FINER)) {
            logger.log(
               Level.FINER,
               "java.security path: "
                  + JAVA_SECURITY
                  + "\n"
                  + "Security providers: "
                  + Arrays.asList(Security.getProviders())
                  + "\n"
                  + (null != var4 ? "SSLContext provider info: " + var4.getInfo() + "\n" + "SSLContext provider services:\n" + var4.getServices() + "\n" : "")
                  + (null != var3 ? "TrustManagerFactory provider info: " + var3.getInfo() + "\n" : "")
                  + (null != var6 ? "TrustManagerFactory default algorithm: " + var6 + "\n" : "")
                  + (null != var5 ? "KeyStore provider info: " + var5.getInfo() + "\n" : "")
                  + "java.ext.dirs: "
                  + System.getProperty("java.ext.dirs")
            );
         }

         MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_sslFailed"));
         Object[] var10 = new Object[]{var23.getMessage()};
         String var11 = var23.getLocalizedMessage();
         if (-1 != var11.indexOf(" ClientConnectionId:")) {
            var11 = var11.substring(0, var11.indexOf(" ClientConnectionId:"));
         }

         if (var23 instanceof IOException
            && TDSChannel.SSLHandhsakeState.SSL_HANDHSAKE_STARTED == var7
            && var11.equals(SQLServerException.getErrString("R_truncatedServerResponse"))) {
            this.con.terminate(7, var9.format(var10), var23);
         } else {
            this.con.terminate(5, var9.format(var10), var23);
         }
      }
   }

   final InputStream loadTrustStore(String var1) {
      FileInputStream var2 = null;
      if (null != var1) {
         try {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this.toString() + " Opening specified trust store: " + var1);
            }

            var2 = new FileInputStream(var1);
         } catch (FileNotFoundException var7) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.toString() + " Trust store not found: " + var7.getMessage());
            }
         }
      } else if (null != (var1 = System.getProperty("javax.net.ssl.trustStore"))) {
         try {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this.toString() + " Opening default trust store (from javax.net.ssl.trustStore): " + var1);
            }

            var2 = new FileInputStream(var1);
         } catch (FileNotFoundException var6) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.toString() + " Trust store not found: " + var6.getMessage());
            }
         }
      } else {
         try {
            if (logger.isLoggable(Level.FINEST)) {
               logger.finest(this.toString() + " Opening default trust store: " + JSSECACERTS);
            }

            var2 = new FileInputStream(JSSECACERTS);
         } catch (FileNotFoundException var5) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.toString() + " Trust store not found: " + var5.getMessage());
            }
         }

         if (null == var2) {
            try {
               if (logger.isLoggable(Level.FINEST)) {
                  logger.finest(this.toString() + " Opening default trust store: " + CACERTS);
               }

               var2 = new FileInputStream(CACERTS);
            } catch (FileNotFoundException var4) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(this.toString() + " Trust store not found: " + var4.getMessage());
               }
            }
         }
      }

      return var2;
   }

   final int read(byte[] var1, int var2, int var3) throws SQLServerException {
      try {
         return this.inputStream.read(var1, var2, var3);
      } catch (IOException var5) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.toString() + " read failed:" + var5.getMessage());
         }

         this.con.terminate(3, var5.getMessage());
         return 0;
      }
   }

   final void write(byte[] var1, int var2, int var3) throws SQLServerException {
      try {
         this.outputStream.write(var1, var2, var3);
      } catch (IOException var5) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + " write failed:" + var5.getMessage());
         }

         this.con.terminate(3, var5.getMessage());
      }
   }

   final void flush() throws SQLServerException {
      try {
         this.outputStream.flush();
      } catch (IOException var2) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + " flush failed:" + var2.getMessage());
         }

         this.con.terminate(3, var2.getMessage());
      }
   }

   final void close() {
      if (null != this.sslSocket) {
         this.disableSSL();
      }

      if (null != this.inputStream) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + ": Closing inputStream...");
         }

         try {
            this.inputStream.close();
         } catch (IOException var4) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, this.toString() + ": Ignored error closing inputStream", var4);
            }
         }
      }

      if (null != this.outputStream) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(this.toString() + ": Closing outputStream...");
         }

         try {
            this.outputStream.close();
         } catch (IOException var3) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, this.toString() + ": Ignored error closing outputStream", var3);
            }
         }
      }

      if (null != this.tcpSocket) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + ": Closing TCP socket...");
         }

         try {
            this.tcpSocket.close();
         } catch (IOException var2) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, this.toString() + ": Ignored error closing socket", var2);
            }
         }
      }
   }

   void logPacket(byte[] var1, int var2, int var3, String var4) {
      assert 0 <= var3 && var3 <= var1.length;
      assert 0 <= var2 && var2 <= var1.length;
      char[] var5 = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
      char[] var6 = new char[]{
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         ' ',
         '!',
         '"',
         '#',
         '$',
         '%',
         '&',
         '\'',
         '(',
         ')',
         '*',
         '+',
         ',',
         '-',
         '.',
         '/',
         '0',
         '1',
         '2',
         '3',
         '4',
         '5',
         '6',
         '7',
         '8',
         '9',
         ':',
         ';',
         '<',
         '=',
         '>',
         '?',
         '@',
         'A',
         'B',
         'C',
         'D',
         'E',
         'F',
         'G',
         'H',
         'I',
         'J',
         'K',
         'L',
         'M',
         'N',
         'O',
         'P',
         'Q',
         'R',
         'S',
         'T',
         'U',
         'V',
         'W',
         'X',
         'Y',
         'Z',
         '[',
         '\\',
         ']',
         '^',
         '_',
         '`',
         'a',
         'b',
         'c',
         'd',
         'e',
         'f',
         'g',
         'h',
         'i',
         'j',
         'k',
         'l',
         'm',
         'n',
         'o',
         'p',
         'q',
         'r',
         's',
         't',
         'u',
         'v',
         'w',
         'x',
         'y',
         'z',
         '{',
         '|',
         '}',
         '~',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.'
      };
      char[] var7 = new char[]{
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         ' ',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.',
         '.'
      };
      char[] var8 = new char[var7.length];
      System.arraycopy(var7, 0, var8, 0, var7.length);
      StringBuilder var9 = new StringBuilder(var4.length() + 4 * var3 + 4 * (1 + var3 / 16) + 80);
      var9.append(this.tcpSocket.getLocalAddress().toString() + ":" + this.tcpSocket.getLocalPort() + " SPID:" + this.spid + " " + var4 + "\r\n");
      int var10 = 0;

      while (true) {
         int var11;
         for (var11 = 0; var11 < 16 && var10 < var3; var10++) {
            int var12 = (var1[var2 + var10] + 256) % 256;
            var8[3 * var11] = var5[var12 / 16];
            var8[3 * var11 + 1] = var5[var12 % 16];
            var8[50 + var11] = var6[var12];
            var11++;
         }

         for (int var13 = var11; var13 < 16; var13++) {
            var8[3 * var13] = ' ';
            var8[3 * var13 + 1] = ' ';
         }

         var9.append(var8, 0, 50 + var11);
         if (var10 == var3) {
            packetLogger.finest(var9.toString());
            return;
         }

         var9.append("\r\n");
      }
   }

   private final class HostNameOverrideX509TrustManager implements X509TrustManager {
      private final Logger logger;
      private final String logContext;
      private final X509TrustManager defaultTrustManager;
      private String hostName;

      HostNameOverrideX509TrustManager(TDSChannel var2, X509TrustManager var3, String var4) {
         this.logger = var2.getLogger();
         this.logContext = var2.toString() + " (HostNameOverrideX509TrustManager):";
         this.defaultTrustManager = var3;
         this.hostName = var4.toLowerCase();
      }

      private String parseCommonName(String var1) {
         int var2 = var1.indexOf("cn=");
         if (var2 == -1) {
            return null;
         }

         var1 = var1.substring(var2 + 3);
         var2 = 0;

         while (var2 < var1.length() && var1.charAt(var2) != ',') {
            var2++;
         }

         String var3 = var1.substring(0, var2);
         if (var3.length() > 1 && '"' == var3.charAt(0)) {
            if ('"' == var3.charAt(var3.length() - 1)) {
               var3 = var3.substring(1, var3.length() - 1);
            } else {
               var3 = null;
            }
         }

         return var3;
      }

      private boolean validateServerName(String var1) throws CertificateException {
         if (null == var1) {
            if (this.logger.isLoggable(Level.FINER)) {
               this.logger.finer(this.logContext + " Failed to parse the name from the certificate or name is empty.");
            }

            return false;
         } else if (!var1.equals(this.hostName)) {
            if (this.logger.isLoggable(Level.FINER)) {
               this.logger.finer(this.logContext + " The name in certificate " + var1 + " does not match with the server name " + this.hostName + ".");
            }

            return false;
         } else {
            if (this.logger.isLoggable(Level.FINER)) {
               this.logger.finer(this.logContext + " The name in certificate:" + var1 + " validated against server name " + this.hostName + ".");
            }

            return true;
         }
      }

      public void checkClientTrusted(X509Certificate[] var1, String var2) throws CertificateException {
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Forwarding ClientTrusted.");
         }

         this.defaultTrustManager.checkClientTrusted(var1, var2);
      }

      public void checkServerTrusted(X509Certificate[] var1, String var2) throws CertificateException {
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Forwarding Trusting server certificate");
         }

         this.defaultTrustManager.checkServerTrusted(var1, var2);
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " default serverTrusted succeeded proceeding with server name validation");
         }

         this.validateServerNameInCertificate(var1[0]);
      }

      private void validateServerNameInCertificate(X509Certificate var1) throws CertificateException {
         String var2 = var1.getSubjectX500Principal().getName("canonical");
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.logContext + " Validating the server name:" + this.hostName);
            this.logger.finer(this.logContext + " The DN name in certificate:" + var2);
         }

         boolean var3 = false;
         String var4 = this.parseCommonName(var2);
         var3 = this.validateServerName(var4);
         if (!var3) {
            Collection var5 = var1.getSubjectAlternativeNames();
            if (var5 != null) {
               for (List var7 : var5) {
                  if (var7 != null && var7.size() >= 2) {
                     Object var8 = var7.get(0);
                     Object var9 = var7.get(1);
                     if (this.logger.isLoggable(Level.FINER)) {
                        this.logger
                           .finer(
                              this.logContext
                                 + "Key: "
                                 + var8
                                 + "; KeyClass:"
                                 + (var8 != null ? var8.getClass() : null)
                                 + ";value: "
                                 + var9
                                 + "; valueClass:"
                                 + (var9 != null ? var9.getClass() : null)
                           );
                     }

                     if (var8 != null && var8 instanceof Integer && (Integer)var8 == 2) {
                        if (var9 != null && var9 instanceof String) {
                           String var10 = (String)var9;
                           var10 = var10.toUpperCase(Locale.US);
                           var10 = var10.toLowerCase(Locale.US);
                           var3 = this.validateServerName(var10);
                           if (var3) {
                              if (this.logger.isLoggable(Level.FINER)) {
                                 this.logger.finer(this.logContext + " found a valid name in certificate: " + var10);
                              }
                              break;
                           }
                        }

                        if (this.logger.isLoggable(Level.FINER)) {
                           this.logger.finer(this.logContext + " the following name in certificate does not match the serverName: " + var9);
                        }
                     }
                  } else if (this.logger.isLoggable(Level.FINER)) {
                     this.logger.finer(this.logContext + " found an invalid san entry: " + var7);
                  }
               }
            }
         }

         if (!var3) {
            String var12 = SQLServerException.getErrString("R_certNameFailed");
            throw new CertificateException(var12);
         }
      }

      public X509Certificate[] getAcceptedIssuers() {
         return this.defaultTrustManager.getAcceptedIssuers();
      }
   }

   private final class PermissiveX509TrustManager implements X509TrustManager {
      private final TDSChannel tdsChannel;
      private final Logger logger;
      private final String logContext;

      PermissiveX509TrustManager(TDSChannel var2) {
         this.tdsChannel = var2;
         this.logger = var2.getLogger();
         this.logContext = var2.toString() + " (PermissiveX509TrustManager):";
      }

      public void checkClientTrusted(X509Certificate[] var1, String var2) throws CertificateException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.logContext + " Trusting client certificate (!)");
         }
      }

      public void checkServerTrusted(X509Certificate[] var1, String var2) throws CertificateException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.logContext + " Trusting server certificate");
         }
      }

      public X509Certificate[] getAcceptedIssuers() {
         return new X509Certificate[0];
      }
   }

   private final class ProxyInputStream extends InputStream {
      private InputStream filteredStream;
      private final byte[] oneByte = new byte[1];

      ProxyInputStream(InputStream var2) {
         this.filteredStream = var2;
      }

      final void setFilteredStream(InputStream var1) {
         this.filteredStream = var1;
      }

      public long skip(long var1) throws IOException {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Skipping " + var1 + " bytes");
         }

         long var3 = this.filteredStream.skip(var1);
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Skipped " + var1 + " bytes");
         }

         return var3;
      }

      public int available() throws IOException {
         int var1 = this.filteredStream.available();
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " " + var1 + " bytes available");
         }

         return var1;
      }

      public int read() throws IOException {
         int var1;
         while (0 == (var1 = this.readInternal(this.oneByte, 0, this.oneByte.length))) {
         }

         assert 1 == var1 || -1 == var1;
         return 1 == var1 ? this.oneByte[0] : -1;
      }

      public int read(byte[] var1) throws IOException {
         return this.readInternal(var1, 0, var1.length);
      }

      public int read(byte[] var1, int var2, int var3) throws IOException {
         return this.readInternal(var1, var2, var3);
      }

      private int readInternal(byte[] var1, int var2, int var3) throws IOException {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Reading " + var3 + " bytes");
         }

         int var4;
         try {
            var4 = this.filteredStream.read(var1, var2, var3);
         } catch (IOException var6) {
            if (TDSChannel.logger.isLoggable(Level.FINER)) {
               TDSChannel.logger.finer(this.toString() + " " + var6.getMessage());
            }

            TDSChannel.logger.finer(this.toString() + " Reading bytes threw exception:" + var6.getMessage());
            throw var6;
         }

         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Read " + var4 + " bytes");
         }

         return var4;
      }

      public boolean markSupported() {
         boolean var1 = this.filteredStream.markSupported();
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Returning markSupported: " + var1);
         }

         return var1;
      }

      public void mark(int var1) {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Marking next " + var1 + " bytes");
         }

         this.filteredStream.mark(var1);
      }

      public void reset() throws IOException {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Resetting to previous mark");
         }

         this.filteredStream.reset();
      }

      public void close() throws IOException {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Closing");
         }

         this.filteredStream.close();
      }
   }

   final class ProxyOutputStream extends OutputStream {
      private OutputStream filteredStream;
      private final byte[] singleByte = new byte[1];

      ProxyOutputStream(OutputStream var2) {
         this.filteredStream = var2;
      }

      final void setFilteredStream(OutputStream var1) {
         this.filteredStream = var1;
      }

      public void close() throws IOException {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Closing");
         }

         this.filteredStream.close();
      }

      public void flush() throws IOException {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Flushing");
         }

         this.filteredStream.flush();
      }

      public void write(int var1) throws IOException {
         this.singleByte[0] = (byte)(var1 & 0xFF);
         this.writeInternal(this.singleByte, 0, this.singleByte.length);
      }

      public void write(byte[] var1) throws IOException {
         this.writeInternal(var1, 0, var1.length);
      }

      public void write(byte[] var1, int var2, int var3) throws IOException {
         this.writeInternal(var1, var2, var3);
      }

      private void writeInternal(byte[] var1, int var2, int var3) throws IOException {
         if (TDSChannel.logger.isLoggable(Level.FINEST)) {
            TDSChannel.logger.finest(this.toString() + " Writing " + var3 + " bytes");
         }

         this.filteredStream.write(var1, var2, var3);
      }
   }

   private class ProxySocket extends Socket {
      private final TDSChannel tdsChannel;
      private final Logger logger;
      private final String logContext;
      private final TDSChannel.ProxyInputStream proxyInputStream;
      private final TDSChannel.ProxyOutputStream proxyOutputStream;

      ProxySocket(TDSChannel var2) {
         this.tdsChannel = var2;
         this.logger = var2.getLogger();
         this.logContext = var2.toString() + " (ProxySocket):";
         TDSChannel.SSLHandshakeOutputStream var3 = TDSChannel.this.new SSLHandshakeOutputStream(var2);
         TDSChannel.SSLHandshakeInputStream var4 = TDSChannel.this.new SSLHandshakeInputStream(var2, var3);
         this.proxyOutputStream = TDSChannel.this.new ProxyOutputStream(var3);
         this.proxyInputStream = TDSChannel.this.new ProxyInputStream(var4);
      }

      void setStreams(InputStream var1, OutputStream var2) {
         this.proxyInputStream.setFilteredStream(var1);
         this.proxyOutputStream.setFilteredStream(var2);
      }

      public InputStream getInputStream() throws IOException {
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Getting input stream");
         }

         return this.proxyInputStream;
      }

      public OutputStream getOutputStream() throws IOException {
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Getting output stream");
         }

         return this.proxyOutputStream;
      }

      public InetAddress getInetAddress() {
         return this.tdsChannel.tcpSocket.getInetAddress();
      }

      public boolean getKeepAlive() throws SocketException {
         return this.tdsChannel.tcpSocket.getKeepAlive();
      }

      public InetAddress getLocalAddress() {
         return this.tdsChannel.tcpSocket.getLocalAddress();
      }

      public int getLocalPort() {
         return this.tdsChannel.tcpSocket.getLocalPort();
      }

      public SocketAddress getLocalSocketAddress() {
         return this.tdsChannel.tcpSocket.getLocalSocketAddress();
      }

      public boolean getOOBInline() throws SocketException {
         return this.tdsChannel.tcpSocket.getOOBInline();
      }

      public int getPort() {
         return this.tdsChannel.tcpSocket.getPort();
      }

      public int getReceiveBufferSize() throws SocketException {
         return this.tdsChannel.tcpSocket.getReceiveBufferSize();
      }

      public SocketAddress getRemoteSocketAddress() {
         return this.tdsChannel.tcpSocket.getRemoteSocketAddress();
      }

      public boolean getReuseAddress() throws SocketException {
         return this.tdsChannel.tcpSocket.getReuseAddress();
      }

      public int getSendBufferSize() throws SocketException {
         return this.tdsChannel.tcpSocket.getSendBufferSize();
      }

      public int getSoLinger() throws SocketException {
         return this.tdsChannel.tcpSocket.getSoLinger();
      }

      public int getSoTimeout() throws SocketException {
         return this.tdsChannel.tcpSocket.getSoTimeout();
      }

      public boolean getTcpNoDelay() throws SocketException {
         return this.tdsChannel.tcpSocket.getTcpNoDelay();
      }

      public int getTrafficClass() throws SocketException {
         return this.tdsChannel.tcpSocket.getTrafficClass();
      }

      public boolean isBound() {
         return true;
      }

      public boolean isClosed() {
         return false;
      }

      public boolean isConnected() {
         return true;
      }

      public boolean isInputShutdown() {
         return false;
      }

      public boolean isOutputShutdown() {
         return false;
      }

      public String toString() {
         return this.tdsChannel.tcpSocket.toString();
      }

      public SocketChannel getChannel() {
         return null;
      }

      public void bind(SocketAddress var1) throws IOException {
         this.logger.finer(this.logContext + " Disallowed call to bind.  Throwing IOException.");
         throw new IOException();
      }

      public void connect(SocketAddress var1) throws IOException {
         this.logger.finer(this.logContext + " Disallowed call to connect (without timeout).  Throwing IOException.");
         throw new IOException();
      }

      public void connect(SocketAddress var1, int var2) throws IOException {
         this.logger.finer(this.logContext + " Disallowed call to connect (with timeout).  Throwing IOException.");
         throw new IOException();
      }

      public void close() throws IOException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.logContext + " Ignoring close");
         }
      }

      public void setReceiveBufferSize(int var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setReceiveBufferSize size:" + var1);
         }
      }

      public void setSendBufferSize(int var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setSendBufferSize size:" + var1);
         }
      }

      public void setReuseAddress(boolean var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setReuseAddress");
         }
      }

      public void setSoLinger(boolean var1, int var2) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setSoLinger");
         }
      }

      public void setSoTimeout(int var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setSoTimeout");
         }
      }

      public void setTcpNoDelay(boolean var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setTcpNoDelay");
         }
      }

      public void setTrafficClass(int var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setTrafficClass");
         }
      }

      public void shutdownInput() throws IOException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring shutdownInput");
         }
      }

      public void shutdownOutput() throws IOException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring shutdownOutput");
         }
      }

      public void sendUrgentData(int var1) throws IOException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring sendUrgentData");
         }
      }

      public void setKeepAlive(boolean var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setKeepAlive");
         }
      }

      public void setOOBInline(boolean var1) throws SocketException {
         if (this.logger.isLoggable(Level.FINER)) {
            this.logger.finer(this.toString() + " Ignoring setOOBInline");
         }
      }
   }

   enum SSLHandhsakeState {
      SSL_HANDHSAKE_NOT_STARTED,
      SSL_HANDHSAKE_STARTED,
      SSL_HANDHSAKE_COMPLETE;
   }

   private class SSLHandshakeInputStream extends InputStream {
      private final TDSReader tdsReader;
      private final TDSChannel.SSLHandshakeOutputStream sslHandshakeOutputStream;
      private final Logger logger;
      private final String logContext;
      private final byte[] oneByte = new byte[1];

      SSLHandshakeInputStream(TDSChannel var2, TDSChannel.SSLHandshakeOutputStream var3) {
         this.tdsReader = var2.getReader(null);
         this.sslHandshakeOutputStream = var3;
         this.logger = var2.getLogger();
         this.logContext = var2.toString() + " (SSLHandshakeInputStream):";
      }

      private final void ensureSSLPayload() throws IOException {
         if (0 == this.tdsReader.available()) {
            if (this.logger.isLoggable(Level.FINEST)) {
               this.logger.finest(this.logContext + " No handshake response bytes available. Flushing SSL handshake output stream.");
            }

            try {
               this.sslHandshakeOutputStream.endMessage();
            } catch (SQLServerException var3) {
               this.logger.finer(this.logContext + " Ending TDS message threw exception:" + var3.getMessage());
               throw new IOException(var3.getMessage());
            }

            if (this.logger.isLoggable(Level.FINEST)) {
               this.logger.finest(this.logContext + " Reading first packet of SSL handshake response");
            }

            try {
               this.tdsReader.readPacket();
            } catch (SQLServerException var2) {
               this.logger.finer(this.logContext + " Reading response packet threw exception:" + var2.getMessage());
               throw new IOException(var2.getMessage());
            }
         }
      }

      public long skip(long var1) throws IOException {
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Skipping " + var1 + " bytes...");
         }

         if (var1 <= 0L) {
            return 0L;
         }

         if (var1 > 2147483647L) {
            var1 = 2147483647L;
         }

         this.ensureSSLPayload();

         try {
            this.tdsReader.skip((int)var1);
            return var1;
         } catch (SQLServerException var4) {
            this.logger.finer(this.logContext + " Skipping bytes threw exception:" + var4.getMessage());
            throw new IOException(var4.getMessage());
         }
      }

      public int read() throws IOException {
         int var1;
         while (0 == (var1 = this.readInternal(this.oneByte, 0, this.oneByte.length))) {
         }

         assert 1 == var1 || -1 == var1;
         return 1 == var1 ? this.oneByte[0] : -1;
      }

      public int read(byte[] var1) throws IOException {
         return this.readInternal(var1, 0, var1.length);
      }

      public int read(byte[] var1, int var2, int var3) throws IOException {
         return this.readInternal(var1, var2, var3);
      }

      private int readInternal(byte[] var1, int var2, int var3) throws IOException {
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Reading " + var3 + " bytes...");
         }

         this.ensureSSLPayload();

         try {
            this.tdsReader.readBytes(var1, var2, var3);
            return var3;
         } catch (SQLServerException var5) {
            this.logger.finer(this.logContext + " Reading bytes threw exception:" + var5.getMessage());
            throw new IOException(var5.getMessage());
         }
      }
   }

   private class SSLHandshakeOutputStream extends OutputStream {
      private final TDSWriter tdsWriter;
      private boolean messageStarted;
      private final Logger logger;
      private final String logContext;
      private final byte[] singleByte = new byte[1];

      SSLHandshakeOutputStream(TDSChannel var2) {
         this.tdsWriter = var2.getWriter();
         this.messageStarted = false;
         this.logger = var2.getLogger();
         this.logContext = var2.toString() + " (SSLHandshakeOutputStream):";
      }

      public void flush() throws IOException {
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Ignored a request to flush the stream");
         }
      }

      void endMessage() throws SQLServerException {
         assert this.messageStarted;
         if (this.logger.isLoggable(Level.FINEST)) {
            this.logger.finest(this.logContext + " Finishing TDS message");
         }

         this.tdsWriter.endMessage();
         this.messageStarted = false;
      }

      public void write(int var1) throws IOException {
         this.singleByte[0] = (byte)(var1 & 0xFF);
         this.writeInternal(this.singleByte, 0, this.singleByte.length);
      }

      public void write(byte[] var1) throws IOException {
         this.writeInternal(var1, 0, var1.length);
      }

      public void write(byte[] var1, int var2, int var3) throws IOException {
         this.writeInternal(var1, var2, var3);
      }

      private void writeInternal(byte[] var1, int var2, int var3) throws IOException {
         try {
            if (!this.messageStarted) {
               if (this.logger.isLoggable(Level.FINEST)) {
                  this.logger.finest(this.logContext + " Starting new TDS packet...");
               }

               this.tdsWriter.startMessage(null, (byte)18);
               this.messageStarted = true;
            }

            if (this.logger.isLoggable(Level.FINEST)) {
               this.logger.finest(this.logContext + " Writing " + var3 + " bytes...");
            }

            this.tdsWriter.writeBytes(var1, var2, var3);
         } catch (SQLServerException var5) {
            this.logger.finer(this.logContext + " Writing bytes threw exception:" + var5.getMessage());
            throw new IOException(var5.getMessage());
         }
      }
   }
}
