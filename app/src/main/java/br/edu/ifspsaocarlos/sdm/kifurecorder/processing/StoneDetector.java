package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.TestsActivity;
import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Board;
import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Move;

/**
 * Detecta as pedras do tabuleiro que está na imagem, retornando um objeto
 * Tabuleiro correspondente ao estado atual do jogo.
 */
public class StoneDetector {

    // Imagem ortogonal e quadrada do tabuleiro
    private Mat imagemDoTabuleiro = null;
    // Dimensões do tabuleiro (9x9, 13x13 ou 19x19)
    private int dimensaoDoTabuleiro = 0;
    // Informações de debug do estado atual sendo visto pelo detector
    public StringBuilder snapshot;

    public void setDimensaoDoTabuleiro(int dimensaoDoTabuleiro) {
        this.dimensaoDoTabuleiro = dimensaoDoTabuleiro;
    }

    public void setImagemDoTabuleiro(Mat imagemDoTabuleiro) {
        this.imagemDoTabuleiro = imagemDoTabuleiro;
    }

    /**
     * Detecção de pedras que não utiliza o estado anterior da partida.
     */
    public Board detectar() {

        Board board = new Board(dimensaoDoTabuleiro);

        double[] corMediaDoTabuleiro = corMediaDoTabuleiro(imagemDoTabuleiro);

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {
                double[] color = recuperarCorMediaNaPosicao(i, j);

                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
                if (hipotese != Board.EMPTY) {
                    board.putStone(i, j, hipotese);
                }
            }
        }

        return board;
    }

    /**
     * Utiliza a informação do último estado do jogo para melhorar a detecção da
     * última jogada feita. Os parâmetros informam se o detector deve procurar
     * uma pedra preta, branca, ou ambas, de acordo com o estado atual da
     * partida.
     */
    public Board detectar(Board lastBoard, boolean podeSerPedraPreta, boolean podeSerPedraBranca) {
        long tempoEntrou             = System.currentTimeMillis();
        double[][] coresMedias       = new double[3][imagemDoTabuleiro.channels()];
        int[] contadores             = new int[3];

        snapshot = new StringBuilder();

        encontrarCoresMedias(lastBoard, coresMedias, contadores);

        List<MoveHypothesis> hipotesesDeJogadasEncontradas = new ArrayList<>();

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {

//                Log.i(TestsActivity.TAG, "(" + i + ", " + j + ")\n");
                snapshot.append(String.format("(%1$2d, %2$2d)", i, j) + "\n");

                // Ignora as interseções das jogadas que já foram feitas
                if (lastBoard.getPosition(i, j) != Board.EMPTY) continue;

                double[] corAoRedorDaPosicao = recuperarCorMediaNaPosicao(i, j);

                double[][] coresNasPosicoesLivresAdjacentes = new double[4][];
                coresNasPosicoesLivresAdjacentes[0] = (i > 0) ? lastBoard.getPosition(i - 1, j) == Board.EMPTY ? recuperarCorMediaNaPosicao(i - 1, j) : null : null;
                coresNasPosicoesLivresAdjacentes[1] = (j < dimensaoDoTabuleiro - 1) ? lastBoard.getPosition(i, j + 1) == Board.EMPTY ? recuperarCorMediaNaPosicao(i, j + 1) : null : null;
                coresNasPosicoesLivresAdjacentes[2] = (i < dimensaoDoTabuleiro - 1) ? lastBoard.getPosition(i + 1, j) == Board.EMPTY ? recuperarCorMediaNaPosicao(i + 1, j) : null : null;
                coresNasPosicoesLivresAdjacentes[3] = (j > 0) ? lastBoard.getPosition(i, j - 1) == Board.EMPTY ? recuperarCorMediaNaPosicao(i, j - 1) : null : null;

/*                Log.d(TestsActivity.TAG, "Cor média ao redor de (" + i + ", " + j + ") = " + printColor(corAoRedorDaPosicao));
                Log.d(TestsActivity.TAG, "Luminancia ao redor de (" + i + ", " + j + ") = " + luminancia(corAoRedorDaPosicao));
                Log.d(TestsActivity.TAG, "Variância ao redor de (" + i + ", " + j + ") = " + variancia(corAoRedorDaPosicao));*/
                snapshot.append("    Cor média ao redor  = " + printColor(corAoRedorDaPosicao) + "\n");
                snapshot.append("    Luminancia ao redor = " + luminancia(corAoRedorDaPosicao) + "\n");
                snapshot.append("    Variância ao redor  = " + variancia(corAoRedorDaPosicao) + "\n");
                snapshot.append("    ---\n");

                MoveHypothesis hipotese = hipoteseDeCor5(corAoRedorDaPosicao, coresMedias, contadores, coresNasPosicoesLivresAdjacentes);
                hipotese.linha = i;
                hipotese.coluna = j;

                snapshot.append("    Hipótese = " + hipotese.cor + " (confiança: " + hipotese.confianca + ")\n");

                if (hipotese.cor != Board.EMPTY) {
                    hipotesesDeJogadasEncontradas.add(hipotese);
                }
                /*
                Ao invés de filtrar as jogadas por cor antes, vamos filtrá-las depois, acho que faz mais
                sentido. Pega-se a jogada mais provável e verifica-se se ela é possível.
                if (hipotese.cor != Board.EMPTY) {
                    if (podeSerPedraPreta && hipotese.cor == Board.BLACK_STONE ||
                            podeSerPedraBranca && hipotese.cor == Board.WHITE_STONE) {
                        hipotesesDeJogadasEncontradas.add(hipotese);
                    }
                }
                */

            }
        }

        // Escolhe a jogada que obteve maior confiança
        // IMPORTANTE: Poderia verificar se a diferença de confiança entre as
        // duas jogadas mais prováveis for muito pequena, desconsiderar ambas,
        // porque é um sinal que o detector está confuso
        
        Move chosenMove = null;
        double maiorConfianca = 0;
        for (MoveHypothesis hipotese : hipotesesDeJogadasEncontradas) {
            if (hipotese.confianca > maiorConfianca) {
                maiorConfianca = hipotese.confianca;
                chosenMove = new Move(hipotese.linha, hipotese.coluna, hipotese.cor);
            }
        }

        if (chosenMove != null && (podeSerPedraPreta && chosenMove.cor == Board.BLACK_STONE ||
                podeSerPedraBranca && chosenMove.cor == Board.WHITE_STONE)) {
            snapshot.append("Jogada escolhida = " + chosenMove + " com confiança " + maiorConfianca + "\n");
        }
        else {
            snapshot.append("Nenhuma jogada detectada.\n");
            chosenMove = null;
        }

        Log.d(TestsActivity.TAG, "TEMPO (detectar()): " + (System.currentTimeMillis() - tempoEntrou));
        return lastBoard.generateNewBoardWith(chosenMove);
    }

    private void encontrarCoresMedias(Board lastBoard, double[][] coresMedias, int[] contadores) {
        long tempoEntrou = System.currentTimeMillis();
        contadores[Board.EMPTY] = 0;
        contadores[Board.BLACK_STONE] = 0;
        contadores[Board.WHITE_STONE] = 0;

        for (int i = 0; i < dimensaoDoTabuleiro; ++i) {
            for (int j = 0; j < dimensaoDoTabuleiro; ++j) {
                int corNaPosicao = lastBoard.getPosition(i, j);
                contadores[corNaPosicao]++;
                double[] mediaDeCorNaPosicao = recuperarCorMediaNaPosicao(i, j);

                for (int k = 0; k < imagemDoTabuleiro.channels(); ++k) {
                    coresMedias[corNaPosicao][k] += mediaDeCorNaPosicao[k];
                }
            }
        }
        Log.d(TestsActivity.TAG, "TEMPO (detectar() (1)): " + (System.currentTimeMillis() - tempoEntrou));

        for (int i = 0; i < 3; ++i) {
            if (contadores[i] > 0) {
                for (int j = 0; j < imagemDoTabuleiro.channels(); ++j) {
                    coresMedias[i][j] /= contadores[i];
                }
//                Log.d(TestsActivity.TAG, "Cor média[" + i + "] = " + printColor(coresMedias[i]));
//                Log.d(TestsActivity.TAG, "Luminancia[" + i + "] = " + luminancia(coresMedias[i]));
                snapshot.append("Cor média (");
                if (i == Board.EMPTY) {
                    snapshot.append("interseções livres");
                }
                else if (i == Board.BLACK_STONE) {
                    snapshot.append("pedras pretas");
                }
                else if (i == Board.WHITE_STONE) {
                    snapshot.append("pedras brancas");
                }
                snapshot.append(") = " + printColor(coresMedias[i]) + "\n");
                snapshot.append("    Luminancia = " + luminancia(coresMedias[i]) + "\n");
                snapshot.append("    Variância = " + variancia(coresMedias[i]) + "\n");
            }
        }
    }

    private double luminancia(double cor[]) {
        return 0.299 * cor[0] + 0.587 * cor[1] + 0.114 * cor[2];
    }

    private MoveHypothesis hipoteseDeCor5(double[] cor, double[][] coresMedias, int[] contadores, double[][] coresNasPosicoesAdjacentes) {
        double[] preto = {10.0, 10.0, 10.0, 255.0};

        double luminanciaSendoVerificada = luminancia(cor);
        double varianciaSendoVerificada = variancia(cor);
        double distanciaParaPreto = distanciaDeCor(cor, preto);
        double diferencaDeLuminanciaParaOsVizinhos = diferencaDeLuminancia(cor, coresNasPosicoesAdjacentes);
        snapshot.append("    Diferença de luminância para as posições adjacentes vazias = " + diferencaDeLuminanciaParaOsVizinhos + "\n");

        double distanciaParaMediaIntersecoes = 999;
        double distanciaParaLuminanciaIntersecoes = 999;
        double distanciaParaVarianciaIntersecoes = 999;
        if (contadores[Board.EMPTY] > 0) {
            distanciaParaMediaIntersecoes = distanciaDeCor(cor, coresMedias[Board.EMPTY]);
            distanciaParaLuminanciaIntersecoes = Math.abs(luminanciaSendoVerificada - luminancia(coresMedias[Board.EMPTY])) ;
            distanciaParaVarianciaIntersecoes = Math.abs(varianciaSendoVerificada - variancia(coresMedias[Board.EMPTY]));
        }
        double distanciaParaMediaPecasPretas = 999;
        double distanciaParaLuminanciaPecasPretas = 999;
        double distanciaParaVarianciaPecasPretas = 999;
        if (contadores[Board.BLACK_STONE] > 0) {
            distanciaParaMediaPecasPretas = distanciaDeCor(cor, coresMedias[Board.BLACK_STONE]);
            distanciaParaLuminanciaPecasPretas = Math.abs(luminanciaSendoVerificada - luminancia(coresMedias[Board.BLACK_STONE]));
            distanciaParaVarianciaPecasPretas = Math.abs(varianciaSendoVerificada - variancia(coresMedias[Board.BLACK_STONE]));
        }
        double distanciaParaMediaPecasBrancas = 999;
        double distanciaParaLuminanciaPecasBrancas = 999;
        double distanciaParaVarianciaPecasBrancas = 999;
        if (contadores[Board.WHITE_STONE] > 0) {
            distanciaParaMediaPecasBrancas = distanciaDeCor(cor, coresMedias[Board.WHITE_STONE]);
            distanciaParaLuminanciaPecasBrancas = Math.abs(luminanciaSendoVerificada - luminancia(coresMedias[Board.WHITE_STONE]));
            distanciaParaVarianciaPecasBrancas = Math.abs(varianciaSendoVerificada - variancia(coresMedias[Board.WHITE_STONE]));
        }

        double distanciaParaIntersecoes = distanciaParaMediaIntersecoes + distanciaParaLuminanciaIntersecoes + distanciaParaVarianciaIntersecoes;
        double distanciaParaPretas      = distanciaParaMediaPecasPretas + distanciaParaLuminanciaPecasPretas + distanciaParaVarianciaPecasPretas;
        double distanciaParaBrancas     = distanciaParaMediaPecasBrancas + distanciaParaLuminanciaPecasBrancas + distanciaParaVarianciaPecasBrancas;

        snapshot.append("    Distância para média das pedras pretas    = " + distanciaParaMediaPecasPretas + "\n");
        snapshot.append("    Distancia para luminância das pretas      = " + distanciaParaLuminanciaPecasPretas + "\n");
        snapshot.append("    Distancia para variancia das pretas       = " + distanciaParaVarianciaPecasPretas + "\n");
        snapshot.append("    Distância para pretas                     = " + distanciaParaPretas + "\n");
        snapshot.append("    Distância para média das pedras brancas   = " + distanciaParaMediaPecasBrancas + "\n");
        snapshot.append("    Distancia para luminância das brancas     = " + distanciaParaLuminanciaPecasBrancas + "\n");
        snapshot.append("    Distancia para variancia das brancas      = " + distanciaParaVarianciaPecasBrancas + "\n");
        snapshot.append("    Distância para brancas                    = " + distanciaParaBrancas + "\n");
        snapshot.append("    Distância para média das interseções      = " + distanciaParaMediaIntersecoes + "\n");
        snapshot.append("    Distancia para luminância das interseçoes = " + distanciaParaLuminanciaIntersecoes + "\n");
        snapshot.append("    Distancia para variancia das interseçoes  = " + distanciaParaVarianciaIntersecoes + "\n");
        snapshot.append("    Distância para interseções                = " + distanciaParaIntersecoes + "\n");

        if (contadores[Board.BLACK_STONE] == 0 && contadores[Board.WHITE_STONE] == 0) {
            if (diferencaDeLuminanciaParaOsVizinhos < -30) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
            if (distanciaParaPreto < 50) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.9);
            }
            if (distanciaParaPreto < distanciaParaMediaIntersecoes) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.7);
            }
            return new MoveHypothesis(Board.EMPTY, 1);
        }

        if (contadores[Board.WHITE_STONE] == 0) {
            if (diferencaDeLuminanciaParaOsVizinhos < -30) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
            if (distanciaParaPreto < 50) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.9);
            }
            if (distanciaParaPreto < distanciaParaMediaIntersecoes) {
                return new MoveHypothesis(Board.BLACK_STONE, 0.7);
            }
            if (diferencaDeLuminanciaParaOsVizinhos > 30) {
                return new MoveHypothesis(Board.WHITE_STONE, 1);
            }
            if (diferencaDeLuminanciaParaOsVizinhos > 15) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.9);
            }
            // Estes valores para pedras brancas precisariam ser revistos
            /*else if (cor[2] >= 150) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.7);
            }*/
            return new MoveHypothesis(Board.EMPTY, 1);
        }

        // Esta condição foi adicionada porque quando uma pedra preta era jogada de forma inválida
        // (após outra pedra preta e não sendo pedra de handicap) acontecia algo inusitado: as
        // posições ao redor da pedra preta eram vistas como pedras brancas devido à diferença de
        // contraste. Verificar se as interseções se parecem com interseções vazias antes de
        // verificar se se parecem com pedras brancas resolve esse problema.
        if (distanciaParaMediaIntersecoes < 20) {
            return new MoveHypothesis(Board.EMPTY, 1);
        }
        if (distanciaParaPreto < 30 || diferencaDeLuminanciaParaOsVizinhos < -30) {
            if (distanciaParaPretas < distanciaParaIntersecoes && distanciaParaIntersecoes - distanciaParaPretas > 100) {
                return new MoveHypothesis(Board.BLACK_STONE, 1);
            }
        }
/*        if (diferencaDeLuminanciaParaOsVizinhos > 30) {
            // O 0.99 é só para os casos em que uma pedra preta é colocada mas uma pedra branca é detectada
            // erroneamente. Com esta confiança em 0.99, a pedra preta tem prioridade.
            return new MoveHypothesis(Board.WHITE_STONE, 0.99);
        }*/
        if (diferencaDeLuminanciaParaOsVizinhos > 15) {
            // Esta verificação é importante, por isso resolvi deixar apenas esta condição de > 15 e tirar a de cima
            if (distanciaParaBrancas < distanciaParaIntersecoes && distanciaParaIntersecoes - distanciaParaBrancas > 100) {
                return new MoveHypothesis(Board.WHITE_STONE, 0.99);
            }
        }

        double[] probabilidadeDeSer = new double[3];

        probabilidadeDeSer[Board.BLACK_STONE] = 1 - (distanciaParaPretas);
        probabilidadeDeSer[Board.WHITE_STONE] = 1 - (distanciaParaBrancas);
        probabilidadeDeSer[Board.EMPTY] = 1 - (distanciaParaIntersecoes);

        snapshot.append("    Probabilidade de ser pedra preta  = " + probabilidadeDeSer[Board.BLACK_STONE] + "\n");
        snapshot.append("    Probabilidade de ser pedra branca = " + probabilidadeDeSer[Board.WHITE_STONE] + "\n");
        snapshot.append("    Probabilidade de ser vazio        = " + probabilidadeDeSer[Board.EMPTY] + "\n");

        if (probabilidadeDeSer[Board.BLACK_STONE] > probabilidadeDeSer[Board.WHITE_STONE] &&
                probabilidadeDeSer[Board.BLACK_STONE] > probabilidadeDeSer[Board.EMPTY]) {

            if (Math.abs(probabilidadeDeSer[Board.BLACK_STONE] - probabilidadeDeSer[Board.EMPTY]) < 100) {
                return new MoveHypothesis(Board.EMPTY, 0.5);
            }

            double diferencas = probabilidadeDeSer[Board.BLACK_STONE] - probabilidadeDeSer[Board.WHITE_STONE];
            diferencas += probabilidadeDeSer[Board.BLACK_STONE] - probabilidadeDeSer[Board.EMPTY];
            snapshot.append("    Hipótese de ser pedra preta com diferenças de " + (diferencas / 2) + "\n");
            return new MoveHypothesis(Board.BLACK_STONE, diferencas / 2);
        }

        if (probabilidadeDeSer[Board.WHITE_STONE] > probabilidadeDeSer[Board.BLACK_STONE] &&
                probabilidadeDeSer[Board.WHITE_STONE] > probabilidadeDeSer[Board.EMPTY]) {

            // Esta possível pedra branca está quase indistinguível de uma interseção vazia.
            // Para diminuir os falsos positivos, consideramos que é uma interseção bazia.
            if (Math.abs(probabilidadeDeSer[Board.WHITE_STONE] - probabilidadeDeSer[Board.EMPTY]) < 100) {
                return new MoveHypothesis(Board.EMPTY, 0.5);
            }

            double diferencas = probabilidadeDeSer[Board.WHITE_STONE] - probabilidadeDeSer[Board.BLACK_STONE];
            diferencas += probabilidadeDeSer[Board.WHITE_STONE] - probabilidadeDeSer[Board.EMPTY];
            snapshot.append("    Hipótese de ser pedra branca com diferenças de " + (diferencas / 2) + "\n");
            return new MoveHypothesis(Board.WHITE_STONE, diferencas / 2);
        }

        return new MoveHypothesis(Board.EMPTY, 1);
    }

    private double diferencaDeLuminancia(double cor[], double corNasPosicoesAdjacentes[][]) {
        double diferenca = 0;
        double luminanciaDoCentro = luminancia(cor);
        int numeroVizinhosValidos = 0;
        for (int i = 0; i < 4; ++i) {
            if (corNasPosicoesAdjacentes[i] != null) {
                double luminancia = luminancia(corNasPosicoesAdjacentes[i]);
                diferenca += (luminanciaDoCentro - luminancia);
                numeroVizinhosValidos++;
            }
        }
        if (numeroVizinhosValidos > 0) {
            return diferenca / (double)numeroVizinhosValidos;
        }
        return 0;
    }

    private String printColor(double color[]) {
        StringBuilder saida = new StringBuilder("(");
        for (int i = 0; i < color.length; ++i) {
            saida.append(color[i] + ", ");
        }
        saida.append(")");
        return saida.toString();
    }

    private double variancia(double cor[]) {
        double media = (cor[0] + cor[1] + cor[2]) / 3;
        double diferencas[] = {cor[0] - media, cor[1] - media, cor[2] - media};
        return (diferencas[0] * diferencas[0] +
                diferencas[1] * diferencas[1] +
                diferencas[2] * diferencas[2]) / 3;
    }

    // TODO: Transformar hipóteses de recuperação de cor em classes separadas
    private double[] recuperarCorMediaNaPosicao(int linha, int coluna) {

        int y = linha * (int)imagemDoTabuleiro.size().width / (dimensaoDoTabuleiro - 1);
        int x = coluna * (int)imagemDoTabuleiro.size().height / (dimensaoDoTabuleiro - 1);

        double[] color = recuperarMediaDeCores(y, x);
//        double[] color = recuperarMediaGaussianaDeCores(imagemDoTabuleiro, linha, coluna);
        return color;
    }

    /**
     * Recupera a cor media ao redor de uma posiçao na imagem
     *
     * @param y
     * @param x
     * @return
     */
    private double[] recuperarMediaDeCores(int y, int x) {
//        long tempoEntrou = System.currentTimeMillis();

        /**
         * A imagem do tabuleiro ortogonal tem 500x500 pixels de dimensão.
         * Este cálculo pega mais ou menos o tamanho de pouco menos de metade de uma pedra na imagem do
         * tabuleiro ortogonal.
         * 9x9 -> 25
         * 13x13 -> 17
         * 19x19 -> 11
         * 
         * Antes o raio sendo utilizado era de 8 pixels. Em um tabuleiro 9x9 em uma imagme de 500x500
         * pixels, um raio de 8 pixels, em uma interseção que tem o ponto do hoshi, o detector quase
         * achava que havia uma pedra preta ali.
         */ 
        //int radius = 500 / (partida.getDimensaoDoTabuleiro() - 1) * 0.33;
        int radius = 0;
        if (dimensaoDoTabuleiro == 9) {
            radius = 21;
        }
        else if (dimensaoDoTabuleiro == 13) {
            radius = 14;
        }
        else if (dimensaoDoTabuleiro == 19) {
            radius = 9;
        }

        // Não é um círculo, mas pelo speedup, acho que compensa pegar a média
        // de cores assim
        Mat roi = imagemDoTabuleiro.submat(
                Math.max(y - radius, 0),
                Math.min(y + radius, imagemDoTabuleiro.height()),
                Math.max(x - radius, 0),
                Math.min(x + radius, imagemDoTabuleiro.width())
        );
        Scalar mediaScalar = Core.mean(roi);

        double[] corMedia = new double[imagemDoTabuleiro.channels()];
        for (int i = 0; i < mediaScalar.val.length; ++i) {
            corMedia[i] = mediaScalar.val[i];
        }

//        Log.i(TestsActivity.TAG, "Cor média ao redor de (" + x + ", " + y + ") = " + printColor(corMedia));
//        Log.d(TestsActivity.TAG, "TEMPO (recuperarMediaDeCores()): " + (System.currentTimeMillis() - tempoEntrou));
        return corMedia;
    }

    /*
    private double[] recuperarMediaGaussianaDeCores(int y, int x) {
        double[] color = new double[imagem.channels()];
        for (int i = 0; i < color.length; ++i) {
            color[i] = 0;
        }
        int radius = 10;
        double contador = 0;

        for (int yy = y - radius; yy <= y + radius; ++yy) {
            if (yy < 0 || yy >= imagem.height()) continue;
            for (int xx = x - radius; xx <= x + radius; ++xx) {
                if (xx < 0 || xx >= imagem.width()) continue;
                double distancia = distance(xx, yy, x, y);
                if (distancia < radius) {
                    double c[] = imagem.get(yy, xx);
                    double peso = 1 / (distancia / 2 + 0.1);
                    for (int i = 0; i < c.length; ++i) {
                        color[i] += c[i] * peso;
                    }
                    contador += peso;
                }
            }
        }

        for (int i = 0; i < color.length; ++i) {
            color[i] /= contador;
        }

//        Log.i(TestsActivity.TAG, printColor(color));

        return color;
    }

    private double distance(int x, int y, int x2, int y2) {
        return Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
    }
    */

    /**
     * Verifica se uma determinada cor está mais próxima de uma pedra preta ou branca.
     *
     * @param cor Cor a ser verificada
     * @param corMediaDoTabuleiro Cor média da imagem do tabuleiro
     * @return Pedra preta, branca, ou vazio
     */
    private int hipoteseDeCor(double[] cor, double[] corMediaDoTabuleiro) {
        double[] preto = {0.0, 0.0, 0.0, 255.0};
        double[] branco = {255.0, 255.0, 255.0, 255.0};
        double distanciaParaPreto = distanciaDeCor(cor, preto);
        double distanciaParaBranco = distanciaDeCor(cor, branco);
        double distanciaParaCorMedia = distanciaDeCor(cor, corMediaDoTabuleiro);

        // Testando outras hipóteses
        if (distanciaParaPreto < 80 || distanciaParaPreto < distanciaParaCorMedia) {
            return Board.BLACK_STONE;
        }
//        else if (cor[2] >= 150) {
        else if (cor[2] >= corMediaDoTabuleiro[2] * 1.35) {
            return Board.WHITE_STONE;
        }
        else if (true) {
            return Board.EMPTY;
        }

        // Se a distância para a média for menor que um certo threshold, muito provavelmente é uma
        // intersecção vazia
        if (distanciaParaCorMedia < 120) {
            return Board.EMPTY;
        }

        if (distanciaParaPreto < distanciaParaBranco) {
            return Board.BLACK_STONE;
        }
        else {
            return Board.WHITE_STONE;
        }
    }

    private double distanciaDeCor(double[] cor1, double[] cor2) {
        double distancia = 0;
        for (int i = 0; i < Math.min(cor1.length, cor2.length); ++i) {
            distancia += Math.abs(cor1[i] - cor2[i]);
        }
        return distancia;
    }

    /**
     * Retorna a cor media do tabuleiro.
     * 
     * ESTA COR MUDA CONFORME O JOGO PROGRIDE E CONFORME A ILUMINAÇÃO MUDA.
     *
     * @param imagemDoTabuleiro
     * @return
     */
    private double[] corMediaDoTabuleiro(Mat imagemDoTabuleiro) {
        Scalar mediaScalar = Core.mean(imagemDoTabuleiro);

        double[] media = new double[imagemDoTabuleiro.channels()];
        for (int i = 0; i < mediaScalar.val.length; ++i) {
            media[i] = mediaScalar.val[i];
        }

        return media;
    }

}