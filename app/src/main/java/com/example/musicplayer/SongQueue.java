package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class SongQueue extends AppCompatActivity {

    ImageView right;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_queue);

        right = findViewById(R.id.right);

        // Making Arraylist and get ListView
        ListView myList = findViewById(R.id.mylist);
        ArrayList<String> things = new ArrayList<>();

        // Checking Song Process same as MainActivity
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,projection,selection,null,null);

        ArrayList<AudioModel> songlist = new ArrayList<>();

        while(cursor.moveToNext()){
            AudioModel song = new AudioModel(cursor.getString(0),cursor.getString(1),cursor.getString((2)));
            if(new File(song.getPath()).exists()){
                songlist.add(song);
            }
        }
        for(AudioModel song: songlist){
            things.add(song.getTitle());
        }
        //ADD BLANK LIST TO ALSO ABLE TO SEE THE LAST SONG OF LIST
        things.add(" ");

        // Getting value to Color It as Blue in BackGround
        Intent data = getIntent();
        System.out.println(data.getIntExtra("mess",3));
        int counter = data.getIntExtra("mess",3);

        // ArrayList Adapter to add Data to ListView
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,things){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                view.setMinimumWidth(2500);
                if(position==counter){
                    view.setBackgroundColor(getResources().getColor(R.color.teal_700));
                }
                else{
                    view.setBackgroundColor(getResources().getColor(R.color.white));
                }
                return view;
            }
        };
        myList.setAdapter(arrayAdapter);

        // Finishing the Activity and Go back to MainActivity Again
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}