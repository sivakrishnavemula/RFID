package org.apache.poi.poifs.macros;

public interface Module {
   String getContent();

   Module.ModuleType geModuleType();

   enum ModuleType {
      Document,
      Module,
      Class;
   }
}
