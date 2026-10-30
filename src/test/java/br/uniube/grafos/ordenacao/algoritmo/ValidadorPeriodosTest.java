package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.app.Cenarios;
import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidadorPeriodosTest {

    private final ValidadorPeriodos validador = new ValidadorPeriodos();

    @Test
    void gradeDoCenario1EhCoerente() {
        assertEquals(List.of(), validador.validar(GrafoDisciplinas.de(Cenarios.semCiclo().disciplinas())));
    }

    @Test
    void preRequisitoNoMesmoPeriodoEhInconsistente() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("ALG", "Algoritmos", 2),
                new Disciplina("ED", "Estruturas de Dados", 2, "ALG"));
        assertEquals(List.of(new ValidadorPeriodos.Inconsistencia("ED", 2, "ALG", 2)), validador.validar(g));
    }

    @Test
    void preRequisitoEmPeriodoPosteriorEhInconsistente() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("ALG", "Algoritmos", 3),
                new Disciplina("ED", "Estruturas de Dados", 2, "ALG"));
        assertEquals(1, validador.validar(g).size());
        assertEquals("ED (período 2) exige ALG, cadastrada no período 3",
                validador.validar(g).get(0).toString());
    }

    @Test
    void periodoNaoInformadoNaoEhConferido() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("ALG", "Algoritmos", 0),
                new Disciplina("ED", "Estruturas de Dados", 1, "ALG"));
        assertEquals(List.of(), validador.validar(g));
    }
}
