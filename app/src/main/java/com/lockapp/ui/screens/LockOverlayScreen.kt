package com.lockapp.ui.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn hình khóa overlay Jetpack Compose.
 * Hiển thị toàn màn hình đè lên ứng dụng bị khóa.
 */
@Composable
fun LockOverlayScreen(
    appName: String,
    appPackage: String,
    appIcon: Drawable?,
    isBiometricAvailable: Boolean,
    onVerifyPin: (String) -> Boolean,
    onUnlockSuccess: () -> Unit,
    onWrongPin: (Int) -> Unit,
    onBiometricClick: () -> Unit,
    onExitToHome: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var wrongAttemptCount by remember { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val maxPinLength = 4

    // Tự động thoát về Home nếu người dùng bấm phím Back hoặc vuốt cử chỉ Back
    LocalOnBackPressedDispatcherOwner.current?.let {
        BackHandler {
            onExitToHome()
        }
    }

    fun triggerWrongPinAnimation() {
        wrongAttemptCount++
        isError = true
        errorMessage = "Mã PIN không đúng (Lần $wrongAttemptCount)"
        onWrongPin(wrongAttemptCount)

        coroutineScope.launch {
            // Hiệu ứng rung lắc ngang (Shake animation)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -25f at 50
                    25f at 100
                    -20f at 150
                    20f at 200
                    -10f at 250
                    10f at 300
                    -5f at 350
                    0f at 400
                }
            )
            delay(1200)
            pin = ""
            isError = false
        }
    }

    fun onNumberClick(num: String) {
        if (pin.length < maxPinLength && !isError) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val newPin = pin + num
            pin = newPin

            if (newPin.length == maxPinLength) {
                // Kiểm tra mã PIN
                if (onVerifyPin(newPin)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUnlockSuccess()
                } else {
                    triggerWrongPinAnimation()
                }
            }
        }
    }

    fun onDeleteClick() {
        if (pin.isNotEmpty() && !isError) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pin = pin.dropLast(1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Dark slate background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF0A0F1D)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // === HEADER: APP INFO ===
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = appIcon),
                                contentDescription = appName,
                                modifier = Modifier.size(52.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = appName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Ứng dụng này đang được bảo vệ",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // === PIN DOTS INDICATOR ===
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(maxPinLength) { index ->
                            val isFilled = index < pin.length
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isError -> Color(0xFFEF4444)
                                            isFilled -> Color(0xFF38BDF8)
                                            else -> Color(0xFF334155)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedVisibility(
                        visible = isError,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isError) {
                        Text(
                            text = "Nhập mã PIN để mở khóa",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }

                // === NUMPAD ===
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )

                    for (row in rows) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            for (num in row) {
                                KeypadButton(
                                    text = num,
                                    onClick = { onNumberClick(num) }
                                )
                            }
                        }
                    }

                    // Hàng cuối: Vân tay | 0 | Xóa
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nút Vân tay
                        if (isBiometricAvailable) {
                            KeypadIconButton(
                                icon = Icons.Default.Fingerprint,
                                contentDescription = "Vân tay",
                                tint = Color(0xFF38BDF8),
                                onClick = onBiometricClick
                            )
                        } else {
                            Spacer(modifier = Modifier.size(72.dp))
                        }

                        // Phím 0
                        KeypadButton(
                            text = "0",
                            onClick = { onNumberClick("0") }
                        )

                        // Phím xóa
                        KeypadIconButton(
                            icon = Icons.Default.Backspace,
                            contentDescription = "Xóa",
                            tint = Color(0xFF94A3B8),
                            onClick = { onDeleteClick() }
                        )
                    }
                }

                // === BOTTOM ACTION: EXIT TO HOME ===
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                            .clickable(onClick = onExitToHome)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Trang chủ",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Trở về màn hình chính",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B).copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = Color(0xFF38BDF8)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = tint),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}
