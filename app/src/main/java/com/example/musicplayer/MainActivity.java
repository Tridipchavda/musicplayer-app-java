package com.example.musicplayer;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.media.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaParser;
import android.media.MediaPlayer;
import android.media.VolumeShaper;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // Making Global Variables for Application
    int counter = 0;
    MediaPlayer mp = new MediaPlayer();

    Button play,prev,next;
    TextView songname,duration,Sduration ;
    ImageView _that,imageView;
    SeekBar seekBar;

    // Thread for changing duration at every 0.5 sec
    Thread updateSongStatus;

    BroadcastReceiver broadcastReceiver;

    // Main Function runs MainActivity
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate( Bundle savedInstanceState) {
        // Creating the MainActivity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assigning Variable to Buttons,Texts,Images
        play = findViewById(R.id.btnPlay);
        prev = findViewById(R.id.btnPrev);
        next = findViewById(R.id.btnNext);

        songname= findViewById(R.id.songText);
        duration = findViewById(R.id.duration);
        Sduration = findViewById(R.id.Sduration);

        _that = findViewById(R.id._that);
        imageView = findViewById(R.id.imageView);
        seekBar = findViewById(R.id.seekBar);

        // Checking For Media Permission
        if(getPermission()==false){
            setPermission();
        }


        // Structure to store Song Details in getContentResolver
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };
        // String For Music Check
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        // Making Cursor for Iteration
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,selection,null,null);
        // Arraylist for storing Song Data in songlist
        ArrayList<AudioModel> songlist = new ArrayList<>();

        // Setting up data from cursor/finded songs to songlist
        while(cursor.moveToNext()){
            AudioModel song = new AudioModel(cursor.getString(0),cursor.getString(1),cursor.getString((2)));
            if(new File(song.getPath()).exists()){
                songlist.add(song);
            }
        }

        // Setting Up Media Player
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        String songPath = songlist.get(counter).getPath();

        Uri audioURI = Uri.parse(songPath);
        // Setting Up song Heading
        if(songlist.get(counter).getTitle().length()>20) {
            songname.setText(songlist.get(counter).getTitle().substring(0, 20) + "...");
        }
        else{
            songname.setText(songlist.get(counter).getTitle());
        }
        duration.setText(getStringTime(songlist.get(counter)));

        try {
            mp.setDataSource(this, audioURI);
            mp.prepare();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        // Thread runs for every 0.5 sec for update Status
        class UpdateSongStatus extends Thread{
            int duration=0;
            boolean running = true;
            UpdateSongStatus(int duration){
                this.duration = duration;
            }

            @Override
            public void interrupt(){
                running = false;
            }

            @Override
            public void run() {
                int total = this.duration;
                int current = 0;

                while (current<total && running){
                    try{
                        sleep(500);
                        current = mp.getCurrentPosition();
                        seekBar.setProgress(current);

                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                next.performClick();
                            }
                        });

                    }catch (InterruptedException | IllegalStateException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        updateSongStatus = new UpdateSongStatus(mp.getDuration());

        // Building Notification

        Drawable drawable = ResourcesCompat.getDrawable(getResources(),R.drawable.logo,null);
        BitmapDrawable bitmap = (BitmapDrawable) drawable;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification;

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getExtras().getString("actionname");

                switch (action){
                    case "prev":
                        prev.performClick();
                        break;
                    case "play":
                        play.performClick();
                        break;
                    case "next":
                        next.performClick();
                        break;
                }
            }
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            Intent intentPrev = new Intent(MainActivity.this, NotificationActionService.class).setAction("prev");
            PendingIntent prev = PendingIntent.getBroadcast(MainActivity.this,0,intentPrev,PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentPlay = new Intent(MainActivity.this, NotificationActionService.class).setAction("play");
            PendingIntent play = PendingIntent.getBroadcast(MainActivity.this,0,intentPlay,PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentNext = new Intent(MainActivity.this, NotificationActionService.class).setAction("next");
            PendingIntent next = PendingIntent.getBroadcast(MainActivity.this,0,intentNext,PendingIntent.FLAG_UPDATE_CURRENT);


            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.logo)
                    .addAction(R.drawable.previous,"Prev",prev)
                    .addAction(R.drawable.play,"Play",play)
                    .addAction(R.drawable.next,"Next",next)
                    .setSubText("Change Song")
                    .setChannelId("Songs")
                    .setOngoing(true)
                    .build();

            manager.createNotificationChannel(new NotificationChannel("Songs","Song Channel",NotificationManager.IMPORTANCE_HIGH));

            registerReceiver(broadcastReceiver,new IntentFilter("TRACKS_TRACKS"));
            startService(new Intent(MainActivity.this, ClearFromService.class));

        }else{
            Intent intentPrev = new Intent(MainActivity.this, NotificationActionService.class).setAction("prev");
            PendingIntent prev = PendingIntent.getBroadcast(MainActivity.this,0,intentPrev,PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentPlay = new Intent(MainActivity.this, NotificationActionService.class).setAction("play");
            PendingIntent play = PendingIntent.getBroadcast(MainActivity.this,0,intentPlay,PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentNext = new Intent(MainActivity.this, NotificationActionService.class).setAction("next");
            PendingIntent next = PendingIntent.getBroadcast(MainActivity.this,0,intentNext,PendingIntent.FLAG_UPDATE_CURRENT);

            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.logo)
                    .addAction(R.drawable.previous,"Prev",prev)
                    .addAction(R.drawable.play,"Play",play)
                    .addAction(R.drawable.next,"Next",next)
                    .setSubText("Change Song")
                    .setAutoCancel(false)
                    .build();


            registerReceiver(broadcastReceiver,new IntentFilter("TRACKS_TRACKS"));
            startService(new Intent(MainActivity.this, ClearFromService.class));
        }

        manager.notify(1, notification);


        seekBar.setMax(mp.getDuration());
        updateSongStatus.start();

        // Add Events to Seekbar for PLaying song from Any Position
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mp.seekTo(seekBar.getProgress());
            }
        });

        // Event to Play and Pause
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mp.isPlaying()){
                    play.setText("Play");
                    mp.pause();
                    imageView.clearAnimation();
                }
                else {
                    play.setText("Pause");
                    mp.start();
                    animate();
                }
            }
        });

        // Event to go to Prev song
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp.stop();
                mp.release();

                counter = ((counter-1)<0)? songlist.size()-1 : counter-1;
                Uri uri = Uri.parse(songlist.get(counter).getPath().toString());
                mp = MediaPlayer.create(getApplicationContext(),uri);

                if(songlist.get(counter).getTitle().length()>20) {
                    songname.setText(songlist.get(counter).getTitle().substring(0, 20) + "...");
                }
                else{
                    songname.setText(songlist.get(counter).getTitle());
                }
                duration.setText(getStringTime(songlist.get(counter)));

                play.setText("Pause");
                seekBar.setProgress(0);
                seekBar.setMax(mp.getDuration());

                updateSongStatus.interrupt();
                updateSongStatus = new UpdateSongStatus(mp.getDuration());
                updateSongStatus.start();

                SlideAnimation(true);
                mp.start();


            }
        });

        // Event to get to Next Song
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp.stop();
                mp.release();

                counter = ((counter+1)%songlist.size());
                Uri uri = Uri.parse(songlist.get(counter).getPath().toString());
                mp = MediaPlayer.create(getApplicationContext(),uri);

                if(songlist.get(counter).getTitle().length()>20) {
                    songname.setText(songlist.get(counter).getTitle().substring(0, 20) + "...");
                }
                else{
                    songname.setText(songlist.get(counter).getTitle());
                }

                duration.setText(getStringTime(songlist.get(counter)));

                play.setText("Pause");
                seekBar.setMax(mp.getDuration());
                seekBar.setProgress(0);

                updateSongStatus.interrupt();
                updateSongStatus = new UpdateSongStatus(mp.getDuration());
                updateSongStatus.start();

                SlideAnimation(false);
                mp.start();

            }
        });

        // Setting Click Event to move on SongList Intent/Activity
        _that.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SongQueue.class);

                intent.putExtra("mess",counter);

                startActivity(intent);
            }
        });

    }

    // Infinitely Animate Rotate.xml by AnimationUtils and Setting to Disc
    public void animate(){
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
        imageView.setAnimation(animation);
    }

    // Animate on Prev/Next or Left/Right as per requirement
    public void SlideAnimation(boolean isLeft){
        int moveX = 100;
        if(isLeft) {
            moveX = -100;
        }
        // Moving Disc from 100 to 0 or -100 to 0
            TranslateAnimation move = new TranslateAnimation(moveX, 0, 0, 0);
            move.setInterpolator(new AccelerateInterpolator());
            move.setDuration(500);
            move.setFillEnabled(true);
            move.setFillAfter(true);

        // Set Disc Rotating Animation on Complete of this Animation Itself
            move.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animate();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            // Setting This animation to ImageView/Disc
            imageView.setAnimation(move);
    }
    // Change Songs on Swipe Left & Right by OnTouchEvent
    float x1=0,x2=0,y1=0,y2=0;
    public boolean onTouchEvent(MotionEvent touchEvent){

        switch (touchEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = touchEvent.getX();
                y1 = touchEvent.getY();
                return true;

            case MotionEvent.ACTION_UP:
                x2 = touchEvent.getX();
                y2 = touchEvent.getY();
                //checking If Swipe Left or Right on Whole MainActivity
                if (x1 < x2 ) {
                    prev.performClick();
                }
                if (x1 > x2 ){
                    next.performClick();
                }
            default:
                return false;
        }
    }
    // Convert Time to a Presentable form like 01:05 in String
    String getStringTime(AudioModel song){
        String time = "";
        int min = Integer.parseInt(song.getDuration())/1000/60;
        int sec = Integer.parseInt(song.getDuration())/1000%60;

        String add1 = "";
        String add2 = "";
        if( sec<10 ){
            add2 += "0";
        }
        if( min<10){
            add1 += "0";
        }
        time += add1 + min + ":" + add2 + sec;

        return time;
    }

    // Get Permission For Reading External Files
    boolean getPermission(){
        int result = ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else{
            return false;
        }
    }

    // Ask For Permission to User
    void setPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            Toast.makeText(MainActivity.this,"Please Give Read File Permission",Toast.LENGTH_SHORT).show();
        }else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        NotificationManager m =(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        m.cancelAll();
        unregisterReceiver(broadcastReceiver);
    }
}