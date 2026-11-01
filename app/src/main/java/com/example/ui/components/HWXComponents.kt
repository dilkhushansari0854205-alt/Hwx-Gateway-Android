package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun HWXCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    hasGlowBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderModifier = if (hasGlowBorder) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(HWXCyan.copy(alpha = 0.5f), HWXEmeraldGreen.copy(alpha = 0.3f))),
            shape = RoundedCornerShape(cornerRadius)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = HWXCardBorder,
            shape = RoundedCornerShape(cornerRadius)
        )
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(borderModifier)
            .background(HWXCardGradient)
            .padding(18.dp),
        content = content
    )
}

@Composable
fun HWXButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isPrimary: Boolean = true,
    trailingIcon: ImageVector? = null,
    testTag: String = "hwx_button"
) {
    val backgroundBrush = if (isPrimary) HWXPrimaryGradient else Brush.horizontalGradient(listOf(HWXCardSurface, HWXCardSurface))
    val textColor = HWXTextPrimary

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(backgroundBrush)
            .then(
                if (!isPrimary) Modifier.border(1.dp, HWXCardBorder, RoundedCornerShape(27.dp))
                else Modifier
            )
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = HWXTextPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HWXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = "hwx_input"
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = HWXTextMuted,
                fontSize = 15.sp
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = HWXCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else null,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = HWXTextMuted
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = HWXTextPrimary,
            unfocusedTextColor = HWXTextPrimary,
            focusedContainerColor = HWXSecondaryBackground,
            unfocusedContainerColor = HWXSecondaryBackground,
            focusedBorderColor = HWXCyan,
            unfocusedBorderColor = HWXCardBorder,
            cursorColor = HWXCyan
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
    )
}

@Composable
fun HWXStatusBadge(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val bgColor = if (isOnline) HWXDarkGreen.copy(alpha = 0.6f) else Color(0xFF3B1520)
    val textColor = if (isOnline) HWXEmeraldGreen else HWXError
    val text = label ?: if (isOnline) "Online" else "Offline"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(0.5.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
