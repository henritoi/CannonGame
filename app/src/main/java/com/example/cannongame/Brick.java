package com.example.cannongame;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.util.Random;

public class Brick {
    private static final String TAG = "Brick";

    protected CannonView view;
    protected Rect shape;
    protected Paint paint = new Paint();
    protected int x;
    protected int y;

    protected int hitsRequired;
    protected int hits = 0;

    protected int destructionReward = 0;

    public Brick(CannonView view, int x, int y, int width, int height, int minHits, int maxHits, int destructionReward) {
        this.view = view;
        this.x = x;
        this.y = y;
        this.destructionReward = destructionReward;
        this.shape = new Rect(x, y, width, height);
        Random random = new Random();
        this.hitsRequired = random.nextInt((maxHits - minHits) + 1) + minHits;
        this.paint.setColor(Color.argb(255,
                255 - (255 / (hitsRequired - hits + 1)),
                0,
                0));
    }

    /*
     * Palauttaa onko tiileen osuttu riittävän monta kertaa
     */
    public boolean hit() {
        this.hits++;
        this.paint.setColor(Color.argb(255,
                255 - (255 / (hitsRequired - hits + 1)),
                0,
                0));
        return this.hits >= this.hitsRequired;
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(this.shape, this.paint);
    }

    public int getDestructionReward() {
        return this.destructionReward;
    }
}
