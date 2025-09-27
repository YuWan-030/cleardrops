package cn.alini.cleardrops.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.HashMap;
import java.util.Map;

/**
 * 将带有占位符与渐变标签的字符串渲染为 Component。
 * 支持：
 * - 占位符：形如 {name}，由调用方传入 map 替换。
 * - 渐变标签：<gradient:#RRGGBB:#RRGGBB>文本</gradient>
 *   会对“文本”的每个字符进行线性渐变上色（按 Unicode 代码点处理，兼容中文/emoji）。
 */
public final class MessageUtil {

    private MessageUtil() {}

    public static Component render(String template) {
        return render(template, new HashMap<>());
    }

    public static Component render(String template, Map<String, String> placeholders) {
        String filled = applyPlaceholders(template, placeholders);
        return parseGradient(filled);
    }

    public static Component withPrefix(Component prefix, Component content) {
        if (prefix == null || prefix.getString().isEmpty()) return content;
        MutableComponent result = Component.empty();
        result.append(prefix);
        result.append(Component.literal(" "));
        result.append(content);
        return result;
    }

    private static String applyPlaceholders(String s, Map<String, String> map) {
        String out = s;
        for (Map.Entry<String, String> e : map.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private static Component parseGradient(String s) {
        MutableComponent out = Component.empty();
        int idx = 0;
        while (true) {
            int open = s.indexOf("<gradient:", idx);
            if (open < 0) {
                out.append(Component.literal(s.substring(idx)));
                break;
            }
            if (open > idx) {
                out.append(Component.literal(s.substring(idx, open)));
            }
            int closeTag = s.indexOf('>', open);
            int endTag = s.indexOf("</gradient>", closeTag + 1);
            if (closeTag < 0 || endTag < 0) {
                out.append(Component.literal(s.substring(open)));
                break;
            }
            String header = s.substring(open + "<gradient:".length(), closeTag);
            String inner = s.substring(closeTag + 1, endTag);

            String[] parts = header.split(":");
            if (parts.length != 2) {
                out.append(Component.literal(s.substring(open, endTag + "</gradient>".length())));
                idx = endTag + "</gradient>".length();
                continue;
            }
            Integer c1 = parseHexColor(parts[0]);
            Integer c2 = parseHexColor(parts[1]);
            if (c1 == null || c2 == null) {
                out.append(Component.literal(s.substring(open, endTag + "</gradient>".length())));
                idx = endTag + "</gradient>".length();
                continue;
            }
            out.append(gradient(inner, c1, c2));
            idx = endTag + "</gradient>".length();
        }
        return out;
    }

    private static Component gradient(String inner, int c1, int c2) {
        int[] cps = inner.codePoints().toArray();
        if (cps.length == 0) return Component.empty();
        MutableComponent out = Component.empty();
        for (int i = 0; i < cps.length; i++) {
            float t = (cps.length == 1) ? 0f : (float) i / (float) (cps.length - 1);
            int rgb = lerpColor(c1, c2, t);
            TextColor color = TextColor.fromRgb(rgb);
            String ch = new String(Character.toChars(cps[i]));
            out.append(Component.literal(ch).withStyle(Style.EMPTY.withColor(color)));
        }
        return out;
    }

    private static Integer parseHexColor(String s) {
        String x = s.trim();
        if (x.startsWith("#")) x = x.substring(1);
        if (x.length() != 6) return null;
        try {
            return Integer.parseInt(x, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }
}