package br.uniube.grafos.ordenacao.web;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTest {

    @Test
    void escreveEstruturasAninhadas() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", "Estruturas \"de\" Dados\n");
        m.put("periodo", 2);
        m.put("ok", true);
        m.put("pre", List.of("ALG", "MAT"));
        m.put("nada", null);
        assertEquals("{\"nome\":\"Estruturas \\\"de\\\" Dados\\n\",\"periodo\":2,\"ok\":true,"
                + "\"pre\":[\"ALG\",\"MAT\"],\"nada\":null}", Json.escrever(m));
    }

    @Test
    void lerEEscreverVoltaAoMesmoValor() {
        String texto = "{\"disciplinas\":[{\"codigo\":\"ALG\",\"nome\":\"Programação → Ç\",\"periodo\":1.0,"
                + "\"preRequisitos\":[]},{\"codigo\":\"ED\",\"nome\":\"x\\\\y\",\"periodo\":null,\"preRequisitos\":[\"ALG\"]}]}";
        assertEquals(texto, Json.escrever(Json.ler(texto)));
    }

    @Test
    void leEscapesEEspacos() {
        Object v = Json.ler("  [ \"a\\u00e7\\/b\" , -1.5e1 , false , { } ]  ");
        assertEquals(Arrays.asList("aç/b", -15.0, false, Map.of()), v);
    }

    @Test
    void rejeitaJsonInvalido() {
        for (String ruim : new String[] {"", "{", "[1,]", "{\"a\" 1}", "\"sem fim", "tru", "{} lixo", "[1 2]"}) {
            assertThrows(IllegalArgumentException.class, () -> Json.ler(ruim), ruim);
        }
    }
}
