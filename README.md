# Food Guide

## Overview

Food Guide is a monorepo containing:

- A Python script (`main.py`) that downloads, processes, and aggregates recipes from online sources into a structured SQLite database (`food_guide_recipes.db`).
- An iOS application (found in `apple/FoodGuide/`) that uses this database to allow users to browse and search for recipes.

The primary purpose of the Python script is to create a comprehensive local recipe database for use in the Food Guide iOS app.

## Python Script (`main.py`)

### Features

- **Data Aggregation**: Collects recipes from GitHub repositories (HowToCook and YunYouJun/cook).
- **Caching**: Saves downloaded files to a `cache/` directory to avoid redundant downloads.
- **Data Extraction**: Parses both Markdown and JSON sources for recipe details and tags.
- **Database Management**: Initializes and populates an SQLite database (`food_guide_recipes.db`) with schema for recipes and their tags.
- **Content Handling**: Identifies text-based versus video-based recipes and flags them in the database.

### Requirements

- Python 3.x (preferably 3.13)
- `wget` command-line utility installed and accessible in your PATH

### Setup & Usage

#### Prerequisites

- Clone the repository to your local machine.
- Ensure Python 3 is installed.
- Install `wget`:
    - macOS: `brew install wget`
    - Debian/Ubuntu: `sudo apt-get install wget`

#### Running the Script

```bash
python main.py
```

The script will:

- Create a `cache/` directory (if it doesn't exist)
- Download and cache archives/data files
- Create/update `food_guide_recipes.db` in the repository root (overwrites if existing)
- Print progress to the console

## Database Schema (`food_guide_recipes.db`)

### Recipe Table

Stores individual recipes.

| Column     | Type    | Constraints               | Description                                                              |
| ---------- | ------- | ------------------------- | ------------------------------------------------------------------------ |
| `id`       | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique identifier for the recipe                                         |
| `name`     | TEXT    | NOT NULL                  | Name of the recipe                                                       |
| `content`  | TEXT    | NOT NULL                  | Detailed recipe content (Markdown or video URL, depending on the source) |
| `is_video` | INTEGER | NOT NULL DEFAULT 0        | Flag: 1 if `content` is a video, 0 otherwise                             |

### RecipeTag Table

Links recipes to their tags.

| Column      | Type    | Constraints                       | Description                             |
| ----------- | ------- | --------------------------------- | --------------------------------------- |
| `recipe_id` | INTEGER | FOREIGN KEY REFERENCES Recipe(id) | Recipe this tag belongs to              |
| `tag`       | TEXT    | NOT NULL                          | Tag string (ingredient/category/method) |
|             |         | PRIMARY KEY (`recipe_id`, `tag`)  | One tag per recipe                      |

## Data Processing Workflow

### Initialization

- Paths for caching and the database are set.

### Download & Extraction

- Downloads `HowToCook` archive and `YunYouJun` JSON if not already cached.
- Extracts Markdown recipes from HowToCook archive.

### Database Setup

- Removes existing `food_guide_recipes.db`, then creates a new database and tables.

### Recipe Processing

#### HowToCook Recipes

- Parses Markdown files in `cache/HowToCook-1.4.0/dishes/`.
- Extracts:
    - Recipe name (from filename)
    - Recipe content (Markdown)
    - Tags (ingredients, from the "## 必备原料和工具" section)
    - Sets `is_video` = 0

#### YunYouJun/cook Recipes

- Parses the cached JSON file.
- Extracts:
    - Recipe name
    - Content: Bilibili URL (if available) or descriptive text
    - Sets `is_video` = 1 if there is a video, 0 otherwise
    - Tags from `stuff` and `tags` fields

### Finishing

- Commits all changes and closes the SQLite connection.

## iOS Companion App (`apple/FoodGuide/`)

Located in the `apple/FoodGuide/` directory, the iOS app:

- Interacts with the SQLite database built by `main.py`
- Lets users browse, search, and filter recipes by tags
- Displays lists and details of recipes

## Acknowledgments & Data Sources

The Food Guide database and app use open community recipe resources.

- **HowToCook** (v1.4.0) https://github.com/Anduin2017/HowToCook
- **YunYouJun/cook** (v1.2.4) https://github.com/YunYouJun/cook
- **SQLite.swift** (v0.15.3) https://github.com/stephencelis/SQLite.swift
- **swift-markdown-ui** (v2.4.1) https://github.com/gonzalezreal/swift-markdown-ui
