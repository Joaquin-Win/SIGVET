package com.sigvet.util;

import com.sigvet.exception.VencimientoInvalidoException;
import java.time.LocalDate;

/**
 * Utilidad de validación para la capa de presentación y servicio del sistema SIGVET.
 *
 * <p>Proporciona métodos estáticos de validación que se ejecutan antes de enviar
 * datos a la base de datos, ofreciendo feedback rápido al usuario mediante
 * {@link IllegalArgumentException} o excepciones de dominio.</p>
 *
 * <p><strong>Principio de validación en dos niveles:</strong></p>
 * <ol>
 *   <li><strong>Java (esta clase):</strong> validación temprana con mensaje amigable.</li>
 *   <li><strong>BD (triggers/SPs):</strong> barrera final que garantiza integridad.</li>
 * </ol>
 *
 * <p>Clase de utilidad: no debe instanciarse (constructor privado).</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public final class ValidadorUtil {

    /**
     * Constructor privado: impide la instanciación de esta clase de utilidad.
     */
    private ValidadorUtil() {
        // No instanciar
    }

    // =========================================================================
    // Validaciones de texto
    // =========================================================================

    /**
     * Valida que un valor de texto no sea nulo ni vacío.
     *
     * @param valor valor a validar
     * @param campo nombre del campo (para el mensaje de error)
     * @throws IllegalArgumentException si el valor es nulo o vacío
     */
    public static void validarNoVacio(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio y no puede estar vacío.");
        }
    }

    /**
     * Valida que un DNI tenga un formato válido.
     *
     * <p>Un DNI válido contiene solo dígitos y tiene entre 7 y 9 caracteres.</p>
     *
     * @param dni DNI a validar
     * @throws IllegalArgumentException si el DNI es nulo, vacío o tiene formato inválido
     */
    public static void validarDni(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede ser nulo ni vacío.");
        }
        String dniLimpio = dni.trim().replaceAll("[.\\-]", ""); // permite puntos y guiones
        if (!dniLimpio.matches("\\d{7,9}")) {
            throw new IllegalArgumentException(
                "El DNI '" + dni + "' tiene un formato inválido. Debe contener entre 7 y 9 dígitos.");
        }
    }

    /**
     * Valida que una matrícula profesional no sea nula ni vacía.
     *
     * @param matricula matrícula a validar
     * @throws IllegalArgumentException si la matrícula es nula o vacía
     */
    public static void validarMatricula(String matricula) {
        if (matricula == null || matricula.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "La matrícula profesional es obligatoria y no puede estar vacía.");
        }
        if (matricula.trim().length() < 3) {
            throw new IllegalArgumentException(
                "La matrícula '" + matricula + "' es demasiado corta (mínimo 3 caracteres).");
        }
    }

    /**
     * Valida que una dirección de correo electrónico tenga un formato básico válido.
     *
     * <p>Verifica que contenga {@code @} y al menos un punto después del {@code @}.
     * Si el email es vacío o nulo, no lanza excepción (el campo es opcional).</p>
     *
     * @param email dirección de correo electrónico (puede ser nulo o vacío)
     * @throws IllegalArgumentException si el email no es vacío pero tiene formato inválido
     */
    public static void validarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return; // campo opcional, no se valida si está vacío
        }
        if (!email.trim().matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException(
                "El email '" + email + "' tiene un formato inválido.");
        }
    }

    // =========================================================================
    // Validaciones de fechas
    // =========================================================================

    /**
     * Valida que la fecha de vencimiento de un lote sea estrictamente mayor a la fecha actual.
     *
     * <p>Implementa la validación previa al trigger {@code trg_validar_vencimiento_stock}
     * que garantiza RN-09 en la BD.</p>
     *
     * @param fecha fecha de vencimiento a validar
     * @throws VencimientoInvalidoException si la fecha es nula, igual o anterior a hoy
     */
    public static void validarFechaVencimiento(LocalDate fecha) throws VencimientoInvalidoException {
        if (fecha == null) {
            throw new VencimientoInvalidoException(
                "La fecha de vencimiento no puede ser nula.");
        }
        if (!fecha.isAfter(LocalDate.now())) {
            throw new VencimientoInvalidoException(
                "La fecha de vencimiento (" + fecha + ") debe ser posterior a la fecha actual ("
                + LocalDate.now() + ") — RN-09.");
        }
    }

    // =========================================================================
    // Validaciones numéricas
    // =========================================================================

    /**
     * Valida que una cantidad sea estrictamente mayor a cero.
     *
     * @param cantidad cantidad a validar
     * @param campo    nombre del campo (para el mensaje de error)
     * @throws IllegalArgumentException si la cantidad es <= 0
     */
    public static void validarCantidadPositiva(int cantidad, String campo) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                "El campo '" + campo + "' debe ser mayor a 0. Valor recibido: " + cantidad);
        }
    }

    /**
     * Valida que un precio de venta sea estrictamente mayor a cero.
     *
     * @param precio precio a validar
     * @throws IllegalArgumentException si el precio es <= 0
     */
    public static void validarPrecioPositivo(double precio) {
        if (precio <= 0) {
            throw new IllegalArgumentException(
                "El precio de venta debe ser mayor a 0. Valor recibido: " + precio);
        }
    }

    /**
     * Valida que un valor entero sea mayor o igual al mínimo especificado.
     *
     * @param valor   valor a validar
     * @param minimo  valor mínimo permitido (inclusive)
     * @param campo   nombre del campo (para el mensaje de error)
     * @throws IllegalArgumentException si el valor es menor al mínimo
     */
    public static void validarMinimo(int valor, int minimo, String campo) {
        if (valor < minimo) {
            throw new IllegalArgumentException(
                "El campo '" + campo + "' debe ser al menos " + minimo
                + ". Valor recibido: " + valor);
        }
    }

    /**
     * Valida que una cadena no exceda una longitud máxima.
     *
     * @param valor      cadena a validar
     * @param maxLongitud longitud máxima permitida
     * @param campo      nombre del campo (para el mensaje de error)
     * @throws IllegalArgumentException si la cadena supera la longitud máxima
     */
    public static void validarLongitudMaxima(String valor, int maxLongitud, String campo) {
        if (valor != null && valor.length() > maxLongitud) {
            throw new IllegalArgumentException(
                "El campo '" + campo + "' no puede superar los " + maxLongitud
                + " caracteres. Longitud actual: " + valor.length());
        }
    }

    // =========================================================================
    // Normalización de texto
    // =========================================================================

    /**
     * Normaliza los espacios de una cadena de texto, colapsando múltiples espacios
     * consecutivos en uno solo y eliminando espacios al inicio y al final.
     *
     * <p>Usa un bucle {@code do-while} para repetir el proceso de reemplazo de
     * dobles espacios hasta que no haya más cambios, garantizando que se eliminen
     * cualquier cantidad de espacios consecutivos.</p>
     *
     * @param texto texto a normalizar (puede ser nulo o vacío)
     * @return texto con espacios normalizados, o cadena vacía si el texto es nulo o en blanco
     */
    public static String normalizarEspacios(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String resultado = texto.trim();
        String anterior;
        do {
            anterior = resultado;
            resultado = resultado.replace("  ", " ");
        } while (!resultado.equals(anterior));
        return resultado;
    }
}
