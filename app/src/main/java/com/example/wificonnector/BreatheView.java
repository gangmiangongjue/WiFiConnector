package com.example.wificonnector;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Created by XuZhen on 2019/5/13 18:02
 */
public class BreatheView extends View {
    private final static String TAG ="BreatheView";
    private int mInsideViewWidth = dp2px(getContext(), 70);
    private int mInsideViewHeight = dp2px(getContext(), 40);
    private int mLightDiffusionWidth = dp2px(getContext(), 20);  //扩散区宽度
    private int mTextColor = Color.parseColor("#ffffff"); //默认字体颜色为白色
    private int mTextSize = sp2px(getContext(), 22);
    private int mInsideColor = Color.parseColor("#9FB6FF");//内部图形颜色，为蓝色
    private boolean isLight = false; //控制是否开启动画

    private int[] colors = {Color.parseColor("#9FB6FF"), Color.parseColor("#A7BAFE"), Color.parseColor("#F26882"), Color.parseColor("#F8DF57"),Color.BLACK,Color.DKGRAY,Color.GRAY,Color.RED,Color.GREEN,Color.BLUE,Color.CYAN,Color.MAGENTA};//变动颜色

    private int mOutsideColor;
    private TextPaint mTextPaint;
    private Paint mInsideViewPaint;
    private float mOutsideRectHeight,mOutsideRectWidth;
    private Paint mOutsideCirclePaint;

    private int mRealWidth;
    private int mRealHeight;
    private LinearGradient mLinearGradient;
    private RadialGradient mRadialGradient;
    private String mBreatheViewText;
    private ValueAnimator mGoLightAnim;
    private ValueAnimator mBackLightAnim;
    private ValueAnimator mColorAnim;
    private int mColor;

    private int mEndGraidentColor = Color.parseColor("#2e2e3D");


    public BreatheView(Context context) {
        this(context, null);
    }

    public BreatheView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BreatheView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainAttributes(attrs);
        initPaint();
        measureText();
    }

    /**
     * 步骤一：根据xml属性，设置控件的属性
     *
     * @param attrs
     */
    private void obtainAttributes(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.BreatheView);
        mInsideViewWidth = typedArray.getDimensionPixelSize(R.styleable.BreatheView_insideViewWidth, mInsideViewWidth);
        mInsideViewHeight = typedArray.getDimensionPixelSize(R.styleable.BreatheView_insideViewHeight, mInsideViewHeight);
        mLightDiffusionWidth = (int) typedArray.getDimension(R.styleable.BreatheView_lightDiffusionWidth, mLightDiffusionWidth);

        mTextSize = typedArray.getDimensionPixelSize(R.styleable.BreatheView_BreatheViewTextSize, mTextSize);
        mTextColor = typedArray.getColor(R.styleable.BreatheView_BreatheViewTextColor, mTextColor);
        //控件文字
        mBreatheViewText = typedArray.getString(R.styleable.BreatheView_BreatheViewText);
        //内部颜色
        mInsideColor = typedArray.getColor(R.styleable.BreatheView_BreatheViewInsideColor, mInsideColor);
        //是否开启灯光扩散，在xml中设置
        isLight = typedArray.getBoolean(R.styleable.BreatheView_BreatheViewIsLight, isLight);
        typedArray.recycle();//回收资源
    }
    private float stringWidth= 0,stringHeight=0;
    /**
     * 步骤二：初始化绘制控件的笔触
     */
    private void initPaint() {
        //文本文字
        mTextPaint = new TextPaint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);
        //内部的图形
        mInsideViewPaint = new Paint();
        mInsideViewPaint.setStyle(Paint.Style.FILL);
        mInsideViewPaint.setAntiAlias(true);
        mInsideViewPaint.setColor(mInsideColor);


        //外部方区域图形
        mOutsideColor = mInsideColor;
        mOutsideRectHeight = mInsideViewHeight;
        mOutsideRectWidth = mInsideViewWidth;
        mLinearGradient = new LinearGradient(0, 0, 0, mOutsideRectHeight,
                new int[]{Color.WHITE, mOutsideColor, Color.WHITE}, new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP);


        //外部的圆形区域
        mOutsideCirclePaint = new Paint();
        mOutsideCirclePaint.setStyle(Paint.Style.FILL);
        mOutsideCirclePaint.setAntiAlias(true);
        mOutsideCirclePaint.setColor(mOutsideColor);
        mRadialGradient = new RadialGradient(mOutsideRectHeight / 2, mOutsideRectHeight / 2,
                mOutsideRectHeight / 2, mOutsideColor, Color.WHITE, Shader.TileMode.CLAMP);
        mOutsideCirclePaint.setShader(mRadialGradient);

    }

    private void measureText(){
        String[] textLines = mBreatheViewText.split("\n");

        stringWidth = mTextPaint.measureText(textLines[0]);
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        stringHeight = (Math.abs(fontMetrics.ascent) - fontMetrics.descent) / 2*textLines.length;
    }
    /**
     * 第三步，测试控件大小
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getPaddingBottom() + getPaddingTop() + Math.abs(mLightDiffusionWidth * 2) + mInsideViewHeight;
        int width = getPaddingLeft() + getPaddingRight() + mInsideViewWidth + mInsideViewHeight / 2 + mLightDiffusionWidth;
        mRealWidth = resolveSize(width, widthMeasureSpec);
        mRealHeight = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(mRealWidth, mRealHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
    private boolean textChange = false;
    public void setMessage(String msg){
        mBreatheViewText = msg;
        measureText();
        textChange = true;
        invalidate();
    }

    StaticLayout myStaticLayout;
    /**
     * 第四步，绘制控件
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //变色
        mInsideViewPaint.setColor(mInsideColor);
        mInsideViewPaint.setAlpha(150);

        canvas.save();//保存图层

        if (isLight) {
            mOutsideCirclePaint.setColor(mOutsideColor);
            mOutsideCirclePaint.setAlpha(150);

            canvas.drawCircle(mRealWidth/ 2,mRealHeight/2,Math.min(mOutsideRectWidth/ 2,mOutsideRectHeight/2),mOutsideCirclePaint);
            canvas.restore();
            canvas.save();
        }

        //绘制内部的长方形与半圆
        canvas.drawCircle(mRealWidth/ 2,mRealHeight/2,Math.min(mInsideViewWidth/ 2,mInsideViewHeight/2),mInsideViewPaint);
        canvas.restore();
        canvas.save();

        //文字的x轴坐标

        float x = (mRealWidth - stringWidth) / 2;
        //文字的y轴坐标

        float y = (float) mRealHeight / 2 - stringHeight;
        canvas.translate(x,y);
        if (myStaticLayout ==null)
            myStaticLayout = new StaticLayout(mBreatheViewText, mTextPaint, getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        if (textChange){
            myStaticLayout = new StaticLayout(mBreatheViewText, mTextPaint, getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            textChange = false;
        }
        myStaticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * 这边是设置动画，动画分成两个部分，第一部分，是扩散出去，第二部分是扩散回来，同时颜色渐变
     */
    public void startLightAnim() {
        mGoLightAnim = ValueAnimator.ofFloat(0f, mLightDiffusionWidth);
        mGoLightAnim.setDuration(1500);
        //动态开始设置扩散出去动画
        mGoLightAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //// TODO: 动画里面里面不应该不停的new对象，对内存会造成影响，,需要优化
                mOutsideRectHeight = (int) (mInsideViewHeight + (float) animation.getAnimatedValue() * 2);
                mOutsideRectWidth = (int) (mOutsideRectWidth + (float) animation.getAnimatedValue() * 2);
                setGradient();
                invalidate();
            }
        });
        mGoLightAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!isLight) {
                    return;
                }
                mGoLightAnim.setStartDelay(1500);
                mGoLightAnim.start();

            }
        });

        //扩散回来的颜色动画
        mBackLightAnim = ValueAnimator.ofFloat(mLightDiffusionWidth, 0f);
        mBackLightAnim.setDuration(1500);
        mBackLightAnim.setStartDelay(1500);
        mBackLightAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mOutsideRectHeight = (int) (mInsideViewHeight + (float) animation.getAnimatedValue() * 2);
                setGradient();
                invalidate();
            }
        });
        mBackLightAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!isLight) {
                    return;
                }
                mBackLightAnim.setDuration(1500);
                mBackLightAnim.start();


            }
        });

        //第二部分颜色渐变动画
        int i = (int) (Math.random() * colors.length-1);
        mColor = colors[i];
        mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), mInsideColor, mColor);
        mColorAnim.setDuration(1500);
        mColorAnim.setStartDelay(1500);
        mColorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mInsideColor = ((Integer) animation.getAnimatedValue());
                mOutsideColor = mInsideColor;
                invalidate();
            }
        });
        mColorAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!isLight) {
                    return;
                }
                mEndGraidentColor = mColor;
                int i = (int) (Math.random() * colors.length-1);
                mColor = colors[i];
                Log.e("TAG", i + "");
                mColorAnim.setObjectValues(mInsideColor, mColor);
                mColorAnim.setStartDelay(1500);
                mColorAnim.start();
            }
        });

        mGoLightAnim.start();
        mBackLightAnim.start();
        mColorAnim.start();
        isLight = true;
    }

    /**
     * 重新设置Gradient
     */
    private void setGradient() {
        mLinearGradient = new LinearGradient(0, 0, 0, mOutsideRectHeight,
                new int[]{mEndGraidentColor, mOutsideColor,mEndGraidentColor}, new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP);
        mRadialGradient = new RadialGradient(mOutsideRectHeight / 2, mOutsideRectHeight / 2,
                mOutsideRectHeight / 2, mOutsideColor, mEndGraidentColor, Shader.TileMode.CLAMP);
        mOutsideCirclePaint.setShader(mRadialGradient);
    }

    /**
     * 设置渐变底色
     * @param endGraidentColor
     */
    public void setEndGraidentColor(int endGraidentColor) {
        mEndGraidentColor = endGraidentColor;
    }

    /**
     * 关闭动画
     */
    public void cancelLightAnim() {
        isLight = false;//关闭动画
        if (mGoLightAnim != null) {
            mGoLightAnim.cancel();
        }
        if (mBackLightAnim != null) {
            mBackLightAnim.cancel();
        }
        if (mColorAnim != null) {
            mColorAnim.cancel();
        }
        mOutsideRectHeight = mInsideViewHeight;
        mInsideColor = mColor;
        mOutsideColor = mColor;
        invalidate();
    }

    /**
     * dp转换成px，scale为像素密度，density越高，分辨率越高
     *
     * @param context 获取屏幕
     * @param dpVal
     * @return
     */
    public static int dp2px(Context context, int dpVal) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpVal * scale + 0.5f);
    }

    /**
     * sp转换成px，fontSCale为文字像素密度，fontScale越高，分辨率越高
     *
     * @param context
     * @param spVal
     * @return
     */
    public static int sp2px(Context context, int spVal) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spVal * fontScale + 0.5f);
    }
}
