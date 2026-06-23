package com.sigvet.dao;

import com.sigvet.model.*;
import com.sigvet.model.enums.EstadoRegistro;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Mascota}.
 *
 * <p>Gestiona las operaciones CRUD sobre la tabla {@code mascota}.
 * Al leer mascotas realiza JOINs con {@code especie}, {@code raza} y {@code dueno}
 * para construir objetos completos por composición (sin IDs sueltos).</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Mascota
 * @see BaseDAO
 */
public class MascotaDAO extends BaseDAO<Mascota> {

    /** SQL base con todos los JOINs necesarios para construir el objeto completo. */
    private static final String SQL_SELECT_COMPLETO =
        "SELECT m.id_mascota, m.nombre, m.fecha_nacimiento, m.sexo, m.color, "
      + "       m.senas_particulares, m.estado AS estado_mascota, "
      + "       d.id_dueno, d.dni, d.nombre AS nombre_dueno, d.apellido AS apellido_dueno, "
      + "       d.telefono AS telefono_dueno, d.direccion, d.email AS email_dueno, "
      + "       d.estado AS estado_dueno, "
      + "       e.id_especie, e.nombre AS nombre_especie, "
      + "       r.id_raza, r.nombre AS nombre_raza "
      + "FROM mascota m "
      + "JOIN dueno d ON d.id_dueno = m.id_dueno "
      + "JOIN especie e ON e.id_especie = m.id_especie "
      + "JOIN raza r ON r.id_raza = m.id_raza ";

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca una mascota por su identificador único, cargando dueño, especie y raza.
     *
     * @param id identificador único de la mascota (PK {@code id_mascota})
     * @return mascota con todos sus objetos relacionados, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Mascota buscarPorId(int id) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE m.id_mascota = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMascota(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todas las mascotas registradas, con sus relaciones cargadas.
     *
     * @return lista de mascotas ordenada por nombre
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Mascota> buscarTodos() throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "ORDER BY m.nombre ASC";
        List<Mascota> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearMascota(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta una nueva mascota en la base de datos.
     *
     * @param m mascota con los datos a persistir; el {@code idMascota} es generado por AUTO_INCREMENT.
     *          Los campos {@code dueno}, {@code especie} y {@code raza} deben tener sus IDs asignados.
     * @throws SQLException si ocurre un error de restricción de FK u otro error de BD
     */
    @Override
    public void insertar(Mascota m) throws SQLException {
        String sql = "INSERT INTO mascota (id_dueno, nombre, id_especie, id_raza, "
                   + "fecha_nacimiento, sexo, color, senas_particulares, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getDueno().getId());
            ps.setString(2, m.getNombre());
            ps.setInt(3, m.getEspecie().getIdEspecie());
            ps.setInt(4, m.getRaza().getIdRaza());
            if (m.getFechaNacimiento() != null) {
                ps.setDate(5, Date.valueOf(m.getFechaNacimiento()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setString(6, m.getSexo());
            ps.setString(7, m.getColor());
            ps.setString(8, m.getSenasParticulares());
            ps.setString(9, m.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    m.setIdMascota(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza los datos de una mascota existente.
     *
     * @param m mascota con los datos actualizados; debe tener {@code idMascota} asignado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Mascota m) throws SQLException {
        String sql = "UPDATE mascota SET id_dueno = ?, nombre = ?, id_especie = ?, id_raza = ?, "
                   + "fecha_nacimiento = ?, sexo = ?, color = ?, senas_particulares = ?, "
                   + "estado = ? WHERE id_mascota = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.getDueno().getId());
            ps.setString(2, m.getNombre());
            ps.setInt(3, m.getEspecie().getIdEspecie());
            ps.setInt(4, m.getRaza().getIdRaza());
            if (m.getFechaNacimiento() != null) {
                ps.setDate(5, Date.valueOf(m.getFechaNacimiento()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setString(6, m.getSexo());
            ps.setString(7, m.getColor());
            ps.setString(8, m.getSenasParticulares());
            ps.setString(9, m.getEstado().name());
            ps.setInt(10, m.getIdMascota());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Retorna todas las mascotas activas de un dueño específico.
     *
     * @param idDueno identificador del dueño
     * @return lista de mascotas activas del dueño
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Mascota> buscarPorDueno(int idDueno) throws SQLException {
        String sql = SQL_SELECT_COMPLETO
                   + "WHERE m.id_dueno = ? AND m.estado = 'Activo' ORDER BY m.nombre ASC";
        List<Mascota> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDueno);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMascota(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Busca mascotas cuyo nombre comienza con el texto dado (búsqueda por prefijo).
     *
     * @param nombre prefijo del nombre a buscar
     * @return lista de mascotas que coinciden
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Mascota> buscarPorNombre(String nombre) throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE m.nombre LIKE ? ORDER BY m.nombre ASC";
        List<Mascota> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMascota(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Retorna todas las mascotas con estado {@link EstadoRegistro#Activo}.
     *
     * @return lista de mascotas activas
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Mascota> buscarActivas() throws SQLException {
        String sql = SQL_SELECT_COMPLETO + "WHERE m.estado = 'Activo' ORDER BY m.nombre ASC";
        List<Mascota> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearMascota(rs));
            }
        }
        return lista;
    }

    /**
     * Aplica baja lógica a una mascota cambiando su estado a {@link EstadoRegistro#Inactivo}.
     *
     * @param id identificador de la mascota a dar de baja
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public void bajaLogica(int id) throws SQLException {
        String sql = "UPDATE mascota SET estado = 'Inactivo' WHERE id_mascota = ?";
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
     * Mapea una fila del {@link ResultSet} al objeto {@link Mascota} con todos sus
     * objetos relacionados construidos por composición.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Mascota} completo
     * @throws SQLException si ocurre un error al leer las columnas del ResultSet
     */
    private Mascota mapearMascota(ResultSet rs) throws SQLException {
        // --- Construir Especie ---
        Especie especie = new Especie(
            rs.getInt("id_especie"),
            rs.getString("nombre_especie")
        );
        // --- Construir Raza ---
        Raza raza = new Raza(
            rs.getInt("id_raza"),
            especie,
            rs.getString("nombre_raza")
        );
        // --- Construir Dueno ---
        EstadoRegistro estadoDueno = EstadoRegistro.valueOf(rs.getString("estado_dueno"));
        Dueno dueno = new Dueno(
            rs.getInt("id_dueno"),
            rs.getString("dni"),
            rs.getString("nombre_dueno"),
            rs.getString("apellido_dueno"),
            rs.getString("telefono_dueno"),
            rs.getString("direccion"),
            rs.getString("email_dueno"),
            estadoDueno
        );
        // --- Construir Mascota ---
        EstadoRegistro estadoMascota = EstadoRegistro.valueOf(rs.getString("estado_mascota"));
        Date fnac = rs.getDate("fecha_nacimiento");
        LocalDate fechaNac = (fnac != null) ? fnac.toLocalDate() : null;
        return new Mascota(
            rs.getInt("id_mascota"),
            dueno,
            rs.getString("nombre"),
            especie,
            raza,
            fechaNac,
            rs.getString("sexo"),
            rs.getString("color"),
            rs.getString("senas_particulares"),
            estadoMascota
        );
    }
}
