package org.apache.poi.sl.usermodel;

import java.util.List;

public interface Slide<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends Sheet<S, P> {
   Notes<S, P> getNotes();

   void setNotes(Notes<S, P> var1);

   boolean getFollowMasterBackground();

   void setFollowMasterBackground(boolean var1);

   boolean getFollowMasterColourScheme();

   void setFollowMasterColourScheme(boolean var1);

   boolean getFollowMasterObjects();

   void setFollowMasterObjects(boolean var1);

   int getSlideNumber();

   String getTitle();

   boolean getDisplayPlaceholder(Placeholder var1);

   void setHidden(boolean var1);

   boolean isHidden();

   List<? extends Comment> getComments();

   MasterSheet getSlideLayout();

   String getSlideName();
}
