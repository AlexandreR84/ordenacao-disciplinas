package br.uniube.grafos.ordenacao.grafo;

import br.uniube.grafos.ordenacao.modelo.Disciplina;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo direcionado de disciplinas, representado por listas de adjacência.
 *
 * <p>Cada vértice é uma disciplina. Uma aresta {@code A -> B} significa que
 * A é pré-requisito de B. O cadastro guarda os pré-requisitos <em>dentro</em> de B,
 * então, ao montar o grafo, a aresta sai do pré-requisito e chega em B.</p>
 *
 * <p>Os mapas são {@link LinkedHashMap}: a ordem de cadastro é preservada,
 * e as saídas dos algoritmos são sempre as mesmas para a mesma entrada.</p>
 */
public class GrafoDisciplinas {

    private final Map<String, Disciplina> disciplinas = new LinkedHashMap<>();
    /** sucessores: pré-requisito -> disciplinas que dependem dele. */
    private final Map<String, Set<String>> adjacencia = new LinkedHashMap<>();
    /** antecessores: disciplina -> seus pré-requisitos (grafo invertido). */
    private final Map<String, Set<String>> antecessores = new LinkedHashMap<>();
    private int numeroArestas;

    /**
     * Monta o grafo a partir das disciplinas cadastradas.
     *
     * @throws IllegalArgumentException se houver código repetido ou
     *                                  pré-requisito que não foi cadastrado
     */
    public static GrafoDisciplinas de(Collection<Disciplina> lista) {
        GrafoDisciplinas g = new GrafoDisciplinas();
        for (Disciplina d : lista) {
            g.adicionarVertice(d);
        }
        for (Disciplina d : lista) {
            for (String pre : d.preRequisitos()) {
                g.adicionarAresta(pre, d.codigo());
            }
        }
        return g;
    }

    public static GrafoDisciplinas de(Disciplina... lista) {
        return de(List.of(lista));
    }

    private void adicionarVertice(Disciplina d) {
        if (disciplinas.containsKey(d.codigo())) {
            throw new IllegalArgumentException("código repetido: " + d.codigo());
        }
        disciplinas.put(d.codigo(), d);
        adjacencia.put(d.codigo(), new LinkedHashSet<>());
        antecessores.put(d.codigo(), new LinkedHashSet<>());
    }

    /** Adiciona a aresta origem -> destino. Arestas repetidas são ignoradas. */
    private void adicionarAresta(String origem, String destino) {
        if (!disciplinas.containsKey(origem)) {
            throw new IllegalArgumentException(
                    "pré-requisito " + origem + " de " + destino + " não foi cadastrado");
        }
        if (adjacencia.get(origem).add(destino)) {
            antecessores.get(destino).add(origem);
            numeroArestas++;
        }
    }

    /** Vértices na ordem de cadastro. */
    public List<String> vertices() {
        return List.copyOf(disciplinas.keySet());
    }

    /** Disciplinas que têm {@code codigo} como pré-requisito. */
    public Set<String> sucessores(String codigo) {
        return Collections.unmodifiableSet(adjacencia.get(exigir(codigo)));
    }

    /** Pré-requisitos diretos de {@code codigo}. */
    public Set<String> preRequisitos(String codigo) {
        return Collections.unmodifiableSet(antecessores.get(exigir(codigo)));
    }

    public Disciplina disciplina(String codigo) {
        return disciplinas.get(exigir(codigo));
    }

    public boolean contem(String codigo) {
        return disciplinas.containsKey(codigo);
    }

    public int numeroVertices() {
        return disciplinas.size();
    }

    public int numeroArestas() {
        return numeroArestas;
    }

    /** Grau de entrada de cada vértice, calculado percorrendo todas as listas: O(V + E). */
    public Map<String, Integer> grausDeEntrada() {
        Map<String, Integer> grau = new LinkedHashMap<>();
        for (String v : disciplinas.keySet()) {
            grau.put(v, 0);
        }
        for (Set<String> destinos : adjacencia.values()) {
            for (String w : destinos) {
                grau.merge(w, 1, Integer::sum);
            }
        }
        return grau;
    }

    /** Lista de arestas no formato "A -> B", na ordem de cadastro. */
    public List<String> arestas() {
        List<String> lista = new ArrayList<>();
        adjacencia.forEach((v, destinos) -> destinos.forEach(w -> lista.add(v + " -> " + w)));
        return lista;
    }

    /**
     * Confere se uma sequência é uma ordenação topológica válida:
     * contém cada vértice exatamente uma vez e, para toda aresta A -> B,
     * A aparece antes de B.
     */
    public boolean ehOrdenacaoValida(List<String> ordem) {
        if (ordem.size() != disciplinas.size()) {
            return false;
        }
        Map<String, Integer> posicao = new LinkedHashMap<>();
        for (int i = 0; i < ordem.size(); i++) {
            if (!disciplinas.containsKey(ordem.get(i)) || posicao.put(ordem.get(i), i) != null) {
                return false;
            }
        }
        for (var e : adjacencia.entrySet()) {
            for (String w : e.getValue()) {
                if (posicao.get(e.getKey()) >= posicao.get(w)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String exigir(String codigo) {
        if (!disciplinas.containsKey(codigo)) {
            throw new IllegalArgumentException("disciplina não cadastrada: " + codigo);
        }
        return codigo;
    }
}
