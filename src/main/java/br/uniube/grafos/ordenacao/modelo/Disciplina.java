package br.uniube.grafos.ordenacao.modelo;

import java.util.List;
import java.util.Objects;

/**
 * Uma disciplina da grade. É um vértice do grafo.
 *
 * @param codigo        identificador único (ex.: "ALG")
 * @param nome          nome por extenso (ex.: "Algoritmos")
 * @param periodo       período em que a disciplina está cadastrada na grade;
 *                      0 quando não informado
 * @param preRequisitos códigos das disciplinas que precisam ser cursadas antes
 */
public record Disciplina(String codigo, String nome, int periodo, List<String> preRequisitos) {

    public static final int PERIODO_NAO_INFORMADO = 0;

    public Disciplina {
        Objects.requireNonNull(codigo, "código é obrigatório");
        Objects.requireNonNull(nome, "nome é obrigatório");
        codigo = codigo.trim().toUpperCase();
        nome = nome.trim();
        if (codigo.isEmpty()) {
            throw new IllegalArgumentException("código não pode ser vazio");
        }
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        if (periodo < 0) {
            throw new IllegalArgumentException("período não pode ser negativo: " + periodo);
        }
        preRequisitos = preRequisitos == null ? List.of()
                : preRequisitos.stream().map(p -> p.trim().toUpperCase()).filter(p -> !p.isEmpty()).toList();
    }

    public Disciplina(String codigo, String nome, int periodo, String... preRequisitos) {
        this(codigo, nome, periodo, List.of(preRequisitos));
    }

    public boolean temPeriodoInformado() {
        return periodo != PERIODO_NAO_INFORMADO;
    }

    @Override
    public String toString() {
        return codigo + " - " + nome;
    }
}
