package br.uniube.grafos.ordenacao.app;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleAppTest {

    private static String executar(String entrada, boolean demo) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        ConsoleApp app = new ConsoleApp(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)), out);
        if (demo) {
            app.demonstracao();
        } else {
            app.executarMenu();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void demonstracaoMostraOsTresCenarios() {
        String s = executar("", true);
        assertTrue(s.contains("Algoritmo Kahn: A → B → C → D → E"));
        assertTrue(s.contains("Algoritmo DFS: A → C → E → B → D"));
        assertTrue(s.contains("Ordenações possíveis: 6"));
        assertTrue(s.contains("Período 1: Algoritmos, Programação, Matemática Discreta"));
        assertTrue(s.contains("Ciclo: ALG → ED → ES → ALG"));
        assertTrue(s.contains("não existe ordenação topológica válida"));
    }

    @Test
    void cadastroPeloMenuEOrdenacao() {
        String entrada = String.join("\n",
                "1", "A", "Algoritmos", "1", "",
                "1", "B", "Estruturas de Dados", "2", "A",
                "6", "0") + "\n";
        String s = executar(entrada, false);
        assertTrue(s.contains("Cadastrada: B - Estruturas de Dados"));
        assertTrue(s.contains("Algoritmo Kahn: A → B"));
        assertTrue(s.contains("Algoritmo DFS: A → B"));
    }

    @Test
    void criarCicloPeloMenu() {
        String entrada = String.join("\n",
                "1", "A", "A", "", "",
                "1", "B", "B", "", "A",
                "2", "B", "A",
                "6", "0") + "\n";
        String s = executar(entrada, false);
        assertTrue(s.contains("Dependência adicionada: B → A"));
        assertTrue(s.contains("Ciclo: A → B → A"));
    }

    @Test
    void periodoInconsistenteEhAvisado() {
        String entrada = String.join("\n",
                "1", "A", "Algoritmos", "2", "",
                "1", "B", "Estruturas de Dados", "2", "A",
                "6", "0") + "\n";
        assertTrue(executar(entrada, false).contains("B (período 2) exige A, cadastrada no período 2"));
    }

    @Test
    void errosNaoDerrubamOMenu() {
        String entrada = String.join("\n",
                "4",                                   // nada cadastrado
                "1", "A", "A", "x", "",                // período inválido
                "1", "B", "B", "", "Z",                // pré-requisito inexistente: só falha ao montar o grafo
                "6",
                "99", "0") + "\n";
        String s = executar(entrada, false);
        assertTrue(s.contains("Erro: nenhuma disciplina cadastrada"));
        assertTrue(s.contains("Erro: For input string"));
        assertTrue(s.contains("Erro: pré-requisito Z de B não foi cadastrado"));
        assertTrue(s.contains("Opção inválida."));
        assertTrue(s.contains("Até mais!"));
    }

    @Test
    void carregarCenarioPeloMenu() {
        String s = executar("8\n2\n5\n0\n", false);
        assertTrue(s.contains("Carregado: Cenário 2"));
        assertTrue(s.contains("Ordem inversa de finalização (ordenação topológica): [A, C, E, B, D]"));
    }
}
