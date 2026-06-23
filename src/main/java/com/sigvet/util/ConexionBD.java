package com.sigvet.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestiona la conexión a la base de datos MySQL del sistema SIGVET.
 *
 * <p>Los parámetros se cargan en este orden:</p>
 * <ol>
 *   <li>Archivo {@code sigvet_config.properties} en el directorio de trabajo (fuera del JAR).</li>
 *   <li>Archivo embebido en el classpath dentro del JAR.</li>
 *   <li>Valores por defecto (localhost, root, sin contraseña).</li>
 * </ol>
 *
 * @author SIGVET
 * @version 1.0
 * @see DatabaseInitializer
 */
public class ConexionBD {

    private static final String ARCHIVO_CONFIG = "sigvet_config.properties";

    private static final String URL_DEFAULT =
        "jdbc:mysql://localhost:3306/sigvet"
            + "?useSSL=false"
            + "&serverTimezone=America/Argentina/Buenos_Aires"
            + "&allowPublicKeyRetrieval=true";

    private static String url;
    private static String urlServidor;
    private static String usuario;
    private static String password;

    static {
        cargarConfiguracion();
    }

    private ConexionBD() { }

    private static void cargarConfiguracion() {
        Properties props = new Properties();
        boolean cargado = false;

        File archivoExterno = getArchivoConfigExterno();
        if (archivoExterno.exists()) {
            try (InputStream is = new FileInputStream(archivoExterno)) {
                props.load(is);
                cargado = true;
            } catch (IOException e) {
                System.err.println("[ConexionBD] No se pudo leer " + archivoExterno.getAbsolutePath()
                    + ": " + e.getMessage());
            }
        }

        if (!cargado) {
            try (InputStream is = ConexionBD.class.getClassLoader()
                    .getResourceAsStream(ARCHIVO_CONFIG)) {
                if (is != null) {
                    props.load(is);
                    cargado = true;
                }
            } catch (IOException e) {
                System.err.println("[ConexionBD] No se pudo leer configuración del classpath: "
                    + e.getMessage());
            }
        }

        if (cargado) {
            url = props.getProperty("db.url", URL_DEFAULT);
            usuario = props.getProperty("db.usuario", "root");
            password = props.getProperty("db.password", "");
        } else {
            url = URL_DEFAULT;
            usuario = "root";
            password = "";
        }

        urlServidor = derivarUrlServidor(url);
    }

    /**
     * Deriva la URL del servidor MySQL (sin nombre de base de datos) a partir de la URL de la BD.
     *
     * @param urlBd URL JDBC con base de datos {@code sigvet}
     * @return URL JDBC al servidor sin BD seleccionada
     */
    static String derivarUrlServidor(String urlBd) {
        if (urlBd == null) {
            return URL_DEFAULT.replace("/sigvet?", "/?");
        }
        return urlBd.replaceAll("/sigvet\\?", "/?");
    }

    /**
     * Actualiza la configuración en memoria (tras guardar desde el diálogo de configuración).
     *
     * @param nuevaUrl         URL JDBC a la BD {@code sigvet}
     * @param nuevaUrlServidor URL JDBC al servidor sin BD
     * @param nuevoUsuario     usuario MySQL
     * @param nuevaPassword    contraseña MySQL
     */
    public static void actualizarConfiguracion(String nuevaUrl, String nuevaUrlServidor,
            String nuevoUsuario, String nuevaPassword) {
        url = nuevaUrl;
        urlServidor = nuevaUrlServidor;
        usuario = nuevoUsuario;
        password = nuevaPassword;
    }

    /**
     * Recarga la configuración desde disco o classpath.
     */
    public static void recargarConfiguracion() {
        cargarConfiguracion();
    }

    /**
     * Archivo de configuración externo en el directorio de trabajo de la aplicación.
     *
     * @return archivo {@code sigvet_config.properties}
     */
    public static File getArchivoConfigExterno() {
        return new File(System.getProperty("user.dir"), ARCHIVO_CONFIG);
    }

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }

    public static Connection getConexionServidor() throws SQLException {
        return DriverManager.getConnection(urlServidor, usuario, password);
    }

    public static boolean probarConexion() {
        try (Connection conn = getConexion()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean probarConexionServidor() {
        try (Connection conn = getConexionServidor()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getUsuario() {
        return usuario;
    }

    public static String getPassword() {
        return password;
    }

    public static String getUrl() {
        return url;
    }

    public static String getUrlServidor() {
        return urlServidor;
    }
}
