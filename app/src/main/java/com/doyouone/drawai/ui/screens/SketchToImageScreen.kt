package com.doyouone.drawai.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import com.doyouone.drawai.ui.components.ProcessingModal
import com.doyouone.drawai.viewmodel.GenerateViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchToImageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: (() -> Unit)? = null,
    viewModel: GenerateViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Premium Logic
    val generationLimit by viewModel.generationLimit.collectAsState()
    val isPremium = remember(generationLimit) {
        generationLimit?.subscriptionType != "free" && generationLimit?.isSubscriptionExpired() == false
    }
    
    // Image States
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var resultImageUrl by remember { mutableStateOf<String?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    
    // Processing States
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Configuration States
    var positivePrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var controlnetStrength by remember { mutableFloatStateOf(1.0f) }  // Standard ControlNet strength
    var cfgScale by remember { mutableFloatStateOf(7.0f) }
    var denoise by remember { mutableFloatStateOf(0.5f) }  // Lower for better sketch preservation
    var steps by remember { mutableIntStateOf(20) }
    var width by remember { mutableIntStateOf(1024) }
    var height by remember { mutableIntStateOf(1024) }
    var seed by remember { mutableStateOf("763189886868065") } // Seed as String for TextField
    
    // Force default for Free users
    LaunchedEffect(isPremium) {
        if (!isPremium) {
            steps = 20
            width = 1024
            height = 1024
            // Reset to default checkpoint?
        }
    }
    
    // Dropdown States
    var selectedSampler by remember { mutableStateOf("dpmpp_2m_sde") }
    var selectedScheduler by remember { mutableStateOf("karras") }
    var expandedSampler by remember { mutableStateOf(false) }
    var expandedScheduler by remember { mutableStateOf(false) }
    var expandedSteps by remember { mutableStateOf(false) }
    var expandedWidth by remember { mutableStateOf(false) }
    var expandedHeight by remember { mutableStateOf(false) }
    
    // Checkpoint selection
    var selectedCheckpoint by remember { mutableStateOf("wildcardxXLTURBO_wildcardxXLTURBOV10.safetensors") }
    var expandedCheckpoint by remember { mutableStateOf(false) }
    
    var isConfigExpanded by remember { mutableStateOf(true) }
    
    val samplerOptions = listOf(
        "euler", "euler_a", "heun", "dpm_2", "dpm_2_a",
        "lms", "dpm_fast", "dpm_adaptive", "dpmpp_2s_a",
        "dpmpp_sde", "dpmpp_2m", "dpmpp_2m_sde", "ddim", "uni_pc"
    )
    
    val schedulerOptions = listOf(
        "normal", "karras", "exponential", "simple", "ddim_uniform"
    )
    
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
    
    val stepsOptions = listOf(20, 25, 30, 35, 40, 50)
    val resolutionOptions = listOf(512, 768, 1024, 1536, 2048)

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileSize = getFileSizeFromUri(context, it)
            if (fileSize > 2 * 1024 * 1024) {
                errorMessage = "File too large. Maximum 2MB"
                selectedImageUri = null
            } else {
                selectedImageUri = it
                resultImageUrl = null
                errorMessage = null
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Sketch to Image", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Colorize Your Sketches", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Image Upload Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { if (!isProcessing) imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Sketch",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            
                            if (!isProcessing) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                ) {
                                    IconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Change", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Brush, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Upload Sketch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Max 2MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))

                // Error Message
                errorMessage?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // 2. Configuration
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isConfigExpanded = !isConfigExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(if(isConfigExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                        }

                        AnimatedVisibility(visible = isConfigExpanded) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                
                                // Checkpoint Selection
                                Text("Workflows", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                ExposedDropdownMenuBox(
                                    expanded = expandedCheckpoint && isPremium,
                                    onExpandedChange = { if (isPremium) expandedCheckpoint = !expandedCheckpoint },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = checkpointFriendlyNames[selectedCheckpoint] ?: selectedCheckpoint,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = isPremium,
                                        leadingIcon = {
                                            val friendlyName = checkpointFriendlyNames[selectedCheckpoint] ?: selectedCheckpoint
                                            val imageId = friendlyName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")
                                            AsyncImage(
                                                model = "${com.doyouone.drawai.data.api.RetrofitClient.getBaseUrl()}workflow-image/$imageId",
                                                contentDescription = null,
                                                modifier = Modifier.padding(8.dp).size(24.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentScale = ContentScale.Crop
                                            )
                                        },
                                        trailingIcon = { 
                                            if (!isPremium) Icon(Icons.Default.Lock, "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            else ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCheckpoint) 
                                        },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
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
                                if (!isPremium) {
                                    Text(
                                        "Upgrade to Basic/Pro plan to unlock full settings", 
                                        style = MaterialTheme.typography.bodySmall, 
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))

                                // Prompts
                                Text("Vision", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = positivePrompt,
                                    onValueChange = { positivePrompt = it },
                                    placeholder = { Text("e.g. colorful, vibrant, anime style") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2
                                )
                                Spacer(Modifier.height(8.dp))

                                Text("Avoid", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = negativePrompt,
                                    onValueChange = { negativePrompt = it },
                                    placeholder = { Text("e.g. monochrome, grayscale, blurry") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2
                                )
                                Spacer(Modifier.height(12.dp))

                                // ControlNet Strength
                                Text("ControlNet Strength: ${String.format("%.1f", controlnetStrength)}", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = controlnetStrength,
                                    onValueChange = { controlnetStrength = it },
                                    valueRange = 0.5f..3.0f,
                                    steps = 24
                                )
                                Text("Higher = Stricter sketch adherence (Recommended: 0.8-1.2)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(Modifier.height(12.dp))

                                // CFG Scale
                                Text("CFG Scale: ${String.format("%.1f", cfgScale)}", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = cfgScale,
                                    onValueChange = { cfgScale = it },
                                    valueRange = 1f..20f,
                                    steps = 18
                                )
                                Spacer(Modifier.height(12.dp))

                                // Denoise
                                Text("Denoise: ${String.format("%.2f", denoise)}", style = MaterialTheme.typography.labelMedium)
                                Slider(
                                    value = denoise,
                                    onValueChange = { denoise = it },
                                    valueRange = 0.1f..1.0f,
                                    steps = 17
                                )
                                Text("Lower = More sketch preservation (Recommended: 0.4-0.6)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(Modifier.height(12.dp))

                                // Steps
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Steps: $steps", style = MaterialTheme.typography.labelMedium)
                                    if (!isPremium) Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(16.dp).padding(start=4.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Box {
                                    Slider(
                                        value = steps.toFloat(),
                                        onValueChange = { if (isPremium) steps = it.toInt() },
                                        valueRange = 10f..50f,
                                        steps = 39,
                                        enabled = true,
                                        colors = if (!isPremium) SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        ) else SliderDefaults.colors()
                                    )
                                    if (!isPremium) {
                                        Box(
                                            modifier = Modifier.matchParentSize().clickable { }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                // Seed
                                Text("Seed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = seed,
                                    onValueChange = { 
                                        if (it.all { char -> char.isDigit() }) {
                                            seed = it 
                                        }
                                    },
                                    placeholder = { Text("Random (-1)") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    trailingIcon = {
                                        if (seed.isNotEmpty()) {
                                            IconButton(onClick = { seed = "" }) {
                                                Icon(Icons.Default.Close, "Clear")
                                            }
                                        } else {
                                            Icon(Icons.Default.Shuffle, "Random", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                )
                                Text("Leave empty for random seed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                                Spacer(Modifier.height(12.dp))

                                // Resolution
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Width", style = MaterialTheme.typography.labelMedium)
                                        ExposedDropdownMenuBox(
                                            expanded = expandedWidth && isPremium,
                                            onExpandedChange = { if (isPremium) expandedWidth = !expandedWidth },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = width.toString(),
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { 
                                                    if (!isPremium) Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    else ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWidth)
                                                },
                                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            if (isPremium) {
                                                ExposedDropdownMenu(
                                                    expanded = expandedWidth,
                                                    onDismissRequest = { expandedWidth = false }
                                                ) {
                                                    resolutionOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option.toString()) },
                                                            onClick = {
                                                                width = option
                                                                expandedWidth = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Height", style = MaterialTheme.typography.labelMedium)
                                        ExposedDropdownMenuBox(
                                            expanded = expandedHeight && isPremium,
                                            onExpandedChange = { if (isPremium) expandedHeight = !expandedHeight },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = height.toString(),
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { 
                                                    if (!isPremium) Icon(Icons.Default.Lock, "Locked", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    else ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHeight)
                                                },
                                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            if (isPremium) {
                                                ExposedDropdownMenu(
                                                    expanded = expandedHeight,
                                                    onDismissRequest = { expandedHeight = false }
                                                ) {
                                                    resolutionOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option.toString()) },
                                                            onClick = {
                                                                height = option
                                                                expandedHeight = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                // Sampler
                                Text("Sampler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                ExposedDropdownMenuBox(
                                    expanded = expandedSampler,
                                    onExpandedChange = { expandedSampler = !expandedSampler },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = selectedSampler,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSampler) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedSampler,
                                        onDismissRequest = { expandedSampler = false }
                                    ) {
                                        samplerOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedSampler = option
                                                    expandedSampler = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Scheduler
                                Text("Scheduler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                ExposedDropdownMenuBox(
                                    expanded = expandedScheduler,
                                    onExpandedChange = { expandedScheduler = !expandedScheduler },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = selectedScheduler,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedScheduler) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedScheduler,
                                        onDismissRequest = { expandedScheduler = false }
                                    ) {
                                        schedulerOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    selectedScheduler = option
                                                    expandedScheduler = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 3. Action Button
                if (isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(processingMessage, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                // Action Button (Hidden when processing)
                if (!isProcessing) {
                    Button(
                        onClick = {
                            selectedImageUri?.let { uri ->
                                scope.launch {
                                    isProcessing = true
                                    errorMessage = null
                                    processingMessage = "Preparing..."
                                    
                                    try {
                                        val apiService = com.doyouone.drawai.data.api.RetrofitClient.apiService
                                        val imagePart = uriToMultipartBodySketch(context, uri, "image") 
                                            ?: throw Exception("Failed to read file")
                                        
                                        val options = mutableMapOf<String, RequestBody>()
                                        options["positive_prompt"] = positivePrompt.toRequestBody()
                                        options["negative_prompt"] = negativePrompt.toRequestBody()
                                        options["controlnet_strength"] = controlnetStrength.toString().toRequestBody()
                                        options["cfg_scale"] = cfgScale.toString().toRequestBody()
                                        options["denoise"] = denoise.toString().toRequestBody()
                                        options["steps"] = steps.toString().toRequestBody()
                                        options["width"] = width.toString().toRequestBody()
                                        options["height"] = height.toString().toRequestBody()
                                        options["sampler"] = selectedSampler.toRequestBody()
                                        options["scheduler"] = selectedScheduler.toRequestBody()
                                        options["checkpoint"] = selectedCheckpoint.toRequestBody()
                                        options["seed"] = (if (seed.isEmpty()) "-1" else seed).toRequestBody()
                                        
                                        processingMessage = "Uploading..."
                                        val response = apiService.sketchToImage(imagePart, options)
                                        
                                        if (!response.isSuccessful) {
                                            val errorBody = response.errorBody()?.string()
                                            val msg = if (errorBody != null && errorBody.contains("error")) {
                                                try {
                                                     org.json.JSONObject(errorBody).getString("error")
                                                } catch(e: Exception) { "Server Error: ${response.code()}" }
                                            } else "Server Error: ${response.code()}"
                                            throw Exception(msg)
                                        }
                                        
                                        val taskId = response.body()?.taskId ?: throw Exception("No Task ID")
                                        
                                        processingMessage = "Colorizing Sketch..."
                                        var isComplete = false
                                        var attempts = 0
                                        while(!isComplete && attempts < 120) {
                                            delay(1000)
                                            attempts++
                                            val statusRes = apiService.getTaskStatus(taskId)
                                            val status = statusRes.body()?.status
                                            
                                            when(status) {
                                                "completed" -> {
                                                    isComplete = true
                                                    val files = statusRes.body()?.resultFiles ?: emptyList()
                                                    if (files.isNotEmpty()) {
                                                         val fname = files[0].substringAfterLast("/").substringAfterLast("\\")
                                                         resultImageUrl = "https://drawai-api.drawai.site/download/$fname"
                                                         processingMessage = "Saving..."
                                                         saveImageToGallery(context, fname, "Sketch to Image")
                                                    }
                                                    
                                                    if (com.doyouone.drawai.ads.AdManager.isInterstitialAdReady()) {
                                                        com.doyouone.drawai.ads.AdManager.showInterstitialAd(
                                                            activity = (context as? android.app.Activity)!!,
                                                            onAdDismissed = {
                                                                onNavigateToGallery?.invoke()
                                                            }
                                                        )
                                                    } else {
                                                        onNavigateToGallery?.invoke()
                                                    }
                                                }
                                                "failed" -> throw Exception(statusRes.body()?.error ?: "Failed")
                                                else -> processingMessage = "Processing... ${attempts}s"
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
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selectedImageUri != null
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Colorize Sketch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
    
    // Limit Dialog
    if (showLimitDialog) {
        com.doyouone.drawai.ui.components.RewardedAdDialog(
            remainingGenerations = 0,
            onDismiss = { showLimitDialog = false },
            onWatchAd = {
                if (context is android.app.Activity) {
                    com.doyouone.drawai.ads.AdManager.showRewardedAd(
                        context,
                        onUserEarnedReward = {
                            scope.launch {
                                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                   val repo = com.doyouone.drawai.data.repository.FirebaseGenerationRepository()
                                   repo.addBonusGeneration(userId)
                                   android.widget.Toast.makeText(context, "Bonus generation added!", android.widget.Toast.LENGTH_SHORT).show()
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
                onNavigateToGallery?.invoke() 
            },
            onRetry = {
                showMaintenanceDialog = false
            }
        )
    }

    // Processing Modal - Blocking dialog
    ProcessingModal(
        isProcessing = isProcessing,
        message = if (processingMessage.isNotEmpty()) processingMessage else "Processing...",
        detail = "Please wait while we colorize your sketch."
    )
}

// Helpers
private fun String.toRequestBody(): RequestBody = this.toRequestBody("text/plain".toMediaTypeOrNull())

private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
    } catch (e: Exception) { 0L }
}

fun uriToMultipartBodySketch(context: Context, uri: Uri, paramName: String): MultipartBody.Part? {
     try {
        val contentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
        val file = File(context.cacheDir, "temp_upload_sketch.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(paramName, file.name, requestBody)
    } catch (e: Exception) { return null }
}

private suspend fun saveImageToGallery(context: Context, filename: String, prompt: String) {
    try {
        val repo = com.doyouone.drawai.data.repository.DrawAIRepository()
        val storage = com.doyouone.drawai.data.local.ImageStorage(context)
        val tempFile = File(context.cacheDir, filename)
        val result = repo.downloadImage(filename, tempFile)
        if (result.isSuccess) {
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            if (bitmap != null) {
                storage.saveImage(bitmap, prompt, "", "sketch_to_image")
            }
        }
    } catch (e: Exception) { }
}
