package br.uniube.grafos.ordenacao.app;

import br.uniube.grafos.ordenacao.modelo.Disciplina;

import java.util.List;

/** Os três cenários de demonstração pedidos no roteiro. */
public final class Cenarios {

    public record Cenario(String titulo, String descricao, List<Disciplina> disciplinas) { }

    private Cenarios() { }

    /**
     * Cenário 1: grafo sem ciclos. Grade com as dependências do roteiro
     * (Algoritmos → Estruturas de Dados → Engenharia de Software;
     * Programação → Banco de Dados → Desenvolvimento Web), mais Matemática Discreta,
     * Algoritmos em Grafos e um Projeto Integrador que fecha a grade.
     */
    public static Cenario semCiclo() {
        return new Cenario(
                "Cenário 1: grafo sem ciclos (grade de exemplo)",
                "Grade com as dependências do roteiro, ampliada com Matemática Discreta, "
                        + "Algoritmos em Grafos e Projeto Integrador.",
                List.of(
                        new Disciplina("ALG", "Algoritmos", 1),
                        new Disciplina("PRG", "Programação", 1),
                        new Disciplina("MAT", "Matemática Discreta", 1),
                        new Disciplina("ED", "Estruturas de Dados", 2, "ALG"),
                        new Disciplina("BD", "Banco de Dados", 2, "PRG"),
                        new Disciplina("ES", "Engenharia de Software", 3, "ED"),
                        new Disciplina("WEB", "Desenvolvimento Web", 3, "BD"),
                        new Disciplina("GRA", "Algoritmos em Grafos", 3, "ED", "MAT"),
                        new Disciplina("PI", "Projeto Integrador", 4, "ES", "WEB")));
    }

    /** Cenário 2: o exemplo de entrada do roteiro, que admite várias ordenações. */
    public static Cenario multiplasOrdenacoes() {
        return new Cenario(
                "Cenário 2: grafo com múltiplas ordenações (exemplo do roteiro)",
                "Depois de A, as cadeias B → D e C → E são independentes e podem se intercalar.",
                List.of(
                        new Disciplina("A", "Algoritmos", 0),
                        new Disciplina("B", "Estruturas de Dados", 0, "A"),
                        new Disciplina("C", "Banco de Dados", 0, "A"),
                        new Disciplina("D", "Engenharia de Software", 0, "B"),
                        new Disciplina("E", "Desenvolvimento Web", 0, "C")));
    }

    /**
     * Cenário 3: grafo com ciclo. Algoritmos → Estruturas de Dados → Engenharia de Software
     * → Algoritmos forma a dependência circular A → B → C → A do roteiro. Projeto Integrador
     * depende do ciclo sem fazer parte dele, e Matemática Discreta é independente.
     */
    public static Cenario comCiclo() {
        return new Cenario(
                "Cenário 3: grafo com ciclo",
                "ALG → ED → ES → ALG é uma dependência circular; PI depende de ES; MAT é independente.",
                List.of(
                        new Disciplina("ALG", "Algoritmos", 0, "ES"),
                        new Disciplina("ED", "Estruturas de Dados", 0, "ALG"),
                        new Disciplina("ES", "Engenharia de Software", 0, "ED"),
                        new Disciplina("PI", "Projeto Integrador", 0, "ES"),
                        new Disciplina("MAT", "Matemática Discreta", 0)));
    }

    /**
     * Cenário 4: disciplinas reais do curso de Sistemas de Informação da Uniube, com os
     * códigos do currículo. O currículo não informa pré-requisitos nem período. Por isso:
     * as dependências entre disciplinas "I" e "II" seguem a sequência do próprio nome, e as
     * demais são SUPOSTAS para a demonstração, não a regra oficial do curso.
     */
    public static Cenario gradeReal() {
        return new Cenario(
                "Cenário 4: disciplinas reais de Sistemas de Informação (Uniube)",
                "Códigos e nomes do currículo do curso. Pré-requisitos: I → II pela sequência do nome; "
                        + "os demais são supostos para a demonstração.",
                List.of(
                        // trilha de programação
                        new Disciplina("12566", "Linguagem e Técnicas de Programação", 0),
                        new Disciplina("90539", "Algoritmos e Estruturas de Dados", 0),
                        new Disciplina("49297", "Programação Orientada a Objetos", 0, "12566"),      // suposto
                        new Disciplina("49302", "Estrutura de Dados I", 0, "90539"),                 // suposto
                        new Disciplina("49306", "Estrutura de Dados II", 0, "49302"),                // sequência I → II
                        // trilha de matemática e teoria
                        new Disciplina("49293", "Matemática Discreta I", 0),
                        new Disciplina("49296", "Matemática Discreta II", 0, "49293"),               // sequência I → II
                        new Disciplina("49310", "Algoritmos em Grafos", 0, "49306", "49296"),        // suposto
                        new Disciplina("49315", "Teoria da Computação", 0, "49296"),                 // suposto
                        // trilha de dados
                        new Disciplina("49291", "Banco de Dados I", 0),
                        new Disciplina("49295", "Banco de Dados II", 0, "49291"),                    // sequência I → II
                        new Disciplina("49328", "Ciência de Dados", 0, "49295"),                     // suposto
                        // trilha de internet
                        new Disciplina("49298", "Tecnologias para a Internet I", 0, "12566"),        // suposto
                        new Disciplina("49305", "Tecnologias para a Internet II", 0, "49298"),       // sequência I → II
                        new Disciplina("49320", "Aplicações para a Internet", 0, "49305", "49295"),  // suposto
                        // trilha de engenharia de software
                        new Disciplina("49301", "Engenharia de Software", 0, "49297"),               // suposto
                        new Disciplina("49308", "Projetos de Software I", 0, "49301"),               // suposto
                        new Disciplina("49314", "Projetos de Software II", 0, "49308"),              // sequência I → II
                        // trilha de redes
                        new Disciplina("49304", "Redes de Computadores I", 0),
                        new Disciplina("49309", "Redes de Computadores II", 0, "49304")));           // sequência I → II
    }

    public static List<Cenario> todos() {
        return List.of(semCiclo(), multiplasOrdenacoes(), comCiclo(), gradeReal());
    }
}
