package com.doyouone.drawai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.imageLoader
import com.doyouone.drawai.ui.components.AnimatedBottomNavigation
import com.doyouone.drawai.ui.navigation.BottomNavItem
import com.doyouone.drawai.ui.navigation.Screen
import com.doyouone.drawai.ui.screens.*
import com.doyouone.drawai.ui.theme.AnimeDrawAITheme
import com.doyouone.drawai.data.repository.ReportRepository
import com.doyouone.drawai.data.model.ImageReport
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var shouldEnterPiPMode = false

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldEnterPiPMode && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(9, 16))
                    .build()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize locale from saved preference with proper country codes
        val prefs = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val savedLanguage = prefs.getString("selected_language", "en") ?: "en"
        val locale = when (savedLanguage) {
            "en" -> java.util.Locale.Builder().setLanguage("en").setRegion("US").build()
            "in" -> java.util.Locale.Builder().setLanguage("id").setRegion("ID").build()  // Indonesian uses 'id' as language code
            "es" -> java.util.Locale.Builder().setLanguage("es").setRegion("ES").build()
            "pt" -> java.util.Locale.Builder().setLanguage("pt").setRegion("PT").build()
            "fr" -> java.util.Locale.Builder().setLanguage("fr").setRegion("FR").build()
            "de" -> java.util.Locale.Builder().setLanguage("de").setRegion("DE").build()
            "zh" -> java.util.Locale.Builder().setLanguage("zh").setRegion("CN").build()
            "ja" -> java.util.Locale.Builder().setLanguage("ja").setRegion("JP").build()
            "ko" -> java.util.Locale.Builder().setLanguage("ko").setRegion("KR").build()
            "hi" -> java.util.Locale.Builder().setLanguage("hi").setRegion("IN").build()
            "th" -> java.util.Locale.Builder().setLanguage("th").setRegion("TH").build()
            "ar" -> java.util.Locale.Builder().setLanguage("ar").setRegion("SA").build()
            "tr" -> java.util.Locale.Builder().setLanguage("tr").setRegion("TR").build()
            "it" -> java.util.Locale.Builder().setLanguage("it").setRegion("IT").build()
            "ru" -> java.util.Locale.Builder().setLanguage("ru").setRegion("RU").build()
            "vi" -> java.util.Locale.Builder().setLanguage("vi").setRegion("VN").build()
            else -> java.util.Locale.Builder().setLanguage("en").setRegion("US").build()
        }
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Configure Edge-to-Edge with proper system bar colors for Android 15
        // Detect dark mode from system settings
        val isDarkMode = (resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        enableEdgeToEdge(
            statusBarStyle = if (isDarkMode) {
                androidx.activity.SystemBarStyle.dark(
                    android.graphics.Color.TRANSPARENT
                )
            } else {
                androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (isDarkMode) {
                androidx.activity.SystemBarStyle.dark(
                    android.graphics.Color.TRANSPARENT
                )
            } else {
                androidx.activity.SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
        
        // Preload ads (interstitial for free users only)
        // Delayed to prevent Binder calls on main thread during startup
        lifecycleScope.launch {
            delay(2000) // Wait 2 seconds to let UI settle
            com.doyouone.drawai.ads.AdManager.loadInterstitialAd(this@MainActivity)
            com.doyouone.drawai.ads.AdManager.loadRewardedAd(this@MainActivity)
        }
        // App Open Ad is managed by AppOpenAdManager in Application class
        
        // Initialize LocalFavoritesManager
        com.doyouone.drawai.data.local.LocalFavoritesManager.init(this)
        
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val themePreferences = remember { com.doyouone.drawai.data.preferences.ThemePreferences(context) }
            val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
            val savedColorArgb by themePreferences.primaryColorArgb.collectAsState(initial = null)
            
            // Update system bars when theme changes
            androidx.compose.runtime.LaunchedEffect(isDarkMode) {
                // Deprecated in API 35 (Android 15) - Edge-to-Edge enforces transparency
                if (android.os.Build.VERSION.SDK_INT < 35) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                }
                
                // Use WindowCompat for backward compatibility and clean API handling
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    // Update icon colors: 
                    // isAppearanceLightStatusBars = true -> Dark Icons (for Light Background)
                    // isAppearanceLightStatusBars = false -> Light Icons (for Dark Background)
                    
                    if (isDarkMode) {
                        // Dark Mode: We want LIGHT icons
                        isAppearanceLightStatusBars = false
                        isAppearanceLightNavigationBars = false
                    } else {
                        // Light Mode: We want DARK icons
                        isAppearanceLightStatusBars = true
                        isAppearanceLightNavigationBars = true
                    }
                }
            }
            
            // Dynamic Primary Color State - Load from preferences
            var appPrimaryColor by remember { mutableStateOf<androidx.compose.ui.graphics.Color?>(null) }
            
            // Load saved color on first composition
            androidx.compose.runtime.LaunchedEffect(savedColorArgb) {
                savedColorArgb?.let { argb ->
                    appPrimaryColor = androidx.compose.ui.graphics.Color(argb.toInt())
                }
            }
            
            AnimeDrawAITheme(
                darkTheme = isDarkMode,
                customPrimaryColor = appPrimaryColor
            ) {
                AnimeDrawAIApp(
                    themePreferences = themePreferences,
                    onColorChange = { newColor -> 
                        appPrimaryColor = newColor
                        // Save to preferences
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            themePreferences.setPrimaryColor(newColor.toArgb().toLong())
                        }
                    },
                    onPiPAllowed = { allowed -> 
                        shouldEnterPiPMode = allowed 
                    }
                )
            }
        }
    }
}

@Composable
fun AnimeDrawAIApp(
    themePreferences: com.doyouone.drawai.data.preferences.ThemePreferences,
    onColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
    onPiPAllowed: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    
    // PiP Logic: Monitor navigation changes
    DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
            onPiPAllowed(destination.route == Screen.SummoningAnimation.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    val authManager = remember { com.doyouone.drawai.auth.AuthManager(context) }
    
    // Initialize MainViewModel to sync subscription status
    val mainViewModel: com.doyouone.drawai.viewmodel.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val currentUser by authManager.currentUser.collectAsState()
    var showSignUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Google Sign-In launcher (also handles account linking for anonymous users)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("GoogleSignIn", "Activity result received: ${result.resultCode}")
        scope.launch {
            // Check if current user is anonymous (for linking)
            val isAnonymous = currentUser?.isAnonymous == true
            
            val signInResult = if (isAnonymous) {
                android.util.Log.d("GoogleSignIn", "🔗 Linking anonymous account to Google")
                authManager.linkAnonymousToGoogle(result.data)
            } else {
                android.util.Log.d("GoogleSignIn", "📝 Regular Google Sign-In")
                authManager.handleGoogleSignInResult(result.data)
            }
            
            signInResult.onSuccess { user ->
                android.util.Log.d("GoogleSignIn", "✅ Sign-in successful: ${user.email}")
                android.util.Log.d("GoogleSignIn", "User UID: ${user.uid}")
                android.util.Log.d("GoogleSignIn", "Is Anonymous: ${user.isAnonymous}")
                if (isAnonymous) {
                    android.util.Log.d("GoogleSignIn", "🎉 Guest account successfully linked to Google!")
                }
                // Force UI update by re-checking current user
                authManager.refreshCurrentUser()
            }
            signInResult.onFailure { e ->
                android.util.Log.e("GoogleSignIn", "❌ Sign-in failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // Onboarding State
    val isFirstLaunch by themePreferences.isFirstLaunch.collectAsState(initial = null)
    
    if (isFirstLaunch == null) {
        // While loading the preference, show a blank/loading screen to prevent flash
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Optional: Add a localized loading indicator or just keep it blank/logo
        }
        return
    }
    
    if (isFirstLaunch == true) {
        com.doyouone.drawai.ui.screens.OnboardingScreen(
            onFinish = {
                scope.launch {
                    themePreferences.setFirstLaunchCompleted()
                }
            }
        )
        return
    }
    
    if (currentUser == null) {
        if (showSignUp) {
            SignUpScreen(
                onSignUpSuccess = { email ->
                    // Sign up handled in LoginScreen
                    showSignUp = false
                },
                onNavigateToLogin = {
                    showSignUp = false
                },
                onGoogleSignUp = {
                    authManager.getGoogleSignInIntent()?.let { intent ->
                        googleSignInLauncher.launch(intent)
                    }
                },
                authManager = authManager
            )
        } else {
            LoginScreen(
                onLoginSuccess = { email ->
                    // Login handled in LoginScreen
                },
                onGuestLogin = {
                    scope.launch {
                        authManager.signInAnonymously()
                    }
                },
                onGoogleLogin = {
                    authManager.getGoogleSignInIntent()?.let { intent ->
                        googleSignInLauncher.launch(intent)
                    }
                },
                onNavigateToSignUp = {
                    showSignUp = true
                },
                authManager = authManager
            )
        }
    } else {
        MainScreen(
            navController = navController,
            authManager = authManager,
            themePreferences = themePreferences,
            onLogout = {
                authManager.signOut()
            },
            onLinkGoogleAccount = {
                // Trigger Google Sign-In for account linking
                authManager.getGoogleSignInIntent()?.let { intent ->
                    googleSignInLauncher.launch(intent)
                }
            },
            onColorChange = onColorChange // Pass it here
        )
    }
}

/**
 * Maps the current route to the corresponding bottom navigation item route.
 * This ensures the correct bottom nav item is highlighted even when on child screens.
 */
private fun getBottomNavRouteForCurrentRoute(currentRoute: String?): String {
    return when {
        currentRoute == null -> Screen.Home.route
        // Home group - Favorites, Settings, Subscription, Vision, News
        currentRoute == Screen.Favorites.route ||
        currentRoute == Screen.Settings.route ||
        currentRoute == Screen.Subscription.route ||
        currentRoute == Screen.News.route ||
        currentRoute.startsWith("vision") ||
        currentRoute.startsWith("generate/") ||
        currentRoute.startsWith("category/") -> Screen.Home.route
        // Explore group - Post details
        currentRoute == Screen.Explore.route ||
        currentRoute.startsWith("post_detail/") -> Screen.Explore.route
        // Gallery - standalone bottom nav item
        currentRoute == Screen.Gallery.route -> Screen.Gallery.route
        // Chat group - Character chat, Summoning
        currentRoute == Screen.Chat.route ||
        currentRoute.startsWith("character_chat/") ||
        currentRoute.startsWith("summoning/") -> Screen.Chat.route
        // Profile
        currentRoute == Screen.Profile.route -> Screen.Profile.route
        // Default to Home
        else -> Screen.Home.route
    }
}

@Composable
fun MainScreen(
    navController: NavHostController,
    authManager: com.doyouone.drawai.auth.AuthManager,
    themePreferences: com.doyouone.drawai.data.preferences.ThemePreferences,
    onLogout: () -> Unit,
    onLinkGoogleAccount: () -> Unit = {},
    mainViewModel: com.doyouone.drawai.viewmodel.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onColorChange: (androidx.compose.ui.graphics.Color) -> Unit // New Parameter
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Collect user data
    val currentUser by authManager.currentUser.collectAsState()
    val userDisplayName by authManager.userDisplayName.collectAsState()
    val generationLimit by mainViewModel.generationLimit.collectAsState()
    val characterCount by mainViewModel.characterCount.collectAsState()
    val userPreferences = remember { com.doyouone.drawai.data.preferences.UserPreferences(navController.context) }
    val isPremium by userPreferences.isPremium.collectAsState(initial = false)
    
    val userEmail = currentUser?.email ?: if (currentUser?.isAnonymous == true) "Guest" else "User"
    
    // State for showing Coming Soon dialog
    var showComingSoonDialog by remember { mutableStateOf(false) }
    
    // Determine if we should show bottom navigation
    val showBottomNav = when (currentRoute) {
        Screen.Home.route,
        Screen.Gallery.route,
        Screen.Favorites.route,
        Screen.Settings.route,
        Screen.Vision.route,
        Screen.Explore.route,
        Screen.Chat.route,
        Screen.Subscription.route,
        Screen.News.route,
        Screen.Leaderboard.route,
        Screen.Profile.route -> true
        else -> false
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope() 

    // Tutorial Init - Aktifkan untuk pengguna baru
    val isTutorialCompleted by themePreferences.isTutorialCompleted.collectAsState(initial = false)

    
    // TEMPORARY: Force reset tutorial untuk testing
    LaunchedEffect(Unit) {
        android.util.Log.d("MainActivity_Tutorial", "Force resetting tutorial for testing...")
        themePreferences.resetTutorial()
        kotlinx.coroutines.delay(500)
    }
    
    LaunchedEffect(isTutorialCompleted) {
        android.util.Log.d("MainActivity_Tutorial", "Tutorial Status - isTutorialCompleted: $isTutorialCompleted")
        android.util.Log.d("MainActivity_Tutorial", "ShowcaseManager.step: ${com.doyouone.drawai.ui.components.ShowcaseManager.step}")
        
        if (!isTutorialCompleted) {
             // Tunggu lebih lama untuk memastikan UI dan data sudah siap
             kotlinx.coroutines.delay(2500)
             android.util.Log.d("MainActivity_Tutorial", "Starting tutorial...")
             com.doyouone.drawai.ui.components.ShowcaseManager.startTutorial(context)
             android.util.Log.d("MainActivity_Tutorial", "Tutorial started. Step: ${com.doyouone.drawai.ui.components.ShowcaseManager.step}")
        } else {
             android.util.Log.d("MainActivity_Tutorial", "Tutorial already completed, skipping")
        }
    }
    
    // Auto-close drawer when entering DrawToImageScreen
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.DrawToImage.route && drawerState.isOpen) {
            drawerState.close()
        }
    }
    
    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != Screen.DrawToImage.route, // Disable drawer gesture in PhotoEditor
        drawerContent = {
            com.doyouone.drawai.ui.components.AppDrawer(
                currentRoute = currentRoute ?: Screen.Home.route,
                onNavigate = { route ->
                    // ... [Navigation Logic] ...
                    when (route) {
                        BottomNavItem.Home.route -> {
                             navController.navigate(Screen.Home.route) {
                                 popUpTo(Screen.Home.route) { inclusive = false }
                                 launchSingleTop = true
                             }
                         }
                        // ... [Other routes same as before] ...
                         Screen.Gallery.route -> {
                            navController.navigate(Screen.Gallery.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        Screen.Favorites.route -> {
                            navController.navigate(Screen.Favorites.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        Screen.Settings.route -> {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        Screen.Vision.route -> {
                            navController.navigate(Screen.Vision.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        BottomNavItem.Explore.route -> {
                            navController.navigate(Screen.Explore.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        BottomNavItem.Chat.route -> {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        Screen.News.route -> {
                            navController.navigate(Screen.News.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        Screen.Inventory.route -> {
                            navController.navigate(Screen.Inventory.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                        Screen.Leaderboard.route -> {
                            navController.navigate(Screen.Leaderboard.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                    scope.launch {
                        drawerState.close()
                    }
                },
                onClose = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                userEmail = userEmail,
                userDisplayName = userDisplayName,
                generationLimit = generationLimit,
                characterCount = characterCount,
                isPremium = isPremium,
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                    scope.launch {
                        drawerState.close()
                    }
                },
                onColorSelected = onColorChange // Pass it here
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomNav) {
                    // Map current route to bottom nav item route
                    val bottomNavRoute = getBottomNavRouteForCurrentRoute(currentRoute)
                    val isMoreEnabled = generationLimit?.moreEnabled == true
                    
                    AnimatedBottomNavigation(
                        selectedItem = bottomNavRoute,
                        onItemSelected = { route ->
                            android.util.Log.d("BottomNav", "🔘 Clicked: $route, Current: $currentRoute, Mapped: $bottomNavRoute")
                            // Always allow navigation if different from current route
                            if (route != currentRoute) {
                                android.util.Log.d("BottomNav", "✅ Navigating to: $route")
                                navController.navigate(route) {
                                    // Clear back stack up to home, but keep home
                                    popUpTo(Screen.Home.route) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            } else {
                                android.util.Log.d("BottomNav", "❌ Blocked: already on $route")
                            }
                        },
                        hideExplore = false
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    val userId = authManager.getCurrentUserId() ?: ""
                    val viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    HomeScreen(
                        onWorkflowSelected = { workflowId ->
                            viewModel.incrementWorkflowView(workflowId) // Track view
                            navController.navigate(Screen.Generate.createRoute(workflowId))
                        },
                        onNavigateToCategory = { category ->
                            navController.navigate("category/$category")
                        },
                        onNavigateToSubscription = {
                            navController.navigate(Screen.Subscription.route)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onNavigateToGallery = {
                            navController.navigate(Screen.Gallery.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        userId = userId,
                        onOpenDrawer = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onNavigateToChat = {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onNavigateToBackgroundRemover = {
                            navController.navigate(Screen.BackgroundRemover.route)
                        },
                        onNavigateToBackgroundRemoverAdvanced = {
                            navController.navigate(Screen.BackgroundRemoverAdvanced.route)
                        },
                        onNavigateToUpscaleImage = {
                            navController.navigate(Screen.UpscaleImage.route)
                        },
                        onNavigateToMakeBackground = {
                            navController.navigate(Screen.MakeBackground.route)
                        },
                        onNavigateToMakeBackgroundAdvanced = {
                            navController.navigate(Screen.MakeBackgroundAdvanced.route)
                        },
                        onNavigateToFaceRestore = {
                            navController.navigate(Screen.FaceRestore.route)
                        },
                        onNavigateToSketchToImage = {
                            navController.navigate(Screen.SketchToImage.route)
                        },
                        onNavigateToDrawToImage = {
                            navController.navigate(Screen.DrawToImage.route)
                        }
                    )
                }
            
            composable("category/{categoryType}") { backStackEntry ->
                val categoryType = backStackEntry.arguments?.getString("categoryType") ?: ""
                val viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val workflowsMap by viewModel.workflows.collectAsState()
                val workflowStats by viewModel.workflowStats.collectAsState() // Observe stats
                
                // Convert to workflow list
                val allWorkflows = workflowsMap.map { (id, info) ->
                    val stats = workflowStats[id]
                    val viewCount = stats?.get("viewCount") ?: 0L
                    val generationCount = stats?.get("generationCount") ?: 0L
                    
                    com.doyouone.drawai.data.model.Workflow(
                        id = id,
                        name = info.name,
                        description = info.description,
                        estimatedTime = info.estimatedTime,
                        category = when {
                            id.contains("anime", ignoreCase = true) -> "Anime"
                            id.contains("animal", ignoreCase = true) -> "Animal"
                            id.contains("flower", ignoreCase = true) -> "Flower"
                            id.contains("background", ignoreCase = true) -> "Background"
                            id.contains("food", ignoreCase = true) -> "Food"
                            else -> "General"
                        },
                        isFavorite = false,
                        isPremium = info.isPremium,
                        viewCount = viewCount,
                        generationCount = generationCount
                    )
                }.toList()
                
                // Filter based on category
                val categoryWorkflows = when (categoryType) {
                    "most_viewed" -> allWorkflows.sortedByDescending { it.viewCount } // Real views
                    "newest" -> allWorkflows.sortedBy { it.generationCount } // Newest = Low generation count
                    "most_popular" -> allWorkflows.sortedByDescending { it.generationCount } // Real generation count
                    else -> allWorkflows
                }
                
                CategoryDetailScreen(
                    category = categoryType,
                    workflows = categoryWorkflows,
                    onWorkflowSelected = { workflowId ->
                        viewModel.incrementWorkflowView(workflowId)
                        navController.navigate(Screen.Generate.createRoute(workflowId))
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    userId = authManager.getCurrentUserId() ?: ""
                )
            }
            
            composable(
                route = "generate/{workflowId}?vision={vision}&avoid={avoid}",
                arguments = listOf(
                    androidx.navigation.navArgument("workflowId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("vision") { type = androidx.navigation.NavType.StringType; nullable = true },
                    androidx.navigation.navArgument("avoid") { type = androidx.navigation.NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val workflowId = backStackEntry.arguments?.getString("workflowId") ?: ""
                val visionArg = backStackEntry.arguments?.getString("vision")?.let { android.net.Uri.decode(it) }
                val avoidArg = backStackEntry.arguments?.getString("avoid")?.let { android.net.Uri.decode(it) }
                val savedVisionFlow = navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("vision", null)
                val savedAvoidFlow = navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("avoid", null)
                val savedVision by savedVisionFlow?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
                val savedAvoid by savedAvoidFlow?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
                val vision = savedVision ?: visionArg
                val avoid = savedAvoid ?: avoidArg
                val context = androidx.compose.ui.platform.LocalContext.current
                val activity = context as? android.app.Activity
                val userPreferences = remember { com.doyouone.drawai.data.preferences.UserPreferences(context) }
                val isPremium by userPreferences.isPremium.collectAsState(initial = false)
                
                // Get generation limit to check subscription type
                val viewModel: com.doyouone.drawai.viewmodel.GenerateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val generationLimit by viewModel.generationLimit.collectAsState()
                
                // Load generation limit and ensure Ad is loaded
                LaunchedEffect(Unit) {
                    viewModel.loadGenerationLimit()
                    // Ensure Ad is loaded for potential display
                    if (!com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                         android.util.Log.d("MainActivity", "⚠️ Interstitial ad not ready, reloading...")
                         com.doyouone.drawai.ads.AdManager.loadInterstitialAd(context)
                    }
                }
                
                // Determine if ads should be shown using new helper
                val shouldShowAds = remember(isPremium, generationLimit) {
                    com.doyouone.drawai.utils.AdDisplayHelper.shouldShowAds(isPremium, generationLimit)
                }
                
                GenerateScreen(
                    workflowId = workflowId,
                    viewModel = viewModel, // Pass the existing ViewModel instance
                    onLinkGoogleAccount = onLinkGoogleAccount,
                    onGenerateComplete = {
                        android.util.Log.d("MainActivity", "🎉 Generation Complete. ShouldShowAds: $shouldShowAds, IsPremium: $isPremium")
                        
                        // Show interstitial ad for free users only
                        if (shouldShowAds && activity != null) {
                            if (com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                                android.util.Log.d("MainActivity", "🎬 Showing Interstitial Ad before Gallery")
                                com.doyouone.drawai.ads.AdManager.showInterstitialAd(activity) {
                                    // Navigate to Gallery after ad is dismissed
                                    navController.navigate(Screen.Gallery.route) {
                                        popUpTo(Screen.Home.route) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                android.util.Log.w("MainActivity", "⚠️ Ad supposed to show but NOT READY. Skipping to Gallery.")
                                // Fallback: Navigate anyway
                                navController.navigate(Screen.Gallery.route) {
                                    popUpTo(Screen.Home.route) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                                // Try reloading for next time
                                com.doyouone.drawai.ads.AdManager.loadInterstitialAd(context)
                            }
                        } else {
                            android.util.Log.d("MainActivity", "⏩ Skipping Ad (Premium/Subscribed). Navigating to Gallery.")
                            // Basic/Pro users - navigate directly to Gallery (no ads)
                            navController.navigate(Screen.Gallery.route) {
                                popUpTo(Screen.Home.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navController.popBackStack(Screen.Home.route, inclusive = false)

                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onNavigateToVision = { selectedWorkflowId ->
                        navController.navigate(Screen.Vision.createRoute(selectedWorkflowId, "workflow"))
                    },
                    vision = vision,
                    avoid = avoid,
                    currentRoute = currentRoute ?: "generate"
                )
            }
            
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onNavigateToGenerate = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToGenerateWithWorkflow = { workflowId ->
                        navController.navigate(Screen.Generate.createRoute(workflowId))
                    },
                    authManager = authManager,
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onNavigateToCharacterChat = { characterId ->
                        navController.navigate(Screen.CharacterChat.createRoute(characterId))
                    },
                    onNavigateToSummoningAnimation = { characterId, imageUrl, sinCount, rarity ->
                        navController.navigate(Screen.SummoningAnimation.createRoute(characterId, imageUrl, sinCount, rarity))
                    },
                    characterCount = characterCount,
                    maxChatLimit = generationLimit?.maxChatLimit ?: if (isPremium) 5 else 1,
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Gallery.route)
                        }
                    },
                    isPremium = isPremium
                )
            }
            
            composable(Screen.Inventory.route) {
                com.doyouone.drawai.ui.screens.InventoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.BackgroundRemover.route) {
                com.doyouone.drawai.ui.screens.BackgroundRemoverScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Gallery.route)
                        }
                    }
                )
            }
            
            composable(Screen.BackgroundRemoverAdvanced.route) {
                com.doyouone.drawai.ui.screens.BackgroundRemoverAdvancedScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Gallery.route)
                        }
                    }
                )
            }
            
            composable(Screen.FaceRestore.route) {
                com.doyouone.drawai.ui.screens.FaceRestoreScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Gallery.route)
                        }
                    }
                )
            }
            
            composable(Screen.SketchToImage.route) {
                com.doyouone.drawai.ui.screens.SketchToImageScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Gallery.route)
                        }
                    }
                )
            }

            composable(Screen.DrawToImage.route) {
                com.doyouone.drawai.ui.screens.DrawToImageScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Gallery.route)
                        }
                    }
                )
            }
            
            composable(Screen.MakeBackground.route) {
                com.doyouone.drawai.ui.screens.MakeBackgroundScreen(
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            // Pop up to home to avoid back stack issues
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.MakeBackgroundAdvanced.route) {
                com.doyouone.drawai.ui.screens.MakeBackgroundAdvancedScreen(
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.UpscaleImage.route) {
                com.doyouone.drawai.ui.screens.UpscaleImageScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route) {
                            popUpTo(Screen.Gallery.route)
                        }
                    }
                )
            }
            
            composable(
                route = Screen.CharacterChat.route,
                arguments = listOf(
                    androidx.navigation.navArgument("characterId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
                com.doyouone.drawai.ui.screens.CharacterChatScreen(
                    characterId = characterId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    }
                )
            }
            
            composable(
                route = Screen.SummoningAnimation.route,
                arguments = listOf(
                    androidx.navigation.navArgument("characterId") {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("imageUrl") {
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("sinCount") {
                        type = androidx.navigation.NavType.IntType
                    },
                    androidx.navigation.navArgument("rarity") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
                val encodedImageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                val imageUrl = java.net.URLDecoder.decode(encodedImageUrl, "UTF-8")
                val sinCount = backStackEntry.arguments?.getInt("sinCount") ?: 1
                val encodedRarity = backStackEntry.arguments?.getString("rarity") ?: "Common"
                val rarity = java.net.URLDecoder.decode(encodedRarity, "UTF-8")
                
                com.doyouone.drawai.ui.screens.SummoningAnimationScreen(
                    characterImageUrl = imageUrl,
                    sinCount = sinCount,
                    rarity = rarity,
                    onAnimationComplete = {
                        // Navigate to character chat after animation
                        navController.navigate(Screen.CharacterChat.createRoute(characterId)) {
                            popUpTo(Screen.Gallery.route)
                        }
                    }
                )
            }
            
            composable(Screen.Favorites.route) {
                val userId = authManager.getCurrentUserId() ?: ""
                FavoritesScreen(
                    onWorkflowSelected = { workflowId ->
                        navController.navigate(Screen.Generate.createRoute(workflowId))
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    userId = userId,
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    themePreferences = themePreferences,
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onNavigateToNews = {
                        navController.navigate(Screen.News.route)
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    }
                )
            }
            composable(Screen.Vision.route) { backStackEntry ->
                val workflowId = backStackEntry.arguments?.getString("workflowId")
                val source = backStackEntry.arguments?.getString("source")
                com.doyouone.drawai.ui.screens.VisionScreen(
                    workflowId = workflowId,
                    source = source,
                    onNavigateToWorkflow = { selectedWorkflowId, vision, avoid ->
                        // Prefer passing data via savedStateHandle for reliability
                        navController.previousBackStackEntry?.savedStateHandle?.set("vision", vision)
                        navController.previousBackStackEntry?.savedStateHandle?.set("avoid", avoid)
                        // Return to Generate screen
                        navController.popBackStack()
                    },
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
            
            composable(Screen.Subscription.route) {
                val userId = authManager.getCurrentUserId() ?: ""
                SubscriptionScreen(
                    userId = userId,
                    onBackPressed = {
                        // Always navigate to Home (discovered workflow menu) instead of previous screen
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onSubscribed = {
                        // Navigate to Home after successful subscription
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.Explore.route) {
                ExploreScreen(
                    onPostClick = { post ->
                        // Navigate with post ID only
                        navController.navigate(Screen.PostDetail.createRoute(post.id))
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    }
                )
            }
            
            composable(
                route = Screen.PostDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("postId") { 
                        type = androidx.navigation.NavType.StringType 
                    }
                )
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                val context = androidx.compose.ui.platform.LocalContext.current
                val viewModel: com.doyouone.drawai.viewmodel.CommunityViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return com.doyouone.drawai.viewmodel.CommunityViewModel(context) as T
                            }
                        }
                    )
                
                // Load post when screen is created
                LaunchedEffect(postId) {
                    android.util.Log.d("MainActivity", "Loading post with ID: $postId")
                    viewModel.getPostById(postId)
                }
                
                val post by viewModel.currentPost.collectAsState()
                
                when {
                    post == null -> {
                        // Loading state
                        Box(
                            modifier = Modifier.fillMaxSize(), 
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = com.doyouone.drawai.ui.theme.AccentPurple)
                        }
                    }
                    else -> {
                        PostDetailScreen(
                            post = post!!,
                            viewModel = viewModel,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
            
            composable(Screen.Chat.route) {
                com.doyouone.drawai.ui.screens.CharacterListScreen(
                    onCharacterClick = { characterId ->
                        navController.navigate(Screen.CharacterChat.createRoute(characterId))
                    },
                    onNewCharacterClick = {
                        // Navigate to Gallery to create new character
                        navController.navigate(Screen.Gallery.route)
                    },
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    maxChatLimit = generationLimit?.maxChatLimit ?: if (isPremium) 5 else 1
                )
            }
            
            composable(Screen.Leaderboard.route) {
                val viewModel: com.doyouone.drawai.ui.viewmodel.LeaderboardViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return com.doyouone.drawai.ui.viewmodel.LeaderboardViewModel(
                                    com.doyouone.drawai.data.repository.LeaderboardRepository()
                                ) as T
                            }
                        }
                    )
                com.doyouone.drawai.ui.screens.LeaderboardScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable(Screen.News.route) {
                com.doyouone.drawai.ui.screens.NewsScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            
            composable(Screen.Profile.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                com.doyouone.drawai.ui.screens.ProfileScreen(
                    onLogout = {
                        scope.launch {
                            // Clear Gallery Cache
                            @OptIn(coil.annotation.ExperimentalCoilApi::class)
                            context.imageLoader.memoryCache?.clear()
                            @OptIn(coil.annotation.ExperimentalCoilApi::class)
                            context.imageLoader.diskCache?.clear()
                            
                            authManager.signOut()
                        }
                    },
                    authManager = authManager,
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onNavigateToUsageStats = {
                        // TODO: Create Usage Stats Screen
                        // For now, show a toast
                        android.widget.Toast.makeText(context, "Usage Stats coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onOpenDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        }
        }
    }
    
    com.doyouone.drawai.ui.components.ShowcaseOverlay(
        onTutorialComplete = {
            scope.launch {
                themePreferences.setTutorialCompleted()
            }
        }
    )
    }

    // Coming Soon Dialog
    if (showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = false },
            title = { Text("Coming Soon") },
            text = { Text("The User Published feature will be available soon!\n\nIn the meantime, you'll be redirected to the workflow to get started.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showComingSoonDialog = false
                        // Navigate to workflow (Home) after closing dialog
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showComingSoonDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}