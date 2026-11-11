package com.microsoft.sqlserver.jdbc;

import java.util.logging.Level;

final class TimeoutTimer implements Runnable {
   private final int timeoutSeconds;
   private final TDSCommand command;
   private Thread timerThread;
   private volatile boolean canceled = false;

   TimeoutTimer(int var1, TDSCommand var2) {
      assert var1 > 0;
      assert null != var2;
      this.timeoutSeconds = var1;
      this.command = var2;
   }

   final void start() {
      this.timerThread = new Thread(this);
      this.timerThread.setDaemon(true);
      this.timerThread.start();
   }

   final void stop() {
      this.canceled = true;
      this.timerThread.interrupt();
   }

   public void run() {
      int var1 = this.timeoutSeconds;

      try {
         do {
            if (this.canceled) {
               return;
            }

            Thread.sleep(1000L);
         } while (--var1 > 0);
      } catch (InterruptedException var4) {
         return;
      }

      try {
         this.command.interrupt(SQLServerException.getErrString("R_queryTimedOut"));
      } catch (SQLServerException var3) {
         this.command.log(Level.FINE, "Command could not be timed out. Reason: " + var3.getMessage());
      }
   }
}
