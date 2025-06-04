package personal.aaron212.foodguide

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentView() {
    var tagStates by remember { mutableStateOf(mapOf<String, TagState>()) }
    val navController = rememberNavController()
    val context = LocalContext.current

    var recipes by remember { mutableStateOf(listOf<Recipe>()) }
    var selectedSearchMode by remember { mutableStateOf(SearchModes.FUZZY) }
    var dropdownExpanded by remember { mutableStateOf(false) } // State for DropdownMenu

    // Handle tag changes and fetch recipes
    LaunchedEffect(tagStates, selectedSearchMode) {
        val wantedTags = tagStates.filter { it.value == TagState.WANTED }.keys
        val unwantedTags = tagStates.filter { it.value == TagState.UNWANTED }.keys

        // Fetch recipes based on selected tags using DatabaseHelper
        withContext(Dispatchers.IO) {
            val dbHelper = DatabaseHelper.getInstance(context)
            recipes =
                if (wantedTags.isEmpty() && selectedSearchMode != SearchModes.SURVIVAL) {
                    listOf()
                } else {
                    when (selectedSearchMode) {
                        SearchModes.FUZZY -> dbHelper.queryRecipesByTagFuzzy(
                            wantedTags.toTypedArray(),
                            unwantedTags.toTypedArray()
                        )

                        SearchModes.ACCURATE -> dbHelper.queryRecipesByTagAccurate(
                            wantedTags.toTypedArray(),
                            unwantedTags.toTypedArray()
                        )

                        SearchModes.SURVIVAL ->
                            dbHelper.queryRecipesByTagSurvival(wantedTags.toTypedArray())

                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // Ensure Box fills the screen to position toolbar correctly
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        NavHost(
            navController,
            startDestination = Screen.Main.route,
            popExitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
                ) + fadeOut()
            },
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
                ) + fadeIn()
            }
        ) {
            composable(route = Screen.Main.route) {
                HomeView(
                    tagStates = tagStates,
                    onTagStatesChanged = { newTagStates -> tagStates = newTagStates },
                    navController = navController,
                    recipes = recipes
                )
            }

            composable(
                route = Screen.MarkdownDetail.route + "?id={id}",
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.IntType
                        nullable = false
                    }
                ),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id")
                requireNotNull(id) { "Recipe ID not found in arguments" }
                MarkdownRecipeView(recipeId = id, navController = navController)
            }

            composable(
                route = Screen.Help.route,
            ) {
                HelpView(navController = navController)
            }
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        tagStates = mapOf()
                        Toast.makeText(
                            context,
                            "清空了所有标签",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    },
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear Selected Tags")
                }
            }
        ) {
            // Shuffle Button
            IconButton(
                onClick = {
                    val randomRecipe = if (recipes.isNotEmpty()) {
                        recipes.random()
                    } else {
                        val dbHelper = DatabaseHelper.getInstance(context)
                        dbHelper.getAllRecipes().random()
                    }

                    Toast.makeText(
                        context,
                        "随机菜谱：${randomRecipe.name}",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    if (randomRecipe.isVideo) {
                        val intent = Intent(Intent.ACTION_VIEW, randomRecipe.content.toUri())
                        context.startActivity(intent)
                    } else {
                        navController.navigate(Screen.MarkdownDetail.route + "?id=${randomRecipe.id}")
                    }
                },
            ) {
                Icon(Icons.Filled.Casino, contentDescription = "Shuffle Random Recipe")
            }

            // Help Button
            IconButton(
                onClick = {
                    navController.navigate(Screen.Help.route)
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help")
            }

            // Search Mode Dropdown
            Box {
                TextButton(onClick = { dropdownExpanded = true }) {
                    Text(selectedSearchMode.displayName)
                    Icon(
                        if (dropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Search Mode"
                    )
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    SearchModes.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName) },
                            onClick = {
                                selectedSearchMode = mode
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeView(
    tagStates: Map<String, TagState>,
    onTagStatesChanged: (Map<String, TagState>) -> Unit,
    navController: NavController,
    recipes: List<Recipe>
) {
    // val context = LocalContext.current
    // var showingImagePicker by remember { mutableStateOf(false) }
    // var inputImage by remember { mutableStateOf<Any?>(null) } // Placeholder for image

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
            initialTagStates = tagStates,
            onTagStatesChanged = onTagStatesChanged
        )

        // MARK: — Matcher Card
        RecipeMatcherCard(
            tagStates = tagStates,
            recipes = recipes,
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
    tagStates: Map<String, TagState>,
    recipes: List<Recipe>,
    navController: NavController
) {
    Column(
        modifier = Modifier.padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
        }

        when {
            tagStates.filter { it.value == TagState.WANTED }.isEmpty() -> {
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
                        )
                    },
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
                        )
                    },
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
