package com.sigvet.model;

import com.sigvet.model.enums.EstadoRegistro;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa a una mascota registrada en el sistema SIGVET.
 *
 * <p>Una mascota está siempre asociada a un {@link Dueno} (responsable legal) y
 * clasificada por su {@link Especie} y {@link Raza}. Es la entidad central
 * alrededor de la cual giran los turnos, consultas e historial clínico.</p>
 *
 * <p><strong>Relaciones por composición:</strong> los atributos {@code dueno},
 * {@code especie} y {@code raza} son objetos completos, NO simples IDs enteros,
 * conforme a las reglas de arquitectura del sistema.</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code mascota}, PK {@code id_mascota}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Dueno
 * @see Especie
 * @see Raza
 */
public class Mascota {

    /** Identificador único de la mascota (PK en BD). */
    private int idMascota;

    /** Dueño responsable de la mascota (FK {@code id_dueno → dueno}). */
    private Dueno dueno;

    /** Nombre de la mascota. */
    private String nombre;

    /** Especie a la que pertenece la mascota (FK {@code id_especie → especie}). */
    private Especie especie;

    /** Raza de la mascota (FK {@code id_raza → raza}). */
    private Raza raza;

    /** Fecha de nacimiento; se usa para calcular la edad con {@code fn_calcular_edad_mascota}. */
    private LocalDate fechaNacimiento;

    /**
     * Sexo de la mascota. Valores posibles: {@code "M"} (macho) o {@code "F"} (hembra).
     * En la BD: columna {@code sexo ENUM('M','F')}.
     */
    private String sexo;

    /** Color o coloración del pelaje/plumaje de la mascota. */
    private String color;

    /** Señas particulares o marcas identificatorias (puede ser nulo). */
    private String senasParticulares;

    /** Estado lógico del registro de la mascota. */
    private EstadoRegistro estado;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Estado por defecto: {@link EstadoRegistro#Activo}.
     */
    public Mascota() {
        this.estado = EstadoRegistro.Activo;
    }

    /**
     * Constructor completo con todos los atributos de la mascota.
     *
     * @param idMascota         identificador único (PK en BD)
     * @param dueno             dueño responsable (no puede ser nulo)
     * @param nombre            nombre de la mascota
     * @param especie           especie de la mascota (no puede ser nulo)
     * @param raza              raza de la mascota (no puede ser nulo)
     * @param fechaNacimiento   fecha de nacimiento (puede ser nulo)
     * @param sexo              sexo: {@code "M"} o {@code "F"}
     * @param color             color del pelaje/plumaje
     * @param senasParticulares señas particulares (puede ser nulo)
     * @param estado            estado lógico del registro
     */
    public Mascota(int idMascota, Dueno dueno, String nombre, Especie especie,
                   Raza raza, LocalDate fechaNacimiento, String sexo, String color,
                   String senasParticulares, EstadoRegistro estado) {
        this.idMascota = idMascota;
        setDueno(dueno);
        setNombre(nombre);
        setEspecie(especie);
        setRaza(raza);
        this.fechaNacimiento = fechaNacimiento;
        setSexo(sexo);
        this.color = (color != null) ? color.trim() : "";
        this.senasParticulares = senasParticulares;
        this.estado = (estado != null) ? estado : EstadoRegistro.Activo;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único de la mascota.
     *
     * @return ID de la mascota
     */
    public int getIdMascota() {
        return idMascota;
    }

    /**
     * Establece el identificador único de la mascota.
     *
     * @param idMascota ID de la mascota; debe ser >= 0
     */
    public void setIdMascota(int idMascota) {
        if (idMascota < 0) {
            throw new IllegalArgumentException("El ID de mascota no puede ser negativo.");
        }
        this.idMascota = idMascota;
    }

    /**
     * Retorna el dueño responsable de la mascota.
     *
     * @return dueño de la mascota
     */
    public Dueno getDueno() {
        return dueno;
    }

    /**
     * Establece el dueño responsable. No puede ser nulo (RN-04).
     *
     * @param dueno dueño de la mascota
     * @throws IllegalArgumentException si el dueño es nulo
     */
    public void setDueno(Dueno dueno) {
        if (dueno == null) {
            throw new IllegalArgumentException("El dueño de la mascota no puede ser nulo.");
        }
        this.dueno = dueno;
    }

    /**
     * Retorna el nombre de la mascota.
     *
     * @return nombre de la mascota
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre de la mascota. No puede ser nulo ni vacío.
     *
     * @param nombre nombre de la mascota
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la mascota no puede ser nulo ni vacío.");
        }
        this.nombre = nombre.trim();
    }

    /**
     * Retorna la especie de la mascota.
     *
     * @return especie
     */
    public Especie getEspecie() {
        return especie;
    }

    /**
     * Establece la especie de la mascota. No puede ser nula.
     *
     * @param especie especie de la mascota
     * @throws IllegalArgumentException si la especie es nula
     */
    public void setEspecie(Especie especie) {
        if (especie == null) {
            throw new IllegalArgumentException("La especie de la mascota no puede ser nula.");
        }
        this.especie = especie;
    }

    /**
     * Retorna la raza de la mascota.
     *
     * @return raza
     */
    public Raza getRaza() {
        return raza;
    }

    /**
     * Establece la raza de la mascota. No puede ser nula.
     *
     * @param raza raza de la mascota
     * @throws IllegalArgumentException si la raza es nula
     */
    public void setRaza(Raza raza) {
        if (raza == null) {
            throw new IllegalArgumentException("La raza de la mascota no puede ser nula.");
        }
        this.raza = raza;
    }

    /**
     * Retorna la fecha de nacimiento de la mascota.
     *
     * @return fecha de nacimiento (puede ser nula si no se registró)
     */
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    /**
     * Establece la fecha de nacimiento de la mascota.
     *
     * @param fechaNacimiento fecha de nacimiento (puede ser nula)
     */
    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    /**
     * Retorna el sexo de la mascota.
     *
     * @return {@code "M"} para macho o {@code "F"} para hembra
     */
    public String getSexo() {
        return sexo;
    }

    /**
     * Establece el sexo de la mascota. Solo acepta {@code "M"} o {@code "F"}.
     *
     * @param sexo sexo de la mascota
     * @throws IllegalArgumentException si el valor no es {@code "M"} ni {@code "F"}
     */
    public void setSexo(String sexo) {
        if (sexo == null || (!sexo.equalsIgnoreCase("M") && !sexo.equalsIgnoreCase("F"))) {
            throw new IllegalArgumentException("El sexo de la mascota debe ser 'M' o 'F'.");
        }
        this.sexo = sexo.toUpperCase();
    }

    /**
     * Retorna el color de la mascota.
     *
     * @return color del pelaje/plumaje
     */
    public String getColor() {
        return color;
    }

    /**
     * Establece el color de la mascota.
     *
     * @param color color del pelaje/plumaje
     */
    public void setColor(String color) {
        this.color = (color != null) ? color.trim() : "";
    }

    /**
     * Retorna las señas particulares de la mascota.
     *
     * @return señas particulares (puede ser nulo)
     */
    public String getSenasParticulares() {
        return senasParticulares;
    }

    /**
     * Establece las señas particulares de la mascota.
     *
     * @param senasParticulares señas identificatorias (puede ser nulo o vacío)
     */
    public void setSenasParticulares(String senasParticulares) {
        this.senasParticulares = senasParticulares;
    }

    /**
     * Retorna el estado lógico del registro de la mascota.
     *
     * @return estado del registro
     */
    public EstadoRegistro getEstado() {
        return estado;
    }

    /**
     * Establece el estado lógico del registro de la mascota.
     *
     * @param estado estado del registro; no puede ser nulo
     * @throws IllegalArgumentException si el estado es nulo
     */
    public void setEstado(EstadoRegistro estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la mascota no puede ser nulo.");
        }
        this.estado = estado;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto de la mascota para uso en combo boxes y tablas.
     *
     * @return cadena con formato {@code "[nombre] - [especie] [raza] (Dueño: [nombre del dueño])"}
     */
    @Override
    public String toString() {
        String nombreDueno = (dueno != null) ? dueno.getNombre() + " " + dueno.getApellido() : "Sin dueño";
        String nombreEspecie = (especie != null) ? especie.getNombre() : "?";
        String nombreRaza = (raza != null) ? raza.getNombre() : "?";
        return nombre + " - " + nombreEspecie + " " + nombreRaza + " (Dueño: " + nombreDueno + ")";
    }

    /**
     * Compara dos mascotas por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Mascota otra = (Mascota) obj;
        return this.idMascota == otra.idMascota;
    }

    /**
     * Hash code basado en el ID de la mascota.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idMascota);
    }
}
