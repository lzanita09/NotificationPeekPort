/*
 * Copyright (C) 2014 ParanoidAndroid Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reindeercrafts.notificationpeek.peek;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import com.reindeercrafts.notificationpeek.utils.SwipeHelper;

public class NotificationLayout extends LinearLayout implements SwipeHelper.Callback {

    private SwipeHelper mSwipeHelper;

    private NotificationPeek mNotificationPeek;

    public NotificationLayout(Context context) {
        this(context, null);
    }

    public NotificationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        float densityScale = context.getResources().getDisplayMetrics().density;
        float pagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
        mSwipeHelper = new SwipeHelper(SwipeHelper.X, this, densityScale, pagingTouchSlop);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getContext().getResources().getDisplayMetrics().density;
        mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mSwipeHelper.onInterceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mSwipeHelper.onTouchEvent(event) || super.onTouchEvent(event);
    }

    public void setNotificationPeek(NotificationPeek peek) {
        mNotificationPeek = peek;
    }

    public NotificationPeek getNotificationPeek() {
        return mNotificationPeek;
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return getChildContentView(null);
    }

    @Override
    public View getChildContentView(View v) {
        return mNotificationPeek.getNotificationView();
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        StatusBarNotification n =
                (StatusBarNotification) mNotificationPeek.getNotificationView().getTag();
        return n.isClearable();
    }

    @Override
    public void onChildDismissed(View v) {
        StatusBarNotification n =
                (StatusBarNotification) mNotificationPeek.getNotificationView().getTag();
        String pkg = n.getPackageName();
        String tag = n.getTag();
        int id = n.getId();

        // Dismiss action confirmed, we need to remove the current notification.
        mNotificationPeek
                .onChildDismissed(NotificationHelper.getContentDescription(n), pkg, tag, id);
        mNotificationPeek.setAnimating(false);
    }

    @Override
    public void onBeginDrag(View v) {
        mNotificationPeek.setAnimating(true);
    }

    @Override
    public void onDragCancelled(View v) {
        mNotificationPeek.setAnimating(false);
    }

    @Override
    public void onAlphaChanged(float alpha) {
        mNotificationPeek.updateNotificationTextAlpha(alpha);
    }

    @Override
    public void onShowContentActionDetected() {
        Intent intent =
                new Intent(NotificationPeekActivity.NotificationPeekReceiver.ACTION_SHOW_CONTENT);
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onHideContentActionDetected() {
        Intent intent =
                new Intent(NotificationPeekActivity.NotificationPeekReceiver.ACTION_HIDE_CONTENT);
        getContext().sendBroadcast(intent);
    }
}