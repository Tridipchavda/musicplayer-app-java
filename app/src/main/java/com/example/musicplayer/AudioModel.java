package com.example.musicplayer;

public class AudioModel {
    String path;
    String title;
    String duration;

    public AudioModel( String title,String path,String duration){
        this.path = path;
        this.title = title;
        this.duration = duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }

    public String getPath() {
        return path;
    }
}
