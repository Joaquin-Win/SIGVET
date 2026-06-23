package com.sigvet.view;

import com.sigvet.model.Veterinario;
import com.sigvet.model.dto.TurnoDelDiaDTO;
import com.sigvet.model.enums.EstadoTurno;
import com.sigvet.service.AgendaDiaService;
import com.sigvet.service.AgendaService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para el caso de uso CU-08: Consultar Agenda del Día.
 *
 * <p>Muestra todos los turnos del día actual en una tabla codificada por colores
 * según el estado del turno. Permite filtrar por veterinario y refrescar datos.
 * Esta pantalla es de <strong>solo lectura</strong>: para gestionar turnos, se
 * debe ir al CU-02 ({@link GestionarTurnosDialog}).</p>
 *
 * <p>Los datos se obtienen de la vista de BD {@code v_agenda_dia} a través del
 * {@link AgendaDiaService}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see AgendaDiaService
 */
public class AgendaDelDiaDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Columnas de la tabla de turnos del día. */
    private static final String[] COLUMNAS = {
        "ID", "Hora", "Mascota", "Especie/Raza", "Dueño", "Teléfono", "Veterinario", "Estado", "Motivo"
    };

    /** Formato de fecha para el encabezado. */
    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM 'de' yyyy",
            new java.util.Locale("es", "AR"));

    // =========================================================================
    // Services
    // =========================================================================

    private final AgendaDiaService agendaDiaService;
    private final AgendaService    agendaService;
    /** Referencia a la ventana principal para abrir CU-02. */
    private final VentanaPrincipal ventanaPrincipal;

    // =========================================================================
    // Componentes
    // =========================================================================

    private JComboBox<String>   cmbVeterinario;
    private JTable              tablaAgenda;
    private DefaultTableModel   modeloAgenda;
    private JLabel              lblFecha;
    private JLabel              lblResumen;
    private JLabel              lblPendientes;
    private JLabel              lblAtendidos;
    private JLabel              lblCancelados;

    /** Lista de nombres de veterinarios para el combo. */
    private final List<String> nombresVeterinarios = new ArrayList<>();

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de agenda del día.
     *
     * @param agendaDiaService  servicio de agenda del día
     * @param agendaService     servicio de agenda (para cargar lista de veterinarios)
     * @param ventanaPrincipal  referencia a la ventana principal para abrir otros módulos
     */
    public AgendaDelDiaDialog(AgendaDiaService agendaDiaService,
                               AgendaService    agendaService,
                               VentanaPrincipal ventanaPrincipal) {
        super("Agenda del Día", true, true, true, true);
        this.agendaDiaService  = agendaDiaService;
        this.agendaService     = agendaService;
        this.ventanaPrincipal  = ventanaPrincipal;

        construirUI();
        cargarVeterinariosEnCombo();
        cargarAgendaDelDia();
    }

    // =========================================================================
    // Construcción de la UI
    // =========================================================================

    /**
     * Construye la interfaz gráfica completa del diálogo.
     */
    private void construirUI() {
        setLayout(new BorderLayout(0, 6));
        getContentPane().setBackground(new Color(248, 249, 252));

        add(construirPanelNorte(),  BorderLayout.NORTH);
        add(construirPanelTabla(),  BorderLayout.CENTER);
        add(construirPanelSur(),    BorderLayout.SOUTH);
    }

    /**
     * Construye el panel superior con la fecha, filtro de veterinario y resumen.
     *
     * @return panel norte
     */
    private JPanel construirPanelNorte() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(new Color(30, 50, 100));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Encabezado: fecha del día
        String fechaStr = LocalDate.now().format(FMT_FECHA);
        // Capitalizar primera letra
        if (!fechaStr.isEmpty()) {
            fechaStr = fechaStr.substring(0, 1).toUpperCase() + fechaStr.substring(1);
        }
        lblFecha = new JLabel("📅 Agenda del Día — " + fechaStr);
        lblFecha.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblFecha.setForeground(Color.WHITE);

        // Panel de filtros
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelFiltros.setBackground(new Color(30, 50, 100));

        JLabel lblVet = new JLabel("Filtrar por veterinario:");
        lblVet.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblVet.setForeground(Color.WHITE);

        cmbVeterinario = new JComboBox<>();
        cmbVeterinario.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbVeterinario.setPreferredSize(new Dimension(230, 26));
        cmbVeterinario.addActionListener(e -> aplicarFiltroVeterinario());

        JButton btnActualizar = new JButton("🔄 Actualizar");
        btnActualizar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnActualizar.setFocusPainted(false);
        btnActualizar.addActionListener(e -> cargarAgendaDelDia());

        panelFiltros.add(lblVet);
        panelFiltros.add(cmbVeterinario);
        panelFiltros.add(btnActualizar);

        // Labels de resumen
        JPanel panelResumen = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        panelResumen.setBackground(new Color(30, 50, 100));

        lblResumen   = crearLabelResumen("Total: —");
        lblPendientes = crearLabelResumen("⏳ Pendientes: —");
        lblAtendidos  = crearLabelResumen("✅ Atendidos: —");
        lblCancelados = crearLabelResumen("❌ Cancelados: —");

        panelResumen.add(lblResumen);
        panelResumen.add(new JLabel("|") {{setForeground(Color.WHITE);}});
        panelResumen.add(lblPendientes);
        panelResumen.add(new JLabel("|") {{setForeground(Color.WHITE);}});
        panelResumen.add(lblAtendidos);
        panelResumen.add(new JLabel("|") {{setForeground(Color.WHITE);}});
        panelResumen.add(lblCancelados);

        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setBackground(new Color(30, 50, 100));
        panelSur.add(panelFiltros, BorderLayout.NORTH);
        panelSur.add(panelResumen, BorderLayout.SOUTH);

        panel.add(lblFecha,   BorderLayout.NORTH);
        panel.add(panelSur,   BorderLayout.CENTER);

        return panel;
    }

    /**
     * Construye el panel central con la tabla de turnos del día.
     *
     * @return panel de tabla
     */
    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Turnos del Día  (🟢 Pendiente  🔵 Atendido  ⚫ Cancelado  🔴 Inasistencia)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
            panel.getBorder(),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        modeloAgenda = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaAgenda = new JTable(modeloAgenda);
        tablaAgenda.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaAgenda.setRowHeight(24);
        tablaAgenda.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaAgenda.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaAgenda.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaAgenda.setFillsViewportHeight(true);

        // Ocultar columna ID
        tablaAgenda.getColumnModel().getColumn(0).setMinWidth(0);
        tablaAgenda.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaAgenda.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 70, 130, 110, 150, 110, 170, 90, 160};
        for (int i = 1; i < anchos.length; i++) {
            tablaAgenda.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Aplicar renderer personalizado para estados
        TurnoDiaCellRenderer renderer = new TurnoDiaCellRenderer();
        for (int i = 0; i < tablaAgenda.getColumnCount(); i++) {
            tablaAgenda.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane scroll = new JScrollPane(tablaAgenda);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel inferior con los botones de acción.
     *
     * @return panel sur
     */
    private JPanel construirPanelSur() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 244, 252));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        izq.setBackground(new Color(240, 244, 252));

        JButton btnGestionarTurnos = new JButton("🕐 Ir a Gestionar Turnos (CU-02)");
        btnGestionarTurnos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnGestionarTurnos.setBackground(new Color(50, 100, 200));
        btnGestionarTurnos.setForeground(Color.WHITE);
        btnGestionarTurnos.setFocusPainted(false);
        btnGestionarTurnos.addActionListener(e -> {
            if (ventanaPrincipal != null) ventanaPrincipal.abrirGestionTurnos();
        });

        JButton btnRefrescar = new JButton("🔄 Actualizar Agenda");
        btnRefrescar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnRefrescar.setFocusPainted(false);
        btnRefrescar.addActionListener(e -> cargarAgendaDelDia());

        izq.add(btnGestionarTurnos);
        izq.add(btnRefrescar);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        der.setBackground(new Color(240, 244, 252));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCerrar.addActionListener(e -> {
            try { setClosed(true); }
            catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
        });
        der.add(btnCerrar);

        panel.add(izq, BorderLayout.WEST);
        panel.add(der, BorderLayout.EAST);
        return panel;
    }

    // =========================================================================
    // Carga y filtrado de datos
    // =========================================================================

    /**
     * Carga los veterinarios activos en el combo de filtro.
     * Agrega la opción "Todos" como primera entrada.
     */
    private void cargarVeterinariosEnCombo() {
        cmbVeterinario.removeAllItems();
        nombresVeterinarios.clear();
        cmbVeterinario.addItem("Todos los veterinarios");

        try {
            List<Veterinario> vets = agendaService.obtenerVeterinariosActivos();
            for (Veterinario v : vets) {
                String nombre = v.getApellido() + ", " + v.getNombre();
                cmbVeterinario.addItem(nombre);
                nombresVeterinarios.add(nombre);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar veterinarios: " + e.getMessage());
        }
    }

    /**
     * Carga todos los turnos del día actual desde la vista de BD y llena la tabla.
     * Actualiza los contadores de resumen.
     */
    public void cargarAgendaDelDia() {
        modeloAgenda.setRowCount(0);

        try {
            List<TurnoDelDiaDTO> turnos = agendaDiaService.obtenerTurnosDelDia();
            llenarTablaConTurnos(turnos);
            actualizarContadores(turnos);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar la agenda del día:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aplica el filtro por veterinario seleccionado en el combo.
     * Si está seleccionado "Todos", carga todos los turnos del día.
     * Si está seleccionado un veterinario específico, consulta la vista filtrada.
     */
    private void aplicarFiltroVeterinario() {
        String seleccion = (String) cmbVeterinario.getSelectedItem();
        if (seleccion == null || seleccion.equals("Todos los veterinarios")) {
            cargarAgendaDelDia();
            return;
        }

        modeloAgenda.setRowCount(0);

        try {
            List<TurnoDelDiaDTO> turnos =
                agendaDiaService.obtenerTurnosDelDiaPorVeterinario(seleccion);
            llenarTablaConTurnos(turnos);
            actualizarContadores(turnos);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al filtrar por veterinario:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Llena la tabla de agenda con la lista de turnos del día.
     *
     * @param turnos lista de {@link TurnoDelDiaDTO} a mostrar
     */
    private void llenarTablaConTurnos(List<TurnoDelDiaDTO> turnos) {
        modeloAgenda.setRowCount(0);

        // Estructuras repetitivas: for-each para llenar la tabla
        for (TurnoDelDiaDTO t : turnos) {
            String hora    = t.getHora() != null ? t.getHora().toString().substring(0, 5) : "—";
            String esraza  = t.getEspecie() != null ? t.getEspecie() : "—";
            if (t.getRaza() != null) esraza += " / " + t.getRaza();
            String estado  = t.getEstado() != null ? t.getEstado().name() : "—";

            modeloAgenda.addRow(new Object[]{
                t.getIdTurno(),
                hora,
                t.getNombreMascota()    != null ? t.getNombreMascota()    : "—",
                esraza,
                t.getNombreDueno()      != null ? t.getNombreDueno()      : "—",
                t.getTelefonoDueno()    != null ? t.getTelefonoDueno()    : "—",
                t.getNombreVeterinario() != null ? t.getNombreVeterinario() : "—",
                estado,
                t.getMotivo()           != null ? t.getMotivo()           : "—"
            });
        }
    }

    /**
     * Actualiza los labels de resumen con los conteos de turnos por estado.
     *
     * @param turnos lista de turnos a contabilizar
     */
    private void actualizarContadores(List<TurnoDelDiaDTO> turnos) {
        int total = turnos.size();
        long pend = turnos.stream()
            .filter(t -> t.getEstado() == EstadoTurno.Pendiente).count();
        long aten = turnos.stream()
            .filter(t -> t.getEstado() == EstadoTurno.Atendido).count();
        long canc = turnos.stream()
            .filter(t -> t.getEstado() == EstadoTurno.Cancelado).count();

        lblResumen.setText("Total: " + total);
        lblPendientes.setText("⏳ Pendientes: " + pend);
        lblAtendidos.setText("✅ Atendidos: " + aten);
        lblCancelados.setText("❌ Cancelados: " + canc);
    }

    // =========================================================================
    // Auxiliares
    // =========================================================================

    /**
     * Crea un {@link JLabel} con estilo de resumen (blanco sobre azul oscuro).
     *
     * @param texto texto inicial del label
     * @return label configurado
     */
    private JLabel crearLabelResumen(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    // =========================================================================
    // Clase interna: TurnoDiaCellRenderer
    // =========================================================================

    /**
     * Renderer personalizado para la tabla de agenda del día.
     * Colorea las filas según el estado del turno (columna 7):
     * <ul>
     *   <li>{@code Pendiente} → fondo verde claro</li>
     *   <li>{@code Atendido} → fondo azul claro</li>
     *   <li>{@code Cancelado} → fondo gris claro</li>
     *   <li>{@code Inasistencia} → fondo rojo claro</li>
     * </ul>
     */
    static class TurnoDiaCellRenderer extends DefaultTableCellRenderer {

        private static final Color COLOR_PENDIENTE   = new Color(210, 255, 210);
        private static final Color COLOR_ATENDIDO    = new Color(210, 230, 255);
        private static final Color COLOR_CANCELADO   = new Color(220, 220, 220);
        private static final Color COLOR_INASISTENCIA = new Color(255, 210, 210);

        /**
         * {@inheritDoc}
         *
         * <p>Lee la columna 7 (Estado) del modelo para determinar el color de fondo.</p>
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                Object estadoObj = table.getModel().getValueAt(row, 7);
                String estado = estadoObj != null ? estadoObj.toString() : "";

                // Estructuras condicionales: colorear según estado
                switch (estado) {
                    case "Pendiente"    -> c.setBackground(COLOR_PENDIENTE);
                    case "Atendido"     -> c.setBackground(COLOR_ATENDIDO);
                    case "Cancelado"    -> c.setBackground(COLOR_CANCELADO);
                    case "Inasistencia" -> c.setBackground(COLOR_INASISTENCIA);
                    default             -> c.setBackground(Color.WHITE);
                }
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
