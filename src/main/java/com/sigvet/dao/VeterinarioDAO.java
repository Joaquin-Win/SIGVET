package com.sigvet.dao;

import com.sigvet.model.Veterinario;
import com.sigvet.model.enums.EstadoRegistro;
import com.sigvet.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Veterinario}.
 *
 * <p>Proporciona operaciones CRUD y consultas específicas sobre la tabla
 * {@code veterinario} de la base de datos MySQL {@code sigvet}.</p>
 *
 * <p>Extiende {@link BaseDAO}{@code <Veterinario>} heredando el método
 * {@code getConnection()} y el contrato de los 4 métodos abstractos.</p>
 *
 * <p>Todas las conexiones se obtienen y cierran dentro de bloques
 * {@code try-with-resources} para garantizar la liberación de recursos.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Veterinario
 * @see BaseDAO
 */
public class VeterinarioDAO extends BaseDAO<Veterinario> {

    // =========================================================================
    // Implementación de métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca un veterinario por su identificador único.
     *
     * @param id identificador único del veterinario (PK {@code id_veterinario})
     * @return veterinario encontrado, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Veterinario buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_veterinario, nombre, apellido, matricula, telefono, email, estado "
                   + "FROM veterinario WHERE id_veterinario = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearVeterinario(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todos los veterinarios registrados (activos e inactivos).
     *
     * @return lista de veterinarios ordenada por apellido
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Veterinario> buscarTodos() throws SQLException {
        String sql = "SELECT id_veterinario, nombre, apellido, matricula, telefono, email, estado "
                   + "FROM veterinario ORDER BY apellido ASC, nombre ASC";
        List<Veterinario> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearVeterinario(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta un nuevo veterinario en la base de datos.
     *
     * @param v veterinario con los datos a persistir; el campo {@code id} es ignorado
     *          ya que la BD lo genera automáticamente (AUTO_INCREMENT)
     * @throws SQLException si la matrícula ya existe (violación de UNIQUE) u otro error de BD
     */
    @Override
    public void insertar(Veterinario v) throws SQLException {
        String sql = "INSERT INTO veterinario (nombre, apellido, matricula, telefono, email, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getNombre());
            ps.setString(2, v.getApellido());
            ps.setString(3, v.getMatricula());
            ps.setString(4, v.getTelefono());
            ps.setString(5, v.getEmail());
            ps.setString(6, v.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    v.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza los datos de un veterinario existente.
     *
     * @param v veterinario con los datos actualizados; debe tener el {@code id} asignado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Veterinario v) throws SQLException {
        String sql = "UPDATE veterinario SET nombre = ?, apellido = ?, matricula = ?, "
                   + "telefono = ?, email = ?, estado = ? WHERE id_veterinario = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getNombre());
            ps.setString(2, v.getApellido());
            ps.setString(3, v.getMatricula());
            ps.setString(4, v.getTelefono());
            ps.setString(5, v.getEmail());
            ps.setString(6, v.getEstado().name());
            ps.setInt(7, v.getId());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Busca un veterinario por su número de matrícula profesional.
     *
     * @param matricula matrícula profesional a buscar
     * @return veterinario encontrado, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Veterinario buscarPorMatricula(String matricula) throws SQLException {
        String sql = "SELECT id_veterinario, nombre, apellido, matricula, telefono, email, estado "
                   + "FROM veterinario WHERE matricula = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matricula);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearVeterinario(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna solo los veterinarios con estado {@link EstadoRegistro#Activo}.
     *
     * <p>Este es el método utilizado para poblar los combo boxes de la UI,
     * ya que solo los veterinarios activos pueden asignarse a turnos y consultas.</p>
     *
     * @return lista de veterinarios activos ordenada por apellido
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Veterinario> buscarActivos() throws SQLException {
        String sql = "SELECT id_veterinario, nombre, apellido, matricula, telefono, email, estado "
                   + "FROM veterinario WHERE estado = 'Activo' ORDER BY apellido ASC, nombre ASC";
        List<Veterinario> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearVeterinario(rs));
            }
        }
        return lista;
    }

    /**
     * Aplica baja lógica a un veterinario cambiando su estado a {@link EstadoRegistro#Inactivo}.
     *
     * <p>No elimina el registro físicamente para preservar la integridad referencial
     * con los turnos y consultas históricas.</p>
     *
     * @param id identificador del veterinario a dar de baja
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void bajaLogica(int id) throws SQLException {
        String sql = "UPDATE veterinario SET estado = 'Inactivo' WHERE id_veterinario = ?";
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
     * Mapea una fila del {@link ResultSet} a un objeto {@link Veterinario}.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Veterinario} con los datos de la fila
     * @throws SQLException si ocurre un error al leer las columnas del ResultSet
     */
    private Veterinario mapearVeterinario(ResultSet rs) throws SQLException {
        int id             = rs.getInt("id_veterinario");
        String nombre      = rs.getString("nombre");
        String apellido    = rs.getString("apellido");
        String matricula   = rs.getString("matricula");
        String telefono    = rs.getString("telefono");
        String email       = rs.getString("email");
        EstadoRegistro est = EstadoRegistro.valueOf(rs.getString("estado"));
        return new Veterinario(id, nombre, apellido, matricula, telefono, email, est);
    }
}
