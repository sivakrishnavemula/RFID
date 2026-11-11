package com.microsoft.sqlserver.jdbc;

class Nanos {
   static final int PER_SECOND = 1000000000;
   static final int PER_MAX_SCALE_INTERVAL = 1000000000 / (int)Math.pow(10.0, 7.0);
   static final int PER_MILLISECOND = 1000000;
   static final long PER_DAY = 86400000000000L;

   private Nanos() {
   }
}
