package com.sigvet.dao;

import com.sigvet.model.AlertaStock;
import com.sigvet.model.Medicamento;
import com.sigvet.model.enums.EstadoAlerta;
import com.sigvet.model.enums.EstadoRegistro;
import com.sigvet.model.enums.TipoAlerta;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link AlertaStock}.
 *
 * <p>Las alertas son generadas automáticamente por los triggers de la BD:
 * <ul>
 *   <li>{@code trg_alerta_stock_bajo_insert} – al ingresar stock</li>
 *   <li>{@code trg_alerta_stock_bajo_update} – al actualizar stock</li>
 *   <li>{@code trg_alerta_vencimiento_insert} – al ingresar lotes próximos a vencer</li>
 * </ul>
 * El DAO solo consulta y actualiza el estado de las alertas.</p>
 *
 * <p><strong>Nota de mapeo BD:</strong> la columna {@code estado} en MySQL usa
 * {@code 'En Gestion'} con espacio. Se usa {@link EstadoAlerta#fromDbValue(String)}
 * y {@link EstadoAlerta#toDbValue()} para la conversión bidireccional.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see AlertaStock
 * @see EstadoAlerta
 * @see BaseDAO
 */
public class AlertaStockDAO extends BaseDAO<AlertaStock> {

    /** SQL base con JOIN para construir alertas completas con medicamento. */
    private static final String SQL_SELECT_COMPLETO =
        "SELECT a.id_alerta, a.tipo, a.mensaje, a.estado AS estado_alerta, "
      + "       a.fecha_generacion, a.fecha_resolucion, "
      + "       m.id_medicamento, m.nombre_generico, m.nombre_comercial, "
      + "       m.dosis_presentacion, m.precio_venta, m.stock_minimo_alerta, "
      + "       m.estado AS estado_med, m.fecha_actualizacion_precio "
      + "FROM alerta_stock a "
      + "JOIN medicamento m ON m.id_medicamento = a.id_medicamento ";

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca una alerta por su identificador único.
     *
     * @param id identificador único de la alerta (PK {@code id_alerta})
     * @return alerta con medicamento completo, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public AlertaStock buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE a.id_alerta = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearAlerta(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todas las alertas (pendientes, en gestión y resueltas).
     *
     * @return lista de alertas ordenada por fecha de generación descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<AlertaStock> buscarTodos() throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "ORDER BY a.fecha_generacion DESC";
        List<AlertaStock> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearAlerta(rs));
            }
        }
        return lista;
    }

    /**
     * Inserción manual de alertas (rara vez usada; normalmente las genera la BD).
     *
     * @param a alerta a insertar
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void insertar(AlertaStock a) throws SQLException {
        String sql = "INSERT INTO alerta_stock (id_medicamento, tipo, mensaje, estado) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getMedicamento().getIdMedicamento());
            ps.setString(2, a.getTipo().name());
            ps.setString(3, a.getMensaje());
            ps.setString(4, a.getEstado().toDbValue());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setIdAlerta(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza el estado de una alerta.
     *
     * @param a alerta con el estado actualizado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(AlertaStock a) throws SQLException {
        cambiarEstado(a.getIdAlerta(), a.getEstado());
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Retorna las alertas activas (con estado {@link EstadoAlerta#Pendiente} o
     * {@link EstadoAlerta#En_Gestion}).
     *
     * <p><strong>NOTA BD:</strong> el valor {@code 'En Gestion'} en la columna de BD
     * se mapea a {@link EstadoAlerta#En_Gestion} en Java. La cláusula WHERE usa los
     * valores de la BD via {@link EstadoAlerta#toDbValue()}.</p>
     *
     * @return lista de alertas activas ordenada por fecha de generación descendente
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<AlertaStock> buscarActivas() throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE a.estado IN (?, ?) AND m.estado = 'Activo' "
                   + "ORDER BY a.fecha_generacion DESC";
        List<AlertaStock> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Usar los valores que la BD almacena realmente
            ps.setString(1, EstadoAlerta.Pendiente.toDbValue());
            ps.setString(2, EstadoAlerta.En_Gestion.toDbValue()); // 'En Gestion' con espacio
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAlerta(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Retorna las alertas de un tipo específico.
     *
     * @param tipo tipo de alerta a filtrar
     * @return lista de alertas del tipo especificado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<AlertaStock> buscarPorTipo(TipoAlerta tipo) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE a.tipo = ? ORDER BY a.fecha_generacion DESC";
        List<AlertaStock> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAlerta(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Cambia el estado de una alerta.
     *
     * <p>Usa {@link EstadoAlerta#toDbValue()} para obtener el valor exacto que
     * corresponde al ENUM de la BD ({@code 'En Gestion'} con espacio si aplica).</p>
     *
     * @param idAlerta    identificador de la alerta
     * @param nuevoEstado nuevo estado a asignar
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void cambiarEstado(int idAlerta, EstadoAlerta nuevoEstado) throws SQLException {
        String sql = "UPDATE alerta_stock SET estado = ? WHERE id_alerta = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.toDbValue());
            ps.setInt(2, idAlerta);
            ps.executeUpdate();
        }
    }

    /**
     * Marca una alerta como resuelta: cambia estado a {@link EstadoAlerta#Resuelta}
     * y registra la fecha de resolución como {@code NOW()}.
     *
     * @param idAlerta identificador de la alerta a resolver
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void resolverAlerta(int idAlerta) throws SQLException {
        String sql = "UPDATE alerta_stock SET estado = ?, fecha_resolucion = NOW() "
                   + "WHERE id_alerta = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EstadoAlerta.Resuelta.toDbValue());
            ps.setInt(2, idAlerta);
            ps.executeUpdate();
        }
    }

    /**
     * Consulta la vista {@code vw_alertas_activas} y retorna alertas con datos
     * enriquecidos del medicamento.
     *
     * <p>La vista ya filtra por estados activos y ordena por prioridad.
     * Retorna los mismos {@link AlertaStock} que {@link #buscarActivas()},
     * pero leyendo directamente de la vista.</p>
     *
     * @return lista de alertas activas desde la vista
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<AlertaStock> buscarAlertasActivas() throws SQLException {
        // Usa la tabla base en vez de la vista porque vw_alertas_activas
        // no expone id_medicamento (columna necesaria para el mapeo completo).
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE a.estado IN ('Pendiente', 'En Gestion') AND m.estado = 'Activo' "
                   + "ORDER BY a.fecha_generacion DESC";
        List<AlertaStock> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearAlerta(rs));
            }
        }
        return lista;
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link AlertaStock}.
     *
     * <p>El estado de la BD ({@code 'En Gestion'} con espacio) se convierte al
     * enum Java usando {@link EstadoAlerta#fromDbValue(String)}.</p>
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link AlertaStock} con medicamento completo
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private AlertaStock mapearAlerta(ResultSet rs) throws SQLException {
        // Medicamento
        EstadoRegistro estMed = EstadoRegistro.valueOf(rs.getString("estado_med"));
        Date fap = rs.getDate("fecha_actualizacion_precio");
        LocalDate fechaAct = (fap != null) ? fap.toLocalDate() : null;
        Medicamento med = new Medicamento(
            rs.getInt("id_medicamento"),
            rs.getString("nombre_generico"),
            rs.getString("nombre_comercial"),
            rs.getString("dosis_presentacion"),
            rs.getDouble("precio_venta"),
            rs.getInt("stock_minimo_alerta"),
            estMed,
            fechaAct
        );
        // Tipo y estado con conversión
        TipoAlerta tipo = TipoAlerta.valueOf(rs.getString("tipo"));
        // Mapeo especial: 'En Gestion' en BD → EstadoAlerta.En_Gestion en Java
        EstadoAlerta estado = EstadoAlerta.fromDbValue(rs.getString("estado_alerta"));
        // Fechas
        Timestamp tsFechaGen = rs.getTimestamp("fecha_generacion");
        LocalDateTime fechaGen = (tsFechaGen != null) ? tsFechaGen.toLocalDateTime() : LocalDateTime.now();
        Timestamp tsFechaRes = rs.getTimestamp("fecha_resolucion");
        LocalDateTime fechaRes = (tsFechaRes != null) ? tsFechaRes.toLocalDateTime() : null;
        return new AlertaStock(
            rs.getInt("id_alerta"),
            med,
            tipo,
            rs.getString("mensaje"),
            estado,
            fechaGen,
            fechaRes
        );
    }
}
