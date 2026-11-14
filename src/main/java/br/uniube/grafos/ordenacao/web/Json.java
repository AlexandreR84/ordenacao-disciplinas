package br.uniube.grafos.ordenacao.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON mínimo, só o que a interface web precisa, para o projeto continuar sem dependências.
 *
 * <p>Escrita: {@link Map} vira objeto, {@link Iterable} vira array, e também
 * {@link String}, {@link Number}, {@link Boolean} e {@code null}.
 * Leitura: devolve {@link LinkedHashMap}, {@link ArrayList}, {@link String},
 * {@link Double}, {@link Boolean} ou {@code null}.</p>
 */
public final class Json {

    private Json() { }

    // ------------------------------------------------------------- escrita

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escrever(valor, sb);
        return sb.toString();
    }

    private static void escrever(Object valor, StringBuilder sb) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String s) {
            escreverTexto(s, sb);
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                escreverTexto(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escrever(e.getValue(), sb);
            }
            sb.append('}');
        } else if (valor instanceof Iterable<?> lista) {
            sb.append('[');
            boolean primeiro = true;
            for (Object item : lista) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                escrever(item, sb);
            }
            sb.append(']');
        } else {
            escreverTexto(valor.toString(), sb);
        }
    }

    private static void escreverTexto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------- leitura

    public static Object ler(String texto) {
        Leitor l = new Leitor(texto);
        l.espacos();
        Object valor = l.valor();
        l.espacos();
        if (l.pos < texto.length()) {
            throw l.erro("conteúdo depois do fim");
        }
        return valor;
    }

    private static final class Leitor {
        private final String s;
        private int pos;

        Leitor(String s) {
            this.s = s;
        }

        Object valor() {
            if (pos >= s.length()) {
                throw erro("fim inesperado");
            }
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> objeto();
                case '[' -> array();
                case '"' -> texto();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        yield numero();
                    }
                    throw erro("caractere inesperado '" + c + "'");
                }
            };
        }

        Map<String, Object> objeto() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++;
            espacos();
            if (consumir('}')) {
                return mapa;
            }
            do {
                espacos();
                if (pos >= s.length() || s.charAt(pos) != '"') {
                    throw erro("esperava o nome de um campo");
                }
                String chave = texto();
                espacos();
                exigir(':');
                espacos();
                mapa.put(chave, valor());
                espacos();
            } while (consumir(','));
            exigir('}');
            return mapa;
        }

        List<Object> array() {
            List<Object> lista = new ArrayList<>();
            pos++;
            espacos();
            if (consumir(']')) {
                return lista;
            }
            do {
                espacos();
                lista.add(valor());
                espacos();
            } while (consumir(','));
            exigir(']');
            return lista;
        }

        String texto() {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= s.length()) {
                    throw erro("texto sem aspas de fechamento");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (pos >= s.length()) {
                    throw erro("escape incompleto");
                }
                char e = s.charAt(pos++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw erro("escape \\u incompleto");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw erro("escape \\u inválido");
                        }
                        pos += 4;
                    }
                    default -> throw erro("escape inválido \\" + e);
                }
            }
        }

        Double numero() {
            int inicio = pos;
            if (s.charAt(pos) == '-') {
                pos++;
            }
            while (pos < s.length() && "0123456789.eE+-".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            try {
                return Double.valueOf(s.substring(inicio, pos));
            } catch (NumberFormatException ex) {
                throw erro("número inválido");
            }
        }

        Object literal(String palavra, Object valor) {
            if (!s.startsWith(palavra, pos)) {
                throw erro("esperava " + palavra);
            }
            pos += palavra.length();
            return valor;
        }

        void espacos() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        boolean consumir(char c) {
            if (pos < s.length() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        void exigir(char c) {
            if (!consumir(c)) {
                throw erro("esperava '" + c + "'");
            }
        }

        IllegalArgumentException erro(String motivo) {
            return new IllegalArgumentException("JSON inválido na posição " + pos + ": " + motivo);
        }
    }
}
