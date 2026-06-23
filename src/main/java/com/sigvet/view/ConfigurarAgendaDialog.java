package com.sigvet.view;

import com.sigvet.exception.FranjaSuperpuestaException;
import com.sigvet.model.AgendaDisponibilidad;
import com.sigvet.model.Veterinario;
import com.sigvet.service.AgendaService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Diálogo para el caso de uso CU-01: Configurar Agenda Veterinaria.
 *
 * <p>Permite al operador:</p>
 * <ol>
 *   <li>Seleccionar un veterinario activo.</li>
 *   <li>Agregar franjas horarias semanales de disponibilidad.</li>
 *   <li>Ver y eliminar franjas existentes del veterinario seleccionado.</li>
 *   <li>Generar slots de 30 minutos para los próximos 14 días.</li>
 * </ol>
 *
 * <p><strong>Manejo de errores:</strong></p>
 * <ul>
 *   <li>{@link FranjaSuperpuestaException} → {@link JOptionPane#ERROR_MESSAGE}</li>
 *   <li>{@link IllegalArgumentException} → {@link JOptionPane#WARNING_MESSAGE}</li>
 *   <li>{@link SQLException} → {@link JOptionPane#ERROR_MESSAGE}</li>
 * </ul>
 *
 * @author SIGVET
 * @version 1.0
 * @see AgendaService
 */
public class ConfigurarAgendaDialog extends JInternalFrame {

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Días de la semana disponibles para configurar franjas. */
    private static final String[] DIAS_SEMANA = {
        "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"
    };

    /** Columnas de la tabla de franjas horarias. */
    private static final String[] COLUMNAS_FRANJAS = {"Día", "Hora Inicio", "Hora Fin"};

    // =========================================================================
    // Service
    // =========================================================================

    private final AgendaService agendaService;

    // =========================================================================
    // Componentes de UI
    // =========================================================================

    /** ComboBox con los veterinarios activos. */
    private JComboBox<Veterinario> cmbVeterinario;

    /** ComboBox con los días de la semana. */
    private JComboBox<String> cmbDia;

    /** Campo de texto para la hora de inicio (formato HH:mm). */
    private JTextField txtHoraInicio;

    /** Campo de texto para la hora de fin (formato HH:mm). */
    private JTextField txtHoraFin;

    /** Botón para agregar una nueva franja horaria. */
    private JButton btnAgregarFranja;

    /** Botón para eliminar la franja seleccionada. */
    private JButton btnEliminarFranja;

    /** Botón para generar slots de la semana. */
    private JButton btnGenerarSlots;

    /** Tabla que muestra las franjas del veterinario seleccionado. */
    private JTable tablaFranjas;

    /** Modelo de datos de la tabla de franjas. */
    private DefaultTableModel modeloFranjas;

    /** Lista de IDs de las franjas mostradas (para eliminar la seleccionada). */
    private java.util.List<Integer> idsAgenda;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor del diálogo de configuración de agenda.
     *
     * @param agendaService servicio de agenda inyectado desde la ventana principal
     */
    public ConfigurarAgendaDialog(AgendaService agendaService) {
        super("Configurar Agenda Veterinaria",
              true, true, true, true);

        this.agendaService = agendaService;
        this.idsAgenda = new java.util.ArrayList<>();

        construirUI();
        cargarVeterinarios();
    }

    // =========================================================================
    // Construcción de la UI
    // =========================================================================

    /**
     * Construye la interfaz gráfica del diálogo de configuración de agenda.
     */
    private void construirUI() {
        setLayout(new BorderLayout(0, 8));
        getContentPane().setBackground(new Color(248, 249, 252));

        add(construirPanelSeleccionVet(), BorderLayout.NORTH);
        add(construirPanelCentral(),      BorderLayout.CENTER);
        add(construirPanelBotones(),      BorderLayout.SOUTH);
    }

    /**
     * Construye el panel superior de selección de veterinario.
     *
     * @return panel de selección
     */
    private JPanel construirPanelSeleccionVet() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JLabel lbl = new JLabel("Veterinario *:");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));

        cmbVeterinario = new JComboBox<>();
        cmbVeterinario.setPreferredSize(new Dimension(280, 28));
        cmbVeterinario.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cmbVeterinario.addActionListener(e -> {
            if (cmbVeterinario.getSelectedItem() instanceof Veterinario) {
                cargarFranjasVeterinario();
            }
        });

        JButton btnActualizarVets = new JButton("🔄");
        btnActualizarVets.setToolTipText("Recargar lista de veterinarios");
        btnActualizarVets.addActionListener(e -> cargarVeterinarios());

        panel.add(lbl);
        panel.add(cmbVeterinario);
        panel.add(btnActualizarVets);

        return panel;
    }

    /**
     * Construye el panel central con el formulario y la tabla de franjas.
     *
     * @return panel central dividido en dos secciones
     */
    private JPanel construirPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(248, 249, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));

        panel.add(construirPanelFormularioFranja(), BorderLayout.NORTH);
        panel.add(construirPanelTablaFranjas(),     BorderLayout.CENTER);

        return panel;
    }

    /**
     * Construye el formulario para ingresar una nueva franja horaria.
     *
     * @return panel del formulario
     */
    private JPanel construirPanelFormularioFranja() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 240)),
            "Nueva Franja Horaria",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 10, 6, 10);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        // Fila 0 — Día de la semana
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Día de la semana *:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbDia = new JComboBox<>(DIAS_SEMANA);
        cmbDia.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(cmbDia, gbc);

        // Fila 1 — Hora inicio
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel lblInicio = new JLabel("Hora de inicio * (HH:mm):");
        panel.add(lblInicio, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtHoraInicio = new JTextField("08:00");
        txtHoraInicio.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtHoraInicio.setPreferredSize(new Dimension(100, 28));
        panel.add(txtHoraInicio, gbc);

        // Fila 2 — Hora fin
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel lblFin = new JLabel("Hora de fin * (HH:mm):");
        panel.add(lblFin, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtHoraFin = new JTextField("12:00");
        txtHoraFin.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panel.add(txtHoraFin, gbc);

        // Fila 3 — Botón Agregar
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        btnAgregarFranja = new JButton("➕ Agregar Franja");
        btnAgregarFranja.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnAgregarFranja.setBackground(new Color(0, 120, 215));
        btnAgregarFranja.setForeground(Color.WHITE);
        btnAgregarFranja.setFocusPainted(false);
        btnAgregarFranja.addActionListener(e -> agregarFranja());
        panel.add(btnAgregarFranja, gbc);

        return panel;
    }

    /**
     * Construye el panel con la tabla de franjas horarias del veterinario.
     *
     * @return panel de la tabla
     */
    private JPanel construirPanelTablaFranjas() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 200, 240)),
            "Franjas Configuradas",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12)
        ));

        // Modelo no editable
        modeloFranjas = new DefaultTableModel(COLUMNAS_FRANJAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaFranjas = new JTable(modeloFranjas);
        tablaFranjas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaFranjas.setRowHeight(22);
        tablaFranjas.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tablaFranjas.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tablaFranjas.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablaFranjas.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tablaFranjas);
        scroll.setPreferredSize(new Dimension(0, 180));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Construye el panel inferior con los botones de acción.
     *
     * @return panel de botones
     */
    private JPanel construirPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        panel.setBackground(new Color(240, 244, 252));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        btnEliminarFranja = new JButton("🗑 Eliminar Seleccionada");
        btnEliminarFranja.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnEliminarFranja.addActionListener(e -> eliminarFranjaSeleccionada());

        btnGenerarSlots = new JButton("⚡ Generar Slots de la Semana");
        btnGenerarSlots.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnGenerarSlots.setBackground(new Color(0, 160, 100));
        btnGenerarSlots.setForeground(Color.WHITE);
        btnGenerarSlots.setFocusPainted(false);
        btnGenerarSlots.addActionListener(e -> generarSlotsSemana());

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCerrar.addActionListener(e -> {
            try {
                setClosed(true);
            } catch (java.beans.PropertyVetoException ex) {
                // Ignorar
            }
        });

        panel.add(btnEliminarFranja);
        panel.add(btnGenerarSlots);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(btnCerrar);

        return panel;
    }

    // =========================================================================
    // Lógica de negocio
    // =========================================================================

    /**
     * Carga los veterinarios activos en el {@link JComboBox} de selección.
     */
    private void cargarVeterinarios() {
        try {
            List<Veterinario> vets = agendaService.obtenerVeterinariosActivos();
            cmbVeterinario.removeAllItems();
            // Estructuras repetitivas: for-each para llenar el combo
            for (Veterinario vet : vets) {
                cmbVeterinario.addItem(vet);
            }
            if (!vets.isEmpty()) {
                cmbVeterinario.setSelectedIndex(0);
                cargarFranjasVeterinario();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar veterinarios:\n" + e.getMessage(),
                "Error de base de datos",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga las franjas del veterinario seleccionado en el {@link JComboBox}
     * y las muestra en la tabla.
     */
    private void cargarFranjasVeterinario() {
        Veterinario vet = (Veterinario) cmbVeterinario.getSelectedItem();
        if (vet == null) return;

        modeloFranjas.setRowCount(0);
        idsAgenda.clear();

        try {
            List<AgendaDisponibilidad> franjas =
                agendaService.obtenerFranjasPorVeterinario(vet.getId());

            // Estructuras repetitivas: for-each para llenar la tabla
            for (AgendaDisponibilidad franja : franjas) {
                modeloFranjas.addRow(new Object[]{
                    franja.getDiaSemana(),
                    franja.getHoraInicio().toString().substring(0, 5),
                    franja.getHoraFin().toString().substring(0, 5)
                });
                idsAgenda.add(franja.getIdAgenda());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar franjas del veterinario:\n" + e.getMessage(),
                "Error de base de datos",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Valida los campos del formulario y agrega una nueva franja horaria.
     *
     * <p>Validaciones:</p>
     * <ol>
     *   <li>Veterinario seleccionado.</li>
     *   <li>Horas en formato HH:mm válido.</li>
     *   <li>Hora de inicio &lt; hora de fin.</li>
     * </ol>
     * <p>Si la BD detecta superposición, muestra un {@link JOptionPane#ERROR_MESSAGE}.</p>
     */
    private void agregarFranja() {
        // Validar veterinario seleccionado
        Veterinario vet = (Veterinario) cmbVeterinario.getSelectedItem();
        if (vet == null) {
            JOptionPane.showMessageDialog(this,
                "Debe seleccionar un veterinario.",
                "Campo obligatorio",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String diaSeleccionado = (String) cmbDia.getSelectedItem();
        LocalTime horaInicio;
        LocalTime horaFin;

        // Validar formato de horas (estructuras condicionales)
        try {
            horaInicio = LocalTime.parse(txtHoraInicio.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                "El formato de 'Hora de inicio' es inválido.\n"
                + "Use el formato HH:mm (ej.: 08:00)",
                "Formato inválido",
                JOptionPane.WARNING_MESSAGE);
            txtHoraInicio.requestFocus();
            return;
        }
        try {
            horaFin = LocalTime.parse(txtHoraFin.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                "El formato de 'Hora de fin' es inválido.\n"
                + "Use el formato HH:mm (ej.: 12:00)",
                "Formato inválido",
                JOptionPane.WARNING_MESSAGE);
            txtHoraFin.requestFocus();
            return;
        }

        // Delegar al service (que también valida coherencia y superposición)
        try {
            agendaService.agregarFranja(vet.getId(), diaSeleccionado, horaInicio, horaFin);

            JOptionPane.showMessageDialog(this,
                "Franja agregada correctamente:\n"
                + diaSeleccionado + " de " + horaInicio + " a " + horaFin,
                "Franja agregada",
                JOptionPane.INFORMATION_MESSAGE);

            // Refrescar tabla
            cargarFranjasVeterinario();

        } catch (FranjaSuperpuestaException e) {
            JOptionPane.showMessageDialog(this,
                "Las franjas horarias se superponen:\n" + e.getMessage()
                + "\n\nVerifique los horarios configurados para este veterinario.",
                "Superposición de franjas (RN-11)",
                JOptionPane.ERROR_MESSAGE);

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                e.getMessage(),
                "Error de validación",
                JOptionPane.WARNING_MESSAGE);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error de base de datos al agregar la franja:\n" + e.getMessage(),
                "Error de base de datos",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Elimina la franja seleccionada en la tabla previa confirmación del usuario.
     */
    private void eliminarFranjaSeleccionada() {
        int filaSeleccionada = tablaFranjas.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this,
                "Seleccione una franja de la tabla para eliminar.",
                "Sin selección",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idAgenda = idsAgenda.get(filaSeleccionada);
        String dia        = (String) modeloFranjas.getValueAt(filaSeleccionada, 0);
        String horaInicio = (String) modeloFranjas.getValueAt(filaSeleccionada, 1);
        String horaFin    = (String) modeloFranjas.getValueAt(filaSeleccionada, 2);

        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Desea eliminar la franja del día " + dia + " de " + horaInicio + " a " + horaFin + "?\n"
            + "Los slots ya generados no se eliminarán.",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) return;

        try {
            agendaService.eliminarFranja(idAgenda);
            cargarFranjasVeterinario();
            JOptionPane.showMessageDialog(this,
                "Franja eliminada correctamente.",
                "Eliminación exitosa",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al eliminar la franja:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Genera los slots de 30 minutos para los próximos 14 días del veterinario seleccionado.
     * Solicita confirmación previa al usuario.
     */
    private void generarSlotsSemana() {
        Veterinario vet = (Veterinario) cmbVeterinario.getSelectedItem();
        if (vet == null) {
            JOptionPane.showMessageDialog(this,
                "Debe seleccionar un veterinario.",
                "Campo obligatorio",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (modeloFranjas.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "El veterinario no tiene franjas configuradas.\n"
                + "Agregue al menos una franja antes de generar slots.",
                "Sin franjas",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Generar slots de 30 minutos para los próximos 14 días\n"
            + "del Dr./Dra. " + vet.toString() + "?\n\n"
            + "Los slots ya existentes no se duplicarán (INSERT IGNORE).",
            "Confirmar generación de slots",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) return;

        try {
            // Deshabilitar botón y mostrar cursor de espera
            btnGenerarSlots.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            agendaService.generarSlotsSemana(vet.getId());

            JOptionPane.showMessageDialog(this,
                "Slots generados correctamente para los próximos 14 días\n"
                + "del Dr./Dra. " + vet.toString() + ".",
                "Slots generados",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                "Error de validación", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error al generar los slots:\n" + e.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnGenerarSlots.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }
}
