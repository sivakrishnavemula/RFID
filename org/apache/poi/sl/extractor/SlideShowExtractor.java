package org.apache.poi.sl.extractor;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.sl.usermodel.Comment;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.Notes;
import org.apache.poi.sl.usermodel.ObjectShape;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.PlaceholderDetails;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.TableCell;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

public class SlideShowExtractor<S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>> extends POITextExtractor {
   private static final POILogger LOG = POILogFactory.getLogger(SlideShowExtractor.class);
   private SlideShow<S, P> slideshow;
   private boolean slidesByDefault = true;
   private boolean notesByDefault;
   private boolean commentsByDefault;
   private boolean masterByDefault;

   public SlideShowExtractor(SlideShow<S, P> slideshow) {
      this.setFilesystem(slideshow);
      this.slideshow = slideshow;
   }

   public final Object getDocument() {
      return this.slideshow.getPersistDocument();
   }

   public void setSlidesByDefault(boolean slidesByDefault) {
      this.slidesByDefault = slidesByDefault;
   }

   public void setNotesByDefault(boolean notesByDefault) {
      this.notesByDefault = notesByDefault;
   }

   public void setCommentsByDefault(boolean commentsByDefault) {
      this.commentsByDefault = commentsByDefault;
   }

   public void setMasterByDefault(boolean masterByDefault) {
      this.masterByDefault = masterByDefault;
   }

   public POITextExtractor getMetadataTextExtractor() {
      return this.slideshow.getMetadataTextExtractor();
   }

   public String getText() {
      StringBuilder sb = new StringBuilder();

      for (Slide<S, P> slide : this.slideshow.getSlides()) {
         sb.append(this.getText(slide));
      }

      return sb.toString();
   }

   public String getText(Slide<S, P> slide) {
      StringBuilder sb = new StringBuilder();
      if (this.slidesByDefault) {
         this.printShapeText(slide, sb);
      }

      if (this.masterByDefault) {
         MasterSheet<S, P> ms = slide.getMasterSheet();
         this.printSlideMaster(ms, sb);
         MasterSheet<S, P> sl = slide.getSlideLayout();
         if (sl != ms) {
            this.printSlideMaster(sl, sb);
         }
      }

      if (this.commentsByDefault) {
         this.printComments(slide, sb);
      }

      if (this.notesByDefault) {
         this.printNotes(slide, sb);
      }

      return sb.toString();
   }

   private void printSlideMaster(MasterSheet<S, P> master, StringBuilder sb) {
      if (master != null) {
         for (Shape<S, P> shape : master) {
            if (shape instanceof TextShape) {
               TextShape<S, P> ts = (TextShape<S, P>)shape;
               String text = ts.getText();
               if (text != null && !text.isEmpty() && !"*".equals(text)) {
                  if (ts.isPlaceholder()) {
                     LOG.log(3, new Object[]{"Ignoring boiler plate (placeholder) text on slide master:", text});
                  } else {
                     sb.append(text);
                     if (!text.endsWith("\n")) {
                        sb.append("\n");
                     }
                  }
               }
            }
         }
      }
   }

   private String printHeaderReturnFooter(Sheet<S, P> sheet, StringBuilder sb) {
      Sheet<S, P> m = (Sheet<S, P>)(sheet instanceof Slide ? sheet.getMasterSheet() : sheet);
      StringBuilder footer = new StringBuilder("\n");
      this.addSheetPlaceholderDatails(sheet, Placeholder.HEADER, sb);
      this.addSheetPlaceholderDatails(sheet, Placeholder.FOOTER, footer);
      if (this.masterByDefault) {
         for (Shape<S, P> s : m) {
            if (s instanceof TextShape) {
               TextShape<S, P> ts = (TextShape<S, P>)s;
               PlaceholderDetails pd = ts.getPlaceholderDetails();
               if (pd != null && pd.isVisible() && pd.getPlaceholder() != null) {
                  switch (pd.getPlaceholder()) {
                     case HEADER:
                        sb.append(ts.getText());
                        sb.append('\n');
                        break;
                     case SLIDE_NUMBER:
                        if (sheet instanceof Slide) {
                           footer.append(ts.getText().replace("‹#›", Integer.toString(((Slide)sheet).getSlideNumber() + 1)));
                           footer.append('\n');
                        }
                        break;
                     case FOOTER:
                        footer.append(ts.getText());
                        footer.append('\n');
                     case DATETIME:
                  }
               }
            }
         }
      }

      return footer.length() > 1 ? footer.toString() : "";
   }

   private void addSheetPlaceholderDatails(Sheet<S, P> sheet, Placeholder placeholder, StringBuilder sb) {
      PlaceholderDetails headerPD = sheet.getPlaceholderDetails(placeholder);
      if (headerPD != null) {
         String headerStr = headerPD.getText();
         if (headerStr != null) {
            sb.append(headerStr);
         }
      }
   }

   private void printShapeText(Sheet<S, P> sheet, StringBuilder sb) {
      String footer = this.printHeaderReturnFooter(sheet, sb);
      this.printShapeText((ShapeContainer<S, P>)sheet, sb);
      sb.append(footer);
   }

   private void printShapeText(ShapeContainer<S, P> container, StringBuilder sb) {
      for (Shape<S, P> shape : container) {
         if (shape instanceof TextShape) {
            this.printShapeText((TextShape<S, P>)shape, sb);
         } else if (shape instanceof TableShape) {
            this.printShapeText((TableShape<S, P>)shape, sb);
         } else if (shape instanceof ShapeContainer) {
            this.printShapeText((ShapeContainer<S, P>)shape, sb);
         }
      }
   }

   private void printShapeText(TextShape<S, P> shape, StringBuilder sb) {
      List<P> paraList = shape.getTextParagraphs();
      if (paraList.isEmpty()) {
         sb.append('\n');
      } else {
         for (P para : paraList) {
            for (TextRun tr : para) {
               String str = tr.getRawText().replace("\r", "");
               String newStr;
               switch (tr.getTextCap()) {
                  case ALL:
                     newStr = str.toUpperCase(LocaleUtil.getUserLocale());
                     break;
                  case SMALL:
                     newStr = str.toLowerCase(LocaleUtil.getUserLocale());
                     break;
                  case NONE:
                  default:
                     newStr = str;
               }

               sb.append(newStr);
            }

            sb.append('\n');
         }
      }
   }

   private void printShapeText(TableShape<S, P> shape, StringBuilder sb) {
      int nrows = shape.getNumberOfRows();
      int ncols = shape.getNumberOfColumns();

      for (int row = 0; row < nrows; row++) {
         for (int col = 0; col < ncols; col++) {
            TableCell<S, P> cell = shape.getCell(row, col);
            if (cell != null) {
               String txt = cell.getText();
               txt = txt == null ? "" : txt;
               sb.append(txt);
               if (col < ncols - 1) {
                  sb.append('\t');
               }
            }
         }

         sb.append('\n');
      }
   }

   private void printComments(Slide<S, P> slide, StringBuilder sb) {
      for (Comment comment : slide.getComments()) {
         sb.append(comment.getAuthor());
         sb.append(" - ");
         sb.append(comment.getText());
         sb.append("\n");
      }
   }

   private void printNotes(Slide<S, P> slide, StringBuilder sb) {
      Notes<S, P> notes = slide.getNotes();
      if (notes != null) {
         String footer = this.printHeaderReturnFooter(notes, sb);
         this.printShapeText(notes, sb);
         sb.append(footer);
      }
   }

   public List<? extends ObjectShape<S, P>> getOLEShapes() {
      List<ObjectShape<S, P>> oleShapes = new ArrayList();

      for (Slide<S, P> slide : this.slideshow.getSlides()) {
         this.addOLEShapes(oleShapes, slide);
      }

      return oleShapes;
   }

   private void addOLEShapes(List<ObjectShape<S, P>> oleShapes, ShapeContainer<S, P> container) {
      for (Shape<S, P> shape : container) {
         if (shape instanceof ShapeContainer) {
            this.addOLEShapes(oleShapes, (ShapeContainer<S, P>)shape);
         } else if (shape instanceof ObjectShape) {
            oleShapes.add(shape);
         }
      }
   }
}
