package com.example.cannongame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CannonView";

    // pelin vakiot
    public static final int GAME_TIME = 30;
    //public static final int MISS_PENALTY = 0;
    public static final int BRICK_DESTRUCTION_REWARD = 3;
    public static final int HIT_REWARD = 3;

    // Kanuunan vakioita
    public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

    // Kuulan vakioita
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;

    // Kohteiden vakioita
    public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
    public static final double TARGET_PIECES = 9;
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

    // Vakioita Blocker:lle
    //public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    //public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    //public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    //public static final double BLOCKER_SPEED_PERCENT = 1.0;

    // Tiiliseinä
    public static final int BRICKWALL_LAYER_COUNT = 2;
    public static final int BRICKS_PER_COLUMN = 10;
    public static final int BRICK_SPACER = 5;
    public static final int BRICK_COLUMN_SPACER = 5;
    public static final int BRICK_WIDTH = 30;
    public static final int BRICK_MIN_HITS = 3;
    public static final int BRICK_MAX_HITS = 6;
    public static final double BRICKWALL_X_PERCENT = 1.0 / 2;

    // tekstin koko 1/18 ruudun leveydestä
    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

    private CannonThread cannonThread;
    private Activity activity;
    private boolean dialogIsDisplayed = false;

    // pelin oliot
    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;
    private ArrayList<Brick> bricks;

    // mittasuhteet
    private int screenWidth;
    private int screenHeight;

    // muuttujat pelin toistoon ja tilastointiin
    private boolean gameOver; // loppuuko peli?
    private double timeLeft; // jäljellä oleva peliaika
    private int shotsFired; // montako laukausta ammuttu
    private double totalElapsettime; // kulunut aika sekunneisa

    // vakiot ja muuttujat äänille
    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool;
    private SparseIntArray soundMap;

    // piirtomuuttujat
    private Paint textPaint;
    private Paint backgroundPaint;

    // lisätty staattisia muuttujia tiedon siirtämiseksi staattiseen sisäluokkaan
    private static int apuMessageId;
    private static int apuShotsFired;
    private static double apuTotalElapsettime;
    private static boolean apuDialogIsDisplayed;
    private static CannonView apuCannonView;

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (Activity) context;

        getHolder().addCallback(this);

        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();

        soundMap = new SparseIntArray(3);

        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint(); // ajan näyttäminen
        backgroundPaint = new Paint(); // taustaväri
        backgroundPaint.setColor(Color.WHITE);
    }

    public void releaseResources() {
        soundPool.release();
        soundPool = null;
    }

    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false); // pyydetään säiettä lopettamaan
    }

    public void playSound(int soundId) {
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    public int getScreenHeight() {
        return this.screenHeight;
    }

    public int getScreenWidth() {
        return this.screenWidth;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            newGame(); // käynnistetään uusi peli
            cannonThread = new CannonThread(holder); // luodaan säie
            cannonThread.setRunning(true); // käynnistetään peli
            cannonThread.start(); // käynnistetään pelisäie
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        cannonThread.setRunning(false); // päätetään säie
        while(true) {
            try {
                cannonThread.join(); // odotetaan että säie päättyy
                retry = false;
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w;
        screenHeight = h;

        textPaint.setTextSize( (int) (TEXT_SIZE_PERCENT * screenHeight));
        textPaint.setAntiAlias(true); // siloittaa tekstiä
    }

    public void newGame() {
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        Random random = new Random();
        targets = new ArrayList<>();
        bricks = new ArrayList<>();

        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);

        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) *
                screenHeight);

        for (int n = 0; n < TARGET_PIECES; n++) {

            double velocity = screenHeight * (random.nextDouble() *
                    (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) +
                    TARGET_MIN_SPEED_PERCENT);

            int color = (n % 2 == 0) ?
                    getResources().getColor(R.color.dark,
                            getContext().getTheme()) :
                    getResources().getColor(R.color.light,
                            getContext().getTheme());

            velocity *= -1;

            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight),
                    (int) velocity));

            targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) * screenWidth;
        }

        int startX = (int) ((BRICKWALL_X_PERCENT * screenWidth) - (BRICKWALL_LAYER_COUNT * BRICK_WIDTH + (BRICKWALL_LAYER_COUNT - 1 * BRICK_SPACER)) / 2);
        int brickHeight = (int) ((screenHeight - ((BRICKS_PER_COLUMN - 1) * BRICK_SPACER)) / BRICKS_PER_COLUMN);
        for (int i = 0; i < BRICKWALL_LAYER_COUNT; i++) {
            if (i % 2 == 1) {
                for(int j = 0; j < BRICKS_PER_COLUMN; j++) {
                    bricks.add(new Brick(
                            this,
                            (int) (startX + (i * (BRICK_WIDTH + BRICK_COLUMN_SPACER))),
                            (int) (j * brickHeight + (j * BRICK_SPACER)),
                            (int) (startX + (i * (BRICK_WIDTH + BRICK_COLUMN_SPACER)) + BRICK_WIDTH),
                            (int) (j * brickHeight + (j * BRICK_SPACER) + brickHeight),
                            BRICK_MIN_HITS,
                            BRICK_MAX_HITS,
                            BRICK_DESTRUCTION_REWARD
                    ));
                }
            }else {
                for(int j = 0; j < BRICKS_PER_COLUMN + 2; j++) {
                    bricks.add(new Brick(
                            this,
                            (int) (startX + (i * (BRICK_WIDTH + BRICK_COLUMN_SPACER))),
                            (int) (j * brickHeight + (j * BRICK_SPACER)  - (brickHeight / 2)),
                            (int) (startX + (i * (BRICK_WIDTH + BRICK_COLUMN_SPACER)) + BRICK_WIDTH),
                            (int) (j * brickHeight + (j * BRICK_SPACER) + brickHeight - (brickHeight / 2)),
                            BRICK_MIN_HITS,
                            BRICK_MAX_HITS,
                            BRICK_DESTRUCTION_REWARD
                    ));
                }
            }

        }

        timeLeft = GAME_TIME;

        shotsFired = 0;
        totalElapsettime = 0.0;

        if (gameOver) {
            gameOver = false;
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        }

        hideSystemBars();
    }

    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0;

        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);

        //blocker.update(interval);

        for (GameElement target : targets)
            target.update(interval);

        //timeLeft -= interval;

        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true;
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.lose);
        }

        if (targets.isEmpty()) {
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.win);
            gameOver = true;
        }
    }

    public void alignAndFireCannonball(MotionEvent event) {
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());

        double centerMinusY = (screenHeight / 2 - touchPoint.y);

        double angle = 0;

        angle = Math.atan2(touchPoint.x, centerMinusY);

        cannon.align(angle);

        if (cannon.getCannonball() == null || !cannon.getCannonball().isOnScreen()) {
            cannon.fireCannonball();
            ++shotsFired;
        }
    }

    public void drawGameElements(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        canvas.drawText(getResources().getString(
                R.string.time_remaining_format, timeLeft), 50, 100, textPaint);

        cannon.draw(canvas);

        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);

        for (GameElement target : targets)
            target.draw(canvas);

        for (Brick brick : bricks) {
            brick.draw(canvas);
        }
    }

    private void showGameOverDialog(final int messageId) {
        apuMessageId = messageId;
        apuShotsFired = shotsFired;
        apuTotalElapsettime = totalElapsettime;
        apuDialogIsDisplayed = dialogIsDisplayed;
        apuCannonView = this;

        final DialogFragment gameResult = new MyAlertDialogFragment();

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showSystemBars();
                        dialogIsDisplayed = true;
                        gameResult.setCancelable(false);
                        gameResult.show(activity.getFragmentManager(), "results");
                    }
                }
        );
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getResources().getString(apuMessageId)); // ei löydä!

            builder.setMessage(getResources().getString(
                    R.string.result_format, apuShotsFired, apuTotalElapsettime
            ));
            builder.setPositiveButton(R.string.reset_game,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            apuDialogIsDisplayed = false;
                            apuCannonView.newGame();
                        }
                    });
            return builder.create();
        }
    }

    public void testForCollisions() {
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++ ) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    targets.get(n).playSound();

                    timeLeft += targets.get(n).getHitReward();

                    cannon.removeCannonball();
                    targets.remove(n);
                    --n;
                    break;
                }
            }
        }
        else {
            cannon.removeCannonball();
        }

        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen()) {
            for(int i = 0; i < bricks.size(); i++) {
                if(cannon.getCannonball().collidesWith(bricks.get(i))) {
                    // TODO: brick.playSound();
                    cannon.removeCannonball();
                    if (bricks.get(i).hit()) {
                        timeLeft += bricks.get(i).getDestructionReward();
                        bricks.remove(i);
                        i--;
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction();

        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE) {
            alignAndFireCannonball(e);
        }
        return true;
    }

    private class CannonThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private boolean threadIsRunning = true;

        public CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("CannonThread");
        }

        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    canvas = surfaceHolder.lockCanvas(null);

                    synchronized (surfaceHolder) {
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsettime += elapsedTimeMS / 1000.0;
                        updatePositions(elapsedTimeMS);
                        testForCollisions();
                        drawGameElements(canvas);
                        previousFrameTime = currentTime;
                    }
                }
                finally {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE
            );
    }

    /*
    Metodi näyttää järjestelmäpalkit
    */
    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
    }
}
