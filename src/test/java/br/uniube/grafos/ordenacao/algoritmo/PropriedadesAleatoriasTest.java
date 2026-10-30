package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes com grafos aleatórios (sementes fixas, então o resultado é reproduzível).
 * Conferem as propriedades que definem uma resposta correta, em vez de uma saída específica.
 */
class PropriedadesAleatoriasTest {

    private static final int GRAFOS = 500;

    /**
     * Grafo acíclico aleatório: sorteia uma ordem secreta dos vértices e só cria
     * arestas de quem vem antes para quem vem depois nessa ordem.
     */
    private static GrafoDisciplinas dagAleatorio(Random rnd) {
        int n = 1 + rnd.nextInt(15);
        List<String> nomes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nomes.add("V" + i);
        }
        List<String> secreta = new ArrayList<>(nomes);
        Collections.shuffle(secreta, rnd);
        double densidade = rnd.nextDouble() * 0.5;
        Map<String, List<String>> pre = new HashMap<>();
        nomes.forEach(v -> pre.put(v, new ArrayList<>()));
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (rnd.nextDouble() < densidade) {
                    pre.get(secreta.get(j)).add(secreta.get(i));
                }
            }
        }
        return GrafoDisciplinas.de(nomes.stream().map(v -> new Disciplina(v, v, 0, pre.get(v))).toList());
    }

    @Test
    void emGrafoAciclicoKahnEDfsDevolvemOrdenacoesValidas() {
        Random rnd = new Random(2026);
        for (int k = 0; k < GRAFOS; k++) {
            GrafoDisciplinas g = dagAleatorio(rnd);
            ResultadoOrdenacao kahn = new OrdenacaoKahn().ordenar(g);
            ResultadoOrdenacao dfs = new OrdenacaoDFS().ordenar(g);
            assertFalse(kahn.possuiCiclo());
            assertFalse(dfs.possuiCiclo());
            assertTrue(g.ehOrdenacaoValida(kahn.ordem()), "Kahn inválida em " + g.arestas());
            assertTrue(g.ehOrdenacaoValida(dfs.ordem()), "DFS inválida em " + g.arestas());
        }
    }

    @Test
    void periodosRespeitamAsArestasEUsamOMinimoDePeriodos() {
        Random rnd = new Random(7);
        for (int k = 0; k < GRAFOS; k++) {
            GrafoDisciplinas g = dagAleatorio(rnd);
            List<List<String>> periodos = new OrdenacaoKahn().periodos(g);
            Map<String, Integer> periodoDe = new HashMap<>();
            for (int p = 0; p < periodos.size(); p++) {
                for (String v : periodos.get(p)) {
                    periodoDe.put(v, p);
                }
            }
            assertEquals(g.numeroVertices(), periodoDe.size());
            for (String v : g.vertices()) {
                for (String w : g.sucessores(v)) {
                    assertTrue(periodoDe.get(v) < periodoDe.get(w));
                }
            }
            assertEquals(maiorCaminho(g), periodos.size());
        }
    }

    @Test
    void cicloInseridoSempreEhDetectadoPelosDois() {
        Random rnd = new Random(42);
        int testados = 0;
        while (testados < GRAFOS) {
            GrafoDisciplinas dag = dagAleatorio(rnd);
            if (dag.numeroArestas() == 0) {
                continue; // sem aresta não há o que inverter: sorteia outro grafo
            }
            testados++;
            // Inverte o sentido de um caminho: escolhe uma aresta u→v e acrescenta v→u.
            List<String> arestas = dag.arestas();
            String[] uv = arestas.get(rnd.nextInt(arestas.size())).split(" -> ");
            List<Disciplina> lista = new ArrayList<>();
            for (String v : dag.vertices()) {
                List<String> pre = new ArrayList<>(dag.preRequisitos(v));
                if (v.equals(uv[0])) {
                    pre.add(uv[1]);
                }
                lista.add(new Disciplina(v, v, 0, pre));
            }
            GrafoDisciplinas g = GrafoDisciplinas.de(lista);

            assertTrue(new OrdenacaoKahn().ordenar(g).possuiCiclo());
            ResultadoOrdenacao dfs = new OrdenacaoDFS().ordenar(g);
            assertTrue(dfs.possuiCiclo());
            assertCicloReal(g, dfs.ciclo());
            assertEquals(List.of(), new TodasOrdenacoes().listar(g, 1));
        }
    }

    /** O ciclo devolvido pela DFS existe de fato: fecha no início e cada passo é uma aresta. */
    private static void assertCicloReal(GrafoDisciplinas g, List<String> ciclo) {
        assertTrue(ciclo.size() >= 2);
        assertEquals(ciclo.get(0), ciclo.get(ciclo.size() - 1));
        for (int i = 0; i + 1 < ciclo.size(); i++) {
            assertTrue(g.sucessores(ciclo.get(i)).contains(ciclo.get(i + 1)),
                    "não há aresta " + ciclo.get(i) + " → " + ciclo.get(i + 1));
        }
    }

    /** Número de vértices do caminho mais longo, por programação dinâmica sobre a ordem de Kahn. */
    private static int maiorCaminho(GrafoDisciplinas g) {
        Map<String, Integer> comprimento = new HashMap<>();
        int maior = 0;
        for (String v : new OrdenacaoKahn().ordenar(g).ordem()) {
            int c = 1;
            for (String p : g.preRequisitos(v)) {
                c = Math.max(c, comprimento.get(p) + 1);
            }
            comprimento.put(v, c);
            maior = Math.max(maior, c);
        }
        return maior;
    }
}
