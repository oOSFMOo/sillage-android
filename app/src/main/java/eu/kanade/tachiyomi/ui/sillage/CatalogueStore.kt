package eu.kanade.tachiyomi.ui.sillage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** The catalogue is independent of favourites and reading history. Each page is committed atomically. */
internal class CatalogueStore(context: Context) : SQLiteOpenHelper(context, "sillage-discovery.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE editions (id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE imports (source TEXT PRIMARY KEY, page INTEGER NOT NULL DEFAULT 1, complete INTEGER NOT NULL DEFAULT 0, message TEXT NOT NULL DEFAULT '', frontier TEXT NOT NULL DEFAULT '[]')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun save(series: List<CatalogueSeries>) {
        writableDatabase.beginTransaction()
        try {
            series.forEach { item ->
                val previous = writableDatabase.rawQuery("SELECT payload FROM editions WHERE id=?", arrayOf(item.key)).use { cursor ->
                    if (cursor.moveToFirst()) Json.decodeFromString<CatalogueSeries>(cursor.getString(0)) else null
                }
                writableDatabase.insertWithOnConflict("editions", null, ContentValues().apply {
                    put("id", item.key)
                    put("payload", Json.encodeToString(item.preservingKnownFields(previous)))
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    fun entries(): List<CatalogueSeries> = readableDatabase.rawQuery("SELECT payload FROM editions", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(Json.decodeFromString<CatalogueSeries>(cursor.getString(0))) }
    }

    fun state(source: Long): ImportState = readableDatabase.rawQuery(
        "SELECT page,complete,message,frontier FROM imports WHERE source=?", arrayOf(source.toString()),
    ).use { cursor ->
        if (cursor.moveToFirst()) ImportState(cursor.getInt(0), cursor.getInt(1) != 0, cursor.getString(2), Json.decodeFromString(cursor.getString(3)))
        else ImportState()
    }

    fun state(source: Long, state: ImportState) {
        writableDatabase.insertWithOnConflict("imports", null, ContentValues().apply {
            put("source", source.toString()); put("page", state.page); put("complete", if (state.complete) 1 else 0)
            put("message", state.message); put("frontier", Json.encodeToString(state.frontier))
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
}

internal data class ImportState(
    val page: Int = 1,
    val complete: Boolean = false,
    val message: String = "En attente",
    val frontier: List<String> = emptyList(),
)
