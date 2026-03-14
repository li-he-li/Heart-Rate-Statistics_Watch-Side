package com.heartrate.phone.`data`.persistence

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HeartRateDao_Impl(
  __db: RoomDatabase,
) : HeartRateDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfHeartRateEntity: EntityInsertAdapter<HeartRateEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfHeartRateEntity = object : EntityInsertAdapter<HeartRateEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `heart_rate_records` (`id`,`timestamp`,`heartRate`,`deviceId`,`batteryLevel`,`signalQuality`,`synced`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: HeartRateEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.timestamp)
        statement.bindLong(3, entity.heartRate.toLong())
        statement.bindText(4, entity.deviceId)
        val _tmpBatteryLevel: Int? = entity.batteryLevel
        if (_tmpBatteryLevel == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpBatteryLevel.toLong())
        }
        val _tmpSignalQuality: Int? = entity.signalQuality
        if (_tmpSignalQuality == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpSignalQuality.toLong())
        }
        val _tmp: Int = if (entity.synced) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindLong(8, entity.createdAt)
      }
    }
  }

  public override suspend fun insert(entity: HeartRateEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfHeartRateEntity.insertAndReturnId(_connection, entity)
    _result
  }

  public override suspend fun insertAll(entities: List<HeartRateEntity>): List<Long> =
      performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfHeartRateEntity.insertAndReturnIdsList(_connection,
        entities)
    _result
  }

  public override suspend fun getPending(limit: Int): List<HeartRateEntity> {
    val _sql: String = """
        |
        |        SELECT * FROM heart_rate_records
        |        WHERE synced = 0
        |        ORDER BY timestamp ASC
        |        LIMIT ?
        |        
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfHeartRate: Int = getColumnIndexOrThrow(_stmt, "heartRate")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfBatteryLevel: Int = getColumnIndexOrThrow(_stmt, "batteryLevel")
        val _columnIndexOfSignalQuality: Int = getColumnIndexOrThrow(_stmt, "signalQuality")
        val _columnIndexOfSynced: Int = getColumnIndexOrThrow(_stmt, "synced")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<HeartRateEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HeartRateEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpHeartRate: Int
          _tmpHeartRate = _stmt.getLong(_columnIndexOfHeartRate).toInt()
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpBatteryLevel: Int?
          if (_stmt.isNull(_columnIndexOfBatteryLevel)) {
            _tmpBatteryLevel = null
          } else {
            _tmpBatteryLevel = _stmt.getLong(_columnIndexOfBatteryLevel).toInt()
          }
          val _tmpSignalQuality: Int?
          if (_stmt.isNull(_columnIndexOfSignalQuality)) {
            _tmpSignalQuality = null
          } else {
            _tmpSignalQuality = _stmt.getLong(_columnIndexOfSignalQuality).toInt()
          }
          val _tmpSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSynced).toInt()
          _tmpSynced = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              HeartRateEntity(_tmpId,_tmpTimestamp,_tmpHeartRate,_tmpDeviceId,_tmpBatteryLevel,_tmpSignalQuality,_tmpSynced,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<HeartRateEntity> {
    val _sql: String = "SELECT * FROM heart_rate_records ORDER BY timestamp ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfHeartRate: Int = getColumnIndexOrThrow(_stmt, "heartRate")
        val _columnIndexOfDeviceId: Int = getColumnIndexOrThrow(_stmt, "deviceId")
        val _columnIndexOfBatteryLevel: Int = getColumnIndexOrThrow(_stmt, "batteryLevel")
        val _columnIndexOfSignalQuality: Int = getColumnIndexOrThrow(_stmt, "signalQuality")
        val _columnIndexOfSynced: Int = getColumnIndexOrThrow(_stmt, "synced")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<HeartRateEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HeartRateEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpHeartRate: Int
          _tmpHeartRate = _stmt.getLong(_columnIndexOfHeartRate).toInt()
          val _tmpDeviceId: String
          _tmpDeviceId = _stmt.getText(_columnIndexOfDeviceId)
          val _tmpBatteryLevel: Int?
          if (_stmt.isNull(_columnIndexOfBatteryLevel)) {
            _tmpBatteryLevel = null
          } else {
            _tmpBatteryLevel = _stmt.getLong(_columnIndexOfBatteryLevel).toInt()
          }
          val _tmpSignalQuality: Int?
          if (_stmt.isNull(_columnIndexOfSignalQuality)) {
            _tmpSignalQuality = null
          } else {
            _tmpSignalQuality = _stmt.getLong(_columnIndexOfSignalQuality).toInt()
          }
          val _tmpSynced: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfSynced).toInt()
          _tmpSynced = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              HeartRateEntity(_tmpId,_tmpTimestamp,_tmpHeartRate,_tmpDeviceId,_tmpBatteryLevel,_tmpSignalQuality,_tmpSynced,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markSynced(ids: List<Long>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("UPDATE heart_rate_records SET synced = 1 WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in ids) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
