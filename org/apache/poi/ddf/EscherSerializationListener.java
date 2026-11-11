package org.apache.poi.ddf;

public interface EscherSerializationListener {
   void beforeRecordSerialize(int var1, short var2, EscherRecord var3);

   void afterRecordSerialize(int var1, short var2, int var3, EscherRecord var4);
}
