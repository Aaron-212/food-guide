package personal.aaron212.foodguide

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContentView() {
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    val navController = rememberNavController()

    Box(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        NavHost(
            navController,
            startDestination = Screen.Main.route,
        ) {
            composable(route = Screen.Main.route) {
                HomeView(
                    selectedTags = selectedTags,
                    onTagsChanged = { newTags -> selectedTags = newTags },
                    navController = navController
                )
            }

            composable(
                route = Screen.MarkdownDetail.route + "?id={id}",
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.IntType
                        nullable = false
                    }
                )) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id")
                requireNotNull(id) { "Recipe ID not found in arguments" }
                MarkdownView(recipeId = id, navController = navController)
            }
        }
    }
}

@Composable
fun HomeView(
    selectedTags: Set<String>,
    onTagsChanged: (Set<String>) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    var showingImagePicker by remember { mutableStateOf(false) }
    var inputImage by remember { mutableStateOf<Any?>(null) } // Placeholder for image
    var recipes by remember { mutableStateOf(listOf<Recipe>()) }
    var previousTagsWereEmpty by remember { mutableStateOf(true) }
    var selectedSearchMode by remember { mutableStateOf(SearchModes.FUZZY) }

    // Handle tag changes
    LaunchedEffect(selectedTags, selectedSearchMode) {
        val wasEmpty = previousTagsWereEmpty
        val isNowEmpty = selectedTags.isEmpty()

        // Fetch recipes based on selected tags using DatabaseHelper
        withContext(Dispatchers.IO) {
            val dbHelper = DatabaseHelper.getInstance(context)
            recipes = if (selectedTags.isEmpty()) {
                listOf() // Show no recipes when no tags are selected
            } else {
                when (selectedSearchMode) {
                    SearchModes.FUZZY -> dbHelper.queryRecipesByTagFuzzy(selectedTags.toTypedArray())
                    SearchModes.ACCURATE -> dbHelper.queryRecipesByTagAccurate(selectedTags.toTypedArray())
                    SearchModes.SURVIVAL -> dbHelper.queryRecipesByTagSurvival(selectedTags.toTypedArray())
                }
            }
        }

        previousTagsWereEmpty = isNowEmpty
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "好的，今天我们来做菜！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Commented out camera section (equivalent to Swift commented code)
        /*
        OutlinedButton(
            onClick = { showingImagePicker = !showingImagePicker },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("拍照识图")
        }
        */

        // MARK: — Ingredient Sections
        TagSelectionView(
            selectedTags = selectedTags,
            onTagsChanged = onTagsChanged
        )

        // MARK: — Matcher Card
        RecipeMatcherCard(
            selectedTags = selectedTags,
            recipes = recipes,
            onRecipesChange = { newRecipes -> recipes = newRecipes },
            selectedSearchMode = selectedSearchMode,
            onSearchModeChange = { newMode -> selectedSearchMode = newMode },
            navController = navController
        )
    }

    // Note: Image picker would be handled differently in Android
    // if (showingImagePicker) {
    //     // Image picker implementation
    // }
}

@Composable
fun RecipeMatcherCard(
    selectedTags: Set<String>,
    recipes: List<Recipe>,
    onRecipesChange: (List<Recipe>) -> Unit,
    selectedSearchMode: SearchModes,
    onSearchModeChange: (SearchModes) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchModes.entries.forEach { mode ->
                FilterChip(
                    selected = selectedSearchMode == mode,
                    onClick = { onSearchModeChange(mode) },
                    label = {
                        Text(
                            mode.displayName,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ShoppingBag,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "来看看组合出的菜谱吧！",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            // Icon for menu/filter could go here
            IconButton(
                onClick = {
                    if (recipes.isNotEmpty()) {
                        val randomRecipe = recipes.random()
                        if (randomRecipe.isVideo) {
                            val intent = Intent(Intent.ACTION_VIEW, randomRecipe.content.toUri())
                            context.startActivity(intent)
                        } else {
                            navController.navigate(route = Screen.MarkdownDetail.route + "?id=${randomRecipe.id}")
                        }
                    }
                },
                enabled = recipes.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Casino, // Or Shuffle, or another appropriate icon
                    contentDescription = "Random Recipe",
                    tint = if (recipes.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0f
                        ) // Hide icon by making it transparent
                    }
                )
            }
        }

        when {
            recipes.isEmpty() && selectedTags.isEmpty() -> {
                Text(
                    text = "你要先选食材或工具哦～",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            recipes.isEmpty() -> {
                Text(
                    text = "没有找到匹配的菜谱，试试别的组合吧～",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                // TagFlow equivalent - using FlowRow or custom implementation
                RecipeFlow(recipes = recipes, navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeFlow(recipes: List<Recipe>, navController: NavController) {
    val context = LocalContext.current

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        recipes.forEach { recipe ->
            if (recipe.isVideo) {
                AssistChip(
                    onClick = {
                        // Handle video URL opening
                        val intent = Intent(Intent.ACTION_VIEW, recipe.content.toUri())
                        context.startActivity(intent)
                    },
                    label = { Text(recipe.name) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                )
            } else {
                AssistChip(
                    onClick = {
                        navController.navigate(route = Screen.MarkdownDetail.route + "?id=${recipe.id}")
                    },
                    label = { Text(recipe.name) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Recipe",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                )
            }
        }
    }
}

enum class SearchModes(val displayName: String) {
    FUZZY("模糊匹配"),
    ACCURATE("严格匹配"),
    SURVIVAL("生存模式")
} 
