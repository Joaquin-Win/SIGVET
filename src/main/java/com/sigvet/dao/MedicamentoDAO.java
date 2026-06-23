package com.sigvet.dao;

import com.sigvet.model.Medicamento;
import com.sigvet.model.enums.EstadoRegistro;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Medicamento}.
 *
 * <p>Gestiona el catálogo de medicamentos veterinarios. El stock físico se gestiona
 * por separado mediante {@link StockDAO}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Medicamento
 * @see BaseDAO
 */
public class MedicamentoDAO extends BaseDAO<Medicamento> {

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca un medicamento por su identificador único.
     *
     * @param id identificador único del medicamento (PK {@code id_medicamento})
     * @return medicamento encontrado, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Medicamento buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_medicamento, nombre_generico, nombre_comercial, "
                   + "       dosis_presentacion, precio_venta, stock_minimo_alerta, "
                   + "       estado, fecha_actualizacion_precio "
                   + "FROM medicamento WHERE id_medicamento = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMedicamento(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos los medicamentos del catálogo (activos e inactivos).
     *
     * @return lista de medicamentos ordenada por nombre comercial
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Medicamento> buscarTodos() throws SQLException {
        String sql = "SELECT id_medicamento, nombre_generico, nombre_comercial, "
                   + "       dosis_presentacion, precio_venta, stock_minimo_alerta, "
                   + "       estado, fecha_actualizacion_precio "
                   + "FROM medicamento ORDER BY nombre_comercial ASC";
        List<Medicamento> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearMedicamento(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un nuevo medicamento en el catálogo.
     *
     * @param med medicamento con los datos a persistir
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void insertar(Medicamento med) throws SQLException {
        String sql = "INSERT INTO medicamento (nombre_generico, nombre_comercial, "
                   + "dosis_presentacion, precio_venta, stock_minimo_alerta, estado, "
                   + "fecha_actualizacion_precio) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, med.getNombreGenerico());
            ps.setString(2, med.getNombreComercial());
            ps.setString(3, med.getDosisPresentacion());
            ps.setDouble(4, med.getPrecioVenta());
            ps.setInt(5, med.getStockMinimoAlerta());
            ps.setString(6, med.getEstado().name());
            if (med.getFechaActualizacionPrecio() != null) {
                ps.setDate(7, Date.valueOf(med.getFechaActualizacionPrecio()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    med.setIdMedicamento(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza los datos de un medicamento existente.
     *
     * @param med medicamento con los datos actualizados; debe tener {@code idMedicamento} asignado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Medicamento med) throws SQLException {
        String sql = "UPDATE medicamento SET nombre_generico = ?, nombre_comercial = ?, "
                   + "dosis_presentacion = ?, precio_venta = ?, stock_minimo_alerta = ?, "
                   + "estado = ?, fecha_actualizacion_precio = ? WHERE id_medicamento = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, med.getNombreGenerico());
            ps.setString(2, med.getNombreComercial());
            ps.setString(3, med.getDosisPresentacion());
            ps.setDouble(4, med.getPrecioVenta());
            ps.setInt(5, med.getStockMinimoAlerta());
            ps.setString(6, med.getEstado().name());
            if (med.getFechaActualizacionPrecio() != null) {
                ps.setDate(7, Date.valueOf(med.getFechaActualizacionPrecio()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setInt(8, med.getIdMedicamento());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Busca medicamentos cuyo nombre genérico o comercial contenga el texto dado.
     *
     * @param nombre texto a buscar (búsqueda por coincidencia parcial)
     * @return lista de medicamentos que coinciden con el texto
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Medicamento> buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT id_medicamento, nombre_generico, nombre_comercial, "
                   + "       dosis_presentacion, precio_venta, stock_minimo_alerta, "
                   + "       estado, fecha_actualizacion_precio "
                   + "FROM medicamento "
                   + "WHERE nombre_generico LIKE ? OR nombre_comercial LIKE ? "
                   + "ORDER BY nombre_comercial ASC";
        List<Medicamento> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String patron = "%" + nombre + "%";
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMedicamento(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Retorna solo los medicamentos con estado {@link EstadoRegistro#Activo}.
     *
     * @return lista de medicamentos activos
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Medicamento> buscarActivos() throws SQLException {
        String sql = "SELECT id_medicamento, nombre_generico, nombre_comercial, "
                   + "       dosis_presentacion, precio_venta, stock_minimo_alerta, "
                   + "       estado, fecha_actualizacion_precio "
                   + "FROM medicamento WHERE estado = 'Activo' ORDER BY nombre_comercial ASC";
        List<Medicamento> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearMedicamento(rs));
            }
        }
        return lista;
    }

    /**
     * Aplica baja lógica a un medicamento cambiando su estado a {@link EstadoRegistro#Inactivo}.
     *
     * @param id identificador del medicamento a dar de baja
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void bajaLogica(int id) throws SQLException {
        String sql = "UPDATE medicamento SET estado = 'Inactivo' WHERE id_medicamento = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Medicamento}.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Medicamento}
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private Medicamento mapearMedicamento(ResultSet rs) throws SQLException {
        int id              = rs.getInt("id_medicamento");
        String nomGen       = rs.getString("nombre_generico");
        String nomCom       = rs.getString("nombre_comercial");
        String dosis        = rs.getString("dosis_presentacion");
        double precio       = rs.getDouble("precio_venta");
        int stockMin        = rs.getInt("stock_minimo_alerta");
        EstadoRegistro est  = EstadoRegistro.valueOf(rs.getString("estado"));
        Date fap            = rs.getDate("fecha_actualizacion_precio");
        LocalDate fechaAct  = (fap != null) ? fap.toLocalDate() : null;
        return new Medicamento(id, nomGen, nomCom, dosis, precio, stockMin, est, fechaAct);
    }
}
