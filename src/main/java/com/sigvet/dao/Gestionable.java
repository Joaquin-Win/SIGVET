package com.sigvet.dao;

import java.util.List;

/**
 * Interfaz genérica que define las operaciones básicas de gestión CRUD
 * para las entidades del sistema SIGVET.
 *
 * <p>Implementa el polimorfismo requerido por la <strong>Regla Académica 3</strong>:
 * todas las clases DAO concretas implementan esta interfaz, lo que permite
 * tratarlas de forma homogénea en la capa de servicio.</p>
 *
 * <p>Los métodos de esta interfaz corresponden a las operaciones de alta, baja,
 * modificación y consulta típicas de un sistema de gestión:</p>
 * <ul>
 *   <li>{@link #alta(Object)} — CREATE (insertar en BD)</li>
 *   <li>{@link #baja(int)} — DELETE lógico o físico</li>
 *   <li>{@link #modificar(Object)} — UPDATE en BD</li>
 *   <li>{@link #consultar(int)} — READ por ID</li>
 * </ul>
 *
 * @param <T> tipo genérico de la entidad gestionada
 *
 * @author SIGVET
 * @version 1.0
 */
public interface Gestionable<T> {

    /**
     * Da de alta (inserta) una nueva entidad en el sistema.
     *
     * @param entidad entidad a insertar
     * @throws Exception si ocurre un error durante la inserción
     */
    void alta(T entidad) throws Exception;

    /**
     * Da de baja una entidad del sistema (puede ser lógica o física según la entidad).
     *
     * @param id identificador único de la entidad a dar de baja
     * @throws Exception si ocurre un error durante la baja
     */
    void baja(int id) throws Exception;

    /**
     * Modifica (actualiza) una entidad existente en el sistema.
     *
     * @param entidad entidad con los datos actualizados
     * @throws Exception si ocurre un error durante la actualización
     */
    void modificar(T entidad) throws Exception;

    /**
     * Consulta una entidad por su identificador único.
     *
     * @param id identificador único de la entidad
     * @return la entidad encontrada, o {@code null} si no existe
     * @throws Exception si ocurre un error durante la consulta
     */
    T consultar(int id) throws Exception;

    /**
     * Retorna todos los registros de la entidad disponibles en el sistema.
     *
     * @return lista de entidades (puede estar vacía si no hay registros)
     * @throws Exception si ocurre un error durante la consulta
     */
    List<T> consultarTodos() throws Exception;
}
