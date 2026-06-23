package com.sigvet.service;

import com.sigvet.dao.ConsultaMedicaDAO;
import com.sigvet.dao.ItemRecetaDAO;
import com.sigvet.dao.MedicamentoDAO;
import com.sigvet.dao.StockDAO;
import com.sigvet.dao.TurnoDAO;
import com.sigvet.dao.VeterinarioDAO;
import com.sigvet.exception.ConsultaNoEliminableException;
import com.sigvet.exception.StockInsuficienteException;
import com.sigvet.model.ConsultaMedica;
import com.sigvet.model.Medicamento;
import com.sigvet.model.Stock;
import com.sigvet.model.Turno;
import com.sigvet.model.enums.EstadoTurno;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Servicio de lógica de negocio para el caso de uso CU-03: Registrar Consulta Médica.
 *
 * <p>Es el servicio más crítico del sistema. Orquesta el flujo multi-paso de
 * registro de una consulta médica con prescripción de medicamentos:</p>
 *
 * <ol>
 *   <li>Validaciones de entrada (feedback rápido al usuario).</li>
 *   <li>Registro de la consulta via SP {@code sp_registrar_consulta_turno} o
 *       {@code sp_registrar_consulta_urgencia}, que retorna {@code id_consulta} via OUT param.</li>
 *   <li>Por cada medicamento prescripto:
 *     <ul>
 *       <li>Verificación de stock disponible ({@link StockDAO#stockTotal}).</li>
 *       <li>Descuento FIFO via SP {@code sp_descontar_stock_fifo} (solo 2 params: idMedicamento, cantidad).</li>
 *       <li>Inserción directa del ítem de receta en {@code item_receta} con PreparedStatement.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><strong>IMPORTANTE — No se usa JSON:</strong> el SP {@code sp_registrar_consulta_turno}
 * NO recibe medicamentos. El descuento de stock y la inserción de {@code item_receta}
 * se realizan paso a paso por separado.</p>
 *
 * <p><strong>RN-07:</strong> NUNCA se expone ni llama un método de eliminación física
 * de consultas médicas.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see ConsultaMedicaDAO
 * @see StockDAO
 * @see ItemRecetaDAO
 */
public class ConsultaMedicaService {

    /** DAO para la gestión de consultas médicas. */
    private final ConsultaMedicaDAO consultaDAO;

    /** DAO para la consulta de turnos. */
    private final TurnoDAO turnoDAO;

    /** DAO para la consulta del catálogo de medicamentos. */
    private final MedicamentoDAO medicamentoDAO;

    /** DAO para la gestión de stock y lotes. */
    private final StockDAO stockDAO;

    /** DAO para la inserción de ítems de receta. */
    private final ItemRecetaDAO itemRecetaDAO;

    /** DAO para la consulta de veterinarios. */
    private final VeterinarioDAO veterinarioDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia todos los DAOs necesarios para el flujo de consulta médica.
     */
    public ConsultaMedicaService() {
        this.consultaDAO    = new ConsultaMedicaDAO();
        this.turnoDAO       = new TurnoDAO();
        this.medicamentoDAO = new MedicamentoDAO();
        this.stockDAO       = new StockDAO();
        this.itemRecetaDAO  = new ItemRecetaDAO();
        this.veterinarioDAO = new VeterinarioDAO();
    }

    // =========================================================================
    // Métodos de negocio
    // =========================================================================

    /**
     * Retorna los turnos con estado {@code Pendiente} del día actual,
     * para que el veterinario seleccione el turno a atender.
     *
     * @return lista de turnos pendientes de hoy
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Turno> obtenerTurnosPendientes() throws SQLException {
        return turnoDAO.buscarPendientesPorFecha(LocalDate.now());
    }

    /**
     * Retorna el stock total disponible (lotes no vencidos) de un medicamento,
     * llamando a la función escalar {@code fn_stock_total_medicamento}.
     *
     * @param idMedicamento identificador del medicamento
     * @return cantidad total disponible en unidades
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public int stockDisponibleMedicamento(int idMedicamento) throws SQLException {
        return stockDAO.stockTotal(idMedicamento);
    }

    /**
     * Registra una consulta médica asociada a un turno existente (flujo normal).
     *
     * <p><strong>Flujo multi-paso:</strong></p>
     * <ol>
     *   <li>Validar campos obligatorios (síntomas, diagnóstico, IDs).</li>
     *   <li>Llamar a {@code sp_registrar_consulta_turno} via DAO → crea consulta +
     *       marca turno como {@link EstadoTurno#Atendido} + retorna {@code id_consulta}.</li>
     *   <li>Para cada medicamento de la lista:
     *     <ul>
     *       <li>Verificar stock disponible; si insuficiente → {@link StockInsuficienteException}.</li>
     *       <li>Llamar a {@code sp_descontar_stock_fifo(idMedicamento, cantidad)} (2 params).</li>
     *       <li>Obtener el primer lote con stock del medicamento para conocer el {@code id_stock}.</li>
     *       <li>Insertar en {@code item_receta} con PreparedStatement directo.</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <p><strong>Estructura de cada Map en {@code medicamentos}:</strong></p>
     * <pre>
     * {
     *   "idMedicamento" → Integer,
     *   "cantidad"      → Integer,
     *   "dosis"         → String,
     *   "frecuencia"    → String (nullable),
     *   "duracion"      → String (nullable)
     * }
     * </pre>
     *
     * @param idTurno       identificador del turno
     * @param idMascota     identificador de la mascota
     * @param idVeterinario identificador del veterinario actuante
     * @param sintomas      síntomas reportados (obligatorio)
     * @param diagnostico   diagnóstico del veterinario (obligatorio)
     * @param medicamentos  lista de medicamentos prescriptos (puede ser vacía)
     * @return identificador de la consulta creada
     * @throws IllegalArgumentException   si los campos obligatorios están vacíos
     * @throws StockInsuficienteException si el stock es insuficiente para algún medicamento (RN-02)
     * @throws SQLException               si ocurre un error de acceso a la BD
     */
    public int registrarConsultaConTurno(int idTurno, int idMascota, int idVeterinario,
                                          String sintomas, String diagnostico,
                                          List<Map<String, Object>> medicamentos)
            throws IllegalArgumentException, StockInsuficienteException, SQLException {

        // --- Validaciones de entrada ---
        if (sintomas == null || sintomas.trim().isEmpty()) {
            throw new IllegalArgumentException("Los síntomas son obligatorios.");
        }
        if (diagnostico == null || diagnostico.trim().isEmpty()) {
            throw new IllegalArgumentException("El diagnóstico es obligatorio.");
        }
        if (idTurno <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un turno válido.");
        }
        if (idVeterinario <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un veterinario válido.");
        }

        // --- Verificar stock de TODOS los medicamentos antes de comenzar ---
        // (fail-fast: no procesar nada si alguno falla)
        if (medicamentos != null) {
            for (Map<String, Object> med : medicamentos) {
                int idMed    = (Integer) med.get("idMedicamento");
                int cantidad = (Integer) med.get("cantidad");
                int stockDisp = stockDAO.stockTotal(idMed);
                if (stockDisp < cantidad) {
                    Medicamento m = medicamentoDAO.buscarPorId(idMed);
                    String nombreMed = (m != null) ? m.getNombreComercial() : "ID=" + idMed;
                    throw new StockInsuficienteException(
                        "Stock insuficiente de '" + nombreMed + "': se necesitan "
                        + cantidad + " unidades, hay " + stockDisp + " disponibles (RN-02).");
                }
            }
        }

        // --- Paso 1: Registrar la consulta via SP (retorna id_consulta) ---
        int idConsulta = consultaDAO.registrarConsultaConTurno(
            idTurno, idMascota, idVeterinario, sintomas.trim(), diagnostico.trim()
        );

        // --- Paso 2: Para cada medicamento, descontar stock e insertar item_receta ---
        if (medicamentos != null) {
            for (Map<String, Object> med : medicamentos) {
                int idMed      = (Integer) med.get("idMedicamento");
                int cantidad   = (Integer) med.get("cantidad");
                String dosis   = (String)  med.get("dosis");
                String frec    = (String)  med.get("frecuencia");
                String dur     = (String)  med.get("duracion");

                // Descontar stock FIFO (SP con 2 params: idMedicamento, cantidad)
                try {
                    stockDAO.descontarStockFifo(idMed, cantidad);
                } catch (StockInsuficienteException e) {
                    // Nunca debería llegar aquí (ya verificamos antes), pero por seguridad
                    throw e;
                } catch (SQLException e) {
                    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (msg.contains("stock") || msg.contains("insuficiente")) {
                        throw new StockInsuficienteException(
                            "Stock insuficiente para completar la dispensación (RN-02).");
                    }
                    throw e;
                }

                // Obtener el primer lote con stock (para registrar el id_stock en item_receta)
                List<Stock> lotesDisponibles = stockDAO.buscarNoVencidosPorMedicamento(idMed);
                int idStock = 0;
                // Recorrer los lotes para encontrar el primero con stock > 0 (orden FIFO)
                for (Stock lote : lotesDisponibles) {
                    if (lote.getCantidad() > 0) {
                        idStock = lote.getIdStock();
                        break;
                    }
                }

                // Insertar item_receta con PreparedStatement directo (NO existe SP para esto)
                itemRecetaDAO.insertarItemReceta(idConsulta, idStock, cantidad, dosis, frec, dur);
            }
        }

        return idConsulta;
    }

    /**
     * Registra una consulta de urgencia (sin turno previo, RN-01) llamando al SP
     * {@code sp_registrar_consulta_urgencia} con 5 parámetros (4 IN + 1 OUT).
     *
     * <p>La urgencia no requiere slot ni turno previo. El SP crea la consulta
     * con {@code id_turno = NULL}.</p>
     *
     * @param idMascota     identificador de la mascota
     * @param idVeterinario identificador del veterinario actuante
     * @param sintomas      síntomas reportados (obligatorio)
     * @param diagnostico   diagnóstico del veterinario (obligatorio)
     * @return identificador de la consulta creada
     * @throws IllegalArgumentException si los campos obligatorios están vacíos
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public int registrarConsultaUrgencia(int idMascota, int idVeterinario,
                                          String sintomas, String diagnostico)
            throws IllegalArgumentException, SQLException {
        // --- Validaciones de entrada ---
        if (sintomas == null || sintomas.trim().isEmpty()) {
            throw new IllegalArgumentException("Los síntomas son obligatorios.");
        }
        if (diagnostico == null || diagnostico.trim().isEmpty()) {
            throw new IllegalArgumentException("El diagnóstico es obligatorio.");
        }
        if (idMascota <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una mascota válida.");
        }
        if (idVeterinario <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un veterinario válido.");
        }

        return consultaDAO.registrarConsultaUrgencia(
            idMascota, idVeterinario, sintomas.trim(), diagnostico.trim()
        );
    }

    /**
     * Aplica baja lógica a una consulta médica (RN-07).
     *
     * <p><strong>RN-07:</strong> NUNCA se elimina físicamente una consulta médica.
     * El trigger {@code trg_prevenir_eliminar_consulta} también lo impide en BD.</p>
     *
     * @param idConsulta         identificador de la consulta
     * @param idVeterinarioModif identificador del veterinario que realiza la modificación
     * @throws ConsultaNoEliminableException si se intenta eliminar físicamente (protección adicional)
     * @throws IllegalArgumentException      si los IDs son inválidos
     * @throws SQLException                  si ocurre un error de acceso a la BD
     */
    public void bajaLogicaConsulta(int idConsulta, int idVeterinarioModif)
            throws ConsultaNoEliminableException, IllegalArgumentException, SQLException {
        if (idConsulta <= 0) {
            throw new IllegalArgumentException("ID de consulta inválido.");
        }
        if (idVeterinarioModif <= 0) {
            throw new ConsultaNoEliminableException(
                "Se requiere identificar al veterinario que realiza la baja lógica.");
        }
        consultaDAO.bajaLogica(idConsulta, idVeterinarioModif);
    }

    /**
     * Retorna los medicamentos activos disponibles para prescribir.
     *
     * @return lista de medicamentos con estado {@code Activo}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Medicamento> obtenerMedicamentosActivos() throws SQLException {
        return medicamentoDAO.buscarActivos();
    }

    /**
     * Retorna los lotes no vencidos de un medicamento en orden FIFO.
     *
     * <p>Utilizado para mostrar al veterinario la información de disponibilidad
     * antes de prescribir (número de lote, vencimiento, cantidad).</p>
     *
     * @param idMedicamento identificador del medicamento
     * @return lista de lotes no vencidos ordenada por fecha de vencimiento (FIFO)
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Stock> obtenerLotesNoVencidos(int idMedicamento) throws SQLException {
        return stockDAO.buscarNoVencidosPorMedicamento(idMedicamento);
    }

    /**
     * Busca una consulta médica por su ID.
     *
     * @param idConsulta identificador de la consulta
     * @return consulta encontrada, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public ConsultaMedica buscarConsulta(int idConsulta) throws SQLException {
        return consultaDAO.buscarPorId(idConsulta);
    }

    /**
     * Retorna las consultas activas de una mascota.
     *
     * @param idMascota identificador de la mascota
     * @return lista de consultas activas ordenada por fecha descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<ConsultaMedica> obtenerConsultasPorMascota(int idMascota) throws SQLException {
        return consultaDAO.buscarPorMascota(idMascota);
    }
}
