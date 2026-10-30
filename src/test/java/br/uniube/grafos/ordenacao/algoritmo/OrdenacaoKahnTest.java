package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.Fixtures;
import br.uniube.grafos.ordenacao.app.Cenarios;
import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenacaoKahnTest {

    private final OrdenacaoKahn kahn = new OrdenacaoKahn();

    @Test
    void reproduzASaidaDoRoteiro() {
        ResultadoOrdenacao r = kahn.ordenar(Fixtures.exemploRoteiro());
        assertFalse(r.possuiCiclo());
        assertEquals(List.of("A", "B", "C", "D", "E"), r.ordem());
        assertEquals("A → B → C → D → E", r.ordemFormatada());
    }

    @Test
    void detectaOCicloDoRoteiro() {
        ResultadoOrdenacao r = kahn.ordenar(Fixtures.cicloRoteiro());
        assertTrue(r.possuiCiclo());
        assertEquals(List.of(), r.ordem(), "nenhum vértice começa com grau zero");
        assertEquals(List.of("A", "B", "C"), r.pendentes());
    }

    @Test
    void comCicloOrdenaOQueConsegueEApontaOsPendentes() {
        GrafoDisciplinas g = GrafoDisciplinas.de(Cenarios.comCiclo().disciplinas());
        ResultadoOrdenacao r = kahn.ordenar(g);
        assertTrue(r.possuiCiclo());
        assertEquals(List.of("MAT"), r.ordem());
        // PI não está no ciclo, mas depende dele: Kahn não distingue.
        assertEquals(List.of("ALG", "ED", "ES", "PI"), r.pendentes());
        assertEquals(List.of(), r.ciclo(), "Kahn não reconstrói o ciclo");
    }

    @Test
    void lacoProprioEhCiclo() {
        ResultadoOrdenacao r = kahn.ordenar(GrafoDisciplinas.de(new Disciplina("A", "A", 0, "A")));
        assertTrue(r.possuiCiclo());
    }

    @Test
    void grafoVazio() {
        ResultadoOrdenacao r = kahn.ordenar(GrafoDisciplinas.de(List.of()));
        assertFalse(r.possuiCiclo());
        assertEquals(List.of(), r.ordem());
    }

    @Test
    void verticesSemArestasSaemNaOrdemDeCadastro() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("Z", "Z", 0), new Disciplina("A", "A", 0), new Disciplina("M", "M", 0));
        assertEquals(List.of("Z", "A", "M"), kahn.ordenar(g).ordem());
    }

    @Test
    void registraPassos() {
        ResultadoOrdenacao r = kahn.ordenar(Fixtures.exemploRoteiro());
        assertTrue(r.passos().get(1).contains("[A]"), "fila inicial só com A");
        assertTrue(r.passos().stream().anyMatch(p -> p.startsWith("Retira A")));
    }

    @Test
    void periodosDoExemploDoRoteiro() {
        assertEquals(List.of(List.of("A"), List.of("B", "C"), List.of("D", "E")),
                kahn.periodos(Fixtures.exemploRoteiro()));
    }

    @Test
    void periodosDaGradeDoCenario1() {
        GrafoDisciplinas g = GrafoDisciplinas.de(Cenarios.semCiclo().disciplinas());
        assertEquals(List.of(
                List.of("ALG", "PRG", "MAT"),
                List.of("ED", "BD"),
                List.of("ES", "GRA", "WEB"),
                List.of("PI")), kahn.periodos(g));
    }

    @Test
    void gradeRealDoCenario4() {
        GrafoDisciplinas g = GrafoDisciplinas.de(Cenarios.gradeReal().disciplinas());
        assertEquals(20, g.numeroVertices());
        assertEquals(17, g.numeroArestas());
        assertTrue(g.ehOrdenacaoValida(kahn.ordenar(g).ordem()));
        assertTrue(g.ehOrdenacaoValida(new OrdenacaoDFS().ordenar(g).ordem()));
        List<List<String>> periodos = kahn.periodos(g);
        assertEquals(5, periodos.size());
        assertEquals(List.of("12566", "90539", "49293", "49291", "49304"), periodos.get(0));
        assertEquals(List.of("49314"), periodos.get(4), "Projetos de Software II fecha a cadeia mais longa");
    }

    @Test
    void periodosComCicloLancamExcecao() {
        assertThrows(IllegalStateException.class, () -> kahn.periodos(Fixtures.cicloRoteiro()));
    }

    @Test
    void periodoDependeDoCaminhoMaisLongo() {
        // A→B→C e A→C: C fica no período 3, não no 2.
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("A", "A", 0),
                new Disciplina("B", "B", 0, "A"),
                new Disciplina("C", "C", 0, "A", "B"));
        assertEquals(List.of(List.of("A"), List.of("B"), List.of("C")), kahn.periodos(g));
    }
}
