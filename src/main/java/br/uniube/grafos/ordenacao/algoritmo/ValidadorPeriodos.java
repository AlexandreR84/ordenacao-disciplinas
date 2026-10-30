package br.uniube.grafos.ordenacao.algoritmo;

import br.uniube.grafos.ordenacao.grafo.GrafoDisciplinas;
import br.uniube.grafos.ordenacao.modelo.Disciplina;

import java.util.ArrayList;
import java.util.List;

/**
 * Confere o período cadastrado de cada disciplina contra seus pré-requisitos.
 *
 * <p>Há inconsistência quando um pré-requisito está cadastrado no mesmo período
 * da disciplina ou depois dela: o estudante não teria como cumprir a grade.
 * Disciplinas sem período informado não são conferidas. O custo é O(V + E):
 * cada aresta é olhada uma vez.</p>
 */
public class ValidadorPeriodos {

    public record Inconsistencia(String disciplina, int periodo, String preRequisito, int periodoPreRequisito) {
        @Override
        public String toString() {
            return disciplina + " (período " + periodo + ") exige " + preRequisito
                    + ", cadastrada no período " + periodoPreRequisito;
        }
    }

    public List<Inconsistencia> validar(GrafoDisciplinas grafo) {
        List<Inconsistencia> lista = new ArrayList<>();
        for (String v : grafo.vertices()) {
            Disciplina d = grafo.disciplina(v);
            if (!d.temPeriodoInformado()) {
                continue;
            }
            for (String pre : grafo.preRequisitos(v)) {
                Disciplina p = grafo.disciplina(pre);
                if (p.temPeriodoInformado() && p.periodo() >= d.periodo()) {
                    lista.add(new Inconsistencia(v, d.periodo(), pre, p.periodo()));
                }
            }
        }
        return lista;
    }
}
