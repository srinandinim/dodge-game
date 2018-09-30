package com.example.srina.dodgegame;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    //Code from this program has been used from "Beginning Android Games" by Mario Zechner

    GameSurface gameSurface;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSurface = new GameSurface(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(gameSurface);
    }

    protected void onPause() {
        super.onPause();
        gameSurface.pause();
    }

    protected void onResume() {
        super.onResume();
        gameSurface.resume();
    }

    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener {

        Thread gameThread;
        SurfaceHolder holder;
        volatile boolean running = false;
        Bitmap bottomImage;
        Bitmap fallingImage;
        Bitmap crushedImage;
        Paint paintProperty;

        int bottomX = 5;
        int bottomY = 525;
        int bottomdisX = 100;
        int bottomdisY = 150;

        int fallX = 10;
        int fallY = 15;
        int falldisX = 80;
        int falldisY = 80;

        int score;
        long startTime;
        boolean point;

        SensorManager sensorManager;
        Sensor sensor;

        SoundPool soundPool;
        int sound;

        public GameSurface(Context context) {
            super(context);

            score = 0;

            bottomImage = BitmapFactory.decodeResource(getResources(), R.drawable.amazonbox);
            bottomImage = Bitmap.createScaledBitmap(bottomImage, bottomdisX, bottomdisY, true);

            fallingImage = BitmapFactory.decodeResource(getResources(), R.drawable.soccerball);
            fallingImage = Bitmap.createScaledBitmap(fallingImage, falldisX, falldisY, true);

            crushedImage = BitmapFactory.decodeResource(getResources(), R.drawable.crushy);
            crushedImage = Bitmap.createScaledBitmap(crushedImage, 100, 100, true);

            startTime = System.currentTimeMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                soundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(audioAttributes).build();
            } else {
                soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
            }

            sound = soundPool.load(context,R.raw.smash, 1);

            holder = getHolder();

            paintProperty = new Paint();
            paintProperty.setTextSize(30);

            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

        }

        public void run() {
            while (running == true) {

                if (holder.getSurface().isValid() == false)
                    continue;

                Canvas canvas = holder.lockCanvas();
                canvas.drawRGB(153, 153, 255);

                canvas.drawBitmap(bottomImage, bottomX, bottomY, null);
                canvas.drawBitmap(fallingImage, fallX, fallY, null);

                Rect bottomRect = new Rect(bottomX, bottomY, bottomX + 100, bottomY + 150);
                Rect fallingRect = new Rect(fallX * 1, fallY * 1, fallX + 100, fallY + 100);

                canvas.drawText("Score: " + score, 15, 30, paintProperty);

                long timeNow = System.currentTimeMillis();
                long timeToGo = 30 - (timeNow - startTime) / 1000;


                if (timeToGo >= 0) {
                    Log.d("msg", bottomY + bottomdisY + 10 + "");
                    canvas.drawText("Timer: " + Long.toString(timeToGo), 325, 30, paintProperty);
                    if (fallingRect.intersect(bottomRect)) {
                        point = true;
                        fallY = 520;
                        fallX = (int) (Math.random() * 350);
                        soundPool.play(sound,1, 1, 0, 0, 1);
                        bottomImage = BitmapFactory.decodeResource(getResources(), R.drawable.crushy);
                        bottomImage = Bitmap.createScaledBitmap(crushedImage, 100, 100, true);
                        fallingImage.eraseColor(0);
                        sensorManager.unregisterListener(this, sensor);
                    }
                    if (fallY < (bottomY + bottomdisY + 10)) {
                        fallY += 8;
                    } else {
                        score++;
                        if (point == true){
                            score--;
                        }
                        fallY = 0;
                        fallX = (int) (Math.random() * 250);
                        bottomImage = BitmapFactory.decodeResource(getResources(),R.drawable.amazonbox);
                        bottomImage = Bitmap.createScaledBitmap(bottomImage, 100, 150, true);
                        fallingImage = BitmapFactory.decodeResource(getResources(), R.drawable.soccerball);
                        fallingImage = Bitmap.createScaledBitmap(fallingImage, falldisX, falldisY, true);
                        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
                        point = false;
                    }
                }
                if (timeToGo < 0) {
                    running = false;
                    canvas.drawText("Timer: 0", 325, 30, paintProperty);
                    canvas.drawText("Game Over!", 160, 310, paintProperty);
                }

                holder.unlockCanvasAndPost(canvas);
            }
        }

        public void resume() {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        public void pause() {
            running = false;
            while (true) {
                try {
                    gameThread.join();
                } catch (InterruptedException e) {
                }
            }
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            int sensorValue = (int) sensorEvent.values[0];
            if (bottomX < 375) {
                if (sensorValue < -3) {
                    bottomX += 6;
                } else if (sensorValue < 0) {
                    bottomX += 3;
                }
            }
            if (bottomX > 5) {
                if (sensorValue > 3) {
                    bottomX -= 6;
                } else if (sensorValue > 0) {
                    bottomX -= 3;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }//GameSurface
}//Activity


