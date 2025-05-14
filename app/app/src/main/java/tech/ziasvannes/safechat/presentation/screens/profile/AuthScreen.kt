package tech.ziasvannes.safechat.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(onAuthSuccess: (token: String) -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isSignUp by remember { mutableStateOf(false) }

        val authResult by viewModel.authResult.collectAsState()

        LaunchedEffect(authResult) {
                when (authResult) {
                        is AuthResult.Success ->
                                onAuthSuccess((authResult as AuthResult.Success).token)
                        else -> {}
                }
        }

        Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Text(
                        text = if (isSignUp) "Sign Up" else "Sign In",
                        style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                        onClick = {
                                if (isSignUp) viewModel.signUp(username, password)
                                else viewModel.signIn(username, password)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = authResult !is AuthResult.Loading
                ) { Text(if (isSignUp) "Sign Up" else "Sign In") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(
                                if (isSignUp) "Already have an account? Sign In"
                                else "Don't have an account? Sign Up"
                        )
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (authResult) {
                        is AuthResult.Error ->
                                Text(
                                        (authResult as AuthResult.Error).message,
                                        color = MaterialTheme.colorScheme.error
                                )
                        is AuthResult.Loading -> CircularProgressIndicator()
                        else -> {}
                }
        }
}
