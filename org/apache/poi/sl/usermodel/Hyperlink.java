package org.apache.poi.sl.usermodel;

public interface Hyperlink<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends org.apache.poi.common.usermodel.Hyperlink {
   void linkToEmail(String var1);

   void linkToUrl(String var1);

   void linkToSlide(Slide<S, P> var1);

   void linkToNextSlide();

   void linkToPreviousSlide();

   void linkToFirstSlide();

   void linkToLastSlide();
}
