package cn.bleu.horizontalpanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * <b>Project:</b> BleuStudio<br>
 * <b>Create Date:</b> 16/1/8<br>
 * <b>Author:</b> Gordon<br>
 * <b>Description:</b>
 * HorizontalPanel, like slidmenu, QQ slides.
 * <br>
 */
public class HorizontalPanel extends ViewGroup {

    public enum Status {
        OPEN(1),
        CLOSE(0);

        int value;

        Status(int value) {
            this.value = value;
        }

        public static Status valueOf(int value) {
            if (value == 0) {
                return CLOSE;
            } else if (value == 1) {
                return OPEN;
            } else {
                return CLOSE;
            }
        }
    }

    public static final float DEFAULT_CONTENT_PERCENT = 0.8f;

    private float mContentPercent = DEFAULT_CONTENT_PERCENT;
    private Status status = Status.CLOSE; // default close

    private View mLeftView;
    private View mRightView;

    private int mPanelDistance; // 抽屉可以打开的长度

    private int mTouchSlop;

    private int mInitMotionEventX;
    private int mInitMotionEventY;

    public HorizontalPanel(Context context) {
        this(context, null);
    }

    public HorizontalPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HorizontalPanel, defStyleAttr, 0);
        mContentPercent = a.getFloat(R.styleable.HorizontalPanel_content_percent, DEFAULT_CONTENT_PERCENT);
        status = Status.valueOf(a.getInt(R.styleable.HorizontalPanel_status, 0));
        a.recycle();

        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(ViewConfiguration.get(context));
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() != 2) {
            throw new IllegalArgumentException("Must include two Views in HorizontalPanel.");
        }

        mLeftView = getChildAt(0);
        mRightView = getChildAt(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int pWidth = MeasureSpec.getSize(widthMeasureSpec);
        mPanelDistance = (int) (pWidth * mContentPercent + 0.5f);

        mLeftView.measure(MeasureSpec.makeMeasureSpec(mPanelDistance, MeasureSpec.EXACTLY),
                          heightMeasureSpec);
        mRightView.measure(widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * {@inheritDoc}
     *
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        PanelLayoutParams lp = (PanelLayoutParams) mLeftView.getLayoutParams();
        layoutChildren(mLeftView,
                       l + lp.leftMargin,
                       t + lp.topMargin,
                       mPanelDistance - lp.rightMargin,
                       b - lp.bottomMargin);

        lp = (PanelLayoutParams) mRightView.getLayoutParams();
        int diffX = lp.diffx;
        if (diffX == 0) {
            if (status == Status.CLOSE) {
                diffX = mPanelDistance;
            }
        }
        layoutChildren(mRightView,
                       diffX + lp.leftMargin,
                       t + lp.topMargin,
                       diffX + r - lp.rightMargin,
                       b - lp.bottomMargin);
    }

    private void layoutChildren(View child, int l, int t, int r, int b) {
        child.layout(l, t, r, b);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        boolean shouldIntercept = false;

        int x = (int) ev.getX();
        int y = (int) ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                shouldIntercept = false;
                mInitMotionEventX = x;
                mInitMotionEventY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                // 需要拦截事件的条件：
                // 1. 横向滑动大于纵向滑动的差分时
                // 2. 按下的点在当前RightContent上面

                boolean isTarget = mRightView == findTopChildUnder(this, x, y);
                int diffX = Math.abs(mInitMotionEventX - x);
                int diffY = Math.abs(mInitMotionEventY - y);

                shouldIntercept = isTarget && diffX > mTouchSlop && diffX > diffY;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                shouldIntercept = false;
                break;
            }
        }
        Log.d("panel",
              String.format("onInterceptTouchEvent, ACTION: %s, intercept: %s",
                            getTouchEventName(action),
                            shouldIntercept));
        return shouldIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        Log.d("panel",
              String.format("onTouchEvent, ACTION: %s",
                            getTouchEventName(action)));

        final int x = (int) event.getX();
        final int y = (int) event.getY();
        boolean isTarget = mRightView == findTopChildUnder(this, x, y);
        if (!isTarget) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 如果RightView不是ViewGroup或者子View数量为0，则消耗该事件
                return !(mRightView instanceof ViewGroup) || ((ViewGroup) mRightView).getChildCount() == 0;
            case MotionEvent.ACTION_MOVE:
                final int scrollX = (int) (event.getX() - mInitMotionEventX + 0.5f);
                if (scrollX != 0) {
                    movePanel(scrollX);
                    break;
                } else {
                    return false;
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                finishMovePanel();
                return false;
            }
        }

        return true;
    }

    /**
     * 移动抽屉
     *
     * @param diffX 水平方向移动的位移
     */
    private void movePanel(int diffX) {
        final int x;
        if (status == Status.CLOSE) {
            x = diffX + mPanelDistance;
        } else {
            x = diffX;
        }

        if (x < 0 || x > mPanelDistance) {
            return;
        }

        PanelLayoutParams lp = (PanelLayoutParams) mRightView.getLayoutParams();
        lp.diffx = x;
        requestLayout();
    }

    private void finishMovePanel() {
        final int left = mRightView.getLeft();
        final PanelLayoutParams lp = (PanelLayoutParams) mRightView.getLayoutParams();
        if (left <= mPanelDistance * 0.5f) {
            status = Status.OPEN;
            lp.diffx = 0;
        } else {
            status = Status.CLOSE;
            lp.diffx = mLeftView.getRight();
        }
        requestLayout();
    }

    public static View findTopChildUnder(ViewGroup parent, int x, int y) {
        final int childCount = parent.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (x >= child.getLeft() && x < child.getRight() &&
                y >= child.getTop() && y < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    public static String getTouchEventName(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return "ACTION_DOWN";
            case MotionEvent.ACTION_MOVE:
                return "ACTION_MOVE";
            case MotionEvent.ACTION_UP:
                return "ACTION_UP";
            case MotionEvent.ACTION_CANCEL:
                return "ACTION_CANCEL";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new PanelLayoutParams(PanelLayoutParams.WRAP_CONTENT,
                                     PanelLayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new PanelLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new PanelLayoutParams(p);
    }

    public class PanelLayoutParams extends MarginLayoutParams {

        /** Indicator the offset of content panel */
        int diffx;

        /**
         * Creates a new set of layout parameters. The values are extracted from
         * the supplied attributes set and context.
         *
         * @param c     the application environment
         * @param attrs the set of attributes from which to extract the layout
         */
        public PanelLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        /**
         * {@inheritDoc}
         *
         * @param source
         */
        public PanelLayoutParams(LayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height and margin values of the source.
         *
         * @param source The layout params to copy from.
         */
        public PanelLayoutParams(MarginLayoutParams source) {
            super(source);
        }

        /**
         * {@inheritDoc}
         *
         * @param width
         * @param height
         */
        public PanelLayoutParams(int width, int height) {
            super(width, height);
        }


    }
}
