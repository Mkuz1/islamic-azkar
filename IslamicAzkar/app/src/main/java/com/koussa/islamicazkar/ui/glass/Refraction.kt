package com.koussa.islamicazkar.ui.glass

import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Liquid Glass lens: near the rim of a pane the backdrop is pulled inward along the
 * surface normal (as light bends through the thick edge of real glass), with a hint
 * of chromatic dispersion. Interior pixels pass through untouched.
 */
private const val LENS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float depth;
uniform float dispersion;

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 coord) {
    float2 c = size * 0.5;
    float2 p = coord - c;
    float d = sdRoundRect(p, c, radius);
    float t = clamp(1.0 + d / depth, 0.0, 1.0);
    float bend = t * t * t;
    float2 n = float2(
        sdRoundRect(p + float2(1.0, 0.0), c, radius) - sdRoundRect(p - float2(1.0, 0.0), c, radius),
        sdRoundRect(p + float2(0.0, 1.0), c, radius) - sdRoundRect(p - float2(0.0, 1.0), c, radius));
    n = n / max(length(n), 0.0001);
    float2 off = -n * bend * depth * 0.85;
    half4 base = content.eval(coord + off);
    half4 warm = content.eval(coord + off * (1.0 + dispersion));
    half4 cool = content.eval(coord + off * (1.0 - dispersion));
    return half4(warm.r, base.g, cool.b, base.a);
}
"""

/** Blur, plus the refracting lens on Android 13+. */
fun liquidGlassEffect(size: Size, cornerRadius: Float, blurPx: Float, depthPx: Float): RenderEffect {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || size.minDimension <= 1f) {
        return BlurEffect(blurPx, blurPx, TileMode.Clamp)
    }
    // A driver whose shader compiler rejects the lens still gets frosted glass.
    return runCatching { lens(size, cornerRadius, blurPx, depthPx) }
        .getOrElse { BlurEffect(blurPx, blurPx, TileMode.Clamp) }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun lens(size: Size, cornerRadius: Float, blurPx: Float, depthPx: Float): RenderEffect {
    val shader = RuntimeShader(LENS_AGSL).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("radius", cornerRadius.coerceIn(0f, size.minDimension / 2f))
        setFloatUniform("depth", depthPx.coerceAtMost(size.minDimension / 2f))
        setFloatUniform("dispersion", 0.18f)
    }
    val blur = android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
    val refract = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
    // chain(outer, inner): blur first, then bend the blurred backdrop.
    return android.graphics.RenderEffect.createChainEffect(refract, blur).asComposeRenderEffect()
}
