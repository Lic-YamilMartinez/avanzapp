package com.avanzapp.avanzapp.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ObservacionIrpNormalizer {

    private static final Pattern COMPRA_FACTURA = Pattern.compile(
            ".*\\bCOMPRA\\b.*(FACTURA|\\bFC\\b).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DOCUMENTO_PROVEEDOR = Pattern.compile(
            "^\\d{3}[- ]\\d{3}[- ]\\d{6,8}.*"
    );

    private static final List<Regla> REGLAS = List.of(
            regla("Consumición", "CONSUMICION(?:ES)?|CONSUMISION(?:ES)?|CONSUMICION(?:ES)?\\s+.*"),
            regla("Vestimenta", "VESTIMENTA(?:S)?|ROPA|ROPAS|PRENDA(?:S)?(?: DE VESTIR)?"),
            regla("Combustible", "COMBUSTIBLE(?:S)?|NAFTA|GASOIL|DIESEL"),
            regla("Farmacia", "FARMACIA(?:S)?|MEDICAMENTO(?:S)?"),
            regla("Flete", "FLETE(?:S)?"),
            regla("Delivery", "DELIVERY|DELIVERI"),
            regla("Gastos bancarios", "GASTO(?:S)? BANCARIO(?:S)?|COMISION(?:ES)? BANCARIA(?:S)?"),
            regla("Artículos alimenticios", "ARTICULO(?:S)? ALIMENTICIO(?:S)?|ALIMENTO(?:S)?|VIVERE(?:S)?"),
            regla("Cuota de colegio", "CUOTA(?:S)? DE COLEGIO|CUOTA(?:S)? COLEGIO|MENSUALIDAD(?:ES)? DE COLEGIO"),
            regla("Accesorios de tenis", "ACCESORIO(?:S)? DE TENIS|ACCESORIO(?:S)? TENIS"),
            regla("Monitoreo", "MONITOREO"),
            regla("Mantenimiento de jardín", "MANT(?:ENIMIENTO)?\\.? DE JARDIN|MANTENIMIENTO JARDIN|JARDINERIA"),
            regla("Cuota social", "CUOTA SOCIAL"),
            regla("Gas", "GAS"),
            regla("Internet", "INTERNET"),
            regla("Seguro médico", "SEGURO MEDICO"),
            regla("Fórmula natural", "FORMULA NATURAL"),
            regla("Examen", "EXAMEN(?:ES)?"),
            regla("Servicios", "SERVICIO(?:S)?"),
            regla("Interés", "INTERES(?:ES)?"),
            regla("Excursión", "EXCURSION(?:ES)?"),
            regla("Seña", "SENA"),
            regla("Instrumentos musicales", "INSTRUMENTO(?:S)? MUSICAL(?:ES)?")
    );

    private static final List<Alias> ALIASES = construirAliases();

    public Resultado normalizar(String original) {
        String limpio = limpiarPresentacion(original);
        if (limpio == null) {
            return new Resultado(null, null, 0.0, "VACIO");
        }

        String clave = clave(limpio);

        // Texto administrativo sin valor semántico para la categoría del gasto.
        if (COMPRA_FACTURA.matcher(clave).matches() || DOCUMENTO_PROVEEDOR.matcher(clave).matches()) {
            return new Resultado("Compra según factura", null, 1.0, "REGLA_FACTURA");
        }

        for (Regla regla : REGLAS) {
            if (regla.pattern().matcher(clave).matches()) {
                return new Resultado(regla.canonica(), regla.canonica(), 1.0, "REGLA");
            }
        }

        Alias mejor = null;
        double mejorScore = 0.0;
        double segundoScore = 0.0;

        for (Alias alias : ALIASES) {
            double score = similitud(clave, alias.alias());
            if (score > mejorScore) {
                segundoScore = mejorScore;
                mejorScore = score;
                mejor = alias;
            } else if (score > segundoScore) {
                segundoScore = score;
            }
        }

        double umbral = clave.length() <= 4 ? 0.96 : clave.length() <= 7 ? 0.90 : 0.84;
        boolean margenSuficiente = (mejorScore - segundoScore) >= 0.05;

        if (mejor != null && mejorScore >= umbral && margenSuficiente) {
            return new Resultado(
                    mejor.canonica(),
                    mejor.canonica(),
                    redondear(mejorScore),
                    "FUZZY"
            );
        }

        // No inventamos una categoría si la coincidencia no es suficientemente segura.
        return new Resultado(limpio, null, 0.40, "LIMPIEZA");
    }

    private static List<Alias> construirAliases() {
        List<Alias> aliases = new ArrayList<>();
        for (Regla regla : REGLAS) {
            aliases.add(new Alias(regla.canonica(), clave(regla.canonica())));
        }

        // Errores de tipeo / abreviaciones frecuentes que queremos absorber de forma explícita.
        aliases.add(new Alias("Consumición", "CONSUMISION"));
        aliases.add(new Alias("Consumición", "CONSUMISIONES"));
        aliases.add(new Alias("Combustible", "COMBUSTIVLE"));
        aliases.add(new Alias("Gastos bancarios", "GASTOS BANCARIOS"));
        aliases.add(new Alias("Mantenimiento de jardín", "MANT DE JARDIN"));
        aliases.add(new Alias("Artículos alimenticios", "ARTICULOS ALIMENTICIOS"));
        aliases.add(new Alias("Seguro médico", "SEGURO MEDICO"));
        return aliases;
    }

    private static Regla regla(String canonica, String regex) {
        return new Regla(canonica, Pattern.compile("^(?:" + regex + ")$", Pattern.CASE_INSENSITIVE));
    }

    private static String limpiarPresentacion(String value) {
        if (value == null) return null;
        String limpio = value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return limpio.isBlank() ? null : limpio;
    }

    private static String clave(String value) {
        if (value == null) return "";
        String normalizado = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace("Ñ", "N")
                .replaceAll("[^A-Z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalizado;
    }

    private static double similitud(String a, String b) {
        if (a.equals(b)) return 1.0;
        if (a.isBlank() || b.isBlank()) return 0.0;

        int distancia = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1.0 : 1.0 - ((double) distancia / max);
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + costo
                );
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }

        return prev[b.length()];
    }

    private static double redondear(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private record Regla(String canonica, Pattern pattern) {}

    private record Alias(String canonica, String alias) {}

    public record Resultado(
            String normalizada,
            String categoria,
            double confianza,
            String metodo
    ) {}
}
