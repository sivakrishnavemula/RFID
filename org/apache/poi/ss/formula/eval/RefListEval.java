package org.apache.poi.ss.formula.eval;

import java.util.ArrayList;
import java.util.List;

public class RefListEval implements ValueEval {
   private final List<ValueEval> list = new ArrayList();

   public RefListEval(ValueEval v1, ValueEval v2) {
      this.add(v1);
      this.add(v2);
   }

   private void add(ValueEval v) {
      if (v instanceof RefListEval) {
         this.list.addAll(((RefListEval)v).list);
      } else {
         this.list.add(v);
      }
   }

   public List<ValueEval> getList() {
      return this.list;
   }
}
