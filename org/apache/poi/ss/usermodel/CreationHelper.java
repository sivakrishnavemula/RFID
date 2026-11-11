package org.apache.poi.ss.usermodel;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

public interface CreationHelper {
   RichTextString createRichTextString(String var1);

   DataFormat createDataFormat();

   Hyperlink createHyperlink(HyperlinkType var1);

   FormulaEvaluator createFormulaEvaluator();

   ExtendedColor createExtendedColor();

   ClientAnchor createClientAnchor();

   AreaReference createAreaReference(String var1);

   AreaReference createAreaReference(CellReference var1, CellReference var2);
}
