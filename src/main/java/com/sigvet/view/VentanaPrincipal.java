package com.sigvet.view;

import com.sigvet.service.*;
import com.sigvet.util.ConexionBD;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana principal del sistema SIGVET.
 *
 * <p>Implementa el patrón MDI (Multiple Document Interface) usando {@link JDesktopPane}
 * como área de trabajo central y {@link JInternalFrame} para cada caso de uso.
 * El {@link JMenuBar} provee acceso a todos los módulos del sistema.</p>
 *
 * <p><strong>Ciclo de vida:</strong></p>
 * <ol>
 *   <li>Instanciar los 8 Services en el constructor.</li>
 *   <li>Configurar Look and Feel del sistema.</li>
 *   <li>Construir la ventana: menú, desktop pane, barra de estado.</li>
 *   <li>Mostrar el {@link PanelDashboard} automáticamente al iniciar.</li>
 *   <li>Iniciar un {@link Timer} que actualiza la hora cada 60 segundos.</li>
 * </ol>
 *
 * @author SIGVET
 * @version 1.0
 */
public class VentanaPrincipal extends JFrame {

    // =========================================================================
    // Constantes de UI
    // =========================================================================

    /** Título de la ventana principal. */
    private static final String TITULO = "SIGVET - Sistema de Gestión Veterinaria";

    /** Ancho inicial de la ventana en píxeles. */
    private static final int ANCHO  = 1200;

    /** Alto inicial de la ventana en píxeles. */
    private static final int ALTO   = 800;

    /** Formato de fecha y hora para la barra de estado. */
    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================================
    // Componentes de UI
    // =========================================================================

    /** Área de trabajo MDI donde se abren los JInternalFrame. */
    private JDesktopPane desktopPane;

    /** Label de la hora actual en la barra de estado. */
    private JLabel lblHora;

    /** Label central del estado de conexión en la barra de estado. */
    private JLabel lblEstado;

    // =========================================================================
    // Services (inyectados / instanciados en el constructor)
    // =========================================================================

    private final AgendaService      agendaService;
    private final TurnoService       turnoService;
    private final ConsultaMedicaService consultaService;
    private final InventarioService  inventarioService;
    private final AlertaService      alertaService;
    private final PacienteService    pacienteService;
    private final HistorialService   historialService;
    private final AgendaDiaService   agendaDiaService;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor de la ventana principal.
     * Instancia todos los Services, aplica el Look and Feel del sistema,
     * construye la interfaz y muestra el dashboard inicial.
     */
    public VentanaPrincipal() {
        // --- Verificar conexión a la BD (DatabaseInitializer ya la creó si no existía) ---
        if (!ConexionBD.probarConexion()) {
            JOptionPane.showMessageDialog(this,
                "No se pudo conectar a la base de datos 'sigvet'.\n\n"
                + "La base de datos debería haber sido creada automáticamente.\n"
                + "Si el problema persiste, verifique que:\n"
                + "  1. MySQL Server esté ejecutándose\n"
                + "  2. El usuario/contraseña en sigvet_config.properties sean correctos\n"
                + "  3. El puerto 3306 esté disponible",
                "Error de Conexión — SIGVET",
                JOptionPane.ERROR_MESSAGE);
        }

        // --- Instanciar todos los Services ---
        this.agendaService     = new AgendaService();
        this.turnoService      = new TurnoService();
        this.consultaService   = new ConsultaMedicaService();
        this.inventarioService = new InventarioService();
        this.alertaService     = new AlertaService();
        this.pacienteService   = new PacienteService();
        this.historialService  = new HistorialService();
        this.agendaDiaService  = new AgendaDiaService();

        // --- Generar slots para los próximos 7 días (automático, sin duplicados) ---
        try {
            agendaService.generarSlotsProximosDias(7);
        } catch (Exception e) {
            System.err.println("[SIGVET] Advertencia al generar slots: " + e.getMessage());
        }

        // --- Aplicar Look and Feel del sistema operativo ---
        aplicarLookAndFeel();

        // --- Construir interfaz ---
        configurarVentana();
        construirMenuBar();
        construirDesktopPane();
        construirBarraEstado();
        construirBarraHerramientas();

        // --- Mostrar dashboard inicial ---
        mostrarDashboard();

        // --- Iniciar timer de actualización de hora ---
        iniciarTimer();
    }


    // =========================================================================
    // Métodos de construcción de la UI
    // =========================================================================

    /**
     * Aplica el Look and Feel del sistema operativo.
     * En caso de error, continúa con el L&F por defecto de Java.
     */
    private void aplicarLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Si falla, continuar con el L&F por defecto (Metal)
            System.err.println("No se pudo aplicar el L&F del sistema: " + e.getMessage());
        }
    }

    /**
     * Configura las propiedades básicas del JFrame.
     */
    private void configurarVentana() {
        setTitle(TITULO);
        setSize(ANCHO, ALTO);
        setLocationRelativeTo(null); // centrar en pantalla
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        // Confirmación al cerrar
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmarSalida();
            }
        });
    }

    /**
     * Construye y configura el {@link JMenuBar} con todos los menús del sistema.
     */
    private void construirMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // ── Menú Archivo ──────────────────────────────────────────────────────
        JMenu menuArchivo = new JMenu("Archivo");
        menuArchivo.setMnemonic(KeyEvent.VK_A);

        JMenuItem itemDashboard = new JMenuItem("Dashboard");
        itemDashboard.setIcon(crearIconoTexto("🏠"));
        itemDashboard.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
        itemDashboard.addActionListener(e -> mostrarDashboard());

        menuArchivo.add(itemDashboard);
        menuArchivo.addSeparator();

        JMenuItem itemSalir = new JMenuItem("Salir");
        itemSalir.setIcon(crearIconoTexto("✕"));
        itemSalir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        itemSalir.addActionListener(e -> confirmarSalida());

        menuArchivo.add(itemSalir);

        // ── Menú Agenda ───────────────────────────────────────────────────────
        JMenu menuAgenda = new JMenu("Agenda");
        menuAgenda.setMnemonic(KeyEvent.VK_G);

        JMenuItem itemConfigAgenda = new JMenuItem("Configurar Agenda");
        itemConfigAgenda.setIcon(crearIconoTexto("📅"));
        itemConfigAgenda.addActionListener(e -> abrirConfiguracionAgenda());

        JMenuItem itemAgendaDia = new JMenuItem("Agenda del Día");
        itemAgendaDia.setIcon(crearIconoTexto("📋"));
        itemAgendaDia.addActionListener(e -> abrirAgendaDelDia());

        menuAgenda.add(itemConfigAgenda);
        menuAgenda.add(itemAgendaDia);

        // ── Menú Turnos ───────────────────────────────────────────────────────
        JMenu menuTurnos = new JMenu("Turnos");
        menuTurnos.setMnemonic(KeyEvent.VK_T);

        JMenuItem itemGestTurnos = new JMenuItem("Gestionar Turnos");
        itemGestTurnos.setIcon(crearIconoTexto("🕐"));
        itemGestTurnos.addActionListener(e -> abrirGestionTurnos());

        menuTurnos.add(itemGestTurnos);

        // ── Menú Clínica ──────────────────────────────────────────────────────
        JMenu menuClinica = new JMenu("Clínica");
        menuClinica.setMnemonic(KeyEvent.VK_C);

        JMenuItem itemConsulta = new JMenuItem("Registrar Consulta");
        itemConsulta.setIcon(crearIconoTexto("🩺"));
        itemConsulta.addActionListener(e -> abrirRegistrarConsulta());

        JMenuItem itemHistorial = new JMenuItem("Historial Clínico");
        itemHistorial.setIcon(crearIconoTexto("📖"));
        itemHistorial.addActionListener(e -> abrirHistorialClinico());

        menuClinica.add(itemConsulta);
        menuClinica.add(itemHistorial);

        // ── Menú Inventario ───────────────────────────────────────────────────
        JMenu menuInventario = new JMenu("Inventario");
        menuInventario.setMnemonic(KeyEvent.VK_I);

        JMenuItem itemInventario = new JMenuItem("Gestionar Inventario");
        itemInventario.setIcon(crearIconoTexto("📦"));
        itemInventario.addActionListener(e -> abrirGestionInventario());

        JMenuItem itemAlertas = new JMenuItem("Alertas de Stock");
        itemAlertas.setIcon(crearIconoTexto("⚠"));
        itemAlertas.addActionListener(e -> abrirAlertasStock());

        menuInventario.add(itemInventario);
        menuInventario.add(itemAlertas);

        // ── Menú Pacientes ────────────────────────────────────────────────────
        JMenu menuPacientes = new JMenu("Pacientes");
        menuPacientes.setMnemonic(KeyEvent.VK_P);

        JMenuItem itemPacientes = new JMenuItem("Gestionar Dueños y Mascotas");
        itemPacientes.setIcon(crearIconoTexto("🐾"));
        itemPacientes.addActionListener(e -> abrirGestionPacientes());

        menuPacientes.add(itemPacientes);

        // ── Agregar menús al menuBar ──────────────────────────────────────────
        menuBar.add(menuArchivo);
        menuBar.add(menuAgenda);
        menuBar.add(menuTurnos);
        menuBar.add(menuClinica);
        menuBar.add(menuInventario);
        menuBar.add(menuPacientes);

        setJMenuBar(menuBar);
    }

    /**
     * Construye el {@link JDesktopPane} que actúa como área de trabajo MDI.
     */
    private void construirDesktopPane() {
        desktopPane = new JDesktopPane();
        desktopPane.setBackground(new Color(45, 52, 70));
        add(desktopPane, BorderLayout.CENTER);
    }

    /**
     * Construye la barra de estado inferior con información del sistema.
     */
    private void construirBarraEstado() {
        JPanel barraEstado = new JPanel(new BorderLayout());
        barraEstado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        barraEstado.setBackground(new Color(240, 240, 240));

        // Panel izquierdo: conexión BD
        JLabel lblConexion = new JLabel("🗄 Conectado a: sigvet");
        lblConexion.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblConexion.setForeground(new Color(0, 128, 0));

        // Panel central: usuario
        JLabel lblUsuario = new JLabel("👤 Usuario: Admin", SwingConstants.CENTER);
        lblUsuario.setFont(new Font("SansSerif", Font.PLAIN, 11));

        // Panel derecho: hora actual
        lblHora = new JLabel(LocalDateTime.now().format(FORMATO_FECHA), SwingConstants.RIGHT);
        lblHora.setFont(new Font("SansSerif", Font.PLAIN, 11));

        barraEstado.add(lblConexion, BorderLayout.WEST);
        barraEstado.add(lblUsuario,  BorderLayout.CENTER);
        barraEstado.add(lblHora,     BorderLayout.EAST);

        add(barraEstado, BorderLayout.SOUTH);
    }

    /**
     * Construye la barra de herramientas de acceso rápido.
     */
    private void construirBarraHerramientas() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));

        JButton btnDashboard = crearBotonToolbar("🏠 Dashboard", e -> mostrarDashboard());
        JButton btnAgenda    = crearBotonToolbar("📅 Agenda",    e -> abrirConfiguracionAgenda());
        JButton btnTurnos    = crearBotonToolbar("🕐 Turnos",    e -> abrirGestionTurnos());
        JButton btnConsulta  = crearBotonToolbar("🩺 Consulta",  e -> abrirRegistrarConsulta());
        JButton btnInventario = crearBotonToolbar("📦 Inventario", e -> abrirGestionInventario());

        toolBar.add(btnDashboard);
        toolBar.addSeparator();
        toolBar.add(btnAgenda);
        toolBar.add(btnTurnos);
        toolBar.add(btnConsulta);
        toolBar.add(btnInventario);

        add(toolBar, BorderLayout.NORTH);
    }

    // =========================================================================
    // Métodos de apertura de ventanas internas
    // =========================================================================

    /**
     * Muestra (o crea) el panel de dashboard en el área de trabajo MDI.
     */
    public void mostrarDashboard() {
        // Verificar si ya hay un dashboard abierto
        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            if (frame instanceof PanelDashboard) {
                try {
                    frame.setSelected(true);
                    frame.setIcon(false);
                } catch (java.beans.PropertyVetoException ex) {
                    // Ignorar
                }
                ((PanelDashboard) frame).actualizarDatos();
                return;
            }
        }
        // Crear nuevo dashboard
        PanelDashboard dashboard = new PanelDashboard(
            agendaService, turnoService, inventarioService,
            alertaService, pacienteService, historialService,
            agendaDiaService,
            this // referencia para abrir otros módulos desde el dashboard
        );
        dashboard.setSize(900, 650);
        dashboard.setLocation(10, 10);
        desktopPane.add(dashboard);
        dashboard.setVisible(true);
        try {
            dashboard.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            // Ignorar
        }
    }

    /**
     * Abre el diálogo de configuración de agenda (CU-01).
     */
    public void abrirConfiguracionAgenda() {
        ConfigurarAgendaDialog dialog = new ConfigurarAgendaDialog(agendaService);
        dialog.setSize(800, 600);
        dialog.setLocation(calcularPosicionCentral(800, 600));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try {
            dialog.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            // Ignorar
        }
    }

    /**
     * Abre el diálogo de gestión de turnos (CU-02).
     */
    public void abrirGestionTurnos() {
        GestionarTurnosDialog dialog = new GestionarTurnosDialog(turnoService, pacienteService);
        dialog.setSize(1000, 650);
        dialog.setLocation(calcularPosicionCentral(1000, 650));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try {
            dialog.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            // Ignorar
        }
    }

    /**
     * Abre el diálogo de registro de consulta médica (CU-03).
     */
    public void abrirRegistrarConsulta() {
        RegistrarConsultaDialog dialog = new RegistrarConsultaDialog(
            consultaService, agendaService, pacienteService);
        dialog.setSize(1050, 700);
        dialog.setLocation(calcularPosicionCentral(1050, 700));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try {
            dialog.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            // Ignorar
        }
    }

    /**
     * Abre el diálogo de gestión de inventario y stock (CU-04).
     */
    public void abrirGestionInventario() {
        GestionarInventarioDialog dialog = new GestionarInventarioDialog(inventarioService);
        dialog.setSize(980, 680);
        dialog.setLocation(calcularPosicionCentral(980, 680));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try {
            dialog.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            // Ignorar
        }
    }

    // =========================================================================
    // Métodos auxiliares
    // =========================================================================

    /**
     * Abre el diálogo de alertas de stock (CU-05).
     */
    public void abrirAlertasStock() {
        AlertasStockDialog dialog = new AlertasStockDialog(alertaService);
        dialog.setSize(900, 580);
        dialog.setLocation(calcularPosicionCentral(900, 580));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try { dialog.setSelected(true); }
        catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
    }

    /**
     * Abre el diálogo de gestión de dueños y mascotas (CU-06).
     */
    public void abrirGestionPacientes() {
        GestionarPacientesDialog dialog = new GestionarPacientesDialog(pacienteService);
        dialog.setSize(1050, 700);
        dialog.setLocation(calcularPosicionCentral(1050, 700));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try { dialog.setSelected(true); }
        catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
    }

    /**
     * Abre el diálogo de historial clínico (CU-07).
     */
    public void abrirHistorialClinico() {
        HistorialClinicoDialog dialog = new HistorialClinicoDialog(historialService);
        dialog.setSize(1100, 680);
        dialog.setLocation(calcularPosicionCentral(1100, 680));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try { dialog.setSelected(true); }
        catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
    }

    /**
     * Abre el diálogo de agenda del día (CU-08).
     */
    public void abrirAgendaDelDia() {
        AgendaDelDiaDialog dialog = new AgendaDelDiaDialog(
            agendaDiaService, agendaService, this);
        dialog.setSize(1000, 620);
        dialog.setLocation(calcularPosicionCentral(1000, 620));
        desktopPane.add(dialog);
        dialog.setVisible(true);
        try { dialog.setSelected(true); }
        catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
    }


    /**
     * Inicia un {@link Timer} que actualiza el label de hora cada 60 segundos.
     */
    private void iniciarTimer() {
        Timer timer = new Timer(60_000, e ->
            lblHora.setText(LocalDateTime.now().format(FORMATO_FECHA))
        );
        timer.setInitialDelay(0);
        timer.start();
    }

    /**
     * Muestra un diálogo de confirmación antes de cerrar la aplicación.
     */
    private void confirmarSalida() {
        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro que desea salir del sistema SIGVET?",
            "Confirmar salida",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    /**
     * Calcula la posición para centrar un JInternalFrame en el JDesktopPane.
     *
     * @param ancho ancho del frame interno
     * @param alto  alto del frame interno
     * @return punto de posición para centrar
     */
    private Point calcularPosicionCentral(int ancho, int alto) {
        int x = Math.max(0, (desktopPane.getWidth()  - ancho) / 2);
        int y = Math.max(0, (desktopPane.getHeight() - alto)  / 2);
        return new Point(x, y);
    }


    /**
     * Crea un botón para la barra de herramientas con el texto dado.
     *
     * @param texto    texto del botón
     * @param listener acción al hacer clic
     * @return botón configurado
     */
    private JButton crearBotonToolbar(String texto, ActionListener listener) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.addActionListener(listener);
        return btn;
    }

    /**
     * Crea un icono de texto (emoji) como {@link ImageIcon} vacío
     * (los emojis se renderizan directamente en el texto del item).
     *
     * @param texto emoji o símbolo a mostrar
     * @return siempre {@code null} (los ítems de menú muestran el emoji en el texto)
     */
    private Icon crearIconoTexto(String texto) {
        return null; // Los menús usan el emoji en el texto del item directamente
    }

    // =========================================================================
    // Punto de entrada — método main
    // =========================================================================

    /**
     * Punto de entrada principal de la aplicación SIGVET.
     *
     * <p>Se ejecuta en el Event Dispatch Thread (EDT) de Swing para garantizar
     * la seguridad de hilo de la interfaz gráfica.</p>
     *
     * @param args argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Verificar conexión a BD antes de abrir la ventana
            if (!ConexionBD.probarConexion()) {
                JOptionPane.showMessageDialog(
                    null,
                    "No se pudo conectar a la base de datos MySQL.\n"
                    + "Verifique que MySQL esté iniciado y que la configuración\n"
                    + "en sigvet_config.properties sea correcta.",
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}
