package core.presentation.effect

// Ports of Balatro's CRT.fs and background.fs. One source runs as AGSL on Android 13+ and as SkSL elsewhere.
// Omitted from the original CRT: the glitch bands (off at the default setting) and the cursor bulge.
// The bloom kernel is 5x5 instead of 7x7 to stay cheap on phones.

internal const val CRT_SHADER = """
uniform shader content;
uniform float2 resolution;
uniform float time;
uniform float intensity;
uniform float noiseAmount;
uniform float bloomAmount;
uniform float scanlineCount;
uniform float vignette;

half4 main(float2 fragCoord) {
    float2 tc = fragCoord / resolution;
    float2 centered = tc * 2.0 - 1.0;
    centered *= 0.99832;
    centered += (centered.yx * centered.yx) * centered * (float2(1.0147, 1.021) - 1.0);

    float mask = (1.0 - smoothstep(0.99, 1.0, abs(centered.x) - 0.01))
        * (1.0 - smoothstep(0.99, 1.0, abs(centered.y) - 0.01));
    tc = (centered + 1.0) / 2.0;

    half4 crt = content.eval(tc * resolution);

    float adjusted = max(0.0, intensity / (0.16 * 0.3));
    if (adjusted > 0.0000001) {
        float shift = 0.0005 * 1600.0;
        crt.r = crt.r * (1.0 - adjusted) + adjusted * content.eval(tc * resolution + float2(shift, 0.0)).r;
        crt.g = crt.g * (1.0 - adjusted) + adjusted * content.eval(tc * resolution - float2(shift, 0.0)).g;
    }
    float3 rgb = crt.rgb * (1.0 - intensity);

    float lines = scanlineCount;
    float3 scan = float3(
        clamp(-0.3 + 2.0 * sin(tc.y * lines - 3.14 / 4.0) - 0.8 * clamp(sin(tc.x * lines * 4.0), 0.4, 1.0), -1.0, 2.0),
        clamp(-0.3 + 2.0 * cos(tc.y * lines) - 0.8 * clamp(cos(tc.x * lines * 4.0), 0.0, 1.0), -1.0, 2.0),
        clamp(-0.3 + 2.0 * cos(tc.y * lines - 3.14 / 3.0) - 0.8 * clamp(cos(tc.x * lines * 4.0 - 3.14 / 4.0), 0.0, 1.0), -1.0, 2.0));
    rgb += crt.rgb * scan * intensity;

    float x = (tc.x - mod(tc.x, 0.002)) * (tc.y - mod(tc.y, 0.0013)) * time * 1000.0;
    x = mod(x, 13.0) * mod(x, 123.0);
    float dx = mod(x, 0.11) / 0.11;
    float n = clamp(noiseAmount, 0.0, 1.0);
    rgb = (1.0 - n) * rgb + dx * n;

    rgb -= 0.55 - 0.02 * (0.0 - adjusted * bloomAmount * 0.7);
    rgb = rgb * (1.0 + 0.14 + adjusted * (0.012 - bloomAmount * 0.12));
    rgb += 0.5;

    float bloom = 0.0;
    float3 glow = float3(0.0);
    if (bloomAmount > 0.00001 && intensity > 0.000001) {
        bloom = 0.03 * adjusted;
        float dist = 0.0015 * 2.0;
        float acc = 0.0;
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                half4 s = content.eval((tc + (dist / 2.0) * float2(float(i), float(j))) * resolution);
                float3 hot = max(5.0 * s.rgb - 4.0, 0.0);
                acc += min(min(hot.r, hot.g), hot.b) * (2.0 - abs(float(i + j)) / 4.0);
            }
        }
        glow = float3(acc / 4.0);
    }

    float2 fromCenter = (fragCoord / resolution - 0.5) * 2.0;
    float vig = 1.0 - vignette * dot(fromCenter, fromCenter) * 0.5;
    return half4(half3((rgb * (1.0 - bloom) + bloom * glow) * mask * vig), 1.0);
}
"""

internal const val SWIRL_SHADER = """
uniform float2 resolution;
uniform float time;
uniform float spinTime;
uniform float spinAmount;
uniform float contrast;
uniform float4 colour1;
uniform float4 colour2;
uniform float4 colour3;

half4 main(float2 fragCoord) {
    float screenLen = length(resolution);
    float pixelSize = screenLen / 700.0;
    float2 uv = (floor(fragCoord * (1.0 / pixelSize)) * pixelSize - 0.5 * resolution) / screenLen - float2(0.12, 0.0);
    float uvLen = length(uv);

    float speed = spinTime * 0.5 * 0.2 + 302.2;
    float angle = atan(uv.y, uv.x) + speed - 0.5 * 20.0 * (spinAmount * uvLen + (1.0 - spinAmount));
    float2 mid = (resolution / screenLen) / 2.0;
    uv = float2(uvLen * cos(angle) + mid.x, uvLen * sin(angle) + mid.y) - mid;

    uv *= 30.0;
    speed = time * 2.0;
    float2 uv2 = float2(uv.x + uv.y);
    for (int i = 0; i < 5; i++) {
        uv2 += sin(max(uv.x, uv.y)) + uv;
        uv += 0.5 * float2(cos(5.1123314 + 0.353 * uv2.y + speed * 0.131121), sin(uv2.x - 0.113 * speed));
        uv -= 1.0 * cos(uv.x + uv.y) - 1.0 * sin(uv.x * 0.711 - uv.y);
    }

    float contrastMod = 0.25 * contrast + 0.5 * spinAmount + 1.2;
    float paint = min(2.0, max(0.0, length(uv) * 0.035 * contrastMod));
    float c1 = max(0.0, 1.0 - contrastMod * abs(1.0 - paint));
    float c2 = max(0.0, 1.0 - contrastMod * abs(paint));
    float c3 = 1.0 - min(1.0, c1 + c2);

    float4 color = (0.3 / contrast) * colour1
        + (1.0 - 0.3 / contrast) * (colour1 * c1 + colour2 * c2 + float4(c3 * colour3.rgb, c3 * colour1.a));
    return half4(half3(color.rgb), 1.0);
}
"""
