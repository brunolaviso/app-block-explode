    package com.example.blocksexplode;

import android.media.AudioManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainActivityFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainActivityFragment extends Fragment {

    //view personalizada para mostrar o jogo
    private CannonView cannonView;

    //chamado quando a view do Fragment precisa ser criada
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //infla o layout fragment_main.xml layout
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        //Obtem a referencia para o componente CannonView
        cannonView = (CannonView) view.findViewById(R.id.cannonView);

        return view;
        //return inflater.inflate(R.layout.fragment_main, container, false);
    }

    //configura o controle do volume quando a Atividade é criada
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //permite que os botoes de volume do dispositivo configure o volume da musica
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    //quando o MainActivity é pausada, termina o jogo
    @Override
    public void onPause() {
        super.onPause();
        cannonView.stopGame(); //termina o game
    }

    //quando MainActivity é pausada, MainActivityFragment libera os recursos
    @Override
    public void onDestroy() {
        super.onDestroy();
        cannonView.releaseResources();
    }





}