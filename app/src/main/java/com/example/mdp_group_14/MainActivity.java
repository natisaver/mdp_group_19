package com.example.mdp_group_14;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Initialization
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        SectionsPagerAdapter sectionsPagerAdapter2 = new SectionsPagerAdapter(getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        sectionsPagerAdapter2.addFragment(new Home(),"Home");
        sectionsPagerAdapter2.addFragment(new BluetoothSetUp(),"Bluetooth");
        sectionsPagerAdapter2.addFragment(new EmergencyFragment(),"Add Obstacle");

        ViewPager viewPager2 = findViewById(R.id.view_pager2);
        viewPager2.setAdapter(sectionsPagerAdapter2);
        viewPager2.setOffscreenPageLimit(2);


        TabLayout tabs2 = findViewById(R.id.tabs2);
        tabs2.setupWithViewPager(viewPager2);

        // Set up sharedPreferences
        MainActivity.context = getApplicationContext();


    }


}