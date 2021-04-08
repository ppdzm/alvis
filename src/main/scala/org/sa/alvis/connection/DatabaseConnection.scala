package org.sa.alvis.connection

import java.sql.{Connection, DriverManager}
import java.util.Properties

import jline.console.completer.ArgumentCompleter
import org.sa.alvis.completer.SQLCompleter
import org.sa.utils.database.common.Drivers
import org.sa.utils.database.pool.jdbc.HikariConnectionPool
import org.sa.utils.universal.implicits.BasicConversions._

import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.util.matching.Regex

/**
 * Created by Stuart Alex on 2017/9/8.
 */
class DatabaseConnection(fullUrl: String, val properties: Properties) {
    lazy val tableCompleter: ArgumentCompleter = {
        val completer = new ArgumentCompleter(this.delimiter, SQLCompleter(this.tables))
        completer.setStrict(false)
        completer
    }
    lazy val columnCompleter: ArgumentCompleter = {
        val completer = new ArgumentCompleter(this.delimiter, SQLCompleter(this.columnCompletions))
        completer.setStrict(false)
        completer
    }
    private lazy val metaData = this.connection.getMetaData
    private lazy val extraNameCharacters = if (this.metaData.isNull || this.metaData.getExtraNameCharacters.isNull)
        ""
    else
        this.metaData.getExtraNameCharacters
    private lazy val delimiter = new ArgumentCompleter.AbstractArgumentDelimiter() {
        def isDelimiterChar(buffer: CharSequence, pos: Int): Boolean = {
            val c = buffer.charAt(pos)
            if (Character.isWhitespace(c))
                true
            else
                !Character.isLetterOrDigit(c) && c != '_' && extraNameCharacters.indexOf(c) == -1
        }
    }
    val m: Option[Regex.Match] = "(.*?):(.*?)://(.*?)/(.*?)$".r.findFirstMatchIn(fullUrl)
    val url: String = if (this.fullUrl.contains("?"))
        this.fullUrl.substring(0, this.fullUrl.indexOf("?")).replace(this.currentDatabase, "")
    else
        this.fullUrl.replace(this.currentDatabase, "")
    var currentDatabase: String = if (m.isDefined && m.get.group(4).nonEmpty)
        m.get.group(4)
    else
        ""
    var connection: Connection = if (fullUrl.contains("presto"))
        HikariConnectionPool(jdbcUrl = fullUrl, driver = Drivers.PRESTO.toString, username = "presto").borrow()
    else
        DriverManager.getConnection(url, properties)

    def isClosed: Boolean = this.connection.isNull || this.connection.isClosed

    override def toString: String = this.url

    def status: String = if (this.connection.isClosed) "closed" else "open"

    def tables: List[String] = {
        val rs = this.connection.createStatement().executeQuery("show tables")
        val tables = ListBuffer[String]()
        while (rs.next())
            tables += rs.getString(1)
        tables.toList
    }

    def columns(tableName: String): List[String] = {
        val columns = ListBuffer[String]()
        val rs = this.metaData.getColumns(this.connection.getCatalog, null, tableName, "%")
        while (rs.next())
            columns += rs.getString("COLUMN_NAME")
        columns.toList
    }

    def functions: List[String] = {
        val functions = ListBuffer[String]()
        //for example: ACCESSIBLE,ADD,ANALYZE,ASC,BEFORE,CASCADE,CHANGE,CONTINUE,DATABASE,DATABASES,DAY_HOUR,DAY_MICROSECOND,DAY_MINUTE,DAY_SECOND,DELAYED,DESC,DISTINCTROW,DIV,DUAL,ELSEIF,ENCLOSED,ESCAPED,EXIT,EXPLAIN,FLOAT4,FLOAT8,FORCE,FULLTEXT,GENERATED,HIGH_PRIORITY,HOUR_MICROSECOND,HOUR_MINUTE,HOUR_SECOND,IF,IGNORE,INDEX,INFILE,INT1,INT2,INT3,INT4,INT8,IO_AFTER_GTIDS,IO_BEFORE_GTIDS,ITERATE,KEY,KEYS,KILL,LEAVE,LIMIT,LINEAR,LINES,LOAD,LOCK,LONG,LONGBLOB,LONGTEXT,LOOP,LOW_PRIORITY,MASTER_BIND,MASTER_SSL_VERIFY_SERVER_CERT,MAXVALUE,MEDIUMBLOB,MEDIUMINT,MEDIUMTEXT,MIDDLEINT,MINUTE_MICROSECOND,MINUTE_SECOND,NO_WRITE_TO_BINLOG,OPTIMIZE,OPTIMIZER_COSTS,OPTION,OPTIONALLY,OUTFILE,PURGE,READ,READ_WRITE,REGEXP,RENAME,REPEAT,REPLACE,REQUIRE,RESIGNAL,RESTRICT,RLIKE,SCHEMA,SCHEMAS,SECOND_MICROSECOND,SEPARATOR,SHOW,SIGNAL,SPATIAL,SQL_BIG_RESULT,SQL_CALC_FOUND_ROWS,SQL_SMALL_RESULT,SSL,STARTING,STORED,STRAIGHT_JOIN,TERMINATED,TINYBLOB,TINYINT,TINYTEXT,UNDO,UNLOCK,UNSIGNED,USAGE,USE,UTC_DATE,UTC_TIME,UTC_TIMESTAMP,VARBINARY,VARCHARACTER,VIRTUAL,WHILE,WRITE,XOR,YEAR_MONTH,ZEROFILL
        //Try(keywords += "," + DatabaseConnections.current.meta.getSQLKeywords)
        //for example: ASCII,BIN,BIT_LENGTH,CHAR,CHARACTER_LENGTH,CHAR_LENGTH,CONCAT,CONCAT_WS,CONV,ELT,EXPORT_SET,FIELD,FIND_IN_SET,HEX,INSERT,INSTR,LCASE,LEFT,LENGTH,LOAD_FILE,LOCATE,LOCATE,LOWER,LPAD,LTRIM,MAKE_SET,MATCH,MID,OCT,OCTET_LENGTH,ORD,POSITION,QUOTE,REPEAT,REPLACE,REVERSE,RIGHT,RPAD,RTRIM,SOUNDEX,SPACE,STRCMP,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING_INDEX,TRIM,UCASE,UPPER
        Try(functions += this.metaData.getStringFunctions.toLowerCase)
        //for example: ABS,ACOS,ASIN,ATAN,ATAN2,BIT_COUNT,CEILING,COS,COT,DEGREES,EXP,FLOOR,LOG,LOG10,MAX,MIN,MOD,PI,POW,POWER,RADIANS,RAND,ROUND,SIN,SQRT,TAN,TRUNCATE
        Try(functions += this.metaData.getNumericFunctions.toLowerCase)
        //for example: DATABASE,USER,SYSTEM_USER,SESSION_USER,PASSWORD,ENCRYPT,LAST_INSERT_ID,VERSION
        //Try(keywords += "," + DatabaseConnections.current.meta.getSystemFunctions)
        //for example: DAYOFWEEK,WEEKDAY,DAYOFMONTH,DAYOFYEAR,MONTH,DAYNAME,MONTHNAME,QUARTER,WEEK,YEAR,HOUR,MINUTE,SECOND,PERIOD_ADD,PERIOD_DIFF,TO_DAYS,FROM_DAYS,DATE_FORMAT,TIME_FORMAT,CURDATE,CURRENT_DATE,CURTIME,CURRENT_TIME,NOW,SYSDATE,CURRENT_TIMESTAMP,UNIX_TIMESTAMP,FROM_UNIXTIME,SEC_TO_TIME,TIME_TO_SEC
        Try(functions += this.metaData.getTimeDateFunctions.toLowerCase)
        functions.mkString(",").split(",").sorted.toList
    }

    private def columnCompletions = {
        val completions = ListBuffer[String]()
        Try {
            val rs = this.metaData.getColumns(this.connection.getCatalog, null, "%", "%")
            while (rs.next()) {
                completions += rs.getString("COLUMN_NAME")
                completions += rs.getString("TABLE_NAME") + "." + rs.getString("COLUMN_NAME")
            }
        }
        completions.toList
    }

}
