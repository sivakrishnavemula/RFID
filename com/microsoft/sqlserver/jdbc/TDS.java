package com.microsoft.sqlserver.jdbc;

import java.sql.Timestamp;

final class TDS {
   static final int VER_DENALI = 1946157060;
   static final int VER_KATMAI = 1930100739;
   static final int VER_YUKON = 1913192450;
   static final int VER_UNKNOWN = 0;
   static final int TDS_RET_STAT = 121;
   static final int TDS_COLMETADATA = 129;
   static final int TDS_TABNAME = 164;
   static final int TDS_COLINFO = 165;
   static final int TDS_ORDER = 169;
   static final int TDS_ERR = 170;
   static final int TDS_MSG = 171;
   static final int TDS_RETURN_VALUE = 172;
   static final int TDS_LOGIN_ACK = 173;
   static final int TDS_FEATURE_EXTENSION_ACK = 174;
   static final int TDS_ROW = 209;
   static final int TDS_NBCROW = 210;
   static final int TDS_ENV_CHG = 227;
   static final int TDS_SSPI = 237;
   static final int TDS_DONE = 253;
   static final int TDS_DONEPROC = 254;
   static final int TDS_DONEINPROC = 255;
   static final int TDS_FEDAUTHINFO = 238;
   static final int TDS_FEATURE_EXT_FEDAUTH = 2;
   static final int TDS_FEDAUTH_LIBRARY_SECURITYTOKEN = 1;
   static final int TDS_FEDAUTH_LIBRARY_ADAL = 2;
   static final int TDS_FEDAUTH_LIBRARY_RESERVED = 127;
   static final byte ADALWORKFLOW_ACTIVEDIRECTORYPASSWORD = 1;
   static final byte ADALWORKFLOW_ACTIVEDIRECTORYINTEGRATED = 2;
   static final byte FEDAUTH_INFO_ID_STSURL = 1;
   static final byte FEDAUTH_INFO_ID_SPN = 2;
   static final int TDS_FEATURE_EXT_AE = 4;
   static final int MAX_SUPPORTED_TCE_VERSION = 1;
   static final int CUSTOM_CIPHER_ALGORITHM_ID = 0;
   static final int AES_256_CBC = 1;
   static final int AEAD_AES_256_CBC_HMAC_SHA256 = 2;
   static final int AE_METADATA = 8;
   static final int TDS_TVP = 243;
   static final int TVP_ROW = 1;
   static final int TVP_NULL_TOKEN = 65535;
   static final int TVP_STATUS_DEFAULT = 2;
   static final int TVP_ORDER_UNIQUE_TOKEN = 16;
   static final byte TVP_ORDERASC_FLAG = 1;
   static final byte TVP_ORDERDESC_FLAG = 2;
   static final byte TVP_UNIQUE_FLAG = 4;
   static final int FLAG_NULLABLE = 1;
   static final int FLAG_TVP_DEFAULT_COLUMN = 512;
   static final int FEATURE_EXT_TERMINATOR = -1;
   static final short PROCID_SP_CURSOR = 1;
   static final short PROCID_SP_CURSOROPEN = 2;
   static final short PROCID_SP_CURSORPREPARE = 3;
   static final short PROCID_SP_CURSOREXECUTE = 4;
   static final short PROCID_SP_CURSORPREPEXEC = 5;
   static final short PROCID_SP_CURSORUNPREPARE = 6;
   static final short PROCID_SP_CURSORFETCH = 7;
   static final short PROCID_SP_CURSOROPTION = 8;
   static final short PROCID_SP_CURSORCLOSE = 9;
   static final short PROCID_SP_EXECUTESQL = 10;
   static final short PROCID_SP_PREPARE = 11;
   static final short PROCID_SP_EXECUTE = 12;
   static final short PROCID_SP_PREPEXEC = 13;
   static final short PROCID_SP_PREPEXECRPC = 14;
   static final short PROCID_SP_UNPREPARE = 15;
   static final short SP_CURSOR_OP_UPDATE = 1;
   static final short SP_CURSOR_OP_DELETE = 2;
   static final short SP_CURSOR_OP_INSERT = 4;
   static final short SP_CURSOR_OP_REFRESH = 8;
   static final short SP_CURSOR_OP_LOCK = 16;
   static final short SP_CURSOR_OP_SETPOSITION = 32;
   static final short SP_CURSOR_OP_ABSOLUTE = 64;
   static final int FETCH_FIRST = 1;
   static final int FETCH_NEXT = 2;
   static final int FETCH_PREV = 4;
   static final int FETCH_LAST = 8;
   static final int FETCH_ABSOLUTE = 16;
   static final int FETCH_RELATIVE = 32;
   static final int FETCH_REFRESH = 128;
   static final int FETCH_INFO = 256;
   static final int FETCH_PREV_NOADJUST = 512;
   static final byte RPC_OPTION_NO_METADATA = 2;
   static final short TM_GET_DTC_ADDRESS = 0;
   static final short TM_PROPAGATE_XACT = 1;
   static final short TM_BEGIN_XACT = 5;
   static final short TM_PROMOTE_PROMOTABLE_XACT = 6;
   static final short TM_COMMIT_XACT = 7;
   static final short TM_ROLLBACK_XACT = 8;
   static final short TM_SAVE_XACT = 9;
   static final byte PKT_QUERY = 1;
   static final byte PKT_RPC = 3;
   static final byte PKT_REPLY = 4;
   static final byte PKT_CANCEL_REQ = 6;
   static final byte PKT_BULK = 7;
   static final byte PKT_DTC = 14;
   static final byte PKT_LOGON70 = 16;
   static final byte PKT_SSPI = 17;
   static final byte PKT_PRELOGIN = 18;
   static final byte PKT_FEDAUTH_TOKEN_MESSAGE = 8;
   static final byte STATUS_NORMAL = 0;
   static final byte STATUS_BIT_EOM = 1;
   static final byte STATUS_BIT_ATTENTION = 2;
   static final byte STATUS_BIT_RESET_CONN = 8;
   static final int INVALID_PACKET_SIZE = -1;
   static final int INITIAL_PACKET_SIZE = 4096;
   static final int MIN_PACKET_SIZE = 512;
   static final int MAX_PACKET_SIZE = 32767;
   static final int DEFAULT_PACKET_SIZE = 8000;
   static final int SERVER_PACKET_SIZE = 0;
   static final int PACKET_HEADER_SIZE = 8;
   static final int PACKET_HEADER_MESSAGE_TYPE = 0;
   static final int PACKET_HEADER_MESSAGE_STATUS = 1;
   static final int PACKET_HEADER_MESSAGE_LENGTH = 2;
   static final int PACKET_HEADER_SPID = 4;
   static final int PACKET_HEADER_SEQUENCE_NUM = 6;
   static final int PACKET_HEADER_WINDOW = 7;
   static final int MARS_HEADER_LENGTH = 18;
   static final int TRACE_HEADER_LENGTH = 26;
   static final short HEADERTYPE_TRACE = 3;
   static final int MESSAGE_HEADER_LENGTH = 22;
   static final byte B_PRELOGIN_OPTION_VERSION = 0;
   static final byte B_PRELOGIN_OPTION_ENCRYPTION = 1;
   static final byte B_PRELOGIN_OPTION_INSTOPT = 2;
   static final byte B_PRELOGIN_OPTION_THREADID = 3;
   static final byte B_PRELOGIN_OPTION_MARS = 4;
   static final byte B_PRELOGIN_OPTION_TRACEID = 5;
   static final byte B_PRELOGIN_OPTION_FEDAUTHREQUIRED = 6;
   static final byte B_PRELOGIN_OPTION_TERMINATOR = -1;
   static final byte LOGIN_OPTION1_ORDER_X86 = 0;
   static final byte LOGIN_OPTION1_ORDER_6800 = 1;
   static final byte LOGIN_OPTION1_CHARSET_ASCII = 0;
   static final byte LOGIN_OPTION1_CHARSET_EBCDIC = 2;
   static final byte LOGIN_OPTION1_FLOAT_IEEE_754 = 0;
   static final byte LOGIN_OPTION1_FLOAT_VAX = 4;
   static final byte LOGIN_OPTION1_FLOAT_ND5000 = 8;
   static final byte LOGIN_OPTION1_DUMPLOAD_ON = 0;
   static final byte LOGIN_OPTION1_DUMPLOAD_OFF = 16;
   static final byte LOGIN_OPTION1_USE_DB_ON = 0;
   static final byte LOGIN_OPTION1_USE_DB_OFF = 32;
   static final byte LOGIN_OPTION1_INIT_DB_WARN = 0;
   static final byte LOGIN_OPTION1_INIT_DB_FATAL = 64;
   static final byte LOGIN_OPTION1_SET_LANG_OFF = 0;
   static final byte LOGIN_OPTION1_SET_LANG_ON = -128;
   static final byte LOGIN_OPTION2_INIT_LANG_WARN = 0;
   static final byte LOGIN_OPTION2_INIT_LANG_FATAL = 1;
   static final byte LOGIN_OPTION2_ODBC_OFF = 0;
   static final byte LOGIN_OPTION2_ODBC_ON = 2;
   static final byte LOGIN_OPTION2_TRAN_BOUNDARY_OFF = 0;
   static final byte LOGIN_OPTION2_TRAN_BOUNDARY_ON = 4;
   static final byte LOGIN_OPTION2_CACHE_CONNECTION_OFF = 0;
   static final byte LOGIN_OPTION2_CACHE_CONNECTION_ON = 8;
   static final byte LOGIN_OPTION2_USER_NORMAL = 0;
   static final byte LOGIN_OPTION2_USER_SERVER = 16;
   static final byte LOGIN_OPTION2_USER_REMUSER = 32;
   static final byte LOGIN_OPTION2_USER_SQLREPL = 48;
   static final byte LOGIN_OPTION2_INTEGRATED_SECURITY_OFF = 0;
   static final byte LOGIN_OPTION2_INTEGRATED_SECURITY_ON = -128;
   static final byte LOGIN_OPTION3_DEFAULT = 0;
   static final byte LOGIN_OPTION3_CHANGE_PASSWORD = 1;
   static final byte LOGIN_OPTION3_SEND_YUKON_BINARY_XML = 2;
   static final byte LOGIN_OPTION3_USER_INSTANCE = 4;
   static final byte LOGIN_OPTION3_UNKNOWN_COLLATION_HANDLING = 8;
   static final byte LOGIN_OPTION3_FEATURE_EXTENSION = 16;
   static final byte LOGIN_SQLTYPE_DEFAULT = 0;
   static final byte LOGIN_SQLTYPE_TSQL = 1;
   static final byte LOGIN_SQLTYPE_ANSI_V1 = 2;
   static final byte LOGIN_SQLTYPE_ANSI89_L1 = 3;
   static final byte LOGIN_SQLTYPE_ANSI89_L2 = 4;
   static final byte LOGIN_SQLTYPE_ANSI89_IEF = 5;
   static final byte LOGIN_SQLTYPE_ANSI89_ENTRY = 6;
   static final byte LOGIN_SQLTYPE_ANSI89_TRANS = 7;
   static final byte LOGIN_SQLTYPE_ANSI89_INTER = 8;
   static final byte LOGIN_SQLTYPE_ANSI89_FULL = 9;
   static final byte LOGIN_OLEDB_OFF = 0;
   static final byte LOGIN_OLEDB_ON = 16;
   static final byte LOGIN_READ_ONLY_INTENT = 32;
   static final byte LOGIN_READ_WRITE_INTENT = 0;
   static final byte ENCRYPT_OFF = 0;
   static final byte ENCRYPT_ON = 1;
   static final byte ENCRYPT_NOT_SUP = 2;
   static final byte ENCRYPT_REQ = 3;
   static final byte ENCRYPT_INVALID = -1;
   static final byte B_PRELOGIN_MESSAGE_LENGTH = 67;
   static final byte B_PRELOGIN_MESSAGE_LENGTH_WITH_FEDAUTH = 73;
   static final int SCROLLOPT_KEYSET = 1;
   static final int SCROLLOPT_DYNAMIC = 2;
   static final int SCROLLOPT_FORWARD_ONLY = 4;
   static final int SCROLLOPT_STATIC = 8;
   static final int SCROLLOPT_FAST_FORWARD = 16;
   static final int SCROLLOPT_PARAMETERIZED_STMT = 4096;
   static final int SCROLLOPT_AUTO_FETCH = 8192;
   static final int SCROLLOPT_AUTO_CLOSE = 16384;
   static final int CCOPT_READ_ONLY = 1;
   static final int CCOPT_SCROLL_LOCKS = 2;
   static final int CCOPT_OPTIMISTIC_CC = 4;
   static final int CCOPT_OPTIMISTIC_CCVAL = 8;
   static final int CCOPT_ALLOW_DIRECT = 8192;
   static final int CCOPT_UPDT_IN_PLACE = 16384;
   static final int ROWSTAT_FETCH_SUCCEEDED = 1;
   static final int ROWSTAT_FETCH_MISSING = 2;
   static final int COLINFO_STATUS_EXPRESSION = 4;
   static final int COLINFO_STATUS_KEY = 8;
   static final int COLINFO_STATUS_HIDDEN = 16;
   static final int COLINFO_STATUS_DIFFERENT_NAME = 32;
   static final int MAX_FRACTIONAL_SECONDS_SCALE = 7;
   static final Timestamp MAX_TIMESTAMP = Timestamp.valueOf("2079-06-06 23:59:59");
   static final Timestamp MIN_TIMESTAMP = Timestamp.valueOf("1900-01-01 00:00:00");
   static final int DAYS_INTO_CE_LENGTH = 3;
   static final int MINUTES_OFFSET_LENGTH = 2;
   static final int DAYS_PER_YEAR = 365;
   static final int BASE_YEAR_1900 = 1900;
   static final int BASE_YEAR_1970 = 1970;
   static final String BASE_DATE_1970 = "1970-01-01";

   static final String getTokenName(int var0) {
      switch (var0) {
         case 121:
            return "TDS_RET_STAT (0x79)";
         case 129:
            return "TDS_COLMETADATA (0x81)";
         case 164:
            return "TDS_TABNAME (0xA4)";
         case 165:
            return "TDS_COLINFO (0xA5)";
         case 169:
            return "TDS_ORDER (0xA9)";
         case 170:
            return "TDS_ERR (0xAA)";
         case 171:
            return "TDS_MSG (0xAB)";
         case 172:
            return "TDS_RETURN_VALUE (0xAC)";
         case 173:
            return "TDS_LOGIN_ACK (0xAD)";
         case 174:
            return "TDS_FEATURE_EXTENSION_ACK (0xAE)";
         case 209:
            return "TDS_ROW (0xD1)";
         case 210:
            return "TDS_NBCROW (0xD2)";
         case 227:
            return "TDS_ENV_CHG (0xE3)";
         case 237:
            return "TDS_SSPI (0xED)";
         case 238:
            return "TDS_FEDAUTHINFO (0xEE)";
         case 253:
            return "TDS_DONE (0xFD)";
         case 254:
            return "TDS_DONEPROC (0xFE)";
         case 255:
            return "TDS_DONEINPROC (0xFF)";
         default:
            return "unknown token (0x" + Integer.toHexString(var0).toUpperCase() + ")";
      }
   }

   static final String getEncryptionLevel(int var0) {
      switch (var0) {
         case 0:
            return "OFF";
         case 1:
            return "ON";
         case 2:
            return "NOT SUPPORTED";
         case 3:
            return "REQUIRED";
         default:
            return "unknown encryption level (0x" + Integer.toHexString(var0).toUpperCase() + ")";
      }
   }

   static int nanosSinceMidnightLength(int var0) {
      int[] var1 = new int[]{3, 3, 3, 4, 4, 5, 5, 5};
      assert var0 >= 0;
      assert var0 <= 7;
      return var1[var0];
   }

   static int timeValueLength(int var0) {
      return nanosSinceMidnightLength(var0);
   }

   static int datetime2ValueLength(int var0) {
      return 3 + nanosSinceMidnightLength(var0);
   }

   static int datetimeoffsetValueLength(int var0) {
      return 5 + nanosSinceMidnightLength(var0);
   }

   private TDS() {
   }
}
