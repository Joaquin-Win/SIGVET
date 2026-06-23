package com.sigvet.view;

import com.sigvet.exception.MascotaNoRegistradaException;
import com.sigvet.exception.SlotNoDisponibleException;
import com.sigvet.exception.TurnoOcupadoException;
import com.sigvet.model.Dueno;
import com.sigvet.model.Mascota;
import com.sigvet.model.SlotAgenda;
import com.sigvet.model.Turno;
import com.sigvet.model.Veterinario;
import com.sigvet.service.PacienteService;
import com.sigvet.service.TurnoService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogo para el caso de uso CU-02: Gestionar Turnos.
 *
 * <p>Implementa un {@link JInternalFrame} con dos pestañas ({@link JTabbedPane}):</p>
 * <ul>
 *   <li><strong>Reservar Turno:</strong> busca slots disponibles por veterinario y fecha,
 *       busca dueños (en tiempo real con {@link DocumentListener}), selecciona mascotas,
 *       y llama a {@link TurnoService#reservarTurno} via SP con control de concurrencia.</li>
 *   <li><strong>Cancelar Turno:</strong> muestra turnos pendientes del día y permite
 *       cancelarlos o registrar inasistencia.</li>
 * </ul>
 *
 * <p><strong>Manejo de errores:</strong></p>
 * <ul>
 *   <li>{@link TurnoOcupadoException} → aviso de concurrencia</li>
 *   <li>{@link SlotNoDisponibleException} → aviso de no disponibilidad</li>
 *   <li>{@link MascotaNoRegistradaException} → error de selección</li>
 *   <li>{@link SQLException} → error genérico de BD</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see TurnoService
 * @see PacienteService
 */
public class GestionarTurnosDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Columnas de la tabla de slots disponibles. */
    private static final String[] COLUMNAS_SLOTS = {"ID Slot", "Fecha", "Hora", "Estado"};

    /** Columnas de la tabla de dueños en la búsqueda. */
    private static final String[] COLUMNAS_DUENOS = {"ID", "DNI", "Apellido", "Nombre", "Teléfono"};

    /** Columnas de la tabla de turnos del día (pestaña cancelación). */
    private static final String[] COLUMNAS_TURNOS = {
        "ID", "Fecha", "Hora", "Mascota", "Veterinario", "Estado", "Motivo"
    };

    /** Formato para mostrar fechas. */
    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================================
    // Services
    // =========================================================================

    private final TurnoService   turnoService;
    private final PacienteService pacienteService;

    // =========================================================================
    // Componentes — Pestaña Reservar
    // =========================================================================

    private JComboBox<Veterinario> cmbVeterinario;
    private JComboBox<String>      cmbFecha;
    private JTable                 tablaSlots;
    private DefaultTableModel      modeloSlots;
    private java.util.List<Integer> idsSlots;

    private JLabel                 lblSlotSeleccionado;
    private JTextField             txtBuscarDueno;
    private JTable                 tablaDuenos;
    private DefaultTableModel      modeloDuenos;
    private java.util.List<Dueno>  duenosEncontrados;
    private JComboBox<Mascota>     cmbMascota;
    private JTextArea              txtMotivo;
    private JButton                btnReservar;

    // =========================================================================
    // Componentes — Pestaña Cancelar
    // =========================================================================

    private JTable            tablaTurnos;
    private DefaultTableModel modeloTurnos;
    private java.util.List<Integer> idsTurnos;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de gestión de turnos.
     *
     * @param turnoService    servicio de turnos inyectado
     * @param pacienteService servicio de pacientes para buscar dueños/mascotas
     */
    public GestionarTurnosDialog(TurnoService turnoService, PacienteService pacienteService) {
        super("Gestionar Turnos", true, true, true, true);

        this.turnoService    = turnoService;
        this.pacienteService = pacienteService;
        this.idsSlots        = new java.util.ArrayList<>();
        this.duenosEncontrados = new java.util.ArrayList<>();
        this.idsTurnos       = new java.util.ArrayList<>();

        construirUI();
        cargarVeterinarios();
        cargarFechasSemana();
    }

    // =========================================================================
    // Construcción de la UI
    // =========================================================================

    /**
     * Construye la interfaz gráfica con el {@link JTabbedPane} de dos pestañas.
     */
    private void construirUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 249, 252));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.addTab("🕐 Reservar Turno",  construirTabReservar());
        tabs.addTab("❌ Cancelar / Inasistencia", construirTabCancelar());

        // Al cambiar de pestaña, cargar datos relevantes
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                cargarTurnosPendientes();
            }
        });

        add(tabs, BorderLayout.CENTER);
        add(construirBarraBotonesGlobal(), BorderLayout.SOUTH);
    }

    // =========================================================================
    // Pestaña 1: Reservar Turno
    // =========================================================================

    /**
     * Construye el panel completo de la pestaña "Reservar Turno".
     *
     * @return panel de la pestaña de reserva
     */
    private JPanel construirTabReservar() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // JSplitPane: slots disponibles (izq) | formulario reserva (der)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(construirPanelBusquedaSlots());
        split.setRightComponent(construirPanelFormularioReserva());
        split.setDividerLocation(400);
        split.setResizeWeight(0.4);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel izquierdo de búsqueda de slots disponibles.
     *
     * @return panel de búsqueda
     */
    private JPanel construirPanelBusquedaSlots() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 240)),
            "Slots Disponibles",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        // Panel de filtros
        JPanel panelFiltros = new JPanel(new GridBagLayout());
        panelFiltros.setBackground(Color.WHITE);
        panelFiltros.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panelFiltros.add(new JLabel("Veterinario:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbVeterinario = new JComboBox<>();
        cmbVeterinario.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panelFiltros.add(cmbVeterinario, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panelFiltros.add(new JLabel("Fecha:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbFecha = new JComboBox<>();
        cmbFecha.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panelFiltros.add(cmbFecha, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        JButton btnBuscar = new JButton("🔍 Buscar Disponibles");
        btnBuscar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnBuscar.addActionListener(e -> buscarSlotsDisponibles());
        panelFiltros.add(btnBuscar, gbc);

        // Tabla de slots
        modeloSlots = new DefaultTableModel(COLUMNAS_SLOTS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaSlots = new JTable(modeloSlots);
        tablaSlots.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaSlots.setRowHeight(22);
        tablaSlots.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaSlots.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaSlots.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // Al seleccionar slot → actualizar label
        tablaSlots.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarLabelSlotSeleccionado();
        });

        // Ocultar columna ID (col 0)
        tablaSlots.getColumnModel().getColumn(0).setMinWidth(0);
        tablaSlots.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaSlots.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane scroll = new JScrollPane(tablaSlots);
        scroll.setPreferredSize(new Dimension(0, 200));

        panel.add(panelFiltros, BorderLayout.NORTH);
        panel.add(scroll,       BorderLayout.CENTER);

        return panel;
    }

    /**
     * Construye el panel derecho con el formulario de reserva.
     *
     * @return panel del formulario de reserva
     */
    private JPanel construirPanelFormularioReserva() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 240)),
            "Datos de la Reserva",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setBackground(Color.WHITE);
        inner.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 6, 5, 6);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        // Slot seleccionado
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        inner.add(new JLabel("Slot seleccionado:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
        lblSlotSeleccionado = new JLabel("(Seleccione un slot de la tabla)");
        lblSlotSeleccionado.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblSlotSeleccionado.setForeground(Color.DARK_GRAY);
        inner.add(lblSlotSeleccionado, gbc);
        gbc.gridwidth = 1;

        // Búsqueda de dueño
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        inner.add(new JLabel("Buscar dueño *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtBuscarDueno = new JTextField();
        txtBuscarDueno.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtBuscarDueno.setToolTipText("Escriba nombre o DNI del dueño");
        // DocumentListener para búsqueda en tiempo real
        txtBuscarDueno.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { buscarDueno(); }
            @Override public void removeUpdate(DocumentEvent e)  { buscarDueno(); }
            @Override public void changedUpdate(DocumentEvent e) { buscarDueno(); }
        });
        inner.add(txtBuscarDueno, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton btnBuscarDueno = new JButton("🔍");
        btnBuscarDueno.setToolTipText("Buscar dueño");
        btnBuscarDueno.addActionListener(e -> buscarDueno());
        inner.add(btnBuscarDueno, gbc);

        // Tabla de dueños encontrados
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        inner.add(new JLabel("Dueños:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
        modeloDuenos = new DefaultTableModel(COLUMNAS_DUENOS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaDuenos = new JTable(modeloDuenos);
        tablaDuenos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaDuenos.setRowHeight(20);
        tablaDuenos.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tablaDuenos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        // Ocultar columna ID
        tablaDuenos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaDuenos.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaDuenos.getColumnModel().getColumn(0).setWidth(0);
        // Al seleccionar dueño → cargar mascotas
        tablaDuenos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarMascotasDeDueno();
        });
        JScrollPane scrollDuenos = new JScrollPane(tablaDuenos);
        scrollDuenos.setPreferredSize(new Dimension(0, 100));
        inner.add(scrollDuenos, gbc);
        gbc.gridwidth = 1;

        // Mascota
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        inner.add(new JLabel("Mascota *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
        cmbMascota = new JComboBox<>();
        cmbMascota.setFont(new Font("SansSerif", Font.PLAIN, 12));
        inner.add(cmbMascota, gbc);
        gbc.gridwidth = 1;

        // Motivo
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        inner.add(new JLabel("Motivo:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        txtMotivo = new JTextArea(3, 20);
        txtMotivo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtMotivo.setLineWrap(true);
        txtMotivo.setWrapStyleWord(true);
        JScrollPane scrollMotivo = new JScrollPane(txtMotivo);
        inner.add(scrollMotivo, gbc);
        gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;

        // Botón Reservar
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.EAST;
        btnReservar = new JButton("✅ Reservar Turno");
        btnReservar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnReservar.setBackground(new Color(0, 120, 215));
        btnReservar.setForeground(Color.WHITE);
        btnReservar.setFocusPainted(false);
        btnReservar.setPreferredSize(new Dimension(180, 34));
        btnReservar.addActionListener(e -> reservarTurno());
        inner.add(btnReservar, gbc);

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Pestaña 2: Cancelar Turno
    // =========================================================================

    /**
     * Construye el panel de la pestaña "Cancelar / Inasistencia".
     *
     * @return panel de cancelación
     */
    private JPanel construirTabCancelar() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Tabla de turnos pendientes
        JPanel panelTabla = new JPanel(new BorderLayout(0, 4));
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 240)),
            "Turnos del Día (Pendientes y Confirmados)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        modeloTurnos = new DefaultTableModel(COLUMNAS_TURNOS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaTurnos = new JTable(modeloTurnos);
        tablaTurnos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaTurnos.setRowHeight(22);
        tablaTurnos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaTurnos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaTurnos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // Anchos de columnas
        int[] anchos = {0, 90, 65, 130, 150, 90, 150};
        for (int i = 0; i < anchos.length; i++) {
            tablaTurnos.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tablaTurnos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaTurnos.getColumnModel().getColumn(0).setMaxWidth(0);

        JScrollPane scroll = new JScrollPane(tablaTurnos);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Botones de acción
        JPanel panelBotonesCancelar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panelBotonesCancelar.setBackground(Color.WHITE);

        JButton btnRefrescar = new JButton("🔄 Refrescar");
        btnRefrescar.addActionListener(e -> cargarTurnosPendientes());

        JButton btnCancelar = new JButton("❌ Cancelar Turno");
        btnCancelar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> cancelarTurnoSeleccionado("Cancelado"));

        JButton btnInasistencia = new JButton("⚠ Marcar Inasistencia");
        btnInasistencia.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnInasistencia.addActionListener(e -> cancelarTurnoSeleccionado("Inasistencia"));

        panelBotonesCancelar.add(btnRefrescar);
        panelBotonesCancelar.add(btnCancelar);
        panelBotonesCancelar.add(btnInasistencia);

        panelTabla.add(scroll,               BorderLayout.CENTER);
        panelTabla.add(panelBotonesCancelar, BorderLayout.SOUTH);

        panel.add(panelTabla, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye la barra de botones global (parte inferior del diálogo).
     *
     * @return panel de botones global
     */
    private JPanel construirBarraBotonesGlobal() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        panel.setBackground(new Color(240, 244, 252));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCerrar.addActionListener(e -> {
            try { setClosed(true); }
            catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
        });
        panel.add(btnCerrar);
        return panel;
    }

    // =========================================================================
    // Lógica de negocio — Pestaña Reservar
    // =========================================================================

    /**
     * Carga los veterinarios activos en el combo de filtro.
     * Usa {@link TurnoService#buscarVeterinariosActivos()} para obtener la lista
     * completa sin depender de que existan slots generados para la fecha actual.
     */
    private void cargarVeterinarios() {
        cmbVeterinario.removeAllItems();
        // Renderer para mostrar "Apellido, Nombre" en el combo
        cmbVeterinario.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Veterinario) {
                    Veterinario v = (Veterinario) value;
                    setText(v.getApellido() + ", " + v.getNombre() + " [" + v.getMatricula() + "]");
                } else if (value == null) {
                    setText("-- Todos los veterinarios --");
                }
                return this;
            }
        });
        try {
            cmbVeterinario.addItem(null); // opción "Todos"
            List<Veterinario> vets = turnoService.buscarVeterinariosActivos();
            for (Veterinario v : vets) {
                cmbVeterinario.addItem(v);
            }
            cmbVeterinario.setSelectedIndex(0);
        } catch (SQLException e) {
            System.err.println("Error al cargar veterinarios: " + e.getMessage());
        }
    }

    /**
     * Carga las fechas de la semana actual (hoy + 7 días) en el combo de fecha.
     */
    private void cargarFechasSemana() {
        LocalDate hoy = LocalDate.now();
        cmbFecha.removeAllItems();
        // Estructuras repetitivas: for para generar fechas de la semana
        for (int i = 0; i <= 7; i++) {
            LocalDate fecha = hoy.plusDays(i);
            String etiqueta = fecha.format(FMT_FECHA);
            if (i == 0) etiqueta += " (hoy)";
            cmbFecha.addItem(etiqueta);
        }
        cmbFecha.setSelectedIndex(0);
    }

    /**
     * Actualiza el label de slot seleccionado cuando el usuario elige una fila.
     */
    private void actualizarLabelSlotSeleccionado() {
        int fila = tablaSlots.getSelectedRow();
        if (fila < 0) {
            lblSlotSeleccionado.setText("(Seleccione un slot de la tabla)");
            lblSlotSeleccionado.setForeground(Color.DARK_GRAY);
        } else {
            String fecha = (String) modeloSlots.getValueAt(fila, 1);
            String hora  = (String) modeloSlots.getValueAt(fila, 2);
            lblSlotSeleccionado.setText("✔ " + fecha + " a las " + hora);
            lblSlotSeleccionado.setForeground(new Color(0, 130, 60));
        }
    }

    /**
     * Busca los slots disponibles para el veterinario y fecha seleccionados.
     */
    private void buscarSlotsDisponibles() {
        modeloSlots.setRowCount(0);
        idsSlots.clear();
        lblSlotSeleccionado.setText("(Seleccione un slot de la tabla)");
        lblSlotSeleccionado.setForeground(Color.DARK_GRAY);

        // Obtener fecha seleccionada
        int idxFecha = cmbFecha.getSelectedIndex();
        if (idxFecha < 0) return;
        LocalDate fecha = LocalDate.now().plusDays(idxFecha);

        // Obtener ID de veterinario (0 si no hay selección)
        int idVet = 0;
        if (cmbVeterinario.getSelectedItem() instanceof Veterinario) {
            idVet = ((Veterinario) cmbVeterinario.getSelectedItem()).getId();
        }

        try {
            List<SlotAgenda> slots = turnoService.buscarSlotsDisponibles(idVet, fecha);

            if (slots.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No hay slots disponibles para la fecha seleccionada.\n"
                    + "Verifique que el veterinario tenga slots generados.",
                    "Sin disponibilidad",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Estructuras repetitivas: for-each para llenar la tabla
            for (SlotAgenda slot : slots) {
                String vet = slot.getAgenda().getVeterinario().toString();
                modeloSlots.addRow(new Object[]{
                    slot.getIdSlot(),
                    slot.getFecha().format(FMT_FECHA),
                    slot.getHora().toString().substring(0, 5),
                    slot.getEstado().name()
                });
                idsSlots.add(slot.getIdSlot());

                // Agregar veterinario al combo si no está
                boolean existe = false;
                for (int i = 0; i < cmbVeterinario.getItemCount(); i++) {
                    Object item = cmbVeterinario.getItemAt(i);
                    if (item instanceof Veterinario &&
                        ((Veterinario) item).getId() == slot.getAgenda().getVeterinario().getId()) {
                        existe = true;
                        break;
                    }
                }
                if (!existe) {
                    cmbVeterinario.addItem(slot.getAgenda().getVeterinario());
                }
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al buscar slots disponibles:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Busca dueños en tiempo real según el texto ingresado en el campo de búsqueda.
     * Se invoca desde el {@link DocumentListener} del campo de texto.
     */
    private void buscarDueno() {
        String texto = txtBuscarDueno.getText().trim();
        modeloDuenos.setRowCount(0);
        duenosEncontrados.clear();
        cmbMascota.removeAllItems();

        // Validación condicional: buscar solo si hay al menos 2 caracteres
        if (texto.length() < 2) return;

        try {
            List<Dueno> duenos = pacienteService.obtenerDuenos();
            String textoLower = texto.toLowerCase();

            // Estructuras repetitivas: for-each con condición de filtro
            for (Dueno d : duenos) {
                boolean coincideNombre = (d.getNombre() + " " + d.getApellido())
                    .toLowerCase().contains(textoLower);
                boolean coincideDni = d.getDni() != null
                    && d.getDni().contains(texto);

                if (coincideNombre || coincideDni) {
                    modeloDuenos.addRow(new Object[]{
                        d.getId(),
                        d.getDni(),
                        d.getApellido(),
                        d.getNombre(),
                        d.getTelefono()
                    });
                    duenosEncontrados.add(d);
                }
            }
        } catch (SQLException e) {
            // No bloquear la escritura del usuario; solo logear
            System.err.println("Error al buscar dueños: " + e.getMessage());
        }
    }

    /**
     * Carga las mascotas del dueño seleccionado en el combo de mascotas.
     */
    private void cargarMascotasDeDueno() {
        cmbMascota.removeAllItems();
        int filaSeleccionada = tablaDuenos.getSelectedRow();
        if (filaSeleccionada < 0 || filaSeleccionada >= duenosEncontrados.size()) return;

        Dueno dueno = duenosEncontrados.get(filaSeleccionada);

        try {
            List<Mascota> mascotas = turnoService.buscarMascotasPorDueno(dueno.getId());

            if (mascotas.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "El dueño " + dueno.toString()
                    + " no tiene mascotas registradas.\n"
                    + "Registre la mascota en el módulo de Pacientes primero.",
                    "Sin mascotas",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Estructuras repetitivas: for-each para llenar el combo
            for (Mascota m : mascotas) {
                cmbMascota.addItem(m);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar mascotas del dueño:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Valida los campos y ejecuta la reserva del turno.
     *
     * <p>Captura las excepciones de concurrencia y las convierte en mensajes
     * amigables para el operador.</p>
     */
    private void reservarTurno() {
        // Validar slot seleccionado
        int filaSlot = tablaSlots.getSelectedRow();
        if (filaSlot < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un slot disponible de la tabla.",
                "Slot requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int idSlot = idsSlots.get(filaSlot);

        // Validar mascota seleccionada
        if (cmbMascota.getSelectedItem() == null || !(cmbMascota.getSelectedItem() instanceof Mascota)) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una mascota para el turno.\n"
                + "Primero busque al dueño y seleccione una mascota.",
                "Mascota requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Mascota mascota = (Mascota) cmbMascota.getSelectedItem();
        String motivo = txtMotivo.getText().trim();

        try {
            btnReservar.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            int idTurno = turnoService.reservarTurno(mascota.getIdMascota(), idSlot, motivo);

            JOptionPane.showMessageDialog(this,
                "Turno reservado correctamente.\n"
                + "ID de turno: " + idTurno + "\n"
                + "Mascota: " + mascota.getNombre() + "\n"
                + "Slot: " + lblSlotSeleccionado.getText().replace("✔ ", ""),
                "Turno reservado",
                JOptionPane.INFORMATION_MESSAGE);

            // Limpiar formulario y refrescar tabla
            limpiarFormularioReserva();
            buscarSlotsDisponibles();

        } catch (MascotaNoRegistradaException e) {
            JOptionPane.showMessageDialog(this,
                "Error con la mascota seleccionada:\n" + e.getMessage(),
                "Mascota no válida", JOptionPane.ERROR_MESSAGE);

        } catch (SlotNoDisponibleException e) {
            JOptionPane.showMessageDialog(this,
                "El horario seleccionado ya no está disponible.\n"
                + "Actualice la lista de slots y seleccione otro horario.\n\n"
                + e.getMessage(),
                "Slot no disponible", JOptionPane.WARNING_MESSAGE);
            buscarSlotsDisponibles(); // Refrescar automáticamente

        } catch (TurnoOcupadoException e) {
            JOptionPane.showMessageDialog(this,
                "El turno acaba de ser reservado por otro operador.\n"
                + "Actualice la lista y seleccione un horario disponible.\n\n"
                + e.getMessage(),
                "Conflicto de reserva (concurrencia)", JOptionPane.WARNING_MESSAGE);
            buscarSlotsDisponibles();

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error de base de datos al reservar el turno:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnReservar.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Limpia los campos del formulario de reserva tras una operación exitosa.
     */
    private void limpiarFormularioReserva() {
        txtBuscarDueno.setText("");
        modeloDuenos.setRowCount(0);
        duenosEncontrados.clear();
        cmbMascota.removeAllItems();
        txtMotivo.setText("");
        tablaSlots.clearSelection();
        lblSlotSeleccionado.setText("(Seleccione un slot de la tabla)");
        lblSlotSeleccionado.setForeground(Color.DARK_GRAY);
    }

    // =========================================================================
    // Lógica de negocio — Pestaña Cancelar
    // =========================================================================

    /**
     * Carga los turnos pendientes del día en la tabla de cancelación.
     */
    private void cargarTurnosPendientes() {
        modeloTurnos.setRowCount(0);
        idsTurnos.clear();

        try {
            List<Turno> turnos = turnoService.buscarTurnosPendientes(LocalDate.now());

            // Estructuras repetitivas: for-each para llenar la tabla
            for (Turno t : turnos) {
                String fecha = t.getSlot() != null
                    ? t.getSlot().getFecha().format(FMT_FECHA) : "—";
                String hora  = t.getSlot() != null
                    ? t.getSlot().getHora().toString().substring(0, 5) : "—";
                String mascota = t.getMascota() != null
                    ? t.getMascota().getNombre() : "—";
                String vet = (t.getSlot() != null && t.getSlot().getAgenda() != null)
                    ? t.getSlot().getAgenda().getVeterinario().toString() : "—";

                modeloTurnos.addRow(new Object[]{
                    t.getIdTurno(), fecha, hora,
                    mascota, vet,
                    t.getEstado() != null ? t.getEstado().name() : "—",
                    t.getMotivo()
                });
                idsTurnos.add(t.getIdTurno());
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar los turnos pendientes:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Cancela el turno seleccionado o registra inasistencia.
     *
     * @param estadoNuevo estado nuevo a aplicar: {@code "Cancelado"} o {@code "Inasistencia"}
     */
    private void cancelarTurnoSeleccionado(String estadoNuevo) {
        int fila = tablaTurnos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un turno de la tabla para " +
                ("Cancelado".equals(estadoNuevo) ? "cancelar." : "marcar como inasistencia."),
                "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idTurno = idsTurnos.get(fila);
        String mascota = (String) modeloTurnos.getValueAt(fila, 3);
        String hora    = (String) modeloTurnos.getValueAt(fila, 2);

        String accion = "Cancelado".equals(estadoNuevo) ? "cancelar" : "marcar como inasistencia";
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Desea " + accion + " el turno de " + mascota + " a las " + hora + "?",
            "Confirmar " + estadoNuevo,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) return;

        try {
            turnoService.cancelarTurno(idTurno, estadoNuevo);

            JOptionPane.showMessageDialog(this,
                "Turno marcado como '" + estadoNuevo + "' correctamente.\n"
                + "El slot fue liberado y está disponible nuevamente.",
                "Operación exitosa", JOptionPane.INFORMATION_MESSAGE);

            cargarTurnosPendientes();

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al " + accion + " el turno:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }
}
