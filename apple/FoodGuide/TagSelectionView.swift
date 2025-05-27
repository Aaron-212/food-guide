import SwiftUI

// Define a new struct for categories
struct SelectableTagCategory: Identifiable, Hashable {
    let id = UUID()
    let name: String  // Internal name
    let tags: [String]
    let tintColor: Color
    let titleDisplay: String  // Full display title including emoji

    // Conformance to Hashable for ForEach
    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    static func == (lhs: SelectableTagCategory, rhs: SelectableTagCategory) -> Bool {
        lhs.id == rhs.id
    }
}

struct TagSelectionView: View {
    // Updated data structure for categories and tags
    private static let tagCategories: [SelectableTagCategory] = [
        SelectableTagCategory(
            name: "蔬菜",
            tags: [
                "土豆", "胡萝卜", "花菜", "白萝卜", "西葫芦", "番茄", "芹菜", "黄瓜", "洋葱", "蘑菇", "茄子", "豆腐", "包菜",
                "白菜",
            ],
            tintColor: .green,
            titleDisplay: "🥬 菜菜们"
        ),
        SelectableTagCategory(
            name: "肉类",
            tags: ["腊肠", "猪肉", "鸡蛋", "牛肉", "鸭肉", "鸡腿", "鱼", "午餐肉", "鸡肉", "虾"],
            tintColor: .pink,
            titleDisplay: "🥩 肉肉们"
        ),
        SelectableTagCategory(
            name: "主食",
            tags: ["面食", "面包", "米", "方便面"],
            tintColor: .yellow,
            titleDisplay: "🍚 主食也要一起下锅吗？"
        ),
        SelectableTagCategory(
            name: "其他",
            tags: ["烤箱", "空气炸锅", "微波炉", "盐", "白砂糖", "醋", "葱", "姜", "蒜"],
            tintColor: .gray,
            titleDisplay: "🔍 其他的标签"
        ),
    ]
    @Binding var selectedTags: Set<String>
    @State private var expandedCategories: Set<UUID> = []

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 20) {
            ForEach(Self.tagCategories) { category in
                VStack(alignment: .leading, spacing: 10) {
                    // Collapsible header
                    let isExpanded = expandedCategories.contains(category.id)
                    Button(action: {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            if isExpanded {
                                expandedCategories.remove(category.id)
                            } else {
                                expandedCategories.insert(category.id)
                            }
                        }
                    }) {
                        HStack {
                            Text(category.titleDisplay)
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(category.tintColor)
                                .rotationEffect(.degrees(isExpanded ? 90 : 0))
                                .animation(.easeInOut(duration: 0.3), value: isExpanded)
                        }
                        .font(.headline)
                    }

                    if isExpanded {
                        TagFlow(data: category.tags) { tag in
                            // Pass category.tintColor to TagView
                            TagView(
                                tag: tag, isSelected: selectedTags.contains(tag),
                                categoryTintColor: category.tintColor
                            ) {
                                if selectedTags.contains(tag) {
                                    selectedTags.remove(tag)
                                } else {
                                    selectedTags.insert(tag)
                                }
                            }
                        }
                        .padding(.top, 5)
                        .transition(
                            .asymmetric(
                                insertion: .opacity.combined(
                                    with: .scale(scale: 0.95, anchor: .top)
                                ).combined(
                                    with: .offset(y: -10)),
                                removal: .opacity.combined(with: .scale(scale: 0.95, anchor: .top))
                            ))
                    }
                }
            }
        }
    }
}

// MARK: - Simple TagFlow
struct TagFlow<Data: RandomAccessCollection, Content: View>: View where Data.Element: Hashable {
    let data: Data
    let content: (Data.Element) -> Content

    init(data: Data, @ViewBuilder content: @escaping (Data.Element) -> Content) {
        self.data = data
        self.content = content
    }

    var body: some View {
        FlowLayout(alignment: .leading) {
            ForEach(Array(data), id: \.self) { item in
                content(item)
            }
        }
    }
}

/// A one-file, minimal "flow" layout (iOS 16+).  From WWDC22 session 10056.
struct FlowLayout: Layout {
    var alignment: Alignment = .leading

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrangement(in: proposal, subviews: subviews).size
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        let arrangement = arrangement(in: proposal, subviews: subviews)
        for (index, place) in arrangement.frames.enumerated() {
            subviews[index].place(
                at: CGPoint(
                    x: bounds.minX + place.origin.x,
                    y: bounds.minY + place.origin.y
                ),
                proposal: ProposedViewSize(place.size)
            )
        }
    }

    private func arrangement(in proposal: ProposedViewSize, subviews: Subviews) -> (
        frames: [CGRect], size: CGSize
    ) {
        let maxWidth = proposal.width ?? .infinity
        var origin = CGPoint.zero
        var lineHeight: CGFloat = 0
        var frames: [CGRect] = []

        for subview in subviews {
            let size = subview.sizeThatFits(ProposedViewSize(width: maxWidth, height: nil))
            if origin.x + size.width > maxWidth {
                origin.x = 0
                origin.y += lineHeight + 8  // spacing between lines
                lineHeight = 0
            }
            frames.append(CGRect(origin: origin, size: size))
            origin.x += size.width + 8  // spacing between items in a line
            lineHeight = max(lineHeight, size.height)
        }
        let totalHeight = origin.y + lineHeight
        return (frames, CGSize(width: proposal.width ?? .zero, height: totalHeight))  // Use proposal.width for consistency
    }
}

struct TagView: View {
    let tag: String
    let isSelected: Bool
    let categoryTintColor: Color  // New parameter for category color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(tag)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                // Updated background and foreground colors based on categoryTintColor and selection state
                .background(isSelected ? categoryTintColor : categoryTintColor.opacity(0.2))
                .foregroundColor(isSelected ? .white : categoryTintColor.opacity(0.9))  // Darker tint for text if not selected
                .cornerRadius(8)
        }
    }
}

#Preview {
    // Update preview to work with new structure if needed, or keep simple
    // For simplicity, the preview still uses a basic set of tags.
    // A more complete preview would involve constructing a sample TagSelectionView with its categories.
    struct PreviewWrapper: View {
        @State var selectedTags: Set<String> = ["土豆", "烤箱"]
        var body: some View {
            ScrollView {  // Added ScrollView for better previewing if content overflows
                TagSelectionView(selectedTags: $selectedTags)
            }
        }
    }
    return PreviewWrapper()
}
