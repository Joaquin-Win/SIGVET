package com.sigvet.view;

import com.sigvet.model.Mascota;
import com.sigvet.model.dto.HistorialClinicoDTO;
import com.sigvet.service.HistorialService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo para el caso de uso CU-07: Consultar Historial Clínico.
 *
 * <p>Permite buscar una mascota por nombre y visualizar su historial clínico completo,
 * incluyendo todas las consultas, medicamentos recetados, lotes y fechas de vencimiento.
 * Los datos se leen desde la vista de BD {@code v_historial_clinico} a través del
 * {@link HistorialService}.</p>
 *
 * <p>Esta pantalla es de <strong>solo lectura</strong>. No permite modificaciones
 * (RN-07: no se expone eliminación de consultas médicas).</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see HistorialService
 */
public class HistorialClinicoDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Columnas de la tabla de resultados de búsqueda. */
    private static final String[] COL_BUSQUEDA = {
        "ID", "Nombre", "Especie", "Raza", "Dueño"
    };

    /** Columnas de la tabla de historial clínico completo. */
    private static final String[] COL_HISTORIAL = {
        "ID Consulta", "Fecha", "Veterinario", "Síntomas", "Diagnóstico",
        "Medicamento", "Dosis", "Frecuencia", "Duración", "Lote", "Vto. Lote"
    };

    private static final DateTimeFormatter FMT_DT   =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================================
    // Service
    // =========================================================================

    private final HistorialService historialService;

    // =========================================================================
    // Estado interno
    // =========================================================================

    private final List<Mascota> resultadosBusqueda = new ArrayList<>();

    // =========================================================================
    // Componentes
    // =========================================================================

    private JTextField    txtBuscarMascota;
    private JTable        tablaBusqueda;
    private DefaultTableModel modeloBusqueda;

    private JLabel        lblInfoMascota;

    private JTable        tablaHistorial;
    private DefaultTableModel modeloHistorial;
    private JLabel        lblSinHistorial;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de historial clínico.
     *
     * @param historialService servicio de historial clínico inyectado
     */
    public HistorialClinicoDialog(HistorialService historialService) {
        super("Historial Clínico", true, true, true, true);
        this.historialService = historialService;

        construirUI();
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

        add(construirPanelBusqueda(),  BorderLayout.NORTH);
        add(construirPanelHistorial(), BorderLayout.CENTER);
        add(construirPanelBotones(),   BorderLayout.SOUTH);
    }

    /**
     * Construye el panel superior de búsqueda de mascota con tabla de resultados.
     *
     * @return panel de búsqueda
     */
    private JPanel construirPanelBusqueda() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "🔍 Buscar Mascota",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));
        panel.setPreferredSize(new Dimension(0, 195));
        panel.setBorder(BorderFactory.createCompoundBorder(
            panel.getBorder(),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // Campo de búsqueda con DocumentListener
        JPanel panelCampo = new JPanel(new BorderLayout(6, 0));
        panelCampo.setBackground(Color.WHITE);
        panelCampo.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        txtBuscarMascota = new JTextField();
        txtBuscarMascota.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtBuscarMascota.setToolTipText("Escriba el nombre de la mascota (mínimo 2 caracteres)");
        txtBuscarMascota.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { buscarMascotas(); }
            @Override public void removeUpdate(DocumentEvent e)  { buscarMascotas(); }
            @Override public void changedUpdate(DocumentEvent e) { buscarMascotas(); }
        });
        JLabel lblCampo = new JLabel("Nombre de la mascota:");
        lblCampo.setFont(new Font("SansSerif", Font.BOLD, 12));
        panelCampo.add(lblCampo,         BorderLayout.WEST);
        panelCampo.add(txtBuscarMascota, BorderLayout.CENTER);

        // Tabla de resultados de búsqueda
        modeloBusqueda = new DefaultTableModel(COL_BUSQUEDA, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaBusqueda = new JTable(modeloBusqueda);
        tablaBusqueda.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaBusqueda.setRowHeight(22);
        tablaBusqueda.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaBusqueda.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tablaBusqueda.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Ocultar columna ID
        tablaBusqueda.getColumnModel().getColumn(0).setMinWidth(0);
        tablaBusqueda.getColumnModel().getColumn(0).setMaxWidth(0);
        tablaBusqueda.getColumnModel().getColumn(0).setWidth(0);

        int[] anchos = {0, 140, 120, 120, 180};
        for (int i = 1; i < anchos.length; i++) {
            tablaBusqueda.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        tablaBusqueda.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onMascotaBusquedaSeleccionada();
        });

        JScrollPane scrollBusqueda = new JScrollPane(tablaBusqueda);
        scrollBusqueda.setPreferredSize(new Dimension(0, 120));
        scrollBusqueda.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(panelCampo,     BorderLayout.NORTH);
        panel.add(scrollBusqueda, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Construye el panel central con la información de la mascota seleccionada
     * y la tabla de historial clínico completo.
     *
     * @return panel de historial
     */
    private JPanel construirPanelHistorial() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Label de información de la mascota seleccionada
        lblInfoMascota = new JLabel("Seleccione una mascota para ver su historial clínico");
        lblInfoMascota.setFont(new Font("SansSerif", Font.ITALIC, 13));
        lblInfoMascota.setForeground(Color.DARK_GRAY);
        lblInfoMascota.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 220, 240)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        lblInfoMascota.setBackground(new Color(235, 245, 255));
        lblInfoMascota.setOpaque(true);

        // Tabla de historial clínico
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBackground(Color.WHITE);
        panelTabla.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 210, 240)),
            "Historial Clínico Completo (consultas + medicamentos)",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        modeloHistorial = new DefaultTableModel(COL_HISTORIAL, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaHistorial = new JTable(modeloHistorial);
        tablaHistorial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaHistorial.setRowHeight(22);
        tablaHistorial.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tablaHistorial.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tablaHistorial.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tablaHistorial.setFillsViewportHeight(true);

        // Ancho de columnas del historial
        int[] anchosH = {70, 120, 160, 200, 200, 130, 80, 100, 80, 100, 90};
        for (int i = 0; i < anchosH.length; i++) {
            tablaHistorial.getColumnModel().getColumn(i).setPreferredWidth(anchosH[i]);
        }

        // Renderer: columna "Vto. Lote" en rojo si está vencida
        tablaHistorial.getColumnModel().getColumn(10).setCellRenderer(
            new VencimientoRenderer()
        );

        // Renderer: alternar color de filas por consulta (misma ID Consulta = misma banda)
        tablaHistorial.setDefaultRenderer(Object.class, new AlternarConsultaRenderer());

        JScrollPane scroll = new JScrollPane(tablaHistorial);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        // Label de "sin historial"
        lblSinHistorial = new JLabel("No hay consultas registradas para esta mascota.",
            SwingConstants.CENTER);
        lblSinHistorial.setFont(new Font("SansSerif", Font.ITALIC, 13));
        lblSinHistorial.setForeground(Color.GRAY);
        lblSinHistorial.setVisible(false);

        panelTabla.add(scroll,          BorderLayout.CENTER);
        panelTabla.add(lblSinHistorial, BorderLayout.SOUTH);

        panel.add(lblInfoMascota, BorderLayout.NORTH);
        panel.add(panelTabla,     BorderLayout.CENTER);

        return panel;
    }

    /**
     * Construye el panel de botones inferiores (Imprimir / Cerrar).
     *
     * @return panel de botones
     */
    private JPanel construirPanelBotones() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 244, 252));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        izq.setBackground(new Color(240, 244, 252));

        JButton btnImprimir = new JButton("🖨 Imprimir Historial");
        btnImprimir.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnImprimir.setToolTipText("Imprimir el historial clínico en la impresora predeterminada");
        btnImprimir.addActionListener(e -> {
            if (modeloHistorial.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    "No hay historial cargado para imprimir.\nSeleccione una mascota primero.",
                    "Sin datos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String titulo = lblInfoMascota.getText().replaceAll("[\\ud800-\\udfff]", "").trim();
                boolean imprimio = tablaHistorial.print(
                    JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat("Historial Clínico SIGVET  |  " + titulo),
                    new MessageFormat("Página {0}")
                );
                if (imprimio) {
                    JOptionPane.showMessageDialog(this,
                        "Historial enviado a la impresora correctamente.",
                        "Impresión exitosa", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al imprimir:\n" + ex.getMessage(),
                    "Error de impresión", JOptionPane.ERROR_MESSAGE);
            }
        });
        izq.add(btnImprimir);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
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
    // Lógica de búsqueda y carga de historial
    // =========================================================================

    /**
     * Busca mascotas cuyo nombre coincida con el texto ingresado.
     * Se activa con el {@code DocumentListener} del campo de búsqueda.
     * Requiere al menos 2 caracteres para consultar la BD.
     */
    private void buscarMascotas() {
        String texto = txtBuscarMascota.getText().trim();
        modeloBusqueda.setRowCount(0);
        resultadosBusqueda.clear();
        limpiarHistorial();

        if (texto.length() < 2) return;

        try {
            List<Mascota> mascotas = historialService.buscarMascotas(texto);

            for (Mascota m : mascotas) {
                String esp  = m.getEspecie() != null ? m.getEspecie().getNombre() : "—";
                String raza = m.getRaza()    != null ? m.getRaza().getNombre()    : "—";
                String due  = m.getDueno()   != null
                    ? m.getDueno().getApellido() + ", " + m.getDueno().getNombre() : "—";

                modeloBusqueda.addRow(new Object[]{
                    m.getIdMascota(), m.getNombre(), esp, raza, due
                });
                resultadosBusqueda.add(m);
            }

            if (mascotas.isEmpty()) {
                modeloBusqueda.addRow(new Object[]{
                    0, "(Sin resultados para '" + texto + "')", "", "", ""
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al buscar mascotas:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Responde a la selección de una mascota en la tabla de búsqueda:
     * muestra sus datos y carga el historial clínico completo.
     */
    private void onMascotaBusquedaSeleccionada() {
        int fila = tablaBusqueda.getSelectedRow();
        if (fila < 0 || fila >= resultadosBusqueda.size()) {
            limpiarHistorial();
            return;
        }

        Mascota mascota = resultadosBusqueda.get(fila);
        mostrarInfoMascota(mascota);
        cargarHistorialMascota(mascota.getIdMascota());
    }

    /**
     * Actualiza el label de información de la mascota, incluyendo la edad calculada.
     *
     * @param mascota mascota seleccionada
     */
    private void mostrarInfoMascota(Mascota mascota) {
        String esp  = mascota.getEspecie() != null ? mascota.getEspecie().getNombre() : "—";
        String raza = mascota.getRaza()    != null ? mascota.getRaza().getNombre()    : "—";
        String due  = mascota.getDueno()   != null
            ? mascota.getDueno().getApellido() + ", " + mascota.getDueno().getNombre() : "—";

        // Calcular edad via service
        int edadAnios = 0;
        try {
            edadAnios = historialService.calcularEdadMascota(mascota.getIdMascota());
        } catch (SQLException e) {
            System.err.println("Error al calcular edad: " + e.getMessage());
        }

        lblInfoMascota.setText(
            "🐾 Mascota: " + mascota.getNombre()
            + "  |  Especie: " + esp + " / " + raza
            + "  |  Dueño: " + due
            + "  |  Edad: " + edadAnios + " año(s)"
        );
        lblInfoMascota.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblInfoMascota.setForeground(new Color(20, 60, 150));
    }

    /**
     * Carga el historial clínico completo de una mascota en la tabla principal.
     * Usa el DTO {@link HistorialClinicoDTO} para llenar cada fila.
     *
     * @param idMascota ID de la mascota cuyo historial se mostrará
     */
    private void cargarHistorialMascota(int idMascota) {
        modeloHistorial.setRowCount(0);
        lblSinHistorial.setVisible(false);

        try {
            List<HistorialClinicoDTO> historial =
                historialService.obtenerHistorialCompleto(idMascota);

            if (historial.isEmpty()) {
                lblSinHistorial.setVisible(true);
                return;
            }

            // Estructuras repetitivas: for-each para llenar la tabla con cada registro del DTO
            for (HistorialClinicoDTO dto : historial) {
                String fecha     = dto.getFechaConsulta() != null
                    ? dto.getFechaConsulta().format(FMT_DT) : "—";
                String vtoLote   = dto.getFechaVencimiento() != null
                    ? dto.getFechaVencimiento().format(FMT_DATE) : "—";

                modeloHistorial.addRow(new Object[]{
                    dto.getIdConsulta(),
                    fecha,
                    dto.getNombreVeterinario()  != null ? dto.getNombreVeterinario()  : "—",
                    dto.getSintomas()           != null ? dto.getSintomas()           : "—",
                    dto.getDiagnostico()        != null ? dto.getDiagnostico()        : "—",
                    dto.getMedicamento()        != null ? dto.getMedicamento()        : "(sin medicación)",
                    dto.getDosis()              != null ? dto.getDosis()              : "—",
                    dto.getFrecuencia()         != null ? dto.getFrecuencia()         : "—",
                    dto.getDuracion()           != null ? dto.getDuracion()           : "—",
                    dto.getLote()               != null ? dto.getLote()               : "—",
                    vtoLote
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar el historial clínico:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Limpia la tabla de historial y restaura el mensaje de selección.
     */
    private void limpiarHistorial() {
        modeloHistorial.setRowCount(0);
        lblSinHistorial.setVisible(false);
        lblInfoMascota.setText("Seleccione una mascota para ver su historial clínico");
        lblInfoMascota.setFont(new Font("SansSerif", Font.ITALIC, 13));
        lblInfoMascota.setForeground(Color.DARK_GRAY);
    }

    // =========================================================================
    // Clases internas: Renderers
    // =========================================================================

    /**
     * Renderer para la columna "Vto. Lote" que colorea en rojo las fechas vencidas
     * y en naranja las que vencen en los próximos 30 días.
     */
    static class VencimientoRenderer extends DefaultTableCellRenderer {

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            if (!isSelected && value != null && !value.toString().equals("—")) {
                try {
                    LocalDate fechaVenc = LocalDate.parse(value.toString(), FMT);
                    LocalDate hoy = LocalDate.now();
                    if (fechaVenc.isBefore(hoy)) {
                        c.setBackground(new Color(255, 200, 200));
                        c.setForeground(new Color(180, 0, 0));
                    } else if (fechaVenc.isBefore(hoy.plusDays(30))) {
                        c.setBackground(new Color(255, 235, 180));
                        c.setForeground(new Color(160, 80, 0));
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                } catch (Exception e) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
            }
            return c;
        }
    }

    /**
     * Renderer que alterna el color de fondo de las filas del historial
     * según el ID de consulta (columna 0): agrupando visualmente todos los
     * medicamentos de una misma consulta.
     */
    static class AlternarConsultaRenderer extends DefaultTableCellRenderer {

        private static final Color COLOR_A = Color.WHITE;
        private static final Color COLOR_B = new Color(240, 246, 255);

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                // Usar el ID de consulta (col 0) para agrupar bandas de color
                Object idConsulta = table.getModel().getValueAt(row, 0);
                int id = 0;
                if (idConsulta instanceof Integer) id = (Integer) idConsulta;

                c.setBackground(id % 2 == 0 ? COLOR_A : COLOR_B);
                c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
