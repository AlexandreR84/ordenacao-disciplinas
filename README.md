# Planejamento Inteligente da Oferta de Disciplinas

Autores: Alexandre Rezende Silva e Lucas dos Santos Pereira Martins (Sistemas de Informação, Turma 12).

Projeto da disciplina **Algoritmos em Grafos** (Uniube, 2026/2): ordenação topológica de
disciplinas com os algoritmos de **Kahn** e **DFS**, detecção de ciclos e montagem dos
períodos (disciplinas que podem ser cursadas ao mesmo tempo).

## Como rodar

Requisitos: Java 21 e Maven.

```bash
mvn test                                              # testes
mvn package                                           # gera o jar
java -jar target/ordenacao-disciplinas-1.0.0.jar      # menu interativo
java -jar target/ordenacao-disciplinas-1.0.0.jar --demo   # os 3 cenários do roteiro
```

## Modelagem

Grafo direcionado com **listas de adjacência**. Cada vértice é uma disciplina; a aresta
`A → B` significa que A é pré-requisito de B. O cadastro guarda os pré-requisitos dentro de B,
e o grafo inverte isso para montar as arestas saindo do pré-requisito.

## Estrutura

| Classe | Papel |
|---|---|
| `modelo.Disciplina` | código, nome, período, lista de pré-requisitos |
| `grafo.GrafoDisciplinas` | listas de adjacência, graus de entrada, validação de uma ordenação |
| `algoritmo.OrdenacaoKahn` | Kahn com fila FIFO; períodos por nível |
| `algoritmo.OrdenacaoDFS` | DFS com três estados (não visitado, em processamento, finalizado); devolve o ciclo exato |
| `algoritmo.TodasOrdenacoes` | lista todas as ordenações por backtracking (para a demonstração) |
| `algoritmo.ValidadorPeriodos` | aponta pré-requisito cadastrado no mesmo período ou depois |
| `app.Cenarios` / `app.ConsoleApp` | os 3 cenários e o menu |

## Complexidade

Kahn e DFS são **O(V + E)** com listas de adjacência: cada vértice é processado uma vez e
cada aresta é examinada uma vez. Com matriz de adjacência, descobrir os sucessores de um
vértice custa O(V), e os dois passariam a O(V²).

## Cenários de demonstração

1. **Sem ciclos**: grade de 9 disciplinas em 4 períodos.
2. **Múltiplas ordenações**: exemplo do roteiro. Kahn dá `A → B → C → D → E`, DFS dá
   `A → C → E → B → D`, e existem 6 ordenações válidas.
3. **Com ciclo**: `ALG → ED → ES → ALG`. Kahn ordena só o que não depende do ciclo; DFS aponta o ciclo.
4. **Disciplinas reais**: 20 disciplinas de Sistemas de Informação da Uniube, com os códigos do
   currículo. O currículo não informa pré-requisitos: as dependências I → II seguem o nome, e as
   demais são supostas para a demonstração.
