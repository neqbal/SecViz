package com.example.secviz.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.secviz.R;
import com.example.secviz.data.StackBlock;

import java.util.ArrayList;
import java.util.List;

public class StackCanvasView extends View {

    private List<StackBlock> stack = new ArrayList<>();
    private int espIndex = 0;
    private int ebpIndex = 0;
    private String userInput = "";
    private int step = 0;
    private String levelId = "";
    private boolean isPatched = false;

    // Animation
    private float fillHeight = 0f;
    private float animEspY = 0f;
    private float animEbpY = 0f;
    private float time = 0f;

    private final Paint blockPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint addressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fluidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StackCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StackCanvasView(Context context) {
        super(context);
        init();
    }

    private void init() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        labelPaint.setTextSize(28f);
        labelPaint.setColor(Color.parseColor("#C9D1D9"));
        valuePaint.setTextSize(28f);
        valuePaint.setFakeBoldText(true);
        addressPaint.setTextSize(22f);
        addressPaint.setColor(Color.parseColor("#8B949E"));
        addressPaint.setTextAlign(Paint.Align.RIGHT);
        pointerPaint.setTextAlign(Paint.Align.CENTER);
        pointerPaint.setTextSize(26f);
        pointerPaint.setFakeBoldText(true);
        pointerPaint.setColor(Color.parseColor("#0D1117"));
        // Animate continuously
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    // ── Public setters ─────────────────────────────────────────────────────────

    public void setStack(List<StackBlock> stack) {
        this.stack = stack;
        invalidate();
    }

    public void setEspIndex(int idx) { this.espIndex = idx; invalidate(); }
    public void setEbpIndex(int idx) { this.ebpIndex = idx; invalidate(); }

    public void setUserInput(String input) { this.userInput = input; }
    public void setStep(int s) { this.step = s; }
    public void setLevelId(String id) { this.levelId = id; }
    public void setIsPatched(boolean p) { this.isPatched = p; }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (stack.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();

        float startX = 140f;
        float startY = 20f;
        float blockW = w * 0.55f;
        float blockH = Math.min(90f, (h - 40f) / stack.size());

        time += 0.05f;

        // ── Target state ──────────────────────────────────────────────────────
        int bufferSize = levelId.equals("2a") ? Math.min(userInput.length(), 16) : userInput.length();
        float targetFill = (step >= 5 && bufferSize > 0) ? (bufferSize / 8f) * blockH : 0f;
        float targetEspY = startY + (stack.size() - 1 - espIndex) * blockH + blockH / 2f;
        float targetEbpY = startY + (stack.size() - 1 - ebpIndex) * blockH + blockH / 2f;

        fillHeight = lerp(fillHeight, targetFill, 0.1f);
        animEspY = lerp(animEspY, targetEspY, 0.15f);
        animEbpY = lerp(animEbpY, targetEbpY, 0.15f);

        // ── Draw stack blocks ─────────────────────────────────────────────────
        for (int i = 0; i < stack.size(); i++) {
            StackBlock block = stack.get(i);
            int vizIdx = stack.size() - 1 - i;
            float y = startY + vizIdx * blockH;

            blockPaint.setColor(blockColor(block.type));
            blockPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(startX, y, startX + blockW, y + blockH, blockPaint);

            borderPaint.setColor(borderColor(block.type));
            canvas.drawRect(startX, y, startX + blockW, y + blockH, borderPaint);

            // Address
            canvas.drawText(block.address, startX - 8f, y + blockH / 2f + 8f, addressPaint);

            // Label
            valuePaint.setColor(Color.parseColor("#C9D1D9"));
            valuePaint.setTextSize(26f);
            canvas.drawText(block.label, startX + 12f, y + 32f, labelPaint);

            // Value
            valuePaint.setColor(valueColor(block.type));
            valuePaint.setTextSize(28f);
            String val = block.value.replace("\\0", "·");
            canvas.drawText(val, startX + 12f, y + blockH - 16f, valuePaint);
        }

        // ── Fluid simulation ──────────────────────────────────────────────────
        if (fillHeight > 0.5f) {
            List<Integer> buffIndices = new ArrayList<>();
            for (int i = 0; i < stack.size(); i++) {
                if (stack.get(i).label.startsWith("buff[")) buffIndices.add(i);
            }
            if (!buffIndices.isEmpty()) {
                int minViz = Integer.MAX_VALUE, maxViz = Integer.MIN_VALUE;
                for (int i : buffIndices) {
                    int viz = stack.size() - 1 - i;
                    if (viz < minViz) minViz = viz;
                    if (viz > maxViz) maxViz = viz;
                }
                float bufTopY = startY + minViz * blockH;
                float bufBotY = startY + (maxViz + 1) * blockH;
                float totalBufH = bufBotY - bufTopY;
                float fluidTopY = Math.max(bufTopY, bufBotY - fillHeight);
                boolean isOverflow = fillHeight > totalBufH;

                int colorTop, colorBot;
                if (isOverflow && !isPatched) {
                    colorTop = Color.argb(153, 248, 81, 73);
                    colorBot = Color.argb(51, 248, 81, 73);
                } else {
                    colorTop = Color.argb(153, 88, 166, 255);
                    colorBot = Color.argb(51, 88, 166, 255);
                }

                LinearGradient gradient = new LinearGradient(
                        0, fluidTopY, 0, bufBotY,
                        colorTop, colorBot, Shader.TileMode.CLAMP);
                fluidPaint.setShader(gradient);

                // Clip to buffer region only
                canvas.save();
                canvas.clipRect(startX, startY, startX + blockW, h);

                Path path = new Path();
                path.moveTo(startX, bufBotY);
                path.lineTo(startX, fluidTopY);
                float step5 = blockW / 20f;
                for (int i = 0; i <= 20; i++) {
                    float px = startX + i * step5;
                    float wave = (float) Math.sin(time + i * 0.3f) * 6f;
                    path.lineTo(px, fluidTopY + wave);
                }
                path.lineTo(startX + blockW, bufBotY);
                path.close();
                canvas.drawPath(path, fluidPaint);
                canvas.restore();
            }
        }

        // ── ESP / EBP pointers ────────────────────────────────────────────────
        drawPointer(canvas, "ESP", animEspY, startX + blockW, Color.parseColor("#58A6FF"), false);
        if (Math.abs(animEspY - animEbpY) > 5f) {
            drawPointer(canvas, "EBP", animEbpY, startX + blockW, Color.parseColor("#A371F7"), true);
        }

        // Continuously animate
        invalidate();
    }

    private void drawPointer(Canvas canvas, String name, float y, float rightX,
                             int color, boolean stagger) {
        float px = rightX + 15f + (stagger ? 20f : 0f);
        float arrowW = 12f;
        float boxW = 70f;
        float boxH = 36f;
        float boxTop = y - boxH / 2f;

        // Arrow
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        Path arrow = new Path();
        arrow.moveTo(px, y);
        arrow.lineTo(px + arrowW, boxTop);
        arrow.lineTo(px + arrowW + boxW, boxTop);
        arrow.lineTo(px + arrowW + boxW, boxTop + boxH);
        arrow.lineTo(px + arrowW, boxTop + boxH);
        arrow.close();
        canvas.drawPath(arrow, p);

        // Label
        pointerPaint.setColor(Color.parseColor("#0D1117"));
        canvas.drawText(name, px + arrowW + boxW / 2f, y + 9f, pointerPaint);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private int blockColor(String type) {
        switch (type) {
            case StackBlock.TYPE_MAIN_FRAME: return Color.argb(180, 30, 17, 40);
            case StackBlock.TYPE_SAFE:       return Color.argb(180, 13, 31, 15);
            case StackBlock.TYPE_WARN:       return Color.argb(180, 31, 21, 0);
            case StackBlock.TYPE_DANGER:     return Color.argb(200, 31, 13, 13);
            case StackBlock.TYPE_FILLED:     return Color.argb(180, 13, 31, 46);
            default:                         return Color.argb(200, 33, 38, 45);
        }
    }

    private int borderColor(String type) {
        switch (type) {
            case StackBlock.TYPE_DANGER: return Color.argb(153, 248, 81, 73);
            case StackBlock.TYPE_WARN:   return Color.argb(153, 210, 153, 34);
            default:                     return Color.argb(25, 255, 255, 255);
        }
    }

    private int valueColor(String type) {
        switch (type) {
            case StackBlock.TYPE_DANGER: return Color.parseColor("#FF7B72");
            case StackBlock.TYPE_WARN:   return Color.parseColor("#D29922");
            default:                     return Color.WHITE;
        }
    }
}
