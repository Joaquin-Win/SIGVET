package com.sigvet.view;

import com.sigvet.util.ConexionBD;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diálogo de configuración de conexión MySQL mostrado cuando SIGVET no puede
 * conectarse al servidor al iniciar.
 *
 * <p>Permite ingresar host, puerto, usuario y contraseña, probar la conexión y
 * guardar la configuración en {@code sigvet_config.properties} en el directorio
 * de trabajo de la aplicación (fuera del JAR).</p>
 *
 * @author SIGVET
 * @version 1.0
 */
public class ConfiguracionBDDialog extends JDialog {

  private static final String NOMBRE_BD = "sigvet";
  private static final String PARAMS_JDBC =
      "?useSSL=false&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true";

  private static final Pattern PATRON_URL =
      Pattern.compile("jdbc:mysql://([^:/]+):(\\d+)/([^?]+)");

  private final JTextField txtHost;
  private final JTextField txtPuerto;
  private final JTextField txtUsuario;
  private final JPasswordField txtPassword;
  private final JCheckBox chkCrearBd;
  private final JLabel lblEstado;
  private final JButton btnGuardar;

  private boolean configuracionGuardada;
  private boolean crearBdAutomaticamente;

  /**
   * Crea el diálogo de configuración con valores actuales de {@link ConexionBD}.
   *
   * @param parent ventana padre (puede ser {@code null})
   */
  public ConfiguracionBDDialog(Frame parent) {
    super(parent, "SIGVET - Configuración de Base de Datos", true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setResizable(false);

    String host = "localhost";
    String puerto = "3306";
    Matcher m = PATRON_URL.matcher(ConexionBD.getUrl());
    if (m.find()) {
      host = m.group(1);
      puerto = m.group(2);
    }

    txtHost = new JTextField(host, 22);
    txtPuerto = new JTextField(puerto, 22);
    txtUsuario = new JTextField(ConexionBD.getUsuario(), 22);
    txtPassword = new JPasswordField(ConexionBD.getPassword(), 22);
    chkCrearBd = new JCheckBox("Crear base de datos automáticamente si no existe", true);

    lblEstado = new JLabel("Ingrese los datos y pulse «Probar Conexión».");
    lblEstado.setForeground(new Color(80, 80, 80));

    JButton btnProbar = new JButton("Probar Conexión");
    btnGuardar = new JButton("Guardar y Continuar");
    btnGuardar.setEnabled(false);
    JButton btnCancelar = new JButton("Cancelar");

    btnProbar.addActionListener(e -> probarConexion());
    btnGuardar.addActionListener(e -> guardarConfiguracion());
    btnCancelar.addActionListener(e -> {
      configuracionGuardada = false;
      dispose();
    });

    JPanel panelCampos = new JPanel(new GridBagLayout());
    panelCampos.setBorder(new EmptyBorder(12, 16, 8, 16));
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4, 4, 4, 8);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    agregarFila(panelCampos, gbc, 0, "Host:", txtHost);
    agregarFila(panelCampos, gbc, 1, "Puerto:", txtPuerto);
    agregarFila(panelCampos, gbc, 2, "Usuario:", txtUsuario);
    agregarFila(panelCampos, gbc, 3, "Contraseña:", txtPassword);

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    gbc.insets = new Insets(8, 4, 4, 8);
    panelCampos.add(chkCrearBd, gbc);

    JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    panelBotones.setBorder(new EmptyBorder(8, 8, 12, 8));
    panelBotones.add(btnProbar);
    panelBotones.add(btnGuardar);
    panelBotones.add(btnCancelar);

    JPanel panelInferior = new JPanel(new BorderLayout());
    panelInferior.setBorder(new EmptyBorder(0, 16, 0, 16));
    panelInferior.add(lblEstado, BorderLayout.CENTER);
    panelInferior.add(panelBotones, BorderLayout.SOUTH);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(panelCampos, BorderLayout.CENTER);
    getContentPane().add(panelInferior, BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(parent);
    getRootPane().setDefaultButton(btnProbar);
  }

  private static void agregarFila(JPanel panel, GridBagConstraints gbc, int fila,
      String etiqueta, JComponent campo) {
    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = fila;
    gbc.weightx = 0;
    panel.add(new JLabel(etiqueta), gbc);
    gbc.gridx = 1;
    gbc.weightx = 1;
    panel.add(campo, gbc);
  }

  private String construirUrlBD() {
    return "jdbc:mysql://" + txtHost.getText().trim() + ":" + txtPuerto.getText().trim()
        + "/" + NOMBRE_BD + PARAMS_JDBC;
  }

  private String construirUrlServidor() {
    return "jdbc:mysql://" + txtHost.getText().trim() + ":" + txtPuerto.getText().trim()
        + "/" + PARAMS_JDBC;
  }

  private void probarConexion() {
    String urlServidor = construirUrlServidor();
    String usuario = txtUsuario.getText().trim();
    String password = new String(txtPassword.getPassword());

    lblEstado.setText("Probando conexión...");
    lblEstado.setForeground(new Color(80, 80, 80));
    btnGuardar.setEnabled(false);

    try (Connection conn = DriverManager.getConnection(urlServidor, usuario, password)) {
      if (conn.isValid(5)) {
        lblEstado.setText("✓ Conexión exitosa al servidor MySQL");
        lblEstado.setForeground(new Color(0, 128, 0));
        btnGuardar.setEnabled(true);
      }
    } catch (SQLException e) {
      manejarErrorConexion(e);
    }
  }

  private void manejarErrorConexion(SQLException e) {
    String msg = e.getMessage() != null ? e.getMessage() : e.toString();
    String mensaje;

    if (msg.contains("Access denied")) {
      mensaje = "✗ Usuario o contraseña incorrectos";
    } else if (msg.contains("Communications link") || msg.contains("Connection refused")) {
      mensaje = "✗ No se puede conectar al servidor. ¿MySQL está ejecutándose?";
    } else if (msg.contains("Unknown database")) {
      lblEstado.setText("✓ Conexión exitosa (la BD se creará después)");
      lblEstado.setForeground(new Color(0, 128, 0));
      btnGuardar.setEnabled(true);
      return;
    } else {
      mensaje = "✗ Error: " + msg;
    }

    lblEstado.setText(mensaje);
    lblEstado.setForeground(Color.RED);
    btnGuardar.setEnabled(false);
  }

  private void guardarConfiguracion() {
    String usuario = txtUsuario.getText().trim();
    String password = new String(txtPassword.getPassword());
    String urlBd = construirUrlBD();
    String urlServidor = construirUrlServidor();

    try {
      Properties props = new Properties();
      props.setProperty("db.url", urlBd);
      props.setProperty("db.usuario", usuario);
      props.setProperty("db.password", password);

      File archivoConfig = ConexionBD.getArchivoConfigExterno();
      try (OutputStream os = new FileOutputStream(archivoConfig)) {
        props.store(os, "Configuración de base de datos SIGVET");
      }

      ConexionBD.actualizarConfiguracion(urlBd, urlServidor, usuario, password);
      crearBdAutomaticamente = chkCrearBd.isSelected();
      configuracionGuardada = true;
      dispose();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this,
          "Error al guardar la configuración: " + e.getMessage(),
          "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * @return {@code true} si el usuario guardó la configuración
   */
  public boolean isConfiguracionGuardada() {
    return configuracionGuardada;
  }

  /**
   * @return {@code true} si el usuario marcó crear la BD automáticamente
   */
  public boolean isCrearBdAutomaticamente() {
    return crearBdAutomaticamente;
  }
}
