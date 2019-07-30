package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa um grupo em um tabuleiro.
 */
public class Group {

    private int cor;
    private Set<Position> posicoes;
    private Set<Position> liberdades;

    public Group(int cor) {
        this.cor = cor;
        posicoes = new HashSet<>();
        liberdades = new HashSet<>();
    }

    public int getCor() {
        return cor;
    }

    public Set<Position> getPosicoes() {
        return posicoes;
    }

    public void adicionarPosicao(Position position) {
        posicoes.add(position);
    }

    public void adicionarLiberdade(Position liberdade) {
        liberdades.add(liberdade);
    }

    public boolean estaEmAtari() {
        return liberdades.size() == 1;
    }

    public boolean ehCapturadoPela(Move move) {
        return move.cor != cor && estaEmAtari() && liberdades.contains(move.posicao());
    }

    public boolean naoTemLiberdades() {
        return liberdades.size() == 0;
    }

    @Override
    public boolean equals(Object outro) {
        if (!(outro instanceof Group)) return false;

        Group otherGroup = (Group)outro;
        return cor == otherGroup.cor
            && posicoes.equals(otherGroup.posicoes)
            && liberdades.equals(otherGroup.liberdades);
    }

    @Override
    public int hashCode() {
        return cor + posicoes.hashCode() + liberdades.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append("Grupo de cor " + cor + ": ");
        for (Position position : posicoes) {
            string.append(position);
        }
        return string.toString();
    }

}