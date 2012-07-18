package com.rainmoon.podcast;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * The home activity of the application.
 * 
 * @author trung nguyen
 * 
 */
public class HomeActivity extends FragmentActivity {
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_home);
  }
  
}
