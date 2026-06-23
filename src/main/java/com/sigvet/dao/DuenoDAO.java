package com.sigvet.dao;

import com.sigvet.model.Dueno;
import com.sigvet.model.enums.EstadoRegistro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Dueno}.
 *
 * <p>Proporciona operaciones CRUD y consultas específicas sobre la tabla
 * {@code dueno} de la base de datos MySQL {@code sigvet}. Incluye la
 * operación de anonimización de datos personales (RN-13, Ley 25.326)
 * mediante el SP {@code sp_anonimizar_dueno}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Dueno
 * @see BaseDAO
 */
public class DuenoDAO extends BaseDAO<Dueno> {

    // =========================================================================
    // Implementación de métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca un dueño por su identificador único.
     *
     * @param id identificador único del dueño (PK {@code id_dueno})
     * @return dueño encontrado, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Dueno buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_dueno, dni, nombre, apellido, telefono, direccion, email, estado "
                   + "FROM dueno WHERE id_dueno = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearDueno(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos los dueños registrados (activos e inactivos).
     *
     * @return lista de dueños ordenada por apellido
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Dueno> buscarTodos() throws SQLException {
        String sql = "SELECT id_dueno, dni, nombre, apellido, telefono, direccion, email, estado "
                   + "FROM dueno ORDER BY apellido ASC, nombre ASC";
        List<Dueno> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearDueno(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un nuevo dueño en la base de datos.
     *
     * @param d dueño con los datos a persistir; el {@code id} es generado por AUTO_INCREMENT
     * @throws SQLException si el DNI ya existe (violación de UNIQUE) u otro error de BD
     */
    @Override
    public void insertar(Dueno d) throws SQLException {
        String sql = "INSERT INTO dueno (dni, nombre, apellido, telefono, direccion, email, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getDni());
            ps.setString(2, d.getNombre());
            ps.setString(3, d.getApellido());
            ps.setString(4, d.getTelefono());
            ps.setString(5, d.getDireccion());
            ps.setString(6, d.getEmail());
            ps.setString(7, d.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    d.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza los datos de un dueño existente.
     *
     * @param d dueño con los datos actualizados; debe tener el {@code id} asignado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Dueno d) throws SQLException {
        String sql = "UPDATE dueno SET dni = ?, nombre = ?, apellido = ?, telefono = ?, "
                   + "direccion = ?, email = ?, estado = ? WHERE id_dueno = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getDni());
            ps.setString(2, d.getNombre());
            ps.setString(3, d.getApellido());
            ps.setString(4, d.getTelefono());
            ps.setString(5, d.getDireccion());
            ps.setString(6, d.getEmail());
            ps.setString(7, d.getEstado().name());
            ps.setInt(8, d.getId());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Busca un dueño por su número de documento de identidad.
     *
     * @param dni DNI a buscar
     * @return dueño encontrado, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Dueno buscarPorDni(String dni) throws SQLException {
        String sql = "SELECT id_dueno, dni, nombre, apellido, telefono, direccion, email, estado "
                   + "FROM dueno WHERE dni = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearDueno(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna solo los dueños con estado {@link EstadoRegistro#Activo}.
     *
     * @return lista de dueños activos ordenada por apellido
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Dueno> buscarActivos() throws SQLException {
        String sql = "SELECT id_dueno, dni, nombre, apellido, telefono, direccion, email, estado "
                   + "FROM dueno WHERE estado = 'Activo' ORDER BY apellido ASC, nombre ASC";
        List<Dueno> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearDueno(rs));
            }
        }
        return lista;
    }

    /**
     * Aplica baja lógica a un dueño cambiando su estado a {@link EstadoRegistro#Inactivo}.
     *
     * @param id identificador del dueño a dar de baja
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void bajaLogica(int id) throws SQLException {
        String sql = "UPDATE dueno SET estado = 'Inactivo' WHERE id_dueno = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Anonimiza los datos personales de un dueño en cumplimiento de la Ley 25.326
     * de Protección de Datos Personales (RN-13).
     *
     * <p>Llama al SP {@code sp_anonimizar_dueno} que reemplaza los datos personales
     * (nombre, apellido, DNI, teléfono, email, dirección) con valores genéricos
     * preservando solo el ID para mantener integridad referencial con mascotas y turnos.</p>
     *
     * @param idDueno identificador del dueño a anonimizar
     * @throws SQLException si ocurre un error durante la anonimización
     */
    public void anonimizar(int idDueno) throws SQLException {
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_anonimizar_dueno(?)}")) {
            cs.setInt(1, idDueno);
            cs.execute();
        }
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Dueno}.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Dueno} con los datos de la fila
     * @throws SQLException si ocurre un error al leer las columnas del ResultSet
     */
    private Dueno mapearDueno(ResultSet rs) throws SQLException {
        int id             = rs.getInt("id_dueno");
        String dni         = rs.getString("dni");
        String nombre      = rs.getString("nombre");
        String apellido    = rs.getString("apellido");
        String telefono    = rs.getString("telefono");
        String direccion   = rs.getString("direccion");
        String email       = rs.getString("email");
        EstadoRegistro est = EstadoRegistro.valueOf(rs.getString("estado"));
        return new Dueno(id, dni, nombre, apellido, telefono, direccion, email, est);
    }
}
