package com.microsoft.sqlserver.jdbc;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;

final class SQLServerSymmetricKeyCache {
   static Object lock = new Object();
   private final ConcurrentHashMap<String, SQLServerSymmetricKey> cache = new ConcurrentHashMap();
   private static final SQLServerSymmetricKeyCache instance = new SQLServerSymmetricKeyCache();
   private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
      public Thread newThread(Runnable var1) {
         Thread var2 = Executors.defaultThreadFactory().newThread(var1);
         var2.setDaemon(true);
         return var2;
      }
   });
   private static final Logger aeLogger = Logger.getLogger("com.microsoft.sqlserver.jdbc.SQLServerSymmetricKeyCache");

   private SQLServerSymmetricKeyCache() {
   }

   static SQLServerSymmetricKeyCache getInstance() {
      return instance;
   }

   ConcurrentHashMap<String, SQLServerSymmetricKey> getCache() {
      return this.cache;
   }

   SQLServerSymmetricKey getKey(EncryptionKeyInfo var1, SQLServerConnection var2) throws SQLServerException {
      SQLServerSymmetricKey var3 = null;
      synchronized (lock) {
         String var5 = var2.getTrustedServerNameAE();
         assert null != var5 : "serverName should not be null in getKey.";
         StringBuffer var6 = new StringBuffer(var5);
         String var7 = null;
         var6.append(":");

         try {
            var6.append(DatatypeConverter.printBase64Binary(new String(var1.encryptedKey, "UTF-8").getBytes()));
         } catch (UnsupportedEncodingException var16) {
            MessageFormat var9 = new MessageFormat(SQLServerException.getErrString("R_unsupportedEncoding"));
            Object[] var10 = new Object[]{"UTF-8"};
            throw new SQLServerException(this, var9.format(var10), null, 0, false);
         }

         var6.append(":");
         var6.append(var1.keyStoreName);
         var7 = var6.toString();
         var6.setLength(0);
         if (aeLogger.isLoggable(Level.FINE)) {
            aeLogger.fine("Checking trusted master key path...");
         }

         Boolean[] var8 = new Boolean[1];
         List var20 = SQLServerConnection.getColumnEncryptionTrustedMasterKeyPaths(var5, var8);
         if (!var8[0] || null != var20 && 0 != var20.size() && var20.contains(var1.keyPath)) {
            if (aeLogger.isLoggable(Level.FINE)) {
               aeLogger.fine("Checking Symmetric key cache...");
            }

            if (!this.cache.containsKey(var7)) {
               SQLServerColumnEncryptionKeyStoreProvider var22 = var2.getSystemColumnEncryptionKeyStoreProvider(var1.keyStoreName);
               if (null == var22) {
                  var22 = SQLServerConnection.getGlobalSystemColumnEncryptionKeyStoreProvider(var1.keyStoreName);
               }

               if (null == var22) {
                  var22 = SQLServerConnection.getGlobalCustomColumnEncryptionKeyStoreProvider(var1.keyStoreName);
               }

               if (null == var22) {
                  String var24 = var2.getAllSystemColumnEncryptionKeyStoreProviders();
                  String var25 = SQLServerConnection.getAllGlobalCustomSystemColumnEncryptionKeyStoreProviders();
                  MessageFormat var13 = new MessageFormat(SQLServerException.getErrString("R_UnrecognizedKeyStoreProviderName"));
                  Object[] var14 = new Object[]{var1.keyStoreName, var24, var25};
                  throw new SQLServerException(this, var13.format(var14), null, 0, false);
               }

               byte[] var23 = var22.decryptColumnEncryptionKey(var1.keyPath, var1.algorithmName, var1.encryptedKey);
               var3 = new SQLServerSymmetricKey(var23);
               long var12 = SQLServerConnection.getColumnEncryptionKeyCacheTtl();
               if (0L != var12) {
                  this.cache.putIfAbsent(var7, var3);
                  if (aeLogger.isLoggable(Level.FINE)) {
                     aeLogger.fine("Adding encryption key to cache...");
                  }

                  scheduler.schedule(new CacheClear(var7), var12, TimeUnit.SECONDS);
               }
            } else {
               var3 = (SQLServerSymmetricKey)this.cache.get(var7);
            }

            return var3;
         } else {
            MessageFormat var21 = new MessageFormat(SQLServerException.getErrString("R_UntrustedKeyPath"));
            Object[] var11 = new Object[]{var1.keyPath, var5};
            throw new SQLServerException(this, var21.format(var11), null, 0, false);
         }
      }
   }
}
