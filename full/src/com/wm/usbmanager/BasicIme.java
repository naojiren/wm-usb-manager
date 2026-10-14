package com.wm.usbmanager;

import android.inputmethodservice.InputMethodService;
import android.graphics.Color;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small offline fallback so removal never leaves the head unit without a keyboard. */
public final class BasicIme extends InputMethodService {
    private boolean caps;
    @Override public View onCreateInputView() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(12, 8, 12, 8);
        panel.setBackgroundColor(Color.rgb(224, 231, 237));
        TextView title = new TextView(this);
        title.setText("向导备用键盘 · 英文 / 数字"); title.setTextSize(22);
        panel.addView(title);
        for (String line : new String[]{"1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm.,"}) {
            LinearLayout row = new LinearLayout(this);
            for (final char ch : line.toCharArray()) {
                Button key = new Button(this); key.setText(String.valueOf(ch));
                key.setTextSize(28); key.setAllCaps(false);
                key.setOnClickListener(v -> {
                    InputConnection c = getCurrentInputConnection();
                    if (c != null) c.commitText(String.valueOf(caps ? Character.toUpperCase(ch) : ch), 1);
                });
                row.addView(key, new LinearLayout.LayoutParams(0, (int)(80 * getResources().getDisplayMetrics().density + .5f), 1));
            }
            panel.addView(row);
        }
        LinearLayout row = new LinearLayout(this);
        String[] labels = {"大小写", "空格", "退格", "回车", "收起"};
        for (int i = 0; i < labels.length; i++) {
            final int action = i; Button key = new Button(this); key.setText(labels[i]); key.setTextSize(24); key.setAllCaps(false);
            key.setOnClickListener(v -> {
                InputConnection c = getCurrentInputConnection();
                if (action == 0) { caps = !caps; key.setText(caps ? "大写" : "小写"); }
                else if (c != null && action == 1) c.commitText(" ", 1);
                else if (c != null && action == 2) c.deleteSurroundingText(1, 0);
                else if (c != null && action == 3) {
                    c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    c.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                } else if (action == 4) requestHideSelf(0);
            });
            row.addView(key, new LinearLayout.LayoutParams(0, (int)(80 * getResources().getDisplayMetrics().density + .5f), action == 1 ? 3 : 1));
        }
        panel.addView(row); return panel;
    }
}
