package org.kittrellcodeworks.repository.hibernate
import org.hibernate.`type`.StandardBasicTypes._

import java.math._
import java.sql.{Blob, Clob, NClob}
import java.{lang => jl, net => jn, util => ju}

package object types {
  val OptionBoolean: OptionalType[jl.Boolean] = new OptionalType(BOOLEAN)
  val OptionNumericBoolean: OptionalType[jl.Boolean] = new OptionalType(NUMERIC_BOOLEAN)
  val OptionTrueFalse: OptionalType[jl.Boolean] = new OptionalType(TRUE_FALSE)
  val OptionYesNo: OptionalType[jl.Boolean] = new OptionalType(YES_NO)
  val OptionByte: OptionalType[jl.Byte] = new OptionalType(BYTE)
  val OptionShort: OptionalType[jl.Short] = new OptionalType(SHORT)
  val OptionInt: OptionalType[jl.Integer] = new OptionalType(INTEGER)
  val OptionLong: OptionalType[jl.Long] = new OptionalType(LONG)
  val OptionFloat: OptionalType[jl.Float] = new OptionalType(FLOAT)
  val OptionDouble: OptionalType[jl.Double] = new OptionalType(DOUBLE)
  val OptionBigInteger: OptionalType[BigInteger] = new OptionalType(BIG_INTEGER)
  val OptionBigDecimal: OptionalType[BigDecimal] = new OptionalType(BIG_DECIMAL)
  val OptionChar: OptionalType[Character] = new OptionalType(CHARACTER)
  val OptionString: OptionalType[String] = new OptionalType(STRING)
  val OptionNString: OptionalType[String] = new OptionalType(NSTRING)
  val OptionUrl: OptionalType[jn.URL] = new OptionalType(URL)
  val OptionTine: OptionalType[ju.Date] = new OptionalType(TIME)
  val OptionDate: OptionalType[ju.Date] = new OptionalType(DATE)
  val OptionTimestamp: OptionalType[ju.Date] = new OptionalType(TIMESTAMP)
  val OptionCalendar: OptionalType[ju.Calendar] = new OptionalType(CALENDAR)
  val OptionCalendarDate: OptionalType[ju.Calendar] = new OptionalType(CALENDAR_DATE)
  val OptionClass: OptionalType[Class[_]] = new OptionalType(CLASS)
  val OptionLocale: OptionalType[ju.Locale] = new OptionalType(LOCALE)
  val OptionCurrency: OptionalType[ju.Currency] = new OptionalType(CURRENCY)
  val OptionTimezone: OptionalType[ju.TimeZone] = new OptionalType(TIMEZONE)
  val OptionUuidBinary: OptionalType[ju.UUID] = new OptionalType(UUID_BINARY)
  val OptionUuidChar: OptionalType[ju.UUID] = new OptionalType(UUID_CHAR)
  val OptionBinary: OptionalType[Array[Byte]] = new OptionalType(BINARY)
  val OptionWrapperBinary: OptionalType[Array[jl.Byte]] = new OptionalType(WRAPPER_BINARY)
  val OptionRowVersion: OptionalType[Array[Byte]] = new OptionalType(ROW_VERSION)
  val OptionImage: OptionalType[Array[Byte]] = new OptionalType(IMAGE)
  val OptionBlob: OptionalType[Blob] = new OptionalType(BLOB)
  val OptionMaterializedBlob: OptionalType[Array[Byte]] = new OptionalType(MATERIALIZED_BLOB)
  val OptionCharArray: OptionalType[Array[Char]] = new OptionalType(CHAR_ARRAY)
  val OptionCharacterArray: OptionalType[Array[jl.Character]] = new OptionalType(CHARACTER_ARRAY)
  val OptionText: OptionalType[String] = new OptionalType(TEXT)
  val OptionNText: OptionalType[String] = new OptionalType(NTEXT)
  val OptionClob: OptionalType[Clob] = new OptionalType(CLOB)
  val OptionNClob: OptionalType[NClob] = new OptionalType(NCLOB)
  val OptionMaterializedClob: OptionalType[String] = new OptionalType(MATERIALIZED_CLOB)
  val OptionMaterializedNClob: OptionalType[String] = new OptionalType(MATERIALIZED_NCLOB)
  val OptionSerializable = new OptionalType(SERIALIZABLE) // including the type yields a compiler error!

  val optionalTypes: Seq[OptionalType[_]] = Seq(
    OptionBoolean,
    OptionNumericBoolean,
    OptionTrueFalse,
    OptionYesNo,
    OptionByte,
    OptionShort,
    OptionInt,
    OptionLong,
    OptionFloat,
    OptionDouble,
    OptionBigInteger,
    OptionBigDecimal,
    OptionChar,
    OptionString,
    OptionNString,
    OptionUrl,
    OptionTine,
    OptionDate,
    OptionTimestamp,
    OptionCalendar,
    OptionCalendarDate,
    OptionClass,
    OptionLocale,
    OptionCurrency,
    OptionTimezone,
    OptionUuidBinary,
    OptionUuidChar,
    OptionBinary,
    OptionWrapperBinary,
    OptionRowVersion,
    OptionImage,
    OptionBlob,
    OptionMaterializedBlob,
    OptionCharArray,
    OptionCharacterArray,
    OptionText,
    OptionNText,
    OptionClob,
    OptionNClob,
    OptionMaterializedClob,
    OptionMaterializedNClob,
    OptionSerializable,
  )
}
