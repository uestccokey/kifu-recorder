package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

import android.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.BuildConfig;
import br.edu.ifspsaocarlos.sdm.kifurecorder.TestsActivity;

/**
 * Representa uma partida completa, com a sequência de tabuleiros e jogadas que foram feitos.
 */
public class Game implements Serializable {

    private String jogadorDePretas;
    private String jogadorDeBrancas;
    private String komi;
    private int dimensaoDoTabuleiro;
    private List<Move> moves;
    private List<Board> boards;

    // Atributo para medir a precisão do sistema
    private int numeroDeVezesQueVoltou;
    private int numeroDeVezesQueTeveQueAdicionarManualmente;

    public Game(int dimensaoDoTabuleiro, String jogadorDePretas, String jogadorDeBrancas, String komi) {
        this.dimensaoDoTabuleiro = dimensaoDoTabuleiro;
        this.jogadorDePretas = jogadorDePretas;
        this.jogadorDeBrancas = jogadorDeBrancas;
        this.komi = komi;
        moves = new ArrayList<>();
        boards = new ArrayList<>();

        Board emptyBoard = new Board(dimensaoDoTabuleiro);
        boards.add(emptyBoard);
        numeroDeVezesQueVoltou = 0;
        numeroDeVezesQueTeveQueAdicionarManualmente = 0;
    }

    public int getDimensaoDoTabuleiro() {
        return dimensaoDoTabuleiro;
    }

    public String getJogadorDePretas() {
        return jogadorDePretas;
    }

    public String getJogadorDeBrancas() {
        return jogadorDeBrancas;
    }

    public boolean adicionarJogadaSeForValida(Board board) {
        Move movePlayed = board.getDifferenceTo(ultimoTabuleiro());

        if (movePlayed == null || repeteEstadoAnterior(board) || !proximaJogadaPodeSer(movePlayed.cor)) {
            return false;
        }

        boards.add(board);
        moves.add(movePlayed);
        Log.i(TestsActivity.TAG, "Adicionando tabuleiro " + board + " (jogada " + movePlayed.sgf() + ") à partida.");
        return true;
    }

    /**
     * Retorna verdadeiro se o tabuleiroNovo repete algum dos tabuleiros anteriores da partida
     * (regra do superko).
     */
    private boolean repeteEstadoAnterior(Board newBoard) {
        for (Board board : boards) {
            if (board.equals(newBoard)) return true;
        }
        return false;
    }

    public boolean proximaJogadaPodeSer(int cor) {
        if (cor == Board.BLACK_STONE)
            return ehPrimeiraJogada() || apenasPedrasPretasForamJogadas() || ultimaJogadaFoiBranca();
        else if (cor == Board.WHITE_STONE)
            return ultimaJogadaFoiPreta();
        return false;
    }

    private boolean ehPrimeiraJogada() {
        return moves.isEmpty();
    }

    /**
     * Este método é usado para verificar se as pedras de handicap estão sendo colocadas.
     */
    private boolean apenasPedrasPretasForamJogadas() {
        for (Move move : moves) {
            if (move.cor == Board.WHITE_STONE) return false;
        }
        return true;
    }

    private boolean ultimaJogadaFoiBranca() {
        return !ehPrimeiraJogada() && ultimaJogada().cor == Board.WHITE_STONE;
    }

    private boolean ultimaJogadaFoiPreta() {
        return !ehPrimeiraJogada() && ultimaJogada().cor == Board.BLACK_STONE;
    }

    public Move ultimaJogada() {
        if (moves.isEmpty()) return null;
        return moves.get(moves.size() - 1);
    }

    public Board ultimoTabuleiro() {
        return boards.get(boards.size() - 1);
    }

    /**
     * Desconsidera a última jogada feita e a retorna
     */
    public Move voltarUltimaJogada() {
        if (moves.isEmpty()) return null;
        boards.remove(boards.size() - 1);
        Move lastMove = moves.remove(moves.size() - 1);
        numeroDeVezesQueVoltou++;
        return lastMove;
    }

    public int numeroDeJogadasFeitas() {
        return moves.size();
    }

    public void adicionouJogadaManualmente() {
        numeroDeVezesQueTeveQueAdicionarManualmente++;
    }

    /**
     * Rotaciona todos os tabuleiros desta partida em sentido horário (direcao = 1) ou em sentido
     * anti-horário (direcao = -1).
     */
    public void rotacionar(int direcao) {
        if (direcao != -1 && direcao != 1) return;

        List<Board> tabuleirosRotacionados = new ArrayList<>();
        for (Board board : boards) {
            tabuleirosRotacionados.add(board.rotate(direcao));
        }

        List<Move> jogadasRotacionadas = new ArrayList<>();
        for (int i = 1; i < tabuleirosRotacionados.size(); ++i) {
            Board ultimo = tabuleirosRotacionados.get(i);
            Board penultimo = tabuleirosRotacionados.get(i - 1);
            jogadasRotacionadas.add(ultimo.getDifferenceTo(penultimo));
        }

        boards = tabuleirosRotacionados;
        moves = jogadasRotacionadas;
    }

    // SGF methods should be extracted to a SgfBuilder class that receives a Game as parameter

    /**
     * Exporta a partida em formato SGF.
     * Referência: http://www.red-bean.com/sgf/
     */
    public String sgf() {
        StringBuilder sgf = new StringBuilder();
        escreverCabecalho(sgf);
        for (Move move : moves) {
            Log.i(TestsActivity.TAG, "construindo SGF - jogada " + move.sgf());
            sgf.append(move.sgf());
        }
        sgf.append(")");
        return sgf.toString();
    }

    private void escreverCabecalho(StringBuilder sgf) {
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String data = sdf.format(new Date(c.getTimeInMillis()));

        sgf.append("(;");
        escreverProperiedade(sgf, "FF", "4");     // Versão do SGF
        escreverProperiedade(sgf, "GM", "1");     // Tipo de jogo (1 = Go)
        escreverProperiedade(sgf, "CA", "UTF-8");
        escreverProperiedade(sgf, "SZ", "" + ultimoTabuleiro().getDimension());
        escreverProperiedade(sgf, "DT", data);
        escreverProperiedade(sgf, "AP", "Kifu Recorder v" + BuildConfig.VERSION_NAME);
        escreverProperiedade(sgf, "KM", komi);
        escreverProperiedade(sgf, "PW", jogadorDeBrancas);
        escreverProperiedade(sgf, "PB", jogadorDePretas);
        escreverProperiedade(sgf, "Z1", "" + numeroDeJogadasFeitas());
        escreverProperiedade(sgf, "Z2", "" + numeroDeVezesQueVoltou);
        escreverProperiedade(sgf, "Z3", "" + numeroDeVezesQueTeveQueAdicionarManualmente);
    }

    private void escreverProperiedade(StringBuilder sgf, String propriedade, String valor) {
        sgf.append(propriedade);
        sgf.append("[");
        sgf.append(valor);
        sgf.append("]");
    }

}