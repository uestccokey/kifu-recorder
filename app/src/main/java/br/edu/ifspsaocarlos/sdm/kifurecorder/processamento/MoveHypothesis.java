package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;

public class MoveHypothesis {
    public int linha;
    public int coluna;
    public int cor;
    public double confianca;
    
    public MoveHypothesis(int cor, double confianca) {
        this.cor = cor;
        this.confianca = confianca;
    }
}