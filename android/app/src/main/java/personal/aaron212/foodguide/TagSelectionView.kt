package personal.aaron212.foodguide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

// Data class for tag categories
data class SelectableTagCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tags: List<String>,
    val tintColor: Color,
    val titleDisplay: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelectionView(
    selectedTags: Set<String>,
    onTagsChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }

    val tagCategories = remember {
        listOf(
            SelectableTagCategory(
                name = "蔬菜",
                tags = listOf(
                    "土豆", "胡萝卜", "花菜", "白萝卜", "西葫芦", "番茄", "芹菜",
                    "黄瓜", "洋葱", "蘑菇", "茄子", "豆腐", "包菜", "白菜"
                ),
                tintColor = Color(0xFF4CAF50), // Green
                titleDisplay = "🥬 菜菜们"
            ),
            SelectableTagCategory(
                name = "肉类",
                tags = listOf(
                    "腊肠", "猪肉", "鸡蛋", "牛肉", "鸭肉", "鸡腿",
                    "鱼", "午餐肉", "鸡肉", "虾"
                ),
                tintColor = Color(0xFFE91E63), // Pink
                titleDisplay = "🥩 肉肉们"
            ),
            SelectableTagCategory(
                name = "主食",
                tags = listOf("面食", "面包", "米", "方便面"),
                tintColor = Color(0xFFFFC107), // Yellow/Amber
                titleDisplay = "🍚 主食也要一起下锅吗？"
            ),
            SelectableTagCategory(
                name = "其他",
                tags = listOf(
                    "烤箱", "空气炸锅", "微波炉", "盐", "白砂糖",
                    "醋", "葱", "姜", "蒜"
                ),
                tintColor = Color(0xFF9E9E9E), // Gray
                titleDisplay = "🔍 其他的标签"
            )
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        tagCategories.forEach { category ->
            TagCategorySection(
                category = category,
                isExpanded = expandedCategories.contains(category.id),
                selectedTags = selectedTags,
                onExpandToggle = {
                    expandedCategories = if (expandedCategories.contains(category.id)) {
                        expandedCategories - category.id
                    } else {
                        expandedCategories + category.id
                    }
                },
                onTagToggle = { tag ->
                    val newTags = if (selectedTags.contains(tag)) {
                        selectedTags - tag
                    } else {
                        selectedTags + tag
                    }
                    onTagsChanged(newTags)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCategorySection(
    category: SelectableTagCategory,
    isExpanded: Boolean,
    selectedTags: Set<String>,
    onExpandToggle: () -> Unit,
    onTagToggle: (String) -> Unit
) {
    Column {
        // Category header
        TextButton(
            onClick = onExpandToggle,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.titleDisplay,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = category.tintColor,
                    modifier = Modifier.rotate(
                        animateFloatAsState(
                            targetValue = if (isExpanded) 90f else 0f,
                            animationSpec = tween(300)
                        ).value
                    )
                )
            }
        }

        // Tags flow layout with animation
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(300),
                expandFrom = Alignment.Top
            ) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(
                animationSpec = tween(300),
                shrinkTowards = Alignment.Top
            ) + fadeOut(animationSpec = tween(300))
        ) {
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                category.tags.forEach { tag ->
                    TagChip(
                        tag = tag,
                        isSelected = selectedTags.contains(tag),
                        categoryColor = category.tintColor,
                        onClick = { onTagToggle(tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: String,
    isSelected: Boolean,
    categoryColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(tag) },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = categoryColor.copy(alpha = 0.2f),
            labelColor = categoryColor.copy(alpha = 0.9f),
            selectedContainerColor = categoryColor,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = categoryColor.copy(alpha = 0.5f),
            selectedBorderColor = categoryColor,
            selected = isSelected,
            enabled = true,
        ),
        modifier = Modifier.padding()
    )
} 
