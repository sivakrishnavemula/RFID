package org.apache.poi.ss.formula;

import org.apache.poi.ss.formula.eval.ValueEval;

interface IEvaluationListener {
   void onCacheHit(int var1, int var2, int var3, ValueEval var4);

   void onReadPlainValue(int var1, int var2, int var3, IEvaluationListener.ICacheEntry var4);

   void onStartEvaluate(EvaluationCell var1, IEvaluationListener.ICacheEntry var2);

   void onEndEvaluate(IEvaluationListener.ICacheEntry var1, ValueEval var2);

   void onClearWholeCache();

   void onClearCachedValue(IEvaluationListener.ICacheEntry var1);

   void sortDependentCachedValues(IEvaluationListener.ICacheEntry[] var1);

   void onClearDependentCachedValue(IEvaluationListener.ICacheEntry var1, int var2);

   void onChangeFromBlankValue(int var1, int var2, int var3, EvaluationCell var4, IEvaluationListener.ICacheEntry var5);

   interface ICacheEntry {
      ValueEval getValue();
   }
}
