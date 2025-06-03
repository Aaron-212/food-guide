package personal.aaron212.foodguide

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun HelpView(navController: NavController) {
    val helpMarkdown = """
# 食尚指南

## 这是什么？

食尚指南是一款帮助你找到适合你当前拥有的食材的菜谱的工具。

## 功能

### 标签搜索

点击对应的栏目，展开后选择你拥有的食材。

对于未选中的食材，点击后会变成填充色，表示你想要这个食材。
对于已经选中的食材，再次点击会划掉，表示你不想要这个食材。
再次点击会取消选中。

### 搜索模式

- 模糊匹配：展示所有含当前选中任意食材的菜谱
- 精准匹配：展示所有含当前选中所有食材的菜谱
- 生存模式：展示当前选中食材即可制作的所有菜谱

### 随机菜谱

在选择完标签后，点击随机菜谱按钮，会随机展示一个菜谱。

### 清空标签

点击清空标签按钮，会清空所有选中的标签。

## 如何使用？

1. 选择你拥有的食材，或者不想要的食材。
2. 选择搜索模式。
3. 下方即可展示符合条件的菜谱。
4. 点击随机菜谱按钮，会随机进入一个菜谱。

> © [Aaron212](https://github.com/Aaron-212) under MIT License 2025
>
> Source code: [GitHub](https://github.com/Aaron-212/food-guide)
    """

    MarkdownDisplayView(
        title = "帮助和关于",
        markdownContent = helpMarkdown,
        navController = navController
    )
} 
