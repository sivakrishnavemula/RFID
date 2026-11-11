package org.apache.poi.ss.formula;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class FormulaCellCache {
   private final Map<Object, FormulaCellCacheEntry> _formulaEntriesByCell = new HashMap();

   public FormulaCellCache() {
   }

   public CellCacheEntry[] getCacheEntries() {
      FormulaCellCacheEntry[] result = new FormulaCellCacheEntry[this._formulaEntriesByCell.size()];
      this._formulaEntriesByCell.values().toArray(result);
      return result;
   }

   public void clear() {
      this._formulaEntriesByCell.clear();
   }

   public FormulaCellCacheEntry get(EvaluationCell cell) {
      return (FormulaCellCacheEntry)this._formulaEntriesByCell.get(cell.getIdentityKey());
   }

   public void put(EvaluationCell cell, FormulaCellCacheEntry entry) {
      this._formulaEntriesByCell.put(cell.getIdentityKey(), entry);
   }

   public FormulaCellCacheEntry remove(EvaluationCell cell) {
      return (FormulaCellCacheEntry)this._formulaEntriesByCell.remove(cell.getIdentityKey());
   }

   public void applyOperation(FormulaCellCache.IEntryOperation operation) {
      Iterator<FormulaCellCacheEntry> i = this._formulaEntriesByCell.values().iterator();

      while (i.hasNext()) {
         operation.processEntry((FormulaCellCacheEntry)i.next());
      }
   }

   interface IEntryOperation {
      void processEntry(FormulaCellCacheEntry var1);
   }
}
