package com.microsoft.sqlserver.jdbc;

class StreamDone extends StreamPacket {
   private short status;
   private long rowCount;
   static final short CMD_SELECT = 193;
   static final short CMD_INSERT = 195;
   static final short CMD_DELETE = 196;
   static final short CMD_UPDATE = 197;
   static final short CMD_EXECUTE = 224;
   static final short CMD_BULKINSERT = 240;
   static final short CMD_MERGE = 279;
   static final short CMD_CNST_CREATE = 46;
   static final short CMD_DENY = 153;
   static final short CMD_DROPSCHEMA = 176;
   static final short CMD_FUNCCREATE = 178;
   static final short CMD_FUNCDESTROY = 179;
   static final short CMD_ASMCREATE = 181;
   static final short CMD_CMD = 182;
   static final short CMD_TABCREATE = 198;
   static final short CMD_TABDESTROY = 199;
   static final short CMD_INDCREATE = 200;
   static final short CMD_INDDESTROY = 201;
   static final short CMD_DBCREATE = 203;
   static final short CMD_DBDESTROY = 204;
   static final short CMD_GRANT = 205;
   static final short CMD_REVOKE = 206;
   static final short CMD_VIEWCREATE = 207;
   static final short CMD_VIEWDESTROY = 208;
   static final short CMD_DBEXTEND = 215;
   static final short CMD_ALTERTAB = 216;
   static final short CMD_TRIGCREATE = 221;
   static final short CMD_PROCCREATE = 222;
   static final short CMD_PROCDESTROY = 223;
   static final short CMD_TRIGDESTROY = 225;
   static final short CMD_DBCC_CMD = 230;
   static final short CMD_DEFAULTCREATE = 233;
   static final short CMD_RULECREATE = 236;
   static final short CMD_RULEDESTROY = 237;
   static final short CMD_DEFAULTDESTROY = 238;
   static final short CMD_STATSDESTROY = 256;
   static final short CMD_ASMDESTROY = 270;
   static final short CMD_ASMALTER = 271;
   static final short CMD_TYPEDESTROY = 272;
   static final short CMD_TYPECREATE = 273;
   static final short CMD_CLRPROCEDURECREATE = 274;
   static final short CMD_CLRFUNCTIONCREATE = 275;
   static final short CMD_SERVICEALTER = 276;
   static final short CMD_MSGTYPECREATE = 277;
   static final short CMD_MSGTYPEDESTROY = 278;
   static final short CMD_CONTRACTCREATE = 281;
   static final short CMD_CONTRACTDESTROY = 282;
   static final short CMD_SERVICECREATE = 283;
   static final short CMD_SERVICEDESTROY = 284;
   static final short CMD_QUEUECREATE = 285;
   static final short CMD_QUEUEDESTROY = 286;
   static final short CMD_QUEUEALTER = 287;
   static final short CMD_FTXTINDEX_CREATE = 294;
   static final short CMD_FTXTINDEX_ALTER = 295;
   static final short CMD_FTXTINDEX_DROP = 296;
   static final short CMD_PRTFUNCTIONCREATE = 297;
   static final short CMD_PRTFUNCTIONDROP = 298;
   static final short CMD_PRTSCHEMECREATE = 299;
   static final short CMD_PRTSCHEMEDROP = 300;
   static final short CMD_FTXTCATALOG_CREATE = 304;
   static final short CMD_FTXTCATALOG_ALTER = 305;
   static final short CMD_FTXTCATALOG_DROP = 306;
   static final short CMD_XMLSCHEMACREATE = 309;
   static final short CMD_XMLSCHEMAALTER = 310;
   static final short CMD_XMLSCHEMADROP = 311;
   static final short CMD_ENDPOINTCREATE = 312;
   static final short CMD_ENDPOINTALTER = 313;
   static final short CMD_ENDPOINTDROP = 314;
   static final short CMD_USERCREATE = 315;
   static final short CMD_USERALTER = 316;
   static final short CMD_USERDROP = 317;
   static final short CMD_ROLECREATE = 319;
   static final short CMD_ROLEALTER = 320;
   static final short CMD_ROLEDROP = 321;
   static final short CMD_APPROLECREATE = 322;
   static final short CMD_APPROLEALTER = 323;
   static final short CMD_APPROLEDROP = 324;
   static final short CMD_LOGINCREATE = 325;
   static final short CMD_LOGINALTER = 326;
   static final short CMD_LOGINDROP = 327;
   static final short CMD_SYNONYMCREATE = 328;
   static final short CMD_SYNONYMDROP = 329;
   static final short CMD_CREATESCHEMA = 330;
   static final short CMD_ALTERSCHEMA = 331;
   static final short CMD_AGGCREATE = 332;
   static final short CMD_AGGDESTROY = 333;
   static final short CMD_CLRTRIGGERCREATE = 334;
   static final short CMD_PRTFUNCTIONALTER = 335;
   static final short CMD_PRTSCHEMEALTER = 336;
   static final short CMD_INDALTER = 337;
   static final short CMD_ROUTECREATE = 343;
   static final short CMD_ROUTEALTER = 344;
   static final short CMD_ROUTEDESTROY = 346;
   static final short CMD_EVENTNOTIFICATIONCREATE = 352;
   static final short CMD_EVENTNOTIFICATIONDROP = 353;
   static final short CMD_XMLINDEXCREATE = 354;
   static final short CMD_BINDINGCREATE = 358;
   static final short CMD_BINDINGALTER = 359;
   static final short CMD_BINDINGDESTROY = 360;
   static final short CMD_MSGTYPEALTER = 366;
   static final short CMD_CERTCREATE = 368;
   static final short CMD_CERTDROP = 369;
   static final short CMD_CERTALTER = 370;
   static final short CMD_SECDESCCREATE = 381;
   static final short CMD_SECDESCDROP = 382;
   static final short CMD_SECDESCALTER = 383;
   static final short CMD_OBFUSKEYCREATE = 386;
   static final short CMD_OBFUSKEYALTER = 387;
   static final short CMD_OBFUSKEYDROP = 388;
   static final short CMD_ALTERAUTHORIZATION = 396;
   static final short CMD_CREDENTIALCREATE = 408;
   static final short CMD_CREDENTIALALTER = 409;
   static final short CMD_CREDENTIALDROP = 410;
   static final short CMD_MASTERKEYCREATE = 411;
   static final short CMD_MASTERKEYDROP = 412;
   static final short CMD_MASTERKEYALTER = 417;
   static final short CMD_ASYMKEYCREATE = 419;
   static final short CMD_ASYMKEYDROP = 420;
   static final short CMD_ASYMKEYALTER = 425;
   private short curCmd;

   void setFromTDS(TDSReader var1) throws SQLServerException {
      this.packetType = var1.readUnsignedByte();
      assert 253 == this.packetType || 254 == this.packetType || 255 == this.packetType;
      this.status = var1.readShort();
      this.curCmd = var1.readShort();
      this.rowCount = var1.readLong();
      if (this.isAttnAck()) {
         var1.getCommand().onAttentionAck();
      }
   }

   final short getCurCmd() {
      return this.curCmd;
   }

   final boolean isFinal() {
      return (this.status & 1) == 0;
   }

   final boolean isError() {
      return (this.status & 2) != 0;
   }

   final boolean updateCountIsValid() {
      return (this.status & 16) != 0;
   }

   final boolean isAttnAck() {
      return (this.status & 32) != 0;
   }

   final boolean wasRPCInBatch() {
      return (this.status & 128) != 0;
   }

   final long getUpdateCount() {
      assert this.cmdIsDMLOrDDL();
      switch (this.curCmd) {
         case 195:
         case 196:
         case 197:
         case 240:
         case 279:
            return this.updateCountIsValid() ? this.rowCount : -1L;
         default:
            return 0L;
      }
   }

   final boolean cmdIsDMLOrDDL() {
      switch (this.curCmd) {
         case 46:
         case 153:
         case 176:
         case 178:
         case 179:
         case 181:
         case 182:
         case 195:
         case 196:
         case 197:
         case 198:
         case 199:
         case 200:
         case 201:
         case 203:
         case 204:
         case 205:
         case 206:
         case 207:
         case 208:
         case 215:
         case 216:
         case 221:
         case 222:
         case 223:
         case 225:
         case 230:
         case 233:
         case 236:
         case 237:
         case 238:
         case 240:
         case 256:
         case 270:
         case 271:
         case 272:
         case 273:
         case 274:
         case 275:
         case 276:
         case 277:
         case 278:
         case 279:
         case 281:
         case 282:
         case 283:
         case 284:
         case 285:
         case 286:
         case 287:
         case 294:
         case 295:
         case 296:
         case 297:
         case 298:
         case 299:
         case 300:
         case 304:
         case 305:
         case 306:
         case 309:
         case 310:
         case 311:
         case 312:
         case 313:
         case 314:
         case 315:
         case 316:
         case 317:
         case 319:
         case 320:
         case 321:
         case 322:
         case 323:
         case 324:
         case 325:
         case 326:
         case 327:
         case 328:
         case 329:
         case 330:
         case 331:
         case 332:
         case 333:
         case 334:
         case 335:
         case 336:
         case 337:
         case 343:
         case 344:
         case 346:
         case 352:
         case 353:
         case 354:
         case 358:
         case 359:
         case 360:
         case 366:
         case 368:
         case 369:
         case 370:
         case 381:
         case 382:
         case 383:
         case 386:
         case 387:
         case 388:
         case 396:
         case 408:
         case 409:
         case 410:
         case 411:
         case 412:
         case 417:
         case 419:
         case 420:
         case 425:
            return true;
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 64:
         case 65:
         case 66:
         case 67:
         case 68:
         case 69:
         case 70:
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 76:
         case 77:
         case 78:
         case 79:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         case 85:
         case 86:
         case 87:
         case 88:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         case 97:
         case 98:
         case 99:
         case 100:
         case 101:
         case 102:
         case 103:
         case 104:
         case 105:
         case 106:
         case 107:
         case 108:
         case 109:
         case 110:
         case 111:
         case 112:
         case 113:
         case 114:
         case 115:
         case 116:
         case 117:
         case 118:
         case 119:
         case 120:
         case 121:
         case 122:
         case 123:
         case 124:
         case 125:
         case 126:
         case 127:
         case 128:
         case 129:
         case 130:
         case 131:
         case 132:
         case 133:
         case 134:
         case 135:
         case 136:
         case 137:
         case 138:
         case 139:
         case 140:
         case 141:
         case 142:
         case 143:
         case 144:
         case 145:
         case 146:
         case 147:
         case 148:
         case 149:
         case 150:
         case 151:
         case 152:
         case 154:
         case 155:
         case 156:
         case 157:
         case 158:
         case 159:
         case 160:
         case 161:
         case 162:
         case 163:
         case 164:
         case 165:
         case 166:
         case 167:
         case 168:
         case 169:
         case 170:
         case 171:
         case 172:
         case 173:
         case 174:
         case 175:
         case 177:
         case 180:
         case 183:
         case 184:
         case 185:
         case 186:
         case 187:
         case 188:
         case 189:
         case 190:
         case 191:
         case 192:
         case 193:
         case 194:
         case 202:
         case 209:
         case 210:
         case 211:
         case 212:
         case 213:
         case 214:
         case 217:
         case 218:
         case 219:
         case 220:
         case 224:
         case 226:
         case 227:
         case 228:
         case 229:
         case 231:
         case 232:
         case 234:
         case 235:
         case 239:
         case 241:
         case 242:
         case 243:
         case 244:
         case 245:
         case 246:
         case 247:
         case 248:
         case 249:
         case 250:
         case 251:
         case 252:
         case 253:
         case 254:
         case 255:
         case 257:
         case 258:
         case 259:
         case 260:
         case 261:
         case 262:
         case 263:
         case 264:
         case 265:
         case 266:
         case 267:
         case 268:
         case 269:
         case 280:
         case 288:
         case 289:
         case 290:
         case 291:
         case 292:
         case 293:
         case 301:
         case 302:
         case 303:
         case 307:
         case 308:
         case 318:
         case 338:
         case 339:
         case 340:
         case 341:
         case 342:
         case 345:
         case 347:
         case 348:
         case 349:
         case 350:
         case 351:
         case 355:
         case 356:
         case 357:
         case 361:
         case 362:
         case 363:
         case 364:
         case 365:
         case 367:
         case 371:
         case 372:
         case 373:
         case 374:
         case 375:
         case 376:
         case 377:
         case 378:
         case 379:
         case 380:
         case 384:
         case 385:
         case 389:
         case 390:
         case 391:
         case 392:
         case 393:
         case 394:
         case 395:
         case 397:
         case 398:
         case 399:
         case 400:
         case 401:
         case 402:
         case 403:
         case 404:
         case 405:
         case 406:
         case 407:
         case 413:
         case 414:
         case 415:
         case 416:
         case 418:
         case 421:
         case 422:
         case 423:
         case 424:
         default:
            return false;
      }
   }
}
