package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.Fixtures;
import br.uniube.grafos.ordenacao.app.Cenarios;
import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodasOrdenacoesTest {

    private final TodasOrdenacoes todas = new TodasOrdenacoes();

    @Test
    void exemploDoRoteiroTemSeisOrdenacoes() {
        List<List<String>> lista = todas.listar(Fixtures.exemploRoteiro());
        assertEquals(List.of(
                List.of("A", "B", "C", "D", "E"),
                List.of("A", "B", "C", "E", "D"),
                List.of("A", "B", "D", "C", "E"),
                List.of("A", "C", "B", "D", "E"),
                List.of("A", "C", "B", "E", "D"),
                List.of("A", "C", "E", "B", "D")), lista);
    }

    @Test
    void asSaidasDeKahnEDfsEstaoEntreElas() {
        GrafoDisciplinas g = Fixtures.exemploRoteiro();
        List<List<String>> lista = todas.listar(g);
        assertTrue(lista.contains(new OrdenacaoKahn().ordenar(g).ordem()));
        assertTrue(lista.contains(new OrdenacaoDFS().ordenar(g).ordem()));
    }

    @Test
    void cenario1TemQuinhentasECinquentaEDuas() {
        GrafoDisciplinas g = GrafoDisciplinas.de(Cenarios.semCiclo().disciplinas());
        List<List<String>> lista = todas.listar(g);
        assertEquals(552, lista.size());
        assertEquals(552, new HashSet<>(lista).size(), "sem repetidas");
        assertTrue(lista.stream().allMatch(g::ehOrdenacaoValida));
    }

    @Test
    void cadeiaTemUmaSo() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("A", "A", 0),
                new Disciplina("B", "B", 0, "A"),
                new Disciplina("C", "C", 0, "B"));
        assertEquals(List.of(List.of("A", "B", "C")), todas.listar(g));
    }

    @Test
    void semArestasSaoNFatorial() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("A", "A", 0), new Disciplina("B", "B", 0),
                new Disciplina("C", "C", 0), new Disciplina("D", "D", 0));
        assertEquals(24, todas.listar(g).size());
    }

    @Test
    void comCicloNaoHaNenhuma() {
        assertEquals(List.of(), todas.listar(Fixtures.cicloRoteiro()));
    }

    @Test
    void cicloEmGrafoGrandeRespondeNaHora() {
        // 30 vértices independentes + um ciclo de 2. Sem a checagem prévia,
        // o backtracking testaria cerca de 30! ordens parciais antes de desistir.
        List<Disciplina> lista = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            lista.add(new Disciplina("L" + i, "L" + i, 0));
        }
        lista.add(new Disciplina("X", "X", 0, "Y"));
        lista.add(new Disciplina("Y", "Y", 0, "X"));
        assertEquals(List.of(), todas.listar(GrafoDisciplinas.de(lista), 1));
    }

    @Test
    void respeitaOLimite() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("A", "A", 0), new Disciplina("B", "B", 0),
                new Disciplina("C", "C", 0), new Disciplina("D", "D", 0));
        assertEquals(5, todas.listar(g, 5).size());
    }
}
