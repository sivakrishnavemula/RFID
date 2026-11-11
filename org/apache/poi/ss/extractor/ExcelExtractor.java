package org.apache.poi.ss.extractor;

public interface ExcelExtractor {
   void setIncludeSheetNames(boolean var1);

   void setFormulasNotResults(boolean var1);

   void setIncludeHeadersFooters(boolean var1);

   void setIncludeCellComments(boolean var1);

   String getText();
}
