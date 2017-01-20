package com.example.slideup;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mancj.slideup.SlideUp;

public class SlideUpViewActivity extends AppCompatActivity {
    private SlideUp slideUp;
    private View dim;
    private View slideView;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_up_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        slideView = findViewById(R.id.slideView);
        dim = findViewById(R.id.dim);
        fab = (FloatingActionButton) findViewById(R.id.fab);


        SlideUp.Listener slideUpListener = new SlideUp.Listener() {
            @Override
            public void onSlide(float percent) {
                dim.setAlpha(1 - (percent / 100));
            }

            @Override
            public void onVisibilityChanged(int visibility) {
                if (visibility == View.GONE){
                    fab.show();
                }
            }
        };

        slideUp = SlideUp.Builder.forView(slideView)
                .withListeners(slideUpListener)
                .withDownToUpVector(false)
                .withLoggingEnabled(true)
                .withStartState(SlideUp.State.HIDDEN)
                .build();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slideUp.show();
                fab.hide();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_slide_up_view, menu);
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_slide_down){
//            startActivity(new Intent(this, SlideDownViewActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
