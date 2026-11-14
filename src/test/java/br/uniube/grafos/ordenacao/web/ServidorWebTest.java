package br.uniube.grafos.ordenacao.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sobe o servidor de verdade numa porta livre e conversa com ele por HTTP. */
class ServidorWebTest {

    private static ServidorWeb servidor;
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @BeforeAll
    static void subir() throws IOException {
        servidor = new ServidorWeb(0);
        servidor.iniciar();
    }

    @AfterAll
    static void descer() {
        servidor.parar();
    }

    private static HttpResponse<String> get(String caminho) throws Exception {
        return CLIENTE.send(HttpRequest.newBuilder(URI.create(servidor.endereco() + caminho)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> post(String caminho, String corpo) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(servidor.endereco() + caminho))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8)).build();
        return CLIENTE.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(HttpResponse<String> r) {
        return (Map<String, Object>) Json.ler(r.body());
    }

    @Test
    void servePaginaEArquivos() throws Exception {
        HttpResponse<String> r = get("");
        assertEquals(200, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
        assertTrue(r.body().contains("Planejamento Inteligente da Oferta de Disciplinas"));
        assertEquals(200, get("app.js").statusCode());
        assertEquals(200, get("style.css").statusCode());
        assertEquals(404, get("nao-existe.js").statusCode());
        assertEquals(404, get("../pom.xml").statusCode());
    }

    @Test
    void listaOsQuatroCenarios() throws Exception {
        List<?> cenarios = (List<?>) json(get("api/cenarios")).get("cenarios");
        assertEquals(4, cenarios.size());
        assertTrue(((Map<?, ?>) cenarios.get(1)).get("titulo").toString().startsWith("Cenário 2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void analisaOExemploDoRoteiro() throws Exception {
        String corpo = """
                {"disciplinas": [
                  {"codigo": "A", "nome": "Algoritmos", "periodo": 0, "preRequisitos": []},
                  {"codigo": "B", "nome": "Estruturas de Dados", "periodo": 0, "preRequisitos": ["A"]},
                  {"codigo": "C", "nome": "Banco de Dados", "periodo": 0, "preRequisitos": ["A"]},
                  {"codigo": "D", "nome": "Engenharia de Software", "periodo": 0, "preRequisitos": ["B"]},
                  {"codigo": "E", "nome": "Desenvolvimento Web", "periodo": 0, "preRequisitos": ["C"]}
                ]}""";
        HttpResponse<String> r = post("api/analisar", corpo);
        assertEquals(200, r.statusCode());
        Map<String, Object> a = json(r);
        assertEquals(false, a.get("possuiCiclo"));
        assertEquals(List.of("A", "B", "C", "D", "E"), ((Map<String, Object>) a.get("kahn")).get("ordem"));
        assertEquals(List.of("A", "C", "E", "B", "D"), ((Map<String, Object>) a.get("dfs")).get("ordem"));
        assertEquals(List.of(List.of("A"), List.of("B", "C"), List.of("D", "E")), a.get("periodos"));
        assertEquals("6", ((Map<String, Object>) a.get("ordenacoes")).get("total"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void analisaGrafoComCicloECodigoNumerico() throws Exception {
        String corpo = "{\"disciplinas\":[{\"codigo\":12566,\"nome\":\"X\",\"preRequisitos\":[\"Y\"]},"
                + "{\"codigo\":\"y\",\"nome\":\"Y\",\"preRequisitos\":[12566]}]}";
        Map<String, Object> a = json(post("api/analisar", corpo));
        assertEquals(true, a.get("possuiCiclo"));
        assertEquals(List.of("12566", "Y", "12566"), ((Map<String, Object>) a.get("dfs")).get("ciclo"));
        assertEquals(List.of(), a.get("periodos"));
        assertEquals("0", ((Map<String, Object>) a.get("ordenacoes")).get("total"));
    }

    @Test
    void errosViram400ComMensagem() throws Exception {
        HttpResponse<String> r = post("api/analisar",
                "{\"disciplinas\":[{\"codigo\":\"B\",\"nome\":\"B\",\"preRequisitos\":[\"Z\"]}]}");
        assertEquals(400, r.statusCode());
        assertEquals("pré-requisito Z de B não foi cadastrado", json(r).get("erro"));

        assertEquals(400, post("api/analisar", "{nada").statusCode());
        assertEquals(400, post("api/analisar", "{\"outra\":1}").statusCode());
        assertEquals(400, post("api/analisar",
                "{\"disciplinas\":[{\"codigo\":\"A\",\"nome\":\"\"}]}").statusCode());
        assertEquals(405, get("api/analisar").statusCode());
    }
}
