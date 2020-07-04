package com.example.blocksexplode;

import android.graphics.Canvas;
import android.graphics.Rect;

public class Cannonball extends GameElement{

    private float velocityX;
    private boolean onScreen;


    public Cannonball(CannonView view, int color, int soundId, int x, int y, int radius, float velocityX, float velocityY )
    {
        super(view, color, soundId, x, y, 2 * radius, 2*radius , velocityY);
        this.velocityX = velocityX;
        onScreen = true;
    }

    //obtem o raio da bala
    private int getRadius() { return (shape.right - shape.left) / 2; }

    //retorna true se essa bala esta na tela
    public boolean isOnScreen() { return onScreen; }

    //teste se a bala colide com o GameElement dado
    public boolean collidesWith(GameElement element)
    {
        return (Rect.intersects(shape, element.shape) && velocityX > 0);
    }

    //inverte a velocidade horizontal da bala
    public void reverseVelocityX()
    {
        velocityX *= -1;
    }

    @Override
    public void update(double interval)
    {
        super.update(interval); //atualiza a posicao vertical

        //atualiza a posicao horizontal
        shape.offset((int) (velocityX * interval), 0);

        //se a bala sai da tela
        if (shape.top < 0 || shape.left < 0 || shape.bottom > view.getScreenHeight() || shape.right > view.getScreenWidth()) onScreen = false; //a bala é removida
    }

    //desenha a bala de canhao na tela de desenho dada
    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(shape.left + getRadius(), shape.top + getRadius(), getRadius(), paint);
    }
}
