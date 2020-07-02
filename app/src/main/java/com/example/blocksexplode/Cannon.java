package com.example.blocksexplode;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
    private int baseRadius;
    private int barrelLenght;
    private double barrelAngle;
    private Cannonball cannonball;
    private Point barrelEnd = new Point();
    private Paint paint = new Paint();
    private CannonView view;

    public Cannon(CannonView view, int baseRadius, int barrelLenght, int barrelWidth){
        this.view = view;
        this.baseRadius = baseRadius;
        this.barrelLenght = barrelLenght;
        paint.setStrokeWidth(barrelWidth);
        paint.setColor(Color.BLACK);
        align(Math.PI / 2);
    }

    private void align(double barrelAngle) {
        this.barrelAngle = barrelAngle;
        barrelEnd.x = (int) (barrelLenght * Math.sin(barrelAngle));
        barrelEnd.y = (int) (-barrelLenght * Math.cos(barrelAngle)) + view.getScreenWidth() / 2;
    }

    public void fireCannonball(){
        int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT * view.getScreenWidth() / Math.sin(barrelAngle));
        int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT * view.getScreenWidth() / -Math.cos(barrelAngle));

        int radius = (int) (view.getScreenWidth() * CannonView.CANNONBALL_RADIUS_PERCENT);

        cannonball = new Cannonball(view, Color.BLACK, CannonView.CANNON_SOUND_ID, -radius, view.getScreenWidth() / 2 - radius, radius, velocityX, velocityY);
        cannonball.playSound();
    }

    public void draw(Canvas canvas){
        canvas.drawLine(0, view.getScreenHeight() / 2, barrelEnd.x, barrelEnd.y, paint);
        canvas.drawCircle(0, (int) view.getScreenHeight() / 2, (int) baseRadius, paint);
    }

    public Cannonball getCannonball() {
        return cannonball;
    }

    public void removeCannonball() {
        cannonball = null;
    }
}
