package com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

final class SocketFinder {
   private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
   private static final int minTimeoutForParallelConnections = 1500;
   private final Object socketFinderlock = new Object();
   private final Object parentThreadLock = new Object();
   private volatile SocketFinder.Result result = SocketFinder.Result.UNKNOWN;
   private int noOfSpawnedThreads = 0;
   private volatile int noOfThreadsThatNotified = 0;
   private volatile Socket selectedSocket = null;
   private volatile IOException selectedException = null;
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SocketFinder");
   private final String traceID;
   private static final int ipAddressLimit = 64;
   private final SQLServerConnection conn;

   SocketFinder(String var1, SQLServerConnection var2) {
      this.traceID = "SocketFinder(" + var1 + ")";
      this.conn = var2;
   }

   Socket findSocket(String var1, int var2, int var3, boolean var4, boolean var5, boolean var6, int var7) throws SQLServerException {
      assert var3 != 0 : "The driver does not allow a time out of 0";

      try {
         InetAddress[] var8 = null;
         if (var4 || var5) {
            var8 = InetAddress.getAllByName(var1);
            if (var5 && var8.length > 64) {
               var5 = false;
               var3 = var7;
            }
         }

         if (!var4) {
            if (var5 && var6) {
               return this.getDefaultSocket(var1, var2, 500);
            }

            if (!var5) {
               return this.getDefaultSocket(var1, var2, var3);
            }
         }

         if (logger.isLoggable(Level.FINER)) {
            String var9 = this.toString() + " Total no of InetAddresses: " + var8.length + ". They are: ";

            for (InetAddress var13 : var8) {
               var9 = var9 + var13.toString() + ";";
            }

            logger.finer(var9);
         }

         if (var8.length > 64) {
            MessageFormat var20 = new MessageFormat(SQLServerException.getErrString("R_ipAddressLimitWithMultiSubnetFailover"));
            Object[] var24 = new Object[]{Integer.toString(64)};
            String var26 = var20.format(var24);
            this.conn.terminate(6, var26);
         }

         if (Util.isIBM()) {
            var3 = Math.max(var3, 1500);
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + "Using Java NIO with timeout:" + var3);
            }

            this.findSocketUsingJavaNIO(var8, var2, var3);
         } else {
            LinkedList var21 = new LinkedList();
            LinkedList var25 = new LinkedList();

            for (InetAddress var14 : var8) {
               if (var14 instanceof Inet4Address) {
                  var21.add((Inet4Address)var14);
               } else {
                  assert var14 instanceof Inet6Address : "Unexpected IP address " + var14.toString();
                  var25.add((Inet6Address)var14);
               }
            }

            int var28;
            if (!var21.isEmpty() && !var25.isEmpty()) {
               var28 = Math.max(var3 / 2, 1500);
            } else {
               var28 = Math.max(var3, 1500);
            }

            if (!var21.isEmpty()) {
               if (logger.isLoggable(Level.FINER)) {
                  logger.finer(this.toString() + "Using Java NIO with timeout:" + var28);
               }

               this.findSocketUsingJavaNIO((InetAddress[])var21.toArray(new InetAddress[0]), var2, var28);
            }

            if (!this.result.equals(SocketFinder.Result.SUCCESS) && !var25.isEmpty()) {
               if (var25.size() == 1) {
                  return this.getConnectedSocket((InetAddress)var25.get(0), var2, var28);
               }

               if (logger.isLoggable(Level.FINER)) {
                  logger.finer(this.toString() + "Using Threading with timeout:" + var28);
               }

               this.findSocketUsingThreading(var25, var2, var28);
            }
         }

         if (this.result.equals(SocketFinder.Result.UNKNOWN)) {
            synchronized (this.socketFinderlock) {
               if (this.result.equals(SocketFinder.Result.UNKNOWN)) {
                  this.result = SocketFinder.Result.FAILURE;
                  if (logger.isLoggable(Level.FINER)) {
                     logger.finer(this.toString() + " The parent thread updated the result to failure");
                  }
               }
            }
         }

         if (this.result.equals(SocketFinder.Result.FAILURE)) {
            if (this.selectedException == null) {
               if (logger.isLoggable(Level.FINER)) {
                  logger.finer(this.toString() + " There is no selectedException. The wait calls timed out before any connect call returned or timed out.");
               }

               String var23 = SQLServerException.getErrString("R_connectionTimedOut");
               this.selectedException = new IOException(var23);
            }

            throw this.selectedException;
         }
      } catch (InterruptedException var17) {
         this.close(this.selectedSocket);
         SQLServerException.ConvertConnectExceptionToSQLServerException(var1, var2, this.conn, var17);
      } catch (IOException var18) {
         this.close(this.selectedSocket);
         SQLServerException.ConvertConnectExceptionToSQLServerException(var1, var2, this.conn, var18);
      }

      assert this.result.equals(SocketFinder.Result.SUCCESS);
      assert this.selectedSocket != null : "Bug in code. Selected Socket cannot be null here.";
      return this.selectedSocket;
   }

   private void findSocketUsingJavaNIO(InetAddress[] var1, int var2, int var3) throws IOException {
      assert var3 != 0 : "The timeout cannot be zero";
      assert var1.length != 0 : "Number of inetAddresses should not be zero in this function";
      Selector var4 = null;
      LinkedList var5 = new LinkedList();
      SocketChannel var6 = null;

      try {
         var4 = Selector.open();

         for (int var7 = 0; var7 < var1.length; var7++) {
            SocketChannel var8 = SocketChannel.open();
            var5.add(var8);
            var8.configureBlocking(false);
            byte var9 = 8;
            SelectionKey var10 = var8.register(var4, var9);
            var8.connect(new InetSocketAddress(var1[var7], var2));
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " initiated connection to address: " + var1[var7] + ", portNumber: " + var2);
            }
         }

         long var30 = System.currentTimeMillis();
         long var31 = var30 + var3;
         int var11 = var1.length;

         while (true) {
            long var12 = var31 - var30;
            if (var12 <= 0L || var6 != null || var11 <= 0) {
               break;
            }

            int var14 = var4.select(var12);
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " no of channels ready: " + var14);
            }

            if (var14 != 0) {
               Set var15 = var4.selectedKeys();

               for (Iterator var16 = var15.iterator(); var16.hasNext(); var11--) {
                  SelectionKey var17 = (SelectionKey)var16.next();
                  SocketChannel var18 = (SocketChannel)var17.channel();
                  if (logger.isLoggable(Level.FINER)) {
                     logger.finer(this.toString() + " processing the channel :" + var18);
                  }

                  boolean var19 = false;

                  try {
                     var19 = var18.finishConnect();
                     assert var19 : "finishConnect on channel:" + var18 + " cannot be false";
                     var6 = var18;
                     if (logger.isLoggable(Level.FINER)) {
                        logger.finer(this.toString() + " selected the channel :" + var6);
                     }
                     break;
                  } catch (IOException var27) {
                     if (logger.isLoggable(Level.FINER)) {
                        logger.finer(
                           this.toString()
                              + " the exception: "
                              + var27.getClass()
                              + " with message: "
                              + var27.getMessage()
                              + " occured while processing the channel: "
                              + var18
                        );
                     }

                     this.updateSelectedException(var27, this.toString());
                     var18.close();
                     var17.cancel();
                     var16.remove();
                  }
               }
            }

            var30 = System.currentTimeMillis();
         }
      } catch (IOException var28) {
         this.close(var6);
         throw var28;
      } finally {
         this.close(var4);
         Iterator var22 = var5.iterator();

         while (var22.hasNext()) {
            SocketChannel var23 = (SocketChannel)var22.next();
            if (var23 != var6) {
               this.close(var23);
            }
         }
      }

      if (var6 != null) {
         var6.configureBlocking(true);
         this.selectedSocket = var6.socket();
         this.result = SocketFinder.Result.SUCCESS;
      }
   }

   private Socket getDefaultSocket(String var1, int var2, int var3) throws IOException {
      InetSocketAddress var4 = new InetSocketAddress(var1, var2);
      return this.getConnectedSocket(var4, var3);
   }

   private Socket getConnectedSocket(InetAddress var1, int var2, int var3) throws IOException {
      InetSocketAddress var4 = new InetSocketAddress(var1, var2);
      return this.getConnectedSocket(var4, var3);
   }

   private Socket getConnectedSocket(InetSocketAddress var1, int var2) throws IOException {
      assert var2 != 0 : "timeout cannot be zero";
      if (var1.isUnresolved()) {
         throw new UnknownHostException();
      }

      this.selectedSocket = new Socket();
      this.selectedSocket.connect(var1, var2);
      return this.selectedSocket;
   }

   private void findSocketUsingThreading(LinkedList<Inet6Address> var1, int var2, int var3) throws IOException, InterruptedException {
      assert var3 != 0 : "The timeout cannot be zero";
      assert !var1.isEmpty() : "Number of inetAddresses should not be zero in this function";
      LinkedList var4 = new LinkedList();
      LinkedList var5 = new LinkedList();

      try {
         this.noOfSpawnedThreads = var1.size();

         for (InetAddress var7 : var1) {
            Socket var8 = new Socket();
            var4.add(var8);
            InetSocketAddress var9 = new InetSocketAddress(var7, var2);
            SocketConnector var10 = new SocketConnector(var8, var9, var3, this);
            var5.add(var10);
         }

         synchronized (this.parentThreadLock) {
            for (SocketConnector var24 : var5) {
               threadPoolExecutor.execute(var24);
            }

            long var23 = System.currentTimeMillis();
            long var25 = var23 + var3;

            while (true) {
               long var11 = var25 - var23;
               if (logger.isLoggable(Level.FINER)) {
                  logger.finer(
                     this.toString()
                        + " TimeRemaining:"
                        + var11
                        + "; Result:"
                        + this.result
                        + "; Max. open thread count: "
                        + threadPoolExecutor.getLargestPoolSize()
                        + "; Current open thread count:"
                        + threadPoolExecutor.getActiveCount()
                  );
               }

               if (var11 <= 0L || !this.result.equals(SocketFinder.Result.UNKNOWN)) {
                  return;
               }

               this.parentThreadLock.wait(var11);
               if (logger.isLoggable(Level.FINER)) {
                  logger.finer(this.toString() + " The parent thread wokeup.");
               }

               var23 = System.currentTimeMillis();
            }
         }
      } finally {
         Iterator var15 = var4.iterator();

         while (var15.hasNext()) {
            Socket var16 = (Socket)var15.next();
            if (var16 != this.selectedSocket) {
               this.close(var16);
            }
         }
      }
   }

   SocketFinder.Result getResult() {
      return this.result;
   }

   void close(Selector var1) {
      if (null != var1) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + ": Closing Selector");
         }

         try {
            var1.close();
         } catch (IOException var3) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, this.toString() + ": Ignored the following error while closing Selector", var3);
            }
         }
      }
   }

   void close(Socket var1) {
      if (null != var1) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + ": Closing TCP socket:" + var1);
         }

         try {
            var1.close();
         } catch (IOException var3) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, this.toString() + ": Ignored the following error while closing socket", var3);
            }
         }
      }
   }

   void close(SocketChannel var1) {
      if (null != var1) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.toString() + ": Closing TCP socket channel:" + var1);
         }

         try {
            var1.close();
         } catch (IOException var3) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, this.toString() + "Ignored the following error while closing socketChannel", var3);
            }
         }
      }
   }

   void updateResult(Socket var1, IOException var2, String var3) {
      if (this.result.equals(SocketFinder.Result.UNKNOWN)) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer("The following child thread is waiting for socketFinderLock:" + var3);
         }

         synchronized (this.socketFinderlock) {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer("The following child thread acquired socketFinderLock:" + var3);
            }

            if (this.result.equals(SocketFinder.Result.UNKNOWN)) {
               if (var2 == null && this.selectedSocket == null) {
                  this.selectedSocket = var1;
                  this.result = SocketFinder.Result.SUCCESS;
                  if (logger.isLoggable(Level.FINER)) {
                     logger.finer("The socket of the following thread has been chosen:" + var3);
                  }
               }

               if (var2 != null) {
                  this.updateSelectedException(var2, var3);
               }
            }

            this.noOfThreadsThatNotified++;
            if (this.noOfThreadsThatNotified >= this.noOfSpawnedThreads && this.result.equals(SocketFinder.Result.UNKNOWN)) {
               this.result = SocketFinder.Result.FAILURE;
            }

            if (!this.result.equals(SocketFinder.Result.UNKNOWN)) {
               if (logger.isLoggable(Level.FINER)) {
                  logger.finer("The following child thread is waiting for parentThreadLock:" + var3);
               }

               synchronized (this.parentThreadLock) {
                  if (logger.isLoggable(Level.FINER)) {
                     logger.finer("The following child thread acquired parentThreadLock:" + var3);
                  }

                  this.parentThreadLock.notify();
               }

               if (logger.isLoggable(Level.FINER)) {
                  logger.finer("The following child thread released parentThreadLock and notified the parent thread:" + var3);
               }
            }
         }

         if (logger.isLoggable(Level.FINER)) {
            logger.finer("The following child thread released socketFinderLock:" + var3);
         }
      }
   }

   public void updateSelectedException(IOException var1, String var2) {
      boolean var3 = false;
      if (this.selectedException == null) {
         this.selectedException = var1;
         var3 = true;
      } else if (!(var1 instanceof SocketTimeoutException) && this.selectedException instanceof SocketTimeoutException) {
         this.selectedException = var1;
         var3 = true;
      }

      if (var3 && logger.isLoggable(Level.FINER)) {
         logger.finer(
            "The selected exception is updated to the following: ExceptionType:"
               + var1.getClass()
               + "; ExceptionMessage:"
               + var1.getMessage()
               + "; by the following thread:"
               + var2
         );
      }
   }

   public String toString() {
      return this.traceID;
   }

   enum Result {
      UNKNOWN,
      SUCCESS,
      FAILURE;
   }
}
