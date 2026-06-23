package com.sigvet.model;

import java.util.Objects;

/**
 * Representa una raza animal asociada a una {@link Especie} en el catálogo SIGVET.
 *
 * <p>Las razas son el segundo nivel de clasificación de las mascotas. Cada raza
 * pertenece exactamente a una especie (combo en cascada Especie → Raza en la UI).</p>
 *
 * <p><strong>Composición:</strong> el atributo {@code especie} es un objeto {@link Especie}
 * completo, no un ID entero.</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code raza}, PK {@code id_raza},
 * FK {@code id_especie → especie}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Especie
 * @see Mascota
 */
public class Raza {

    /** Identificador único de la raza (PK en BD). */
    private int idRaza;

    /** Especie a la que pertenece esta raza (FK {@code id_especie → especie}). */
    private Especie especie;

    /** Nombre de la raza (ej.: "Labrador Retriever", "Siamés"). */
    private String nombre;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío.
     */
    public Raza() {
    }

    /**
     * Constructor completo.
     *
     * @param idRaza  identificador único de la raza (PK en BD)
     * @param especie especie a la que pertenece; no puede ser nula
     * @param nombre  nombre de la raza; no puede ser nulo ni vacío
     */
    public Raza(int idRaza, Especie especie, String nombre) {
        this.idRaza = idRaza;
        setEspecie(especie);
        setNombre(nombre);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único de la raza.
     *
     * @return ID de la raza
     */
    public int getIdRaza() {
        return idRaza;
    }

    /**
     * Establece el identificador único de la raza.
     *
     * @param idRaza ID de la raza; debe ser >= 0
     */
    public void setIdRaza(int idRaza) {
        if (idRaza < 0) {
            throw new IllegalArgumentException("El ID de raza no puede ser negativo.");
        }
        this.idRaza = idRaza;
    }

    /**
     * Retorna la especie a la que pertenece esta raza.
     *
     * @return especie asociada
     */
    public Especie getEspecie() {
        return especie;
    }

    /**
     * Establece la especie a la que pertenece esta raza. No puede ser nula.
     *
     * @param especie especie de la raza
     * @throws IllegalArgumentException si la especie es nula
     */
    public void setEspecie(Especie especie) {
        if (especie == null) {
            throw new IllegalArgumentException("La especie de la raza no puede ser nula.");
        }
        this.especie = especie;
    }

    /**
     * Retorna el nombre de la raza.
     *
     * @return nombre de la raza
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre de la raza. No puede ser nulo ni vacío.
     *
     * @param nombre nombre de la raza
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la raza no puede ser nulo ni vacío.");
        }
        this.nombre = nombre.trim();
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto de la raza (nombre).
     * Se usa directamente en los combo boxes de la UI.
     *
     * @return nombre de la raza
     */
    @Override
    public String toString() {
        return nombre;
    }

    /**
     * Compara dos razas por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Raza otra = (Raza) obj;
        return this.idRaza == otra.idRaza;
    }

    /**
     * Hash code basado en el ID de la raza.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idRaza);
    }
}
