package com.sigvet.dao;

import com.sigvet.util.ConexionBD;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Clase abstracta genérica que forma la base de todos los Data Access Objects (DAOs)
 * del sistema SIGVET.
 *
 * <p>Implementa el patrón <strong>Template Method</strong> y la <strong>Abstracción</strong>
 * requerida por la Regla Académica 3: centraliza la lógica de obtención de conexión
 * y define los métodos abstractos que cada DAO concreto debe implementar.</p>
 *
 * <p><strong>Herencia:</strong> todos los DAOs concretos extienden esta clase,
 * heredando el método {@link #getConnection()} y los métodos abstractos del contrato CRUD.</p>
 *
 * <p><strong>Importante:</strong> las conexiones se obtienen mediante
 * {@link ConexionBD#getConexion()} y SIEMPRE deben usarse con {@code try-with-resources}
 * en los métodos de los DAOs concretos para garantizar el cierre automático.</p>
 *
 * @param <T> tipo genérico de la entidad gestionada por el DAO
 *
 * @author SIGVET
 * @version 1.0
 * @see ConexionBD
 * @see Gestionable
 */
public abstract class BaseDAO<T> {

    // =========================================================================
    // Métodos de infraestructura
    // =========================================================================

    /**
     * Obtiene una nueva conexión JDBC a la base de datos {@code sigvet}.
     *
     * <p>Las subclases usan este método para obtener la conexión en sus operaciones.
     * SIEMPRE debe usarse dentro de un bloque {@code try-with-resources}.</p>
     *
     * @return nueva conexión JDBC a la BD
     * @throws SQLException si no se puede establecer la conexión
     */
    protected Connection getConnection() throws SQLException {
        return ConexionBD.getConexion();
    }

    // =========================================================================
    // Métodos abstractos — Contrato CRUD
    // =========================================================================

    /**
     * Busca y retorna una entidad por su identificador único.
     *
     * <p>Los DAOs concretos implementan este método con la consulta SQL específica
     * para su tabla y el mapeo {@code ResultSet → Objeto}.</p>
     *
     * @param id identificador único de la entidad (PK en BD)
     * @return entidad encontrada, o {@code null} si no existe
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public abstract T buscarPorId(int id) throws SQLException;

    /**
     * Retorna todos los registros de la entidad disponibles en la BD.
     *
     * @return lista con todas las entidades (puede estar vacía)
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public abstract List<T> buscarTodos() throws SQLException;

    /**
     * Inserta una nueva entidad en la base de datos.
     *
     * @param entidad entidad con los datos a persistir
     * @throws SQLException si ocurre un error de acceso a la BD o violación de restricciones
     */
    public abstract void insertar(T entidad) throws SQLException;

    /**
     * Actualiza los datos de una entidad existente en la base de datos.
     *
     * @param entidad entidad con los datos actualizados (debe tener el ID asignado)
     * @throws SQLException si ocurre un error de acceso a la BD o la entidad no existe
     */
    public abstract void actualizar(T entidad) throws SQLException;
}
