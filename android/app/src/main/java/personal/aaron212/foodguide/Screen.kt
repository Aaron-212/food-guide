package personal.aaron212.foodguide

sealed class Screen(val route: String) {
    data object Main : Screen("main_screen")
    data object MarkdownDetail : Screen("detail_screen")
}