/*
 * Copyright 2013 Evelio Tarazona Cáceres <evelio@evelio.info>
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
package com.reindeercrafts.notificationpeek.views;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * A Drawable that draws an oval with given {@link android.graphics.Bitmap}
 */
public class RoundedAvatarDrawable extends Drawable {
  private final Bitmap mBitmap;
  private final Paint mPaint;
  private final RectF mRectF;
  private final int mBitmapWidth;
  private final int mBitmapHeight;
  private final float mShadowOffset;
  private final Paint mShadowPaint;

  public RoundedAvatarDrawable(Bitmap bitmap, float shadowSize, int shadowColor) {
    mBitmap = bitmap;
    mRectF = new RectF();
    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);
    mShadowPaint = new Paint(mPaint);

    final BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    mPaint.setShader(shader);

    // Reason to use another paint for this is explained below
    mShadowPaint.setColor(Color.WHITE);
    mShadowPaint.setShadowLayer(shadowSize, 0.0f, 0.0f, shadowColor);

    // NOTE: we assume bitmap is properly scaled to current density
    mShadowOffset = shadowSize;
    mBitmapWidth = (int) (mBitmap.getWidth() + mShadowOffset + 0.5f);
    mBitmapHeight = (int) (mBitmap.getHeight() + mShadowOffset + 0.5f);
  }

  @Override
  public void draw(Canvas canvas) {
    // Overdraw dragons ahead
    // We draw another oval below our actual bitmap so shadow is drawn properly
    canvas.drawOval(mRectF, mShadowPaint);
    canvas.drawOval(mRectF, mPaint);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);

    mRectF.set(bounds);
    mRectF.inset(mShadowOffset, mShadowOffset);
  }

  @Override
  public void setAlpha(int alpha) {
    if (mPaint.getAlpha() != alpha) {
      mPaint.setAlpha(alpha);
      invalidateSelf();
    }
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mPaint.setColorFilter(cf);
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public int getIntrinsicWidth() {
    return mBitmapWidth;
  }

  @Override
  public int getIntrinsicHeight() {
    return mBitmapHeight;
  }

  public void setAntiAlias(boolean aa) {
    mPaint.setAntiAlias(aa);
    invalidateSelf();
  }

  @Override
  public void setFilterBitmap(boolean filter) {
    mPaint.setFilterBitmap(filter);
    invalidateSelf();
  }

  @Override
  public void setDither(boolean dither) {
    mPaint.setDither(dither);
    invalidateSelf();
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }

  // TODO allow set and use target density, mutate, constant state, changing configurations, etc.
}