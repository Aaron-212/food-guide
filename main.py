import json
import os
import re
import sqlite3
import subprocess
import tarfile
from pathlib import Path

# --- Configuration Constants ---
CACHE_DIR_NAME = "cache"
DATABASE_NAME = "food_guide_recipes.db"
RECIPE_JSON_NAME = "yunyoujun_recipes.json"
HOWTOCOOK_ARCHIVE_NAME = "howtocook.tar.gz"
HOWTOCOOK_EXTRACTED_DIR_NAME = (
    "HowToCook-1.4.0"  # This is the root folder name inside the tar.gz
)

# URLs
HOWTOCOOK_URL = "https://github.com/Anduin2017/HowToCook/archive/refs/tags/1.4.0.tar.gz"
YUNYOUJUN_COOK_URL = "https://raw.githubusercontent.com/YunYouJun/cook/refs/tags/v1.2.4/app/data/recipe.json"

# --- Dynamic Paths (derived from constants) ---
BASE_DIR = Path(__file__).resolve().parent
CACHE_DIR = BASE_DIR / CACHE_DIR_NAME
DATABASE_PATH = BASE_DIR / DATABASE_NAME
RECIPE_JSON_PATH = CACHE_DIR / RECIPE_JSON_NAME
HOWTOCOOK_TAR_PATH = CACHE_DIR / HOWTOCOOK_ARCHIVE_NAME
HOWTOCOOK_DISHES_DIR = CACHE_DIR / HOWTOCOOK_EXTRACTED_DIR_NAME / "dishes"


def download_and_extract():
    # Ensure cache directory exists
    CACHE_DIR.mkdir(parents=True, exist_ok=True)

    # Download the HowToCook tar.gz file
    if not HOWTOCOOK_TAR_PATH.exists():
        print(f"Downloading {HOWTOCOOK_ARCHIVE_NAME} from {HOWTOCOOK_URL}...")
        try:
            subprocess.run(
                ["wget", "-O", str(HOWTOCOOK_TAR_PATH), HOWTOCOOK_URL], check=True
            )
            print(f"Saved to {HOWTOCOOK_TAR_PATH}")
        except subprocess.CalledProcessError as e:
            print(f"Error downloading {HOWTOCOOK_ARCHIVE_NAME}: {e}")
            # Optionally, re-raise the exception or handle it more gracefully
            raise
    else:
        print(f"{HOWTOCOOK_TAR_PATH} already exists. Skipping download.")

    # Extract the tar.gz file
    extracted_dir_path = CACHE_DIR / HOWTOCOOK_EXTRACTED_DIR_NAME
    if not extracted_dir_path.exists() or not any(
        extracted_dir_path.iterdir()
    ):  # Check if not exists or is empty
        print(f"Extracting {HOWTOCOOK_TAR_PATH} to {CACHE_DIR}...")
        with tarfile.open(HOWTOCOOK_TAR_PATH, "r:gz") as tar:
            tar.extractall(path=CACHE_DIR)
        print(f"Extracted to {CACHE_DIR}")
    else:
        print(
            f"{HOWTOCOOK_EXTRACTED_DIR_NAME} already exists in {CACHE_DIR}. Skipping extraction."
        )

    # Download the YunYouJun recipe.json file
    if not RECIPE_JSON_PATH.exists():
        print(f"Downloading {RECIPE_JSON_NAME} from {YUNYOUJUN_COOK_URL}...")
        try:
            subprocess.run(
                ["wget", "-O", str(RECIPE_JSON_PATH), YUNYOUJUN_COOK_URL], check=True
            )
            print(f"Saved to {RECIPE_JSON_PATH}")
        except subprocess.CalledProcessError as e:
            print(f"Error downloading {RECIPE_JSON_NAME}: {e}")
            # Optionally, re-raise the exception or handle it more gracefully
            raise
    else:
        print(
            f"{RECIPE_JSON_NAME} already exists at {RECIPE_JSON_PATH}. Skipping download."
        )


def extract_ingredients(markdown_content):
    # Find the section with ingredients
    pattern = r"## 必备原料和工具\n\n(.*?)(?=\n##|\Z)"
    match = re.search(pattern, markdown_content, re.DOTALL)

    if not match:
        return []

    # Extract the ingredients section
    ingredients_section = match.group(1)

    # Extract list items that start with hyphens and clean up
    return [
        line.strip()[1:].strip()
        for line in ingredients_section.splitlines()
        if line.strip().startswith("-") and line.strip()[1:].strip()
    ]


def init_database():
    # Remove existing database if it exists
    if DATABASE_PATH.exists():
        DATABASE_PATH.unlink()

    # Ensure cache directory exists for the database
    CACHE_DIR.mkdir(parents=True, exist_ok=True)

    # Create new database and tables
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()

    # Create Recipe table
    cursor.execute("""
    CREATE TABLE Recipe (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        content TEXT NOT NULL,
        is_video INTEGER NOT NULL DEFAULT 0
    )
    """)

    # Create RecipeTag table
    cursor.execute("""
    CREATE TABLE RecipeTag (
        recipe_id INTEGER,
        tag TEXT NOT NULL,
        FOREIGN KEY (recipe_id) REFERENCES Recipe (id),
        PRIMARY KEY (recipe_id, tag)
    )
    """)

    conn.commit()
    return conn, cursor


def process_recipes(conn, cursor):
    # Path to the dishes directory
    dishes_dir = HOWTOCOOK_DISHES_DIR

    # Walk through all markdown files in the dishes directory
    for root, dirs, files in os.walk(dishes_dir):
        # Skip the template directory
        if "template" in dirs:
            dirs.remove("template")

        for file in files:
            if file.endswith(".md"):
                file_path = Path(root) / file

                # Read the markdown file
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()

                # Extract ingredients
                ingredients = extract_ingredients(content)

                # Insert recipe
                recipe_name = file_path.stem
                cursor.execute(
                    "INSERT INTO Recipe (name, content, is_video) VALUES (?, ?, ?)",
                    (recipe_name, content, 0),
                )
                recipe_id = cursor.lastrowid

                # Insert ingredients as tags
                for ingredient in ingredients:
                    cursor.execute(
                        "INSERT OR IGNORE INTO RecipeTag (recipe_id, tag) VALUES (?, ?)",
                        (recipe_id, ingredient),
                    )

    # Commit changes and close connection
    conn.commit()


def process_json_recipes(conn, cursor):
    """Processes recipes from the downloaded JSON file and adds them to the database."""
    try:
        with open(RECIPE_JSON_PATH, "r", encoding="utf-8") as f:
            recipes_data = json.load(f)
    except FileNotFoundError:
        print(
            f"Error: {RECIPE_JSON_PATH} not found. Make sure to run download_and_extract first."
        )
        return
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from {RECIPE_JSON_PATH}.")
        return

    for recipe_item in recipes_data:
        recipe_name = recipe_item.get("name")
        if not recipe_name:
            print(f"Skipping recipe due to missing name: {recipe_item}")
            continue

        # Create content string
        bv_id = recipe_item.get("bv")
        if bv_id:
            content = f"https://www.bilibili.com/video/{bv_id}"
            is_video = 1
        else:
            # Fallback or alternative content if no Bilibili video
            # For now, let's use the tags if available, or a placeholder
            tags_for_content = ", ".join(recipe_item.get("tags", []))
            methods_for_content = ", ".join(recipe_item.get("methods", []))
            tools_for_content = ", ".join(recipe_item.get("tools", []))

            content_parts = []
            if tags_for_content:
                content_parts.append(f"Tags: {tags_for_content}")
            if methods_for_content:
                content_parts.append(f"Methods: {methods_for_content}")
            if tools_for_content:
                content_parts.append(f"Tools: {tools_for_content}")

            if content_parts:
                content = "; ".join(content_parts)
            else:
                content = "No detailed content available, check source JSON."
            is_video = 0

        # Insert recipe
        try:
            cursor.execute(
                "INSERT INTO Recipe (name, content, is_video) VALUES (?, ?, ?)",
                (recipe_name, content, is_video),
            )
            recipe_id = cursor.lastrowid
        except sqlite3.IntegrityError:
            print(
                f"Recipe '{recipe_name}' already exists or another integrity error occurred. Skipping."
            )
            # Optionally, you could fetch the existing recipe_id if you want to update its tags
            # For now, we skip adding new tags if the recipe name is not unique.
            continue

        # Insert tags from JSON fields
        all_tags = recipe_item.get("stuff", []) + recipe_item.get("tags", [])
        insert_tags(cursor, recipe_id, all_tags)

    conn.commit()
    print(f"Processed {len(recipes_data)} recipes from {RECIPE_JSON_PATH}")


def insert_tags(cursor, recipe_id, tags):
    """Insert tags for a recipe into the database, ignoring duplicates."""
    for tag in tags:
        cursor.execute(
            "INSERT OR IGNORE INTO RecipeTag (recipe_id, tag) VALUES (?, ?)",
            (recipe_id, tag),
        )


def main():
    print("Downloading and extracting HowToCook repository...")
    download_and_extract()

    # Initialize database
    conn, cursor = init_database()

    print(f"Processing recipes from {HOWTOCOOK_DISHES_DIR}...")
    process_recipes(conn, cursor)

    print(f"Processing recipes from {RECIPE_JSON_PATH}...")
    process_json_recipes(conn, cursor)

    print(f"Done! Results saved to {DATABASE_PATH}")

    # Close the database connection
    conn.close()


if __name__ == "__main__":
    main()
