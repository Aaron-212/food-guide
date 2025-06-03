package personal.aaron212.foodguide

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.FileOutputStream

data class Recipe(
    val id: Int,
    val name: String,
    val content: String,
    val isVideo: Boolean,
)

data class RecipeTag(
    val recipeId: Int,
    val tag: String
)

class DatabaseHelper private constructor(private val db: SQLiteDatabase) {
    // Query Functions
    fun getAllRecipes(): List<Recipe> {
        val list = mutableListOf<Recipe>()
        val c: Cursor = db.query(
            "Recipe",
            null, null, null, null, null, null
        )
        c.use {
            while (it.moveToNext()) {
                val recipe = Recipe(
                    id = it.getInt(0),
                    name = it.getString(1),
                    content = it.getString(2),
                    isVideo = it.getInt(3) == 1,
                )
                list += recipe
            }
        }
        return list
    }

    fun queryRecipesByTagFuzzy(tags: Array<String>): List<Recipe> {
        // Fuzzy: if a recipe contains one of the tags provided, add it to the result
        val result = mutableListOf<Recipe>()
        if (tags.isEmpty()) return result

        val tagsString = tags.joinToString { "'$it'" }

        val cursor: Cursor = db.query(
            "Recipe AS r JOIN RecipeTag AS t ON t.recipe_id = r.id",
            null,
            "t.tag IN ($tagsString)",
            null,
            "r.id",
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                result += Recipe(
                    id = it.getInt(0),
                    name = it.getString(1),
                    content = it.getString(2),
                    isVideo = it.getInt(3) == 1
                )
            }
        }
        return result
    }

    fun queryRecipesByTagAccurate(tags: Array<String>): List<Recipe> {
        // Accurate: if a recipe contains all of the tags provided, add it to the result
        val result = mutableListOf<Recipe>()
        if (tags.isEmpty()) return result

        val tagsString = tags.joinToString { "'$it'" }

        val c: Cursor = db.query(
            "Recipe AS r JOIN RecipeTag AS t ON t.recipe_id = r.id",
            null,
            "t.tag IN ($tagsString)", null,
            "r.id",
            "COUNT(DISTINCT t.tag) = ${tags.size}",
            null
        )
        c.use {
            while (it.moveToNext()) {
                val recipe = Recipe(
                    id = it.getInt(0),
                    name = it.getString(1),
                    content = it.getString(2),
                    isVideo = it.getInt(3) == 1,
                )
                result += recipe
            }
        }
        return result
    }

    fun queryRecipesByTagSurvival(tags: Array<String>): List<Recipe> {
        // Survival: if a recipe contains one of the tags provided plus no extra tags, add it to the result
        val result = mutableListOf<Recipe>()
        if (tags.isEmpty()) return result

        val tagsString = tags.joinToString { "'$it'" }

        // We need to use rawQuery for this more complex query
        val query = """
            SELECT DISTINCT r.id, r.name, r.content, r.is_video
            FROM Recipe r
            WHERE r.id IN (
                SELECT recipe_id FROM RecipeTag WHERE tag IN ($tagsString)
            )
            AND NOT EXISTS (
                SELECT 1 FROM RecipeTag 
                WHERE recipe_id = r.id AND tag NOT IN ($tagsString)
            )
        """.trimIndent()

        val cursor: Cursor = db.rawQuery(query, null)

        cursor.use {
            while (it.moveToNext()) {
                result += Recipe(
                    id = it.getInt(0),
                    name = it.getString(1),
                    content = it.getString(2),
                    isVideo = it.getInt(3) == 1
                )
            }
        }
        return result
    }

    fun get1RecipeById(id: Int): Recipe? {
        val cursor: Cursor = db.query(
            "Recipe",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
        )

        cursor.use {
            if (it.moveToFirst()) {
                return Recipe(
                    id = it.getInt(0),
                    name = it.getString(1),
                    content = it.getString(2),
                    isVideo = it.getInt(3) == 1
                )
            }
        }
        return null
    }

    fun close() = db.close()

    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null
        private const val DB_NAME = "food_guide_recipes.db"

        fun getInstance(ctx: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: createFromAsset(ctx.applicationContext).also { instance = it }
            }

        // 1. copy from assets once; 2. open database readonly
        private fun createFromAsset(ctx: Context): DatabaseHelper {
            val dbPath = ctx.getDatabasePath(DB_NAME)
            // first launch -> copy
            dbPath.parentFile?.mkdirs()
            ctx.assets.open(DB_NAME).use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("DatabaseHelper", "Database copied to ${dbPath.absolutePath}")
            // Could also use OPEN_READWRITE if you plan to insert/update
            val sqliteDb = SQLiteDatabase.openDatabase(
                dbPath.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            return DatabaseHelper(sqliteDb)
        }
    }
}
