package br.uniube.grafos.ordenacao.app;

import br.uniube.grafos.ordenacao.algoritmo.OrdenacaoDFS;
import br.uniube.grafos.ordenacao.algoritmo.OrdenacaoKahn;
import br.uniube.grafos.ordenacao.algoritmo.ResultadoOrdenacao;
import br.uniube.grafos.ordenacao.algoritmo.TodasOrdenacoes;
import br.uniube.grafos.ordenacao.algoritmo.ValidadorPeriodos;
import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Aplicação de console: cadastro de disciplinas, ordenação por Kahn e DFS,
 * comparação, períodos e os três cenários de demonstração.
 *
 * <p>Uso: {@code java -jar ordenacao-disciplinas.jar} (menu) ou
 * {@code java -jar ordenacao-disciplinas.jar --demo} (roda os três cenários).</p>
 */
public class ConsoleApp {

    private static final int MAX_ORDENACOES_EXIBIDAS = 10;

    private final Scanner entrada;
    private final PrintStream saida;
    private final Map<String, Disciplina> cadastro = new LinkedHashMap<>();

    public ConsoleApp(InputStream in, PrintStream out) {
        this.entrada = new Scanner(in, StandardCharsets.UTF_8);
        this.saida = out;
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        ConsoleApp app = new ConsoleApp(System.in, out);
        if (args.length > 0 && args[0].equals("--demo")) {
            app.demonstracao();
        } else {
            app.executarMenu();
        }
    }

    // ------------------------------------------------------------------ menu

    public void executarMenu() {
        saida.println("Planejamento Inteligente da Oferta de Disciplinas");
        while (true) {
            saida.println();
            saida.println("1 - Cadastrar disciplina");
            saida.println("2 - Adicionar dependência (pré-requisito → disciplina)");
            saida.println("3 - Mostrar disciplinas e grafo");
            saida.println("4 - Ordenar com Kahn (passo a passo)");
            saida.println("5 - Ordenar com DFS (passo a passo)");
            saida.println("6 - Comparar Kahn e DFS e montar os períodos");
            saida.println("7 - Listar todas as ordenações possíveis");
            saida.println("8 - Carregar cenário de demonstração");
            saida.println("9 - Limpar cadastro");
            saida.println("0 - Sair");
            String opcao = ler("Opção: ");
            if (opcao == null || opcao.equals("0")) {
                saida.println("Até mais!");
                return;
            }
            try {
                switch (opcao) {
                    case "1" -> cadastrarDisciplina();
                    case "2" -> adicionarDependencia();
                    case "3" -> mostrarGrafo(montarGrafo());
                    case "4" -> imprimirResultado(new OrdenacaoKahn().ordenar(montarGrafo()), true);
                    case "5" -> imprimirResultado(new OrdenacaoDFS().ordenar(montarGrafo()), true);
                    case "6" -> comparar(montarGrafo());
                    case "7" -> listarTodas(montarGrafo());
                    case "8" -> carregarCenario();
                    case "9" -> {
                        cadastro.clear();
                        saida.println("Cadastro limpo.");
                    }
                    default -> saida.println("Opção inválida.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                saida.println("Erro: " + e.getMessage());
            }
        }
    }

    private void cadastrarDisciplina() {
        String codigo = ler("Código: ");
        String nome = ler("Nome: ");
        String textoPeriodo = ler("Período na grade (Enter se não houver): ");
        String textoPre = ler("Pré-requisitos, separados por vírgula (Enter se não houver): ");
        int periodo = textoPeriodo == null || textoPeriodo.isBlank() ? 0 : Integer.parseInt(textoPeriodo.trim());
        List<String> pre = textoPre == null || textoPre.isBlank() ? List.of() : Arrays.asList(textoPre.split(","));
        Disciplina d = new Disciplina(codigo == null ? "" : codigo, nome == null ? "" : nome, periodo, pre);
        if (cadastro.containsKey(d.codigo())) {
            throw new IllegalArgumentException("já existe disciplina com o código " + d.codigo());
        }
        cadastro.put(d.codigo(), d);
        saida.println("Cadastrada: " + d);
    }

    private void adicionarDependencia() {
        String pre = textoMaiusculo(ler("Pré-requisito (código): "));
        String disc = textoMaiusculo(ler("Disciplina que depende dele (código): "));
        Disciplina d = cadastro.get(disc);
        if (d == null) {
            throw new IllegalArgumentException("disciplina não cadastrada: " + disc);
        }
        if (!cadastro.containsKey(pre)) {
            throw new IllegalArgumentException("disciplina não cadastrada: " + pre);
        }
        List<String> novos = new ArrayList<>(d.preRequisitos());
        if (!novos.contains(pre)) {
            novos.add(pre);
        }
        cadastro.put(disc, new Disciplina(d.codigo(), d.nome(), d.periodo(), novos));
        saida.println("Dependência adicionada: " + pre + " → " + disc);
    }

    private void carregarCenario() {
        List<Cenarios.Cenario> cenarios = Cenarios.todos();
        for (int i = 0; i < cenarios.size(); i++) {
            saida.println((i + 1) + " - " + cenarios.get(i).titulo());
        }
        String escolha = ler("Cenário: ");
        int i = Integer.parseInt(escolha == null ? "" : escolha.trim()) - 1;
        if (i < 0 || i >= cenarios.size()) {
            throw new IllegalArgumentException("cenário inexistente");
        }
        cadastro.clear();
        cenarios.get(i).disciplinas().forEach(d -> cadastro.put(d.codigo(), d));
        saida.println("Carregado: " + cenarios.get(i).titulo());
    }

    // --------------------------------------------------------- demonstração

    /** Roda os três cenários do roteiro, um após o outro. */
    public void demonstracao() {
        for (Cenarios.Cenario c : Cenarios.todos()) {
            saida.println("=".repeat(78));
            saida.println(c.titulo());
            saida.println(c.descricao());
            saida.println("=".repeat(78));
            GrafoDisciplinas g = GrafoDisciplinas.de(c.disciplinas());
            mostrarGrafo(g);
            comparar(g);
            listarTodas(g);
            saida.println();
        }
    }

    // ------------------------------------------------------------- análises

    private GrafoDisciplinas montarGrafo() {
        if (cadastro.isEmpty()) {
            throw new IllegalStateException("nenhuma disciplina cadastrada (use 1 ou 8)");
        }
        return GrafoDisciplinas.de(cadastro.values());
    }

    private void mostrarGrafo(GrafoDisciplinas g) {
        saida.println("Disciplinas (V = " + g.numeroVertices() + "):");
        for (String v : g.vertices()) {
            Disciplina d = g.disciplina(v);
            String periodo = d.temPeriodoInformado() ? " [período " + d.periodo() + "]" : "";
            saida.println("  " + d + periodo);
        }
        saida.println("Dependências (E = " + g.numeroArestas() + "):");
        if (g.numeroArestas() == 0) {
            saida.println("  (nenhuma)");
        }
        g.arestas().forEach(a -> saida.println("  " + a));
        saida.println("Lista de adjacência:");
        for (String v : g.vertices()) {
            saida.println("  " + v + " → " + g.sucessores(v));
        }
    }

    private void comparar(GrafoDisciplinas g) {
        ResultadoOrdenacao kahn = new OrdenacaoKahn().ordenar(g);
        ResultadoOrdenacao dfs = new OrdenacaoDFS().ordenar(g);
        saida.println();
        imprimirResultado(kahn, false);
        imprimirResultado(dfs, false);
        if (kahn.possuiCiclo() || dfs.possuiCiclo()) {
            saida.println("Conclusão: não existe ordenação topológica válida (dependência circular).");
            return;
        }
        saida.println("Kahn válida? " + simNao(g.ehOrdenacaoValida(kahn.ordem()))
                + " | DFS válida? " + simNao(g.ehOrdenacaoValida(dfs.ordem()))
                + " | Mesma sequência? " + simNao(kahn.ordem().equals(dfs.ordem())));

        List<List<String>> periodos = new OrdenacaoKahn().periodos(g);
        saida.println();
        saida.println("Períodos (disciplinas que podem ser cursadas ao mesmo tempo):");
        for (int i = 0; i < periodos.size(); i++) {
            List<String> nomes = periodos.get(i).stream().map(v -> g.disciplina(v).nome()).toList();
            saida.println("  Período " + (i + 1) + ": " + String.join(", ", nomes));
        }
        List<ValidadorPeriodos.Inconsistencia> problemas = new ValidadorPeriodos().validar(g);
        boolean algumPeriodo = g.vertices().stream().anyMatch(v -> g.disciplina(v).temPeriodoInformado());
        if (algumPeriodo) {
            if (problemas.isEmpty()) {
                saida.println("Períodos cadastrados: coerentes com os pré-requisitos.");
            } else {
                saida.println("Períodos cadastrados com inconsistência:");
                problemas.forEach(p -> saida.println("  " + p));
            }
        }
    }

    private void imprimirResultado(ResultadoOrdenacao r, boolean comPassos) {
        if (comPassos) {
            saida.println("Passo a passo (" + r.algoritmo() + "):");
            r.passos().forEach(p -> saida.println("  " + p));
        }
        if (!r.possuiCiclo()) {
            saida.println("Algoritmo " + r.algoritmo() + ": " + r.ordemFormatada());
            return;
        }
        StringBuilder sb = new StringBuilder("Algoritmo " + r.algoritmo() + ": CICLO DETECTADO");
        if (!r.ciclo().isEmpty()) {
            sb.append(". Ciclo: ").append(String.join(" → ", r.ciclo()));
        }
        if (!r.ordem().isEmpty()) {
            sb.append(". Conseguiu ordenar: ").append(r.ordemFormatada());
        }
        if (!r.pendentes().isEmpty()) {
            sb.append(". Ficaram com grau de entrada positivo: ").append(r.pendentes());
        }
        saida.println(sb);
    }

    private void listarTodas(GrafoDisciplinas g) {
        List<List<String>> todas = new TodasOrdenacoes().listar(g);
        saida.println();
        if (todas.isEmpty()) {
            saida.println("Ordenações possíveis: nenhuma.");
            return;
        }
        String total = todas.size() >= TodasOrdenacoes.LIMITE_PADRAO
                ? "pelo menos " + todas.size() : String.valueOf(todas.size());
        saida.println("Ordenações possíveis: " + total);
        todas.stream().limit(MAX_ORDENACOES_EXIBIDAS)
                .forEach(o -> saida.println("  " + String.join(" → ", o)));
        if (todas.size() > MAX_ORDENACOES_EXIBIDAS) {
            saida.println("  ... (exibindo " + MAX_ORDENACOES_EXIBIDAS + ")");
        }
    }

    // ------------------------------------------------------------ utilidades

    private String ler(String rotulo) {
        saida.print(rotulo);
        return entrada.hasNextLine() ? entrada.nextLine() : null;
    }

    private static String textoMaiusculo(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private static String simNao(boolean b) {
        return b ? "sim" : "não";
    }
}
