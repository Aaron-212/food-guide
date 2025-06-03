package personal.aaron212.foodguide

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

@Composable
fun MarkdownRecipeView(
    recipeId: Int,
    navController: NavController
) {
    val context = LocalContext.current
    val dbHelper = DatabaseHelper.getInstance(context)
    val recipe = dbHelper.get1RecipeById(recipeId)

    MarkdownDisplayView(
        title = recipe?.name ?: "Recipe",
        markdownContent = recipe?.content ?: "Recipe content not found.",
        navController = navController
    )
}
