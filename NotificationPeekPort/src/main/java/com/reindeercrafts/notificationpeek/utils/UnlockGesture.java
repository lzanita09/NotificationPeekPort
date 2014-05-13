package com.reindeercrafts.notificationpeek.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Helper class for creating GestureDetector instance.
 * <p/>
 * Created by zhelu on 5/12/14.
 */
public class UnlockGesture {

    public static View.OnTouchListener createTouchListener(Context context,
                                                           UnlockGestureCallback callback) {
        final GestureDetector detector =
                new GestureDetector(context, new UnlockGestureDoubleTapListener(callback));

        View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        };

        return listener;
    }


    public interface UnlockGestureCallback {
        public void onUnlocked();
    }

    /**
     * Unlock screen gesture listener class. Here we can customize different gestures for
     * unlocking screen.
     */
    public static class UnlockGestureDoubleTapListener
            extends GestureDetector.SimpleOnGestureListener {

        private UnlockGestureCallback mCallback;

        public UnlockGestureDoubleTapListener(UnlockGestureCallback mCallback) {
            this.mCallback = mCallback;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mCallback != null) {
                mCallback.onUnlocked();
            }
            return true;
        }

    }
}
