package com.sigvet.view;

import com.sigvet.exception.StockInsuficienteException;
import com.sigvet.model.Mascota;
import com.sigvet.model.Medicamento;
import com.sigvet.model.Turno;
import com.sigvet.model.Veterinario;
import com.sigvet.service.AgendaService;
import com.sigvet.service.ConsultaMedicaService;
import com.sigvet.service.PacienteService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diálogo para el caso de uso CU-03: Registrar Consulta Médica.
 *
 * <p>Es el caso de uso más crítico del sistema. Implementa el flujo multi-paso:</p>
 * <ol>
 *   <li>Selección de turno pendiente (o modo urgencia sin turno).</li>
 *   <li>Ingreso de síntomas, diagnóstico y medicamentos recetados.</li>
 *   <li>Al guardar:
 *     <ol type="a">
 *       <li>Llama a {@code sp_registrar_consulta_turno} (crea consulta + marca turno Atendido + retorna {@code id_consulta}).</li>
 *       <li>Por cada medicamento: {@code sp_descontar_stock_fifo(idMedicamento, cantidad)}.</li>
 *       <li>Por cada medicamento: INSERT directo en {@code item_receta}.</li>
 *     </ol>
 *   </li>
 * </ol>
 *
 * <p><strong>IMPORTANTE:</strong> NO se usa JSON. El SP {@code sp_registrar_consulta_turno}
 * NO recibe medicamentos. Todo el procesamiento de medicamentos es multi-paso en Java.</p>
 *
 * <p><strong>Manejo de excepciones:</strong></p>
 * <ul>
 *   <li>{@link StockInsuficienteException} → alerta de stock con nombre del medicamento.</li>
 *   <li>{@link IllegalArgumentException} → aviso de validación de campos.</li>
 *   <li>{@link SQLException} → error genérico de BD.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see ConsultaMedicaService
 */
public class RegistrarConsultaDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Columnas de la tabla de turnos pendientes. */
    private static final String[] COL_TURNOS = {
        "ID", "Fecha/Hora", "Mascota", "Especie", "Dueño", "Motivo"
    };

    /** Columnas de la tabla de medicamentos recetados (col 0 = idMedicamento oculto). */
    private static final String[] COL_MEDS = {
        "idMed", "Medicamento", "Cantidad", "Dosis", "Frecuencia", "Duración", "Stock Disp."
    };

    /** Formato para fechas/horas de turnos. */
    private static final DateTimeFormatter FMT_DT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================================
    // Services
    // =========================================================================

    private final ConsultaMedicaService consultaService;
    private final AgendaService         agendaService;
    private final PacienteService       pacienteService;

    // =========================================================================
    // Estado interno
    // =========================================================================

    /** ID del turno seleccionado (-1 si es urgencia). */
    private int idTurnoSeleccionado  = -1;

    /** ID de la mascota de la consulta actual. */
    private int idMascotaActual      = -1;

    /** ID del veterinario que atiende. */
    private int idVeterinarioActual  = -1;

    /** Lista de turnos cargados en la tabla (para obtener IDs). */
    private final List<Turno> turnosCargados = new ArrayList<>();

    // =========================================================================
    // Componentes — modo (radio buttons)
    // =========================================================================

    private JRadioButton rbConTurno;
    private JRadioButton rbUrgencia;

    // =========================================================================
    // Componentes — panel con turno
    // =========================================================================

    private JTable         tablaTurnos;
    private DefaultTableModel modeloTurnos;
    private JPanel         panelSeleccionTurno;

    // =========================================================================
    // Componentes — panel urgencia
    // =========================================================================

    private JComboBox<Mascota>     cmbMascotaUrgencia;
    private JComboBox<Veterinario> cmbVeterinarioUrgencia;
    private JPanel panelUrgencia;

    // =========================================================================
    // Componentes — datos de la mascota (solo lectura)
    // =========================================================================

    private JLabel lblMascotaNombre;
    private JLabel lblMascotaEspecie;
    private JLabel lblMascotaDueno;
    private JLabel lblMascotaDni;
    private JLabel lblMascotaEdad;

    // =========================================================================
    // Componentes — formulario consulta
    // =========================================================================

    private JComboBox<Veterinario> cmbVeterinarioConsulta;
    private JTextArea txtSintomas;
    private JTextArea txtDiagnostico;

    // =========================================================================
    // Componentes — medicamentos
    // =========================================================================

    private JTable         tablaMedicamentos;
    private DefaultTableModel modeloMedicamentos;
    private JLabel         lblResumenMeds;
    private JButton        btnGuardar;
    private JButton        btnGuardarUrgencia;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de registro de consulta médica.
     *
     * @param consultaService servicio de consulta médica
     * @param agendaService   servicio de agenda (para lista de veterinarios)
     * @param pacienteService servicio de pacientes (para lista de mascotas en urgencia)
     */
    public RegistrarConsultaDialog(ConsultaMedicaService consultaService,
                                    AgendaService         agendaService,
                                    PacienteService       pacienteService) {
        super("Registrar Consulta Médica", true, true, true, true);
        this.consultaService = consultaService;
        this.agendaService   = agendaService;
        this.pacienteService = pacienteService;

        construirUI();
        cargarVeterinarios();
        cargarTurnosPendientes();
        cargarMascotasUrgencia();
        actualizarModoSeleccionado();
    }

    // =========================================================================
    // Construcción principal de la UI
    // =========================================================================

    /**
     * Construye la interfaz gráfica completa del diálogo.
     */
    private void construirUI() {
        setLayout(new BorderLayout(0, 6));
        getContentPane().setBackground(new Color(248, 249, 252));

        add(construirPanelNorte(),  BorderLayout.NORTH);
        add(construirPanelCentro(), BorderLayout.CENTER);
        add(construirPanelSur(),    BorderLayout.SOUTH);
        add(construirPanelBotones(), BorderLayout.PAGE_END);
    }

    // =========================================================================
    // Panel NORTE: selección de modo (turno / urgencia)
    // =========================================================================

    /**
     * Construye el panel norte con la selección de modo y la tabla/combo de origen.
     *
     * @return panel norte
     */
    private JPanel construirPanelNorte() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));

        // ── Radio buttons de modo ────────────────────────────────────────────
        JPanel panelModo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        panelModo.setBackground(new Color(230, 236, 255));
        panelModo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 240)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        rbConTurno  = new JRadioButton("Consulta con Turno", true);
        rbUrgencia  = new JRadioButton("Consulta de Urgencia (sin turno)");
        rbConTurno.setBackground(new Color(230, 236, 255));
        rbUrgencia.setBackground(new Color(230, 236, 255));
        rbConTurno.setFont(new Font("SansSerif", Font.BOLD, 12));
        rbUrgencia.setFont(new Font("SansSerif", Font.PLAIN, 12));

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rbConTurno);
        grupo.add(rbUrgencia);

        rbConTurno.addActionListener(e -> actualizarModoSeleccionado());
        rbUrgencia.addActionListener(e -> actualizarModoSeleccionado());

        JLabel lblModo = new JLabel("Tipo de consulta:");
        lblModo.setFont(new Font("SansSerif", Font.BOLD, 12));

        panelModo.add(lblModo);
        panelModo.add(rbConTurno);
        panelModo.add(rbUrgencia);

        // ── Panel con tabla de turnos ─────────────────────────────────────────
        panelSeleccionTurno = construirPanelTablaTurnos();

        // ── Panel urgencia ─────────────────────────────────────────────────────
        panelUrgencia = construirPanelUrgencia();
        panelUrgencia.setVisible(false);

        JPanel panelContenido = new JPanel(new CardLayout());
        panelContenido.add(panelSeleccionTurno, "turno");
        panelContenido.add(panelUrgencia,       "urgencia");

        panel.add(panelModo,      BorderLayout.NORTH);
        panel.add(panelContenido, BorderLayout.CENTER);

        // Guardar referencia al CardLayout para cambiar entre paneles
        panel.putClientProperty("cardLayout",  panelContenido.getLayout());
        panel.putClientProperty("cardPanel",   panelContenido);

        return panel;
    }

    /**
     * Construye el panel con la tabla de turnos pendientes.
     *
     * @return panel de tabla de turnos
     */
    private JPanel construirPanelTablaTurnos() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Seleccionar Turno Pendiente",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 11)
        ));

        modeloTurnos = new DefaultTableModel(COL_TURNOS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaTurnos = new JTable(modeloTurnos);
        tablaTurnos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaTurnos.setRowHeight(22);
        tablaTurnos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaTurnos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tablaTurnos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Ocultar columna ID (col 0)
        tablaTurnos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaTurnos.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaTurnos.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 130, 120, 100, 140, 160};
        for (int i = 1; i < anchos.length; i++) {
            tablaTurnos.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Al seleccionar turno → llenar datos de mascota
        tablaTurnos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onTurnoSeleccionado();
        });

        JScrollPane scroll = new JScrollPane(tablaTurnos);
        scroll.setPreferredSize(new Dimension(0, 130));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JButton btnRefrescar = new JButton("🔄 Refrescar turnos");
        btnRefrescar.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnRefrescar.addActionListener(e -> cargarTurnosPendientes());

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        panelBoton.setBackground(Color.WHITE);
        panelBoton.add(btnRefrescar);

        panel.add(scroll,      BorderLayout.CENTER);
        panel.add(panelBoton,  BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Construye el panel de selección de mascota y veterinario para urgencias.
     *
     * @return panel de urgencia
     */
    private JPanel construirPanelUrgencia() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(255, 248, 230));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 180, 50)),
            "⚡ Urgencia — Sin Turno Previo",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 11)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Mascota *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbMascotaUrgencia = new JComboBox<>();
        cmbMascotaUrgencia.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbMascotaUrgencia.addActionListener(e -> onMascotaUrgenciaSeleccionada());
        panel.add(cmbMascotaUrgencia, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Veterinario *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbVeterinarioUrgencia = new JComboBox<>();
        cmbVeterinarioUrgencia.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(cmbVeterinarioUrgencia, gbc);

        return panel;
    }

    // =========================================================================
    // Panel CENTRO: datos mascota + formulario consulta
    // =========================================================================

    /**
     * Construye el panel central con datos de la mascota (lectura) y el formulario de consulta.
     *
     * @return panel central con JSplitPane
     */
    private JPanel construirPanelCentro() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 0, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            construirPanelDatosMascota(),
            construirPanelFormularioConsulta()
        );
        split.setDividerLocation(260);
        split.setResizeWeight(0.25);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel con los datos de la mascota (solo lectura).
     *
     * @return panel de datos mascota
     */
    private JPanel construirPanelDatosMascota() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Datos del Paciente",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 11)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        String placeholder = "—";
        lblMascotaNombre  = new JLabel(placeholder);
        lblMascotaEspecie = new JLabel(placeholder);
        lblMascotaDueno   = new JLabel(placeholder);
        lblMascotaDni     = new JLabel(placeholder);
        lblMascotaEdad    = new JLabel(placeholder);

        Font fontBold  = new Font("SansSerif", Font.BOLD, 12);
        Font fontPlain = new Font("SansSerif", Font.PLAIN, 12);
        lblMascotaNombre.setFont(fontBold);
        lblMascotaEspecie.setFont(fontPlain);
        lblMascotaDueno.setFont(fontPlain);
        lblMascotaDni.setFont(fontPlain);
        lblMascotaEdad.setFont(fontPlain);

        String[][] filas = {
            {"Mascota:", null},
            {"Especie/Raza:", null},
            {"Dueño:", null},
            {"DNI Dueño:", null},
            {"Edad:", null}
        };
        JLabel[] valores = {lblMascotaNombre, lblMascotaEspecie,
                            lblMascotaDueno,  lblMascotaDni, lblMascotaEdad};

        for (int i = 0; i < filas.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            JLabel lbl = new JLabel(filas[i][0]);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(Color.DARK_GRAY);
            panel.add(lbl, gbc);

            gbc.gridx = 1; gbc.weightx = 1.0;
            panel.add(valores[i], gbc);
        }

        // Espaciador vertical al final
        gbc.gridx = 0; gbc.gridy = filas.length; gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    /**
     * Construye el panel del formulario de consulta (veterinario, síntomas, diagnóstico).
     *
     * @return panel del formulario
     */
    private JPanel construirPanelFormularioConsulta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Datos de la Consulta  (* obligatorio)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 11)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 8, 5, 8);
        gbc.anchor  = GridBagConstraints.NORTHWEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        // Veterinario
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Veterinario actuante *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbVeterinarioConsulta = new JComboBox<>();
        cmbVeterinarioConsulta.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(cmbVeterinarioConsulta, gbc);

        // Síntomas
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Síntomas *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        txtSintomas = new JTextArea(4, 30);
        txtSintomas.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtSintomas.setLineWrap(true);
        txtSintomas.setWrapStyleWord(true);
        txtSintomas.setBorder(BorderFactory.createLoweredBevelBorder());
        panel.add(new JScrollPane(txtSintomas), gbc);

        // Diagnóstico
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Diagnóstico *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        txtDiagnostico = new JTextArea(3, 30);
        txtDiagnostico.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtDiagnostico.setLineWrap(true);
        txtDiagnostico.setWrapStyleWord(true);
        txtDiagnostico.setBorder(BorderFactory.createLoweredBevelBorder());
        panel.add(new JScrollPane(txtDiagnostico), gbc);

        return panel;
    }

    // =========================================================================
    // Panel SUR: medicamentos recetados
    // =========================================================================

    /**
     * Construye el panel sur con la tabla de medicamentos recetados y botones de acción.
     *
     * @return panel de medicamentos
     */
    private JPanel construirPanelSur() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "💊 Medicamentos Recetados (opcional)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 11)
        ));
        panel.setPreferredSize(new Dimension(0, 190));

        // Tabla de medicamentos (col 0 = idMedicamento, oculta)
        modeloMedicamentos = new DefaultTableModel(COL_MEDS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaMedicamentos = new JTable(modeloMedicamentos);
        tablaMedicamentos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaMedicamentos.setRowHeight(22);
        tablaMedicamentos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaMedicamentos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tablaMedicamentos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Ocultar col 0 (idMedicamento)
        tablaMedicamentos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaMedicamentos.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaMedicamentos.getColumnModel().getColumn(0).setWidth(0);

        int[] anchosMeds = {0, 160, 70, 90, 120, 80, 80};
        for (int i = 1; i < anchosMeds.length; i++) {
            tablaMedicamentos.getColumnModel().getColumn(i).setPreferredWidth(anchosMeds[i]);
        }

        JScrollPane scroll = new JScrollPane(tablaMedicamentos);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Botones y resumen
        JPanel panelBotonesMed = new JPanel(new BorderLayout());
        panelBotonesMed.setBackground(Color.WHITE);

        JPanel btnsMed = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnsMed.setBackground(Color.WHITE);

        JButton btnAgregar = new JButton("➕ Agregar Medicamento");
        btnAgregar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnAgregar.addActionListener(e -> abrirDialogoAgregarMedicamento());

        JButton btnQuitar = new JButton("➖ Quitar Seleccionado");
        btnQuitar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnQuitar.addActionListener(e -> quitarMedicamentoSeleccionado());

        btnsMed.add(btnAgregar);
        btnsMed.add(btnQuitar);

        lblResumenMeds = new JLabel("Total medicamentos: 0 | Stock verificado: ✓");
        lblResumenMeds.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblResumenMeds.setForeground(new Color(0, 120, 60));
        lblResumenMeds.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        panelBotonesMed.add(btnsMed,        BorderLayout.WEST);
        panelBotonesMed.add(lblResumenMeds, BorderLayout.EAST);

        panel.add(scroll,          BorderLayout.CENTER);
        panel.add(panelBotonesMed, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Panel de botones principales
    // =========================================================================

    /**
     * Construye la barra de botones de acción principal.
     *
     * @return panel de botones
     */
    private JPanel construirPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        panel.setBackground(new Color(235, 242, 255));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        btnGuardar = new JButton("💾 GUARDAR CONSULTA");
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnGuardar.setBackground(new Color(0, 120, 215));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setPreferredSize(new Dimension(200, 36));
        btnGuardar.addActionListener(e -> guardarConsultaConTurno());

        btnGuardarUrgencia = new JButton("⚡ GUARDAR URGENCIA");
        btnGuardarUrgencia.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnGuardarUrgencia.setBackground(new Color(200, 100, 0));
        btnGuardarUrgencia.setForeground(Color.WHITE);
        btnGuardarUrgencia.setFocusPainted(false);
        btnGuardarUrgencia.setPreferredSize(new Dimension(200, 36));
        btnGuardarUrgencia.setVisible(false);
        btnGuardarUrgencia.addActionListener(e -> guardarConsultaUrgencia());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> {
            int op = JOptionPane.showConfirmDialog(this,
                "¿Cerrar sin guardar la consulta?",
                "Confirmar cierre", JOptionPane.YES_NO_OPTION);
            if (op == JOptionPane.YES_OPTION) {
                try { setClosed(true); } catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
            }
        });

        panel.add(btnGuardarUrgencia);
        panel.add(btnGuardar);
        panel.add(Box.createHorizontalStrut(8));
        panel.add(btnCancelar);

        return panel;
    }

    // =========================================================================
    // Carga de datos
    // =========================================================================

    /**
     * Carga los turnos pendientes del día en la tabla.
     */
    private void cargarTurnosPendientes() {
        modeloTurnos.setRowCount(0);
        turnosCargados.clear();
        idTurnoSeleccionado = -1;
        idMascotaActual     = -1;
        limpiarDatosMascota();

        try {
            List<Turno> turnos = consultaService.obtenerTurnosPendientes();

            if (turnos.isEmpty()) {
                modeloTurnos.addRow(new Object[]{
                    -1, "(No hay turnos pendientes hoy)", "", "", "", ""
                });
                return;
            }

            // Estructuras repetitivas: for-each para llenar la tabla
            for (Turno t : turnos) {
                String fechaHora = "";
                if (t.getSlot() != null) {
                    fechaHora = t.getSlot().getFecha().format(DateTimeFormatter.ofPattern("dd/MM")) +
                        " " + t.getSlot().getHora().toString().substring(0, 5);
                }
                String mascota  = t.getMascota() != null ? t.getMascota().getNombre() : "—";
                String especie  = (t.getMascota() != null && t.getMascota().getEspecie() != null)
                    ? t.getMascota().getEspecie().getNombre() : "—";
                String dueno    = (t.getMascota() != null && t.getMascota().getDueno() != null)
                    ? t.getMascota().getDueno().getApellido() + ", "
                      + t.getMascota().getDueno().getNombre() : "—";

                modeloTurnos.addRow(new Object[]{
                    t.getIdTurno(), fechaHora, mascota, especie, dueno, t.getMotivo()
                });
                turnosCargados.add(t);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar turnos pendientes:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga los veterinarios activos en todos los combos de veterinarios.
     */
    private void cargarVeterinarios() {
        try {
            List<Veterinario> vets = agendaService.obtenerVeterinariosActivos();

            cmbVeterinarioConsulta.removeAllItems();
            cmbVeterinarioUrgencia.removeAllItems();

            // Estructuras repetitivas: for-each para llenar combos
            for (Veterinario v : vets) {
                cmbVeterinarioConsulta.addItem(v);
                cmbVeterinarioUrgencia.addItem(v);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar veterinarios:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga las mascotas activas en el combo de urgencia.
     */
    private void cargarMascotasUrgencia() {
        try {
            List<Mascota> mascotas = new ArrayList<>();
            // Obtener dueños y sus mascotas (demo: cargamos todas las mascotas activas)
            // En producción se usaría una búsqueda. Aquí simplificamos.
            List<com.sigvet.model.Dueno> duenos = pacienteService.obtenerDuenos();
            for (com.sigvet.model.Dueno d : duenos) {
                List<Mascota> mMascotas = pacienteService.obtenerMascotasPorDueno(d.getId());
                mascotas.addAll(mMascotas);
            }

            cmbMascotaUrgencia.removeAllItems();
            for (Mascota m : mascotas) {
                cmbMascotaUrgencia.addItem(m);
            }
        } catch (SQLException e) {
            // No bloquear: el usuario puede buscar después
            System.err.println("Error al cargar mascotas urgencia: " + e.getMessage());
        }
    }

    // =========================================================================
    // Lógica de interacción
    // =========================================================================

    /**
     * Actualiza la visibilidad de paneles y botones según el modo seleccionado.
     */
    private void actualizarModoSeleccionado() {
        boolean esTurno   = rbConTurno.isSelected();
        boolean esUrgencia = rbUrgencia.isSelected();

        panelSeleccionTurno.setVisible(esTurno);
        panelUrgencia.setVisible(esUrgencia);
        btnGuardar.setVisible(esTurno);
        btnGuardarUrgencia.setVisible(esUrgencia);

        if (esUrgencia) {
            onMascotaUrgenciaSeleccionada();
        } else {
            // Limpiar datos si cambiamos a turno
            idTurnoSeleccionado = -1;
            idMascotaActual     = -1;
            limpiarDatosMascota();
        }

        revalidate();
        repaint();
    }

    /**
     * Responde a la selección de un turno en la tabla: llena los datos de la mascota.
     */
    private void onTurnoSeleccionado() {
        int fila = tablaTurnos.getSelectedRow();
        if (fila < 0 || fila >= turnosCargados.size()) {
            idTurnoSeleccionado = -1;
            idMascotaActual     = -1;
            limpiarDatosMascota();
            return;
        }

        Turno turno = turnosCargados.get(fila);
        idTurnoSeleccionado = turno.getIdTurno();

        Mascota mascota = turno.getMascota();
        if (mascota != null) {
            idMascotaActual = mascota.getIdMascota();
            llenarDatosMascota(mascota);
        }

        // Si el turno tiene veterinario asociado, preseleccionarlo
        if (turno.getSlot() != null && turno.getSlot().getAgenda() != null) {
            Veterinario vetTurno = turno.getSlot().getAgenda().getVeterinario();
            for (int i = 0; i < cmbVeterinarioConsulta.getItemCount(); i++) {
                Veterinario v = cmbVeterinarioConsulta.getItemAt(i);
                if (v != null && v.getId() == vetTurno.getId()) {
                    cmbVeterinarioConsulta.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Responde a la selección de mascota en el combo de urgencia.
     */
    private void onMascotaUrgenciaSeleccionada() {
        if (cmbMascotaUrgencia.getSelectedItem() instanceof Mascota) {
            Mascota m = (Mascota) cmbMascotaUrgencia.getSelectedItem();
            idMascotaActual = m.getIdMascota();
            llenarDatosMascota(m);
        }
    }

    /**
     * Llena los labels de datos de mascota con los valores del objeto.
     *
     * @param m mascota cuyos datos mostrar
     */
    private void llenarDatosMascota(Mascota m) {
        lblMascotaNombre.setText(m.getNombre());
        String espRaza = "";
        if (m.getEspecie() != null) espRaza += m.getEspecie().getNombre();
        if (m.getRaza() != null)    espRaza += " / " + m.getRaza().getNombre();
        lblMascotaEspecie.setText(espRaza.isEmpty() ? "—" : espRaza);

        if (m.getDueno() != null) {
            lblMascotaDueno.setText(m.getDueno().getApellido() + ", " + m.getDueno().getNombre());
            lblMascotaDni.setText(m.getDueno().getDni() != null ? m.getDueno().getDni() : "—");
        } else {
            lblMascotaDueno.setText("—");
            lblMascotaDni.setText("—");
        }

        // Calcular edad aproximada si tiene fecha de nacimiento
        if (m.getFechaNacimiento() != null) {
            long anios = java.time.Period.between(m.getFechaNacimiento(), LocalDate.now()).getYears();
            lblMascotaEdad.setText(anios + " año(s)");
        } else {
            lblMascotaEdad.setText("Sin fecha de nacimiento");
        }
    }

    /**
     * Limpia los labels de datos de mascota.
     */
    private void limpiarDatosMascota() {
        lblMascotaNombre.setText("—");
        lblMascotaEspecie.setText("—");
        lblMascotaDueno.setText("—");
        lblMascotaDni.setText("—");
        lblMascotaEdad.setText("—");
    }

    // =========================================================================
    // Diálogo modal para agregar medicamento
    // =========================================================================

    /**
     * Abre el diálogo modal {@link AgregarMedicamentoDialog} para agregar un medicamento
     * a la receta. Si el usuario confirma, agrega una fila a la tabla de medicamentos.
     */
    private void abrirDialogoAgregarMedicamento() {
        try {
            List<Medicamento> medicamentos = consultaService.obtenerMedicamentosActivos();
            if (medicamentos.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No hay medicamentos activos en el sistema.",
                    "Sin medicamentos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Crear y mostrar el diálogo modal
            Window owner = SwingUtilities.getWindowAncestor(this);
            AgregarMedicamentoDialog dlg = new AgregarMedicamentoDialog(
                owner instanceof Frame ? (Frame) owner : null,
                medicamentos,
                consultaService
            );
            dlg.setVisible(true);

            // Si el usuario confirmó, agregar la fila a la tabla
            if (dlg.isConfirmado()) {
                int    idMed     = dlg.getIdMedicamentoSeleccionado();
                String nomMed    = dlg.getNombreMedicamento();
                int    cantidad  = dlg.getCantidad();
                String dosis     = dlg.getDosis();
                String frecuencia = dlg.getFrecuencia();
                String duracion  = dlg.getDuracion();
                int    stockDisp = dlg.getStockDisponible();

                modeloMedicamentos.addRow(new Object[]{
                    idMed, nomMed, cantidad, dosis, frecuencia, duracion, stockDisp
                });
                actualizarResumenMedicamentos();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar medicamentos:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Elimina el medicamento seleccionado de la tabla de receta.
     */
    private void quitarMedicamentoSeleccionado() {
        int fila = tablaMedicamentos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un medicamento de la tabla para quitarlo.",
                "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        modeloMedicamentos.removeRow(fila);
        actualizarResumenMedicamentos();
    }

    /**
     * Actualiza el label de resumen de medicamentos.
     */
    private void actualizarResumenMedicamentos() {
        int total = modeloMedicamentos.getRowCount();
        lblResumenMeds.setText("Total medicamentos: " + total
            + (total > 0 ? " | Stock verificado: ✓" : ""));
        lblResumenMeds.setForeground(total > 0 ? new Color(0, 120, 60) : Color.DARK_GRAY);
    }

    // =========================================================================
    // Lógica de guardado
    // =========================================================================

    /**
     * Valida y guarda una consulta con turno (flujo normal CU-03).
     *
     * <p>Construye la lista de medicamentos desde la tabla y delega al
     * {@link ConsultaMedicaService#registrarConsultaConTurno} que ejecuta el
     * flujo multi-paso SP + stock FIFO + INSERT item_receta.</p>
     */
    private void guardarConsultaConTurno() {
        // ── Validaciones de entrada ───────────────────────────────────────────
        if (idTurnoSeleccionado <= 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un turno pendiente de la tabla.",
                "Turno requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (idMascotaActual <= 0) {
            JOptionPane.showMessageDialog(this,
                "No se pudo determinar la mascota del turno seleccionado.",
                "Mascota no identificada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Veterinario vet = (Veterinario) cmbVeterinarioConsulta.getSelectedItem();
        if (vet == null) {
            JOptionPane.showMessageDialog(this,
                "Seleccione el veterinario que atiende la consulta.",
                "Veterinario requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        idVeterinarioActual = vet.getId();

        String sintomas    = txtSintomas.getText().trim();
        String diagnostico = txtDiagnostico.getText().trim();

        if (sintomas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Los síntomas son obligatorios (*). Complete este campo.",
                "Campo obligatorio", JOptionPane.WARNING_MESSAGE);
            txtSintomas.requestFocus();
            return;
        }
        if (diagnostico.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El diagnóstico es obligatorio (*). Complete este campo.",
                "Campo obligatorio", JOptionPane.WARNING_MESSAGE);
            txtDiagnostico.requestFocus();
            return;
        }

        // ── Confirmar si no hay medicamentos ────────────────────────────────
        int totalMeds = modeloMedicamentos.getRowCount();
        if (totalMeds == 0) {
            int op = JOptionPane.showConfirmDialog(this,
                "No se han agregado medicamentos a la receta.\n"
                + "¿Desea guardar la consulta sin prescripción de medicamentos?",
                "Consulta sin medicamentos", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            if (op != JOptionPane.YES_OPTION) return;
        }

        // ── Construir lista de medicamentos desde la JTable ──────────────────
        List<Map<String, Object>> listaMeds = new ArrayList<>();
        for (int i = 0; i < modeloMedicamentos.getRowCount(); i++) {
            Map<String, Object> med = new HashMap<>();
            med.put("idMedicamento", (Integer) modeloMedicamentos.getValueAt(i, 0));
            med.put("cantidad",      (Integer) modeloMedicamentos.getValueAt(i, 2));
            med.put("dosis",         (String)  modeloMedicamentos.getValueAt(i, 3));
            med.put("frecuencia",    (String)  modeloMedicamentos.getValueAt(i, 4));
            med.put("duracion",      (String)  modeloMedicamentos.getValueAt(i, 5));
            listaMeds.add(med);
        }

        // ── Ejecutar el flujo multi-paso via Service ─────────────────────────
        try {
            btnGuardar.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // El service ejecuta:
            // 1. sp_registrar_consulta_turno → id_consulta via OUT param
            // 2. Para cada med: sp_descontar_stock_fifo(idMed, cantidad)
            // 3. Para cada med: INSERT item_receta (PreparedStatement directo)
            int idConsulta = consultaService.registrarConsultaConTurno(
                idTurnoSeleccionado, idMascotaActual, idVeterinarioActual,
                sintomas, diagnostico, listaMeds
            );

            JOptionPane.showMessageDialog(this,
                "✅ Consulta registrada correctamente.\n"
                + "ID de consulta: " + idConsulta + "\n"
                + (totalMeds > 0
                    ? "Stock actualizado para " + totalMeds + " medicamento(s)."
                    : "Sin medicamentos prescriptos."),
                "Consulta registrada", JOptionPane.INFORMATION_MESSAGE);

            // Cerrar el diálogo tras guardar exitosamente
            try { setClosed(true); } catch (java.beans.PropertyVetoException ex) { /* ignorar */ }

        } catch (StockInsuficienteException e) {
            JOptionPane.showMessageDialog(this,
                "⚠ No hay stock suficiente:\n" + e.getMessage()
                + "\n\nLa operación fue cancelada. Ningún dato fue guardado.",
                "Stock insuficiente (RN-02)", JOptionPane.ERROR_MESSAGE);

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error de base de datos al guardar la consulta:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnGuardar.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Valida y guarda una consulta de urgencia (sin turno previo).
     *
     * <p>Llama al SP {@code sp_registrar_consulta_urgencia} (5 params, IN+OUT)
     * que crea la consulta con {@code id_turno = NULL}.</p>
     */
    private void guardarConsultaUrgencia() {
        Mascota mascota = (Mascota) cmbMascotaUrgencia.getSelectedItem();
        Veterinario vet = (Veterinario) cmbVeterinarioUrgencia.getSelectedItem();

        if (mascota == null) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una mascota para la urgencia.",
                "Mascota requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (vet == null) {
            JOptionPane.showMessageDialog(this,
                "Seleccione el veterinario actuante.",
                "Veterinario requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sintomas    = txtSintomas.getText().trim();
        String diagnostico = txtDiagnostico.getText().trim();

        if (sintomas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Los síntomas son obligatorios (*).",
                "Campo obligatorio", JOptionPane.WARNING_MESSAGE);
            txtSintomas.requestFocus();
            return;
        }
        if (diagnostico.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El diagnóstico es obligatorio (*).",
                "Campo obligatorio", JOptionPane.WARNING_MESSAGE);
            txtDiagnostico.requestFocus();
            return;
        }

        try {
            btnGuardarUrgencia.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            int idConsulta = consultaService.registrarConsultaUrgencia(
                mascota.getIdMascota(), vet.getId(), sintomas, diagnostico
            );

            JOptionPane.showMessageDialog(this,
                "✅ Consulta de urgencia registrada correctamente.\n"
                + "ID de consulta: " + idConsulta,
                "Urgencia registrada", JOptionPane.INFORMATION_MESSAGE);

            try { setClosed(true); } catch (java.beans.PropertyVetoException ex) { /* ignorar */ }

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al registrar la urgencia:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnGuardarUrgencia.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // =========================================================================
    // Clase interna: Diálogo modal para agregar medicamento
    // =========================================================================

    /**
     * Diálogo modal para seleccionar un medicamento, verificar su stock disponible
     * y definir dosis, frecuencia y duración para agregar a la receta.
     *
     * <p>Consulta el stock disponible vía {@link ConsultaMedicaService#stockDisponibleMedicamento}
     * cada vez que el usuario cambia el medicamento seleccionado.</p>
     */
    static class AgregarMedicamentoDialog extends JDialog {

        private final ConsultaMedicaService consultaService;
        private final List<Medicamento>     medicamentos;

        private JComboBox<Medicamento> cmbMedicamento;
        private JLabel                 lblStockDisp;
        private JSpinner               spnCantidad;
        private JTextField             txtDosis;
        private JTextField             txtFrecuencia;
        private JTextField             txtDuracion;

        private boolean confirmado = false;
        private int     stockActual = 0;

        /**
         * Constructor del diálogo modal de agregar medicamento.
         *
         * @param owner           ventana padre
         * @param medicamentos    lista de medicamentos activos disponibles
         * @param consultaService servicio para verificar stock
         */
        AgregarMedicamentoDialog(Frame owner, List<Medicamento> medicamentos,
                                  ConsultaMedicaService consultaService) {
            super(owner, "Agregar Medicamento a la Receta", true);
            this.medicamentos    = medicamentos;
            this.consultaService = consultaService;
            construirUI();
            pack();
            setLocationRelativeTo(owner);
            setResizable(false);
        }

        /**
         * Construye la interfaz del diálogo modal.
         */
        private void construirUI() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));
            panel.setBackground(Color.WHITE);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets  = new Insets(5, 6, 5, 6);
            gbc.fill    = GridBagConstraints.HORIZONTAL;
            gbc.anchor  = GridBagConstraints.WEST;

            // Medicamento
            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
            panel.add(new JLabel("Medicamento *:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
            cmbMedicamento = new JComboBox<>();
            for (Medicamento m : medicamentos) cmbMedicamento.addItem(m);
            cmbMedicamento.setFont(new Font("SansSerif", Font.PLAIN, 12));
            cmbMedicamento.setPreferredSize(new Dimension(280, 28));
            cmbMedicamento.addActionListener(e -> actualizarStockDisponible());
            panel.add(cmbMedicamento, gbc);
            gbc.gridwidth = 1;

            // Stock disponible
            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
            panel.add(new JLabel("Stock disponible:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
            lblStockDisp = new JLabel("Cargando...");
            lblStockDisp.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblStockDisp.setForeground(new Color(0, 100, 200));
            panel.add(lblStockDisp, gbc);
            gbc.gridwidth = 1;

            // Cantidad
            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
            panel.add(new JLabel("Cantidad *:"), gbc);
            gbc.gridx = 1; gbc.weightx = 0.5;
            spnCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
            spnCantidad.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(spnCantidad, gbc);

            // Dosis
            gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
            panel.add(new JLabel("Dosis *:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
            txtDosis = new JTextField("10mg");
            txtDosis.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(txtDosis, gbc);
            gbc.gridwidth = 1;

            // Frecuencia
            gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
            panel.add(new JLabel("Frecuencia:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
            txtFrecuencia = new JTextField("cada 8 horas");
            txtFrecuencia.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(txtFrecuencia, gbc);
            gbc.gridwidth = 1;

            // Duración
            gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
            panel.add(new JLabel("Duración:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.gridwidth = 2;
            txtDuracion = new JTextField("7 días");
            txtDuracion.setFont(new Font("SansSerif", Font.PLAIN, 12));
            panel.add(txtDuracion, gbc);
            gbc.gridwidth = 1;

            // Separador
            gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 3;
            panel.add(new JSeparator(), gbc);
            gbc.gridwidth = 1;

            // Botones
            gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.EAST;
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnPanel.setBackground(Color.WHITE);

            JButton btnAgregar   = new JButton("Agregar");
            JButton btnCancelarD = new JButton("Cancelar");
            btnAgregar.setFont(new Font("SansSerif", Font.BOLD, 12));
            btnAgregar.setBackground(new Color(0, 120, 215));
            btnAgregar.setForeground(Color.WHITE);
            btnAgregar.setFocusPainted(false);
            btnAgregar.addActionListener(e -> validarYConfirmar());
            btnCancelarD.addActionListener(e -> dispose());

            btnPanel.add(btnAgregar);
            btnPanel.add(btnCancelarD);
            panel.add(btnPanel, gbc);

            setContentPane(panel);

            // Cargar stock inicial al abrir
            SwingUtilities.invokeLater(this::actualizarStockDisponible);
        }

        /**
         * Consulta y actualiza el label de stock disponible del medicamento seleccionado.
         * Ajusta el máximo del spinner a ese stock.
         */
        private void actualizarStockDisponible() {
            Medicamento med = (Medicamento) cmbMedicamento.getSelectedItem();
            if (med == null) { lblStockDisp.setText("—"); return; }

            try {
                stockActual = consultaService.stockDisponibleMedicamento(med.getIdMedicamento());

                if (stockActual <= 0) {
                    lblStockDisp.setText("⚠ Sin stock disponible");
                    lblStockDisp.setForeground(new Color(200, 0, 0));
                    ((SpinnerNumberModel) spnCantidad.getModel()).setMaximum(0);
                    ((SpinnerNumberModel) spnCantidad.getModel()).setValue(0);
                } else {
                    lblStockDisp.setText(stockActual + " unidades disponibles");
                    lblStockDisp.setForeground(new Color(0, 140, 60));
                    ((SpinnerNumberModel) spnCantidad.getModel()).setMaximum(stockActual);
                    ((SpinnerNumberModel) spnCantidad.getModel()).setValue(1);
                }
            } catch (SQLException e) {
                lblStockDisp.setText("Error al consultar stock");
                lblStockDisp.setForeground(Color.RED);
            }
        }

        /**
         * Valida los campos obligatorios y cierra el diálogo marcándolo como confirmado.
         */
        private void validarYConfirmar() {
            Medicamento med = (Medicamento) cmbMedicamento.getSelectedItem();
            if (med == null) {
                JOptionPane.showMessageDialog(this,
                    "Seleccione un medicamento.", "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            int cantidad = (Integer) spnCantidad.getValue();
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this,
                    "La cantidad debe ser mayor a 0.", "Cantidad inválida",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (cantidad > stockActual) {
                JOptionPane.showMessageDialog(this,
                    "La cantidad (" + cantidad + ") supera el stock disponible ("
                    + stockActual + "). (RN-02)",
                    "Stock insuficiente", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String dosis = txtDosis.getText().trim();
            if (dosis.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "La dosis es obligatoria.", "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
                txtDosis.requestFocus();
                return;
            }

            confirmado = true;
            dispose();
        }

        /** @return {@code true} si el usuario confirmó la adición del medicamento */
        public boolean isConfirmado()               { return confirmado; }

        /** @return ID del medicamento seleccionado */
        public int getIdMedicamentoSeleccionado()   {
            return ((Medicamento) cmbMedicamento.getSelectedItem()).getIdMedicamento();
        }

        /** @return Nombre comercial del medicamento seleccionado */
        public String getNombreMedicamento()        {
            return ((Medicamento) cmbMedicamento.getSelectedItem()).getNombreComercial();
        }

        /** @return cantidad indicada en el spinner */
        public int    getCantidad()                 { return (Integer) spnCantidad.getValue(); }

        /** @return dosis ingresada */
        public String getDosis()                    { return txtDosis.getText().trim(); }

        /** @return frecuencia ingresada (puede ser vacía) */
        public String getFrecuencia()               { return txtFrecuencia.getText().trim(); }

        /** @return duración ingresada (puede ser vacía) */
        public String getDuracion()                 { return txtDuracion.getText().trim(); }

        /** @return stock disponible consultado para el medicamento */
        public int    getStockDisponible()          { return stockActual; }
    }
}
