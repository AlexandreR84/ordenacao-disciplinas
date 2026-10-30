package br.uniube.grafos.ordenacao;

import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;

/** Grafos usados em mais de uma classe de teste. */
public final class Fixtures {

    private Fixtures() { }

    /** Exemplo de entrada do roteiro: A→B, A→C, B→D, C→E. */
    public static GrafoDisciplinas exemploRoteiro() {
        return GrafoDisciplinas.de(
                new Disciplina("A", "Algoritmos", 0),
                new Disciplina("B", "Estruturas de Dados", 0, "A"),
                new Disciplina("C", "Banco de Dados", 0, "A"),
                new Disciplina("D", "Engenharia de Software", 0, "B"),
                new Disciplina("E", "Desenvolvimento Web", 0, "C"));
    }

    /** Ciclo do roteiro: A→B, B→C, C→A. */
    public static GrafoDisciplinas cicloRoteiro() {
        return GrafoDisciplinas.de(
                new Disciplina("A", "A", 0, "C"),
                new Disciplina("B", "B", 0, "A"),
                new Disciplina("C", "C", 0, "B"));
    }
}
