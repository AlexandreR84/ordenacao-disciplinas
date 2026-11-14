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
java -jar target/ordenacao-disciplinas-1.0.0.jar --demo   # os 4 cenários, no console
java -jar target/ordenacao-disciplinas-1.0.0.jar --web    # interface no navegador
```

## Interface web

`--web` sobe um servidor HTTP do próprio JDK (sem Spring, sem dependências) em
`http://localhost:8080/` e abre o navegador. Para usar outra porta: `--web 8081`.
Deixe a janela do terminal aberta enquanto usa a página.

A página só cadastra e desenha: os algoritmos continuam rodando em Java. Ela envia as disciplinas
para `POST /api/analisar` e recebe Kahn, DFS, períodos, ordenações e o grafo. Nela dá para:

- cadastrar, editar e remover disciplinas e ligar dependências, ou carregar um dos 4 cenários;
- ver o grafo em colunas pelo período da grade (SE) ou pelo período mínimo que os pré-requisitos
  permitem, com o ciclo destacado em vermelho;
- reproduzir passo a passo o Kahn (fila e ordem se formando) e o DFS (não visitado, em processamento,
  finalizado), usando o registro de passos gerado pelo próprio algoritmo;
- comparar Kahn × DFS, ver os períodos, a lista de adjacência, as ordenações e a complexidade.

O total de ordenações é contado sem listar uma a uma (`algoritmo.ContagemOrdenacoes`): o grafo é
separado em componentes independentes, cada componente é contado por programação dinâmica sobre os
conjuntos de disciplinas já cursadas, e os resultados se combinam pelo coeficiente multinomial.

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
| `algoritmo.ContagemOrdenacoes` | conta o total exato de ordenações (programação dinâmica) |
| `app.Cenarios` / `app.ConsoleApp` | os 4 cenários e o menu |
| `web.ServidorWeb` / `web.Analise` / `web.Json` | servidor HTTP, ponte com os algoritmos e JSON |
| `src/main/resources/web` | a página (HTML, CSS e JavaScript) |

## Complexidade

Kahn e DFS são **O(V + E)** com listas de adjacência: cada vértice é processado uma vez e
cada aresta é examinada uma vez. Com matriz de adjacência, descobrir os sucessores de um
vértice custa O(V), e os dois passariam a O(V²).

## Cenários de demonstração

1. **Sem ciclos**: grade de 9 disciplinas em 4 períodos.
2. **Múltiplas ordenações**: exemplo do roteiro. Kahn dá `A → B → C → D → E`, DFS dá
   `A → C → E → B → D`, e existem 6 ordenações válidas.
3. **Com ciclo**: `ALG → ED → ES → ALG`. Kahn ordena só o que não depende do ciclo; DFS aponta o ciclo.
4. **Grade completa do curso**: os 59 itens do currículo de Sistemas de Informação da Uniube
   (curso 160, currículo 1), com código, nome e o período (SE) oficial de 1 a 8; o ENADE fica sem
   período. O currículo não informa pré-requisitos: as 8 dependências I → II seguem o nome, e as
   outras 34 são supostas para a demonstração, todas respeitando os períodos oficiais. Pelos
   pré-requisitos bastariam 5 períodos; a grade usa 8. O total de ordenações tem 69 dígitos
   (≈ 1,1 × 10⁶⁸).
