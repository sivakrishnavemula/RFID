package org.apache.poi.sl.draw;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.InvalidObjectException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.common.usermodel.fonts.FontGroup;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.common.usermodel.fonts.FontGroup.FontGroupRange;
import org.apache.poi.sl.usermodel.AutoNumberingScheme;
import org.apache.poi.sl.usermodel.Hyperlink;
import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.sl.usermodel.TextParagraph.BulletStyle;
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign;
import org.apache.poi.sl.usermodel.TextRun.FieldType;
import org.apache.poi.sl.usermodel.TextShape.TextDirection;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;

public class DrawTextParagraph implements Drawable {
   private static final POILogger LOG = POILogFactory.getLogger(DrawTextParagraph.class);
   public static final DrawTextParagraph.XlinkAttribute HYPERLINK_HREF = new DrawTextParagraph.XlinkAttribute("href");
   public static final DrawTextParagraph.XlinkAttribute HYPERLINK_LABEL = new DrawTextParagraph.XlinkAttribute("label");
   protected TextParagraph<?, ?, ?> paragraph;
   double x;
   double y;
   protected List<DrawTextFragment> lines = new ArrayList();
   protected String rawText;
   protected DrawTextFragment bullet;
   protected int autoNbrIdx;
   protected double maxLineHeight;

   public DrawTextParagraph(TextParagraph<?, ?, ?> paragraph) {
      this.paragraph = paragraph;
   }

   public void setPosition(double x, double y) {
      this.x = x;
      this.y = y;
   }

   public double getY() {
      return this.y;
   }

   public void setAutoNumberingIdx(int index) {
      this.autoNbrIdx = index;
   }

   public void draw(Graphics2D graphics) {
      if (!this.lines.isEmpty()) {
         double penY = this.y;
         boolean firstLine = true;
         int indentLevel = this.paragraph.getIndentLevel();
         Double leftMargin = this.paragraph.getLeftMargin();
         if (leftMargin == null) {
            leftMargin = Units.toPoints(347663L * indentLevel);
         }

         Double indent = this.paragraph.getIndent();
         if (indent == null) {
            indent = Units.toPoints(347663L * indentLevel);
         }

         if (this.isHSLF()) {
            indent = indent - leftMargin;
         }

         Double spacing = this.paragraph.getLineSpacing();
         if (spacing == null) {
            spacing = 100.0;
         }

         for (DrawTextFragment line : this.lines) {
            double penX;
            if (firstLine) {
               if (!this.isEmptyParagraph()) {
                  this.bullet = this.getBullet(graphics, line.getAttributedString().getIterator());
               }

               if (this.bullet != null) {
                  this.bullet.setPosition(this.x + leftMargin + indent, penY);
                  this.bullet.draw(graphics);
                  double bulletWidth = this.bullet.getLayout().getAdvance() + 1.0F;
                  penX = this.x + Math.max(leftMargin, leftMargin + indent + bulletWidth);
               } else {
                  penX = this.x + leftMargin;
               }
            } else {
               penX = this.x + leftMargin;
            }

            Rectangle2D anchor = DrawShape.getAnchor(graphics, this.paragraph.getParentShape());
            Insets2D insets = this.paragraph.getParentShape().getInsets();
            double leftInset = insets.left;
            double rightInset = insets.right;
            TextAlign ta = this.paragraph.getTextAlign();
            if (ta == null) {
               ta = TextAlign.LEFT;
            }

            switch (ta) {
               case CENTER:
                  penX += (anchor.getWidth() - line.getWidth() - leftInset - rightInset - leftMargin) / 2.0;
                  break;
               case RIGHT:
                  penX += anchor.getWidth() - line.getWidth() - leftInset - rightInset;
            }

            line.setPosition(penX, penY);
            line.draw(graphics);
            if (spacing > 0.0) {
               penY += spacing * 0.01 * line.getHeight();
            } else {
               penY += -spacing;
            }

            firstLine = false;
         }

         this.y = penY - this.y;
      }
   }

   public float getFirstLineLeading() {
      return this.lines.isEmpty() ? 0.0F : ((DrawTextFragment)this.lines.get(0)).getLeading();
   }

   public float getFirstLineHeight() {
      return this.lines.isEmpty() ? 0.0F : ((DrawTextFragment)this.lines.get(0)).getHeight();
   }

   public float getLastLineHeight() {
      return this.lines.isEmpty() ? 0.0F : ((DrawTextFragment)this.lines.get(this.lines.size() - 1)).getHeight();
   }

   public boolean isEmptyParagraph() {
      return this.lines.isEmpty() || this.rawText.trim().isEmpty();
   }

   public void applyTransform(Graphics2D graphics) {
   }

   public void drawContent(Graphics2D graphics) {
   }

   protected void breakText(Graphics2D graphics) {
      this.lines.clear();
      DrawFactory fact = DrawFactory.getInstance(graphics);
      fact.fixFonts(graphics);
      StringBuilder text = new StringBuilder();
      AttributedString at = this.getAttributedString(graphics, text);
      boolean emptyParagraph = text.toString().trim().isEmpty();
      AttributedCharacterIterator it = at.getIterator();
      LineBreakMeasurer measurer = new LineBreakMeasurer(it, graphics.getFontRenderContext());

      int endIndex;
      do {
         int startIndex = measurer.getPosition();
         double wrappingWidth = this.getWrappingWidth(this.lines.isEmpty(), graphics) + 1.0;
         if (wrappingWidth < 0.0) {
            wrappingWidth = 1.0;
         }

         int nextBreak = text.indexOf("\n", startIndex + 1);
         if (nextBreak == -1) {
            nextBreak = it.getEndIndex();
         }

         TextLayout layout = measurer.nextLayout((float)wrappingWidth, nextBreak, true);
         if (layout == null) {
            layout = measurer.nextLayout((float)wrappingWidth, nextBreak, false);
         }

         if (layout == null) {
            break;
         }

         endIndex = measurer.getPosition();
         if (endIndex < it.getEndIndex() && text.charAt(endIndex) == '\n') {
            measurer.setPosition(endIndex + 1);
         }

         TextAlign hAlign = this.paragraph.getTextAlign();
         if (hAlign == TextAlign.JUSTIFY || hAlign == TextAlign.JUSTIFY_LOW) {
            layout = layout.getJustifiedLayout((float)wrappingWidth);
         }

         AttributedString str = emptyParagraph ? null : new AttributedString(it, startIndex, endIndex);
         DrawTextFragment line = fact.getTextFragment(layout, str);
         this.lines.add(line);
         this.maxLineHeight = Math.max(this.maxLineHeight, line.getHeight());
      } while (endIndex != it.getEndIndex());

      this.rawText = text.toString();
   }

   protected DrawTextFragment getBullet(Graphics2D graphics, AttributedCharacterIterator firstLineAttr) {
      BulletStyle bulletStyle = this.paragraph.getBulletStyle();
      if (bulletStyle == null) {
         return null;
      }

      AutoNumberingScheme ans = bulletStyle.getAutoNumberingScheme();
      String buCharacter;
      if (ans != null) {
         buCharacter = ans.format(this.autoNbrIdx);
      } else {
         buCharacter = bulletStyle.getBulletCharacter();
      }

      if (buCharacter == null) {
         return null;
      }

      PlaceableShape<?, ?> ps = this.getParagraphShape();
      PaintStyle fgPaintStyle = bulletStyle.getBulletFontColor();
      Paint fgPaint;
      if (fgPaintStyle == null) {
         fgPaint = (Paint)firstLineAttr.getAttribute(TextAttribute.FOREGROUND);
      } else {
         fgPaint = new DrawPaint(ps).getPaint(graphics, fgPaintStyle);
      }

      float fontSize = (Float)firstLineAttr.getAttribute(TextAttribute.SIZE);
      Double buSz = bulletStyle.getBulletFontSize();
      if (buSz == null) {
         buSz = 100.0;
      }

      if (buSz > 0.0) {
         fontSize = (float)(fontSize * (buSz * 0.01));
      } else {
         fontSize = (float)(-buSz);
      }

      String buFontStr = bulletStyle.getBulletFont();
      if (buFontStr == null) {
         buFontStr = this.paragraph.getDefaultFontFamily();
      }

      assert buFontStr != null;
      FontInfo buFont = new DrawFontInfo(buFontStr);
      DrawFontManager dfm = DrawFactory.getInstance(graphics).getFontManager(graphics);
      buFont = dfm.getMappedFont(graphics, buFont);
      AttributedString str = new AttributedString(dfm.mapFontCharset(graphics, buFont, buCharacter));
      str.addAttribute(TextAttribute.FOREGROUND, fgPaint);
      str.addAttribute(TextAttribute.FAMILY, buFont.getTypeface());
      str.addAttribute(TextAttribute.SIZE, fontSize);
      TextLayout layout = new TextLayout(str.getIterator(), graphics.getFontRenderContext());
      DrawFactory fact = DrawFactory.getInstance(graphics);
      return fact.getTextFragment(layout, str);
   }

   protected String getRenderableText(Graphics2D graphics, TextRun tr) {
      if (tr.getFieldType() == FieldType.SLIDE_NUMBER) {
         Slide<?, ?> slide = (Slide<?, ?>)graphics.getRenderingHint(Drawable.CURRENT_SLIDE);
         return slide == null ? "" : Integer.toString(slide.getSlideNumber());
      } else {
         return this.getRenderableText(tr);
      }
   }

   @Internal
   public String getRenderableText(TextRun tr) {
      String txtSpace = tr.getRawText().replace("\t", this.tab2space(tr)).replace('\u000b', '\n');
      Locale loc = LocaleUtil.getUserLocale();
      switch (tr.getTextCap()) {
         case ALL:
            return txtSpace.toUpperCase(loc);
         case SMALL:
            return txtSpace.toLowerCase(loc);
         default:
            return txtSpace;
      }
   }

   private String tab2space(TextRun tr) {
      AttributedString string = new AttributedString(" ");
      String fontFamily = tr.getFontFamily();
      if (fontFamily == null) {
         fontFamily = "Lucida Sans";
      }

      string.addAttribute(TextAttribute.FAMILY, fontFamily);
      Double fs = tr.getFontSize();
      if (fs == null) {
         fs = 12.0;
      }

      string.addAttribute(TextAttribute.SIZE, fs.floatValue());
      TextLayout l = new TextLayout(string.getIterator(), new FontRenderContext(null, true, true));
      double wspace = l.getAdvance();
      Double tabSz = this.paragraph.getDefaultTabSize();
      if (tabSz == null) {
         tabSz = wspace * 4.0;
      }

      int numSpaces = (int)Math.ceil(tabSz / wspace);
      StringBuilder buf = new StringBuilder();

      for (int i = 0; i < numSpaces; i++) {
         buf.append(' ');
      }

      return buf.toString();
   }

   protected double getWrappingWidth(boolean firstLine, Graphics2D graphics) {
      TextShape<?, ?> ts = this.paragraph.getParentShape();
      Insets2D insets = ts.getInsets();
      double leftInset = insets.left;
      double rightInset = insets.right;
      int indentLevel = this.paragraph.getIndentLevel();
      if (indentLevel == -1) {
         indentLevel = 0;
      }

      Double leftMargin = this.paragraph.getLeftMargin();
      if (leftMargin == null) {
         leftMargin = Units.toPoints(347663L * (indentLevel + 1));
      }

      Double indent = this.paragraph.getIndent();
      if (indent == null) {
         indent = Units.toPoints(347663L * indentLevel);
      }

      Double rightMargin = this.paragraph.getRightMargin();
      if (rightMargin == null) {
         rightMargin = 0.0;
      }

      Rectangle2D anchor = DrawShape.getAnchor(graphics, ts);
      TextDirection textDir = ts.getTextDirection();
      double width;
      if (!ts.getWordWrap()) {
         Dimension pageDim = ts.getSheet().getSlideShow().getPageSize();
         switch (textDir) {
            case VERTICAL:
               width = pageDim.getHeight() - anchor.getX();
               break;
            case VERTICAL_270:
               width = anchor.getX();
               break;
            default:
               width = pageDim.getWidth() - anchor.getX();
         }
      } else {
         switch (textDir) {
            case VERTICAL:
            case VERTICAL_270:
               width = anchor.getHeight() - leftInset - rightInset - leftMargin - rightMargin;
               break;
            default:
               width = anchor.getWidth() - leftInset - rightInset - leftMargin - rightMargin;
         }

         if (firstLine && !this.isHSLF()) {
            if (this.bullet != null) {
               if (indent > 0.0) {
                  width -= indent;
               }
            } else if (indent > 0.0) {
               width -= indent;
            } else if (indent < 0.0) {
               width += leftMargin;
            }
         }
      }

      return width;
   }

   private PlaceableShape<?, ?> getParagraphShape() {
      return new PlaceableShape() {
         public ShapeContainer<?, ?> getParent() {
            return null;
         }

         public Rectangle2D getAnchor() {
            return DrawTextParagraph.this.paragraph.getParentShape().getAnchor();
         }

         public void setAnchor(Rectangle2D anchor) {
         }

         public double getRotation() {
            return 0.0;
         }

         public void setRotation(double theta) {
         }

         public void setFlipHorizontal(boolean flip) {
         }

         public void setFlipVertical(boolean flip) {
         }

         public boolean getFlipHorizontal() {
            return false;
         }

         public boolean getFlipVertical() {
            return false;
         }

         public Sheet<?, ?> getSheet() {
            return DrawTextParagraph.this.paragraph.getParentShape().getSheet();
         }
      };
   }

   protected AttributedString getAttributedString(Graphics2D graphics, StringBuilder text) {
      List<DrawTextParagraph.AttributedStringData> attList = new ArrayList();
      if (text == null) {
         text = new StringBuilder();
      }

      PlaceableShape<?, ?> ps = this.getParagraphShape();
      DrawFontManager dfm = DrawFactory.getInstance(graphics).getFontManager(graphics);
      assert dfm != null;

      for (TextRun run : this.paragraph) {
         String runText = this.getRenderableText(graphics, run);
         if (!runText.isEmpty()) {
            runText = dfm.mapFontCharset(graphics, run.getFontInfo(null), runText);
            int beginIndex = text.length();
            text.append(runText);
            int endIndex = text.length();
            PaintStyle fgPaintStyle = run.getFontColor();
            Paint fgPaint = new DrawPaint(ps).getPaint(graphics, fgPaintStyle);
            attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.FOREGROUND, fgPaint, beginIndex, endIndex));
            Double fontSz = run.getFontSize();
            if (fontSz == null) {
               fontSz = this.paragraph.getDefaultFontSize();
            }

            attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.SIZE, fontSz.floatValue(), beginIndex, endIndex));
            if (run.isBold()) {
               attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, beginIndex, endIndex));
            }

            if (run.isItalic()) {
               attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, beginIndex, endIndex));
            }

            if (run.isUnderlined()) {
               attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, beginIndex, endIndex));
               attList.add(
                  new DrawTextParagraph.AttributedStringData(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_TWO_PIXEL, beginIndex, endIndex)
               );
            }

            if (run.isStrikethrough()) {
               attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, beginIndex, endIndex));
            }

            if (run.isSubscript()) {
               attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, beginIndex, endIndex));
            }

            if (run.isSuperscript()) {
               attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER, beginIndex, endIndex));
            }

            Hyperlink<?, ?> hl = run.getHyperlink();
            if (hl != null) {
               attList.add(new DrawTextParagraph.AttributedStringData(HYPERLINK_HREF, hl.getAddress(), beginIndex, endIndex));
               attList.add(new DrawTextParagraph.AttributedStringData(HYPERLINK_LABEL, hl.getLabel(), beginIndex, endIndex));
            }

            this.processGlyphs(graphics, dfm, attList, beginIndex, run, runText);
         }
      }

      if (text.length() == 0) {
         Double fontSz = this.paragraph.getDefaultFontSize();
         text.append(" ");
         attList.add(new DrawTextParagraph.AttributedStringData(TextAttribute.SIZE, fontSz.floatValue(), 0, 1));
      }

      AttributedString string = new AttributedString(text.toString());

      for (DrawTextParagraph.AttributedStringData asd : attList) {
         string.addAttribute(asd.attribute, asd.value, asd.beginIndex, asd.endIndex);
      }

      return string;
   }

   private void processGlyphs(
      Graphics2D graphics, DrawFontManager dfm, List<DrawTextParagraph.AttributedStringData> attList, int beginIndex, TextRun run, String runText
   ) {
      List<FontGroupRange> ttrList = FontGroup.getFontGroupRanges(runText);
      int rangeBegin = 0;

      for (FontGroupRange ttr : ttrList) {
         FontInfo fiRun = run.getFontInfo(ttr.getFontGroup());
         if (fiRun == null) {
            fiRun = run.getFontInfo(FontGroup.LATIN);
         }

         FontInfo fiMapped = dfm.getMappedFont(graphics, fiRun);
         FontInfo fiFallback = dfm.getFallbackFont(graphics, fiRun);
         assert fiFallback != null;
         if (fiMapped == null) {
            fiMapped = dfm.getMappedFont(graphics, new DrawFontInfo(this.paragraph.getDefaultFontFamily()));
         }

         if (fiMapped == null) {
            fiMapped = fiFallback;
         }

         Font fontMapped = dfm.createAWTFont(graphics, fiMapped, 10.0, run.isBold(), run.isItalic());
         Font fontFallback = dfm.createAWTFont(graphics, fiFallback, 10.0, run.isBold(), run.isItalic());
         int rangeLen = ttr.getLength();
         int partEnd = rangeBegin;

         while (partEnd < rangeBegin + rangeLen) {
            int partBegin = partEnd;
            partEnd = nextPart(fontMapped, runText, partBegin, rangeBegin + rangeLen, true);
            if (partBegin < partEnd) {
               attList.add(
                  new DrawTextParagraph.AttributedStringData(
                     TextAttribute.FAMILY, fontMapped.getFontName(Locale.ROOT), beginIndex + partBegin, beginIndex + partEnd
                  )
               );
               if (LOG.check(1)) {
                  LOG.log(
                     1,
                     new Object[]{
                        "mapped: ",
                        fontMapped.getFontName(Locale.ROOT),
                        " ",
                        beginIndex + partBegin,
                        " ",
                        beginIndex + partEnd,
                        " - ",
                        runText.substring(beginIndex + partBegin, beginIndex + partEnd)
                     }
                  );
               }
            }

            partBegin = partEnd;
            partEnd = nextPart(fontMapped, runText, partBegin, rangeBegin + rangeLen, false);
            if (partBegin < partEnd) {
               attList.add(
                  new DrawTextParagraph.AttributedStringData(
                     TextAttribute.FAMILY, fontFallback.getFontName(Locale.ROOT), beginIndex + partBegin, beginIndex + partEnd
                  )
               );
               if (LOG.check(1)) {
                  LOG.log(
                     1,
                     new Object[]{
                        "fallback: ",
                        fontFallback.getFontName(Locale.ROOT),
                        " ",
                        beginIndex + partBegin,
                        " ",
                        beginIndex + partEnd,
                        " - ",
                        runText.substring(beginIndex + partBegin, beginIndex + partEnd)
                     }
                  );
               }
            }
         }

         rangeBegin += rangeLen;
      }
   }

   private static int nextPart(Font fontMapped, String runText, int beginPart, int endPart, boolean isDisplayed) {
      int rIdx = beginPart;

      while (rIdx < endPart) {
         int codepoint = runText.codePointAt(rIdx);
         if (fontMapped.canDisplay(codepoint) != isDisplayed) {
            break;
         }

         rIdx += Character.charCount(codepoint);
      }

      return rIdx;
   }

   protected boolean isHSLF() {
      return DrawShape.isHSLF(this.paragraph.getParentShape());
   }

   private static class AttributedStringData {
      Attribute attribute;
      Object value;
      int beginIndex;
      int endIndex;

      AttributedStringData(Attribute attribute, Object value, int beginIndex, int endIndex) {
         this.attribute = attribute;
         this.value = value;
         this.beginIndex = beginIndex;
         this.endIndex = endIndex;
      }
   }

   private static class XlinkAttribute extends Attribute {
      XlinkAttribute(String name) {
         super(name);
      }

      protected Object readResolve() throws InvalidObjectException {
         if (DrawTextParagraph.HYPERLINK_HREF.getName().equals(this.getName())) {
            return DrawTextParagraph.HYPERLINK_HREF;
         } else if (DrawTextParagraph.HYPERLINK_LABEL.getName().equals(this.getName())) {
            return DrawTextParagraph.HYPERLINK_LABEL;
         } else {
            throw new InvalidObjectException("unknown attribute name");
         }
      }
   }
}
