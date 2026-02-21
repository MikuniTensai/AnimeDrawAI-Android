package com.doyouone.drawai.util

import com.doyouone.drawai.data.model.Workflow

object WorkflowFilterUtils {

    fun filterAndSortWorkflows(
        workflows: List<Workflow>,
        searchQuery: String,
        selectedFilter: String,
        selectedSortOption: String?,
        allFavoriteIds: List<String>,
        filterAllLabel: String,
        filterFavoritesLabel: String,
        filterAnimeLabel: String,
        filterGeneralLabel: String,
        filterAnimalLabel: String,
        filterFlowerLabel: String,
        filterFoodLabel: String,
        filterBackgroundLabel: String,
        sortMostViewedLabel: String,
        sortNewestLabel: String,
        sortMostPopularLabel: String,
        randomSeed: Long = 0
    ): List<Workflow> {
        var result = workflows.toList()
        
        // 1. Filter by Search
        if (searchQuery.isNotEmpty()) {
            result = result.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // 2. Filter by Category/Favorite
        if (selectedFilter != filterAllLabel) {
            result = result.filter { workflow ->
                 when (selectedFilter) {
                    filterFavoritesLabel -> workflow.id in allFavoriteIds
                    filterAnimeLabel -> workflow.category == "Anime"
                    filterGeneralLabel -> workflow.category == "General"
                    filterAnimalLabel -> workflow.category == "Animal"
                    filterFlowerLabel -> workflow.category == "Flower"
                    filterFoodLabel -> workflow.category == "Food"
                    filterBackgroundLabel -> workflow.category == "Background"
                    "Content" -> true // Show all workflows when Content tool is selected
                    else -> workflow.category.equals(selectedFilter, ignoreCase = true)
                }
            }
        }

        // 3. Apply Sorting
        if (selectedSortOption != null) {
             result = when (selectedSortOption) {
                sortMostViewedLabel -> result.sortedByDescending { it.viewCount }
                sortNewestLabel -> result.sortedBy { it.generationCount } // Convention used in existing code
                sortMostPopularLabel -> result.sortedByDescending { it.generationCount }
                else -> result
            }
        } else {
            // Default: Randomize if not filtered
            if (searchQuery.isEmpty() && selectedFilter == filterAllLabel) {
                result = result.sortedBy { it.id }.shuffled(java.util.Random(randomSeed))
            }
        }

        return result
    }
}
