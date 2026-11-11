package org.apache.poi.hssf.usermodel;

public abstract class HeaderFooter implements org.apache.poi.ss.usermodel.HeaderFooter {
   protected HeaderFooter() {
   }

   protected abstract String getRawText();

   private String[] splitParts() {
      String text = this.getRawText();
      String _left = "";
      String _center = "";
      String _right = "";

      while (text.length() > 1) {
         if (text.charAt(0) != '&') {
            _center = text;
            break;
         }

         int pos = text.length();
         switch (text.charAt(1)) {
            case 'C':
               if (text.contains("&L")) {
                  pos = Math.min(pos, text.indexOf("&L"));
               }

               if (text.contains("&R")) {
                  pos = Math.min(pos, text.indexOf("&R"));
               }

               _center = text.substring(2, pos);
               text = text.substring(pos);
               break;
            case 'L':
               if (text.contains("&C")) {
                  pos = Math.min(pos, text.indexOf("&C"));
               }

               if (text.contains("&R")) {
                  pos = Math.min(pos, text.indexOf("&R"));
               }

               _left = text.substring(2, pos);
               text = text.substring(pos);
               break;
            case 'R':
               if (text.contains("&C")) {
                  pos = Math.min(pos, text.indexOf("&C"));
               }

               if (text.contains("&L")) {
                  pos = Math.min(pos, text.indexOf("&L"));
               }

               _right = text.substring(2, pos);
               text = text.substring(pos);
               break;
            default:
               _center = text;
               return new String[]{_left, _center, _right};
         }
      }

      return new String[]{_left, _center, _right};
   }

   public final String getLeft() {
      return this.splitParts()[0];
   }

   public final void setLeft(String newLeft) {
      this.updatePart(0, newLeft);
   }

   public final String getCenter() {
      return this.splitParts()[1];
   }

   public final void setCenter(String newCenter) {
      this.updatePart(1, newCenter);
   }

   public final String getRight() {
      return this.splitParts()[2];
   }

   public final void setRight(String newRight) {
      this.updatePart(2, newRight);
   }

   private void updatePart(int partIndex, String newValue) {
      String[] parts = this.splitParts();
      parts[partIndex] = newValue == null ? "" : newValue;
      this.updateHeaderFooterText(parts);
   }

   private void updateHeaderFooterText(String[] parts) {
      String _left = parts[0];
      String _center = parts[1];
      String _right = parts[2];
      if (_center.length() < 1 && _left.length() < 1 && _right.length() < 1) {
         this.setHeaderFooterText("");
      } else {
         StringBuilder sb = new StringBuilder(64);
         sb.append("&C");
         sb.append(_center);
         sb.append("&L");
         sb.append(_left);
         sb.append("&R");
         sb.append(_right);
         String text = sb.toString();
         this.setHeaderFooterText(text);
      }
   }

   protected abstract void setHeaderFooterText(String var1);

   public static String fontSize(short size) {
      return "&" + size;
   }

   public static String font(String font, String style) {
      return "&\"" + font + "," + style + "\"";
   }

   public static String page() {
      return HeaderFooter.MarkupTag.PAGE_FIELD.getRepresentation();
   }

   public static String numPages() {
      return HeaderFooter.MarkupTag.NUM_PAGES_FIELD.getRepresentation();
   }

   public static String date() {
      return HeaderFooter.MarkupTag.DATE_FIELD.getRepresentation();
   }

   public static String time() {
      return HeaderFooter.MarkupTag.TIME_FIELD.getRepresentation();
   }

   public static String file() {
      return HeaderFooter.MarkupTag.FILE_FIELD.getRepresentation();
   }

   public static String tab() {
      return HeaderFooter.MarkupTag.SHEET_NAME_FIELD.getRepresentation();
   }

   public static String startBold() {
      return HeaderFooter.MarkupTag.BOLD_FIELD.getRepresentation();
   }

   public static String endBold() {
      return HeaderFooter.MarkupTag.BOLD_FIELD.getRepresentation();
   }

   public static String startUnderline() {
      return HeaderFooter.MarkupTag.UNDERLINE_FIELD.getRepresentation();
   }

   public static String endUnderline() {
      return HeaderFooter.MarkupTag.UNDERLINE_FIELD.getRepresentation();
   }

   public static String startDoubleUnderline() {
      return HeaderFooter.MarkupTag.DOUBLE_UNDERLINE_FIELD.getRepresentation();
   }

   public static String endDoubleUnderline() {
      return HeaderFooter.MarkupTag.DOUBLE_UNDERLINE_FIELD.getRepresentation();
   }

   public static String stripFields(String pText) {
      if (pText != null && pText.length() != 0) {
         String text = pText;

         for (HeaderFooter.MarkupTag mt : HeaderFooter.MarkupTag.values()) {
            String seq = mt.getRepresentation();

            int pos;
            while ((pos = text.indexOf(seq)) >= 0) {
               text = text.substring(0, pos) + text.substring(pos + seq.length());
            }
         }

         text = text.replaceAll("\\&\\d+", "");
         return text.replaceAll("\\&\".*?,.*?\"", "");
      } else {
         return pText;
      }
   }

   private enum MarkupTag {
      SHEET_NAME_FIELD("&A", false),
      DATE_FIELD("&D", false),
      FILE_FIELD("&F", false),
      FULL_FILE_FIELD("&Z", false),
      PAGE_FIELD("&P", false),
      TIME_FIELD("&T", false),
      NUM_PAGES_FIELD("&N", false),
      PICTURE_FIELD("&G", false),
      BOLD_FIELD("&B", true),
      ITALIC_FIELD("&I", true),
      STRIKETHROUGH_FIELD("&S", true),
      SUBSCRIPT_FIELD("&Y", true),
      SUPERSCRIPT_FIELD("&X", true),
      UNDERLINE_FIELD("&U", true),
      DOUBLE_UNDERLINE_FIELD("&E", true);

      private final String _representation;
      private final boolean _occursInPairs;

      MarkupTag(String sequence, boolean occursInPairs) {
         this._representation = sequence;
         this._occursInPairs = occursInPairs;
      }

      public String getRepresentation() {
         return this._representation;
      }

      public boolean occursPairs() {
         return this._occursInPairs;
      }
   }
}
