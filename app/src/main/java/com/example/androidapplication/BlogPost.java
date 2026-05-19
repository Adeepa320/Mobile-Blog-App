package com.example.androidapplication;

import java.io.Serializable;

public class BlogPost implements Serializable {
    public int id;
    public String title;
    public String body;
    public String imageUri;
    public String date;

    public BlogPost(int id, String title, String body, String imageUri, String date) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.imageUri = imageUri;
        this.date = date;
    }
}
