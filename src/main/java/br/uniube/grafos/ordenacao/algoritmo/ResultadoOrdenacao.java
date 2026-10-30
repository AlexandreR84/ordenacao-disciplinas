package br.uniube.grafos.ordenacao.algoritmo;

import java.util.List;

/**
 * Resultado de uma ordenação topológica.
 *
 * @param algoritmo nome do algoritmo que produziu o resultado
 * @param ordem     ordenação encontrada; se houver ciclo, é a parte que o algoritmo
 *                  conseguiu ordenar antes de parar (pode ser vazia)
 * @param ciclo     se houver ciclo e o algoritmo souber apontá-lo (DFS), o ciclo
 *                  fechado, ex.: [A, B, C, A]; vazio caso contrário
 * @param pendentes se houver ciclo, vértices que ficaram com grau de entrada positivo
 *                  (Kahn): os do ciclo e os que dependem dele; vazio caso contrário
 * @param passos    registro passo a passo da execução, para explicação e demonstração
 */
public record ResultadoOrdenacao(String algoritmo, List<String> ordem, boolean possuiCiclo,
                                 List<String> ciclo, List<String> pendentes, List<String> passos) {

    public ResultadoOrdenacao {
        ordem = List.copyOf(ordem);
        ciclo = List.copyOf(ciclo);
        pendentes = List.copyOf(pendentes);
        passos = List.copyOf(passos);
    }

    public static ResultadoOrdenacao sucesso(String algoritmo, List<String> ordem, List<String> passos) {
        return new ResultadoOrdenacao(algoritmo, ordem, false, List.of(), List.of(), passos);
    }

    public static ResultadoOrdenacao comCiclo(String algoritmo, List<String> ordemParcial, List<String> ciclo,
                                              List<String> pendentes, List<String> passos) {
        return new ResultadoOrdenacao(algoritmo, ordemParcial, true, ciclo, pendentes, passos);
    }

    /** Ordem no formato "A → B → C". */
    public String ordemFormatada() {
        return String.join(" → ", ordem);
    }
}
