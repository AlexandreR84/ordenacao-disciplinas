package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Conta quantas ordenações topológicas um grafo tem, sem listá-las.
 *
 * <ol>
 *   <li><b>Componentes.</b> O grafo é separado em componentes fracamente conexos (ignorando a
 *       direção das arestas). Disciplinas de componentes diferentes não têm relação nenhuma, então
 *       as ordenações de cada componente podem se intercalar livremente. Com componentes de tamanhos
 *       k1, ..., km (soma n), o total é o coeficiente multinomial n! / (k1! ... km!) vezes o produto
 *       das contagens de cada componente.</li>
 *   <li><b>Cada componente.</b> Programação dinâmica sobre os conjuntos "fechados para baixo":
 *       {@code formas(S)} é o número de maneiras de terminar a grade quando o conjunto S já foi
 *       cursado. Uma disciplina v fora de S pode ser a próxima se todos os seus pré-requisitos estão
 *       em S, e então {@code formas(S) = soma de formas(S ∪ {v})}. Só aparecem conjuntos que respeitam
 *       os pré-requisitos, o que é bem menos que todos os 2^k subconjuntos.</li>
 * </ol>
 *
 * <p>O resultado é um {@link BigInteger}, porque cresce como n!: a grade completa do curso
 * (59 disciplinas) tem um total de 69 dígitos. No pior caso o número de conjuntos ainda é
 * exponencial; por isso há um limite de {@value #LIMITE_ESTADOS} estados, e acima dele a
 * contagem desiste.</p>
 */
public class ContagemOrdenacoes {

    /** Máximo de conjuntos guardados na memória da programação dinâmica. */
    public static final int LIMITE_ESTADOS = 2_000_000;
    /** Um componente é representado por um {@code long}: no máximo 63 disciplinas. */
    public static final int LIMITE_COMPONENTE = 63;

    /**
     * @return o número exato de ordenações (0 se houver ciclo), ou vazio se a conta
     *         passar dos limites
     */
    public Optional<BigInteger> contar(GrafoDisciplinas grafo) {
        if (new OrdenacaoKahn().ordenar(grafo).possuiCiclo()) {
            return Optional.of(BigInteger.ZERO);
        }
        int n = grafo.numeroVertices();
        BigInteger total = fatorial(n);
        List<BigInteger> porComponente = new ArrayList<>();
        int estados = 0;
        for (List<String> componente : componentes(grafo)) {
            if (componente.size() > LIMITE_COMPONENTE) {
                return Optional.empty();
            }
            total = total.divide(fatorial(componente.size()));
            Map<Long, BigInteger> memo = new HashMap<>();
            BigInteger c = contarComponente(grafo, componente, memo, LIMITE_ESTADOS - estados);
            if (c == null) {
                return Optional.empty();
            }
            estados += memo.size();
            porComponente.add(c);
        }
        for (BigInteger c : porComponente) {
            total = total.multiply(c);
        }
        return Optional.of(total);
    }

    /** Componentes fracamente conexos, na ordem de cadastro. */
    public static List<List<String>> componentes(GrafoDisciplinas grafo) {
        Set<String> visitados = new LinkedHashSet<>();
        List<List<String>> lista = new ArrayList<>();
        for (String inicio : grafo.vertices()) {
            if (!visitados.add(inicio)) {
                continue;
            }
            List<String> componente = new ArrayList<>();
            Deque<String> pilha = new ArrayDeque<>(List.of(inicio));
            while (!pilha.isEmpty()) {
                String v = pilha.pop();
                componente.add(v);
                for (String w : grafo.sucessores(v)) {
                    if (visitados.add(w)) {
                        pilha.push(w);
                    }
                }
                for (String w : grafo.preRequisitos(v)) {
                    if (visitados.add(w)) {
                        pilha.push(w);
                    }
                }
            }
            lista.add(componente);
        }
        return lista;
    }

    /** @return a contagem do componente, ou {@code null} se passar do limite de estados */
    private BigInteger contarComponente(GrafoDisciplinas grafo, List<String> componente,
                                        Map<Long, BigInteger> memo, int limite) {
        int k = componente.size();
        Map<String, Integer> indice = new HashMap<>();
        for (int i = 0; i < k; i++) {
            indice.put(componente.get(i), i);
        }
        long[] exigidos = new long[k];
        for (int i = 0; i < k; i++) {
            for (String pre : grafo.preRequisitos(componente.get(i))) {
                exigidos[i] |= 1L << indice.get(pre);
            }
        }
        long todos = k == 64 ? -1L : (1L << k) - 1;
        try {
            return formas(0L, todos, exigidos, memo, limite);
        } catch (LimiteExcedido e) {
            return null;
        }
    }

    private BigInteger formas(long cursadas, long todos, long[] exigidos, Map<Long, BigInteger> memo, int limite) {
        if (cursadas == todos) {
            return BigInteger.ONE;
        }
        BigInteger guardado = memo.get(cursadas);
        if (guardado != null) {
            return guardado;
        }
        BigInteger soma = BigInteger.ZERO;
        for (int v = 0; v < exigidos.length; v++) {
            long bit = 1L << v;
            if ((cursadas & bit) == 0 && (exigidos[v] & ~cursadas) == 0) {
                soma = soma.add(formas(cursadas | bit, todos, exigidos, memo, limite));
            }
        }
        if (memo.size() >= limite) {
            throw new LimiteExcedido();
        }
        memo.put(cursadas, soma);
        return soma;
    }

    private static BigInteger fatorial(int n) {
        BigInteger f = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            f = f.multiply(BigInteger.valueOf(i));
        }
        return f;
    }

    private static final class LimiteExcedido extends RuntimeException {
        private static final long serialVersionUID = 1L;

        LimiteExcedido() {
            super(null, null, false, false);
        }
    }
}
