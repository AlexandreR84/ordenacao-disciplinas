package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lista todas as ordenações topológicas de um grafo, por backtracking sobre a ideia
 * de Kahn: em cada passo, qualquer vértice ainda não usado com grau de entrada zero
 * pode ser o próximo. Testa cada escolha, desfaz e tenta a seguinte.
 *
 * <p>Serve para demonstrar que um mesmo grafo admite várias respostas corretas.
 * O número de ordenações pode crescer de forma exponencial (um grafo sem arestas
 * com n vértices tem n! ordenações), por isso há um limite.</p>
 */
public class TodasOrdenacoes {

    public static final int LIMITE_PADRAO = 1_000;

    public List<List<String>> listar(GrafoDisciplinas grafo) {
        return listar(grafo, LIMITE_PADRAO);
    }

    public List<List<String>> listar(GrafoDisciplinas grafo, int limite) {
        // Com ciclo não existe nenhuma ordenação, mas o backtracking só descobriria
        // isso depois de testar todas as ordens parciais, o que é exponencial.
        // Kahn responde em O(V + E).
        if (new OrdenacaoKahn().ordenar(grafo).possuiCiclo()) {
            return List.of();
        }
        List<List<String>> resultado = new ArrayList<>();
        Map<String, Integer> grau = grafo.grausDeEntrada();
        backtrack(grafo, grau, new LinkedHashSet<>(), new ArrayList<>(), resultado, limite);
        return resultado;
    }

    private void backtrack(GrafoDisciplinas grafo, Map<String, Integer> grau, Set<String> usados,
                           List<String> atual, List<List<String>> resultado, int limite) {
        if (resultado.size() >= limite) {
            return;
        }
        if (atual.size() == grafo.numeroVertices()) {
            resultado.add(List.copyOf(atual));
            return;
        }
        for (String v : grafo.vertices()) {
            if (!usados.contains(v) && grau.get(v) == 0) {
                usados.add(v);
                atual.add(v);
                grafo.sucessores(v).forEach(w -> grau.merge(w, -1, Integer::sum));

                backtrack(grafo, grau, usados, atual, resultado, limite);

                grafo.sucessores(v).forEach(w -> grau.merge(w, 1, Integer::sum));
                atual.remove(atual.size() - 1);
                usados.remove(v);
            }
        }
    }
}
