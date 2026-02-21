package com.doyouone.drawai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeVerificationDialog(
    onDismiss: () -> Unit,
    onVerified: (Boolean) -> Unit
) {
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = "Age Verification Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = "This content is restricted to users 18 years and older. Please verify your age by entering your date of birth.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Date Input Fields
                Text(
                    text = "Date of Birth",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Day
                    OutlinedTextField(
                        value = day,
                        onValueChange = { if (it.length <= 2) day = it },
                        label = { Text("Day") },
                        placeholder = { Text("DD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // Month
                    OutlinedTextField(
                        value = month,
                        onValueChange = { if (it.length <= 2) month = it },
                        label = { Text("Month") },
                        placeholder = { Text("MM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // Year
                    OutlinedTextField(
                        value = year,
                        onValueChange = { if (it.length <= 4) year = it },
                        label = { Text("Year") },
                        placeholder = { Text("YYYY") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }
                
                // Error Message
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isVerifying
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            isVerifying = true
                            errorMessage = ""
                            
                            try {
                                // Validate input
                                if (day.isEmpty() || month.isEmpty() || year.isEmpty()) {
                                    errorMessage = "Please fill in all fields"
                                    isVerifying = false
                                    return@Button
                                }
                                
                                val dayInt = day.toIntOrNull()
                                val monthInt = month.toIntOrNull()
                                val yearInt = year.toIntOrNull()
                                
                                if (dayInt == null || monthInt == null || yearInt == null) {
                                    errorMessage = "Please enter valid numbers"
                                    isVerifying = false
                                    return@Button
                                }
                                
                                if (dayInt !in 1..31 || monthInt !in 1..12 || yearInt < 1900 || yearInt > LocalDate.now().year) {
                                    errorMessage = "Please enter a valid date"
                                    isVerifying = false
                                    return@Button
                                }
                                
                                // Create date and calculate age
                                val birthDate = LocalDate.of(yearInt, monthInt, dayInt)
                                val age = Period.between(birthDate, LocalDate.now()).years
                                
                                if (age >= 18) {
                                    onVerified(true)
                                } else {
                                    errorMessage = "You must be 18 years or older to access this content"
                                    isVerifying = false
                                }
                                
                            } catch (e: DateTimeParseException) {
                                errorMessage = "Please enter a valid date"
                                isVerifying = false
                            } catch (e: Exception) {
                                errorMessage = "Invalid date format"
                                isVerifying = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isVerifying
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Verify")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Privacy Notice
                Text(
                    text = "Your date of birth is only used for age verification and is not stored or shared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}