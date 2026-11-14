package br.uniube.grafos.ordenacao.app;

import br.uniube.grafos.ordenacao.modelo.Disciplina;

import java.util.List;

/** Os cenários de demonstração: os três pedidos no roteiro e a grade real do curso. */
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
     * Cenário 4: a grade completa de Sistemas de Informação da Uniube (curso 160, currículo 1),
     * tirada do currículo individual do aluno: os 59 itens, com código, nome e o semestre
     * oficial (1 a 8; o ENADE não tem semestre). O currículo não informa pré-requisitos.
     * Por isso as dependências entre disciplinas "I" e "II" seguem a sequência do próprio nome,
     * e as demais são SUPOSTAS para a demonstração, não a regra oficial do curso. Todas as
     * supostas respeitam os semestres oficiais (o pré-requisito vem num semestre anterior).
     */
    public static Cenario gradeReal() {
        return new Cenario(
                "Cenário 4: grade completa de Sistemas de Informação (Uniube)",
                "Os 59 itens do currículo, com código, nome e semestre oficial. Pré-requisitos: "
                        + "I → II pela sequência do nome; os demais são supostos para a demonstração.",
                List.of(
                        // 1º semestre
                        new Disciplina("49288", "Arquitetura e Organização de Computadores", 1),
                        new Disciplina("49289", "Interface Homem-Máquina", 1),
                        new Disciplina("49329", "Fundamentos de Tecnologia da Informação", 1),
                        new Disciplina("63625", "Comportamento Empreendedor", 1),
                        new Disciplina("90534", "Tecnologias Digitais Emergentes", 1),
                        new Disciplina("90535", "Estatística Aplicada", 1),
                        new Disciplina("90538", "Álgebra Linear e Geometria Analítica", 1),
                        new Disciplina("90539", "Algoritmos e Estruturas de Dados", 1),
                        new Disciplina("99643", "Leitura e Produção de Textos Acadêmicos", 1),
                        // 2º semestre
                        new Disciplina("12566", "Linguagem e Técnicas de Programação", 2),
                        new Disciplina("49291", "Banco de Dados I", 2),
                        new Disciplina("49293", "Matemática Discreta I", 2),
                        new Disciplina("49299", "Teoria Geral dos Sistemas", 2),
                        new Disciplina("49323", "Laboratório de Programação", 2, "90539"),  // suposto
                        new Disciplina("49327", "Sistemas Distribuídos e de Tempo Real", 2, "90539"),  // suposto
                        new Disciplina("90562", "Direito Digital", 2),
                        new Disciplina("99652", "Educação Financeira", 2),
                        // 3º semestre
                        new Disciplina("49295", "Banco de Dados II", 3, "49291"),  // I → II
                        new Disciplina("49296", "Matemática Discreta II", 3, "49293"),  // I → II
                        new Disciplina("49297", "Programação Orientada a Objetos", 3, "12566"),  // suposto
                        new Disciplina("49298", "Tecnologias para a Internet I", 3, "12566"),  // suposto
                        new Disciplina("49326", "Laboratório de Programação Competitiva", 3, "12566", "49323"),  // suposto
                        new Disciplina("99644", "Cidadania: Heterogeneidade e Diversidade", 3),
                        // 4º semestre
                        new Disciplina("49301", "Engenharia de Software", 4, "49297"),  // suposto
                        new Disciplina("49302", "Estrutura de Dados I", 4, "90539"),  // suposto
                        new Disciplina("49303", "Processamento de Imagens", 4, "90538"),  // suposto
                        new Disciplina("49305", "Tecnologias para a Internet II", 4, "49298"),  // I → II
                        new Disciplina("49308", "Projetos de Software I", 4, "49297"),  // suposto
                        new Disciplina("49325", "Administração de Sistemas Operacionais de Redes I", 4, "49288"),  // suposto
                        new Disciplina("90548", "Gestão de Projetos", 4, "49299"),  // suposto
                        new Disciplina("99642", "Felicidade e Bem-Estar", 4),
                        // 5º semestre
                        new Disciplina("49304", "Redes de Computadores I", 5),
                        new Disciplina("49306", "Estrutura de Dados II", 5, "49302"),  // I → II
                        new Disciplina("49307", "Linguagem de Programação para Internet", 5, "49298"),  // suposto
                        new Disciplina("49311", "Desenvolvimento para Dispositivos Móveis", 5, "49297"),  // suposto
                        new Disciplina("49314", "Projetos de Software II", 5, "49308"),  // I → II
                        new Disciplina("63624", "Contabilidade Básica", 5, "99652"),  // suposto
                        new Disciplina("99655", "Atividades Complementares", 5),
                        // 6º semestre
                        new Disciplina("38225", "Indústria 4.0", 6),
                        new Disciplina("49309", "Redes de Computadores II", 6, "49304"),  // I → II
                        new Disciplina("49310", "Algoritmos em Grafos", 6, "49296", "49306"),  // suposto
                        new Disciplina("49312", "Inteligência Artificial", 6, "90535", "90539"),  // suposto
                        new Disciplina("49315", "Teoria da Computação", 6, "49296"),  // suposto
                        new Disciplina("63636", "Marketing", 6, "63625"),  // suposto
                        new Disciplina("99648", "Extensão", 6),
                        // 7º semestre
                        new Disciplina("49313", "Projetos Integrados I", 7, "49301"),  // suposto
                        new Disciplina("49316", "Governança e Estratégia Corporativa", 7, "49299"),  // suposto
                        new Disciplina("49317", "Segurança e Auditoria de Sistemas de Informação", 7, "49309"),  // suposto
                        new Disciplina("49320", "Aplicações para a Internet", 7, "49295", "49305"),  // suposto
                        new Disciplina("63627", "Ética Empresarial", 7, "63625"),  // suposto
                        new Disciplina("90546", "Responsabilidade Socioambiental", 7),
                        new Disciplina("90555", "Fundamentos de Administração e Economia", 7, "63624"),  // suposto
                        new Disciplina("99649", "Extensão", 7),
                        // 8º semestre
                        new Disciplina("49318", "Projetos Integrados II", 8, "49313"),  // I → II
                        new Disciplina("49319", "Administração de Sistemas Operacionais de Redes II", 8,
                                "49325", "49309"),  // I → II e suposto
                        new Disciplina("49321", "Sistemas Operacionais", 8, "49288"),  // suposto
                        new Disciplina("49328", "Ciência de Dados", 8, "90535", "49295"),  // suposto
                        new Disciplina("63637", "Sistemas de Informações Gerenciais", 8, "49329", "49299"),  // suposto
                        // sem semestre na grade
                        new Disciplina("99407", "ENADE (obrigatório)", 0)));
    }

    public static List<Cenario> todos() {
        return List.of(semCiclo(), multiplasOrdenacoes(), comCiclo(), gradeReal());
    }
}
