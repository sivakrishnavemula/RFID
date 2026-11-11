package com.microsoft.sqlserver.jdbc;

import com.microsoft.sqlserver.jdbc.SocketFinder.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

final class SocketConnector implements Runnable {
   private final Socket socket;
   private final SocketFinder socketFinder;
   private final InetSocketAddress inetSocketAddress;
   private final int timeoutInMilliseconds;
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SocketConnector");
   private final String traceID;
   private final String threadID;
   private static long lastThreadID = 0L;

   SocketConnector(Socket var1, InetSocketAddress var2, int var3, SocketFinder var4) {
      this.socket = var1;
      this.inetSocketAddress = var2;
      this.timeoutInMilliseconds = var3;
      this.socketFinder = var4;
      this.threadID = Long.toString(nextThreadID());
      this.traceID = "SocketConnector:" + this.threadID + "(" + var4.toString() + ")";
   }

   public void run() {
      IOException var1 = null;
      Result var2 = this.socketFinder.getResult();
      if (var2.equals(Result.UNKNOWN)) {
         try {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(this.toString() + " connecting to InetSocketAddress:" + this.inetSocketAddress + " with timeout:" + this.timeoutInMilliseconds);
            }

            this.socket.connect(this.inetSocketAddress, this.timeoutInMilliseconds);
         } catch (IOException var4) {
            if (logger.isLoggable(Level.FINER)) {
               logger.finer(
                  this.toString()
                     + " exception:"
                     + var4.getClass()
                     + " with message:"
                     + var4.getMessage()
                     + " occured while connecting to InetSocketAddress:"
                     + this.inetSocketAddress
               );
            }

            var1 = var4;
         }

         this.socketFinder.updateResult(this.socket, var1, this.toString());
      }
   }

   public String toString() {
      return this.traceID;
   }

   private static synchronized long nextThreadID() {
      if (lastThreadID == Long.MAX_VALUE) {
         if (logger.isLoggable(Level.FINER)) {
            logger.finer("Resetting the Id count");
         }

         lastThreadID = 1L;
      } else {
         lastThreadID++;
      }

      return lastThreadID;
   }
}
