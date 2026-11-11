package com.microsoft.sqlserver.jdbc;

final class InputStreamGetterArgs {
   final StreamType streamType;
   final boolean isAdaptive;
   final boolean isStreaming;
   final String logContext;
   static final InputStreamGetterArgs defaultArgs = new InputStreamGetterArgs(StreamType.NONE, false, false, "");

   static final InputStreamGetterArgs getDefaultArgs() {
      return defaultArgs;
   }

   InputStreamGetterArgs(StreamType var1, boolean var2, boolean var3, String var4) {
      this.streamType = var1;
      this.isAdaptive = var2;
      this.isStreaming = var3;
      this.logContext = var4;
   }
}
