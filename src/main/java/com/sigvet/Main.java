package com.sigvet;

import com.sigvet.util.ConexionBD;
import com.sigvet.util.DatabaseInitializer;
import com.sigvet.view.ConfiguracionBDDialog;
import com.sigvet.view.VentanaPrincipal;

import javax.swing.*;

/**
 * Punto de entrada principal del sistema SIGVET.
 *
 * <p>Si no puede conectarse a MySQL al iniciar, muestra {@link ConfiguracionBDDialog}
 * para que el usuario ingrese host, puerto, usuario y contraseña sin editar archivos.</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public class Main {

    private Main() { }

    /**
     * Método principal de la aplicación.
     *
     * @param args argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo configurar Look and Feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            boolean conectado = ConexionBD.probarConexionServidor();

            if (!conectado) {
                ConfiguracionBDDialog dialog = new ConfiguracionBDDialog(null);
                dialog.setVisible(true);

                if (!dialog.isConfiguracionGuardada()) {
                    System.exit(0);
                }

                DatabaseInitializer.setCrearBaseDatosAutomaticamente(
                    dialog.isCrearBdAutomaticamente());

                conectado = ConexionBD.probarConexionServidor();
                if (!conectado) {
                    JOptionPane.showMessageDialog(null,
                        "La configuración ingresada no permite conectar al servidor MySQL.\n"
                        + "Verifique que MySQL Server esté instalado y ejecutándose.\n\n"
                        + "Si no tiene MySQL instalado, descárguelo de:\n"
                        + "https://dev.mysql.com/downloads/mysql/",
                        "Error de Conexión",
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }

            boolean bdLista = DatabaseInitializer.inicializar();
            if (!bdLista) {
                JOptionPane.showMessageDialog(null,
                    "No se pudo inicializar la base de datos.\n"
                    + "La aplicación no puede continuar sin la BD.\n\n"
                    + "Verifique que MySQL Server esté ejecutándose y que el usuario\n"
                    + "tenga permisos CREATE DATABASE.",
                    "Error Fatal",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}
