package com.example.blocksexplode;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {

    protected CannonView view; //a view que contém esse gameElement
    protected Paint paint = new Paint(); //desenhar esse GameElement
    protected Rect shape; //os limites retangulares desse GameElement
    private float velocityY; //a velocidade vertical desse GameElement
    private int soundId; //o som associado com esse GameElement

    public GameElement(CannonView view, int color, int soundId, int x, int y, int width, int length, float velocityY)
    {
        this.view = view;
        paint.setColor(color);

        //define os limites
        shape = new Rect(x, y, x + width, y + length);
        this.soundId= soundId;
        this.velocityY = velocityY;
    }

    //atualizacao a posicao do GameElement e verifica colisoes de borda
    public void update(double interval)
    {
        //atualiza posicao vertical
        shape.offset(0, (int) (velocityY * interval));

        //se este GameElement colide com a borda, inverte a direcao
        if(shape.top < 0 && velocityY < 00 || shape.bottom > view.getScreenHeight() && velocityY > 0)
            velocityY *= -1; //inverte a velocidade desse GameElement
    }

    //desenha este gameElement no objeto Canvas dado
    public void draw(Canvas canvas){canvas.drawRect(shape, paint);}

    //reproduz o som correspondente a esse tipo de GameElement
    public void playSound() { view.playSound(soundId); }


}
