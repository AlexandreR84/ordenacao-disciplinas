package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordenação topológica por Busca em Profundidade (DFS).
 *
 * <p>Cada vértice passa por três estados:</p>
 * <ul>
 *   <li>{@link Estado#NAO_VISITADO}: ainda não foi alcançado;</li>
 *   <li>{@link Estado#EM_PROCESSAMENTO}: a busca entrou nele e ainda está
 *       explorando seus sucessores (ele está na pilha de recursão);</li>
 *   <li>{@link Estado#FINALIZADO}: todos os seus sucessores já foram explorados.</li>
 * </ul>
 *
 * <p>Quando um vértice é finalizado, tudo o que depende dele já foi finalizado antes.
 * Por isso a ordem inversa de finalização é uma ordenação topológica.</p>
 *
 * <p>Ciclo: se, a partir de v, a busca encontra uma aresta para um vértice
 * EM_PROCESSAMENTO, esse vértice é antecessor de v na pilha e também seu sucessor.
 * Isso é uma aresta de retorno, e ela fecha um ciclo. Como a pilha de recursão
 * está guardada, dá para devolver o ciclo exato.</p>
 *
 * <p>Complexidade: O(V + E) com listas de adjacência. Cada vértice é visitado
 * uma vez (só se entra em vértices NAO_VISITADO) e cada aresta é examinada uma vez,
 * quando sua origem é visitada.</p>
 */
public class OrdenacaoDFS {

    public static final String NOME = "DFS";

    public enum Estado { NAO_VISITADO, EM_PROCESSAMENTO, FINALIZADO }

    /** Sinaliza o ciclo encontrado e interrompe a recursão. */
    private static final class CicloEncontrado extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;
        private final transient List<String> ciclo;

        CicloEncontrado(List<String> ciclo) {
            super(null, null, false, false);
            this.ciclo = ciclo;
        }
    }

    private Map<String, Estado> estadosFinais = Map.of();

    public ResultadoOrdenacao ordenar(GrafoDisciplinas grafo) {
        Map<String, Estado> estado = new LinkedHashMap<>();
        for (String v : grafo.vertices()) {
            estado.put(v, Estado.NAO_VISITADO);
        }
        List<String> finalizados = new ArrayList<>();
        List<String> pilha = new ArrayList<>();
        List<String> passos = new ArrayList<>();

        try {
            for (String v : grafo.vertices()) {
                if (estado.get(v) == Estado.NAO_VISITADO) {
                    passos.add("Nova busca a partir de " + v);
                    visitar(grafo, v, estado, pilha, finalizados, passos);
                }
            }
        } catch (CicloEncontrado c) {
            estadosFinais = Map.copyOf(estado);
            passos.add("Aresta de retorno encontrada. Ciclo: " + String.join(" → ", c.ciclo));
            // A busca para no primeiro ciclo: não há ordenação a devolver, só o ciclo.
            return ResultadoOrdenacao.comCiclo(NOME, List.of(), c.ciclo, List.of(), passos);
        }

        estadosFinais = Map.copyOf(estado);
        List<String> ordem = new ArrayList<>(finalizados);
        Collections.reverse(ordem);
        passos.add("Ordem de finalização: " + finalizados);
        passos.add("Ordem inversa de finalização (ordenação topológica): " + ordem);
        return ResultadoOrdenacao.sucesso(NOME, ordem, passos);
    }

    private void visitar(GrafoDisciplinas grafo, String v, Map<String, Estado> estado,
                         List<String> pilha, List<String> finalizados, List<String> passos) {
        estado.put(v, Estado.EM_PROCESSAMENTO);
        pilha.add(v);
        passos.add("Entra em " + v + " (em processamento). Pilha: " + pilha);

        for (String w : grafo.sucessores(v)) {
            switch (estado.get(w)) {
                case NAO_VISITADO -> visitar(grafo, w, estado, pilha, finalizados, passos);
                case EM_PROCESSAMENTO -> {
                    passos.add("Aresta " + v + " → " + w + ": " + w + " está em processamento");
                    List<String> ciclo = new ArrayList<>(pilha.subList(pilha.indexOf(w), pilha.size()));
                    ciclo.add(w);
                    throw new CicloEncontrado(ciclo);
                }
                case FINALIZADO -> passos.add("Aresta " + v + " → " + w + ": " + w + " já finalizado, ignora");
            }
        }

        pilha.remove(pilha.size() - 1);
        estado.put(v, Estado.FINALIZADO);
        finalizados.add(v);
        passos.add("Finaliza " + v + ". Finalizados: " + finalizados);
    }

    /** Estados de visita ao fim da última execução (para testes e demonstração). */
    public Map<String, Estado> estadosFinais() {
        return estadosFinais;
    }
}
