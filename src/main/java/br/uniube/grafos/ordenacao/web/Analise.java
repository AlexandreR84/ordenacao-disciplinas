package br.uniube.grafos.ordenacao.web;

import br.uniube.grafos.ordenacao.algoritmo.ContagemOrdenacoes;
import br.uniube.grafos.ordenacao.algoritmo.OrdenacaoDFS;
import br.uniube.grafos.ordenacao.algoritmo.OrdenacaoKahn;
import br.uniube.grafos.ordenacao.algoritmo.ResultadoOrdenacao;
import br.uniube.grafos.ordenacao.algoritmo.TodasOrdenacoes;
import br.uniube.grafos.ordenacao.algoritmo.ValidadorPeriodos;
import br.uniube.grafos.ordenacao.app.Cenarios;
import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Faz a ponte entre a interface web e os algoritmos: recebe as disciplinas,
 * monta o grafo, roda Kahn, DFS, períodos, validação e ordenações, e devolve tudo
 * em mapas e listas prontos para virar JSON. Não tem lógica de grafo própria.
 */
public final class Analise {

    /** Quantas ordenações a página recebe para exibir. */
    public static final int ORDENACOES_ENVIADAS = 50;

    private Analise() { }

    /** Os cenários de demonstração, no formato que a página usa. */
    public static List<Map<String, Object>> cenarios() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Cenarios.Cenario c : Cenarios.todos()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("titulo", c.titulo());
            m.put("descricao", c.descricao());
            m.put("disciplinas", c.disciplinas().stream().map(Analise::disciplina).toList());
            lista.add(m);
        }
        return lista;
    }

    /**
     * Lê o corpo {@code {"disciplinas": [{"codigo", "nome", "periodo", "preRequisitos"}]}}.
     *
     * @throws IllegalArgumentException se o formato estiver errado
     */
    public static List<Disciplina> lerDisciplinas(Object json) {
        if (!(json instanceof Map<?, ?> corpo) || !(corpo.get("disciplinas") instanceof List<?> itens)) {
            throw new IllegalArgumentException("o corpo precisa ter a lista \"disciplinas\"");
        }
        List<Disciplina> lista = new ArrayList<>();
        for (Object item : itens) {
            if (!(item instanceof Map<?, ?> d)) {
                throw new IllegalArgumentException("cada disciplina precisa ser um objeto");
            }
            String codigo = texto(d.get("codigo"));
            String nome = texto(d.get("nome"));
            int periodo = 0;
            if (d.get("periodo") instanceof Number n) {
                if (n.doubleValue() != Math.rint(n.doubleValue())) {
                    throw new IllegalArgumentException("período precisa ser inteiro: " + codigo);
                }
                periodo = n.intValue();
            } else if (d.get("periodo") != null) {
                throw new IllegalArgumentException("período precisa ser um número: " + codigo);
            }
            List<String> pre = new ArrayList<>();
            if (d.get("preRequisitos") instanceof List<?> ps) {
                ps.forEach(p -> pre.add(texto(p)));
            }
            lista.add(new Disciplina(codigo, nome, periodo, pre));
        }
        return lista;
    }

    /** Roda tudo sobre as disciplinas e devolve o resultado para a página. */
    public static Map<String, Object> analisar(List<Disciplina> disciplinas) {
        GrafoDisciplinas g = GrafoDisciplinas.de(disciplinas);
        ResultadoOrdenacao kahn = new OrdenacaoKahn().ordenar(g);
        ResultadoOrdenacao dfs = new OrdenacaoDFS().ordenar(g);
        boolean ciclo = kahn.possuiCiclo() || dfs.possuiCiclo();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("v", g.numeroVertices());
        r.put("e", g.numeroArestas());
        r.put("disciplinas", g.vertices().stream().map(v -> disciplina(g.disciplina(v))).toList());

        List<Map<String, String>> arestas = new ArrayList<>();
        Map<String, Object> adjacencia = new LinkedHashMap<>();
        for (String v : g.vertices()) {
            adjacencia.put(v, List.copyOf(g.sucessores(v)));
            for (String w : g.sucessores(v)) {
                arestas.add(Map.of("de", v, "para", w));
            }
        }
        r.put("arestas", arestas);
        r.put("adjacencia", adjacencia);
        r.put("grausEntrada", g.grausDeEntrada());

        r.put("possuiCiclo", ciclo);
        r.put("kahn", resultado(kahn, g));
        r.put("dfs", resultado(dfs, g));
        r.put("mesmaSequencia", !ciclo && kahn.ordem().equals(dfs.ordem()));

        r.put("periodos", ciclo ? List.of() : new OrdenacaoKahn().periodos(g));
        r.put("inconsistencias", new ValidadorPeriodos().validar(g).stream().map(Object::toString).toList());
        r.put("temPeriodoInformado", disciplinas.stream().anyMatch(Disciplina::temPeriodoInformado));

        List<List<String>> todas = new TodasOrdenacoes().listar(g, ORDENACOES_ENVIADAS);
        Optional<BigInteger> total = new ContagemOrdenacoes().contar(g);
        Map<String, Object> ordenacoes = new LinkedHashMap<>();
        ordenacoes.put("exemplos", todas);
        // texto, porque números acima de 2^53 perdem precisão no JavaScript
        ordenacoes.put("total", total.map(BigInteger::toString).orElse(null));
        ordenacoes.put("componentes", ContagemOrdenacoes.componentes(g).size());
        r.put("ordenacoes", ordenacoes);
        return r;
    }

    private static Map<String, Object> resultado(ResultadoOrdenacao res, GrafoDisciplinas g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("algoritmo", res.algoritmo());
        m.put("ordem", res.ordem());
        m.put("possuiCiclo", res.possuiCiclo());
        m.put("ciclo", res.ciclo());
        m.put("pendentes", res.pendentes());
        m.put("passos", res.passos());
        m.put("valida", !res.possuiCiclo() && g.ehOrdenacaoValida(res.ordem()));
        return m;
    }

    private static Map<String, Object> disciplina(Disciplina d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", d.codigo());
        m.put("nome", d.nome());
        m.put("periodo", d.periodo());
        m.put("preRequisitos", d.preRequisitos());
        return m;
    }

    private static String texto(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof Double n && n == Math.rint(n)) {
            return String.valueOf(n.longValue()); // código numérico, como 12566
        }
        return o.toString();
    }
}
