import MarkdownUI
import SwiftUI

struct RecipeDetailView: View {
    let recipe: Recipe

    var body: some View {
        ScrollView {
            Markdown(recipe.content)
                .padding()
        }
        .navigationTitle(recipe.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

// A preview for RecipeDetailView, assuming a sample recipe.
#Preview {
    RecipeDetailView(
        recipe: Recipe(
            id: 1,
            name: "Sample Recipe",
            content: """
                # Ingredients
                - Item 1
                - Item 2
                - Item 3

                # Instructions
                1. Do this.
                2. Then do that.
                3. Finally, serve.
                """,
            isVideo: false
        )
    )
}
