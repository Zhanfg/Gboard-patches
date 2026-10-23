package cc.axymorrsen.gboardfusion.lsp;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private final String[][] features = {
        {FlagsProvider.EMOJI, "候选 Emoji 色彩修复", "将候选栏 Emoji 文字绘制设置为不透明。仅 Hook 已知的 Gboard 控件。"},
        {FlagsProvider.ACCESSIBILITY, "功能菜单无障碍增强", "改善 Gboard 顶部功能菜单的 TalkBack 窗口事件与标识。"},
        {FlagsProvider.CLIPBOARD, "剪贴板前台同步（实验性）", "仅在非密码输入、非无痕提示时请求 Gboard 自身的剪贴板监听器同步；可能有隐私影响。"},
        {FlagsProvider.ROUNDED, "Material 圆角键（实验性）", "仅当目标版本保留已知 Phenotype 字段，修改六项圆角键布尔标志。"},
        {FlagsProvider.LANGUAGE, "语音包下载去重（实验性）", "仅对已知的 18.3/18.4 队列方法进行短时去重，可能推迟语言包重试。"}
    };

    private TextView label(LinearLayout parent, String text, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(Color.rgb(32, 39, 53));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setPadding(0, 14, 0, 14);
        parent.addView(v);
        return v;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 22);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        label(root, "Gboard Fusion", 28, true);
        label(root, "LSPosed 运行时增强 · Preview 0.1", 15, false);
        label(root, "作用域只勾选原版 Gboard (com.google.android.inputmethod.latin)。修改开关后请强制停止并重启 Gboard。", 14, false);

        SharedPreferences prefs = getSharedPreferences(FlagsProvider.PREFS, MODE_PRIVATE);
        for (String[] feature : features) {
            String key = feature[0];
            Switch sw = new Switch(this);
            sw.setText(feature[1]);
            sw.setTextSize(17);
            sw.setPadding(0, pad / 2, 0, pad / 3);
            sw.setChecked(prefs.getBoolean(key, FlagsProvider.defaultFor(key)));
            sw.setOnCheckedChangeListener((btn, enabled) ->
                    prefs.edit().putBoolean(key, enabled).apply());
            root.addView(sw);
            label(root, feature[2], 13, false);
        }

        label(root, "说明：这是独立模块，不会修改 Gboard APK，也不会替代之前两个完整 MPP 补丁集。未匹配到的混淆方法将自动跳过。没有网络权限，不读取或上传键盘输入内容。", 13, false);
        label(root, "兼容性：有界、按目标类与方法签名检查。Android 17 真机尚未验证。", 13, false);
        setContentView(scroll);
    }
}
