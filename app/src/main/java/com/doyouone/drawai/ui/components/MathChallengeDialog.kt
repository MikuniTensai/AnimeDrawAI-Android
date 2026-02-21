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
import kotlin.random.Random

data class MathChallenge(
    val question: String,
    val answer: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathChallengeDialog(
    onDismiss: () -> Unit,
    onVerified: (Boolean) -> Unit
) {
    var userAnswer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    val maxAttempts = 3
    
    // Generate math challenge
    val challenge by remember {
        mutableStateOf(generateMathChallenge())
    }

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
                // Math Icon
                Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Math Challenge",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(32.dp)
                    )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = "Age Verification Challenge",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description
                Text(
                    text = "Please solve this simple math problem to verify you are old enough to access this content.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Math Question
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Solve this:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = challenge.question,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Answer Input
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    label = { Text("Your Answer") },
                    placeholder = { Text("Enter the result") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty()
                )
                
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
                
                // Attempts Counter
                if (attempts > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Attempts: $attempts/$maxAttempts",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (attempts >= maxAttempts - 1) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onSurfaceVariant
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
                            
                            val answer = userAnswer.toIntOrNull()
                            if (answer == null) {
                                errorMessage = "Please enter a valid number"
                                isVerifying = false
                                return@Button
                            }
                            
                            if (answer == challenge.answer) {
                                onVerified(true)
                            } else {
                                attempts++
                                if (attempts >= maxAttempts) {
                                    errorMessage = "Maximum attempts reached. Please try age verification instead."
                                    isVerifying = false
                                } else {
                                    errorMessage = "Incorrect answer. Try again."
                                    userAnswer = ""
                                    isVerifying = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isVerifying && attempts < maxAttempts
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Help Text
                Text(
                    text = "Having trouble? You can use the age verification option instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun generateMathChallenge(): MathChallenge {
    val operations = listOf(
        { a: Int, b: Int -> Triple("$a + $b", a + b, "+") },
        { a: Int, b: Int -> Triple("$a - $b", a - b, "-") },
        { a: Int, b: Int -> Triple("$a × $b", a * b, "×") }
    )
    
    val operation = operations.random()
    val a = Random.nextInt(5, 25)
    val b = Random.nextInt(2, 15)
    
    // Ensure subtraction doesn't result in negative numbers
    val (num1, num2) = if (operation(0, 0).third == "-" && a < b) {
        Pair(b, a)
    } else {
        Pair(a, b)
    }
    
    val result = operation(num1, num2)
    return MathChallenge(
        question = "${result.first} = ?",
        answer = result.second
    )
}