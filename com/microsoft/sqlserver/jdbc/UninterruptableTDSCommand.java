package com.microsoft.sqlserver.jdbc;

abstract class UninterruptableTDSCommand extends TDSCommand {
   UninterruptableTDSCommand(String var1) {
      super(var1, 0);
   }

   final void interrupt(String var1) throws SQLServerException {
      logger.finest(this.toString() + " Ignoring interrupt of uninterruptable TDS command; Reason:" + var1);
   }
}
