package cc.axymorrsen.gboardfusion.lsp;

import android.app.Application;
import android.content.Context;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.os.Build;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextPaint;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Runtime port of selected PixelBoard/Gboard-patches behavior, targeting stock Gboard only. */
public final class HookEntry implements IXposedHookLoadPackage {
    private static final String GBOARD = "com.google.android.inputmethod.latin";
    private static final String LOG = "GboardFusionLSP: ";
    private static final AtomicBoolean READY = new AtomicBoolean();
    private static final Map<View, Long> ACCESS_EVENTS = new WeakHashMap<>();
    private static final Map<Object, Long> CLIP_EVENTS = new WeakHashMap<>();
    private static final ConcurrentHashMap<String, Long> LANGUAGE_EVENTS = new ConcurrentHashMap<>();

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!GBOARD.equals(lp.packageName)) return;
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            XposedBridge.hookMethod(attach, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (!(param.thisObject instanceof Application) || !READY.compareAndSet(false, true)) return;
                    Context ctx = (Context) param.thisObject;
                    ClassLoader loader = ctx.getClassLoader();
                    if (loader == null) loader = lp.classLoader;
                    if (loader == null) return;
                    Features cfg = Features.read(ctx);
                    int hooks = 0;
                    if (cfg.emoji) hooks += emoji(loader);
                    if (cfg.accessibility) hooks += accessibility(loader);
                    if (cfg.clipboard) hooks += clipboard(loader);
                    if (cfg.rounded) hooks += rounded(loader);
                    if (cfg.language) hooks += language(loader);
                    XposedBridge.log(LOG + "loaded process=" + lp.processName + ", hooks=" + hooks
                            + ", flags [emoji,accessibility,clipboard,rounded,language]=" + cfg);
                }
            });
        } catch (Throwable ex) {
            XposedBridge.log(LOG + "bootstrap hook unavailable: " + ex.getClass().getSimpleName());
        }
    }

    private static final class Features {
        boolean emoji, accessibility, clipboard, rounded, language;
        static Features read(Context context) {
            Features c = new Features();
            c.emoji = readFlag(context, FlagsProvider.EMOJI);
            c.accessibility = readFlag(context, FlagsProvider.ACCESSIBILITY);
            c.clipboard = readFlag(context, FlagsProvider.CLIPBOARD);
            c.rounded = readFlag(context, FlagsProvider.ROUNDED);
            c.language = readFlag(context, FlagsProvider.LANGUAGE);
            return c;
        }
        @Override public String toString() {
            return emoji+","+accessibility+","+clipboard+","+rounded+","+language;
        }
    }

    private static boolean readFlag(Context ctx, String key) {
        // Public provider exposes only an explicit allowlist of 5 boolean preferences.
        android.net.Uri uri = android.net.Uri.parse("content://" + FlagsProvider.AUTHORITY + "/flag/" + key);
        try (Cursor c = ctx.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getInt(0) != 0;
        } catch (Throwable ignored) { }
        return FlagsProvider.defaultFor(key);
    }

    private static Class<?> find(ClassLoader loader, String name) {
        try { return Class.forName(name, false, loader); }
        catch (Throwable ignored) { return null; }
    }

    private static int hookExact(ClassLoader loader, String owner, String name,
                                 Class<?>[] params, XC_MethodHook hook) {
        Class<?> type = find(loader, owner);
        if (type == null) return 0;
        try {
            Method method = type.getDeclaredMethod(name, params);
            method.setAccessible(true);
            XposedBridge.hookMethod(method, hook);
            return 1;
        } catch (Throwable ignored) { return 0; }
    }

    // Port of PixelBoard's candidate-emoji paint opacity fix. Paint is restored on return.
    private static int emoji(ClassLoader loader) {
        return hookExact(loader,
                "com.google.android.libraries.inputmethod.widgets.text.AutoCenterScaleTextView",
                "onDraw", new Class<?>[]{Canvas.class}, new XC_MethodHook() {
                    private final ThreadLocal<Integer> oldColor = new ThreadLocal<>();
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        oldColor.remove();
                        if (!(param.thisObject instanceof TextView)) return;
                        TextView view = (TextView) param.thisObject;
                        if (!hasEmoji(view.getText())) return;
                        TextPaint paint = view.getPaint();
                        if (paint == null) return;
                        int old = paint.getColor();
                        oldColor.set(old);
                        paint.setColor(old | 0xFF000000);
                        paint.setAlpha(255);
                    }
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        Integer old = oldColor.get();
                        oldColor.remove();
                        if (old != null && param.thisObject instanceof TextView) {
                            ((TextView) param.thisObject).getPaint().setColor(old);
                        }
                    }
                });
    }

    private static boolean hasEmoji(CharSequence s) {
        if (s == null) return false;
        for (int i = 0; i < s.length();) {
            int cp = Character.codePointAt(s, i);
            if ((cp >= 0x1F000 && cp <= 0x1FAFF) || cp == 0xFE0F
                    || (cp >= 0x2600 && cp <= 0x27BF)) return true;
            i += Character.charCount(cp);
        }
        return false;
    }

    private static int accessibility(ClassLoader loader) {
        String root = "com.google.android.libraries.inputmethod.accesspoint.widget.";
        int count = 0;
        for (String c : new String[]{"AccessPointsPanelV2", "AccessPointsPanel"}) {
            count += hookExact(loader, root + c, "onFinishInflate", new Class<?>[0],
                    new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject instanceof View) prepareAccessibility((View) param.thisObject, false);
                        }
                    });
        }
        for (String c : new String[]{"AccessPointsPanelV2", "AccessPointsPanel", "AccessPointsPanelViewPage"}) {
            count += hookExact(loader, root + c, "onVisibilityChanged",
                    new Class<?>[]{View.class, int.class}, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject instanceof View) prepareAccessibility((View) param.thisObject, true);
                        }
                    });
        }
        return count;
    }

    private static void prepareAccessibility(View view, boolean visibility) {
        try {
            if (Build.VERSION.SDK_INT >= 28 && view.getAccessibilityPaneTitle() == null) {
                view.setAccessibilityPaneTitle("Keyboard options");
            }
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            if (!visibility || !view.isShown()) return;
            AccessibilityManager am = (AccessibilityManager) view.getContext()
                    .getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null || !am.isEnabled()) return;
            long now = SystemClock.elapsedRealtime();
            synchronized (ACCESS_EVENTS) {
                Long last = ACCESS_EVENTS.get(view);
                if (last != null && now - last < 300) return;
                ACCESS_EVENTS.put(view, now);
            }
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        } catch (Throwable ignored) { }
    }

    // PixelBoard 18.0.3 internal callback; no clipboard content is read by this module.
    // Only calls Gboard's own listener, which must honor the app's user setting.
    private static int clipboard(ClassLoader loader) {
        Class<?> owner = find(loader, "fml");
        if (owner == null) return 0;
        int count = 0;
        for (Method method : owner.getDeclaredMethods()) {
            if (!"ft".equals(method.getName()) || method.getReturnType() != boolean.class
                    || method.getParameterTypes().length != 5
                    || method.getParameterTypes()[1] != EditorInfo.class) continue;
            try {
                method.setAccessible(true);
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            EditorInfo info = (EditorInfo) param.args[1];
                            if (sensitiveInput(info)) return;
                            Object handler = null;
                            Class<?> clazz = param.thisObject.getClass();
                            for (; clazz != null && handler == null; clazz = clazz.getSuperclass()) {
                                try {
                                    Field f = clazz.getDeclaredField("f");
                                    f.setAccessible(true);
                                    handler = f.get(param.thisObject);
                                } catch (NoSuchFieldException ignored) { }
                            }
                            if (!(handler instanceof ClipboardManager.OnPrimaryClipChangedListener)) return;
                            long now = SystemClock.elapsedRealtime();
                            synchronized (CLIP_EVENTS) {
                                Long last = CLIP_EVENTS.get(handler);
                                if (last != null && now - last < 1800) return;
                                CLIP_EVENTS.put(handler, now);
                            }
                            ((ClipboardManager.OnPrimaryClipChangedListener) handler).onPrimaryClipChanged();
                        } catch (Throwable ignored) { }
                    }
                });
                count++;
            } catch (Throwable ignored) { }
        }
        return count;
    }

    private static boolean sensitiveInput(EditorInfo info) {
        if (info == null) return true;
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        if (cls == InputType.TYPE_CLASS_TEXT &&
                (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                 variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                 variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) return true;
        if (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) return true;
        return (info.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0;
    }

    // Optional, fail-closed hook. Only modifies Boolean-returning flag getters.
    private static int rounded(ClassLoader loader) {
        Class<?> klass = find(loader, "nxw");
        if (klass == null) return 0;
        int count = 0;
        for (Method m : klass.getDeclaredMethods()) {
            if (!"g".equals(m.getName()) || m.getParameterCount() != 0) continue;
            try {
                Field name = klass.getDeclaredField("a");
                if (name.getType() != String.class) continue;
                name.setAccessible(true);
                m.setAccessible(true);
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.getResult() instanceof Boolean)) return;
                        try {
                            String n = (String) name.get(param.thisObject);
                            if (n == null) return;
                            switch (n) {
                                case "belka_rounded_keyboard":
                                case "belka_ui":
                                case "belka_background":
                                case "more_pill_keys":
                                case "enable_rounded_key_by_default":
                                case "enable_rounded_keys":
                                    param.setResult(Boolean.TRUE);
                                    break;
                                default: break;
                            }
                        } catch (Throwable ignored) { }
                    }
                });
                count++;
            } catch (Throwable ignored) { }
        }
        return count;
    }

    // 18.3/18.4 only. Short TTL (15s) and 64-entry bound, not a permanent block.
    private static int language(ClassLoader loader) {
        int count = 0;
        for (String ownerName : new String[]{"sqk", "snm"}) {
            Class<?> owner = find(loader, ownerName);
            if (owner == null) continue;
            for (Method m : owner.getDeclaredMethods()) {
                Class<?>[] types = m.getParameterTypes();
                if (!"c".equals(m.getName()) || m.getReturnType() != void.class
                        || types.length != 3 || types[0] != String.class
                        || !Consumer.class.isAssignableFrom(types[2])) continue;
                try {
                    m.setAccessible(true);
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            String tag = param.args[0] instanceof String ? (String) param.args[0] : null;
                            if (tag == null || tag.isEmpty()) return;
                            String key = tag + "|" + String.valueOf(param.args[1]);
                            long now = SystemClock.elapsedRealtime();
                            if (LANGUAGE_EVENTS.size() > 64) LANGUAGE_EVENTS.clear();
                            Long old = LANGUAGE_EVENTS.put(key, now);
                            if (old != null && now - old < 15000) param.setResult(null);
                        }
                    });
                    count++;
                } catch (Throwable ignored) { }
            }
        }
        return count;
    }
}
