package com.example.damian.monitorapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.michaldrabik.tapbarmenulib.TapBarMenu;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.tapBarMenu)
    TapBarMenu tapBarMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @OnClick(R.id.tapBarMenu)
    public void onMenuButtonClick() {
        tapBarMenu.toggle();
    }

    public void aboutMe(View view){
        Intent intent = new Intent(this, AbouteMe.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.registerItem:
                Toast.makeText(this,"Register",Toast.LENGTH_SHORT).show();
                return true;

            case R.id.loginItem:
                Toast.makeText(this,"Login",Toast.LENGTH_SHORT).show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
        return true;
        }
    }


}
