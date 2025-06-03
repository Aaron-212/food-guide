package personal.aaron212.foodguide

sealed class Screen(val route: String) {
    object Main: Screen("main_screen")
    object MarkdownDetail: Screen("detail_screen")
}