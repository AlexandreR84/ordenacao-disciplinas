package br.uniube.grafos.ordenacao.grafo;

import br.uniube.grafos.ordenacao.Fixtures;
import br.uniube.grafos.ordenacao.modelo.Disciplina;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrafoDisciplinasTest {

    @Test
    void arestaSaiDoPreRequisitoEChegaNaDisciplina() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("ALG", "Algoritmos", 1),
                new Disciplina("ED", "Estruturas de Dados", 2, "ALG"));
        assertEquals(Set.of("ED"), g.sucessores("ALG"));
        assertEquals(Set.of(), g.sucessores("ED"));
        assertEquals(Set.of("ALG"), g.preRequisitos("ED"));
        assertEquals(List.of("ALG -> ED"), g.arestas());
    }

    @Test
    void contaVerticesEArestasDoExemploDoRoteiro() {
        GrafoDisciplinas g = Fixtures.exemploRoteiro();
        assertEquals(5, g.numeroVertices());
        assertEquals(4, g.numeroArestas());
        assertEquals(List.of("A", "B", "C", "D", "E"), g.vertices());
    }

    @Test
    void grausDeEntrada() {
        Map<String, Integer> grau = Fixtures.exemploRoteiro().grausDeEntrada();
        assertEquals(Map.of("A", 0, "B", 1, "C", 1, "D", 1, "E", 1), grau);
    }

    @Test
    void preRequisitoRepetidoViraUmaArestaSo() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("A", "A", 0),
                new Disciplina("B", "B", 0, "A", "A", "a"));
        assertEquals(1, g.numeroArestas());
        assertEquals(1, g.grausDeEntrada().get("B"));
    }

    @Test
    void preRequisitoNaoCadastradoEhRecusado() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GrafoDisciplinas.de(new Disciplina("B", "B", 0, "X")));
        assertTrue(e.getMessage().contains("X"));
    }

    @Test
    void codigoRepetidoEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> GrafoDisciplinas.de(
                new Disciplina("A", "Um", 0), new Disciplina("a", "Outro", 0)));
    }

    @Test
    void preRequisitoPodeSerCadastradoDepoisDaDisciplina() {
        GrafoDisciplinas g = GrafoDisciplinas.de(
                new Disciplina("B", "B", 0, "A"),
                new Disciplina("A", "A", 0));
        assertEquals(Set.of("B"), g.sucessores("A"));
    }

    @Test
    void lacoProprioEhUmaAresta() {
        GrafoDisciplinas g = GrafoDisciplinas.de(new Disciplina("A", "A", 0, "A"));
        assertEquals(1, g.numeroArestas());
        assertEquals(1, g.grausDeEntrada().get("A"));
    }

    @Test
    void validaOrdenacaoTopologica() {
        GrafoDisciplinas g = Fixtures.exemploRoteiro();
        assertTrue(g.ehOrdenacaoValida(List.of("A", "B", "C", "D", "E")));
        assertTrue(g.ehOrdenacaoValida(List.of("A", "C", "E", "B", "D")));
        assertFalse(g.ehOrdenacaoValida(List.of("B", "A", "C", "D", "E")), "B antes de A");
        assertFalse(g.ehOrdenacaoValida(List.of("A", "B", "C", "D")), "falta E");
        assertFalse(g.ehOrdenacaoValida(List.of("A", "B", "C", "D", "D")), "D repetido");
    }

    @Test
    void disciplinaNormalizaCodigoEValidaCampos() {
        Disciplina d = new Disciplina(" alg ", " Algoritmos ", 1, " ed ");
        assertEquals("ALG", d.codigo());
        assertEquals("Algoritmos", d.nome());
        assertEquals(List.of("ED"), d.preRequisitos());
        assertThrows(IllegalArgumentException.class, () -> new Disciplina("", "X", 0));
        assertThrows(IllegalArgumentException.class, () -> new Disciplina("X", " ", 0));
        assertThrows(IllegalArgumentException.class, () -> new Disciplina("X", "X", -1));
    }
}
