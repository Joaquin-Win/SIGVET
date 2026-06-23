package com.sigvet.view;

import com.sigvet.model.AlertaStock;
import com.sigvet.model.enums.EstadoAlerta;
import com.sigvet.model.enums.TipoAlerta;
import com.sigvet.service.AlertaService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para el caso de uso CU-05: Gestionar Alertas de Stock.
 *
 * <p>Las alertas son generadas <em>automáticamente</em> por los triggers de BD
 * ({@code trg_alerta_stock_bajo_insert}, {@code trg_alerta_vencimiento_insert}, etc.)
 * al insertar o actualizar registros en {@code stock_medicamento}. Esta pantalla solo
 * consulta y actualiza su estado.</p>
 *
 * <p>Implementa filtrado client-side mediante {@link TableRowSorter} y {@link RowFilter}
 * sin volver a la BD, y un {@link DefaultTableCellRenderer} personalizado para
 * colorear filas según tipo y estado.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see AlertaService
 */
public class AlertasStockDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Columnas de la tabla de alertas. */
    private static final String[] COLUMNAS = {
        "ID", "Tipo", "Medicamento", "Mensaje", "Estado", "Fecha Generación", "Fecha Resolución"
    };

    /** Formato para fechas/horas de las alertas. */
    private static final DateTimeFormatter FMT_DT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================================
    // Service
    // =========================================================================

    private final AlertaService alertaService;

    // =========================================================================
    // Estado interno
    // =========================================================================

    /** Todas las alertas cargadas (para filtrado client-side). */
    private final List<AlertaStock> alertasCargadas = new ArrayList<>();

    // =========================================================================
    // Componentes de UI
    // =========================================================================

    private JComboBox<String>  cmbFiltroTipo;
    private JComboBox<String>  cmbFiltroEstado;
    private JTable             tablaAlertas;
    private DefaultTableModel  modeloAlertas;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel             lblContador;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de alertas de stock.
     *
     * @param alertaService servicio de alertas inyectado desde la ventana principal
     */
    public AlertasStockDialog(AlertaService alertaService) {
        super("Alertas de Stock", true, true, true, true);
        this.alertaService = alertaService;

        construirUI();
        cargarAlertas();
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

        add(construirPanelFiltros(), BorderLayout.NORTH);
        add(construirPanelTabla(),   BorderLayout.CENTER);
        add(construirPanelAcciones(), BorderLayout.SOUTH);
    }

    /**
     * Construye el panel de filtros por tipo y estado.
     *
     * @return panel de filtros
     */
    private JPanel construirPanelFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        panel.setBackground(new Color(235, 240, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));

        JLabel lblTipo = new JLabel("Tipo:");
        lblTipo.setFont(new Font("SansSerif", Font.BOLD, 12));
        cmbFiltroTipo = new JComboBox<>(new String[]{
            "Todas", "Stock Bajo", "Vencimiento Próximo"
        });
        cmbFiltroTipo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbFiltroTipo.addActionListener(e -> aplicarFiltros());

        JLabel lblEstado = new JLabel("Estado:");
        lblEstado.setFont(new Font("SansSerif", Font.BOLD, 12));
        cmbFiltroEstado = new JComboBox<>(new String[]{
            "Todas", "Pendiente", "En Gestión", "Resuelta"
        });
        cmbFiltroEstado.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbFiltroEstado.addActionListener(e -> aplicarFiltros());

        JButton btnActualizar = new JButton("🔄 Actualizar");
        btnActualizar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnActualizar.addActionListener(e -> cargarAlertas());

        lblContador = new JLabel("Total alertas pendientes: —");
        lblContador.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblContador.setForeground(new Color(180, 0, 0));

        panel.add(lblTipo);
        panel.add(cmbFiltroTipo);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(lblEstado);
        panel.add(cmbFiltroEstado);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(btnActualizar);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lblContador);

        return panel;
    }

    /**
     * Construye el panel central con la tabla de alertas y su renderer personalizado.
     *
     * @return panel de la tabla
     */
    private JPanel construirPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 240)),
            "Alertas de Stock  (🔴 Stock Bajo  🟠 Vencimiento  🟡 En Gestión  🟢 Resuelta)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));
        panel.setBorder(BorderFactory.createCompoundBorder(
            panel.getBorder(),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        modeloAlertas = new DefaultTableModel(COLUMNAS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tablaAlertas = new JTable(modeloAlertas);
        tablaAlertas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaAlertas.setRowHeight(22);
        tablaAlertas.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaAlertas.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaAlertas.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaAlertas.setFillsViewportHeight(true);

        // Ocultar columna ID (col 0)
        tablaAlertas.getColumnModel().getColumn(0).setMinWidth(0);
        tablaAlertas.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaAlertas.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 130, 170, 280, 90, 140, 140};
        for (int i = 1; i < anchos.length; i++) {
            tablaAlertas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        // CellRenderer personalizado
        AlertaCellRenderer renderer = new AlertaCellRenderer();
        for (int i = 0; i < tablaAlertas.getColumnCount(); i++) {
            tablaAlertas.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // TableRowSorter para filtrado client-side
        sorter = new TableRowSorter<>(modeloAlertas);
        tablaAlertas.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(tablaAlertas);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel inferior de acciones sobre alertas seleccionadas.
     *
     * @return panel de acciones
     */
    private JPanel construirPanelAcciones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 244, 252));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        izq.setBackground(new Color(240, 244, 252));

        JButton btnEnGestion = new JButton("⏳ Marcar En Gestión");
        btnEnGestion.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnEnGestion.setBackground(new Color(255, 200, 50));
        btnEnGestion.setFocusPainted(false);
        btnEnGestion.addActionListener(e -> cambiarEstadoAlertaSeleccionada(EstadoAlerta.En_Gestion));

        JButton btnResuelta = new JButton("✅ Marcar Resuelta");
        btnResuelta.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnResuelta.setBackground(new Color(50, 180, 100));
        btnResuelta.setForeground(Color.WHITE);
        btnResuelta.setFocusPainted(false);
        btnResuelta.addActionListener(e -> resolverAlertaSeleccionada());

        izq.add(btnEnGestion);
        izq.add(btnResuelta);

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
     * Carga todas las alertas activas desde la BD y las muestra en la tabla.
     * Actualiza el contador de alertas pendientes.
     */
    public void cargarAlertas() {
        modeloAlertas.setRowCount(0);
        alertasCargadas.clear();

        try {
            List<AlertaStock> alertas = alertaService.obtenerAlertasActivas();
            alertasCargadas.addAll(alertas);

            // Estructuras repetitivas: for-each para llenar la tabla
            for (AlertaStock a : alertas) {
                String tipo     = a.getTipo() == TipoAlerta.STOCK_BAJO
                    ? "⚠ Stock Bajo" : "⏰ Vencimiento Próximo";
                String medNom   = a.getMedicamento() != null
                    ? a.getMedicamento().getNombreComercial() : "—";
                String estado   = a.getEstado() != null ? a.getEstado().name() : "—";
                String fGen     = a.getFechaGeneracion() != null
                    ? a.getFechaGeneracion().format(FMT_DT) : "—";
                String fResol   = a.getFechaResolucion() != null
                    ? a.getFechaResolucion().format(FMT_DT) : "—";

                modeloAlertas.addRow(new Object[]{
                    a.getIdAlerta(), tipo, medNom, a.getMensaje(), estado, fGen, fResol
                });
            }

            // Actualizar contador
            int pendientes = alertaService.contarAlertasPendientes();
            lblContador.setText("Total alertas pendientes: " + pendientes);
            lblContador.setForeground(pendientes > 0 ? new Color(180, 0, 0) : new Color(0, 140, 60));

            // Re-aplicar filtros activos
            aplicarFiltros();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar alertas:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aplica los filtros de tipo y estado seleccionados sin consultar la BD.
     * Usa {@link RowFilter} del {@link TableRowSorter} para filtrar client-side.
     */
    private void aplicarFiltros() {
        String filtroTipo   = (String) cmbFiltroTipo.getSelectedItem();
        String filtroEstado = (String) cmbFiltroEstado.getSelectedItem();

        // Construir lista de filtros a combinar con AND
        List<RowFilter<DefaultTableModel, Object>> filtros = new ArrayList<>();

        // Filtro por tipo (columna 1)
        if (filtroTipo != null && !filtroTipo.equals("Todas")) {
            String textoBuscar = filtroTipo.equals("Stock Bajo") ? "Stock Bajo" : "Vencimiento";
            filtros.add(RowFilter.regexFilter("(?i)" + textoBuscar, 1));
        }

        // Filtro por estado (columna 4)
        if (filtroEstado != null && !filtroEstado.equals("Todas")) {
            String estadoBuscar;
            switch (filtroEstado) {
                case "En Gestión" -> estadoBuscar = "En_Gestion";
                case "Pendiente"  -> estadoBuscar = "Pendiente";
                case "Resuelta"   -> estadoBuscar = "Resuelta";
                default           -> estadoBuscar = filtroEstado;
            }
            filtros.add(RowFilter.regexFilter("(?i)" + estadoBuscar, 4));
        }

        // Aplicar: sin filtros → mostrar todo; con filtros → AND
        if (filtros.isEmpty()) {
            sorter.setRowFilter(null);
        } else if (filtros.size() == 1) {
            sorter.setRowFilter(filtros.get(0));
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filtros));
        }
    }

    // =========================================================================
    // Lógica de negocio
    // =========================================================================

    /**
     * Cambia el estado de la alerta seleccionada en la tabla al estado indicado.
     *
     * @param nuevoEstado nuevo estado a aplicar ({@code En_Gestion})
     */
    private void cambiarEstadoAlertaSeleccionada(EstadoAlerta nuevoEstado) {
        int filaVista = tablaAlertas.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una alerta de la tabla.",
                "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convertir fila vista → fila modelo (el sorter puede cambiar el índice)
        int filaModelo = tablaAlertas.convertRowIndexToModel(filaVista);
        int idAlerta   = (Integer) modeloAlertas.getValueAt(filaModelo, 0);

        // Validar transición de estado
        String estadoActual = (String) modeloAlertas.getValueAt(filaModelo, 4);
        if ("Resuelta".equals(estadoActual)) {
            JOptionPane.showMessageDialog(this,
                "Esta alerta ya está resuelta y no puede modificarse.",
                "Transición no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int op = JOptionPane.showConfirmDialog(this,
            "¿Marcar la alerta #" + idAlerta + " como 'En Gestión'?",
            "Confirmar cambio de estado", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        try {
            alertaService.cambiarEstadoAlerta(idAlerta, nuevoEstado);
            cargarAlertas();
            JOptionPane.showMessageDialog(this,
                "Estado de la alerta actualizado a '" + nuevoEstado.name() + "'.",
                "Estado actualizado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cambiar el estado:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Marca la alerta seleccionada como "Resuelta" via {@link AlertaService#resolverAlerta}.
     * Valida que la alerta esté previamente "En Gestión" para garantizar el flujo correcto.
     */
    private void resolverAlertaSeleccionada() {
        int filaVista = tablaAlertas.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una alerta de la tabla.",
                "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int filaModelo  = tablaAlertas.convertRowIndexToModel(filaVista);
        int idAlerta    = (Integer) modeloAlertas.getValueAt(filaModelo, 0);
        String estadoActual = (String) modeloAlertas.getValueAt(filaModelo, 4);

        if ("Resuelta".equals(estadoActual)) {
            JOptionPane.showMessageDialog(this,
                "Esta alerta ya está resuelta.",
                "Sin cambios necesarios", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Advertir si salta de Pendiente a Resuelta directamente
        String msgConfirm = "Pendiente".equals(estadoActual)
            ? "La alerta está en estado 'Pendiente'. ¿Marcarla como 'Resuelta' directamente?\n"
              + "(Se saltará el estado 'En Gestión')"
            : "¿Marcar la alerta #" + idAlerta + " como 'Resuelta'?";

        int op = JOptionPane.showConfirmDialog(this,
            msgConfirm,
            "Confirmar resolución", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        try {
            alertaService.resolverAlerta(idAlerta);
            cargarAlertas();
            JOptionPane.showMessageDialog(this,
                "Alerta #" + idAlerta + " marcada como 'Resuelta' correctamente.",
                "Alerta resuelta", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al resolver la alerta:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Clase interna: AlertaCellRenderer
    // =========================================================================

    /**
     * Renderer personalizado que colorea las celdas de la tabla de alertas
     * según tipo y estado:
     * <ul>
     *   <li>Estado {@code Pendiente} + tipo Stock Bajo → fondo rojo claro</li>
     *   <li>Estado {@code Pendiente} + tipo Vencimiento → fondo naranja claro</li>
     *   <li>Estado {@code En_Gestion} → fondo amarillo claro</li>
     *   <li>Estado {@code Resuelta} → fondo verde claro (texto gris)</li>
     * </ul>
     */
    static class AlertaCellRenderer extends DefaultTableCellRenderer {

        private static final Color ROJO_CLARO    = new Color(255, 210, 210);
        private static final Color NARANJA_CLARO = new Color(255, 230, 180);
        private static final Color AMARILLO      = new Color(255, 248, 180);
        private static final Color VERDE_CLARO   = new Color(210, 245, 215);

        /**
         * {@inheritDoc}
         *
         * <p>Lee el tipo (columna 1) y el estado (columna 4) del modelo para
         * determinar el color de fondo de la fila.</p>
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                // Leer tipo y estado desde el modelo (no desde la vista)
                int modelRow = table.convertRowIndexToModel(row);
                Object tipoObj   = table.getModel().getValueAt(modelRow, 1);
                Object estadoObj = table.getModel().getValueAt(modelRow, 4);

                String tipo   = tipoObj   != null ? tipoObj.toString()   : "";
                String estado = estadoObj != null ? estadoObj.toString()  : "";

                // Aplicar color según estado (tiene precedencia) y tipo
                if ("Resuelta".equals(estado)) {
                    c.setBackground(VERDE_CLARO);
                    c.setForeground(Color.DARK_GRAY);
                } else if ("En_Gestion".equals(estado)) {
                    c.setBackground(AMARILLO);
                    c.setForeground(Color.BLACK);
                } else if (tipo.contains("Stock")) {
                    // Pendiente + Stock Bajo → rojo
                    c.setBackground(ROJO_CLARO);
                    c.setForeground(Color.BLACK);
                } else {
                    // Pendiente + Vencimiento → naranja
                    c.setBackground(NARANJA_CLARO);
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }
}
