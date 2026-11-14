package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.app.Cenarios;
import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContagemOrdenacoesTest {

    private final ContagemOrdenacoes contagem = new ContagemOrdenacoes();

    private BigInteger contar(Cenarios.Cenario c) {
        return contagem.contar(GrafoDisciplinas.de(c.disciplinas())).orElseThrow();
    }

    private static BigInteger fatorial(int n) {
        BigInteger f = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            f = f.multiply(BigInteger.valueOf(i));
        }
        return f;
    }

    @Test
    void cenariosDoTrabalho() {
        assertEquals(BigInteger.valueOf(552), contar(Cenarios.semCiclo()));
        assertEquals(BigInteger.valueOf(6), contar(Cenarios.multiplasOrdenacoes()));
        assertEquals(BigInteger.ZERO, contar(Cenarios.comCiclo()));
    }

    @Test
    void gradeCompletaDoCursoNaHora() {
        // 59 disciplinas: número de 69 dígitos, conferido por um programa independente em Python
        BigInteger esperado = new BigInteger(
                "110592494999742243530832358941050129348966665927746856358510592000000");
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(10),
                () -> assertEquals(esperado, contar(Cenarios.gradeReal())));
    }

    @Test
    void semArestasDaFatorial() {
        List<Disciplina> lista = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            lista.add(new Disciplina("D" + i, "Disciplina " + i, 0));
        }
        assertEquals(fatorial(30), contagem.contar(GrafoDisciplinas.de(lista)).orElseThrow());
    }

    @Test
    void cadeiaTemUmaSo() {
        List<Disciplina> lista = new ArrayList<>();
        lista.add(new Disciplina("D0", "D0", 0));
        for (int i = 1; i < 60; i++) {
            lista.add(new Disciplina("D" + i, "D" + i, 0, "D" + (i - 1)));
        }
        assertEquals(BigInteger.ONE, contagem.contar(GrafoDisciplinas.de(lista)).orElseThrow());
    }

    @Test
    void componenteMaiorQueOLimiteNaoConta() {
        // estrela: A é pré-requisito de 63 disciplinas, então tudo fica num componente só, de 64
        List<Disciplina> lista = new ArrayList<>();
        lista.add(new Disciplina("A", "A", 0));
        for (int i = 0; i < ContagemOrdenacoes.LIMITE_COMPONENTE; i++) {
            lista.add(new Disciplina("D" + i, "D" + i, 0, "A"));
        }
        assertTrue(contagem.contar(GrafoDisciplinas.de(lista)).isEmpty());
    }

    @Test
    void grafoVazioTemUmaOrdenacao() {
        assertEquals(BigInteger.ONE, contagem.contar(GrafoDisciplinas.de(List.of())).orElseThrow());
    }

    @Test
    void bateComOBacktrackingEmGrafosAleatorios() {
        Random r = new Random(7);
        for (int t = 0; t < 300; t++) {
            int n = 1 + r.nextInt(8);
            List<Disciplina> lista = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                List<String> pre = new ArrayList<>();
                for (int j = 0; j < n; j++) {
                    // j < i gera grafo sem ciclo; às vezes uma aresta de volta cria ciclo
                    if ((j < i && r.nextInt(3) == 0) || (j > i && r.nextInt(25) == 0)) {
                        pre.add("V" + j);
                    }
                }
                lista.add(new Disciplina("V" + i, "V" + i, 0, pre));
            }
            GrafoDisciplinas g = GrafoDisciplinas.de(lista);
            long listadas = new TodasOrdenacoes().listar(g, Integer.MAX_VALUE).size();
            assertEquals(BigInteger.valueOf(listadas), contagem.contar(g).orElseThrow(), "grafo " + lista);
        }
    }
}
