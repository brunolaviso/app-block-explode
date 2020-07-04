package com.example.blocksexplode;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CannonView";

    //constantes para gameplay
    public static final int MISS_PENALTY = 2; //segundos subtraídos em caso de erro
    public static final int HIT_REWARD = 3; //segundos adicionados em caso de acerto

    //constantes para o canhao
    public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

    // constantes para a bala de canhão
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;

    // constantes para os alvos
    public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
    public static final double TARGET_PIECES = 9;
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

    // constantes para a barreira
    public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT = 1.0;

    // o tamanho do texto é 1/18 da largura da tela
    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

    private CannonThread cannonThread; // controla o loop do jogo
    private Activity activity; // para exibir a caixa de diálogo GameOver
    private boolean dialogIsDisplayed = false;

    // objetos do jogo
    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;

    // variáveis de dimensão
    private int screenWidth;
    private int screenHeight;

    // variáveis para o loop do jogo e
// controle de estatísticas
    private boolean gameOver; // é o fim do jogo?
    private double timeLeft; // tempo restante em segundos
    private int shotsFired; // tiros disparados pelo jogador
    private double totalElapsedTime; // segundos decorridos

    // constantes e variáveis para gerenciar sons
    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // reproduz os efeitos de sonoros
    private SparseIntArray soundMap; // mapeia os identificadores de sons

    // variáveis Paint utilizadas ao desenhar cada item na tela
    private Paint textPaint; // objeto Paint usado para desenhar texto
    private Paint backgroundPaint; // objeto Paint para limpar a área do desenho

    // construtor
    public CannonView(Context context, AttributeSet attrs)
    {
        super(context, attrs); // chama a superclasse do construtor
        activity = (Activity) context; // armazena uma referência para MainActivity

        // registra o receptor de SurfaceHolder.Callback listener
        getHolder().addCallback(this);

        // configura os atributos de audio para o jogo
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

        // inicializa SoundPool para reproduzir os efeitos sonoros
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();

        // cria lista Map de sons e carrega-os previamente
        soundMap = new SparseIntArray(3); // cria um SparseIntArray
        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint();
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
    }

    // chamado quando o tamanho de SurfaceView muda,
// assim como quando a View é adicionada na hierarquia
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w; // armazena a largura de CannonView
        screenHeight = h; // armazena a altura de CannonView's
        // configuração das propriedades de texto
        textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
        textPaint.setAntiAlias(true); // suaviza o texto
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    // reproduz um som com o soundId em soundMap
    public void playSound(int soundId) {
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    // reseta todos os elementos da tela e cria um novo jogo
    public void newGame() {
        // construir um novo jogo
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        Random random = new Random(); // para determinar velocidades aleatórias
        targets = new ArrayList<>(); // constrói uma nova lista de alvos

        // inicializa targetX para o primeiro alvo da esquerda
        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);
        // calcula a coordenada Y dos alvos
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) *
                screenHeight);

        // adiciona TARGET_PIECES alvos a lista de alvos
        for (int n = 0; n < TARGET_PIECES; n++)
        {
            // determina a velocidade aleatória entre os valores
            // min e max para o alvo n
            double velocity = screenHeight * (random.nextDouble() *
                    (TARGET_MAX_SPEED_PERCENT -
                            TARGET_MIN_SPEED_PERCENT) +
                    TARGET_MIN_SPEED_PERCENT);
            // alterna as cores dos alvos entre escura e clara
            int color = 0;
            if (n % 2 == 0) {
                color = getResources().getColor(R.color.dark,
                        getContext().getTheme());
            } else {
                color = getResources().getColor(R.color.light,
                        getContext().getTheme());
            }
            velocity *= -1; // inverte a velocidade inicial para o próximo alvo

            // cria e adiciona um novo alvo na lista de alvos
            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight),
                    (int) velocity));

            // aumenta a coordenada X para posicionar o próximo alvo mais
            // a direita
            targetX += (TARGET_WIDTH_PERCENT +
                    TARGET_SPACING_PERCENT) * screenWidth;
        }

        // cria uma nova barreira
        blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight));

        timeLeft = 90; // inicia a contagem regressiva de 10 segundos
        shotsFired = 0; // valor inicial de tiros disparados
        totalElapsedTime = 0.0; // tempo decorrido inicia em 0

        if (gameOver) { // inicia um novo jogo depois que último terminou
            gameOver = false; // o jogo não terminou
            cannonThread = new CannonThread(getHolder()); // cria thread
            cannonThread.start(); // inicia a thread de loop do jogo
        }

        hideSystemBars();
    }

    // chamado repetidamente por CannonThread para atualizar os elementos do game
    private void updatePositions(double elapsedTimeMS)
    {
        double interval = elapsedTimeMS / 1000.0; // converte e, segundos
        // atualiza a posição da bala, se estiver na tela
        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);
        blocker.update(interval); // atualiza a posição da barreira
        for (GameElement target : targets)
            target.update(interval); // atualiza a posição de cada alvo da lista
        timeLeft -= interval; // subtrai do tempo restante
        // se o cronômetro foi zerado
        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true; // o jogo acabou
            cannonThread.setRunning(false); // encerra a Thread
            showGameOverDialog(R.string.perdedor); // mostra a caixa de diálogo de perda
        }
        // se todos os alvos foram atingidos
        if (targets.isEmpty()) {
            cannonThread.setRunning(false); // encerra a Thread
            showGameOverDialog(R.string.ganhador); // mostra a cx de diálogo do vencedor
            gameOver = true;
        }
    }

    // alinha o cano e dispara uma bala caso ainda não
// haja uma na tela
    public void alignAndFireCannonball(MotionEvent event)
    {
        // obtém o local do toque nessa view
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());
        // calcula a distância do toque a partir do centro da tela
        // no eixo y
        double centerMinusY = (screenHeight / 2 - touchPoint.y);
        double angle = 0; // inicializa o ângulo com 0
        // calcula o ângulo do cano em relação à horizontal
        angle = Math.atan2(touchPoint.x, centerMinusY);
        // aponta o cano para onde a tela foi tocada
        cannon.align(angle);
        // dispara a bala se ainda não houver uma em tela
        if (cannon.getCannonball() == null || !cannon.getCannonball().isOnScreen())
        {
            cannon.fireCannonball(); //dispara
            ++shotsFired; //incrementa o número de tiros
        }
    }

    // mostra um componente AlertDialog quando o jogo Termina
    private void showGameOverDialog(final int messageId)
    {
        /*

        // DialogFragment para exibir estatísticas do jogo e comçar outro
        final DialogFragment gameResult = new DialogFragment()
        {
            //método depreciado
            // cria um AlertDialog e o retorna
            @Override
            public Dialog onCreateDialog(Bundle bundle)
            {
                // cria a caixa de diálogo exibindo a String para messageId
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(messageId));

                // mostra o total de tiros disparados e o tempo decorrido
                builder.setMessage(getResources().getString(R.string.resultados_formatado, shotsFired, totalElapsedTime));
                builder.setPositiveButton(R.string.reset_game,
                        new DialogInterface.OnClickListener()
                        {
                            // chamado quando o botão "Reset Game" é pressionado
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialogIsDisplayed = false;
                                newGame(); // prepara e inicia um novo jogo
                            }
                        });
                return builder.create(); // return the AlertDialog
            }
        };

        // na Thread da interface Gráfica do usuário, usa FragmentManager
        // para mostrar o DialogFragment
        activity.runOnUiThread(
                new Runnable()
                {
                    public void run()
                    {
                        showSystemBars(); //ativa as barras do sistema
                        dialogIsDisplayed = true;
                        gameResult.setCancelable(false); // caixa de diálogo modal
                        gameResult.show(activity.getFragmentManager(), "results");
                    }
                }
        );
         */
    }

    // desenha o jogo no objeto Canvas
    public void drawGameElements(Canvas canvas)
    {
        // limpa o background
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
        // mostra o tempo restante
        canvas.drawText(getResources().getString(R.string.tempo_restante_formatado, timeLeft), 50, 100, textPaint);
        cannon.draw(canvas); // desenha o canhão
        // desenha a bala
        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);
        blocker.draw(canvas); // desenha a barreira
        // desenha todos os alvos
        for (GameElement target : targets)
            target.draw(canvas);
    }

    // verifica se abala colide com a barreira ou qualquer
    // um dos alvos e trata essas colisões
    public void testForCollisions()
    {
        // remove quaisquer alvos em que a bala colida
        //testa se a bala está na tela
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) { //colidiu
                    targets.get(n).playSound(); // reproduz o som de golpe no alvo
                    // adiciona a recompensa de tempo pelo acerto
                    timeLeft += targets.get(n).getHitReward();
                    cannon.removeCannonball(); // remove a bala do jogo
                    targets.remove(n); // remove o alvo que foi acertado
                    --n; // certifica-se de que não pulamos o teste do novo alvo n
                    break;
                }
            }
        }
        else { // remove a bala, caso não deva estar na tela
            cannon.removeCannonball();
        }
        // verifica se a bala colide com a barreira
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound(); //toca o som de golpe na barreira
            // inverte a direçao da bala
            cannon.getCannonball().reverseVelocityX();
            // subtrai do tempo restante a penalidade por erro
            timeLeft -= blocker.getMissPenalty();
        }
    }

    // interrompe o jogo chamado pelo método
// onPause de MainActivityFragment
    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false); // diz a thread para terminar
    }

    // libera recursos: chamado pelo método onDestroy do app
    public void releaseResources() {
        soundPool.release(); // libera todos os recursos usados pelo SoundPool
        soundPool = null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (!dialogIsDisplayed) {
            newGame(); //prepara e inicia um novo jogo
            cannonThread = new CannonThread(holder); // cria a Thread
            cannonThread.setRunning(true); // inicia o jogo de fato
            cannonThread.start(); // inicia o loop do jogo
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // garante que essa thread seja encerrada corretamente
        boolean retry = true;
        cannonThread.setRunning(false); //finaliza a thread
        while (retry)
        {
            try
            {
                cannonThread.join(); // espera a thread terminar
                retry = false;
            }
            catch (InterruptedException e)
            {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    }

    // chamado quando o jogador toca na tela nessa atividade
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        // obtém o valor int representando o tipo de ação
        // que causou esse evento
        int action = e.getAction();
        // o jogador tocou ou arrastou os dedos na tela
        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE) {
        // dispara a bala na direção do ponto do toque
            alignAndFireCannonball(e);
        }
        return true;
    }

    // Subclasse de Thread para controlar o loop do jogo
    private class CannonThread extends Thread {
        private SurfaceHolder surfaceHolder; // para manipular Canvas
        private boolean threadIsRunning = true; // executando por padrão

        // inicializa o SurfaceHolder
        public CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("CannonThread");
        }

        // muda o estado de execução
        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        // controla o loop do jogo
        @Override
        public void run() {
            Canvas canvas = null; // usado para desenhar
            long previousFrameTime = System.currentTimeMillis();
            while (threadIsRunning) {
                try {
                    // obtém objeto Canvas para desenho exclusivo a partir dessa thread
                    canvas = surfaceHolder.lockCanvas(null);
                    // bloqueia o SurfaceHolder para desenho
                    synchronized(surfaceHolder) {
                        long currentTime = System.currentTimeMillis(); // tempo atual
                        // calculo do tempo decorrido, com base no quadro anterior
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(elapsedTimeMS); // atualiza o estado do jogo
                        testForCollisions(); // testa colisões contra GameElement
                        drawGameElements(canvas); // desenha usando o canvas
                        previousFrameTime = currentTime; // atualiza o tempo anterior
                    }
                }
                finally {
                    // exibe o conteúdo da tela de desenho em CannonView
                    // e permite que outras Threads utilizem o objeto Canvas
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    // esconde as barras do sistema e do app
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // mostra as barras do sistema e do app
    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }



}
