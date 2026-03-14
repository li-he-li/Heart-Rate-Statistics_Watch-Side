package com.heartrate.phone.`data`.persistence

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HeartRateDatabase_Impl : HeartRateDatabase() {
  private val _heartRateDao: Lazy<HeartRateDao> = lazy {
    HeartRateDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "a73011ccc7d04bebb3a450c382919aac", "0f6700241da097eff3123446883df6a5") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `heart_rate_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `heartRate` INTEGER NOT NULL, `deviceId` TEXT NOT NULL, `batteryLevel` INTEGER, `signalQuality` INTEGER, `synced` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a73011ccc7d04bebb3a450c382919aac')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `heart_rate_records`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsHeartRateRecords: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsHeartRateRecords.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHeartRateRecords.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHeartRateRecords.put("heartRate", TableInfo.Column("heartRate", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHeartRateRecords.put("deviceId", TableInfo.Column("deviceId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHeartRateRecords.put("batteryLevel", TableInfo.Column("batteryLevel", "INTEGER",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHeartRateRecords.put("signalQuality", TableInfo.Column("signalQuality", "INTEGER",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHeartRateRecords.put("synced", TableInfo.Column("synced", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHeartRateRecords.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysHeartRateRecords: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesHeartRateRecords: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoHeartRateRecords: TableInfo = TableInfo("heart_rate_records",
            _columnsHeartRateRecords, _foreignKeysHeartRateRecords, _indicesHeartRateRecords)
        val _existingHeartRateRecords: TableInfo = read(connection, "heart_rate_records")
        if (!_infoHeartRateRecords.equals(_existingHeartRateRecords)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |heart_rate_records(com.heartrate.phone.data.persistence.HeartRateEntity).
              | Expected:
              |""".trimMargin() + _infoHeartRateRecords + """
              |
              | Found:
              |""".trimMargin() + _existingHeartRateRecords)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "heart_rate_records")
  }

  public override fun clearAllTables() {
    super.performClear(false, "heart_rate_records")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(HeartRateDao::class, HeartRateDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun heartRateDao(): HeartRateDao = _heartRateDao.value
}
