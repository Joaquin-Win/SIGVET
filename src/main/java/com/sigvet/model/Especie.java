package com.sigvet.model;

import java.util.Objects;

/**
 * Representa una especie animal registrada en el catálogo del sistema SIGVET.
 *
 * <p>Las especies son los clasificadores de primer nivel de las mascotas
 * (ej.: Canino, Felino, Aviario, Reptil, etc.). Cada especie puede tener
 * múltiples {@link Raza} asociadas.</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code especie}, PK {@code id_especie}.
 * Tabla de catálogo: no se modifica en tiempo de ejecución por la aplicación Java.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Raza
 * @see Mascota
 */
public class Especie {

    /** Identificador único de la especie (PK en BD). */
    private int idEspecie;

    /** Nombre de la especie (ej.: "Canino", "Felino"). */
    private String nombre;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío.
     */
    public Especie() {
    }

    /**
     * Constructor completo.
     *
     * @param idEspecie identificador único de la especie (PK en BD)
     * @param nombre    nombre de la especie; no puede ser nulo ni vacío
     */
    public Especie(int idEspecie, String nombre) {
        this.idEspecie = idEspecie;
        setNombre(nombre);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único de la especie.
     *
     * @return ID de la especie
     */
    public int getIdEspecie() {
        return idEspecie;
    }

    /**
     * Establece el identificador único de la especie.
     *
     * @param idEspecie ID de la especie; debe ser >= 0
     */
    public void setIdEspecie(int idEspecie) {
        if (idEspecie < 0) {
            throw new IllegalArgumentException("El ID de especie no puede ser negativo.");
        }
        this.idEspecie = idEspecie;
    }

    /**
     * Retorna el nombre de la especie.
     *
     * @return nombre de la especie
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre de la especie. No puede ser nulo ni vacío.
     *
     * @param nombre nombre de la especie
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la especie no puede ser nulo ni vacío.");
        }
        this.nombre = nombre.trim();
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto de la especie (nombre).
     * Se usa directamente en los combo boxes de la UI.
     *
     * @return nombre de la especie
     */
    @Override
    public String toString() {
        return nombre;
    }

    /**
     * Compara dos especies por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Especie otra = (Especie) obj;
        return this.idEspecie == otra.idEspecie;
    }

    /**
     * Hash code basado en el ID de la especie.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idEspecie);
    }
}
