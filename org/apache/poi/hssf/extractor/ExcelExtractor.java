package org.apache.poi.hssf.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import org.apache.poi.extractor.POIOLE2TextExtractor;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.HeaderFooter;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;

public class ExcelExtractor extends POIOLE2TextExtractor implements org.apache.poi.ss.extractor.ExcelExtractor {
   private final HSSFWorkbook _wb;
   private final HSSFDataFormatter _formatter;
   private boolean _includeSheetNames = true;
   private boolean _shouldEvaluateFormulas = true;
   private boolean _includeCellComments;
   private boolean _includeBlankCells;
   private boolean _includeHeadersFooters = true;

   public ExcelExtractor(HSSFWorkbook wb) {
      super(wb);
      this._wb = wb;
      this._formatter = new HSSFDataFormatter();
   }

   public ExcelExtractor(POIFSFileSystem fs) throws IOException {
      this(fs.getRoot());
   }

   public ExcelExtractor(DirectoryNode dir) throws IOException {
      this(new HSSFWorkbook(dir, true));
   }

   private static void printUsageMessage(PrintStream ps) {
      ps.println("Use:");
      ps.println("    " + ExcelExtractor.class.getName() + " [<flag> <value> [<flag> <value> [...]]] [-i <filename.xls>]");
      ps.println("       -i <filename.xls> specifies input file (default is to use stdin)");
      ps.println("       Flags can be set on or off by using the values 'Y' or 'N'.");
      ps.println("       Following are available flags and their default values:");
      ps.println("       --show-sheet-names  Y");
      ps.println("       --evaluate-formulas Y");
      ps.println("       --show-comments     N");
      ps.println("       --show-blanks       Y");
      ps.println("       --headers-footers   Y");
   }

   public static void main(String[] args) throws IOException {
      ExcelExtractor.CommandArgs cmdArgs;
      try {
         cmdArgs = new ExcelExtractor.CommandArgs(args);
      } catch (ExcelExtractor.CommandParseException e) {
         System.err.println(e.getMessage());
         printUsageMessage(System.err);
         System.exit(1);
         return;
      }

      if (cmdArgs.isRequestHelp()) {
         printUsageMessage(System.out);
      } else {
         InputStream is;
         if (cmdArgs.getInputFile() == null) {
            is = System.in;
         } else {
            is = new FileInputStream(cmdArgs.getInputFile());
         }

         HSSFWorkbook wb = new HSSFWorkbook(is);
         is.close();
         ExcelExtractor extractor = new ExcelExtractor(wb);
         extractor.setIncludeSheetNames(cmdArgs.shouldShowSheetNames());
         extractor.setFormulasNotResults(!cmdArgs.shouldEvaluateFormulas());
         extractor.setIncludeCellComments(cmdArgs.shouldShowCellComments());
         extractor.setIncludeBlankCells(cmdArgs.shouldShowBlankCells());
         extractor.setIncludeHeadersFooters(cmdArgs.shouldIncludeHeadersFooters());
         System.out.println(extractor.getText());
         extractor.close();
         wb.close();
      }
   }

   public void setIncludeSheetNames(boolean includeSheetNames) {
      this._includeSheetNames = includeSheetNames;
   }

   public void setFormulasNotResults(boolean formulasNotResults) {
      this._shouldEvaluateFormulas = !formulasNotResults;
   }

   public void setIncludeCellComments(boolean includeCellComments) {
      this._includeCellComments = includeCellComments;
   }

   public void setIncludeBlankCells(boolean includeBlankCells) {
      this._includeBlankCells = includeBlankCells;
   }

   public void setIncludeHeadersFooters(boolean includeHeadersFooters) {
      this._includeHeadersFooters = includeHeadersFooters;
   }

   public String getText() {
      StringBuffer text = new StringBuffer();
      this._wb.setMissingCellPolicy(MissingCellPolicy.RETURN_BLANK_AS_NULL);

      for (int i = 0; i < this._wb.getNumberOfSheets(); i++) {
         HSSFSheet sheet = this._wb.getSheetAt(i);
         if (sheet != null) {
            if (this._includeSheetNames) {
               String name = this._wb.getSheetName(i);
               if (name != null) {
                  text.append(name);
                  text.append("\n");
               }
            }

            if (this._includeHeadersFooters) {
               text.append(_extractHeaderFooter(sheet.getHeader()));
            }

            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();

            for (int j = firstRow; j <= lastRow; j++) {
               HSSFRow row = sheet.getRow(j);
               if (row != null) {
                  int firstCell = row.getFirstCellNum();
                  int lastCell = row.getLastCellNum();
                  if (this._includeBlankCells) {
                     firstCell = 0;
                  }

                  for (int k = firstCell; k < lastCell; k++) {
                     HSSFCell cell = row.getCell(k);
                     boolean outputContents = true;
                     if (cell == null) {
                        outputContents = this._includeBlankCells;
                     } else {
                        label62:
                        switch (cell.getCellType()) {
                           case STRING:
                              text.append(cell.getRichStringCellValue().getString());
                              break;
                           case NUMERIC:
                              text.append(this._formatter.formatCellValue(cell));
                              break;
                           case BOOLEAN:
                              text.append(cell.getBooleanCellValue());
                              break;
                           case ERROR:
                              text.append(ErrorEval.getText(cell.getErrorCellValue()));
                              break;
                           case FORMULA:
                              if (!this._shouldEvaluateFormulas) {
                                 text.append(cell.getCellFormula());
                                 break;
                              } else {
                                 switch (cell.getCachedFormulaResultType()) {
                                    case STRING:
                                       HSSFRichTextString str = cell.getRichStringCellValue();
                                       if (str != null && str.length() > 0) {
                                          text.append(str);
                                       }
                                       break label62;
                                    case NUMERIC:
                                       HSSFCellStyle style = cell.getCellStyle();
                                       double nVal = cell.getNumericCellValue();
                                       short df = style.getDataFormat();
                                       String dfs = style.getDataFormatString();
                                       text.append(this._formatter.formatRawCellContents(nVal, df, dfs));
                                       break label62;
                                    case BOOLEAN:
                                       text.append(cell.getBooleanCellValue());
                                       break label62;
                                    case ERROR:
                                       text.append(ErrorEval.getText(cell.getErrorCellValue()));
                                       break label62;
                                    default:
                                       throw new IllegalStateException("Unexpected cell cached formula result type: " + cell.getCachedFormulaResultType());
                                 }
                              }
                           default:
                              throw new RuntimeException("Unexpected cell type (" + cell.getCellType() + ")");
                        }

                        HSSFComment comment = cell.getCellComment();
                        if (this._includeCellComments && comment != null) {
                           String commentText = comment.getString().getString().replace('\n', ' ');
                           text.append(" Comment by " + comment.getAuthor() + ": " + commentText);
                        }
                     }

                     if (outputContents && k < lastCell - 1) {
                        text.append("\t");
                     }
                  }

                  text.append("\n");
               }
            }

            if (this._includeHeadersFooters) {
               text.append(_extractHeaderFooter(sheet.getFooter()));
            }
         }
      }

      return text.toString();
   }

   public static String _extractHeaderFooter(HeaderFooter hf) {
      StringBuffer text = new StringBuffer();
      if (hf.getLeft() != null) {
         text.append(hf.getLeft());
      }

      if (hf.getCenter() != null) {
         if (text.length() > 0) {
            text.append("\t");
         }

         text.append(hf.getCenter());
      }

      if (hf.getRight() != null) {
         if (text.length() > 0) {
            text.append("\t");
         }

         text.append(hf.getRight());
      }

      if (text.length() > 0) {
         text.append("\n");
      }

      return text.toString();
   }

   private static final class CommandArgs {
      private final boolean _requestHelp;
      private final File _inputFile;
      private final boolean _showSheetNames;
      private final boolean _evaluateFormulas;
      private final boolean _showCellComments;
      private final boolean _showBlankCells;
      private final boolean _headersFooters;

      public CommandArgs(String[] args) throws ExcelExtractor.CommandParseException {
         int nArgs = args.length;
         File inputFile = null;
         boolean requestHelp = false;
         boolean showSheetNames = true;
         boolean evaluateFormulas = true;
         boolean showCellComments = false;
         boolean showBlankCells = false;
         boolean headersFooters = true;

         for (int i = 0; i < nArgs; i++) {
            String arg = args[i];
            if ("-help".equalsIgnoreCase(arg)) {
               requestHelp = true;
               break;
            }

            if ("-i".equals(arg)) {
               if (++i >= nArgs) {
                  throw new ExcelExtractor.CommandParseException("Expected filename after '-i'");
               }

               arg = args[i];
               if (inputFile != null) {
                  throw new ExcelExtractor.CommandParseException("Only one input file can be supplied");
               }

               inputFile = new File(arg);
               if (!inputFile.exists()) {
                  throw new ExcelExtractor.CommandParseException("Specified input file '" + arg + "' does not exist");
               }

               if (inputFile.isDirectory()) {
                  throw new ExcelExtractor.CommandParseException("Specified input file '" + arg + "' is a directory");
               }
            } else if ("--show-sheet-names".equals(arg)) {
               showSheetNames = parseBoolArg(args, ++i);
            } else if ("--evaluate-formulas".equals(arg)) {
               evaluateFormulas = parseBoolArg(args, ++i);
            } else if ("--show-comments".equals(arg)) {
               showCellComments = parseBoolArg(args, ++i);
            } else if ("--show-blanks".equals(arg)) {
               showBlankCells = parseBoolArg(args, ++i);
            } else {
               if (!"--headers-footers".equals(arg)) {
                  throw new ExcelExtractor.CommandParseException("Invalid argument '" + arg + "'");
               }

               headersFooters = parseBoolArg(args, ++i);
            }
         }

         this._requestHelp = requestHelp;
         this._inputFile = inputFile;
         this._showSheetNames = showSheetNames;
         this._evaluateFormulas = evaluateFormulas;
         this._showCellComments = showCellComments;
         this._showBlankCells = showBlankCells;
         this._headersFooters = headersFooters;
      }

      private static boolean parseBoolArg(String[] args, int i) throws ExcelExtractor.CommandParseException {
         if (i >= args.length) {
            throw new ExcelExtractor.CommandParseException("Expected value after '" + args[i - 1] + "'");
         } else {
            String value = args[i].toUpperCase(Locale.ROOT);
            if ("Y".equals(value) || "YES".equals(value) || "ON".equals(value) || "TRUE".equals(value)) {
               return true;
            } else if (!"N".equals(value) && !"NO".equals(value) && !"OFF".equals(value) && !"FALSE".equals(value)) {
               throw new ExcelExtractor.CommandParseException("Invalid value '" + args[i] + "' for '" + args[i - 1] + "'. Expected 'Y' or 'N'");
            } else {
               return false;
            }
         }
      }

      public boolean isRequestHelp() {
         return this._requestHelp;
      }

      public File getInputFile() {
         return this._inputFile;
      }

      public boolean shouldShowSheetNames() {
         return this._showSheetNames;
      }

      public boolean shouldEvaluateFormulas() {
         return this._evaluateFormulas;
      }

      public boolean shouldShowCellComments() {
         return this._showCellComments;
      }

      public boolean shouldShowBlankCells() {
         return this._showBlankCells;
      }

      public boolean shouldIncludeHeadersFooters() {
         return this._headersFooters;
      }
   }

   private static final class CommandParseException extends Exception {
      public CommandParseException(String msg) {
         super(msg);
      }
   }
}
