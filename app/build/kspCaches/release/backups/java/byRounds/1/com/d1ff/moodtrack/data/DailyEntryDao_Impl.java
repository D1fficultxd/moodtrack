package com.d1ff.moodtrack.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DailyEntryDao_Impl implements DailyEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyEntry> __insertionAdapterOfDailyEntry;

  private final EntityDeletionOrUpdateAdapter<DailyEntry> __updateAdapterOfDailyEntry;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public DailyEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyEntry = new EntityInsertionAdapter<DailyEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_entries` (`id`,`date`,`sleepHours`,`sleepEase`,`anxiety`,`irritability`,`impulsivity`,`racingThoughts`,`suicidalThoughts`,`selfHarm`,`mood`,`apathy`,`fatigue`,`lossOfInterest`,`hopelessness`,`note`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindDouble(3, entity.getSleepHours());
        statement.bindLong(4, entity.getSleepEase());
        statement.bindLong(5, entity.getAnxiety());
        statement.bindLong(6, entity.getIrritability());
        statement.bindLong(7, entity.getImpulsivity());
        statement.bindLong(8, entity.getRacingThoughts());
        statement.bindLong(9, entity.getSuicidalThoughts());
        final int _tmp = entity.getSelfHarm() ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.getMood());
        statement.bindLong(12, entity.getApathy());
        statement.bindLong(13, entity.getFatigue());
        statement.bindLong(14, entity.getLossOfInterest());
        statement.bindLong(15, entity.getHopelessness());
        statement.bindString(16, entity.getNote());
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
      }
    };
    this.__updateAdapterOfDailyEntry = new EntityDeletionOrUpdateAdapter<DailyEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `daily_entries` SET `id` = ?,`date` = ?,`sleepHours` = ?,`sleepEase` = ?,`anxiety` = ?,`irritability` = ?,`impulsivity` = ?,`racingThoughts` = ?,`suicidalThoughts` = ?,`selfHarm` = ?,`mood` = ?,`apathy` = ?,`fatigue` = ?,`lossOfInterest` = ?,`hopelessness` = ?,`note` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindDouble(3, entity.getSleepHours());
        statement.bindLong(4, entity.getSleepEase());
        statement.bindLong(5, entity.getAnxiety());
        statement.bindLong(6, entity.getIrritability());
        statement.bindLong(7, entity.getImpulsivity());
        statement.bindLong(8, entity.getRacingThoughts());
        statement.bindLong(9, entity.getSuicidalThoughts());
        final int _tmp = entity.getSelfHarm() ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.getMood());
        statement.bindLong(12, entity.getApathy());
        statement.bindLong(13, entity.getFatigue());
        statement.bindLong(14, entity.getLossOfInterest());
        statement.bindLong(15, entity.getHopelessness());
        statement.bindString(16, entity.getNote());
        statement.bindLong(17, entity.getCreatedAt());
        statement.bindLong(18, entity.getUpdatedAt());
        statement.bindLong(19, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_entries WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_entries";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final DailyEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyEntry.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DailyEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDailyEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DailyEntry>> getAllEntries() {
    final String _sql = "SELECT * FROM daily_entries ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_entries"}, new Callable<List<DailyEntry>>() {
      @Override
      @NonNull
      public List<DailyEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSleepHours = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepHours");
          final int _cursorIndexOfSleepEase = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepEase");
          final int _cursorIndexOfAnxiety = CursorUtil.getColumnIndexOrThrow(_cursor, "anxiety");
          final int _cursorIndexOfIrritability = CursorUtil.getColumnIndexOrThrow(_cursor, "irritability");
          final int _cursorIndexOfImpulsivity = CursorUtil.getColumnIndexOrThrow(_cursor, "impulsivity");
          final int _cursorIndexOfRacingThoughts = CursorUtil.getColumnIndexOrThrow(_cursor, "racingThoughts");
          final int _cursorIndexOfSuicidalThoughts = CursorUtil.getColumnIndexOrThrow(_cursor, "suicidalThoughts");
          final int _cursorIndexOfSelfHarm = CursorUtil.getColumnIndexOrThrow(_cursor, "selfHarm");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfApathy = CursorUtil.getColumnIndexOrThrow(_cursor, "apathy");
          final int _cursorIndexOfFatigue = CursorUtil.getColumnIndexOrThrow(_cursor, "fatigue");
          final int _cursorIndexOfLossOfInterest = CursorUtil.getColumnIndexOrThrow(_cursor, "lossOfInterest");
          final int _cursorIndexOfHopelessness = CursorUtil.getColumnIndexOrThrow(_cursor, "hopelessness");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<DailyEntry> _result = new ArrayList<DailyEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final float _tmpSleepHours;
            _tmpSleepHours = _cursor.getFloat(_cursorIndexOfSleepHours);
            final int _tmpSleepEase;
            _tmpSleepEase = _cursor.getInt(_cursorIndexOfSleepEase);
            final int _tmpAnxiety;
            _tmpAnxiety = _cursor.getInt(_cursorIndexOfAnxiety);
            final int _tmpIrritability;
            _tmpIrritability = _cursor.getInt(_cursorIndexOfIrritability);
            final int _tmpImpulsivity;
            _tmpImpulsivity = _cursor.getInt(_cursorIndexOfImpulsivity);
            final int _tmpRacingThoughts;
            _tmpRacingThoughts = _cursor.getInt(_cursorIndexOfRacingThoughts);
            final int _tmpSuicidalThoughts;
            _tmpSuicidalThoughts = _cursor.getInt(_cursorIndexOfSuicidalThoughts);
            final boolean _tmpSelfHarm;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSelfHarm);
            _tmpSelfHarm = _tmp != 0;
            final int _tmpMood;
            _tmpMood = _cursor.getInt(_cursorIndexOfMood);
            final int _tmpApathy;
            _tmpApathy = _cursor.getInt(_cursorIndexOfApathy);
            final int _tmpFatigue;
            _tmpFatigue = _cursor.getInt(_cursorIndexOfFatigue);
            final int _tmpLossOfInterest;
            _tmpLossOfInterest = _cursor.getInt(_cursorIndexOfLossOfInterest);
            final int _tmpHopelessness;
            _tmpHopelessness = _cursor.getInt(_cursorIndexOfHopelessness);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new DailyEntry(_tmpId,_tmpDate,_tmpSleepHours,_tmpSleepEase,_tmpAnxiety,_tmpIrritability,_tmpImpulsivity,_tmpRacingThoughts,_tmpSuicidalThoughts,_tmpSelfHarm,_tmpMood,_tmpApathy,_tmpFatigue,_tmpLossOfInterest,_tmpHopelessness,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getEntryByDate(final String date,
      final Continuation<? super DailyEntry> $completion) {
    final String _sql = "SELECT * FROM daily_entries WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailyEntry>() {
      @Override
      @Nullable
      public DailyEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSleepHours = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepHours");
          final int _cursorIndexOfSleepEase = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepEase");
          final int _cursorIndexOfAnxiety = CursorUtil.getColumnIndexOrThrow(_cursor, "anxiety");
          final int _cursorIndexOfIrritability = CursorUtil.getColumnIndexOrThrow(_cursor, "irritability");
          final int _cursorIndexOfImpulsivity = CursorUtil.getColumnIndexOrThrow(_cursor, "impulsivity");
          final int _cursorIndexOfRacingThoughts = CursorUtil.getColumnIndexOrThrow(_cursor, "racingThoughts");
          final int _cursorIndexOfSuicidalThoughts = CursorUtil.getColumnIndexOrThrow(_cursor, "suicidalThoughts");
          final int _cursorIndexOfSelfHarm = CursorUtil.getColumnIndexOrThrow(_cursor, "selfHarm");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfApathy = CursorUtil.getColumnIndexOrThrow(_cursor, "apathy");
          final int _cursorIndexOfFatigue = CursorUtil.getColumnIndexOrThrow(_cursor, "fatigue");
          final int _cursorIndexOfLossOfInterest = CursorUtil.getColumnIndexOrThrow(_cursor, "lossOfInterest");
          final int _cursorIndexOfHopelessness = CursorUtil.getColumnIndexOrThrow(_cursor, "hopelessness");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final DailyEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final float _tmpSleepHours;
            _tmpSleepHours = _cursor.getFloat(_cursorIndexOfSleepHours);
            final int _tmpSleepEase;
            _tmpSleepEase = _cursor.getInt(_cursorIndexOfSleepEase);
            final int _tmpAnxiety;
            _tmpAnxiety = _cursor.getInt(_cursorIndexOfAnxiety);
            final int _tmpIrritability;
            _tmpIrritability = _cursor.getInt(_cursorIndexOfIrritability);
            final int _tmpImpulsivity;
            _tmpImpulsivity = _cursor.getInt(_cursorIndexOfImpulsivity);
            final int _tmpRacingThoughts;
            _tmpRacingThoughts = _cursor.getInt(_cursorIndexOfRacingThoughts);
            final int _tmpSuicidalThoughts;
            _tmpSuicidalThoughts = _cursor.getInt(_cursorIndexOfSuicidalThoughts);
            final boolean _tmpSelfHarm;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSelfHarm);
            _tmpSelfHarm = _tmp != 0;
            final int _tmpMood;
            _tmpMood = _cursor.getInt(_cursorIndexOfMood);
            final int _tmpApathy;
            _tmpApathy = _cursor.getInt(_cursorIndexOfApathy);
            final int _tmpFatigue;
            _tmpFatigue = _cursor.getInt(_cursorIndexOfFatigue);
            final int _tmpLossOfInterest;
            _tmpLossOfInterest = _cursor.getInt(_cursorIndexOfLossOfInterest);
            final int _tmpHopelessness;
            _tmpHopelessness = _cursor.getInt(_cursorIndexOfHopelessness);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new DailyEntry(_tmpId,_tmpDate,_tmpSleepHours,_tmpSleepEase,_tmpAnxiety,_tmpIrritability,_tmpImpulsivity,_tmpRacingThoughts,_tmpSuicidalThoughts,_tmpSelfHarm,_tmpMood,_tmpApathy,_tmpFatigue,_tmpLossOfInterest,_tmpHopelessness,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
