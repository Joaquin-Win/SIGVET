package com.sigvet.view;

import com.sigvet.model.AlertaStock;
import com.sigvet.model.dto.TurnoDelDiaDTO;
import com.sigvet.model.enums.EstadoTurno;
import com.sigvet.model.enums.TipoAlerta;
import com.sigvet.service.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel principal del dashboard de SIGVET.
 *
 * <p>Se muestra dentro del {@link JDesktopPane} como el primer {@link JInternalFrame}
 * al iniciar la aplicación. Presenta:</p>
 * <ul>
 *   <li>Resumen de los turnos del día ({@code vw_turnos_del_dia}).</li>
 *   <li>Panel de alertas activas de stock con indicadores visuales por color.</li>
 *   <li>Botones de acceso rápido a los módulos más utilizados.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see AgendaDiaService
 * @see AlertaService
 */
public class PanelDashboard extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Columnas para la tabla de turnos del día. */
    private static final String[] COLUMNAS_TURNOS = {
        "Hora", "Mascota", "Especie", "Dueño", "Teléfono", "Veterinario", "Estado"
    };

    /** Formato de fecha para el encabezado. */
    private static final DateTimeFormatter FORMATO_FECHA =
        DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
            java.util.Locale.forLanguageTag("es-AR"));

    // =========================================================================
    // Services
    // =========================================================================

    private final AgendaService      agendaService;
    private final TurnoService       turnoService;
    private final InventarioService  inventarioService;
    private final AlertaService      alertaService;
    private final PacienteService    pacienteService;
    private final HistorialService   historialService;
    private final AgendaDiaService   agendaDiaService;
    private final VentanaPrincipal   ventanaPadre;

    // =========================================================================
    // Componentes de UI
    // =========================================================================

    /** Modelo de la tabla de turnos del día. */
    private DefaultTableModel modeloTurnos;

    /** Tabla de turnos del día. */
    private JTable tablaTurnos;

    /** Panel de alertas activas (se reconstruye en cada actualización). */
    private JPanel panelAlertas;

    /** Label con el total de turnos del día. */
    private JLabel lblResumenTurnos;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del panel de dashboard.
     *
     * @param agendaService     servicio de agenda de veterinarios
     * @param turnoService      servicio de turnos
     * @param inventarioService servicio de inventario
     * @param alertaService     servicio de alertas de stock
     * @param pacienteService   servicio de dueños y mascotas
     * @param historialService  servicio de historial clínico
     * @param agendaDiaService  servicio de agenda del día
     * @param ventanaPadre      referencia a la ventana principal para navegación
     */
    public PanelDashboard(
            AgendaService agendaService,
            TurnoService turnoService,
            InventarioService inventarioService,
            AlertaService alertaService,
            PacienteService pacienteService,
            HistorialService historialService,
            AgendaDiaService agendaDiaService,
            VentanaPrincipal ventanaPadre) {

        super("Dashboard — " + LocalDate.now().format(FORMATO_FECHA),
              true,   // resizable
              true,   // closable
              true,   // maximizable
              true);  // iconifiable

        this.agendaService     = agendaService;
        this.turnoService      = turnoService;
        this.inventarioService = inventarioService;
        this.alertaService     = alertaService;
        this.pacienteService   = pacienteService;
        this.historialService  = historialService;
        this.agendaDiaService  = agendaDiaService;
        this.ventanaPadre      = ventanaPadre;

        construirUI();
        actualizarDatos();
    }

    // =========================================================================
    // Construcción de la UI
    // =========================================================================

    /**
     * Construye la estructura principal del panel de dashboard.
     */
    private void construirUI() {
        setLayout(new BorderLayout(0, 8));
        getContentPane().setBackground(new Color(245, 247, 250));

        add(construirPanelNorte(),  BorderLayout.NORTH);
        add(construirPanelCentro(), BorderLayout.CENTER);
        add(construirPanelSur(),    BorderLayout.SOUTH);
    }

    /**
     * Construye el panel norte con el título y subtítulo del dashboard.
     *
     * @return panel norte configurado
     */
    private JPanel construirPanelNorte() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(30, 90, 160));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel lblTitulo = new JLabel("🏥 Bienvenido a SIGVET");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSubtitulo = new JLabel(
            "Sistema de Gestión Clínica y de Stock para Centros Veterinarios");
        lblSubtitulo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSubtitulo.setForeground(new Color(200, 220, 255));
        lblSubtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblResumenTurnos = new JLabel("Cargando resumen...");
        lblResumenTurnos.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblResumenTurnos.setForeground(new Color(180, 210, 255));
        lblResumenTurnos.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lblTitulo);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblSubtitulo);
        panel.add(Box.createVerticalStrut(6));
        panel.add(lblResumenTurnos);

        return panel;
    }

    /**
     * Construye el panel central con la tabla de turnos y el panel de alertas.
     *
     * @return panel central con JSplitPane
     */
    private JPanel construirPanelCentro() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(245, 247, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));

        // ── Tabla de turnos del día ─────────────────────────────────────────
        JPanel panelTurnos = construirPanelTurnos();

        // ── Panel de alertas ────────────────────────────────────────────────
        panelAlertas = new JPanel();
        panelAlertas.setLayout(new BoxLayout(panelAlertas, BoxLayout.Y_AXIS));
        panelAlertas.setBackground(Color.WHITE);
        panelAlertas.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "⚠ Alertas de Stock",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));
        panelAlertas.setPreferredSize(new Dimension(280, 0));

        // JSplitPane: tabla izquierda, alertas derecha
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelTurnos, panelAlertas);
        split.setDividerLocation(620);
        split.setResizeWeight(0.75);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel de la tabla de turnos del día.
     *
     * @return panel con la tabla de turnos
     */
    private JPanel construirPanelTurnos() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "📋 Turnos del Día",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        // Modelo no editable
        modeloTurnos = new DefaultTableModel(COLUMNAS_TURNOS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaTurnos = new JTable(modeloTurnos);
        tablaTurnos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaTurnos.setRowHeight(22);
        tablaTurnos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaTurnos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaTurnos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaTurnos.setFillsViewportHeight(true);

        // Anchos de columnas
        int[] anchos = {65, 120, 90, 140, 110, 140, 90};
        for (int i = 0; i < anchos.length && i < tablaTurnos.getColumnCount(); i++) {
            tablaTurnos.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Renderer para colorear filas según estado
        tablaTurnos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String estado = (String) table.getModel().getValueAt(row, 6);
                    if ("Atendido".equals(estado)) {
                        c.setBackground(new Color(220, 255, 220));
                    } else if ("Cancelado".equals(estado) || "Inasistencia".equals(estado)) {
                        c.setBackground(new Color(255, 230, 230));
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 255));
                    }
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(tablaTurnos);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Botón refrescar
        JButton btnRefrescar = new JButton("🔄 Actualizar");
        btnRefrescar.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnRefrescar.addActionListener((ActionEvent e) -> actualizarDatos());

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        panelBotones.setBackground(Color.WHITE);
        panelBotones.add(btnRefrescar);

        panel.add(scroll,       BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Construye el panel sur con los botones de acceso rápido.
     *
     * @return panel sur con botones de acceso rápido
     */
    private JPanel construirPanelSur() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        panel.setBackground(new Color(235, 240, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 210, 230)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        panel.add(new JLabel("Acceso rápido:"));

        panel.add(crearBotonAcceso("📅 Agenda del Día",
            e -> ventanaPadre.abrirAgendaDelDia()));
        panel.add(crearBotonAcceso("🩺 Registrar Consulta",
            e -> ventanaPadre.abrirRegistrarConsulta()));
        panel.add(crearBotonAcceso("🕐 Gestionar Turnos",
            e -> ventanaPadre.abrirGestionTurnos()));
        panel.add(crearBotonAcceso("⚠ Alertas",
            e -> ventanaPadre.abrirAlertasStock()));

        return panel;
    }

    /**
     * Crea un botón de acceso rápido con estilo consistente.
     *
     * @param texto    texto del botón
     * @param listener acción al hacer clic
     * @return botón configurado
     */
    private JButton crearBotonAcceso(String texto, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setPreferredSize(new Dimension(180, 34));
        btn.setFocusPainted(false);
        btn.addActionListener(listener);
        return btn;
    }

    // =========================================================================
    // Actualización de datos
    // =========================================================================

    /**
     * Actualiza la tabla de turnos y el panel de alertas con datos frescos de la BD.
     *
     * <p>Se llama al abrir el dashboard y al presionar el botón Actualizar.
     * Captura excepciones y muestra mensajes de error sin bloquear la UI.</p>
     */
    public void actualizarDatos() {
        actualizarTablaTurnos();
        actualizarPanelAlertas();
    }

    /**
     * Carga los turnos del día actual desde {@code vw_turnos_del_dia}
     * y los muestra en la tabla.
     */
    private void actualizarTablaTurnos() {
        modeloTurnos.setRowCount(0); // limpiar tabla

        try {
            List<TurnoDelDiaDTO> turnos = agendaDiaService.obtenerTurnosDelDia();

            // Estructuras repetitivas: for-each para llenar la tabla
            for (TurnoDelDiaDTO dto : turnos) {
                String hora = dto.getHora() != null
                    ? dto.getHora().toString().substring(0, 5) : "—";
                String estado = dto.getEstado() != null
                    ? dto.getEstado().name() : "—";

                modeloTurnos.addRow(new Object[]{
                    hora,
                    dto.getNombreMascota(),
                    dto.getEspecie(),
                    dto.getNombreDueno(),
                    dto.getTelefonoDueno(),
                    dto.getNombreVeterinario(),
                    estado
                });
            }

            // Actualizar resumen
            int total     = turnos.size();
            long atendidos = turnos.stream()
                .filter(t -> t.getEstado() == EstadoTurno.Atendido).count();
            long pendientes = turnos.stream()
                .filter(t -> t.getEstado() == EstadoTurno.Pendiente).count();

            lblResumenTurnos.setText(String.format(
                "Hoy: %d turnos en total — %d pendientes — %d atendidos",
                total, pendientes, atendidos
            ));

        } catch (Exception e) {
            lblResumenTurnos.setText("No se pudo cargar la agenda del día.");
            System.err.println("Error al cargar turnos del día: " + e.getMessage());
        }
    }

    /**
     * Actualiza el panel de alertas con los datos actuales de la BD.
     * Muestra indicadores visuales por tipo y cantidad de alertas.
     */
    private void actualizarPanelAlertas() {
        panelAlertas.removeAll();
        panelAlertas.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "⚠ Alertas de Stock",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        try {
            List<AlertaStock> alertas = alertaService.obtenerAlertasActivas();

            if (alertas.isEmpty()) {
                // Sin alertas activas
                JLabel lblOk = new JLabel("✓ Sin alertas activas");
                lblOk.setFont(new Font("SansSerif", Font.BOLD, 13));
                lblOk.setForeground(new Color(0, 150, 0));
                lblOk.setAlignmentX(Component.LEFT_ALIGNMENT);
                lblOk.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
                panelAlertas.add(lblOk);
            } else {
                // Contar por tipo usando for-each con estructuras condicionales
                int stockBajo     = 0;
                int vencProximo   = 0;
                int enGestion     = 0;

                for (AlertaStock alerta : alertas) {
                    if (alerta.getTipo() == TipoAlerta.STOCK_BAJO) {
                        stockBajo++;
                    } else if (alerta.getTipo() == TipoAlerta.VENCIMIENTO_PROXIMO) {
                        vencProximo++;
                    }
                    switch (alerta.getEstado()) {
                        case En_Gestion -> enGestion++;
                        default -> { /* no hace nada */ }
                    }
                }

                // Label de stock bajo (rojo)
                if (stockBajo > 0) {
                    JLabel lblBajo = new JLabel(
                        "⚠ " + stockBajo + " medicamento(s) con stock bajo");
                    lblBajo.setFont(new Font("SansSerif", Font.BOLD, 12));
                    lblBajo.setForeground(new Color(200, 0, 0));
                    lblBajo.setAlignmentX(Component.LEFT_ALIGNMENT);
                    lblBajo.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
                    panelAlertas.add(lblBajo);
                }

                // Label de vencimiento próximo (naranja)
                if (vencProximo > 0) {
                    JLabel lblVenc = new JLabel(
                        "⚠ " + vencProximo + " lote(s) próximo(s) a vencer");
                    lblVenc.setFont(new Font("SansSerif", Font.BOLD, 12));
                    lblVenc.setForeground(new Color(210, 120, 0));
                    lblVenc.setAlignmentX(Component.LEFT_ALIGNMENT);
                    lblVenc.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
                    panelAlertas.add(lblVenc);
                }

                // Label de alertas en gestión (azul)
                if (enGestion > 0) {
                    JLabel lblGest = new JLabel(
                        "ℹ " + enGestion + " alerta(s) en gestión");
                    lblGest.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    lblGest.setForeground(new Color(0, 80, 200));
                    lblGest.setAlignmentX(Component.LEFT_ALIGNMENT);
                    lblGest.setBorder(BorderFactory.createEmptyBorder(2, 10, 8, 10));
                    panelAlertas.add(lblGest);
                }

                panelAlertas.add(Box.createVerticalStrut(8));

                // Listar primeras 5 alertas activas
                int maxMostrar = Math.min(5, alertas.size());
                for (int i = 0; i < maxMostrar; i++) {
                    AlertaStock a = alertas.get(i);
                    JPanel itemAlerta = construirItemAlerta(a);
                    panelAlertas.add(itemAlerta);
                }
                if (alertas.size() > 5) {
                    JLabel lblMas = new JLabel(
                        "... y " + (alertas.size() - 5) + " más. Ver módulo Alertas.");
                    lblMas.setFont(new Font("SansSerif", Font.ITALIC, 11));
                    lblMas.setForeground(Color.DARK_GRAY);
                    lblMas.setBorder(BorderFactory.createEmptyBorder(4, 10, 0, 0));
                    lblMas.setAlignmentX(Component.LEFT_ALIGNMENT);
                    panelAlertas.add(lblMas);
                }
            }

        } catch (Exception e) {
            JLabel lblError = new JLabel("No se pudieron cargar las alertas.");
            lblError.setFont(new Font("SansSerif", Font.ITALIC, 11));
            lblError.setForeground(Color.GRAY);
            lblError.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 0));
            panelAlertas.add(lblError);
            System.err.println("Error al cargar alertas: " + e.getMessage());
        }

        panelAlertas.revalidate();
        panelAlertas.repaint();
    }

    /**
     * Construye un panel visual para representar una alerta individual.
     *
     * @param alerta alerta a representar
     * @return panel con la información de la alerta
     */
    private JPanel construirItemAlerta(AlertaStock alerta) {
        JPanel item = new JPanel(new BorderLayout(4, 0));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        // Color de fondo según tipo
        Color bg;
        if (alerta.getTipo() == TipoAlerta.STOCK_BAJO) {
            bg = new Color(255, 245, 245);
        } else {
            bg = new Color(255, 248, 235);
        }
        item.setBackground(bg);

        // Nombre del medicamento
        String nomMed = alerta.getMedicamento() != null
            ? alerta.getMedicamento().getNombreComercial() : "Desconocido";
        JLabel lblNom = new JLabel(nomMed);
        lblNom.setFont(new Font("SansSerif", Font.BOLD, 11));

        // Mensaje de la alerta (truncado)
        String msg = alerta.getMensaje() != null ? alerta.getMensaje() : "";
        if (msg.length() > 45) msg = msg.substring(0, 42) + "...";
        JLabel lblMsg = new JLabel(msg);
        lblMsg.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lblMsg.setForeground(Color.DARK_GRAY);

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 1));
        textos.setBackground(bg);
        textos.add(lblNom);
        textos.add(lblMsg);

        item.add(textos, BorderLayout.CENTER);
        return item;
    }
}
