package com.sigvet.view;

import com.sigvet.exception.VencimientoInvalidoException;
import com.sigvet.model.Medicamento;
import com.sigvet.model.Stock;
import com.sigvet.model.enums.EstadoRegistro;
import com.sigvet.service.InventarioService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Diálogo para el caso de uso CU-04: Gestionar Inventario y Stock.
 *
 * <p>Implementa un {@link JInternalFrame} con dos pestañas:</p>
 * <ul>
 *   <li><strong>Tab 1 — Catálogo de Medicamentos:</strong> CRUD completo de medicamentos
 *       (alta, modificación, baja lógica) con formulario lateral y tabla principal.</li>
 *   <li><strong>Tab 2 — Stock por Lotes:</strong> ingreso de lotes via SP
 *       {@code sp_ingresar_stock} con renderer personalizado que colorea filas vencidas
 *       (rojo) y con stock bajo (amarillo).</li>
 * </ul>
 *
 * <p><strong>Reglas de negocio aplicadas:</strong></p>
 * <ul>
 *   <li>RN-09: validación de fecha de vencimiento antes de ir a BD.</li>
 *   <li>RN-06: alertas de stock bajo generadas automáticamente por triggers.</li>
 *   <li>Las bajas son siempre lógicas (estado → Inactivo), nunca físicas.</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see InventarioService
 */
public class GestionarInventarioDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Columnas de la tabla de medicamentos. */
    private static final String[] COL_MEDS = {
        "ID", "Nombre Genérico", "Nombre Comercial", "Dosis/Pres.", "Precio", "Stock Mín.", "Estado"
    };

    /** Columnas de la tabla de stock. */
    private static final String[] COL_STOCK = {
        "ID Stock", "Medicamento", "Cantidad", "N° Lote", "F. Vencimiento", "F. Ingreso"
    };

    /** Formato para fechas de vencimiento en el formulario. */
    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================================
    // Service
    // =========================================================================

    private final InventarioService inventarioService;

    // =========================================================================
    // Componentes — Tab 1: Catálogo
    // =========================================================================

    private JTable         tablaMeds;
    private DefaultTableModel modeloMeds;
    private List<Medicamento> medicamentosCargados = new java.util.ArrayList<>();

    private JTextField txtNombreGenerico;
    private JTextField txtNombreComercial;
    private JTextField txtDosisPres;
    private JTextField txtPrecio;
    private JSpinner   spnStockMin;

    private JButton btnNuevo;
    private JButton btnGuardarMed;
    private JButton btnModificar;
    private JButton btnBajaLogica;

    /** Indica si el formulario está en modo "nuevo" (true) o "editar" (false). */
    private boolean modoNuevo = false;

    /** ID del medicamento actualmente seleccionado en la tabla (0 si ninguno). */
    private int idMedSeleccionado = 0;

    // =========================================================================
    // Componentes — Tab 2: Stock
    // =========================================================================

    private JComboBox<Medicamento> cmbMedStock;
    private JTable                 tablaStock;
    private DefaultTableModel      modeloStock;
    private JLabel                 lblStockTotal;

    private JComboBox<Medicamento> cmbMedIngreso;
    private JSpinner               spnCantidad;
    private JTextField             txtNumeroLote;
    private JTextField             txtFechaVencimiento;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de gestión de inventario.
     *
     * @param inventarioService servicio de inventario inyectado desde la ventana principal
     */
    public GestionarInventarioDialog(InventarioService inventarioService) {
        super("Gestionar Inventario y Stock", true, true, true, true);
        this.inventarioService = inventarioService;

        construirUI();
        cargarMedicamentos();
        cargarCombosStock();
    }

    // =========================================================================
    // Construcción principal
    // =========================================================================

    /**
     * Construye la interfaz completa con el {@link JTabbedPane} de dos pestañas.
     */
    private void construirUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 249, 252));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.addTab("💊 Catálogo de Medicamentos", construirTabCatalogo());
        tabs.addTab("📦 Stock por Lotes",           construirTabStock());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                cargarCombosStock();
                int idMed = getIdMedSeleccionadoEnStock();
                if (idMed > 0) cargarStockPorMedicamento(idMed);
            }
        });

        add(tabs, BorderLayout.CENTER);
        add(construirBarraBotones(), BorderLayout.SOUTH);
    }

    /**
     * Construye la barra de botones global del diálogo (solo "Cerrar").
     *
     * @return panel de botones
     */
    private JPanel construirBarraBotones() {
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
    // Tab 1: Catálogo de Medicamentos
    // =========================================================================

    /**
     * Construye la pestaña completa de catálogo de medicamentos.
     *
     * @return panel de la pestaña
     */
    private JPanel construirTabCatalogo() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            construirPanelTablaMeds(),
            construirPanelFormularioMed()
        );
        split.setDividerLocation(560);
        split.setResizeWeight(0.6);
        split.setBorder(null);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel izquierdo con la tabla de medicamentos.
     *
     * @return panel de tabla
     */
    private JPanel construirPanelTablaMeds() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Medicamentos registrados",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        modeloMeds = new DefaultTableModel(COL_MEDS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaMeds = new JTable(modeloMeds);
        tablaMeds.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaMeds.setRowHeight(22);
        tablaMeds.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaMeds.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaMeds.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaMeds.setFillsViewportHeight(true);

        int[] anchos = {40, 160, 150, 110, 80, 80, 80};
        for (int i = 0; i < anchos.length; i++) {
            tablaMeds.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Renderer para colorear filas según estado
        tablaMeds.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String estado = (String) table.getModel().getValueAt(row, 6);
                    if ("Inactivo".equals(estado)) {
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

        // Al seleccionar una fila → llenar el formulario
        tablaMeds.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onMedSeleccionado();
        });

        JScrollPane scroll = new JScrollPane(tablaMeds);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Barra de búsqueda rápida
        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panelBusqueda.setBackground(Color.WHITE);
        JTextField txtBuscar = new JTextField(18);
        txtBuscar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtBuscar.setToolTipText("Buscar por nombre genérico o comercial");
        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.addActionListener(e -> buscarMedicamentos(txtBuscar.getText().trim()));
        JButton btnVerTodos = new JButton("Ver todos");
        btnVerTodos.addActionListener(e -> { txtBuscar.setText(""); cargarMedicamentos(); });

        panelBusqueda.add(new JLabel("Buscar:"));
        panelBusqueda.add(txtBuscar);
        panelBusqueda.add(btnBuscar);
        panelBusqueda.add(btnVerTodos);

        panel.add(panelBusqueda, BorderLayout.NORTH);
        panel.add(scroll,        BorderLayout.CENTER);

        return panel;
    }

    /**
     * Construye el panel derecho con el formulario de alta/modificación de medicamentos.
     *
     * @return panel de formulario
     */
    private JPanel construirPanelFormularioMed() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Datos del Medicamento  (* obligatorio)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        // ── Formulario ────────────────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 6, 5, 6);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        // Nombre genérico
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("Nombre genérico *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtNombreGenerico = new JTextField();
        txtNombreGenerico.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtNombreGenerico.setEnabled(false);
        formPanel.add(txtNombreGenerico, gbc);

        // Nombre comercial
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Nombre comercial *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtNombreComercial = new JTextField();
        txtNombreComercial.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtNombreComercial.setEnabled(false);
        formPanel.add(txtNombreComercial, gbc);

        // Dosis/Presentación
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Dosis/Presentación *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtDosisPres = new JTextField();
        txtDosisPres.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtDosisPres.setEnabled(false);
        formPanel.add(txtDosisPres, gbc);

        // Precio de venta
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Precio de venta *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtPrecio = new JTextField("0.00");
        txtPrecio.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtPrecio.setEnabled(false);
        formPanel.add(txtPrecio, gbc);

        // Stock mínimo de alerta
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("Stock mínimo alerta:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        spnStockMin = new JSpinner(new SpinnerNumberModel(5, 0, 9999, 1));
        spnStockMin.setFont(new Font("SansSerif", Font.PLAIN, 12));
        spnStockMin.setEnabled(false);
        formPanel.add(spnStockMin, gbc);

        // Espaciador
        gbc.gridx = 0; gbc.gridy = 5; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL;
        formPanel.add(Box.createVerticalGlue(), gbc);

        // ── Botones CRUD ──────────────────────────────────────────────────────
        JPanel panelBtnsCrud = new JPanel(new GridLayout(4, 1, 0, 6));
        panelBtnsCrud.setBackground(Color.WHITE);
        panelBtnsCrud.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        btnNuevo       = new JButton("➕ Nuevo");
        btnGuardarMed  = new JButton("💾 Guardar");
        btnModificar   = new JButton("✏ Modificar");
        btnBajaLogica  = new JButton("🗑 Baja Lógica");

        Font fontBtn = new Font("SansSerif", Font.PLAIN, 12);
        btnNuevo.setFont(fontBtn);
        btnGuardarMed.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnModificar.setFont(fontBtn);
        btnBajaLogica.setFont(fontBtn);

        btnGuardarMed.setEnabled(false);
        btnModificar.setEnabled(false);
        btnBajaLogica.setEnabled(false);

        btnNuevo.addActionListener(e -> iniciarModoNuevo());
        btnGuardarMed.addActionListener(e -> guardarMedicamento());
        btnModificar.addActionListener(e -> modificarMedicamento());
        btnBajaLogica.addActionListener(e -> bajaLogicaMedicamento());

        panelBtnsCrud.add(btnNuevo);
        panelBtnsCrud.add(btnGuardarMed);
        panelBtnsCrud.add(btnModificar);
        panelBtnsCrud.add(btnBajaLogica);

        panel.add(formPanel,     BorderLayout.CENTER);
        panel.add(panelBtnsCrud, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Tab 2: Stock por Lotes
    // =========================================================================

    /**
     * Construye la pestaña completa de gestión de stock por lotes.
     *
     * @return panel de la pestaña de stock
     */
    private JPanel construirTabStock() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        panel.add(construirPanelFiltroStock(),   BorderLayout.NORTH);
        panel.add(construirPanelTablaStock(),    BorderLayout.CENTER);
        panel.add(construirPanelIngresarStock(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Construye el panel de filtro por medicamento para la tabla de stock.
     *
     * @return panel de filtro
     */
    private JPanel construirPanelFiltroStock() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel lbl = new JLabel("Medicamento:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));

        cmbMedStock = new JComboBox<>();
        cmbMedStock.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbMedStock.setPreferredSize(new Dimension(250, 28));
        cmbMedStock.addActionListener(e -> {
            int idMed = getIdMedSeleccionadoEnStock();
            if (idMed > 0) cargarStockPorMedicamento(idMed);
        });

        lblStockTotal = new JLabel("Stock total: —");
        lblStockTotal.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblStockTotal.setForeground(new Color(0, 100, 180));

        JButton btnRefrescar = new JButton("🔄");
        btnRefrescar.setToolTipText("Refrescar");
        btnRefrescar.addActionListener(e -> {
            int id = getIdMedSeleccionadoEnStock();
            if (id > 0) cargarStockPorMedicamento(id);
        });

        panel.add(lbl);
        panel.add(cmbMedStock);
        panel.add(btnRefrescar);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lblStockTotal);

        return panel;
    }

    /**
     * Construye el panel central con la tabla de lotes de stock y su renderer personalizado.
     *
     * @return panel de tabla de stock
     */
    private JPanel construirPanelTablaStock() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Lotes de Stock (🔴 Vencido  🟡 Stock bajo  🟢 OK)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        modeloStock = new DefaultTableModel(COL_STOCK, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaStock = new JTable(modeloStock);
        tablaStock.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaStock.setRowHeight(22);
        tablaStock.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaStock.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaStock.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaStock.setFillsViewportHeight(true);

        int[] anchos = {60, 180, 70, 120, 120, 110};
        for (int i = 0; i < anchos.length; i++) {
            tablaStock.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // Aplicar renderer personalizado con colores por vencimiento/stock
        StockCellRenderer renderer = new StockCellRenderer();
        for (int i = 0; i < tablaStock.getColumnCount(); i++) {
            tablaStock.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane scroll = new JScrollPane(tablaStock);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel inferior de ingreso de nuevos lotes de stock.
     *
     * @return panel de ingreso de stock
     */
    private JPanel construirPanelIngresarStock() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Ingresar Nuevo Lote de Stock",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));
        panel.setPreferredSize(new Dimension(0, 145));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(4, 8, 4, 8);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        // Fila 0: Medicamento + Cantidad
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Medicamento *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbMedIngreso = new JComboBox<>();
        cmbMedIngreso.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(cmbMedIngreso, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Cantidad *:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        spnCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
        spnCantidad.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(spnCantidad, gbc);

        // Fila 1: Número de lote + Fecha vencimiento
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Número de lote *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtNumeroLote = new JTextField();
        txtNumeroLote.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtNumeroLote.setToolTipText("Ej.: LOT-2025-001");
        panel.add(txtNumeroLote, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("F. Vencimiento * (dd/MM/yyyy):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        txtFechaVencimiento = new JTextField(LocalDate.now().plusMonths(12).format(FMT_FECHA));
        txtFechaVencimiento.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtFechaVencimiento.setToolTipText("Formato: dd/MM/yyyy. Debe ser posterior a hoy (RN-09).");
        panel.add(txtFechaVencimiento, gbc);

        // Fila 2: Botón ingresar
        gbc.gridx = 3; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        JButton btnIngresar = new JButton("📥 Ingresar Stock");
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnIngresar.setBackground(new Color(0, 140, 70));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setPreferredSize(new Dimension(170, 32));
        btnIngresar.addActionListener(e -> ingresarStock());
        panel.add(btnIngresar, gbc);

        return panel;
    }

    // =========================================================================
    // Carga de datos
    // =========================================================================

    /**
     * Carga todos los medicamentos activos en la tabla del catálogo.
     */
    private void cargarMedicamentos() {
        modeloMeds.setRowCount(0);
        medicamentosCargados.clear();
        idMedSeleccionado = 0;
        limpiarFormularioMed();
        habilitarBotonesCrud(false);

        try {
            List<Medicamento> lista = inventarioService.obtenerMedicamentos();

            // Estructuras repetitivas: for-each para llenar la tabla
            for (Medicamento m : lista) {
                modeloMeds.addRow(new Object[]{
                    m.getIdMedicamento(),
                    m.getNombreGenerico(),
                    m.getNombreComercial(),
                    m.getDosisPresentacion(),
                    String.format("$%.2f", m.getPrecioVenta()),
                    m.getStockMinimoAlerta(),
                    m.getEstado() != null ? m.getEstado().name() : "—"
                });
                medicamentosCargados.add(m);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar medicamentos:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Busca medicamentos por nombre y recarga la tabla con los resultados.
     *
     * @param texto texto de búsqueda
     */
    private void buscarMedicamentos(String texto) {
        if (texto.isEmpty()) { cargarMedicamentos(); return; }

        modeloMeds.setRowCount(0);
        medicamentosCargados.clear();

        try {
            List<Medicamento> lista = inventarioService.buscarMedicamentosPorNombre(texto);
            for (Medicamento m : lista) {
                modeloMeds.addRow(new Object[]{
                    m.getIdMedicamento(),
                    m.getNombreGenerico(),
                    m.getNombreComercial(),
                    m.getDosisPresentacion(),
                    String.format("$%.2f", m.getPrecioVenta()),
                    m.getStockMinimoAlerta(),
                    m.getEstado() != null ? m.getEstado().name() : "—"
                });
                medicamentosCargados.add(m);
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error en la búsqueda:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga los combos de medicamentos en la pestaña de stock.
     */
    private void cargarCombosStock() {
        try {
            List<Medicamento> lista = inventarioService.obtenerMedicamentos();

            cmbMedStock.removeAllItems();
            cmbMedIngreso.removeAllItems();

            for (Medicamento m : lista) {
                cmbMedStock.addItem(m);
                cmbMedIngreso.addItem(m);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar combos de stock: " + e.getMessage());
        }
    }

    /**
     * Carga los lotes de stock de un medicamento específico en la tabla.
     *
     * @param idMedicamento ID del medicamento a consultar
     */
    private void cargarStockPorMedicamento(int idMedicamento) {
        modeloStock.setRowCount(0);

        try {
            List<Stock> lotes = inventarioService.obtenerStockPorMedicamento(idMedicamento);

            for (Stock s : lotes) {
                String fVenc  = s.getFechaVencimiento() != null
                    ? s.getFechaVencimiento().format(FMT_FECHA) : "—";
                String fIng   = s.getFechaIngreso() != null
                    ? s.getFechaIngreso().format(FMT_FECHA) : "—";
                String nomMed = s.getMedicamento() != null
                    ? s.getMedicamento().getNombreComercial() : "—";

                modeloStock.addRow(new Object[]{
                    s.getIdStock(),
                    nomMed,
                    s.getCantidad(),
                    s.getNumeroLote(),
                    fVenc,
                    fIng
                });
            }

            // Actualizar stock total
            int total = inventarioService.obtenerStockTotal(idMedicamento);
            lblStockTotal.setText("Stock total disponible: " + total + " unidades");
            lblStockTotal.setForeground(total > 0 ? new Color(0, 130, 60) : new Color(200, 0, 0));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar el stock:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Lógica de catálogo — CRUD de medicamentos
    // =========================================================================

    /**
     * Responde a la selección de un medicamento en la tabla:
     * llena el formulario y habilita los botones de acción.
     */
    private void onMedSeleccionado() {
        int fila = tablaMeds.getSelectedRow();
        if (fila < 0 || fila >= medicamentosCargados.size()) {
            idMedSeleccionado = 0;
            limpiarFormularioMed();
            habilitarBotonesCrud(false);
            return;
        }

        Medicamento m = medicamentosCargados.get(fila);
        idMedSeleccionado = m.getIdMedicamento();
        modoNuevo = false;

        // Llenar formulario con los datos del medicamento seleccionado
        txtNombreGenerico.setText(m.getNombreGenerico());
        txtNombreComercial.setText(m.getNombreComercial());
        txtDosisPres.setText(m.getDosisPresentacion());
        txtPrecio.setText(String.format("%.2f", m.getPrecioVenta()));
        spnStockMin.setValue(m.getStockMinimoAlerta());

        // Habilitar campos para edición
        habilitarCamposFormulario(true);
        habilitarBotonesCrud(true);
        btnGuardarMed.setEnabled(false); // Solo habilitar tras clic en "Modificar"
    }

    /**
     * Activa el modo de alta de nuevo medicamento: limpia y habilita el formulario.
     */
    private void iniciarModoNuevo() {
        modoNuevo = true;
        idMedSeleccionado = 0;
        tablaMeds.clearSelection();
        limpiarFormularioMed();
        habilitarCamposFormulario(true);
        btnGuardarMed.setEnabled(true);
        btnModificar.setEnabled(false);
        btnBajaLogica.setEnabled(false);
        txtNombreGenerico.requestFocus();
    }

    /**
     * Valida el formulario y da de alta un nuevo medicamento.
     */
    private void guardarMedicamento() {
        if (!modoNuevo) return;

        try {
            Medicamento m = construirMedicamentoDesdeFormulario();
            inventarioService.altaMedicamento(m);

            JOptionPane.showMessageDialog(this,
                "Medicamento '" + m.getNombreComercial() + "' registrado correctamente.",
                "Alta exitosa", JOptionPane.INFORMATION_MESSAGE);

            cargarMedicamentos();
            cargarCombosStock();
            modoNuevo = false;
            btnGuardarMed.setEnabled(false);

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al guardar el medicamento:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Valida el formulario y aplica las modificaciones al medicamento seleccionado.
     */
    private void modificarMedicamento() {
        if (idMedSeleccionado <= 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un medicamento de la tabla para modificar.",
                "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int op = JOptionPane.showConfirmDialog(this,
            "¿Confirma la modificación del medicamento?",
            "Confirmar modificación", JOptionPane.YES_NO_OPTION);
        if (op != JOptionPane.YES_OPTION) return;

        try {
            Medicamento m = construirMedicamentoDesdeFormulario();
            m.setIdMedicamento(idMedSeleccionado);
            inventarioService.modificarMedicamento(m);

            JOptionPane.showMessageDialog(this,
                "Medicamento modificado correctamente.",
                "Modificación exitosa", JOptionPane.INFORMATION_MESSAGE);

            cargarMedicamentos();
            cargarCombosStock();

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al modificar el medicamento:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aplica baja lógica al medicamento seleccionado (estado → Inactivo).
     */
    private void bajaLogicaMedicamento() {
        if (idMedSeleccionado <= 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un medicamento de la tabla.",
                "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nombre = txtNombreComercial.getText().trim();
        int op = JOptionPane.showConfirmDialog(this,
            "¿Aplicar baja lógica al medicamento '" + nombre + "'?\n"
            + "El medicamento quedará marcado como Inactivo.",
            "Confirmar baja lógica", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        try {
            inventarioService.bajaLogicaMedicamento(idMedSeleccionado);

            JOptionPane.showMessageDialog(this,
                "Medicamento '" + nombre + "' dado de baja lógica correctamente.",
                "Baja exitosa", JOptionPane.INFORMATION_MESSAGE);

            cargarMedicamentos();
            cargarCombosStock();

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al aplicar la baja:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Construye un objeto {@link Medicamento} desde los valores actuales del formulario.
     *
     * @return medicamento construido desde el formulario
     * @throws IllegalArgumentException si el precio no es un número válido
     */
    private Medicamento construirMedicamentoDesdeFormulario() {
        Medicamento m = new Medicamento();
        m.setNombreGenerico(txtNombreGenerico.getText().trim());
        m.setNombreComercial(txtNombreComercial.getText().trim());
        m.setDosisPresentacion(txtDosisPres.getText().trim());
        m.setEstado(EstadoRegistro.Activo);

        try {
            double precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
            m.setPrecioVenta(precio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "El precio de venta no es un número válido: '" + txtPrecio.getText() + "'.");
        }

        m.setStockMinimoAlerta((Integer) spnStockMin.getValue());
        return m;
    }

    // =========================================================================
    // Lógica de stock — Ingreso de lotes
    // =========================================================================

    /**
     * Valida los campos y llama a {@link InventarioService#ingresarStock} para
     * registrar un nuevo lote via SP {@code sp_ingresar_stock}.
     *
     * <p>El SP activa los triggers de alerta automática (RN-06/09).</p>
     */
    private void ingresarStock() {
        Medicamento med = (Medicamento) cmbMedIngreso.getSelectedItem();
        if (med == null) {
            JOptionPane.showMessageDialog(this,
                "Seleccione un medicamento.", "Campo requerido",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidad = (Integer) spnCantidad.getValue();
        String lote  = txtNumeroLote.getText().trim();
        String fechaStr = txtFechaVencimiento.getText().trim();

        if (lote.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El número de lote es obligatorio.", "Campo requerido",
                JOptionPane.WARNING_MESSAGE);
            txtNumeroLote.requestFocus();
            return;
        }

        // Parsear y validar la fecha de vencimiento
        LocalDate fechaVenc;
        try {
            fechaVenc = LocalDate.parse(fechaStr, FMT_FECHA);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                "El formato de fecha de vencimiento es inválido.\n"
                + "Use el formato dd/MM/yyyy (ej.: 31/12/2026).",
                "Formato inválido", JOptionPane.WARNING_MESSAGE);
            txtFechaVencimiento.requestFocus();
            return;
        }

        try {
            int idStockNuevo = inventarioService.ingresarStock(
                med.getIdMedicamento(), cantidad, lote, fechaVenc
            );

            int stockTotal = inventarioService.obtenerStockTotal(med.getIdMedicamento());

            JOptionPane.showMessageDialog(this,
                "✅ Stock ingresado correctamente.\n"
                + "Medicamento: " + med.getNombreComercial() + "\n"
                + "Lote: " + lote + " | Cantidad: " + cantidad + "\n"
                + "Vencimiento: " + fechaStr + "\n"
                + "ID de stock generado: " + idStockNuevo + "\n\n"
                + "Stock total disponible: " + stockTotal + " unidades.",
                "Ingreso exitoso", JOptionPane.INFORMATION_MESSAGE);

            // Limpiar campos de ingreso y refrescar tabla
            txtNumeroLote.setText("");
            txtFechaVencimiento.setText(
                LocalDate.now().plusMonths(12).format(FMT_FECHA));
            spnCantidad.setValue(1);

            // Sincronizar el combo de filtro con el medicamento recién ingresado
            for (int i = 0; i < cmbMedStock.getItemCount(); i++) {
                Medicamento item = cmbMedStock.getItemAt(i);
                if (item != null && item.getIdMedicamento() == med.getIdMedicamento()) {
                    cmbMedStock.setSelectedIndex(i);
                    break;
                }
            }
            cargarStockPorMedicamento(med.getIdMedicamento());

        } catch (VencimientoInvalidoException e) {
            JOptionPane.showMessageDialog(this,
                "⚠ Fecha de vencimiento inválida (RN-09):\n" + e.getMessage()
                + "\n\nLa fecha de vencimiento debe ser posterior a hoy.",
                "Vencimiento inválido", JOptionPane.ERROR_MESSAGE);

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al ingresar el stock:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Auxiliares de UI
    // =========================================================================

    /**
     * Obtiene el ID del medicamento seleccionado en el combo de la pestaña de stock.
     *
     * @return ID del medicamento, o 0 si no hay selección
     */
    private int getIdMedSeleccionadoEnStock() {
        Object sel = cmbMedStock.getSelectedItem();
        return (sel instanceof Medicamento) ? ((Medicamento) sel).getIdMedicamento() : 0;
    }

    /**
     * Limpia todos los campos del formulario de medicamentos.
     */
    private void limpiarFormularioMed() {
        txtNombreGenerico.setText("");
        txtNombreComercial.setText("");
        txtDosisPres.setText("");
        txtPrecio.setText("0.00");
        spnStockMin.setValue(5);
        habilitarCamposFormulario(false);
    }

    /**
     * Habilita o deshabilita los campos del formulario de medicamentos.
     *
     * @param habilitar {@code true} para habilitar, {@code false} para deshabilitar
     */
    private void habilitarCamposFormulario(boolean habilitar) {
        txtNombreGenerico.setEnabled(habilitar);
        txtNombreComercial.setEnabled(habilitar);
        txtDosisPres.setEnabled(habilitar);
        txtPrecio.setEnabled(habilitar);
        spnStockMin.setEnabled(habilitar);
    }

    /**
     * Habilita o deshabilita los botones de CRUD según si hay selección activa.
     *
     * @param habilitar {@code true} para habilitar
     */
    private void habilitarBotonesCrud(boolean habilitar) {
        btnModificar.setEnabled(habilitar);
        btnBajaLogica.setEnabled(habilitar);
    }

    // =========================================================================
    // Clase interna: StockCellRenderer
    // =========================================================================

    /**
     * Renderer personalizado para la tabla de stock que colorea las filas según
     * el estado del lote:
     * <ul>
     *   <li>🔴 Rojo claro: lote vencido (fecha de vencimiento anterior a hoy).</li>
     *   <li>🟡 Amarillo claro: lote próximo a vencer (dentro de los próximos 30 días)
     *       o con cantidad = 0.</li>
     *   <li>🟢 Sin color (blanco/alternado): lote en condiciones normales.</li>
     * </ul>
     */
    static class StockCellRenderer extends DefaultTableCellRenderer {

        /** Color de fondo para lotes vencidos. */
        private static final Color COLOR_VENCIDO   = new Color(255, 210, 210);

        /** Color de fondo para lotes próximos a vencer o con stock 0. */
        private static final Color COLOR_PROXIMO   = new Color(255, 248, 180);

        /** Color de fila impar para filas normales. */
        private static final Color COLOR_IMPAR     = new Color(245, 248, 255);

        /** Formato de fecha para parsear la columna de vencimiento. */
        private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

        /**
         * {@inheritDoc}
         *
         * <p>Aplica el color de fondo según el estado del lote evaluando
         * la columna 4 (fecha de vencimiento) y la columna 2 (cantidad).</p>
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                // Leer fecha de vencimiento (columna 4) y cantidad (columna 2)
                Object fVencObj  = table.getModel().getValueAt(row, 4);
                Object cantidadObj = table.getModel().getValueAt(row, 2);

                LocalDate fechaVenc = null;
                if (fVencObj != null && !fVencObj.toString().equals("—")) {
                    try { fechaVenc = LocalDate.parse(fVencObj.toString(), FMT); }
                    catch (DateTimeParseException e) { /* ignorar parse error */ }
                }

                int cantidad = 0;
                if (cantidadObj instanceof Integer) {
                    cantidad = (Integer) cantidadObj;
                } else if (cantidadObj != null) {
                    try { cantidad = Integer.parseInt(cantidadObj.toString()); }
                    catch (NumberFormatException e) { /* ignorar */ }
                }

                // Aplicar color según condición (estructuras condicionales)
                if (fechaVenc != null && fechaVenc.isBefore(LocalDate.now())) {
                    // Lote vencido → fondo rojo claro
                    c.setBackground(COLOR_VENCIDO);
                } else if (cantidad == 0
                        || (fechaVenc != null
                            && fechaVenc.isBefore(LocalDate.now().plusDays(30)))) {
                    // Stock 0 o vence en menos de 30 días → fondo amarillo
                    c.setBackground(COLOR_PROXIMO);
                } else {
                    // Normal → alternado blanco/azul claro
                    c.setBackground(row % 2 == 0 ? Color.WHITE : COLOR_IMPAR);
                }
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
