package com.microsoft.sqlserver.jdbc;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

final class SQLCollation implements Serializable {
   private final int info;
   private final int sortId;
   private final Encoding encoding;
   private static final Map<Integer, SQLCollation.WindowsLocale> localeIndex = new HashMap();
   private static final HashMap<Integer, SQLCollation.SortOrder> sortOrderIndex;

   private final int langID() {
      return this.info & 65535;
   }

   final String getCharset() {
      return this.encoding.charsetName();
   }

   final boolean supportsAsciiConversion() {
      return this.encoding.supportsAsciiConversion();
   }

   final boolean hasAsciiCompatibleSBCS() {
      return this.encoding.hasAsciiCompatibleSBCS();
   }

   static final int tdsLength() {
      return 5;
   }

   SQLCollation(TDSReader var1) throws UnsupportedEncodingException, SQLServerException {
      this.info = var1.readInt();
      this.sortId = var1.readUnsignedByte();
      this.encoding = 0 == this.sortId ? this.encodingFromLCID() : this.encodingFromSortId();
   }

   void writeCollation(TDSWriter var1) throws SQLServerException {
      var1.writeInt(this.info);
      var1.writeByte((byte)(this.sortId & 0xFF));
   }

   private Encoding encodingFromLCID() throws UnsupportedEncodingException {
      SQLCollation.WindowsLocale var1 = (SQLCollation.WindowsLocale)localeIndex.get(this.langID());
      if (null == var1) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_unknownLCID"));
         Object[] var7 = new Object[]{Integer.toHexString(this.langID()).toUpperCase()};
         throw new UnsupportedEncodingException(var2.format(var7));
      }

      try {
         return var1.getEncoding();
      } catch (UnsupportedEncodingException var6) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_unknownLCID"));
         Object[] var4 = new Object[]{var1};
         UnsupportedEncodingException var5 = new UnsupportedEncodingException(var3.format(var4));
         var5.initCause(var6);
         throw var5;
      }
   }

   private Encoding encodingFromSortId() throws UnsupportedEncodingException {
      SQLCollation.SortOrder var1 = (SQLCollation.SortOrder)sortOrderIndex.get(this.sortId);
      if (null == var1) {
         MessageFormat var2 = new MessageFormat(SQLServerException.getErrString("R_unknownSortId"));
         Object[] var7 = new Object[]{this.sortId};
         throw new UnsupportedEncodingException(var2.format(var7));
      }

      try {
         return var1.getEncoding();
      } catch (UnsupportedEncodingException var6) {
         MessageFormat var3 = new MessageFormat(SQLServerException.getErrString("R_unknownSortId"));
         Object[] var4 = new Object[]{var1};
         UnsupportedEncodingException var5 = new UnsupportedEncodingException(var3.format(var4));
         var5.initCause(var6);
         throw var5;
      }
   }

   static {
      for (SQLCollation.WindowsLocale var1 : EnumSet.allOf(SQLCollation.WindowsLocale.class)) {
         localeIndex.put(var1.langID, var1);
      }

      sortOrderIndex = new HashMap();

      for (SQLCollation.SortOrder var3 : EnumSet.allOf(SQLCollation.SortOrder.class)) {
         sortOrderIndex.put(var3.sortId, var3);
      }
   }

   enum SortOrder {
      BIN_CP437(30, "SQL_Latin1_General_CP437_BIN", Encoding.CP437),
      DICTIONARY_437(31, "SQL_Latin1_General_CP437_CS_AS", Encoding.CP437),
      NOCASE_437(32, "SQL_Latin1_General_CP437_CI_AS", Encoding.CP437),
      NOCASEPREF_437(33, "SQL_Latin1_General_Pref_CP437_CI_AS", Encoding.CP437),
      NOACCENTS_437(34, "SQL_Latin1_General_CP437_CI_AI", Encoding.CP437),
      BIN2_CP437(35, "SQL_Latin1_General_CP437_BIN2", Encoding.CP437),
      BIN_CP850(40, "SQL_Latin1_General_CP850_BIN", Encoding.CP850),
      DICTIONARY_850(41, "SQL_Latin1_General_CP850_CS_AS", Encoding.CP850),
      NOCASE_850(42, "SQL_Latin1_General_CP850_CI_AS", Encoding.CP850),
      NOCASEPREF_850(43, "SQL_Latin1_General_Pref_CP850_CI_AS", Encoding.CP850),
      NOACCENTS_850(44, "SQL_Latin1_General_CP850_CI_AI", Encoding.CP850),
      BIN2_CP850(45, "SQL_Latin1_General_CP850_BIN2", Encoding.CP850),
      CASELESS_34(49, "SQL_1xCompat_CP850_CI_AS", Encoding.CP850),
      BIN_ISO_1(50, "bin_iso_1", Encoding.CP1252),
      DICTIONARY_ISO(51, "SQL_Latin1_General_CP1_CS_AS", Encoding.CP1252),
      NOCASE_ISO(52, "SQL_Latin1_General_CP1_CI_AS", Encoding.CP1252),
      NOCASEPREF_ISO(53, "SQL_Latin1_General_Pref_CP1_CI_AS", Encoding.CP1252),
      NOACCENTS_ISO(54, "SQL_Latin1_General_CP1_CI_AI", Encoding.CP1252),
      ALT_DICTIONARY(55, "SQL_AltDiction_CP850_CS_AS", Encoding.CP850),
      ALT_NOCASEPREF(56, "SQL_AltDiction_Pref_CP850_CI_AS", Encoding.CP850),
      ALT_NOACCENTS(57, "SQL_AltDiction_CP850_CI_AI", Encoding.CP850),
      SCAND_NOCASEPREF(58, "SQL_Scandinavian_Pref_CP850_CI_AS", Encoding.CP850),
      SCAND_DICTIONARY(59, "SQL_Scandinavian_CP850_CS_AS", Encoding.CP850),
      SCAND_NOCASE(60, "SQL_Scandinavian_CP850_CI_AS", Encoding.CP850),
      ALT_NOCASE(61, "SQL_AltDiction_CP850_CI_AS", Encoding.CP850),
      DICTIONARY_1252(71, "dictionary_1252", Encoding.CP1252),
      NOCASE_1252(72, "nocase_1252", Encoding.CP1252),
      DNK_NOR_DICTIONARY(73, "dnk_nor_dictionary", Encoding.CP1252),
      FIN_SWE_DICTIONARY(74, "fin_swe_dictionary", Encoding.CP1252),
      ISL_DICTIONARY(75, "isl_dictionary", Encoding.CP1252),
      BIN_CP1250(80, "bin_cp1250", Encoding.CP1250),
      DICTIONARY_1250(81, "SQL_Latin1_General_CP1250_CS_AS", Encoding.CP1250),
      NOCASE_1250(82, "SQL_Latin1_General_CP1250_CI_AS", Encoding.CP1250),
      CSYDIC(83, "SQL_Czech_CP1250_CS_AS", Encoding.CP1250),
      CSYNC(84, "SQL_Czech_CP1250_CI_AS", Encoding.CP1250),
      HUNDIC(85, "SQL_Hungarian_CP1250_CS_AS", Encoding.CP1250),
      HUNNC(86, "SQL_Hungarian_CP1250_CI_AS", Encoding.CP1250),
      PLKDIC(87, "SQL_Polish_CP1250_CS_AS", Encoding.CP1250),
      PLKNC(88, "SQL_Polish_CP1250_CI_AS", Encoding.CP1250),
      ROMDIC(89, "SQL_Romanian_CP1250_CS_AS", Encoding.CP1250),
      ROMNC(90, "SQL_Romanian_CP1250_CI_AS", Encoding.CP1250),
      SHLDIC(91, "SQL_Croatian_CP1250_CS_AS", Encoding.CP1250),
      SHLNC(92, "SQL_Croatian_CP1250_CI_AS", Encoding.CP1250),
      SKYDIC(93, "SQL_Slovak_CP1250_CS_AS", Encoding.CP1250),
      SKYNC(94, "SQL_Slovak_CP1250_CI_AS", Encoding.CP1250),
      SLVDIC(95, "SQL_Slovenian_CP1250_CS_AS", Encoding.CP1250),
      SLVNC(96, "SQL_Slovenian_CP1250_CI_AS", Encoding.CP1250),
      POLISH_CS(97, "polish_cs", Encoding.CP1250),
      POLISH_CI(98, "polish_ci", Encoding.CP1250),
      BIN_CP1251(104, "bin_cp1251", Encoding.CP1251),
      DICTIONARY_1251(105, "SQL_Latin1_General_CP1251_CS_AS", Encoding.CP1251),
      NOCASE_1251(106, "SQL_Latin1_General_CP1251_CI_AS", Encoding.CP1251),
      UKRDIC(107, "SQL_Ukrainian_CP1251_CS_AS", Encoding.CP1251),
      UKRNC(108, "SQL_Ukrainian_CP1251_CI_AS", Encoding.CP1251),
      BIN_CP1253(112, "bin_cp1253", Encoding.CP1253),
      DICTIONARY_1253(113, "SQL_Latin1_General_CP1253_CS_AS", Encoding.CP1253),
      NOCASE_1253(114, "SQL_Latin1_General_CP1253_CI_AS", Encoding.CP1253),
      GREEK_MIXEDDICTIONARY(120, "SQL_MixDiction_CP1253_CS_AS", Encoding.CP1253),
      GREEK_ALTDICTIONARY(121, "SQL_AltDiction_CP1253_CS_AS", Encoding.CP1253),
      GREEK_ALTDICTIONARY2(122, "SQL_AltDiction2_CP1253_CS_AS", Encoding.CP1253),
      GREEK_NOCASEDICT(124, "SQL_Latin1_General_CP1253_CI_AI", Encoding.CP1253),
      BIN_CP1254(128, "bin_cp1254", Encoding.CP1254),
      DICTIONARY_1254(129, "SQL_Latin1_General_CP1254_CS_AS", Encoding.CP1254),
      NOCASE_1254(130, "SQL_Latin1_General_CP1254_CI_AS", Encoding.CP1254),
      BIN_CP1255(136, "bin_cp1255", Encoding.CP1255),
      DICTIONARY_1255(137, "SQL_Latin1_General_CP1255_CS_AS", Encoding.CP1255),
      NOCASE_1255(138, "SQL_Latin1_General_CP1255_CI_AS", Encoding.CP1255),
      BIN_CP1256(144, "bin_cp1256", Encoding.CP1256),
      DICTIONARY_1256(145, "SQL_Latin1_General_CP1256_CS_AS", Encoding.CP1256),
      NOCASE_1256(146, "SQL_Latin1_General_CP1256_CI_AS", Encoding.CP1256),
      BIN_CP1257(152, "bin_cp1257", Encoding.CP1257),
      DICTIONARY_1257(153, "SQL_Latin1_General_CP1257_CS_AS", Encoding.CP1257),
      NOCASE_1257(154, "SQL_Latin1_General_CP1257_CI_AS", Encoding.CP1257),
      ETIDIC(155, "SQL_Estonian_CP1257_CS_AS", Encoding.CP1257),
      ETINC(156, "SQL_Estonian_CP1257_CI_AS", Encoding.CP1257),
      LVIDIC(157, "SQL_Latvian_CP1257_CS_AS", Encoding.CP1257),
      LVINC(158, "SQL_Latvian_CP1257_CI_AS", Encoding.CP1257),
      LTHDIC(159, "SQL_Lithuanian_CP1257_CS_AS", Encoding.CP1257),
      LTHNC(160, "SQL_Lithuanian_CP1257_CI_AS", Encoding.CP1257),
      DANNO_NOCASEPREF(183, "SQL_Danish_Pref_CP1_CI_AS", Encoding.CP1252),
      SVFI1_NOCASEPREF(184, "SQL_SwedishPhone_Pref_CP1_CI_AS", Encoding.CP1252),
      SVFI2_NOCASEPREF(185, "SQL_SwedishStd_Pref_CP1_CI_AS", Encoding.CP1252),
      ISLAN_NOCASEPREF(186, "SQL_Icelandic_Pref_CP1_CI_AS", Encoding.CP1252),
      BIN_CP932(192, "bin_cp932", Encoding.CP932),
      NLS_CP932(193, "nls_cp932", Encoding.CP932),
      BIN_CP949(194, "bin_cp949", Encoding.CP949),
      NLS_CP949(195, "nls_cp949", Encoding.CP949),
      BIN_CP950(196, "bin_cp950", Encoding.CP950),
      NLS_CP950(197, "nls_cp950", Encoding.CP950),
      BIN_CP936(198, "bin_cp936", Encoding.CP936),
      NLS_CP936(199, "nls_cp936", Encoding.CP936),
      NLS_CP932_CS(200, "nls_cp932_cs", Encoding.CP932),
      NLS_CP949_CS(201, "nls_cp949_cs", Encoding.CP949),
      NLS_CP950_CS(202, "nls_cp950_cs", Encoding.CP950),
      NLS_CP936_CS(203, "nls_cp936_cs", Encoding.CP936),
      BIN_CP874(204, "bin_cp874", Encoding.CP874),
      NLS_CP874(205, "nls_cp874", Encoding.CP874),
      NLS_CP874_CS(206, "nls_cp874_cs", Encoding.CP874),
      EBCDIC_037(210, "SQL_EBCDIC037_CP1_CS_AS", Encoding.CP1252),
      EBCDIC_273(211, "SQL_EBCDIC273_CP1_CS_AS", Encoding.CP1252),
      EBCDIC_277(212, "SQL_EBCDIC277_CP1_CS_AS", Encoding.CP1252),
      EBCDIC_278(213, "SQL_EBCDIC278_CP1_CS_AS", Encoding.CP1252),
      EBCDIC_280(214, "SQL_EBCDIC280_CP1_CS_AS", Encoding.CP1252),
      EBCDIC_284(215, "SQL_EBCDIC284_CP1_CS_AS", Encoding.CP1252),
      EBCDIC_285(216, "SQL_EBCDIC285_CP1_CS_AS", Encoding.CP1252),
      EBCDIC_297(217, "SQL_EBCDIC297_CP1_CS_AS", Encoding.CP1252);

      private final int sortId;
      private final String name;
      private final Encoding encoding;

      final Encoding getEncoding() throws UnsupportedEncodingException {
         return this.encoding.checkSupported();
      }

      SortOrder(int var3, String var4, Encoding var5) {
         this.sortId = var3;
         this.name = var4;
         this.encoding = var5;
      }

      public final String toString() {
         return this.name;
      }
   }

   enum WindowsLocale {
      ar_SA(1025, Encoding.CP1256),
      bg_BG(1026, Encoding.CP1251),
      ca_ES(1027, Encoding.CP1252),
      zh_TW(1028, Encoding.CP950),
      cs_CZ(1029, Encoding.CP1250),
      da_DK(1030, Encoding.CP1252),
      de_DE(1031, Encoding.CP1252),
      el_GR(1032, Encoding.CP1253),
      en_US(1033, Encoding.CP1252),
      es_ES_tradnl(1034, Encoding.CP1252),
      fi_FI(1035, Encoding.CP1252),
      fr_FR(1036, Encoding.CP1252),
      he_IL(1037, Encoding.CP1255),
      hu_HU(1038, Encoding.CP1250),
      is_IS(1039, Encoding.CP1252),
      it_IT(1040, Encoding.CP1252),
      ja_JP(1041, Encoding.CP932),
      ko_KR(1042, Encoding.CP949),
      nl_NL(1043, Encoding.CP1252),
      nb_NO(1044, Encoding.CP1252),
      pl_PL(1045, Encoding.CP1250),
      pt_BR(1046, Encoding.CP1252),
      rm_CH(1047, Encoding.CP1252),
      ro_RO(1048, Encoding.CP1250),
      ru_RU(1049, Encoding.CP1251),
      hr_HR(1050, Encoding.CP1250),
      sk_SK(1051, Encoding.CP1250),
      sq_AL(1052, Encoding.CP1250),
      sv_SE(1053, Encoding.CP1252),
      th_TH(1054, Encoding.CP874),
      tr_TR(1055, Encoding.CP1254),
      ur_PK(1056, Encoding.CP1256),
      id_ID(1057, Encoding.CP1252),
      uk_UA(1058, Encoding.CP1251),
      be_BY(1059, Encoding.CP1251),
      sl_SI(1060, Encoding.CP1250),
      et_EE(1061, Encoding.CP1257),
      lv_LV(1062, Encoding.CP1257),
      lt_LT(1063, Encoding.CP1257),
      tg_Cyrl_TJ(1064, Encoding.CP1251),
      fa_IR(1065, Encoding.CP1256),
      vi_VN(1066, Encoding.CP1258),
      hy_AM(1067, Encoding.CP1252),
      az_Latn_AZ(1068, Encoding.CP1254),
      eu_ES(1069, Encoding.CP1252),
      wen_DE(1070, Encoding.CP1252),
      mk_MK(1071, Encoding.CP1251),
      tn_ZA(1074, Encoding.CP1252),
      xh_ZA(1076, Encoding.CP1252),
      zu_ZA(1077, Encoding.CP1252),
      Af_ZA(1078, Encoding.CP1252),
      ka_GE(1079, Encoding.CP1252),
      fo_FO(1080, Encoding.CP1252),
      hi_IN(1081, Encoding.UNICODE),
      mt_MT(1082, Encoding.UNICODE),
      se_NO(1083, Encoding.CP1252),
      ms_MY(1086, Encoding.CP1252),
      kk_KZ(1087, Encoding.CP1251),
      ky_KG(1088, Encoding.CP1251),
      sw_KE(1089, Encoding.CP1252),
      tk_TM(1090, Encoding.CP1250),
      uz_Latn_UZ(1091, Encoding.CP1254),
      tt_RU(1092, Encoding.CP1251),
      bn_IN(1093, Encoding.UNICODE),
      pa_IN(1094, Encoding.UNICODE),
      gu_IN(1095, Encoding.UNICODE),
      or_IN(1096, Encoding.UNICODE),
      ta_IN(1097, Encoding.UNICODE),
      te_IN(1098, Encoding.UNICODE),
      kn_IN(1099, Encoding.UNICODE),
      ml_IN(1100, Encoding.UNICODE),
      as_IN(1101, Encoding.UNICODE),
      mr_IN(1102, Encoding.UNICODE),
      sa_IN(1103, Encoding.UNICODE),
      mn_MN(1104, Encoding.CP1251),
      bo_CN(1105, Encoding.UNICODE),
      cy_GB(1106, Encoding.CP1252),
      km_KH(1107, Encoding.UNICODE),
      lo_LA(1108, Encoding.UNICODE),
      gl_ES(1110, Encoding.CP1252),
      kok_IN(1111, Encoding.UNICODE),
      syr_SY(1114, Encoding.UNICODE),
      si_LK(1115, Encoding.UNICODE),
      iu_Cans_CA(1117, Encoding.CP1252),
      am_ET(1118, Encoding.CP1252),
      ne_NP(1121, Encoding.UNICODE),
      fy_NL(1122, Encoding.CP1252),
      ps_AF(1123, Encoding.UNICODE),
      fil_PH(1124, Encoding.CP1252),
      dv_MV(1125, Encoding.UNICODE),
      ha_Latn_NG(1128, Encoding.CP1252),
      yo_NG(1130, Encoding.CP1252),
      quz_BO(1131, Encoding.CP1252),
      nso_ZA(1132, Encoding.CP1252),
      ba_RU(1133, Encoding.CP1251),
      lb_LU(1134, Encoding.CP1252),
      kl_GL(1135, Encoding.CP1252),
      ig_NG(1136, Encoding.CP1252),
      ii_CN(1144, Encoding.CP1252),
      arn_CL(1146, Encoding.CP1252),
      moh_CA(1148, Encoding.CP1252),
      br_FR(1150, Encoding.CP1252),
      ug_CN(1152, Encoding.CP1256),
      mi_NZ(1153, Encoding.UNICODE),
      oc_FR(1154, Encoding.CP1252),
      co_FR(1155, Encoding.CP1252),
      gsw_FR(1156, Encoding.CP1252),
      sah_RU(1157, Encoding.CP1251),
      qut_GT(1158, Encoding.CP1252),
      rw_RW(1159, Encoding.CP1252),
      wo_SN(1160, Encoding.CP1252),
      prs_AF(1164, Encoding.CP1256),
      ar_IQ(2049, Encoding.CP1256),
      zh_CN(2052, Encoding.CP936),
      de_CH(2055, Encoding.CP1252),
      en_GB(2057, Encoding.CP1252),
      es_MX(2058, Encoding.CP1252),
      fr_BE(2060, Encoding.CP1252),
      it_CH(2064, Encoding.CP1252),
      nl_BE(2067, Encoding.CP1252),
      nn_NO(2068, Encoding.CP1252),
      pt_PT(2070, Encoding.CP1252),
      sr_Latn_CS(2074, Encoding.CP1250),
      sv_FI(2077, Encoding.CP1252),
      Lithuanian_Classic(2087, Encoding.CP1257),
      az_Cyrl_AZ(2092, Encoding.CP1251),
      dsb_DE(2094, Encoding.CP1252),
      se_SE(2107, Encoding.CP1252),
      ga_IE(2108, Encoding.CP1252),
      ms_BN(2110, Encoding.CP1252),
      uz_Cyrl_UZ(2115, Encoding.CP1251),
      bn_BD(2117, Encoding.UNICODE),
      mn_Mong_CN(2128, Encoding.CP1251),
      iu_Latn_CA(2141, Encoding.CP1252),
      tzm_Latn_DZ(2143, Encoding.CP1252),
      quz_EC(2155, Encoding.CP1252),
      ar_EG(3073, Encoding.CP1256),
      zh_HK(3076, Encoding.CP950),
      de_AT(3079, Encoding.CP1252),
      en_AU(3081, Encoding.CP1252),
      es_ES(3082, Encoding.CP1252),
      fr_CA(3084, Encoding.CP1252),
      sr_Cyrl_CS(3098, Encoding.CP1251),
      se_FI(3131, Encoding.CP1252),
      quz_PE(3179, Encoding.CP1252),
      ar_LY(4097, Encoding.CP1256),
      zh_SG(4100, Encoding.CP936),
      de_LU(4103, Encoding.CP1252),
      en_CA(4105, Encoding.CP1252),
      es_GT(4106, Encoding.CP1252),
      fr_CH(4108, Encoding.CP1252),
      hr_BA(4122, Encoding.CP1250),
      smj_NO(4155, Encoding.CP1252),
      ar_DZ(5121, Encoding.CP1256),
      zh_MO(5124, Encoding.CP950),
      de_LI(5127, Encoding.CP1252),
      en_NZ(5129, Encoding.CP1252),
      es_CR(5130, Encoding.CP1252),
      fr_LU(5132, Encoding.CP1252),
      bs_Latn_BA(5146, Encoding.CP1250),
      smj_SE(5179, Encoding.CP1252),
      ar_MA(6145, Encoding.CP1256),
      en_IE(6153, Encoding.CP1252),
      es_PA(6154, Encoding.CP1252),
      fr_MC(6156, Encoding.CP1252),
      sr_Latn_BA(6170, Encoding.CP1250),
      sma_NO(6203, Encoding.CP1252),
      ar_TN(7169, Encoding.CP1256),
      en_ZA(7177, Encoding.CP1252),
      es_DO(7178, Encoding.CP1252),
      sr_Cyrl_BA(7194, Encoding.CP1251),
      sma_SB(7227, Encoding.CP1252),
      ar_OM(8193, Encoding.CP1256),
      en_JM(8201, Encoding.CP1252),
      es_VE(8202, Encoding.CP1252),
      bs_Cyrl_BA(8218, Encoding.CP1251),
      sms_FI(8251, Encoding.CP1252),
      ar_YE(9217, Encoding.CP1256),
      en_CB(9225, Encoding.CP1252),
      es_CO(9226, Encoding.CP1252),
      smn_FI(9275, Encoding.CP1252),
      ar_SY(10241, Encoding.CP1256),
      en_BZ(10249, Encoding.CP1252),
      es_PE(10250, Encoding.CP1252),
      ar_JO(11265, Encoding.CP1256),
      en_TT(11273, Encoding.CP1252),
      es_AR(11274, Encoding.CP1252),
      ar_LB(12289, Encoding.CP1256),
      en_ZW(12297, Encoding.CP1252),
      es_EC(12298, Encoding.CP1252),
      ar_KW(13313, Encoding.CP1256),
      en_PH(13321, Encoding.CP1252),
      es_CL(13322, Encoding.CP1252),
      ar_AE(14337, Encoding.CP1256),
      es_UY(14346, Encoding.CP1252),
      ar_BH(15361, Encoding.CP1256),
      es_PY(15370, Encoding.CP1252),
      ar_QA(16385, Encoding.CP1256),
      en_IN(16393, Encoding.CP1252),
      es_BO(16394, Encoding.CP1252),
      en_MY(17417, Encoding.CP1252),
      es_SV(17418, Encoding.CP1252),
      en_SG(18441, Encoding.CP1252),
      es_HN(18442, Encoding.CP1252),
      es_NI(19466, Encoding.CP1252),
      es_PR(20490, Encoding.CP1252),
      es_US(21514, Encoding.CP1252);

      private final int langID;
      private final Encoding encoding;

      WindowsLocale(int var3, Encoding var4) {
         this.langID = var3;
         this.encoding = var4;
      }

      final Encoding getEncoding() throws UnsupportedEncodingException {
         return this.encoding.checkSupported();
      }
   }
}
