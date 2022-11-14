package com.benny.openlauncher.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.util.Tool;
import com.benny.openlauncher.viewutil.IconLabelItem;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import net.skywall.openlauncher.R;

import java.util.ArrayList;
import java.util.List;

public class DesktopOptionView extends FrameLayout {

    private final RecyclerView[] _actionRecyclerViews = new RecyclerView[2];
    private final List<FastItemAdapter<IconLabelItem>> _actionAdapters = new ArrayList<>(2);
    private DesktopOptionViewListener _desktopOptionViewListener;

    public DesktopOptionView(@NonNull Context context) {
        super(context);
        init();
    }

    public DesktopOptionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DesktopOptionView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setDesktopOptionViewListener(DesktopOptionViewListener desktopOptionViewListener) {
        _desktopOptionViewListener = desktopOptionViewListener;
    }

    public void updateHomeIcon(final boolean home) {
        post(() -> {
            if (home) {
                _actionAdapters.get(0).getAdapterItem(1)._icon = getContext().getResources().getDrawable(R.drawable.ic_star);
            } else {
                _actionAdapters.get(0).getAdapterItem(1)._icon = getContext().getResources().getDrawable(R.drawable.ic_star_border);
            }
            _actionAdapters.get(0).notifyAdapterItemChanged(1);
        });
    }

    public void updateLockIcon(final boolean lock) {
        if (_actionAdapters.size() == 0) return;
        if (_actionAdapters.get(0).getAdapterItemCount() == 0) return;
        post(new Runnable() {
            @Override
            public void run() {
                if (lock) {
                    _actionAdapters.get(0).getAdapterItem(2)._icon = getContext().getResources().getDrawable(R.drawable.ic_lock);
                } else {
                    _actionAdapters.get(0).getAdapterItem(2)._icon = getContext().getResources().getDrawable(R.drawable.ic_lock_open);
                }
                _actionAdapters.get(0).notifyAdapterItemChanged(2);
            }
        });
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
        return insets;
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        final int paddingHorizontal = Tool.dp2px(42);

        _actionAdapters.set(0, new FastItemAdapter<>());
        _actionAdapters.set(1, new FastItemAdapter<>());

        _actionRecyclerViews[0] = createRecyclerView(_actionAdapters.get(0), Gravity.TOP | Gravity.CENTER_HORIZONTAL, paddingHorizontal);
        _actionRecyclerViews[1] = createRecyclerView(_actionAdapters.get(1), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, paddingHorizontal);

        final com.mikepenz.fastadapter.listeners.OnClickListener<IconLabelItem> clickListener = (v, adapter, item, position) -> {
            if (_desktopOptionViewListener != null) {
                final int id = (int) item.getIdentifier();
                if (id == R.string.home) {
                    updateHomeIcon(true);
                    _desktopOptionViewListener.onSetHomePage();
                } else if (id == R.string.remove) {
                    if (!Setup.appSettings().getDesktopLock()) {
                        _desktopOptionViewListener.onRemovePage();
                    } else {
                        Tool.toast(getContext(), "Desktop is locked.");
                    }
                } else if (id == R.string.widget) {
                    if (!Setup.appSettings().getDesktopLock()) {
                        _desktopOptionViewListener.onPickWidget();
                    } else {
                        Tool.toast(getContext(), "Desktop is locked.");
                    }
                } else if (id == R.string.action) {
                    if (!Setup.appSettings().getDesktopLock()) {
                        _desktopOptionViewListener.onPickAction();
                    } else {
                        Tool.toast(getContext(), "Desktop is locked.");
                    }
                } else if (id == R.string.lock) {
                    Setup.appSettings().setDesktopLock(!Setup.appSettings().getDesktopLock());
                    updateLockIcon(Setup.appSettings().getDesktopLock());
                } else if (id == R.string.pref_title__settings) {
                    _desktopOptionViewListener.onLaunchSettings();
                } else {
                    return false;
                }
                return true;
            }
            return false;
        };

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int itemWidth = (getWidth() - 2 * paddingHorizontal) / 3;
                initItems(clickListener, itemWidth);
            }
        });
    }

    private void initItems(final com.mikepenz.fastadapter.listeners.OnClickListener<IconLabelItem> clickListener, int itemWidth) {
        List<IconLabelItem> itemsTop = new ArrayList<>();
        itemsTop.add(createItem(R.drawable.ic_delete, R.string.remove, itemWidth));
        itemsTop.add(createItem(R.drawable.ic_star, R.string.home, itemWidth));
        itemsTop.add(createItem(R.drawable.ic_lock, R.string.lock, itemWidth));
        _actionAdapters.get(0).set(itemsTop);
        _actionAdapters.get(0).withOnClickListener(clickListener);

        List<IconLabelItem> itemsBottom = new ArrayList<>();
        itemsBottom.add(createItem(R.drawable.ic_dashboard, R.string.widget, itemWidth));
        itemsBottom.add(createItem(R.drawable.ic_launch, R.string.action, itemWidth));
        itemsBottom.add(createItem(R.drawable.ic_settings, R.string.pref_title__settings, itemWidth));
        _actionAdapters.get(0).set(itemsBottom);
        _actionAdapters.get(0).withOnClickListener(clickListener);

        ((MarginLayoutParams) ((View) _actionRecyclerViews[0].getParent()).getLayoutParams()).topMargin = Tool.dp2px(Setup.appSettings().getSearchBarEnable() ? 36 : 4);
    }

    private RecyclerView createRecyclerView(FastItemAdapter<IconLabelItem> adapter, int gravity, int paddingHorizontal) {
        RecyclerView actionRecyclerView = new RecyclerView(getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        actionRecyclerView.setClipToPadding(false);
        actionRecyclerView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
        actionRecyclerView.setLayoutManager(linearLayoutManager);
        actionRecyclerView.setAdapter(adapter);
        actionRecyclerView.setOverScrollMode(OVER_SCROLL_ALWAYS);
        LayoutParams actionRecyclerViewLP = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionRecyclerViewLP.gravity = gravity;

        addView(actionRecyclerView, actionRecyclerViewLP);
        return actionRecyclerView;
    }

    private IconLabelItem createItem(int icon, int label, int width) {
        return new IconLabelItem(getContext(), icon, label)
                .withIdentifier(label)
                .withOnClickListener(null)
                .withTextColor(Color.WHITE)
                .withIconSize(36)
                .withIconColor(Color.WHITE)
                .withIconPadding(4)
                .withIconGravity(Gravity.TOP)
                .withWidth(width)
                .withTextGravity(Gravity.CENTER);
    }

    public interface DesktopOptionViewListener {
        void onRemovePage();

        void onSetHomePage();

        void onPickWidget();

        void onPickAction();

        void onLaunchSettings();
    }
}
