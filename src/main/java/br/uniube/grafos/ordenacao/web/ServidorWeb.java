package br.uniube.grafos.ordenacao.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interface web: um servidor HTTP do próprio JDK ({@code com.sun.net.httpserver}),
 * sem framework. Serve a página (HTML, CSS e JS em {@code src/main/resources/web})
 * e dois endpoints JSON que chamam os mesmos algoritmos do menu de console:
 *
 * <ul>
 *   <li>{@code GET /api/cenarios}: os quatro cenários de demonstração;</li>
 *   <li>{@code POST /api/analisar}: recebe as disciplinas e devolve Kahn, DFS,
 *       períodos, ordenações e o grafo.</li>
 * </ul>
 *
 * <p>Escuta só em {@code localhost}: a página é para uso no próprio computador.</p>
 */
public class ServidorWeb {

    public static final int PORTA_PADRAO = 8080;
    private static final int TAMANHO_MAXIMO_CORPO = 1 << 20; // 1 MB

    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml",
            "ico", "image/x-icon");

    private final HttpServer servidor;
    private final ExecutorService threads = Executors.newFixedThreadPool(4);

    /** @param porta porta TCP; 0 escolhe uma livre (usado nos testes) */
    public ServidorWeb(int porta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), porta), 0);
        servidor.createContext("/api/cenarios", this::cenarios);
        servidor.createContext("/api/analisar", this::analisar);
        servidor.createContext("/", this::arquivo);
        servidor.setExecutor(threads);
    }

    public void iniciar() {
        servidor.start();
    }

    public void parar() {
        servidor.stop(0);
        threads.shutdownNow();
    }

    public int porta() {
        return servidor.getAddress().getPort();
    }

    public String endereco() {
        return "http://localhost:" + porta() + "/";
    }

    /** Uso: {@code java -jar ordenacao-disciplinas.jar --web [porta]}. */
    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : PORTA_PADRAO;
        ServidorWeb web;
        try {
            web = new ServidorWeb(porta);
        } catch (java.net.BindException e) {
            System.err.println("A porta " + porta + " já está em uso. Tente outra: --web 8081");
            System.exit(1);
            return;
        }
        web.iniciar();
        System.out.println("Interface web em " + web.endereco());
        System.out.println("Deixe esta janela aberta enquanto usa a página. Ctrl+C encerra.");
        abrirNavegador(web.endereco());
    }

    private static void abrirNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception | Error e) {
            // sem navegador (ex.: terminal remoto): o endereço já foi impresso
        }
    }

    // ------------------------------------------------------------ rotas

    private void cenarios(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) {
            responderErro(ex, 405, "use GET");
            return;
        }
        responderJson(ex, 200, Map.of("cenarios", Analise.cenarios()));
    }

    private void analisar(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) {
            responderErro(ex, 405, "use POST");
            return;
        }
        try {
            byte[] corpo = lerCorpo(ex.getRequestBody());
            Object json = Json.ler(new String(corpo, StandardCharsets.UTF_8));
            responderJson(ex, 200, Analise.analisar(Analise.lerDisciplinas(json)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            responderErro(ex, 400, e.getMessage());
        } catch (RuntimeException e) {
            responderErro(ex, 500, "erro interno: " + e);
        }
    }

    private void arquivo(HttpExchange ex) throws IOException {
        String caminho = ex.getRequestURI().getPath();
        if (caminho.equals("/")) {
            caminho = "/index.html";
        }
        if (!ex.getRequestMethod().equals("GET") || caminho.contains("..")) {
            responderErro(ex, 404, "não encontrado");
            return;
        }
        try (InputStream in = ServidorWeb.class.getResourceAsStream("/web" + caminho)) {
            if (in == null) {
                responderErro(ex, 404, "não encontrado: " + caminho);
                return;
            }
            String extensao = caminho.substring(caminho.lastIndexOf('.') + 1);
            ex.getResponseHeaders().set("Content-Type", TIPOS.getOrDefault(extensao, "application/octet-stream"));
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            enviar(ex, 200, in.readAllBytes());
        }
    }

    // ------------------------------------------------------------ utilidades

    private static byte[] lerCorpo(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(TAMANHO_MAXIMO_CORPO + 1);
        if (bytes.length > TAMANHO_MAXIMO_CORPO) {
            throw new IllegalArgumentException("corpo maior que 1 MB");
        }
        return bytes;
    }

    private static void responderJson(HttpExchange ex, int status, Object corpo) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        enviar(ex, status, Json.escrever(corpo).getBytes(StandardCharsets.UTF_8));
    }

    private static void responderErro(HttpExchange ex, int status, String mensagem) throws IOException {
        responderJson(ex, status, Map.of("erro", mensagem == null ? "erro" : mensagem));
    }

    private static void enviar(HttpExchange ex, int status, byte[] corpo) throws IOException {
        ex.sendResponseHeaders(status, corpo.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(corpo);
        }
    }
}
