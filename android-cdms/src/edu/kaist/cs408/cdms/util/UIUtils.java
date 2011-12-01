/*
 * Copyright 2010 Google Inc.
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

package edu.kaist.cs408.cdms.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.ui.HomeActivity;
import edu.kaist.cs408.cdms.ui.LoginActivity;

public class UIUtils {

  private static final int BRIGHTNESS_THRESHOLD = 150;

  private static StyleSpan sBoldSpan = new StyleSpan(Typeface.BOLD);

  public static void setTitleBarColor(View titleBarView, int color) {
    final ViewGroup titleBar = (ViewGroup) titleBarView;
    titleBar.setBackgroundColor(color);

    /*
     * Calculate the brightness of the titlebar color, based on the commonly
     * known brightness formula:
     * http://en.wikipedia.org/wiki/HSV_color_space%23Lightness
     */
    int brColor = (30 * Color.red(color) + 59 * Color.green(color) + 11 * Color
        .blue(color)) / 100;
    if (brColor > BRIGHTNESS_THRESHOLD) {
      ((TextView) titleBar.findViewById(R.id.title_text)).setTextColor(titleBar
          .getContext().getResources().getColor(R.color.title_text_alt));

      // Iterate through all children of the titlebar and if they're a
      // LevelListDrawable,
      // set their level to 1 (alternate).
      // TODO: find a less hacky way of doing this.
      titleBar.post(new Runnable() {
        public void run() {
          final int childCount = titleBar.getChildCount();
          for (int i = 0; i < childCount; i++) {
            final View child = titleBar.getChildAt(i);
            if (child instanceof ImageButton) {
              final ImageButton childButton = (ImageButton) child;
              if (childButton.getDrawable() != null
                  && childButton.getDrawable() instanceof LevelListDrawable) {
                ((LevelListDrawable) childButton.getDrawable()).setLevel(1);
              }
            }
          }
        }
      });
    }
  }

  /**
   * Invoke "login" action, returning to {@link LoginActivity}.
   * 
   * @param context
   */
  public static void goLogin(Context context) {
    final Intent intent = new Intent(context, LoginActivity.class);
    context.startActivity(intent);
  }
  
  /**
   * Invoke "home" action, returning to {@link HomeActivity}.
   */
  public static void goHome(Context context) {
    final Intent intent = new Intent(context, HomeActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    context.startActivity(intent);
  }

  /**
   * Invoke "search" action, triggering a default search.
   */
  public static void goSearch(Activity activity) {
    activity.startSearch(null, false, Bundle.EMPTY, false);
  }

  /**
   * Populate the given {@link TextView} with the requested text, formatting
   * through {@link Html#fromHtml(String)} when applicable. Also sets
   * {@link TextView#setMovementMethod} so inline links are handled.
   */
  public static void setTextMaybeHtml(TextView view, String text) {
    if (text.contains("<") && text.contains(">")) {
      view.setText(Html.fromHtml(text));
      view.setMovementMethod(LinkMovementMethod.getInstance());
    } else {
      view.setText(text);
    }
  }

  /**
   * Given a snippet string with matching segments surrounded by curly braces,
   * turn those areas into bold spans, removing the curly braces.
   */
  public static Spannable buildStyledSnippet(String snippet) {
    final SpannableStringBuilder builder = new SpannableStringBuilder(snippet);

    // Walk through string, inserting bold snippet spans
    int startIndex = -1, endIndex = -1, delta = 0;
    while ((startIndex = snippet.indexOf('{', endIndex)) != -1) {
      endIndex = snippet.indexOf('}', startIndex);

      // Remove braces from both sides
      builder.delete(startIndex - delta, startIndex - delta + 1);
      builder.delete(endIndex - delta - 1, endIndex - delta);

      // Insert bold style
      builder.setSpan(sBoldSpan, startIndex - delta, endIndex - delta - 1,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      delta += 2;
    }

    return builder;
  }
 
}
