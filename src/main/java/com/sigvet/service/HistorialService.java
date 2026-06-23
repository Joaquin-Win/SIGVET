package com.sigvet.service;

import com.sigvet.dao.ConsultaMedicaDAO;
import com.sigvet.dao.MascotaDAO;
import com.sigvet.model.Mascota;
import com.sigvet.model.dto.HistorialClinicoDTO;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Servicio de lógica de negocio para el caso de uso CU-07: Consultar Historial Clínico.
 *
 * <p>Provee acceso al historial clínico completo de una mascota, incluyendo consultas,
 * síntomas, diagnósticos, medicamentos recetados, lotes y vencimientos.</p>
 *
 * <p>El historial se construye mediante un JOIN multi-tabla en la capa DAO que
 * consulta directamente las tablas de {@code consulta_medica}, {@code item_receta},
 * {@code stock} y {@code medicamento}.</p>
 *
 * <p>También expone el cálculo de edad de mascotas mediante la función escalar
 * {@code fn_calcular_edad_mascota} de la BD.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see ConsultaMedicaDAO
 * @see MascotaDAO
 */
public class HistorialService {

    /** DAO para la consulta del historial clínico. */
    private final ConsultaMedicaDAO consultaDAO;

    /** DAO para la búsqueda de mascotas. */
    private final MascotaDAO mascotaDAO;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Constructor que instancia los DAOs necesarios para el historial clínico.
     */
    public HistorialService() {
        this.consultaDAO = new ConsultaMedicaDAO();
        this.mascotaDAO  = new MascotaDAO();
    }

    // =========================================================================
    // Métodos de negocio
    // =========================================================================

    /**
     * Retorna el historial clínico completo de una mascota.
     *
     * <p>Cada {@link HistorialClinicoDTO} representa una fila del resultado
     * (puede haber múltiples filas por consulta si tiene varios medicamentos).</p>
     *
     * <p>Solo incluye consultas con estado {@code Activa}.</p>
     *
     * @param idMascota identificador de la mascota
     * @return lista de filas del historial, ordenada por fecha de consulta descendente
     * @throws IllegalArgumentException si el ID de mascota es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<HistorialClinicoDTO> obtenerHistorialCompleto(int idMascota)
            throws IllegalArgumentException, SQLException {
        if (idMascota <= 0) {
            throw new IllegalArgumentException("Debe seleccionar una mascota válida.");
        }
        return consultaDAO.buscarHistorialClinico(idMascota);
    }

    /**
     * Busca una mascota por su ID para mostrar sus datos en el encabezado del historial.
     *
     * @param idMascota identificador de la mascota
     * @return mascota encontrada con dueño, especie y raza completos, o {@code null}
     * @throws SQLException si ocurre un error de acceso a la BD
     */
    public Mascota buscarMascota(int idMascota) throws SQLException {
        return mascotaDAO.buscarPorId(idMascota);
    }

    /**
     * Busca mascotas cuyo nombre comience con el texto dado.
     *
     * <p>Utilizado para el campo de búsqueda rápida en la pantalla de historial:
     * el operador escribe el nombre y se muestra una lista de coincidencias.</p>
     *
     * @param nombre texto de búsqueda (se busca por prefijo)
     * @return lista de mascotas que coinciden con el nombre
     * @throws IllegalArgumentException si el texto de búsqueda está vacío
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public List<Mascota> buscarMascotas(String nombre)
            throws IllegalArgumentException, SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingrese al menos un carácter para buscar.");
        }
        return mascotaDAO.buscarPorNombre(nombre.trim());
    }

    /**
     * Calcula la edad de una mascota en años llamando a la función escalar
     * {@code fn_calcular_edad_mascota(id_mascota)} de la BD.
     *
     * <p>La función devuelve la diferencia en años entre la fecha de nacimiento
     * y la fecha actual (retorna {@code NULL} si la mascota no tiene fecha de
     * nacimiento registrada, en cuyo caso se retorna {@code -1}).</p>
     *
     * <p>Se usa el patrón JDBC {@code {? = CALL fn_calcular_edad_mascota(?)}}
     * registrando el primer parámetro como OUT con {@link Types#INTEGER}.</p>
     *
     * @param idMascota identificador de la mascota
     * @return edad en años, o {@code -1} si la fecha de nacimiento no está registrada
     * @throws IllegalArgumentException si el ID es inválido
     * @throws SQLException             si ocurre un error de acceso a la BD
     */
    public int calcularEdadMascota(int idMascota)
            throws IllegalArgumentException, SQLException {
        if (idMascota <= 0) {
            throw new IllegalArgumentException("ID de mascota inválido.");
        }

        // Llamada a la función escalar de la BD (ConexionBD es clase de utilidad estática)
        try (Connection conn = com.sigvet.util.ConexionBD.getConexion();
             CallableStatement cs = conn.prepareCall(
                     "{? = CALL fn_calcular_edad_mascota(?)}")) {
            cs.registerOutParameter(1, Types.INTEGER); // OUT: edad en años
            cs.setInt(2, idMascota);
            cs.execute();
            int edad = cs.getInt(1);
            if (cs.wasNull()) {
                return -1; // Sin fecha de nacimiento registrada
            }
            return edad;
        }
    }
}
