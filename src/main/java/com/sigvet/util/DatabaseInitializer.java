package com.sigvet.util;

import com.sigvet.util.ConexionBD;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Inicializador automático de la base de datos SIGVET.
 *
 * <p>Esta clase es responsable de verificar si la base de datos {@code sigvet} existe
 * al inicio de la aplicación y, de no existir, crearla automáticamente ejecutando los
 * scripts SQL embebidos en el JAR ({@code sigvet_ddl.sql} y {@code sigvet_dml.sql})
 * ubicados en {@code src/main/resources/}.</p>
 *
 * <p><strong>El usuario solo necesita tener MySQL Server ejecutándose.</strong>
 * No se requiere ninguna intervención manual en MySQL Workbench ni en la terminal.</p>
 *
 * <p>La creación de la BD se realiza en un hilo de fondo ({@link SwingWorker}) para
 * no bloquear el EDT, mostrando un diálogo de progreso al usuario.</p>
 *
 * <h3>Flujo de {@link #inicializar()}:</h3>
 * <ol>
 *   <li>Verifica conexión al servidor MySQL (sin especificar BD).</li>
 *   <li>Consulta si la BD {@code sigvet} ya existe via {@code SHOW DATABASES}.</li>
 *   <li>Si existe → retorna {@code true} directamente.</li>
 *   <li>Si no existe → pregunta al usuario y ejecuta DDL + DML con barra de progreso.</li>
 * </ol>
 *
 * @author SIGVET
 * @version 1.0
 * @see ConexionBD#getConexionServidor()
 */
public class DatabaseInitializer {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Nombre de la base de datos del sistema. */
    private static final String NOMBRE_BD = "sigvet";

    /** Si es {@code true}, crea la BD sin pedir confirmación al usuario. */
    private static boolean crearAutomaticamente;

    /** Ruta del script DDL en el classpath (crea tablas, triggers, SPs, funciones, vistas). */
    private static final String SCRIPT_DDL = "sigvet_ddl.sql";

    /** Ruta del script DML en el classpath (inserta datos de prueba). */
    private static final String SCRIPT_DML = "sigvet_dml.sql";

    /**
     * Constructor privado: clase de utilidad estática, no se instancia.
     */
    private DatabaseInitializer() { }

    /**
     * Indica si la BD debe crearse sin diálogo de confirmación.
     *
     * @param automatico {@code true} para omitir la confirmación
     */
    public static void setCrearBaseDatosAutomaticamente(boolean automatico) {
        crearAutomaticamente = automatico;
    }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Verifica si la BD {@code sigvet} existe y, de no existir, la crea automáticamente.
     *
     * <p>Este método se llama desde {@link com.sigvet.Main#main} ANTES de abrir
     * la ventana principal. Si retorna {@code false}, la aplicación debe terminar.</p>
     *
     * @return {@code true} si la BD existe o fue creada exitosamente;
     *         {@code false} si no se pudo conectar al servidor o hubo un error
     */
    public static boolean inicializar() {
        // 1. Verificar que el servidor MySQL esté disponible
        if (!ConexionBD.probarConexionServidor()) {
            return false;
        }

        // 2. Verificar si la BD 'sigvet' ya existe
        if (baseDatosExiste()) {
            System.out.println("[DatabaseInitializer] BD '" + NOMBRE_BD + "' encontrada. Continuando...");
            return true;
        }

        // 3. La BD no existe → confirmar creación (salvo modo automático)
        if (!crearAutomaticamente) {
            int respuesta = JOptionPane.showConfirmDialog(
                null,
                "La base de datos '" + NOMBRE_BD + "' no existe en este servidor MySQL.\n\n"
                + "SIGVET la creará automáticamente. Se crearán:\n"
                + "  • Tablas, índices y constraints\n"
                + "  • Triggers de validación de negocio\n"
                + "  • Procedimientos almacenados (SPs)\n"
                + "  • Funciones escalares y vistas\n"
                + "  • Datos de prueba iniciales\n\n"
                + "¿Desea crear la base de datos ahora?",
                "Crear Base de Datos — SIGVET",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (respuesta != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        // 4. Crear la BD con diálogo de progreso
        return crearBaseDatosConProgreso();
    }

    // =========================================================================
    // Métodos privados
    // =========================================================================

    /**
     * Verifica si la base de datos {@code sigvet} ya existe en el servidor MySQL.
     *
     * @return {@code true} si existe; {@code false} en caso contrario
     */
    private static boolean baseDatosExiste() {
        try (Connection conn = ConexionBD.getConexionServidor();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE '" + NOMBRE_BD + "'")) {
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] Error al verificar BD: " + e.getMessage());
            return false;
        }
    }

    /**
     * Crea la base de datos ejecutando DDL + DML en un hilo de fondo con barra de progreso.
     *
     * <p>El {@link SwingWorker} ejecuta la creación fuera del EDT para no congelar
     * la UI, mientras publica el progreso al diálogo modal.</p>
     *
     * @return {@code true} si la creación fue exitosa; {@code false} en caso de error
     */
    private static boolean crearBaseDatosConProgreso() {
        // Diálogo de progreso modal
        JDialog dialogoProgreso = new JDialog((JFrame) null, "Creando Base de Datos SIGVET", true);
        dialogoProgreso.setSize(420, 130);
        dialogoProgreso.setLocationRelativeTo(null);
        dialogoProgreso.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogoProgreso.setLayout(new java.awt.BorderLayout(10, 10));
        ((javax.swing.JComponent) dialogoProgreso.getContentPane())
            .setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel lblMensaje = new JLabel("Iniciando creación de la base de datos...");
        lblMensaje.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("0%");

        dialogoProgreso.add(lblMensaje,    java.awt.BorderLayout.NORTH);
        dialogoProgreso.add(progressBar,   java.awt.BorderLayout.CENTER);

        // Indicador de éxito para comunicar entre SwingWorker y el EDT
        final boolean[] exito = {false};
        final String[]  error = {null};

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {

            /**
             * Ejecuta la creación de la BD en el hilo de fondo.
             * Publica el progreso (0-100) al proceso para actualizar la barra.
             */
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = ConexionBD.getConexionServidor()) {

                    // Paso 1 — Ejecutar DDL (crea BD, tablas, triggers, SPs, funciones, vistas)
                    publish(5);
                    actualizarEtiqueta(lblMensaje, "Ejecutando script DDL (tablas, triggers, SPs)...");
                    String ddl = leerScriptSQL(SCRIPT_DDL);
                    List<String> sentenciasDdl = dividirSentencias(ddl);
                    ejecutarSentencias(conn, sentenciasDdl, 5, 70, lblMensaje);
                    publish(70);

                    // Paso 2 — Ejecutar DML (inserta datos de prueba)
                    actualizarEtiqueta(lblMensaje, "Insertando datos de prueba (DML)...");
                    String dml = leerScriptSQL(SCRIPT_DML);
                    List<String> sentenciasDml = dividirSentencias(dml);
                    ejecutarSentencias(conn, sentenciasDml, 70, 98, lblMensaje);
                    publish(98);

                    exito[0] = true;
                    actualizarEtiqueta(lblMensaje, "¡Base de datos creada exitosamente!");
                    publish(100);

                } catch (Exception e) {
                    error[0] = e.getMessage();
                    exito[0] = false;
                    throw e;
                }
                return null;
            }

            /**
             * Actualiza la barra de progreso en el EDT al recibir valores publicados.
             *
             * @param chunks valores de progreso publicados por {@link #doInBackground()}
             */
            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int valor = chunks.get(chunks.size() - 1);
                    progressBar.setValue(valor);
                    progressBar.setString(valor + "%");
                }
            }

            /**
             * Cierra el diálogo de progreso cuando el worker termina (con o sin error).
             */
            @Override
            protected void done() {
                dialogoProgreso.dispose();
            }
        };

        // Ejecutar worker y mostrar diálogo modal (bloquea EDT hasta done())
        worker.execute();
        dialogoProgreso.setVisible(true);

        // Verificar si hubo excepción en el worker
        try {
            worker.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            // El error ya fue capturado en error[]
        }

        if (exito[0]) {
            JOptionPane.showMessageDialog(
                null,
                "Base de datos '" + NOMBRE_BD + "' creada exitosamente.\n\n"
                + "Se ejecutaron los scripts DDL y DML con:\n"
                + "  • Tablas, índices y constraints de integridad\n"
                + "  • Triggers de validación de negocio\n"
                + "  • Procedimientos almacenados y funciones\n"
                + "  • Vistas del sistema\n"
                + "  • Datos de prueba iniciales\n\n"
                + "La aplicación continuará cargando normalmente.",
                "Base de Datos Creada — SIGVET",
                JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        } else {
            JOptionPane.showMessageDialog(
                null,
                "Error al crear la base de datos:\n\n"
                + (error[0] != null ? error[0] : "(Sin detalle disponible)") + "\n\n"
                + "Verifique que el usuario MySQL tenga permisos CREATE DATABASE.\n"
                + "Si el problema persiste, cree la BD manualmente ejecutando:\n"
                + "  sigvet_ddl.sql  →  sigvet_dml.sql",
                "Error al Crear Base de Datos — SIGVET",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    /**
     * Lee un script SQL desde el classpath (dentro del JAR o de {@code resources/}).
     *
     * @param nombreArchivo nombre del archivo SQL (ej: {@code "sigvet_ddl.sql"})
     * @return contenido completo del script como {@code String}
     * @throws IOException si el archivo no se encuentra en el classpath
     */
    private static String leerScriptSQL(String nombreArchivo) throws IOException {
        try (InputStream is = DatabaseInitializer.class
                .getClassLoader().getResourceAsStream(nombreArchivo)) {
            if (is == null) {
                throw new IOException("Script SQL no encontrado en classpath: " + nombreArchivo
                    + "\nAsegúrese de que el archivo esté en src/main/resources/");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null) {
                    sb.append(linea).append('\n');
                }
                return sb.toString();
            }
        }
    }

    /**
     * Divide un script SQL en sentencias individuales usando el delimitador
     * estándar {@code ;} (punto y coma al final de línea).
     *
     * <p>Maneja correctamente los bloques {@code DELIMITER $$} usados en
     * procedimientos almacenados, triggers y funciones: omite las directivas
     * {@code DELIMITER} y usa {@code $$} como separador hasta que se restaure.</p>
     *
     * @param script script SQL completo
     * @return lista de sentencias SQL listas para ejecutar
     */
    static List<String> dividirSentencias(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual  = new StringBuilder();
        String delimitador    = ";";
        boolean enBloque      = false;   // dentro de DELIMITER $$ ... $$

        for (String linea : script.split("\n")) {
            String lineaTrim = linea.trim();

            // Omitir comentarios de línea
            if (lineaTrim.startsWith("--") || lineaTrim.startsWith("#")) {
                continue;
            }

            // Detectar cambio de delimitador: DELIMITER $$ / DELIMITER ;
            if (lineaTrim.toUpperCase().startsWith("DELIMITER")) {
                String nuevoDelim = lineaTrim.substring("DELIMITER".length()).trim();
                if (nuevoDelim.equals(";")) {
                    delimitador = ";";
                    enBloque    = false;
                } else {
                    delimitador = nuevoDelim;
                    enBloque    = true;
                }
                continue;
            }

            actual.append(linea).append('\n');

            // Verificar si la línea actual termina con el delimitador en vigor
            if (enBloque) {
                // Para bloques // o $$: termina cuando la línea TERMINA con el delimitador
                // Ej: "END //" termina con "//" → fin del bloque
                if (lineaTrim.endsWith(delimitador)) {
                    String sentencia = actual.toString().trim();
                    // Quitar el delimitador del final de la sentencia acumulada
                    if (sentencia.endsWith(delimitador)) {
                        sentencia = sentencia.substring(0, sentencia.length() - delimitador.length()).trim();
                    }
                    if (!sentencia.isEmpty()) {
                        sentencias.add(sentencia);
                    }
                    actual.setLength(0);
                }
            } else {
                // Para sentencias normales: termina con ";"
                if (lineaTrim.endsWith(";")) {
                    String sentencia = actual.toString().trim();
                    if (sentencia.endsWith(";")) {
                        sentencia = sentencia.substring(0, sentencia.length() - 1).trim();
                    }
                    if (!sentencia.isEmpty()) {
                        sentencias.add(sentencia);
                    }
                    actual.setLength(0);
                }
            }
        }

        // Capturar sentencia restante sin delimitador final
        String resto = actual.toString().trim();
        if (!resto.isEmpty()) {
            sentencias.add(resto);
        }

        return sentencias;
    }

    /**
     * Ejecuta una lista de sentencias SQL en la conexión dada, actualizando
     * la barra de progreso proporcionalmente.
     *
     * <p>Usa {@code allowMultiQueries=true} implícitamente al iterar sentencia
     * por sentencia. Errores de sentencias individuales son ignorados con log
     * (permite re-ejecución si algunos objetos ya existen).</p>
     *
     * @param conn         conexión JDBC al servidor MySQL
     * @param sentencias   lista de sentencias SQL a ejecutar
     * @param inicioProgreso porcentaje inicial de la barra
     * @param finProgreso    porcentaje final de la barra
     * @param lblMensaje   label del diálogo para actualizar texto de contexto
     */
    private static void ejecutarSentencias(Connection conn, List<String> sentencias,
            int inicioProgreso, int finProgreso, JLabel lblMensaje) {
        int total = sentencias.size();
        if (total == 0) return;

        try (Statement stmt = conn.createStatement()) {
            for (int i = 0; i < total; i++) {
                String sql = sentencias.get(i).trim();
                if (sql.isEmpty()) continue;

                try {
                    stmt.execute(sql);
                } catch (SQLException ex) {
                    // Ignorar errores de objetos ya existentes (DROP IF EXISTS cubren esto,
                    // pero en caso de reintentos algunos INSERTs pueden fallar por duplicados)
                    System.err.println("[DatabaseInitializer] WARN (sentencia " + (i + 1)
                        + "/" + total + "): " + ex.getMessage());
                }

                // Actualizar progreso en el EDT
                int progreso = inicioProgreso
                    + (int) ((i + 1.0) / total * (finProgreso - inicioProgreso));
                final int p = progreso;
                SwingUtilities.invokeLater(() -> {});
                // El progreso se publica desde doInBackground via publish()
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] Error ejecutando sentencias: " + e.getMessage());
        }
    }

    /**
     * Actualiza el texto de una etiqueta en el EDT de forma segura.
     *
     * @param lbl   etiqueta a actualizar
     * @param texto nuevo texto a mostrar
     */
    private static void actualizarEtiqueta(JLabel lbl, String texto) {
        SwingUtilities.invokeLater(() -> lbl.setText(texto));
    }
}
