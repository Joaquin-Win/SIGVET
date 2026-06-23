package com.sigvet.dao;

import com.sigvet.model.Especie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Especie}.
 *
 * <p>Gestiona el catálogo de especies animales. Las especies son datos de
 * catálogo que raramente cambian; se usan principalmente para poblar
 * el combo box de especie en la pantalla de gestión de mascotas.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Especie
 * @see BaseDAO
 */
public class EspecieDAO extends BaseDAO<Especie> {

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca una especie por su identificador único.
     *
     * @param id identificador único de la especie (PK {@code id_especie})
     * @return especie encontrada, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Especie buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_especie, nombre FROM especie WHERE id_especie = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearEspecie(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todas las especies del catálogo, ordenadas por nombre.
     *
     * @return lista de todas las especies
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Especie> buscarTodos() throws SQLException {
        String sql = "SELECT id_especie, nombre FROM especie ORDER BY nombre ASC";
        List<Especie> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearEspecie(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta una nueva especie en el catálogo.
     *
     * @param e especie con el nombre a persistir
     * @throws SQLException si el nombre ya existe (violación de UNIQUE) u otro error
     */
    @Override
    public void insertar(Especie e) throws SQLException {
        String sql = "INSERT INTO especie (nombre) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNombre());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setIdEspecie(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza el nombre de una especie existente.
     *
     * @param e especie con el nombre actualizado; debe tener {@code idEspecie} asignado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Especie e) throws SQLException {
        String sql = "UPDATE especie SET nombre = ? WHERE id_especie = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getNombre());
            ps.setInt(2, e.getIdEspecie());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Busca una especie por su nombre exacto.
     *
     * @param nombre nombre de la especie a buscar
     * @return especie encontrada, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Especie buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT id_especie, nombre FROM especie WHERE nombre = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearEspecie(rs);
                }
            }
        }
        return null;
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Especie}.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Especie}
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private Especie mapearEspecie(ResultSet rs) throws SQLException {
        return new Especie(rs.getInt("id_especie"), rs.getString("nombre"));
    }
}
