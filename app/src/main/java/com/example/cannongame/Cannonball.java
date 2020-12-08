package com.example.cannongame;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;

public class Cannonball extends GameElement {
    private static final String TAG = "Cannonball";
    private float velocityX;
    private boolean onScreen;

    public Cannonball(CannonView view, int color, int soundId, int x,
                      int y, int radius, float velocityX, float velocityY) {
        super(view, color, soundId, x, y, 2 * radius, 2 * radius, velocityY);
        this.velocityX = velocityX;
        this.onScreen = true;
    }

    private int getRadius() {
        return (shape.right - shape.left) / 2;
    }

    public boolean collidesWith(GameElement element) {
        return (Rect.intersects(shape, element.shape) && this.velocityX > 0);
    }

    public boolean collidesWith(Brick brick) {
        return (Rect.intersects(shape, brick.shape) && this.velocityX > 0);
    }

    public boolean isOnScreen() {
        return this.onScreen;
    }

    public void reverseVelocityX() {
        this.velocityX *= -1;
    }

    @Override
    public void update(double interval) {
        super.update(interval);

        shape.offset((int) (this.velocityX * interval), 0);

        if (shape.top < 0 || shape.left < 0 ||
                shape.bottom > view.getScreenHeight() ||
                shape.right > view.getScreenWidth())
            this.onScreen = false;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(shape.left + getRadius(),
                shape.top + getRadius(), getRadius(), paint);
    }
}