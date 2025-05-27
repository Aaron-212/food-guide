import Foundation
import SQLite

final class Database {
    // MARK: - Properties
    private static var path: String {
        let documentsPath = NSSearchPathForDirectoriesInDomains(
            .documentDirectory,
            .userDomainMask,
            true
        )[0]
        return (documentsPath as NSString)
            .appendingPathComponent("food_guide_recipes.db")
    }

    private static var bundledDbPath: String? {
        Bundle.main.path(forResource: "food_guide_recipes", ofType: "db")
    }

    static let shared = Database()
    private let db: Connection

    // MARK: - Table / column definitions
    private let recipes = Table("Recipe")
    private let recipeTags = Table("RecipeTag")

    private let id = Expression<Int>("id")
    private let name = Expression<String>("name")
    private let content = Expression<String>("content")
    private let isVideo = Expression<Int>("is_video")
    private let recipeId = Expression<Int>("recipe_id")
    private let tag = Expression<String>("tag")

    // MARK: - Init
    private init() {
        do {
            let fileManager = FileManager.default
            guard let bundledPath = Database.bundledDbPath else {
                fatalError("Bundled database not found.")
            }
            // Always remove the existing database file if it exists
            if fileManager.fileExists(atPath: Database.path) {
                do {
                    try fileManager.removeItem(atPath: Database.path)
                } catch {
                    fatalError("Failed to remove existing database: \(error)")
                }
            }
            do {
                try fileManager.copyItem(atPath: bundledPath, toPath: Database.path)
            } catch {
                fatalError("Failed to copy database from bundle: \(error)")
            }
            db = try Connection(Database.path, readonly: true)
        } catch {
            fatalError("Failed to open DB: \(error)")
        }
    }

    // MARK: - Public API
    /// Returns every recipe that contains ALL of the supplied tags.
    /// If `tags` has only one element, "all" === "any".
    func getRecipes(withTags tags: [String])
        -> [Recipe]
    {
        guard !tags.isEmpty else { return [] }

        do {
            let mapRowIterator = try db.prepareRowIterator(
                recipes
                    .join(recipeTags, on: recipeId == recipes[id])
                    .filter(tags.contains(tag))
                    .group(id, having: recipeTags[tag].count == tags.count)
            )

            return try mapRowIterator.map {
                Recipe(id: $0[id], name: $0[name], content: $0[content], isVideo: $0[isVideo] == 1)
            }
        } catch {
            print("Failed!")
            return []
        }
    }
}

struct Recipe: Hashable {
    let id: Int
    let name: String
    let content: String
    let isVideo: Bool
}
