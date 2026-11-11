package com.microsoft.sqlserver.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

final class SQLServerSQLXML implements SQLXML {
   private final SQLServerConnection con;
   private final PLPXMLInputStream contents;
   private final InputStreamGetterArgs getterArgs;
   private final TypeInfo typeInfo;
   private boolean isUsed = false;
   private boolean isFreed = false;
   private static final Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc.internals.SQLServerSQLXML");
   private ByteArrayOutputStreamToInputStream outputStreamValue;
   private Document docValue;
   private String strValue;
   private static int baseID = 0;
   private final String traceID;

   public final String toString() {
      return this.traceID;
   }

   private static synchronized int nextInstanceID() {
      baseID++;
      return baseID;
   }

   InputStream getValue() throws SQLServerException {
      this.checkClosed();
      if (!this.isUsed) {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_noDataXML"), null, true);
      }

      assert null == this.contents;
      InputStream var1 = null;
      if (null != this.outputStreamValue) {
         var1 = this.outputStreamValue.getInputStream();
         assert null == this.docValue;
         assert null == this.strValue;
      } else if (null != this.docValue) {
         assert null == this.outputStreamValue;
         assert null == this.strValue;
         ByteArrayOutputStreamToInputStream var2 = new ByteArrayOutputStreamToInputStream();
         Object var4 = null;

         try {
            TransformerFactory var3 = TransformerFactory.newInstance();
            var3.newTransformer().transform(new DOMSource(this.docValue), new StreamResult(var2));
         } catch (TransformerException var9) {
            MessageFormat var6 = new MessageFormat(SQLServerException.getErrString("R_noParserSupport"));
            Object[] var7 = new Object[]{var9.toString()};
            SQLServerException.makeFromDriverError(this.con, null, var6.format(var7), null, true);
         }

         var1 = var2.getInputStream();
      } else {
         assert null == this.outputStreamValue;
         assert null == this.docValue;
         assert null != this.strValue;

         try {
            var1 = new ByteArrayInputStream(this.strValue.getBytes(Encoding.UNICODE.charsetName()));
         } catch (UnsupportedEncodingException var8) {
            throw new SQLServerException(null, var8.getMessage(), null, 0, true);
         }
      }

      assert null != var1;
      this.isFreed = true;
      return var1;
   }

   SQLServerSQLXML(SQLServerConnection var1) {
      this.contents = null;
      this.traceID = " SQLServerSQLXML:" + nextInstanceID();
      this.con = var1;
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.toString() + " created by (" + var1.toString() + ")");
      }

      this.getterArgs = null;
      this.typeInfo = null;
   }

   SQLServerSQLXML(InputStream var1, InputStreamGetterArgs var2, TypeInfo var3) throws SQLServerException {
      this.traceID = " SQLServerSQLXML:" + nextInstanceID();
      this.contents = (PLPXMLInputStream)var1;
      this.con = null;
      this.getterArgs = var2;
      this.typeInfo = var3;
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.toString() + " created by (null connection)");
      }
   }

   InputStream getStream() {
      return this.contents;
   }

   public void free() throws SQLException {
      if (!this.isFreed) {
         this.isFreed = true;
         if (null != this.contents) {
            try {
               this.contents.close();
            } catch (IOException var2) {
               SQLServerException.makeFromDriverError(null, null, var2.getMessage(), null, true);
            }
         }
      }
   }

   private void checkClosed() throws SQLServerException {
      if (this.isFreed || null != this.con && this.con.isClosed()) {
         MessageFormat var1 = new MessageFormat(SQLServerException.getErrString("R_isFreed"));
         SQLServerException.makeFromDriverError(this.con, null, var1.format(new Object[]{"SQLXML"}), null, true);
      }
   }

   private void checkReadXML() throws SQLException {
      if (null == this.contents) {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_writeOnlyXML"), null, true);
      }

      if (this.isUsed) {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_dataHasBeenReadXML"), null, true);
      }

      try {
         this.contents.checkClosed();
      } catch (IOException var3) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_isFreed"));
         SQLServerException.makeFromDriverError(this.con, null, var2.format(new Object[]{"SQLXML"}), null, true);
      }
   }

   void checkWriteXML() throws SQLException {
      if (null != this.contents) {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_readOnlyXML"), null, true);
      }

      if (this.isUsed) {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_dataHasBeenSetXML"), null, true);
      }
   }

   public InputStream getBinaryStream() throws SQLException {
      this.checkClosed();
      this.checkReadXML();
      this.isUsed = true;
      return this.contents;
   }

   public OutputStream setBinaryStream() throws SQLException {
      this.checkClosed();
      this.checkWriteXML();
      this.isUsed = true;
      this.outputStreamValue = new ByteArrayOutputStreamToInputStream();
      return this.outputStreamValue;
   }

   public Writer setCharacterStream() throws SQLException {
      this.checkClosed();
      this.checkWriteXML();
      this.isUsed = true;
      this.outputStreamValue = new ByteArrayOutputStreamToInputStream();
      Object var1 = null;

      try {
         return new OutputStreamWriter(this.outputStreamValue, Encoding.UNICODE.charsetName());
      } catch (UnsupportedEncodingException var3) {
         throw new SQLServerException(null, var3.getMessage(), null, 0, true);
      }
   }

   public Reader getCharacterStream() throws SQLException {
      this.checkClosed();
      this.checkReadXML();
      this.isUsed = true;
      StreamType var1 = StreamType.CHARACTER;
      InputStreamGetterArgs var2 = new InputStreamGetterArgs(var1, this.getterArgs.isAdaptive, this.getterArgs.isStreaming, this.getterArgs.logContext);
      assert null != this.contents;

      try {
         this.contents.read();
         this.contents.read();
      } catch (IOException var4) {
         SQLServerException.makeFromDriverError(null, null, var4.getMessage(), null, true);
      }

      return (Reader)DDC.convertStreamToObject(this.contents, this.typeInfo, var1.getJDBCType(), var2);
   }

   public String getString() throws SQLException {
      this.checkClosed();
      this.checkReadXML();
      this.isUsed = true;
      assert null != this.contents;

      try {
         this.contents.read();
         this.contents.read();
      } catch (IOException var5) {
         SQLServerException.makeFromDriverError(null, null, var5.getMessage(), null, true);
      }

      byte[] var1 = this.contents.getBytes();
      Object var2 = null;

      try {
         return new String(var1, 0, var1.length, Encoding.UNICODE.charsetName());
      } catch (UnsupportedEncodingException var4) {
         throw new SQLServerException(null, var4.getMessage(), null, 0, true);
      }
   }

   public void setString(String var1) throws SQLException {
      this.checkClosed();
      this.checkWriteXML();
      this.isUsed = true;
      if (null == var1) {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_cantSetNull"), null, true);
      }

      this.strValue = var1;
   }

   public <T extends Source> T getSource(Class<T> var1) throws SQLException {
      this.checkClosed();
      this.checkReadXML();
      return null == var1 ? this.getSourceInternal((Class<T>)StreamSource.class) : this.getSourceInternal(var1);
   }

   <T extends Source> T getSourceInternal(Class<T> var1) throws SQLException {
      this.isUsed = true;
      Source var2 = null;
      if (DOMSource.class == var1) {
         var2 = (Source)var1.cast(this.getDOMSource());
      } else if (SAXSource.class == var1) {
         var2 = (Source)var1.cast(this.getSAXSource());
      } else if (StAXSource.class == var1) {
         var2 = (Source)var1.cast(this.getStAXSource());
      } else if (StreamSource.class == var1) {
         var2 = (Source)var1.cast(new StreamSource(this.contents));
      } else {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_notSupported"), null, true);
      }

      return (T)var2;
   }

   public <T extends Result> T setResult(Class<T> var1) throws SQLException {
      this.checkClosed();
      this.checkWriteXML();
      return null == var1 ? this.setResultInternal((Class<T>)StreamResult.class) : this.setResultInternal(var1);
   }

   <T extends Result> T setResultInternal(Class<T> var1) throws SQLException {
      this.isUsed = true;
      Result var2 = null;
      if (DOMResult.class == var1) {
         var2 = (Result)var1.cast(this.getDOMResult());
      } else if (SAXResult.class == var1) {
         var2 = (Result)var1.cast(this.getSAXResult());
      } else if (StAXResult.class == var1) {
         var2 = (Result)var1.cast(this.getStAXResult());
      } else if (StreamResult.class == var1) {
         this.outputStreamValue = new ByteArrayOutputStreamToInputStream();
         var2 = (Result)var1.cast(new StreamResult(this.outputStreamValue));
      } else {
         SQLServerException.makeFromDriverError(this.con, null, SQLServerException.getErrString("R_notSupported"), null, true);
      }

      return (T)var2;
   }

   private final DOMSource getDOMSource() throws SQLException {
      Document var1 = null;
      DocumentBuilderFactory var2 = DocumentBuilderFactory.newInstance();

      try {
         var2.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
         DocumentBuilder var3 = var2.newDocumentBuilder();
         var3.setEntityResolver(new SQLServerEntityResolver());

         try {
            var1 = var3.parse(this.contents);
         } catch (IOException var7) {
            MessageFormat var11 = new MessageFormat(SQLServerException.getErrString("R_errorReadingStream"));
            Object[] var13 = new Object[]{var7.toString()};
            SQLServerException.makeFromDriverError(null, null, var11.format(var13), "", true);
         }

         return new DOMSource(var1);
      } catch (ParserConfigurationException var8) {
         MessageFormat var10 = new MessageFormat(SQLServerException.getErrString("R_noParserSupport"));
         Object[] var12 = new Object[]{var8.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var10.format(var12), null, true);
      } catch (SAXException var9) {
         MessageFormat var5 = new MessageFormat(SQLServerException.getErrString("R_failedToParseXML"));
         Object[] var6 = new Object[]{var9.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var5.format(var6), null, true);
      }

      return null;
   }

   private final SAXSource getSAXSource() throws SQLException {
      try {
         InputSource var1 = new InputSource(this.contents);
         XMLReader var5 = XMLReaderFactory.createXMLReader();
         return new SAXSource(var5, var1);
      } catch (SAXException var4) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_failedToParseXML"));
         Object[] var3 = new Object[]{var4.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var2.format(var3), null, true);
         return null;
      }
   }

   private final StAXSource getStAXSource() throws SQLException {
      XMLInputFactory var1 = XMLInputFactory.newInstance();

      try {
         XMLStreamReader var2 = var1.createXMLStreamReader(this.contents);
         return new StAXSource(var2);
      } catch (XMLStreamException var5) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_noParserSupport"));
         Object[] var4 = new Object[]{var5.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var3.format(var4), null, true);
         return null;
      }
   }

   private final StAXResult getStAXResult() throws SQLException {
      XMLOutputFactory var1 = XMLOutputFactory.newInstance();
      this.outputStreamValue = new ByteArrayOutputStreamToInputStream();

      try {
         XMLStreamWriter var2 = var1.createXMLStreamWriter(this.outputStreamValue);
         return new StAXResult(var2);
      } catch (XMLStreamException var5) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_noParserSupport"));
         Object[] var4 = new Object[]{var5.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var3.format(var4), null, true);
         return null;
      }
   }

   private final SAXResult getSAXResult() throws SQLException {
      TransformerHandler var1 = null;

      try {
         SAXTransformerFactory var2 = (SAXTransformerFactory)TransformerFactory.newInstance();
         var1 = var2.newTransformerHandler();
      } catch (TransformerConfigurationException var5) {
         MessageFormat var7 = new MessageFormat(SQLServerException.getErrString("R_noParserSupport"));
         Object[] var8 = new Object[]{var5.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var7.format(var8), null, true);
      } catch (ClassCastException var6) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_noParserSupport"));
         Object[] var4 = new Object[]{var6.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var3.format(var4), null, true);
      }

      this.outputStreamValue = new ByteArrayOutputStreamToInputStream();
      var1.setResult(new StreamResult(this.outputStreamValue));
      return new SAXResult(var1);
   }

   private final DOMResult getDOMResult() throws SQLException {
      DocumentBuilderFactory var1 = DocumentBuilderFactory.newInstance();
      assert null == this.outputStreamValue;

      try {
         DocumentBuilder var2 = var1.newDocumentBuilder();
         this.docValue = var2.newDocument();
         return new DOMResult(this.docValue);
      } catch (ParserConfigurationException var6) {
         MessageFormat var4 = new MessageFormat(SQLServerException.getErrString("R_noParserSupport"));
         Object[] var5 = new Object[]{var6.toString()};
         SQLServerException.makeFromDriverError(this.con, null, var4.format(var5), null, true);
         return null;
      }
   }
}
