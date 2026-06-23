package com.sigvet.dao;

import com.sigvet.model.Especie;
import com.sigvet.model.Raza;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad {@link Raza}.
 *
 * <p>Gestiona el catálogo de razas animales. Cada raza pertenece a una {@link Especie},
 * lo que permite implementar el combo box en cascada Especie → Raza en la UI
 * mediante el método {@link #buscarPorEspecie(int)}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Raza
 * @see BaseDAO
 */
public class RazaDAO extends BaseDAO<Raza> {

    // =========================================================================
    // Métodos abstractos de BaseDAO
    // =========================================================================

    /**
     * Busca una raza por su identificador único, incluyendo su especie.
     *
     * @param id identificador único de la raza (PK {@code id_raza})
     * @return raza con especie cargada, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public Raza buscarPorId(int id) throws SQLException {
        String sql = "SELECT r.id_raza, r.nombre AS nombre_raza, "
                   + "       e.id_especie, e.nombre AS nombre_especie "
                   + "FROM raza r JOIN especie e ON e.id_especie = r.id_especie "
                   + "WHERE r.id_raza = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearRaza(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retorna todas las razas del catálogo con sus especies.
     *
     * @return lista de todas las razas ordenada por nombre
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public List<Raza> buscarTodos() throws SQLException {
        String sql = "SELECT r.id_raza, r.nombre AS nombre_raza, "
                   + "       e.id_especie, e.nombre AS nombre_especie "
                   + "FROM raza r JOIN especie e ON e.id_especie = r.id_especie "
                   + "ORDER BY e.nombre ASC, r.nombre ASC";
        List<Raza> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearRaza(rs));
            }
        }
        return lista;
    }

    /**
     * Inserta una nueva raza en el catálogo.
     *
     * @param r raza con especie y nombre a persistir
     * @throws SQLException si ocurre un error de restricción u otro error de BD
     */
    @Override
    public void insertar(Raza r) throws SQLException {
        String sql = "INSERT INTO raza (id_especie, nombre) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getEspecie().getIdEspecie());
            ps.setString(2, r.getNombre());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setIdRaza(keys.getInt(1));
                }
            }
        }
    }

    /**
     * Actualiza los datos de una raza existente.
     *
     * @param r raza con los datos actualizados; debe tener {@code idRaza} asignado
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    @Override
    public void actualizar(Raza r) throws SQLException {
        String sql = "UPDATE raza SET id_especie = ?, nombre = ? WHERE id_raza = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getEspecie().getIdEspecie());
            ps.setString(2, r.getNombre());
            ps.setInt(3, r.getIdRaza());
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // Métodos específicos
    // =========================================================================

    /**
     * Retorna todas las razas pertenecientes a una especie específica.
     *
     * <p>Utilizado para implementar el combo box en cascada Especie → Raza en la UI.
     * Cuando el usuario selecciona una especie, se llama a este método para cargar
     * las razas correspondientes.</p>
     *
     * @param idEspecie identificador de la especie
     * @return lista de razas de la especie, ordenada por nombre
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public List<Raza> buscarPorEspecie(int idEspecie) throws SQLException {
        String sql = "SELECT r.id_raza, r.nombre AS nombre_raza, "
                   + "       e.id_especie, e.nombre AS nombre_especie "
                   + "FROM raza r JOIN especie e ON e.id_especie = r.id_especie "
                   + "WHERE r.id_especie = ? ORDER BY r.nombre ASC";
        List<Raza> lista = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecie);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearRaza(rs));
                }
            }
        }
        return lista;
    }

    // =========================================================================
    // Método privado de mapeo
    // =========================================================================

    /**
     * Mapea una fila del {@link ResultSet} a un objeto {@link Raza} con su especie.
     *
     * @param rs ResultSet posicionado en la fila a mapear
     * @return objeto {@link Raza} con especie cargada
     * @throws SQLException si ocurre un error al leer el ResultSet
     */
    private Raza mapearRaza(ResultSet rs) throws SQLException {
        Especie especie = new Especie(rs.getInt("id_especie"), rs.getString("nombre_especie"));
        return new Raza(rs.getInt("id_raza"), especie, rs.getString("nombre_raza"));
    }
}
