package com.doyouone.drawai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import android.graphics.BitmapFactory
import java.io.File
import com.doyouone.drawai.ui.components.ProcessingModal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import com.doyouone.drawai.R
import com.doyouone.drawai.data.repository.SubscriptionRepository
import com.doyouone.drawai.data.model.SubscriptionPlan
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeBackgroundAdvancedScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // State
    var visionPrompt by remember { mutableStateOf("") }
    var seed by remember { mutableLongStateOf(Random.nextLong(100000000000000L, 999999999999999L)) }
    
    // Advanced State
    var selectedCheckpoint by remember { mutableStateOf("wildcardxXLTURBO_wildcardxXLTURBOV10.safetensors") }
    var customWidth by remember { mutableStateOf("1280") }
    var customHeight by remember { mutableStateOf("720") }
    
    var steps by remember { mutableFloatStateOf(20f) }
    var cfg by remember { mutableFloatStateOf(7f) }
    var selectedSampler by remember { mutableStateOf("euler_ancestral") }
    var selectedScheduler by remember { mutableStateOf("normal") }
    var denoise by remember { mutableFloatStateOf(1f) }
    
    var selectedUpscaleMethod by remember { mutableStateOf("nearest-exact") }
    
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var blockedWord by remember { mutableStateOf("") }
    
    // Subscription State
    var userSubscription by remember { mutableStateOf<com.doyouone.drawai.data.model.UserSubscription?>(null) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    // Derived state: calculate premium status based on plan name
    val isPremium = userSubscription != null && 
                   (userSubscription?.plan == SubscriptionPlan.BASIC || userSubscription?.plan == SubscriptionPlan.PRO)
    
    // Load Subscription
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
             try {
                // SubscriptionRepository constructor requires userId
                SubscriptionRepository(userId).getSubscription().collect { sub ->
                    userSubscription = sub
                }
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }
    }
    
    // Options
    val checkpointOptions = listOf(
        "BSSEquinoxILSemi_v30.safetensors",
        "miaomiaoPixel_vPred11.safetensors",
        "akashicpulse_v41.safetensors",
        "ramthrustsNSFWPINK_alchemyMix176.safetensors",
        "astreapixieRadiance_v16.safetensors",
        "catCarrier_v70.safetensors",
        "animeCelestialMagic_v30.safetensors",
        "animeChangefulXL_v10ReleasedCandidate.safetensors",
        "animecollection_v420.safetensors",
        "corepomMIX2_sakuraMochi.safetensors",
        "dollamor_v10.safetensors",
        "flatAnimix_v20.safetensors",
        "floraEclipse_v10.safetensors",
        "hotaruBlend_vPredV20.safetensors",
        "hyperpixel_v10.safetensors",
        "lakeLakeSeriesOfModels_renoLakeK1S.safetensors",
        "lizmix_version18.safetensors",
        "lilithsLullaby_v10.safetensors",
        "endlustriaLUMICA_v2.safetensors",
        "meinamix_meinaV11.safetensors",
        "mritualIllustrious_v20.safetensors",
        "novaAnimeXL_ilV125.safetensors",
        "nova3DCGXL_ilV70.safetensors",
        "novaFurryXL_ilV120.safetensors",
        "novaMatureXL_v35.safetensors",
        "novaOrangeXL_reV30.safetensors",
        "novaRetroXL_v20.safetensors",
        "novaUnrealXL_v100.safetensors",
        "scyraxPastelCore_v10.safetensors",
        "perfectdeliberate_v20A.safetensors",
        "wildcardxXLTURBO_wildcardxXLTURBOV10.safetensors",
        "PVCStyleModelMovable_epsIll11.safetensors",
        "AnimeRealPantheon_k1ssBakedvae.safetensors",
        "redLilyIllu_v10.safetensors",
        "silenceMix_v7.safetensors",
        "softMixKR_v23.safetensors",
        "3dStock3dAnimeStyle_v30.safetensors",
        "tinyNovaMerge_v25.safetensors",
        "trattoNero_vitta.safetensors",
        "dvine_v60.safetensors",
        "xeHentaiAnimePDXL_02.safetensors",
        "asianBlendIllustrious_v10.safetensors",
        "asianBlendPDXLPony_v1.safetensors",
        "margaretAsianWomanPony_v10.safetensors",
        "ponyAsianRealismAlpha_v05.safetensors",
        "iniverseMixSFWNSFW_realXLV1.safetensors",
        "intorealismUltra_v10.safetensors",
        "novaRealityXL_ilV901.safetensors",
        "realcosplay_realchenkinv10.safetensors",
        "ultra_v11.safetensors"
    )
    
    val samplerOptions = listOf(
        "euler", "euler_ancestral", "heun", "dpm_2", "dpm_2_ancestral",
        "lms", "dpm_fast", "dpm_adaptive", "dpmpp_2s_ancestral", 
        "dpmpp_sde", "dpmpp_sde_gpu", "dpmpp_2m", "dpmpp_2m_sde", 
        "dpmpp_2m_sde_gpu", "dpmpp_3m_sde", "dpmpp_3m_sde_gpu", 
        "ddpm", "lcm"
    )
    
    val schedulerOptions = listOf(
        "normal", "karras", "exponential", "sgm_uniform", "simple", "ddim_uniform"
    )
    
    val upscaleMethodOptions = listOf(
        "nearest-exact", "bilinear", "area", "bicubic", "lanczos"
    )
    
    // Dropdown States
    var expandedCheckpoint by remember { mutableStateOf(false) }
    var expandedSampler by remember { mutableStateOf(false) }
    var expandedScheduler by remember { mutableStateOf(false) }
    var expandedUpscale by remember { mutableStateOf(false) }
    
    // Load Interstitial Ad
    LaunchedEffect(Unit) {
        com.doyouone.drawai.ads.AdManager.loadInterstitialAd(context)
    }
    
    // Checkpoint Friendly Names
    val checkpointFriendlyNames = mapOf(
        "BSSEquinoxILSemi_v30.safetensors" to "Animal Equinox",
        "miaomiaoPixel_vPred11.safetensors" to "Anime Ah Pixel",
        "akashicpulse_v41.safetensors" to "Anime Akashic Pulse",
        "ramthrustsNSFWPINK_alchemyMix176.safetensors" to "Anime Alchemy Mix",
        "astreapixieRadiance_v16.safetensors" to "Anime Astrea Pixie Radiance",
        "catCarrier_v70.safetensors" to "Anime Cat Carrier",
        "animeCelestialMagic_v30.safetensors" to "Anime Celestial Magic",
        "animeChangefulXL_v10ReleasedCandidate.safetensors" to "Anime Change Full Perform",
        "animecollection_v420.safetensors" to "Anime Collection",
        "corepomMIX2_sakuraMochi.safetensors" to "Anime Corepom Mix 2",
        "dollamor_v10.safetensors" to "Anime Doll Doll",
        "flatAnimix_v20.safetensors" to "Anime Flat Animix",
        "floraEclipse_v10.safetensors" to "Anime Flora Eclipse",
        "hotaruBlend_vPredV20.safetensors" to "Anime Hotaru Blend",
        "hyperpixel_v10.safetensors" to "Anime Hyper Pixel",
        "lakeLakeSeriesOfModels_renoLakeK1S.safetensors" to "Anime Lake Reno Series",
        "lizmix_version18.safetensors" to "Anime Lizmix",
        "lilithsLullaby_v10.safetensors" to "Anime Lulaby",
        "endlustriaLUMICA_v2.safetensors" to "Anime Lumica Illusion",
        "meinamix_meinaV11.safetensors" to "Anime Main Mix",
        "mritualIllustrious_v20.safetensors" to "Anime Mritual Ilustrious",
        "novaAnimeXL_ilV125.safetensors" to "Anime Nova",
        "nova3DCGXL_ilV70.safetensors" to "Anime Nova 3D",
        "novaFurryXL_ilV120.safetensors" to "Anime Nova Furry",
        "novaMatureXL_v35.safetensors" to "Anime Nova Mature",
        "novaOrangeXL_reV30.safetensors" to "Anime Nova Orange",
        "novaRetroXL_v20.safetensors" to "Anime Nova Retro",
        "novaUnrealXL_v100.safetensors" to "Anime Nova Unreal",
        "scyraxPastelCore_v10.safetensors" to "Anime Pastel Core",
        "perfectdeliberate_v20A.safetensors" to "Anime Perfect Deliberate",
        "wildcardxXLTURBO_wildcardxXLTURBOV10.safetensors" to "Anime Premium Ultra",
        "PVCStyleModelMovable_epsIll11.safetensors" to "Anime PVC Moveable",
        "AnimeRealPantheon_k1ssBakedvae.safetensors" to "Anime Real Pantheon",
        "redLilyIllu_v10.safetensors" to "Anime Red Lily",
        "silenceMix_v7.safetensors" to "Anime Silence Mix",
        "softMixKR_v23.safetensors" to "Anime Soft Mix",
        "3dStock3dAnimeStyle_v30.safetensors" to "Anime Stock Style",
        "tinyNovaMerge_v25.safetensors" to "Anime Tiny Merge",
        "trattoNero_vitta.safetensors" to "Anime Tratto Nero",
        "dvine_v60.safetensors" to "Anime Well Vell",
        "xeHentaiAnimePDXL_02.safetensors" to "Anime Xe",
        "asianBlendIllustrious_v10.safetensors" to "General Asia Blend Illustrious",
        "asianBlendPDXLPony_v1.safetensors" to "General Asia Blend Pony",
        "margaretAsianWomanPony_v10.safetensors" to "General Asia Margaret Pony",
        "ponyAsianRealismAlpha_v05.safetensors" to "General Asia Realism Alpha",
        "iniverseMixSFWNSFW_realXLV1.safetensors" to "General Iniverse Mix",
        "intorealismUltra_v10.safetensors" to "General Intorealism Ultra",
        "novaRealityXL_ilV901.safetensors" to "General Nova Reality",
        "realcosplay_realchenkinv10.safetensors" to "General Real Cosplay",
        "ultra_v11.safetensors" to "General Ultra"
    )

    val appPreferences = remember { com.doyouone.drawai.data.preferences.AppPreferences(context) }
    val isRestrictedContentEnabled by appPreferences.isRestrictedContentEnabled.collectAsState(initial = false)

    val filteredCheckpointOptions = remember(isRestrictedContentEnabled) {
        if (isRestrictedContentEnabled) {
            checkpointOptions
        } else {
            checkpointOptions.filter { 
                !it.contains("nsfw", ignoreCase = true) && 
                !it.contains("hentai", ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Advanced Background",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Full Control • V2 Workflow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Image
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                AsyncImage(
                    model = "https://drawai-api.drawai.site/workflow-image/make_background_v2",
                    contentDescription = "Make Background Advanced Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Generate beautiful anime-style backgrounds with advanced controls. Custom Dimensions, Workflows, and Samplers.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )
            
            // Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Model & Prompt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    // Workflow
                    Text("Workflows", style = MaterialTheme.typography.labelMedium)
                    Box {
                        ExposedDropdownMenuBox(
                            expanded = expandedCheckpoint && isPremium,
                            onExpandedChange = { 
                                if (isPremium) expandedCheckpoint = !expandedCheckpoint 
                                else showUpgradeDialog = true
                            }
                        ) {
                            OutlinedTextField(
                                value = checkpointFriendlyNames[selectedCheckpoint] ?: selectedCheckpoint,
                                onValueChange = {},
                                readOnly = true,
                                enabled = isPremium,
                                leadingIcon = {
                                    val friendlyName = checkpointFriendlyNames[selectedCheckpoint] ?: selectedCheckpoint
                                    // Construct simpler ID for image URL (e.g. "Anime Premium Ultra" -> "anime_premium_ultra")
                                    val imageId = friendlyName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")
                                    AsyncImage(
                                        model = "${com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl()}workflow-image/$imageId",
                                        contentDescription = null,
                                        modifier = Modifier.padding(8.dp).size(24.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                },
                                trailingIcon = { 
                                    if (isPremium) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCheckpoint)
                                    else Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                            if (isPremium) {
                                ExposedDropdownMenu(
                                    expanded = expandedCheckpoint,
                                    onDismissRequest = { expandedCheckpoint = false }
                                ) {
                                    filteredCheckpointOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                     val friendlyName = checkpointFriendlyNames[option] ?: option
                                                     val imageId = friendlyName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")
                                                     AsyncImage(
                                                         model = "${com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl()}workflow-image/$imageId",
                                                         contentDescription = null,
                                                         modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                                         contentScale = ContentScale.Crop
                                                     )
                                                     Spacer(Modifier.width(12.dp))
                                                     Text(friendlyName)
                                                }
                                            },
                                            onClick = {
                                                selectedCheckpoint = option
                                                expandedCheckpoint = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (!isPremium) Box(modifier = Modifier.matchParentSize().clickable { showUpgradeDialog = true })
                    }

                    Spacer(Modifier.height(16.dp))

                    // Text Prompt
                    OutlinedTextField(
                        value = visionPrompt,
                        onValueChange = { visionPrompt = it },
                        label = { Text("Vision Prompt") },
                        placeholder = { Text("e.g. A futuristic city...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            
            // Dimensions
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AspectRatio, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Dimensions (Max 4K)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = customWidth,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() }) {
                                    val newVal = it.toIntOrNull() ?: 0
                                    if (!isPremium && newVal > 1024) {
                                        showUpgradeDialog = true
                                        customWidth = "1024"
                                    } else {
                                        customWidth = it 
                                    }
                                }
                            },
                            label = { Text("Width" + if (!isPremium) " (Max 1024)" else "") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = if (!isPremium && (customWidth.toIntOrNull() ?: 0) >= 1024) {
                                { Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.primary) }
                            } else null
                        )
                        OutlinedTextField(
                            value = customHeight,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() }) {
                                    val newVal = it.toIntOrNull() ?: 0
                                    if (!isPremium && newVal > 1024) {
                                        showUpgradeDialog = true
                                        customHeight = "1024"
                                    } else {
                                        customHeight = it
                                    }
                                }
                            },
                            label = { Text("Height" + if (!isPremium) " (Max 1024)" else "") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = if (!isPremium && (customHeight.toIntOrNull() ?: 0) >= 1024) {
                                { Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.primary) }
                            } else null
                        )
                    }
                }
            }

            // KSampler & Advanced
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Advanced Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Steps
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Steps: ${steps.toInt()}", style = MaterialTheme.typography.labelMedium)
                        if (!isPremium) Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(14.dp).padding(start=4.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        Slider(
                             value = steps,
                             onValueChange = { if (isPremium) steps = it else showUpgradeDialog = true },
                             valueRange = 1f..100f,
                             steps = 99,
                             enabled = true // Handle click via Box overlay
                        )
                        if (!isPremium) Box(modifier = Modifier.matchParentSize().clickable { showUpgradeDialog = true })
                    }
                    
                    // CFG
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CFG Scale: %.1f".format(cfg), style = MaterialTheme.typography.labelMedium)
                        if (!isPremium) Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(14.dp).padding(start=4.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        Slider(
                            value = cfg,
                            onValueChange = { if (isPremium) cfg = it else showUpgradeDialog = true },
                            valueRange = 1f..20f,
                            steps = 38,
                            enabled = true
                        )
                        if (!isPremium) Box(modifier = Modifier.matchParentSize().clickable { showUpgradeDialog = true })
                    }
                    
                    // Denoise
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Denoise: %.2f".format(denoise), style = MaterialTheme.typography.labelMedium)
                        if (!isPremium) Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(14.dp).padding(start=4.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        Slider(
                            value = denoise,
                            onValueChange = { if (isPremium) denoise = it else showUpgradeDialog = true },
                            valueRange = 0f..1f,
                            enabled = true
                        )
                        if (!isPremium) Box(modifier = Modifier.matchParentSize().clickable { showUpgradeDialog = true })
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Sampler
                    Text("Sampler", style = MaterialTheme.typography.labelMedium)
                    Box {
                        ExposedDropdownMenuBox(
                            expanded = expandedSampler,
                            onExpandedChange = { 
                                if (isPremium) expandedSampler = !expandedSampler 
                                else showUpgradeDialog = true
                            }
                        ) {
                            OutlinedTextField(
                                value = selectedSampler,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { 
                                    if (isPremium) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSampler)
                                    else Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (isPremium) {
                                ExposedDropdownMenu(
                                    expanded = expandedSampler,
                                    onDismissRequest = { expandedSampler = false }
                                ) {
                                    samplerOptions.forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = { selectedSampler = option; expandedSampler = false })
                                    }
                                }
                            }
                        }
                        if (!isPremium) Box(modifier = Modifier.matchParentSize().clickable { showUpgradeDialog = true })
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Scheduler
                    Text("Scheduler", style = MaterialTheme.typography.labelMedium)
                    Box {
                        ExposedDropdownMenuBox(
                             expanded = expandedScheduler,
                             onExpandedChange = { 
                                 if (isPremium) expandedScheduler = !expandedScheduler 
                                 else showUpgradeDialog = true
                             }
                        ) {
                             OutlinedTextField(
                                value = selectedScheduler,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { 
                                    if (isPremium) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScheduler)
                                    else Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (isPremium) {
                                ExposedDropdownMenu(
                                    expanded = expandedScheduler,
                                    onDismissRequest = { expandedScheduler = false }
                                ) {
                                    schedulerOptions.forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = { selectedScheduler = option; expandedScheduler = false })
                                    }
                                }
                             }
                        }
                        if (!isPremium) Box(modifier = Modifier.matchParentSize().clickable { showUpgradeDialog = true })
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Seed
                    Text("Seed", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = seed.toString(),
                            onValueChange = { seed = it.toLongOrNull() ?: seed },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = { seed = Random.nextLong(100000000000000L, 999999999999999L) },
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        ) { Icon(Icons.Default.Refresh, null) }
                    }
                }
            }
            
            // Upscale Method
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Upscale Method", style = MaterialTheme.typography.labelMedium)
                    Box {
                        ExposedDropdownMenuBox(
                            expanded = expandedUpscale,
                            onExpandedChange = { 
                                if (isPremium) expandedUpscale = !expandedUpscale 
                                else showUpgradeDialog = true
                            }
                        ) {
                            OutlinedTextField(
                                value = selectedUpscaleMethod,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { 
                                    if (isPremium) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUpscale)
                                    else Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true),
                                enabled = true, // Keep enabled to allow clicks for dialog
                                colors = if (!isPremium) OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ) else OutlinedTextFieldDefaults.colors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (isPremium) {
                                ExposedDropdownMenu(
                                    expanded = expandedUpscale,
                                    onDismissRequest = { expandedUpscale = false }
                                ) {
                                    upscaleMethodOptions.forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = { selectedUpscaleMethod = option; expandedUpscale = false })
                                    }
                                }
                            }
                        }
                        if (!isPremium) {
                            Box(modifier = Modifier.matchParentSize().clickable { showUpgradeDialog = true })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Generate Button (Hidden when generating)
            if (!isGenerating) {
                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            errorMessage = null
                            
                            // 1. Validation
                            val w = customWidth.toIntOrNull() ?: 1280
                            val h = customHeight.toIntOrNull() ?: 720
                            
                            // Premium Check: Lock dimensions to 1024 for free users
                            if (!isPremium && (w > 1024 || h > 1024)) {
                                showUpgradeDialog = true
                                isGenerating = false
                                return@launch
                            }

                            if (w > 4096 || h > 4096) {
                                errorMessage = "Max dimension is 4096"
                                isGenerating = false
                                return@launch
                            }

                            // 2. NSFW Check
                            val nsfwKeywords = listOf("nsfw", "nude", "naked", "sex", "porn", "adult", "hentai") // Simplified list
                            val promptToCheck = visionPrompt.lowercase()
                            if (nsfwKeywords.any { promptToCheck.contains(it) }) {
                                 val userId = FirebaseAuth.getInstance().currentUser?.uid
                                 if (userId != null) {
                                     // Check subscription logic here if needed
                                 }
                            }

                            try {
                                val apiService = com.doyouone.drawai.data.api.RetrofitClient.apiService
                                
                                val basePrompt = "((masterpiece, best quality, anime background))"
                                val finalPrompt = if (visionPrompt.isNotBlank()) "$basePrompt, $visionPrompt" else basePrompt
                                
                                val request = com.doyouone.drawai.data.model.GenerateRequest(
                                    positivePrompt = finalPrompt,
                                    negativePrompt = "",
                                    workflow = "make_background_v2",
                                    width = w,
                                    height = h,
                                    seed = seed,
                                    ckptName = selectedCheckpoint,
                                    steps = steps.toInt(),
                                    cfg = cfg,
                                    samplerName = selectedSampler,
                                    scheduler = selectedScheduler,
                                    denoise = denoise,
                                    upscaleMethod = selectedUpscaleMethod
                                )
                                
                                val response = apiService.generateImage(request)
                                if (!response.isSuccessful) throw Exception("Error: ${response.code()}")
                                
                                val taskId = response.body()?.taskId ?: throw Exception("No task ID")
                                
                                var isComplete = false
                                var attempts = 0
                                while (!isComplete && attempts < 120) {
                                    delay(1000)
                                    attempts++
                                    val statusResponse = apiService.getTaskStatus(taskId)
                                    val status = statusResponse.body()?.status
                                    if (status == "completed") {
                                        isComplete = true
                                        val files = statusResponse.body()?.resultFiles
                                        if (!files.isNullOrEmpty()) {
                                            val filename = files[0].substringAfterLast("/").substringAfterLast("\\")
                                            val baseUrl = com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl()
                                            generatedImageUrl = "${baseUrl}download/$filename"
                                            
                                            // Auto-save logic here (simplified)
                                            try {
                                                 val repo = com.doyouone.drawai.data.repository.DrawAIRepository()
                                                 val tempFile = File(context.cacheDir, filename)
                                                 if (repo.downloadImage(filename, tempFile).isSuccess) {
                                                     val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                                     com.doyouone.drawai.data.local.ImageStorage(context).saveImage(bitmap, "Advanced Background", visionPrompt, "make_background_v2")
                                                 }
                                            } catch (e: Exception) { e.printStackTrace() }
                                            
                                            if (com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                                                com.doyouone.drawai.ads.AdManager.showInterstitialAd(
                                                    activity = (context as? android.app.Activity)!!,
                                                    onAdDismissed = { onNavigateToGallery() }
                                                )
                                            } else {
                                                onNavigateToGallery()
                                            }
                                        }
                                    } else if (status == "failed") {
                                        throw Exception(statusResponse.body()?.error ?: "Failed")
                                    }
                                }
                            } catch (e: Exception) {
                                if (e.message?.contains("403") == true || e.message?.contains("limit", ignoreCase = true) == true) {
                                    showLimitDialog = true
                                } else if (e.message?.contains("530") == true) {
                                    showMaintenanceDialog = true
                                } else {
                                    errorMessage = e.message
                                }
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Generate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            errorMessage?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    
    if (showLimitDialog) {
         com.doyouone.drawai.ui.components.RewardedAdDialog(
            remainingGenerations = 0,
            onDismiss = { showLimitDialog = false },
            onWatchAd = {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    com.doyouone.drawai.ads.AdManager.showRewardedAd(
                        activity,
                        onUserEarnedReward = {
                            scope.launch {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                   com.doyouone.drawai.data.repository.FirebaseGenerationRepository().addBonusGeneration(userId)
                                }
                            }
                            showLimitDialog = false
                        }
                    )
                }
            },
            onUpgradeToPremium = { showLimitDialog = false }
        )
    }



    // Maintenance Popup
    if (showMaintenanceDialog) {
        com.doyouone.drawai.ui.components.MaintenancePopup(
            onGoToGalleryClick = { 
                showMaintenanceDialog = false
                onNavigateToGallery() 
            },
            onRetry = {
                showMaintenanceDialog = false
            }
        )
    }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Premium Feature") },
            text = { Text("Advanced settings and high resolutions (4K) are available for Basic & Pro users only.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showUpgradeDialog = false
                        onNavigateToSubscription()
                    }
                ) {
                    Text("Upgrade Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }

    // Processing Modal - Blocking dialog
    ProcessingModal(
        isProcessing = isGenerating,
        message = "Generating...",
        detail = "Please wait while we create your background."
    )
}
