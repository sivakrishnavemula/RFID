package com.microsoft.sqlserver.jdbc;

final class StreamSetterArgs {
   private long length;
   final StreamType streamType;

   final long getLength() {
      return this.length;
   }

   final void setLength(long var1) {
      assert -1L == this.length;
      assert var1 >= 0L;
      this.length = var1;
   }

   StreamSetterArgs(StreamType var1, long var2) {
      this.streamType = var1;
      this.length = var2;
   }
}
