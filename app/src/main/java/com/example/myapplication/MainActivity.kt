package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RecipeApp()
            }
        }
    }
}

enum class RecipeStatus { WANT_TO_COOK, COOKING, COOKED }

enum class RecipeDifficulty { EASY, MEDIUM, HARD }

data class Recipe(
    val id: Int,
    val name: String,
    val description: String,
    val ingredients: List<String>,
    val prepTime: Int,
    val difficulty: RecipeDifficulty,
    val category: String,
    val status: RecipeStatus = RecipeStatus.WANT_TO_COOK
)

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: RecipeStatus? = null
) {
    val filteredRecipes: List<Recipe>
        get() = recipes.filter { recipe ->
            val matchesSearch = searchQuery.isBlank() ||
                    recipe.name.contains(searchQuery, ignoreCase = true) ||
                    recipe.category.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedFilter == null || recipe.status == selectedFilter
            matchesSearch && matchesFilter
        }

    val statistics: Statistics
        get() {
            val total = recipes.size
            val wantToCook = recipes.count { it.status == RecipeStatus.WANT_TO_COOK }
            val cooking = recipes.count { it.status == RecipeStatus.COOKING }
            val cooked = recipes.count { it.status == RecipeStatus.COOKED }
            return Statistics(total, wantToCook, cooking, cooked)
        }
}

data class Statistics(
    val total: Int,
    val wantToCook: Int,
    val cooking: Int,
    val cooked: Int
)

class RecipeViewModel : ViewModel() {
    var uiState by mutableStateOf(RecipeListUiState(recipes = getSampleRecipes()))
        private set

    fun onSearchChange(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    fun onFilterChange(filter: RecipeStatus?) {
        uiState = uiState.copy(selectedFilter = filter)
    }

    fun updateRecipeStatus(recipeId: Int, newStatus: RecipeStatus) {
        uiState = uiState.copy(
            recipes = uiState.recipes.map { recipe ->
                if (recipe.id == recipeId) {
                    recipe.copy(status = newStatus)
                } else {
                    recipe
                }
            }
        )
    }

    fun getRecipeById(id: Int): Recipe? {
        return uiState.recipes.find { it.id == id }
    }
}

@Composable
fun RecipeApp() {
    val navController = rememberNavController()
    val viewModel: RecipeViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "recipe_list"
    ) {
        composable("recipe_list") {
            RecipeListScreen(
                uiState = viewModel.uiState,
                onSearchChange = viewModel::onSearchChange,
                onFilterChange = viewModel::onFilterChange,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe_detail/$recipeId")
                }
            )
        }
        composable(
            route = "recipe_detail/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
        ) { backStackEntry ->
            val recipeId = requireNotNull(backStackEntry.arguments?.getInt("recipeId")) {
                "ID рецепта не найден"
            }
            val recipe = viewModel.getRecipeById(recipeId)
            if (recipe != null) {
                RecipeDetailScreen(
                    recipe = recipe,
                    onStatusChange = { newStatus ->
                        viewModel.updateRecipeStatus(recipe.id, newStatus)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    uiState: RecipeListUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (RecipeStatus?) -> Unit,
    onRecipeClick: (Int) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Кулинарная книга") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск рецептов...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterChange = onFilterChange,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatisticsBlock(
                statistics = uiState.statistics,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.filteredRecipes.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.filteredRecipes,
                        key = { it.id }
                    ) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabs(
    selectedFilter: RecipeStatus?,
    onFilterChange: (RecipeStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "Все" to null,
        "Хочу приготовить" to RecipeStatus.WANT_TO_COOK,
        "Готовлю" to RecipeStatus.COOKING,
        "Приготовлено" to RecipeStatus.COOKED
    )

    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.second == selectedFilter }.coerceAtLeast(0),
        modifier = modifier
    ) {
        tabs.forEach { (title, filter) ->
            Tab(
                selected = filter == selectedFilter,
                onClick = { onFilterChange(filter) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
fun StatisticsBlock(
    statistics: Statistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Всего", value = statistics.total.toString())
            StatItem(label = "Хочу приготовить", value = statistics.wantToCook.toString())
            StatItem(label = "Готовлю", value = statistics.cooking.toString())
            StatItem(label = "Приготовлено", value = statistics.cooked.toString())
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.titleLarge.fontSize
        )
        Text(
            text = label,
            fontSize = MaterialTheme.typography.bodySmall.fontSize
        )
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = recipe.name,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
            Text("${recipe.category} • ${recipe.prepTime} мин • ${getDifficultyText(recipe.difficulty)}")
            Text(
                text = "Статус: ${getStatusText(recipe.status)}",
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onStatusChange: (RecipeStatus) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.name) },
                navigationIcon = {
                    Button(onClick = onBack) {
                        Text("Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = recipe.name,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )

            Text(
                text = "Категория: ${recipe.category}",
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
            Text(
                text = "Время приготовления: ${recipe.prepTime} минут",
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
            Text(
                text = "Сложность: ${getDifficultyText(recipe.difficulty)}",
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )

            Text(
                text = "Описание:",
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
            Text(text = recipe.description)

            Text(
                text = "Ингредиенты:",
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
            recipe.ingredients.forEach { ingredient ->
                Text(text = "• $ingredient")
            }

            Text(
                text = "Текущий статус: ${getStatusText(recipe.status)}",
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecipeStatus.values().forEach { status ->
                    Button(
                        onClick = { onStatusChange(status) },
                        enabled = recipe.status != status
                    ) {
                        Text(getStatusText(status))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Рецепты не найдены",
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

fun getStatusText(status: RecipeStatus): String {
    return when (status) {
        RecipeStatus.WANT_TO_COOK -> "Хочу приготовить"
        RecipeStatus.COOKING -> "Готовлю"
        RecipeStatus.COOKED -> "Приготовлено"
    }
}

fun getDifficultyText(difficulty: RecipeDifficulty): String {
    return when (difficulty) {
        RecipeDifficulty.EASY -> "Легко"
        RecipeDifficulty.MEDIUM -> "Средне"
        RecipeDifficulty.HARD -> "Сложно"
    }
}

fun getSampleRecipes(): List<Recipe> {
    return listOf(
        Recipe(
            id = 1,
            name = "Borscht",
            description = "Традиционный украинский суп со свеклой, капустой и мясом. Подается со сметаной и зеленью.",
            ingredients = listOf(
                "Свекла - 2 шт",
                "Капуста - 300 г",
                "Картофель - 4 шт",
                "Морковь - 1 шт",
                "Лук - 1 шт",
                "Говядина - 500 г",
                "Томатная паста - 2 ст.л.",
                "Сметана для подачи",
                "Зелень"
            ),
            prepTime = 120,
            difficulty = RecipeDifficulty.MEDIUM,
            category = "Супы",
            status = RecipeStatus.WANT_TO_COOK
        ),
        Recipe(
            id = 2,
            name = "Caesar with Chicken",
            description = "Классический салат с курицей, листьями романо, пармезаном и фирменной заправкой.",
            ingredients = listOf(
                "Куриное филе - 300 г",
                "Салат романо - 1 кочан",
                "Пармезан - 100 г",
                "Сухарики - 100 г",
                "Яйца - 2 шт",
                "Оливковое масло",
                "Сок лимона",
                "Соус Цезарь"
            ),
            prepTime = 30,
            difficulty = RecipeDifficulty.EASY,
            category = "Салаты",
            status = RecipeStatus.COOKING
        ),
        Recipe(
            id = 3,
            name = "Syrniki",
            description = "Нежные творожные оладьи, которые отлично подходят для завтрака. Подаются со сметаной, вареньем или медом.",
            ingredients = listOf(
                "Творог - 500 г",
                "Яйца - 2 шт",
                "Мука - 4 ст.л.",
                "Сахар - 3 ст.л.",
                "Соль - щепотка",
                "Растительное масло для жарки"
            ),
            prepTime = 25,
            difficulty = RecipeDifficulty.EASY,
            category = "Завтраки",
            status = RecipeStatus.COOKED
        ),
        Recipe(
            id = 4,
            name = "Plov",
            description = "Ароматное узбекское блюдо из риса, баранины и овощей, приготовленное в казане.",
            ingredients = listOf(
                "Баранина - 600 г",
                "Рис - 500 г",
                "Морковь - 3 шт",
                "Лук - 2 шт",
                "Чеснок - 1 головка",
                "Курдючный жир - 100 г",
                "Зира, барбарис",
                "Соль, перец"
            ),
            prepTime = 90,
            difficulty = RecipeDifficulty.MEDIUM,
            category = "Основные блюда",
            status = RecipeStatus.WANT_TO_COOK
        ),
        Recipe(
            id = 5,
            name = "Olivier Salad",
            description = "Советский классический салат, обязательное блюдо на новогоднем столе.",
            ingredients = listOf(
                "Колбаса вареная - 400 г",
                "Картофель - 4 шт",
                "Морковь - 2 шт",
                "Яйца - 4 шт",
                "Огурцы соленые - 3 шт",
                "Горошек консервированный - 1 банка",
                "Майонез",
                "Лук зеленый"
            ),
            prepTime = 45,
            difficulty = RecipeDifficulty.EASY,
            category = "Салаты",
            status = RecipeStatus.COOKING
        ),
        Recipe(
            id = 6,
            name = "Shchi",
            description = "Наваристый русский суп из свежей или квашеной капусты с мясом и кореньями.",
            ingredients = listOf(
                "Капуста свежая - 400 г",
                "Говядина - 500 г",
                "Картофель - 3 шт",
                "Морковь - 1 шт",
                "Лук - 1 шт",
                "Томатная паста - 1 ст.л.",
                "Корень петрушки",
                "Сметана для подачи"
            ),
            prepTime = 100,
            difficulty = RecipeDifficulty.MEDIUM,
            category = "Супы",
            status = RecipeStatus.COOKED
        ),
        Recipe(
            id = 7,
            name = "Napoleon Cake",
            description = "Слоеный торт с нежным заварным кремом, который готовится по классическому рецепту.",
            ingredients = listOf(
                "Для теста: мука, масло сливочное, яйцо, вода",
                "Для крема: молоко, яйца, сахар, мука, масло сливочное",
                "Сахарная пудра для посыпки"
            ),
            prepTime = 180,
            difficulty = RecipeDifficulty.HARD,
            category = "Десерты",
            status = RecipeStatus.WANT_TO_COOK
        ),
        Recipe(
            id = 8,
            name = "Greek Salad",
            description = "Свежий салат из помидоров, огурцов, болгарского перца, феты и оливок с оливковым маслом.",
            ingredients = listOf(
                "Помидоры - 3 шт",
                "Огурцы - 2 шт",
                "Болгарский перец - 1 шт",
                "Фета - 200 г",
                "Маслины - 100 г",
                "Лук красный - 1 шт",
                "Оливковое масло",
                "Орегано"
            ),
            prepTime = 15,
            difficulty = RecipeDifficulty.EASY,
            category = "Салаты",
            status = RecipeStatus.COOKED
        )
    )
}
