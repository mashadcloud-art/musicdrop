package com.musicdrop.tv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TvFocusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    cornerRadius: Dp = 16.dp,
    focusColor: Color = Color.White,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(120),
        label = "tv_focus_scale"
    )

    var boxModifier = modifier
        .scale(scale)
        .onFocusChanged { state ->
            isFocused = state.isFocused
            if (state.isFocused) {
                TvFocusRegistry.activeClickAction = onClick
            } else if (TvFocusRegistry.activeClickAction == onClick) {
                TvFocusRegistry.activeClickAction = null
            }
        }

    if (focusRequester != null) {
        boxModifier = boxModifier.focusRequester(focusRequester)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = boxModifier
            .focusable()
            .then(
                if (isFocused)
                    Modifier
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(cornerRadius))
                        .border(3.5.dp, focusColor, RoundedCornerShape(cornerRadius))
                else Modifier
            )
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .onPreviewKeyEvent { event ->
                val nativeCode = event.nativeKeyEvent.keyCode
                if (event.type == KeyEventType.KeyDown &&
                    (nativeCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                     nativeCode == android.view.KeyEvent.KEYCODE_ENTER ||
                     nativeCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER ||
                     nativeCode == android.view.KeyEvent.KEYCODE_BUTTON_A ||
                     nativeCode == android.view.KeyEvent.KEYCODE_BUTTON_SELECT)
                ) {
                    onClick()
                    true
                } else false
            }
            .padding(contentPadding)
    ) {
        content()
    }
}
