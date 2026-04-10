package com.example.secviz.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.secviz.R;
import com.example.secviz.data.StackBlock;

import java.util.ArrayList;
import java.util.List;

public class StackCanvasView extends View {

    // ── Selection support ─────────────────────────────────────────────────────
    public enum SelectionMode {
        NONE,
        JUNK_SELECT,    // user taps first then last block for junk range
        ADDRESS_SELECT  // user taps the single block to receive the target address
    }

    public interface BlockTapListener {
        void onBlockTapped(int blockIndex);
    }

    /** Long-press on any stack block — fires regardless of selectionMode. */
    public interface BlockLongPressListener {
        void onBlockLongPressed(int blockIndex, StackBlock block);
    }

    private SelectionMode selectionMode = SelectionMode.NONE;
    private int junkStartIndex = -1;
    private int junkEndIndex   = -1;
    private int targetIndex    = -1;
    private BlockTapListener blockTapListener;
    private BlockLongPressListener blockLongPressListener;

    // Long-press detection
    private static final long LONG_PRESS_MS = 450;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private int longPressCandidateIdx = -1;
    private boolean longPressFired = false;

    // ── Simulation state ──────────────────────────────────────────────────────
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

    // Shake animation for invalid tap
    private float shakeOffsetX = 0f;
    private int shakeTargetBlock = -1;

    private final Paint blockPaint   = new Paint();
    private final Paint borderPaint  = new Paint();
    private final Paint labelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint addressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fluidPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        borderPaint.setStrokeWidth(2.5f);
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
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    // ── Public setters ────────────────────────────────────────────────────────

    public void setStack(List<StackBlock> stack) {
        this.stack = stack;
        requestLayout();
        invalidate();
    }

    public void setEspIndex(int idx) { this.espIndex = idx; invalidate(); }
    public void setEbpIndex(int idx) { this.ebpIndex = idx; invalidate(); }
    public void setUserInput(String input) { this.userInput = input; }
    public void setStep(int s) { this.step = s; }
    public void setLevelId(String id) { this.levelId = id; }
    public void setIsPatched(boolean p) { this.isPatched = p; }

    // ── Selection API ─────────────────────────────────────────────────────────

    public void setSelectionMode(SelectionMode mode) {
        this.selectionMode = mode;
        setClickable(mode != SelectionMode.NONE);
        invalidate();
    }

    public void setBlockTapListener(BlockTapListener listener) {
        this.blockTapListener = listener;
        setClickable(true); // always accept touch so long-press works
    }

    public void setBlockLongPressListener(BlockLongPressListener listener) {
        this.blockLongPressListener = listener;
        setClickable(true);
    }

    /** Programmatically mark the junk range (after wizard logic resolves order). */
    public void setJunkRange(int startIndex, int endIndex) {
        this.junkStartIndex = Math.min(startIndex, endIndex);
        this.junkEndIndex   = Math.max(startIndex, endIndex);
        invalidate();
    }

    public void clearJunkRange() {
        junkStartIndex = -1;
        junkEndIndex   = -1;
        invalidate();
    }

    /** Highlight a single block as the address target. */
    public void setTargetBlock(int index) {
        this.targetIndex = index;
        invalidate();
    }

    public void clearTargetBlock() {
        this.targetIndex = -1;
        invalidate();
    }

    public void clearSelection() {
        junkStartIndex = -1;
        junkEndIndex   = -1;
        targetIndex    = -1;
        selectionMode  = SelectionMode.NONE;
        setClickable(false);
        invalidate();
    }

    public int getJunkStartIndex() { return junkStartIndex; }
    public int getJunkEndIndex()   { return junkEndIndex; }
    public int getTargetIndex()    { return targetIndex; }

    /** Shake a specific block to indicate an invalid tap. */
    public void shakeBlock(int blockIndex) {
        shakeTargetBlock = blockIndex;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 18f, -14f, 10f, -6f, 0f);
        anim.setDuration(300);
        anim.addUpdateListener(a -> {
            shakeOffsetX = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    // ── Touch handling ────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchY = event.getY();
        int blockIdx = blockIndexFromY(touchY);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                longPressFired = false;
                longPressCandidateIdx = blockIdx;
                if (blockIdx >= 0 && blockLongPressListener != null) {
                    longPressRunnable = () -> {
                        if (blockIdx < stack.size()) {
                            longPressFired = true;
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            blockLongPressListener.onBlockLongPressed(blockIdx, stack.get(blockIdx));
                        }
                    };
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_MS);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                // Cancel long-press if finger moved significantly
                if (longPressRunnable != null && blockIndexFromY(touchY) != longPressCandidateIdx) {
                    longPressHandler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                longPressHandler.removeCallbacks(longPressRunnable != null ? longPressRunnable : () -> {});
                longPressRunnable = null;
                // Fire tap only if long-press did NOT fire and we're in a selection mode
                if (!longPressFired && selectionMode != SelectionMode.NONE
                        && blockIdx >= 0 && blockIdx < stack.size() && blockTapListener != null) {
                    blockTapListener.onBlockTapped(blockIdx);
                }
                longPressFired = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    /** Convert a Y pixel coordinate to a stack block index (0 = top/highest address). */
    private int blockIndexFromY(float y) {
        if (stack.isEmpty()) return -1;
        int w = getWidth();
        float startX = 140f;
        float startY = 20f;
        float blockW = w * 0.55f;
        float blockH = Math.min(90f, (getHeight() - 40f) / stack.size());

        for (int i = 0; i < stack.size(); i++) {
            int vizIdx = stack.size() - 1 - i;
            float top = startY + vizIdx * blockH;
            if (y >= top && y < top + blockH) return i;
        }
        return -1;
    }

    // ── Measure ───────────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        float density = getResources().getDisplayMetrics().density;
        int minHeight = (int) (60 * density * Math.max(1, stack.size()));

        int modeH = MeasureSpec.getMode(heightMeasureSpec);
        int finalH = (modeH == MeasureSpec.EXACTLY) ? h : Math.max(h, minHeight);

        setMeasuredDimension(w, finalH);
    }

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
        animEspY   = lerp(animEspY, targetEspY, 0.15f);
        animEbpY   = lerp(animEbpY, targetEbpY, 0.15f);

        // ── Draw stack blocks ─────────────────────────────────────────────────
        for (int i = 0; i < stack.size(); i++) {
            StackBlock block = stack.get(i);
            int vizIdx = stack.size() - 1 - i;
            float y = startY + vizIdx * blockH;

            // Shake offset for invalid-tap feedback
            float offsetX = (shakeTargetBlock == i) ? shakeOffsetX : 0f;

            // Selection colouring overrides
            int fillColor  = blockColor(block.type);
            int strokeColor = borderColor(block.type);
            if (selectionMode != SelectionMode.NONE) {
                boolean inJunk = junkStartIndex >= 0 && i >= junkStartIndex && i <= junkEndIndex;
                boolean isTarget = (targetIndex == i);
                boolean isFirstTap = (junkStartIndex >= 0 && junkEndIndex < 0 && i == junkStartIndex);
                if (isTarget) {
                    fillColor  = Color.argb(200, 31, 111, 235); // accent blue
                    strokeColor = Color.argb(220, 88, 166, 255);
                } else if (inJunk) {
                    fillColor  = Color.argb(190, 180, 50, 30);  // junk red
                    strokeColor = Color.argb(220, 248, 81, 73);
                } else if (isFirstTap) {
                    fillColor  = Color.argb(180, 210, 120, 30); // amber for first tap
                    strokeColor = Color.argb(220, 240, 160, 60);
                }
            }

            blockPaint.setColor(fillColor);
            blockPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(startX + offsetX, y, startX + offsetX + blockW, y + blockH, blockPaint);

            borderPaint.setColor(strokeColor);
            borderPaint.setStrokeWidth(selectionMode != SelectionMode.NONE ? 3f : 2f);
            canvas.drawRect(startX + offsetX, y, startX + offsetX + blockW, y + blockH, borderPaint);

            // Address (left of block — not shaken)
            canvas.drawText(block.address, startX - 8f, y + blockH / 2f + 8f, addressPaint);

            // Label
            labelPaint.setColor(Color.parseColor("#C9D1D9"));
            canvas.drawText(block.label, startX + offsetX + 12f, y + 32f, labelPaint);

            // Value
            valuePaint.setColor(valueColor(block.type));
            valuePaint.setTextSize(28f);
            String val = block.value.replace("\\0", "·");
            canvas.drawText(val, startX + offsetX + 12f, y + blockH - 16f, valuePaint);
        }

        // ── Fluid simulation (only when NOT in selection mode) ────────────────
        if (selectionMode == SelectionMode.NONE && fillHeight > 0.5f) {
            List<Integer> buffIndices = new ArrayList<>();
            for (int i = 0; i < stack.size(); i++) {
                if (stack.get(i).label.startsWith("buff[") || stack.get(i).label.startsWith("buf[")) buffIndices.add(i);
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

                int colorTop = isOverflow && !isPatched
                        ? Color.argb(153, 248, 81, 73)
                        : Color.argb(153, 88, 166, 255);
                int colorBot = isOverflow && !isPatched
                        ? Color.argb(51, 248, 81, 73)
                        : Color.argb(51, 88, 166, 255);

                LinearGradient gradient = new LinearGradient(
                        0, fluidTopY, 0, bufBotY, colorTop, colorBot, Shader.TileMode.CLAMP);
                fluidPaint.setShader(gradient);

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

        // ── ESP / EBP pointers (only when NOT in selection mode) ──────────────
        if (selectionMode == SelectionMode.NONE) {
            drawPointer(canvas, "ESP", animEspY, startX + blockW, Color.parseColor("#58A6FF"), false);
            if (Math.abs(animEspY - animEbpY) > 5f) {
                drawPointer(canvas, "EBP", animEbpY, startX + blockW, Color.parseColor("#A371F7"), true);
            }
        }

        // Continuously animate (always so selection pulses)
        invalidate();
    }

    private void drawPointer(Canvas canvas, String name, float y, float rightX,
                             int color, boolean stagger) {
        float px = rightX + 15f + (stagger ? 20f : 0f);
        float arrowW = 12f;
        float boxW = 70f;
        float boxH = 36f;
        float boxTop = y - boxH / 2f;

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

        pointerPaint.setColor(Color.parseColor("#0D1117"));
        canvas.drawText(name, px + arrowW + boxW / 2f, y + 9f, pointerPaint);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private int blockColor(String type) {
        switch (type) {
            case StackBlock.TYPE_MAIN_FRAME: return Color.argb(180, 30, 17, 40);
            case StackBlock.TYPE_SAFE:       return Color.argb(180, 13, 31, 15);
            case StackBlock.TYPE_WARN:       return Color.argb(180, 31, 21, 0);
            case StackBlock.TYPE_DANGER:     return Color.argb(200, 31, 13, 13);
            case StackBlock.TYPE_FILLED:     return Color.argb(180, 13, 31, 46);
            case StackBlock.TYPE_JUNK:       return Color.argb(190, 180, 50, 30);
            case StackBlock.TYPE_TARGET:     return Color.argb(200, 31, 111, 235);
            default:                         return Color.argb(200, 33, 38, 45);
        }
    }

    private int borderColor(String type) {
        switch (type) {
            case StackBlock.TYPE_DANGER:
            case StackBlock.TYPE_JUNK:   return Color.argb(153, 248, 81, 73);
            case StackBlock.TYPE_WARN:   return Color.argb(153, 210, 153, 34);
            case StackBlock.TYPE_TARGET: return Color.argb(200, 88, 166, 255);
            default:                     return Color.argb(25, 255, 255, 255);
        }
    }

    private int valueColor(String type) {
        switch (type) {
            case StackBlock.TYPE_DANGER:
            case StackBlock.TYPE_JUNK:   return Color.parseColor("#FF7B72");
            case StackBlock.TYPE_WARN:   return Color.parseColor("#D29922");
            case StackBlock.TYPE_TARGET: return Color.parseColor("#79C0FF");
            default:                     return Color.WHITE;
        }
    }
}
