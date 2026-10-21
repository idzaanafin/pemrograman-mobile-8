package com.example.tugas8;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private final int navy = Color.rgb(7, 35, 44);
    private final int teal = Color.rgb(23, 142, 135);
    private final int secondary = Color.rgb(91, 112, 116);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(246, 250, 248));
        scrollView.addView(root);

        TextView eyebrow = text("TOWER 2  /  SENSOR ASSIST", 12, teal, true);
        root.addView(eyebrow);
        TextView title = text("Pilih alat bantu", 30, navy, true);
        title.setPadding(0, dp(6), 0, 0);
        root.addView(title);
        TextView subtitle = text("Gunakan sensor perangkat sesuai kebutuhanmu.", 15, secondary, false);
        subtitle.setPadding(0, dp(4), 0, dp(26));
        root.addView(subtitle);

        root.addView(toolCard(
                "01",
                "Arah Kiblat",
                "Temukan arah kiblat dengan kompas digital dan lokasi perangkat.",
                "Buka kompas",
                ToolActivity.TOOL_QIBLA
        ), marginParams(0, 0, 0, 16));
        root.addView(toolCard(
                "02",
                "Level Ketinggian",
                "Ukur estimasi ketinggian dan perubahan level dari titik awal.",
                "Buka pengukur",
                ToolActivity.TOOL_ALTITUDE
        ), marginParams(0, 0, 0, 16));

        TextView footer = text("Sensor akan aktif hanya saat halaman alat sedang dibuka.", 12, secondary, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(10), 0, 0);
        root.addView(footer);
        setContentView(scrollView);
    }

    private MaterialCardView toolCard(String number, String title, String description,
                                      String action, String tool) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(22));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(Color.rgb(224, 235, 232));
        card.setCardBackgroundColor(Color.WHITE);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openTool(tool));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView label = text(number, 12, teal, true);
        content.addView(label);
        TextView heading = text(title, 23, navy, true);
        heading.setPadding(0, dp(8), 0, dp(5));
        content.addView(heading);
        TextView body = text(description, 14, secondary, false);
        content.addView(body);
        TextView button = text(action + "  ���", 14, teal, true);
        button.setPadding(0, dp(18), 0, 0);
        content.addView(button);
        card.addView(content);
        return card;
    }

    private void openTool(String tool) {
        Intent intent = new Intent(this, ToolActivity.class);
        intent.putExtra(ToolActivity.EXTRA_TOOL, tool);
        startActivity(intent);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView result = new TextView(this);
        result.setText(value);
        result.setTextSize(size);
        result.setTextColor(color);
        result.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return result;
    }

    private LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
