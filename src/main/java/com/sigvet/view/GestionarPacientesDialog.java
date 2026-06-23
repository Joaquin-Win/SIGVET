package com.sigvet.view;

import com.sigvet.model.*;
import com.sigvet.model.enums.EstadoRegistro;
import com.sigvet.service.PacienteService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para el caso de uso CU-06: Gestionar Dueños y Mascotas.
 *
 * <p>Implementa un {@link JInternalFrame} con dos pestañas:</p>
 * <ul>
 *   <li><strong>Tab 1 — Dueños:</strong> CRUD completo con búsqueda en tiempo real,
 *       baja lógica y anonimización (Ley 25.326 de Protección de Datos Personales).</li>
 *   <li><strong>Tab 2 — Mascotas:</strong> CRUD por dueño con combo cascada
 *       especie → raza cargado via {@link PacienteService#obtenerRazasPorEspecie}.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see PacienteService
 */
public class GestionarPacientesDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    private static final String[] COL_DUENOS = {
        "ID", "DNI", "Apellido", "Nombre", "Teléfono", "Email", "Estado"
    };
    private static final String[] COL_MASCOTAS = {
        "ID", "Nombre", "Especie", "Raza", "F. Nacimiento", "Sexo", "Color", "Estado"
    };
    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================================
    // Service
    // =========================================================================

    private final PacienteService pacienteService;

    // =========================================================================
    // Estado interno
    // =========================================================================

    /** Dueños mostrados actualmente en la tabla (para acceso por índice). */
    private final List<Dueno>   duenosMostrados  = new ArrayList<>();
    /** Mascotas mostradas en la tabla de la pestaña Mascotas. */
    private final List<Mascota> mascotasMostradas = new ArrayList<>();
    /** ID del dueño seleccionado en Tab 1 (se propaga a Tab 2). */
    private int idDuenoSeleccionado  = 0;
    /** ID de la mascota seleccionada en Tab 2. */
    private int idMascotaSeleccionada = 0;
    /** Modo "nuevo" para el formulario de dueño. */
    private boolean modoNuevoDueno    = false;
    /** Modo "nuevo" para el formulario de mascota. */
    private boolean modoNuevaMascota  = false;

    // =========================================================================
    // Componentes — Tab 1: Dueños
    // =========================================================================

    private JTextField    txtBuscarDueno;
    private JTable        tablaDuenos;
    private DefaultTableModel modeloDuenos;
    private TableRowSorter<DefaultTableModel> sorterDuenos;

    private JTextField txtDni;
    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtTelefono;
    private JTextField txtDireccion;
    private JTextField txtEmail;

    private JButton btnNuevoDueno;
    private JButton btnGuardarDueno;
    private JButton btnModificarDueno;
    private JButton btnBajaDueno;
    private JButton btnAnonimizar;

    // =========================================================================
    // Componentes — Tab 2: Mascotas
    // =========================================================================

    private JComboBox<Dueno>   cmbDuenoMascota;
    private JTable             tablaMascotas;
    private DefaultTableModel  modeloMascotas;

    private JTextField          txtNombreMascota;
    private JComboBox<Especie>  cmbEspecie;
    private JComboBox<Raza>     cmbRaza;
    private JTextField          txtFechaNac;
    private JComboBox<String>   cmbSexo;
    private JTextField          txtColor;
    private JTextArea           txtSenas;

    private JButton btnNuevaMascota;
    private JButton btnGuardarMascota;
    private JButton btnModificarMascota;
    private JButton btnBajaMascota;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de gestión de dueños y mascotas.
     *
     * @param pacienteService servicio de pacientes inyectado
     */
    public GestionarPacientesDialog(PacienteService pacienteService) {
        super("Gestionar Due\u00F1os y Mascotas", true, true, true, true);
        this.pacienteService = pacienteService;

        construirUI();
        cargarDuenos();
        cargarEspecies();
        cargarDuenosEnCombo();
    }

    // =========================================================================
    // Construcción de la UI
    // =========================================================================

    /**
     * Construye la interfaz con JTabbedPane de dos pestañas.
     */
    private void construirUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 249, 252));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.addTab("👤 Dueños",    construirTabDuenos());
        tabs.addTab("🐾 Mascotas",  construirTabMascotas());

        // Al cambiar a pestaña Mascotas → sincronizar selección de dueño
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1 && idDuenoSeleccionado > 0) {
                seleccionarDuenoEnCombo(idDuenoSeleccionado);
                cargarMascotasDeDueno(idDuenoSeleccionado);
            }
        });

        add(tabs, BorderLayout.CENTER);
        add(construirBarraBotones(), BorderLayout.SOUTH);
    }

    private JPanel construirBarraBotones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        p.setBackground(new Color(240, 244, 252));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCerrar.addActionListener(e -> {
            try { setClosed(true); }
            catch (java.beans.PropertyVetoException ex) { /* ignorar */ }
        });
        p.add(btnCerrar);
        return p;
    }

    // =========================================================================
    // Tab 1: Dueños
    // =========================================================================

    /**
     * Construye la pestaña de gestión de dueños.
     *
     * @return panel de la pestaña
     */
    private JPanel construirTabDuenos() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            construirPanelListaDuenos(),
            construirPanelFormularioDueno()
        );
        split.setDividerLocation(480);
        split.setResizeWeight(0.55);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel izquierdo con la lista y búsqueda de dueños.
     *
     * @return panel de lista
     */
    private JPanel construirPanelListaDuenos() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Dueños registrados",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        // Campo de búsqueda con DocumentListener
        JPanel panelBusqueda = new JPanel(new BorderLayout(4, 0));
        panelBusqueda.setBackground(Color.WHITE);
        panelBusqueda.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        txtBuscarDueno = new JTextField();
        txtBuscarDueno.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtBuscarDueno.setToolTipText("Buscar por DNI o nombre/apellido");
        txtBuscarDueno.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrarDuenos(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrarDuenos(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrarDuenos(); }
        });
        panelBusqueda.add(new JLabel("🔍 Buscar:"), BorderLayout.WEST);
        panelBusqueda.add(txtBuscarDueno, BorderLayout.CENTER);

        // Tabla de dueños
        modeloDuenos = new DefaultTableModel(COL_DUENOS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaDuenos = new JTable(modeloDuenos);
        tablaDuenos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaDuenos.setRowHeight(22);
        tablaDuenos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaDuenos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaDuenos.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaDuenos.setFillsViewportHeight(true);

        // Ocultar columna ID
        tablaDuenos.getColumnModel().getColumn(0).setMinWidth(0);
        tablaDuenos.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaDuenos.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 90, 130, 120, 100, 140, 70};
        for (int i = 1; i < anchos.length; i++) {
            tablaDuenos.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Colorear filas inactivas
        tablaDuenos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                if (!sel) {
                    Object est = t.getModel().getValueAt(
                        t.convertRowIndexToModel(row), 6);
                    if ("Inactivo".equals(est)) {
                        c.setBackground(new Color(245, 245, 245));
                        c.setForeground(Color.GRAY);
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 255));
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        sorterDuenos = new TableRowSorter<>(modeloDuenos);
        tablaDuenos.setRowSorter(sorterDuenos);
        tablaDuenos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onDuenoSeleccionado();
        });

        JScrollPane scroll = new JScrollPane(tablaDuenos);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(panelBusqueda, BorderLayout.NORTH);
        panel.add(scroll,        BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel derecho con el formulario de alta/modificación de dueños.
     *
     * @return panel de formulario
     */
    private JPanel construirPanelFormularioDueno() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Datos del Dueño  (* obligatorio)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        String[][] campos = {
            {"DNI *:", null},
            {"Apellido *:", null},
            {"Nombre *:", null},
            {"Teléfono *:", null},
            {"Dirección:", null},
            {"Email:", null}
        };
        txtDni       = crearTextField(false);
        txtApellido  = crearTextField(false);
        txtNombre    = crearTextField(false);
        txtTelefono  = crearTextField(false);
        txtDireccion = crearTextField(false);
        txtEmail     = crearTextField(false);
        JTextField[] campos2 = {txtDni, txtApellido, txtNombre, txtTelefono, txtDireccion, txtEmail};

        for (int i = 0; i < campos.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            form.add(new JLabel(campos[i][0]), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0;
            form.add(campos2[i], gbc);
        }
        gbc.gridx = 0; gbc.gridy = 6; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL;
        form.add(Box.createVerticalGlue(), gbc);

        // Botones CRUD
        JPanel btnPanel = new JPanel(new GridLayout(5, 1, 0, 6));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        btnNuevoDueno    = new JButton("➕ Nuevo");
        btnGuardarDueno  = new JButton("💾 Guardar");
        btnModificarDueno = new JButton("✏ Modificar");
        btnBajaDueno     = new JButton("🗑 Baja Lógica");
        btnAnonimizar    = new JButton("🔒 Anonimizar");

        Font f = new Font("SansSerif", Font.PLAIN, 12);
        for (JButton b : new JButton[]{btnNuevoDueno, btnGuardarDueno,
                btnModificarDueno, btnBajaDueno, btnAnonimizar}) {
            b.setFont(f);
            b.setFocusPainted(false);
        }
        btnGuardarDueno.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnAnonimizar.setBackground(new Color(180, 60, 60));
        btnAnonimizar.setForeground(Color.WHITE);

        btnGuardarDueno.setEnabled(false);
        btnModificarDueno.setEnabled(false);
        btnBajaDueno.setEnabled(false);
        btnAnonimizar.setEnabled(false);

        btnNuevoDueno.addActionListener(e    -> iniciarModoNuevoDueno());
        btnGuardarDueno.addActionListener(e  -> guardarDueno());
        btnModificarDueno.addActionListener(e -> modificarDueno());
        btnBajaDueno.addActionListener(e     -> bajaLogicaDueno());
        btnAnonimizar.addActionListener(e    -> anonimizarDueno());

        btnPanel.add(btnNuevoDueno);
        btnPanel.add(btnGuardarDueno);
        btnPanel.add(btnModificarDueno);
        btnPanel.add(btnBajaDueno);
        btnPanel.add(btnAnonimizar);

        panel.add(form,     BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Tab 2: Mascotas
    // =========================================================================

    /**
     * Construye la pestaña de gestión de mascotas.
     *
     * @return panel de la pestaña
     */
    private JPanel construirTabMascotas() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        panel.add(construirPanelSelectorDueno(),   BorderLayout.NORTH);
        panel.add(construirPanelTablaMascotas(),   BorderLayout.CENTER);
        panel.add(construirPanelFormularioMascota(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Construye el selector de dueño para la pestaña de mascotas.
     *
     * @return panel selector
     */
    private JPanel construirPanelSelectorDueno() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setBackground(new Color(235, 240, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JLabel lbl = new JLabel("Seleccionar Dueño:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));

        cmbDuenoMascota = new JComboBox<>();
        cmbDuenoMascota.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbDuenoMascota.setPreferredSize(new Dimension(280, 28));
        cmbDuenoMascota.addActionListener(e -> {
            if (cmbDuenoMascota.getSelectedItem() instanceof Dueno d) {
                idDuenoSeleccionado = d.getId();
                cargarMascotasDeDueno(d.getId());
            }
        });

        panel.add(lbl);
        panel.add(cmbDuenoMascota);
        return panel;
    }

    /**
     * Construye la tabla de mascotas del dueño seleccionado.
     *
     * @return panel de tabla
     */
    private JPanel construirPanelTablaMascotas() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Mascotas del dueño",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        modeloMascotas = new DefaultTableModel(COL_MASCOTAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaMascotas = new JTable(modeloMascotas);
        tablaMascotas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaMascotas.setRowHeight(22);
        tablaMascotas.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaMascotas.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaMascotas.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaMascotas.setFillsViewportHeight(true);

        tablaMascotas.getColumnModel().getColumn(0).setMinWidth(0);
        tablaMascotas.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaMascotas.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 110, 100, 100, 100, 50, 90, 70};
        for (int i = 1; i < anchos.length; i++) {
            tablaMascotas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        tablaMascotas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onMascotaSeleccionada();
        });

        JScrollPane scroll = new JScrollPane(tablaMascotas);
        scroll.setPreferredSize(new Dimension(0, 160));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el formulario de alta/modificación de mascotas con combo cascada.
     *
     * @return panel de formulario
     */
    private JPanel construirPanelFormularioMascota() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Datos de la Mascota  (* obligatorio)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Nombre
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Nombre *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtNombreMascota = crearTextField(false);
        form.add(txtNombreMascota, gbc);

        // Especie (con listener de cascada)
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Especie *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbEspecie = new JComboBox<>();
        cmbEspecie.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbEspecie.setEnabled(false);
        // Combo cascada especie → raza
        cmbEspecie.addActionListener(e -> {
            if (cmbEspecie.getSelectedItem() instanceof Especie esp) {
                cargarRazasPorEspecie(esp.getIdEspecie());
            }
        });
        form.add(cmbEspecie, gbc);

        // Raza
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        form.add(new JLabel("Raza *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbRaza = new JComboBox<>();
        cmbRaza.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbRaza.setEnabled(false);
        form.add(cmbRaza, gbc);

        // Fecha de nacimiento
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        form.add(new JLabel("F. Nacimiento (dd/MM/yyyy):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtFechaNac = crearTextField(false);
        txtFechaNac.setToolTipText("Formato: dd/MM/yyyy");
        form.add(txtFechaNac, gbc);

        // Sexo
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        form.add(new JLabel("Sexo:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbSexo = new JComboBox<>(new String[]{"M", "F", "(Sin especificar)"});
        cmbSexo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbSexo.setEnabled(false);
        form.add(cmbSexo, gbc);

        // Color
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        form.add(new JLabel("Color:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtColor = crearTextField(false);
        form.add(txtColor, gbc);

        // Señas particulares
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Señas particulares:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        txtSenas = new JTextArea(2, 20);
        txtSenas.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtSenas.setLineWrap(true);
        txtSenas.setEnabled(false);
        form.add(new JScrollPane(txtSenas), gbc);
        gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Botones
        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 0, 5));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        btnNuevaMascota    = new JButton("➕ Nueva");
        btnGuardarMascota  = new JButton("💾 Guardar");
        btnModificarMascota = new JButton("✏ Modificar");
        btnBajaMascota     = new JButton("🗑 Baja Lógica");

        Font fm = new Font("SansSerif", Font.PLAIN, 12);
        for (JButton b : new JButton[]{btnNuevaMascota, btnGuardarMascota,
                btnModificarMascota, btnBajaMascota}) b.setFont(fm);
        btnGuardarMascota.setFont(new Font("SansSerif", Font.BOLD, 12));

        btnGuardarMascota.setEnabled(false);
        btnModificarMascota.setEnabled(false);
        btnBajaMascota.setEnabled(false);

        btnNuevaMascota.addActionListener(e    -> iniciarModoNuevaMascota());
        btnGuardarMascota.addActionListener(e  -> guardarMascota());
        btnModificarMascota.addActionListener(e -> modificarMascota());
        btnBajaMascota.addActionListener(e     -> bajaLogicaMascota());

        btnPanel.add(btnNuevaMascota);
        btnPanel.add(btnGuardarMascota);
        btnPanel.add(btnModificarMascota);
        btnPanel.add(btnBajaMascota);

        panel.add(form,     BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.EAST);
        return panel;
    }

    // =========================================================================
    // Carga de datos
    // =========================================================================

    /**
     * Carga todos los dueños activos e inactivos en la tabla principal.
     */
    private void cargarDuenos() {
        modeloDuenos.setRowCount(0);
        duenosMostrados.clear();
        idDuenoSeleccionado = 0;
        limpiarFormularioDueno();
        habilitarBotonesDueno(false);

        try {
            List<Dueno> lista = pacienteService.obtenerDuenos();
            for (Dueno d : lista) {
                modeloDuenos.addRow(new Object[]{
                    d.getId(), d.getDni(), d.getApellido(), d.getNombre(),
                    d.getTelefono(), d.getEmail(),
                    d.getEstado() != null ? d.getEstado().name() : "—"
                });
                duenosMostrados.add(d);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar dueños:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga los dueños en el combo de la pestaña de mascotas.
     */
    private void cargarDuenosEnCombo() {
        try {
            List<Dueno> lista = pacienteService.obtenerDuenos();
            cmbDuenoMascota.removeAllItems();
            for (Dueno d : lista) cmbDuenoMascota.addItem(d);
        } catch (SQLException e) {
            System.err.println("Error al cargar combo dueños: " + e.getMessage());
        }
    }

    /**
     * Carga las mascotas de un dueño específico en la tabla de mascotas.
     *
     * @param idDueno ID del dueño cuyas mascotas se cargarán
     */
    private void cargarMascotasDeDueno(int idDueno) {
        modeloMascotas.setRowCount(0);
        mascotasMostradas.clear();
        idMascotaSeleccionada = 0;
        limpiarFormularioMascota();
        habilitarBotonesMascota(false);

        if (idDueno <= 0) return;

        try {
            List<Mascota> lista = pacienteService.obtenerMascotasPorDueno(idDueno);
            for (Mascota m : lista) {
                String esp = m.getEspecie() != null ? m.getEspecie().getNombre() : "—";
                String raza = m.getRaza() != null ? m.getRaza().getNombre() : "—";
                String fNac = m.getFechaNacimiento() != null
                    ? m.getFechaNacimiento().format(FMT_FECHA) : "—";
                modeloMascotas.addRow(new Object[]{
                    m.getIdMascota(), m.getNombre(), esp, raza, fNac,
                    m.getSexo() != null ? m.getSexo() : "—",
                    m.getColor() != null ? m.getColor() : "—",
                    m.getEstado() != null ? m.getEstado().name() : "—"
                });
                mascotasMostradas.add(m);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar mascotas:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga las especies en el combo de la pestaña de mascotas.
     */
    private void cargarEspecies() {
        try {
            List<Especie> especies = pacienteService.obtenerEspecies();
            cmbEspecie.removeAllItems();
            for (Especie esp : especies) cmbEspecie.addItem(esp);
        } catch (SQLException e) {
            System.err.println("Error al cargar especies: " + e.getMessage());
        }
    }

    /**
     * Carga las razas correspondientes a la especie seleccionada (combo cascada).
     *
     * @param idEspecie ID de la especie seleccionada
     */
    private void cargarRazasPorEspecie(int idEspecie) {
        try {
            List<Raza> razas = pacienteService.obtenerRazasPorEspecie(idEspecie);
            cmbRaza.removeAllItems();
            for (Raza r : razas) cmbRaza.addItem(r);
        } catch (SQLException e) {
            System.err.println("Error al cargar razas: " + e.getMessage());
        }
    }

    // =========================================================================
    // Lógica de interacción — Dueños
    // =========================================================================

    /**
     * Filtra la tabla de dueños con el texto del campo de búsqueda (client-side).
     */
    private void filtrarDuenos() {
        String texto = txtBuscarDueno.getText().trim();
        if (texto.isEmpty()) {
            sorterDuenos.setRowFilter(null);
        } else {
            // Filtrar columnas DNI (1), Apellido (2) y Nombre (3)
            sorterDuenos.setRowFilter(RowFilter.orFilter(List.of(
                RowFilter.regexFilter("(?i)" + texto, 1),
                RowFilter.regexFilter("(?i)" + texto, 2),
                RowFilter.regexFilter("(?i)" + texto, 3)
            )));
        }
    }

    /**
     * Responde a la selección de un dueño en la tabla: llena el formulario.
     */
    private void onDuenoSeleccionado() {
        int fila = tablaDuenos.getSelectedRow();
        if (fila < 0) {
            idDuenoSeleccionado = 0;
            limpiarFormularioDueno();
            habilitarBotonesDueno(false);
            return;
        }
        int filaModelo = tablaDuenos.convertRowIndexToModel(fila);
        if (filaModelo >= duenosMostrados.size()) return;

        Dueno d = duenosMostrados.get(filaModelo);
        idDuenoSeleccionado = d.getId();
        modoNuevoDueno = false;

        txtDni.setText(d.getDni() != null ? d.getDni() : "");
        txtApellido.setText(d.getApellido() != null ? d.getApellido() : "");
        txtNombre.setText(d.getNombre() != null ? d.getNombre() : "");
        txtTelefono.setText(d.getTelefono() != null ? d.getTelefono() : "");
        txtDireccion.setText(d.getDireccion() != null ? d.getDireccion() : "");
        txtEmail.setText(d.getEmail() != null ? d.getEmail() : "");

        habilitarCamposDueno(true);
        habilitarBotonesDueno(true);
        btnGuardarDueno.setEnabled(false);
    }

    /** Activa el modo de alta de nuevo dueño. */
    private void iniciarModoNuevoDueno() {
        modoNuevoDueno = true;
        idDuenoSeleccionado = 0;
        tablaDuenos.clearSelection();
        limpiarFormularioDueno();
        habilitarCamposDueno(true);
        btnGuardarDueno.setEnabled(true);
        btnModificarDueno.setEnabled(false);
        btnBajaDueno.setEnabled(false);
        btnAnonimizar.setEnabled(false);
        txtDni.requestFocus();
    }

    /** Valida y da de alta un nuevo dueño. */
    private void guardarDueno() {
        if (!modoNuevoDueno) return;
        try {
            Dueno d = construirDuenoDesdeFormulario();
            pacienteService.altaDueno(d);
            JOptionPane.showMessageDialog(this,
                "Dueño '" + d.getApellido() + ", " + d.getNombre() + "' registrado.",
                "Alta exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarDuenos();
            cargarDuenosEnCombo();
            modoNuevoDueno = false;
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al guardar el dueño:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Valida y modifica el dueño seleccionado. */
    private void modificarDueno() {
        if (idDuenoSeleccionado <= 0) return;
        int op = JOptionPane.showConfirmDialog(this,
            "¿Confirmar modificación del dueño?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            Dueno d = construirDuenoDesdeFormulario();
            d.setId(idDuenoSeleccionado);
            pacienteService.modificarDueno(d);
            JOptionPane.showMessageDialog(this,
                "Datos actualizados correctamente.",
                "Modificación exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarDuenos();
            cargarDuenosEnCombo();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al modificar:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Aplica baja lógica al dueño seleccionado previa confirmación. */
    private void bajaLogicaDueno() {
        if (idDuenoSeleccionado <= 0) return;
        String nombre = txtApellido.getText() + ", " + txtNombre.getText();
        int op = JOptionPane.showConfirmDialog(this,
            "¿Dar de baja al dueño '" + nombre + "'?\nSe conservará el historial clínico.",
            "Confirmar baja lógica", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            pacienteService.bajaLogicaDueno(idDuenoSeleccionado);
            JOptionPane.showMessageDialog(this,
                "Dueño dado de baja correctamente.",
                "Baja exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarDuenos();
            cargarDuenosEnCombo();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al aplicar baja:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Anonimiza los datos personales del dueño seleccionado de acuerdo a la Ley 25.326.
     * Muestra una advertencia clara e irreversible antes de proceder.
     */
    private void anonimizarDueno() {
        if (idDuenoSeleccionado <= 0) return;

        // Confirmación con advertencia de Ley 25.326
        int op = JOptionPane.showConfirmDialog(this,
            "⚠ ADVERTENCIA — Ley 25.326 de Protección de Datos Personales\n\n"
            + "Esta acción reemplazará TODOS los datos personales del dueño\n"
            + "(nombre, DNI, teléfono, email, dirección) por valores genéricos.\n\n"
            + "Esta acción es IRREVERSIBLE. El historial clínico se conservará\n"
            + "pero sin datos identificatorios del propietario.\n\n"
            + "¿Confirma que desea anonimizar este dueño?",
            "⚠ ANONIMIZACIÓN IRREVERSIBLE (Ley 25.326)",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        try {
            pacienteService.anonimizarDueno(idDuenoSeleccionado);
            JOptionPane.showMessageDialog(this,
                "Dueño anonimizado correctamente conforme a la Ley 25.326.",
                "Anonimización exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarDuenos();
            cargarDuenosEnCombo();
            limpiarFormularioDueno();
            habilitarBotonesDueno(false);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al anonimizar:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Construye un objeto {@link Dueno} desde los valores del formulario.
     *
     * @return dueño construido
     * @throws IllegalArgumentException si faltan campos obligatorios
     */
    private Dueno construirDuenoDesdeFormulario() {
        String dni      = txtDni.getText().trim();
        String apellido = txtApellido.getText().trim();
        String nombre   = txtNombre.getText().trim();
        String tel      = txtTelefono.getText().trim();

        if (dni.isEmpty())      throw new IllegalArgumentException("El DNI es obligatorio (*).");
        if (apellido.isEmpty()) throw new IllegalArgumentException("El apellido es obligatorio (*).");
        if (nombre.isEmpty())   throw new IllegalArgumentException("El nombre es obligatorio (*).");
        if (tel.isEmpty())      throw new IllegalArgumentException("El teléfono es obligatorio (*).");

        Dueno d = new Dueno();
        d.setDni(dni);
        d.setApellido(apellido);
        d.setNombre(nombre);
        d.setTelefono(tel);
        d.setDireccion(txtDireccion.getText().trim());
        d.setEmail(txtEmail.getText().trim());
        d.setEstado(EstadoRegistro.Activo);
        return d;
    }

    // =========================================================================
    // Lógica de interacción — Mascotas
    // =========================================================================

    /** Responde a la selección de mascota en la tabla de mascotas. */
    private void onMascotaSeleccionada() {
        int fila = tablaMascotas.getSelectedRow();
        if (fila < 0 || fila >= mascotasMostradas.size()) {
            idMascotaSeleccionada = 0;
            limpiarFormularioMascota();
            habilitarBotonesMascota(false);
            return;
        }
        Mascota m = mascotasMostradas.get(fila);
        idMascotaSeleccionada = m.getIdMascota();
        modoNuevaMascota = false;

        txtNombreMascota.setText(m.getNombre());
        txtFechaNac.setText(m.getFechaNacimiento() != null
            ? m.getFechaNacimiento().format(FMT_FECHA) : "");
        txtColor.setText(m.getColor() != null ? m.getColor() : "");
        txtSenas.setText(m.getSenasParticulares() != null ? m.getSenasParticulares() : "");

        String sexo = m.getSexo() != null ? m.getSexo() : "(Sin especificar)";
        for (int i = 0; i < cmbSexo.getItemCount(); i++) {
            if (cmbSexo.getItemAt(i).equals(sexo)) { cmbSexo.setSelectedIndex(i); break; }
        }

        // Seleccionar especie
        if (m.getEspecie() != null) {
            for (int i = 0; i < cmbEspecie.getItemCount(); i++) {
                if (cmbEspecie.getItemAt(i).getIdEspecie() == m.getEspecie().getIdEspecie()) {
                    cmbEspecie.setSelectedIndex(i);
                    break;
                }
            }
        }
        // Seleccionar raza (después de cargar por especie)
        if (m.getRaza() != null) {
            for (int i = 0; i < cmbRaza.getItemCount(); i++) {
                if (cmbRaza.getItemAt(i).getIdRaza() == m.getRaza().getIdRaza()) {
                    cmbRaza.setSelectedIndex(i);
                    break;
                }
            }
        }

        habilitarCamposMascota(true);
        habilitarBotonesMascota(true);
        btnGuardarMascota.setEnabled(false);
    }

    /** Activa el modo de alta de nueva mascota. */
    private void iniciarModoNuevaMascota() {
        if (idDuenoSeleccionado <= 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un dueño antes de agregar una mascota.",
                "Dueño requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        modoNuevaMascota = true;
        idMascotaSeleccionada = 0;
        tablaMascotas.clearSelection();
        limpiarFormularioMascota();
        habilitarCamposMascota(true);
        btnGuardarMascota.setEnabled(true);
        btnModificarMascota.setEnabled(false);
        btnBajaMascota.setEnabled(false);
        txtNombreMascota.requestFocus();
    }

    /** Valida y da de alta una nueva mascota asociada al dueño seleccionado. */
    private void guardarMascota() {
        if (!modoNuevaMascota) return;
        try {
            Mascota m = construirMascotaDesdeFormulario();
            pacienteService.altaMascota(m);
            JOptionPane.showMessageDialog(this,
                "Mascota '" + m.getNombre() + "' registrada correctamente.",
                "Alta exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarMascotasDeDueno(idDuenoSeleccionado);
            modoNuevaMascota = false;
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al guardar la mascota:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Valida y modifica la mascota seleccionada. */
    private void modificarMascota() {
        if (idMascotaSeleccionada <= 0) return;
        int op = JOptionPane.showConfirmDialog(this,
            "¿Confirmar modificación de la mascota?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            Mascota m = construirMascotaDesdeFormulario();
            m.setIdMascota(idMascotaSeleccionada);
            pacienteService.modificarMascota(m);
            JOptionPane.showMessageDialog(this,
                "Mascota modificada correctamente.",
                "Modificación exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarMascotasDeDueno(idDuenoSeleccionado);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al modificar:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Aplica baja lógica a la mascota seleccionada. */
    private void bajaLogicaMascota() {
        if (idMascotaSeleccionada <= 0) return;
        int op = JOptionPane.showConfirmDialog(this,
            "¿Dar de baja a la mascota '" + txtNombreMascota.getText() + "'?",
            "Confirmar baja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;
        try {
            pacienteService.bajaLogicaMascota(idMascotaSeleccionada);
            JOptionPane.showMessageDialog(this,
                "Mascota dada de baja correctamente.",
                "Baja exitosa", JOptionPane.INFORMATION_MESSAGE);
            cargarMascotasDeDueno(idDuenoSeleccionado);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al aplicar baja:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Construye un objeto {@link Mascota} desde los valores del formulario de mascotas.
     *
     * @return mascota construida
     * @throws IllegalArgumentException si faltan campos obligatorios o la fecha es inválida
     */
    private Mascota construirMascotaDesdeFormulario() {
        String nombre = txtNombreMascota.getText().trim();
        if (nombre.isEmpty()) throw new IllegalArgumentException("El nombre de la mascota es obligatorio (*).");

        Especie esp = (Especie) cmbEspecie.getSelectedItem();
        if (esp == null) throw new IllegalArgumentException("La especie es obligatoria (*).");

        Mascota m = new Mascota();
        m.setNombre(nombre);
        m.setEspecie(esp);
        m.setRaza((Raza) cmbRaza.getSelectedItem());

        // Parsear fecha de nacimiento
        String fechaStr = txtFechaNac.getText().trim();
        if (!fechaStr.isEmpty()) {
            try {
                m.setFechaNacimiento(LocalDate.parse(fechaStr, FMT_FECHA));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(
                    "Formato de fecha inválido. Use dd/MM/yyyy.");
            }
        }

        String sexo = (String) cmbSexo.getSelectedItem();
        m.setSexo("(Sin especificar)".equals(sexo) ? null : sexo);
        m.setColor(txtColor.getText().trim().isEmpty() ? null : txtColor.getText().trim());
        m.setSenasParticulares(txtSenas.getText().trim().isEmpty() ? null : txtSenas.getText().trim());
        m.setEstado(EstadoRegistro.Activo);

        // Asociar al dueño seleccionado
        Dueno dueno = new Dueno();
        dueno.setId(idDuenoSeleccionado);
        m.setDueno(dueno);

        return m;
    }

    // =========================================================================
    // Auxiliares de UI
    // =========================================================================

    /** Sincroniza el combo de dueños de la pestaña de mascotas con el ID dado. */
    private void seleccionarDuenoEnCombo(int idDueno) {
        for (int i = 0; i < cmbDuenoMascota.getItemCount(); i++) {
            Dueno d = cmbDuenoMascota.getItemAt(i);
            if (d != null && d.getId() == idDueno) {
                cmbDuenoMascota.setSelectedIndex(i);
                return;
            }
        }
    }

    private void limpiarFormularioDueno() {
        txtDni.setText(""); txtApellido.setText(""); txtNombre.setText("");
        txtTelefono.setText(""); txtDireccion.setText(""); txtEmail.setText("");
        habilitarCamposDueno(false);
    }

    private void limpiarFormularioMascota() {
        txtNombreMascota.setText(""); txtFechaNac.setText("");
        txtColor.setText(""); txtSenas.setText("");
        habilitarCamposMascota(false);
    }

    private void habilitarCamposDueno(boolean h) {
        for (JTextField tf : new JTextField[]{txtDni, txtApellido, txtNombre,
                txtTelefono, txtDireccion, txtEmail}) tf.setEnabled(h);
    }

    private void habilitarBotonesDueno(boolean h) {
        btnModificarDueno.setEnabled(h);
        btnBajaDueno.setEnabled(h);
        btnAnonimizar.setEnabled(h);
    }

    private void habilitarCamposMascota(boolean h) {
        txtNombreMascota.setEnabled(h); cmbEspecie.setEnabled(h);
        cmbRaza.setEnabled(h); txtFechaNac.setEnabled(h);
        cmbSexo.setEnabled(h); txtColor.setEnabled(h); txtSenas.setEnabled(h);
    }

    private void habilitarBotonesMascota(boolean h) {
        btnModificarMascota.setEnabled(h);
        btnBajaMascota.setEnabled(h);
    }

    private JTextField crearTextField(boolean enabled) {
        JTextField tf = new JTextField();
        tf.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tf.setEnabled(enabled);
        return tf;
    }
}
