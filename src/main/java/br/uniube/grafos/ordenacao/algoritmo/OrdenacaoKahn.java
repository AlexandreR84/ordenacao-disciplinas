package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Ordenação topológica pelo algoritmo de Kahn.
 *
 * <ol>
 *   <li>Calcula o grau de entrada de cada vértice.</li>
 *   <li>Coloca numa fila (FIFO) todos os vértices com grau de entrada zero:
 *       disciplinas sem pré-requisito pendente.</li>
 *   <li>Retira um vértice da fila, coloca-o na ordem e "remove" suas arestas
 *       de saída, decrementando o grau de entrada dos sucessores. Quem chegar a
 *       zero entra na fila.</li>
 *   <li>Se a fila esvaziar antes de todos os vértices serem ordenados, os que
 *       sobraram têm grau de entrada positivo para sempre: há um ciclo.</li>
 * </ol>
 *
 * <p>Complexidade: O(V + E) com listas de adjacência. Cada vértice entra e sai
 * da fila uma vez; cada aresta é examinada uma vez, quando sua origem sai da fila.</p>
 */
public class OrdenacaoKahn {

    public static final String NOME = "Kahn";

    public ResultadoOrdenacao ordenar(GrafoDisciplinas grafo) {
        List<String> passos = new ArrayList<>();
        Map<String, Integer> grau = grafo.grausDeEntrada();
        passos.add("Graus de entrada iniciais: " + grau);

        Deque<String> fila = new ArrayDeque<>();
        for (String v : grafo.vertices()) {
            if (grau.get(v) == 0) {
                fila.addLast(v);
            }
        }
        passos.add("Fila inicial (grau zero): " + fila);

        List<String> ordem = new ArrayList<>();
        while (!fila.isEmpty()) {
            String v = fila.removeFirst();
            ordem.add(v);
            StringBuilder passo = new StringBuilder("Retira " + v + " da fila e adiciona à ordem");
            for (String w : grafo.sucessores(v)) {
                int novo = grau.merge(w, -1, Integer::sum);
                passo.append("; grau(").append(w).append(") passa a ").append(novo);
                if (novo == 0) {
                    fila.addLast(w);
                    passo.append(" → ").append(w).append(" entra na fila");
                }
            }
            passo.append(". Fila: ").append(fila);
            passos.add(passo.toString());
        }

        if (ordem.size() < grafo.numeroVertices()) {
            List<String> pendentes = grafo.vertices().stream().filter(v -> grau.get(v) > 0).toList();
            passos.add("Fila vazia com " + pendentes.size() + " vértice(s) ainda com grau de entrada positivo: "
                    + pendentes + ". Existe ciclo.");
            return ResultadoOrdenacao.comCiclo(NOME, ordem, List.of(), pendentes, passos);
        }
        passos.add("Todos os " + ordem.size() + " vértices foram ordenados. Não há ciclo.");
        return ResultadoOrdenacao.sucesso(NOME, ordem, passos);
    }

    /**
     * Agrupa as disciplinas em períodos: Kahn executado nível a nível.
     *
     * <p>O nível 1 são os vértices de grau de entrada zero. O nível k+1 são os que
     * ficam com grau zero quando todo o nível k é removido. Assim, cada disciplina
     * cai no período mais cedo possível, e o número de períodos é igual ao número de
     * vértices do caminho mais longo do grafo, que é o mínimo possível. Também é O(V + E).</p>
     *
     * @return lista de períodos (o índice 0 é o 1º período)
     * @throws IllegalStateException se o grafo tiver ciclo
     */
    public List<List<String>> periodos(GrafoDisciplinas grafo) {
        Map<String, Integer> grau = grafo.grausDeEntrada();
        List<String> nivel = grafo.vertices().stream().filter(v -> grau.get(v) == 0).toList();
        List<List<String>> periodos = new ArrayList<>();
        int processados = 0;
        while (!nivel.isEmpty()) {
            periodos.add(nivel);
            processados += nivel.size();
            List<String> proximo = new ArrayList<>();
            for (String v : nivel) {
                for (String w : grafo.sucessores(v)) {
                    if (grau.merge(w, -1, Integer::sum) == 0) {
                        proximo.add(w);
                    }
                }
            }
            nivel = proximo;
        }
        if (processados < grafo.numeroVertices()) {
            throw new IllegalStateException("o grafo tem ciclo: não é possível montar os períodos");
        }
        return periodos;
    }
}
