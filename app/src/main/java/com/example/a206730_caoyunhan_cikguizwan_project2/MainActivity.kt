package com.example.a206730_caoyunhan_cikguizwan_project2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap

val PrimaryIndigo = Color(0xFF53679B)
val LightPurple = Color(0xFF7E72B8)
val BgGreen = Color(0xFFB1D8B0)
val SoftWhite = Color(0xFFF9FBF9)

val FeatureGradient = Brush.linearGradient(
    listOf(Color(0xFF5B6291), Color(0xFF8F638A))
)

class TreeViewModel(
    private val repository: TreeRepository
) : ViewModel() {

    val history = repository.allOrders

    fun submitOrder(
        name: String,
        tree: String,
        location: String,
        price: Int
    ) {
        viewModelScope.launch {
            repository.insert(
                TreeOrderEntity(
                    name = name,
                    tree = tree,
                    location = location,
                    price = price
                )
            )
        }
    }

    fun uploadToCloud(
        name: String,
        tree: String,
        location: String,
        price: Int
    ) {
        val data = hashMapOf(
            "name" to name,
            "tree" to tree,
            "location" to location,
            "price" to price
        )

        FirebaseFirestore.getInstance()
            .collection("community_trees")
            .add(data)
    }
}

class TreeViewModelFactory(
    private val repository: TreeRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TreeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Adopt : Screen("adopt", "Adopt", Icons.Default.Add)
    object Summary : Screen("summary", "Summary", Icons.Default.Info)
    object History : Screen("history", "History", Icons.Default.List)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Environment : Screen("environment", "API", Icons.Default.LocationOn)
    object Motion : Screen("motion", "Sensor", Icons.Default.Star)
    object Community : Screen("community", "Cloud", Icons.Default.Public)
    object ParkTrees : Screen("park_trees", "Park Trees", Icons.Default.Park)
    object AncientTrees : Screen("ancient_trees", "Ancient Trees", Icons.Default.Star)

}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {

    val navController = rememberNavController()
    val context = LocalContext.current

    val database = remember {
        TreeDatabase.getDatabase(context)
    }

    val repository = remember {
        TreeRepository(database.treeDao())
    }

    val viewModel: TreeViewModel = viewModel(
        factory = TreeViewModelFactory(repository)
    )

    val history by viewModel.history.collectAsState(
        initial = emptyList()
    )

    val screens = listOf(
        Screen.Home,
        Screen.Adopt,
        Screen.Summary,
        Screen.History,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        },
                        icon = {
                            Icon(screen.icon, contentDescription = screen.title)
                        },
                        label = {
                            Text(screen.title)
                        }
                    )
                }
            }
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {

            composable(Screen.Home.route) {
                HomeScreen(navController, history)
            }

            composable(Screen.Adopt.route) {
                AdoptScreen(navController, viewModel)
            }

            composable(Screen.Summary.route) {
                SummaryScreen(history)
            }

            composable(Screen.History.route) {
                HistoryScreen(history)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(history)
            }

            composable(Screen.Environment.route) {
                EnvironmentScreen()
            }

            composable(Screen.Motion.route) {
                MotionSensorScreen()
            }

            composable(Screen.Community.route) {
                CommunityScreen()
            }
            composable(Screen.ParkTrees.route) {
                ParkTreesScreen()
            }
            composable(Screen.AncientTrees.route) {
                AncientTreesScreen()
            }
        }
    }
}

/* HOME */

@Composable
fun HomeScreen(
    navController: NavController,
    history: List<TreeOrderEntity>
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BgGreen, SoftWhite)
                )
            )
    ) {

        item {

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    Modifier
                        .size(50.dp)
                        .background(LightPurple, CircleShape)
                        .clickable {
                            navController.navigate(Screen.Profile.route)
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {

                    Text(
                        "Welcome back,",
                        color = Color.Gray
                    )

                    Text(
                        "Tree Lover 🌿",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = PrimaryIndigo
                    )
                }
            }
        }

        item {
            StatCard(
                adoptedCount = history.size
            )
        }

        item {
            FeatureBox(
                "Park Trees",
                Icons.Default.Home
            ) {
                navController.navigate(Screen.ParkTrees.route)
            }
        }

        item {
            FeatureBox(
                "Ancient Trees",
                Icons.Default.Star
            ) {
                navController.navigate(Screen.AncientTrees.route)
            }
        }

        item {

            Column(
                Modifier.padding(16.dp)
            ) {

                Text(
                    "Quick Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryIndigo
                )

                Spacer(Modifier.height(12.dp))

                val actions = listOf(
                    Triple("Plant Tree", Icons.Default.Favorite) {
                        navController.navigate(Screen.Adopt.route)
                    },
                    Triple("Records", Icons.Default.List) {
                        navController.navigate(Screen.History.route)
                    },
                    Triple("Summary", Icons.Default.Info) {
                        navController.navigate(Screen.Summary.route)
                    },
                    Triple("Profile", Icons.Default.Person) {
                        navController.navigate(Screen.Profile.route)
                    },
                    Triple("Live API", Icons.Default.LocationOn) {
                        navController.navigate(Screen.Environment.route)
                    },
                    Triple("Stats", Icons.Default.BarChart) {
                        navController.navigate(Screen.Motion.route)
                    },
                    Triple("Community", Icons.Default.Public) {
                        navController.navigate(Screen.Community.route)
                    },
                    Triple("My Progress", Icons.Default.TrendingUp) {
                        navController.navigate(Screen.History.route)
                    }
                )

                actions.chunked(2).forEach { row ->

                    Row(
                        Modifier.fillMaxWidth()
                    ) {

                        row.forEach { (name, icon, action) ->

                            ActionItem(
                                name,
                                icon,
                                Modifier.weight(1f),
                                action
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(adoptedCount: Int) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            StatComponent(
                "12",
                "Trees",
                Modifier.weight(1f)
            )

            StatComponent(
                adoptedCount.toString(),
                "Adopted",
                Modifier.weight(1f)
            )

            StatComponent(
                "3",
                "Progress",
                Modifier.weight(1f)
            )
        }
    }
}

/* ADOPT */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdoptScreen(
    navController: NavController,
    viewModel: TreeViewModel
) {

    var name by remember {
        mutableStateOf("")
    }

    val treeOptions = listOf(
        "Banyan Tree" to 50,
        "Rain Tree" to 80,
        "Mangrove Tree" to 100,
        "Ancient Oak" to 150
    )

    val locationOptions = listOf(
        "Kajang Eco Park",
        "Putrajaya Botanical Garden",
        "FRIM Forest",
        "Langkawi Nature Reserve"
    )

    var selectedTree by remember {
        mutableStateOf(treeOptions[0])
    }

    var selectedLocation by remember {
        mutableStateOf(locationOptions[0])
    }

    var treeExpanded by remember {
        mutableStateOf(false)
    }

    var locationExpanded by remember {
        mutableStateOf(false)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "Tree Adoption Form",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            label = {
                Text("Tree Name")
            },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = treeExpanded,
            onExpandedChange = {
                treeExpanded = !treeExpanded
            }
        ) {

            OutlinedTextField(
                value = selectedTree.first,
                onValueChange = {},
                readOnly = true,
                label = {
                    Text("Tree Type")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(treeExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = treeExpanded,
                onDismissRequest = {
                    treeExpanded = false
                }
            ) {

                treeOptions.forEach {

                    DropdownMenuItem(
                        text = {
                            Text("${it.first} - RM${it.second}")
                        },
                        onClick = {
                            selectedTree = it
                            treeExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = locationExpanded,
            onExpandedChange = {
                locationExpanded = !locationExpanded
            }
        ) {

            OutlinedTextField(
                value = selectedLocation,
                onValueChange = {},
                readOnly = true,
                label = {
                    Text("Location")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = locationExpanded,
                onDismissRequest = {
                    locationExpanded = false
                }
            ) {

                locationOptions.forEach {

                    DropdownMenuItem(
                        text = {
                            Text(it)
                        },
                        onClick = {
                            selectedLocation = it
                            locationExpanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "Price: RM${selectedTree.second}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo
        )

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.submitOrder(
                        name = name,
                        tree = selectedTree.first,
                        location = selectedLocation,
                        price = selectedTree.second
                    )

                    viewModel.uploadToCloud(
                        name = name,
                        tree = selectedTree.first,
                        location = selectedLocation,
                        price = selectedTree.second
                    )

                    navController.navigate(Screen.Summary.route) {
                        launchSingleTop = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit")
        }

        Text(
            "This form saves data locally using Room and uploads a copy to Firebase Firestore.",
            color = Color.Gray
        )
    }
}

/* SUMMARY */

@Composable
fun SummaryScreen(
    history: List<TreeOrderEntity>
) {

    val latest = history.lastOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "Tree Growth Status",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo
        )

        if (latest != null) {

            Card {

                Column(
                    Modifier.padding(16.dp)
                ) {

                    Text(
                        "Latest Adopted Tree",
                        fontWeight = FontWeight.Bold
                    )

                    Text("Name: ${latest.name}")
                    Text("Tree: ${latest.tree}")
                    Text("Location: ${latest.location}")
                    Text("Price: RM${latest.price}")
                    Text("Status: Seed Planted 🌱")
                }
            }

            Card {

                Column(
                    Modifier.padding(16.dp)
                ) {

                    Text(
                        "Growth Progress",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { 0.65f },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("65% Growth Completed")
                    Text("Estimated Full Growth: 8 months")
                }
            }

            Card {

                Column(
                    Modifier.padding(16.dp)
                ) {

                    Text(
                        "Environmental Impact",
                        fontWeight = FontWeight.Bold
                    )

                    Text("CO₂ Absorbed: 24kg")
                    Text("Supports SDG 15 🌱")
                }
            }

        } else {

            Text("No tree adoption yet.")
        }
    }
}

/* HISTORY */

@Composable
fun HistoryScreen(
    history: List<TreeOrderEntity>
) {

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {
            Text(
                "Adoption History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                modifier = Modifier.padding(8.dp)
            )
        }

        if (history.isEmpty()) {
            item {
                Text(
                    "No adoption records yet.",
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(history) {

                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Text(
                            it.name,
                            fontWeight = FontWeight.Bold
                        )

                        Text(it.tree)
                        Text(it.location)
                        Text("RM${it.price}")
                    }
                }
            }
        }
    }
}

/* PROFILE */

@Composable
fun ProfileScreen(
    history: List<TreeOrderEntity>
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    LightPurple,
                    CircleShape
                ),

            contentAlignment = Alignment.Center
        ) {

            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Tree Lover",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo
        )

        Text(
            "Eco Supporter",
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                Modifier.padding(16.dp)
            ) {

                Text("📍 Region: Malaysia")
                Text("🌱 Trees Adopted: ${history.size}")
                Text("⭐ Eco Points: ${history.size * 50}")
                Text("🎯 SDG Goal: Life on Land")
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Edit Profile")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("About App")
        }
    }
}

/* WEB API */

data class WeatherResponse(
    val current_weather: CurrentWeather?
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double
)

interface WeatherApi {
    @GET("v1/forecast?latitude=2.99&longitude=101.79&current_weather=true")
    suspend fun getWeather(): WeatherResponse
}

object RetrofitClient {
    val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}

@Composable
fun EnvironmentScreen() {

    var temperature by remember {
        mutableStateOf("Loading...")
    }

    var wind by remember {
        mutableStateOf("Loading...")
    }

    LaunchedEffect(Unit) {
        try {
            val result = RetrofitClient.api.getWeather()
            temperature = "${result.current_weather?.temperature ?: 0.0} °C"
            wind = "${result.current_weather?.windspeed ?: 0.0} km/h"
        } catch (e: Exception) {
            temperature = "Failed to load API data"
            wind = "Please check internet connection"
        }
    }

    val weatherIcon = when {
        temperature.contains("Failed") -> "⚠️"
        temperature.contains("Loading") -> "⏳"
        temperature.substringBefore(" ").toDoubleOrNull()?.let { it >= 30 } == true -> "☀️"
        temperature.substringBefore(" ").toDoubleOrNull()?.let { it >= 25 } == true -> "🌤️"
        else -> "🌱"
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "Live Environment Data",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo
        )

        Card(Modifier.fillMaxWidth()) {

            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = weatherIcon,
                    fontSize = 72.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Web API: Open-Meteo",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text("📍 Location: Kajang, Malaysia")

                Text(
                    "🌡 Temperature: $temperature",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "💨 Wind Speed: $wind",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {

            Column(Modifier.padding(16.dp)) {

                Text(
                    "SDG 15 Connection",
                    fontWeight = FontWeight.Bold
                )

                Text("This live weather data helps users understand the environment before planting trees.")
            }
        }
    }
}

/* SENSOR */

@Composable
fun MotionSensorScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "Tree Statistics Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                Text(
                    "🌳 Total Trees Goal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("75% of reforestation goal completed")
                Text("Goal: 100 trees")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                Text("🌱 Environmental Impact", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("CO₂ Absorbed: 240 kg")
                Text("Eco Points Generated: 750")
                Text("Community Contributions: Active")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                Text("📊 Project Integration", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Room Database: Local adoption history")
                Text("Firebase Firestore: Community tree records")
                Text("Weather API: Live environment data")
            }
        }
    }
}
/* FIREBASE CLOUD */

data class CommunityTree(
    val name: String = "",
    val tree: String = "",
    val location: String = "",
    val price: Int = 0
)

@Composable
fun CommunityScreen() {

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    var trees by remember {
        mutableStateOf<List<CommunityTree>>(emptyList())
    }

    DisposableEffect(Unit) {

        val listener = db.collection("community_trees")
            .addSnapshotListener { snapshot, _ ->

                trees = snapshot?.documents?.mapNotNull {
                    it.toObject(CommunityTree::class.java)
                } ?: emptyList()
            }

        onDispose {
            listener.remove()
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        item {
            Text(
                "Community Forest",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                modifier = Modifier.padding(8.dp)
            )
        }

        item {
            Text(
                "This screen loads shared tree records from Firebase Firestore.",
                modifier = Modifier.padding(8.dp),
                color = Color.Gray
            )
        }

        if (trees.isEmpty()) {
            item {
                Text(
                    "No cloud records yet.",
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(trees) {

                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Text(
                            it.name,
                            fontWeight = FontWeight.Bold
                        )

                        Text(it.tree)
                        Text(it.location)
                        Text("RM${it.price}")
                    }
                }
            }
        }
    }
}

/* COMPONENTS */

@Composable
fun StatComponent(
    value: String,
    label: String,
    modifier: Modifier
) {

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            label,
            color = Color.Gray
        )
    }
}

@Composable
fun FeatureBox(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    Card(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                onClick()
            }
    ) {

        Box(
            Modifier
                .background(FeatureGradient)
                .padding(24.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        title,
                        color = Color.White
                    )

                    Text(
                        "Tap to explore",
                        color = Color.White.copy(0.8f)
                    )
                }

                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ActionItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .padding(6.dp)
            .height(90.dp)
            .clickable {
                onClick()
            }
    ) {

        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),

            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryIndigo
            )

            Spacer(Modifier.height(8.dp))

            Text(title)
        }
    }
}
data class ParkTree(
    val emoji: String,
    val name: String,
    val location: String
)

@Composable
fun ParkTreesScreen() {

    val trees = listOf(
        ParkTree("🌳", "Banyan Tree", "Kajang Eco Park"),
        ParkTree("🌲", "Rain Tree", "Putrajaya Botanical Garden"),
        ParkTree("🌴", "Mangrove Tree", "FRIM Forest"),
        ParkTree("🌿", "Ancient Oak", "Langkawi Nature Reserve")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BgGreen, SoftWhite)
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Text(
                "Park Trees Gallery",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Explore available trees and their locations.",
                color = Color.Gray
            )
        }

        items(trees) { tree ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        tree.emoji,
                        fontSize = 56.sp
                    )

                    Spacer(Modifier.width(16.dp))

                    Column {

                        Text(
                            tree.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = PrimaryIndigo
                        )

                        Text(
                            "📍 ${tree.location}",
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }


}
data class AncientTree(
    val emoji: String,
    val name: String,
    val location: String,
    val age: String,
    val description: String
)

@Composable
fun AncientTreesScreen() {

    val trees = listOf(
        AncientTree(
            emoji = "🌲",
            name = "Chankiri Tree",
            location = "Cambodia",
            age = "Approx. 2,000 years old",
            description = "A historically significant ancient tree in Cambodia."
        ),
        AncientTree(
            emoji = "🌳",
            name = "Major Oak",
            location = "Sherwood Forest, England",
            age = "Approx. 800–1,000 years old",
            description = "A famous ancient oak tree connected with the legend of Robin Hood."
        ),
        AncientTree(
            emoji = "🌲",
            name = "Jomon Sugi",
            location = "Yakushima, Japan",
            age = "Approx. 2,170–7,200 years old",
            description = "One of the oldest and most famous cedar trees in Japan."
        ),
        AncientTree(
            emoji = "🌳",
            name = "Senator Tree",
            location = "Florida, USA",
            age = "Approx. 3,500 years old",
            description = "A very old bald cypress tree once known as one of the oldest trees in the United States."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BgGreen, SoftWhite)
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Text(
                "Ancient Trees Gallery",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Discover famous ancient trees around the world.",
                color = Color.Gray
            )
        }

        items(trees) { tree ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        tree.emoji,
                        fontSize = 56.sp
                    )

                    Spacer(Modifier.width(16.dp))

                    Column {

                        Text(
                            tree.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = PrimaryIndigo
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "📍 ${tree.location}",
                            color = Color.Gray
                        )

                        Text(
                            "🕒 ${tree.age}",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "🌿 ${tree.description}",
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
