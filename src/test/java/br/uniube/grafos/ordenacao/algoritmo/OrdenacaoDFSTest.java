package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.Fixtures;
import br.uniube.grafos.ordenacao.app.Cenarios;
import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenacaoDFSTest {

    private final OrdenacaoDFS dfs = new OrdenacaoDFS();

    @Test
    void reproduzASaidaDoRoteiro() {
        ResultadoOrdenacao r = dfs.ordenar(Fixtures.exemploRoteiro());
        assertFalse(r.possuiCiclo());
        assertEquals(List.of("A", "C", "E", "B", "D"), r.ordem());
    }

    @Test
    void ordemEhOInversoDaFinalizacao() {
        ResultadoOrdenacao r = dfs.ordenar(Fixtures.exemploRoteiro());
        assertTrue(r.passos().contains("Ordem de finalização: [D, B, E, C, A]"));
    }

    @Test
    void semCicloTodosTerminamFinalizados() {
        dfs.ordenar(Fixtures.exemploRoteiro());
        assertTrue(dfs.estadosFinais().values().stream().allMatch(e -> e == OrdenacaoDFS.Estado.FINALIZADO));
    }

    @Test
    void detectaEApontaOCicloDoRoteiro() {
        ResultadoOrdenacao r = dfs.ordenar(Fixtures.cicloRoteiro());
        assertTrue(r.possuiCiclo());
        assertEquals(List.of("A", "B", "C", "A"), r.ciclo());
        assertEquals(List.of(), r.ordem());
    }

    @Test
    void noCicloOsVerticesDaPilhaFicamEmProcessamento() {
        dfs.ordenar(Fixtures.cicloRoteiro());
        assertEquals(OrdenacaoDFS.Estado.EM_PROCESSAMENTO, dfs.estadosFinais().get("A"));
        assertEquals(OrdenacaoDFS.Estado.EM_PROCESSAMENTO, dfs.estadosFinais().get("C"));
    }

    @Test
    void apontaSoOCicloENaoQuemDependeDele() {
        GrafoDisciplinas g = GrafoDisciplinas.de(Cenarios.comCiclo().disciplinas());
        ResultadoOrdenacao r = dfs.ordenar(g);
        assertTrue(r.possuiCiclo());
        assertEquals(List.of("ALG", "ED", "ES", "ALG"), r.ciclo());
    }

    @Test
    void cicloQueNaoComecaNoPrimeiroVertice() {
        // X → A → B → C → A: o ciclo não inclui X.
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("X", "X", 0),
                new Disciplina("A", "A", 0, "X", "C"),
                new Disciplina("B", "B", 0, "A"),
                new Disciplina("C", "C", 0, "B"));
        assertEquals(List.of("A", "B", "C", "A"), dfs.ordenar(g).ciclo());
    }

    @Test
    void lacoProprioEhCiclo() {
        ResultadoOrdenacao r = dfs.ordenar(GrafoDisciplinas.de(new Disciplina("A", "A", 0, "A")));
        assertEquals(List.of("A", "A"), r.ciclo());
    }

    @Test
    void arestaParaVerticeFinalizadoNaoEhCiclo() {
        // Losango A→B, A→C, B→D, C→D: D é alcançado duas vezes, sem ciclo.
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("A", "A", 0),
                new Disciplina("B", "B", 0, "A"),
                new Disciplina("C", "C", 0, "A"),
                new Disciplina("D", "D", 0, "B", "C"));
        ResultadoOrdenacao r = dfs.ordenar(g);
        assertFalse(r.possuiCiclo());
        assertTrue(g.ehOrdenacaoValida(r.ordem()));
    }

    @Test
    void grafoDesconexo() {
        GrafoDisciplinas g = GrafoDisciplinas.de(Cenarios.semCiclo().disciplinas());
        ResultadoOrdenacao r = dfs.ordenar(g);
        assertEquals(List.of("MAT", "PRG", "BD", "WEB", "ALG", "ED", "GRA", "ES", "PI"), r.ordem());
        assertTrue(g.ehOrdenacaoValida(r.ordem()));
    }

    @Test
    void cadeiaLongaNaoEstouraAPilha() {
        List<Disciplina> lista = new ArrayList<>();
        lista.add(new Disciplina("D0", "D0", 0));
        for (int i = 1; i < 1_000; i++) {
            lista.add(new Disciplina("D" + i, "D" + i, 0, "D" + (i - 1)));
        }
        GrafoDisciplinas g = GrafoDisciplinas.de(lista);
        ResultadoOrdenacao r = dfs.ordenar(g);
        assertTrue(g.ehOrdenacaoValida(r.ordem()));
    }
}
