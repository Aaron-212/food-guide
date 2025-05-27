import SwiftUI

struct ContentView: View {
    var body: some View {
        GeometryReader { proxy in
            HomeView()
            VariableBlurView(maxBlurRadius: 10)
                .frame(height: proxy.safeAreaInsets.top)
                .ignoresSafeArea()
        }

    }
}

struct HomeView: View {
    @State private var showingImagePicker = false
    @State private var inputImage: UIImage?  // Holds the image returned from the picker
    @State private var selectedTags: Set<String> = []
    @State private var recipes: [Recipe] = []
    @State private var previousTagsWereEmpty = true

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 32) {
                    Text("好的，今天我们来做菜！")
                        .font(.title)
                        .bold()

                    // Button(action: {
                    //     showingImagePicker.toggle()
                    // }) {
                    //     Label("拍照识图", systemImage: "magnifyingglass")
                    //         .frame(maxWidth: .infinity)
                    //         .padding(8)
                    // }
                    // .buttonStyle(.bordered)
                    // .tint(.blue)
                    // .overlay(
                    //     RoundedRectangle(cornerRadius: 12)
                    //         .stroke(.blue.opacity(0.45), lineWidth: 1)
                    // )

                    // MARK: — Ingredient Sections
                    TagSelectionView(selectedTags: $selectedTags)

                    // MARK: — Matcher Card
                    RecipeMatcherCard(selectedTags: $selectedTags, recipes: $recipes)
                }
                .padding()
            }
            .sheet(isPresented: $showingImagePicker) {
                ImagePicker(selectedImage: $inputImage, sourceType: .camera)
            }
            .onChange(of: selectedTags) {
                let wasEmpty = previousTagsWereEmpty
                let isNowEmpty = selectedTags.isEmpty

                // Determine animation type
                let animation: Animation? =
                    if wasEmpty && !isNowEmpty {
                        // Transitioning from empty to having tags - use opacity ease in
                        .easeIn(duration: 0.3)
                    } else if !isNowEmpty {
                        // Changing between non-empty tag sets - use default animation
                        .default
                    } else {
                        // Other cases (removing all tags) - use default animation
                        .default
                    }

                withAnimation(animation) {
                    recipes = Database.shared.getRecipes(withTags: Array(selectedTags))
                }

                // Update tracking state
                previousTagsWereEmpty = isNowEmpty
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Fake "Matcher" Card
struct RecipeMatcherCard: View {
    @Binding var selectedTags: Set<String>
    @Binding var recipes: [Recipe]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "takeoutbag.and.cup.and.straw")
                Text("来看看组合出的菜谱吧！")
                    .font(.title3.bold())
                Spacer()
                // Image(systemName: "line.3.horizontal.decrease")
            }

            if recipes.isEmpty {
                if selectedTags.isEmpty {
                    Text("你要先选食材或工具哦～")
                        .foregroundStyle(.secondary)
                        .font(.footnote)
                } else {
                    Text("没有找到匹配的菜谱，试试别的组合吧～")
                        .foregroundStyle(.secondary)
                        .font(.footnote)
                }
            } else {
                TagFlow(data: recipes) { recipe in
                    if recipe.isVideo {
                        Button(action: {
                            if let url = URL(string: recipe.content) {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            Label(recipe.name, systemImage: "arrowtriangle.right")
                                .font(.subheadline)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(.purple.opacity(0.2))
                                .foregroundColor(.purple.opacity(0.9))
                                .cornerRadius(8)
                        }
                    } else {
                        NavigationLink(destination: RecipeDetailView(recipe: recipe)) {
                            Label(recipe.name, systemImage: "text.document")
                                .font(.subheadline)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(.orange.opacity(0.2))
                                .foregroundColor(.orange.opacity(0.9))
                                .cornerRadius(8)
                        }
                    }
                }
                .transition(.opacity)
            }
        }
    }
}

enum searchModes: String, CaseIterable, Identifiable {
    case fuzzy = "模糊匹配"
    case accurate = "严格匹配"
    case survival = "生存模式"

    var id: Self { self }
}

#Preview {
    ContentView()
}
