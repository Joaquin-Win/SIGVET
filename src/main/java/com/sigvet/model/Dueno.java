package com.sigvet.model;

import com.sigvet.model.enums.EstadoRegistro;

/**
 * Representa al dueño de una o más mascotas registradas en el sistema SIGVET.
 *
 * <p><strong>Herencia:</strong> extiende {@link Persona} heredando los atributos comunes
 * (nombre, apellido, teléfono, email, estado) y agrega los atributos propios:
 * {@link #dni} (identificador único del ciudadano) y {@link #direccion} (domicilio).</p>
 *
 * <p>El dueño es el responsable legal de las mascotas. El SP {@code sp_anonimizar_dueno}
 * permite anonimizar sus datos personales en cumplimiento de la Ley 25.326 de
 * Protección de Datos Personales (RN-13).</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code dueno}, PK {@code id_dueno}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Persona
 * @see Mascota
 */
public class Dueno extends Persona {

    /** Documento Nacional de Identidad. Restricción UNIQUE en la BD. */
    private String dni;

    /** Dirección postal del dueño. */
    private String direccion;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Estado por defecto: {@link EstadoRegistro#Activo}.
     */
    public Dueno() {
        super();
    }

    /**
     * Constructor completo con todos los atributos del dueño.
     *
     * @param id        identificador único (PK en BD, columna {@code id_dueno})
     * @param dni       DNI del dueño (único, NOT NULL)
     * @param nombre    nombre de pila
     * @param apellido  apellido
     * @param telefono  teléfono de contacto
     * @param direccion dirección postal
     * @param email     correo electrónico (puede ser nulo o vacío)
     * @param estado    estado lógico del registro
     */
    public Dueno(int id, String dni, String nombre, String apellido,
                 String telefono, String direccion,
                 String email, EstadoRegistro estado) {
        super(id, nombre, apellido, telefono, email, estado);
        setDni(dni);
        this.direccion = (direccion != null) ? direccion.trim() : "";
    }

    /**
     * Constructor parcial sin email. Estado {@link EstadoRegistro#Activo} por defecto.
     *
     * @param id        identificador único
     * @param dni       DNI del dueño
     * @param nombre    nombre de pila
     * @param apellido  apellido
     * @param telefono  teléfono de contacto
     * @param direccion dirección postal
     */
    public Dueno(int id, String dni, String nombre, String apellido,
                 String telefono, String direccion) {
        this(id, dni, nombre, apellido, telefono, direccion, "", EstadoRegistro.Activo);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el DNI del dueño.
     *
     * @return documento nacional de identidad
     */
    public String getDni() {
        return dni;
    }

    /**
     * Establece el DNI del dueño. No puede ser nulo ni vacío.
     *
     * @param dni documento nacional de identidad
     * @throws IllegalArgumentException si el DNI es nulo o vacío
     */
    public void setDni(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI del dueño no puede ser nulo ni vacío.");
        }
        this.dni = dni.trim();
    }

    /**
     * Retorna la dirección postal del dueño.
     *
     * @return dirección postal
     */
    public String getDireccion() {
        return direccion;
    }

    /**
     * Establece la dirección postal del dueño.
     *
     * @param direccion dirección postal (puede ser nulo o vacío)
     */
    public void setDireccion(String direccion) {
        this.direccion = (direccion != null) ? direccion.trim() : "";
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del dueño para uso en combo boxes y tablas de la UI.
     *
     * @return cadena con formato {@code "[Apellido], [Nombre] - DNI: [dni]"}
     */
    @Override
    public String toString() {
        return getApellido() + ", " + getNombre() + " - DNI: " + dni;
    }

    /**
     * Compara dos dueños por su ID (identidad de dominio).
     *
     * @param obj objeto a comparar
     * @return {@code true} si ambos objetos son {@link Dueno} y tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Dueno otro = (Dueno) obj;
        return this.getId() == otro.getId();
    }

    /**
     * Código hash basado en el ID del dueño.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId());
    }
}
