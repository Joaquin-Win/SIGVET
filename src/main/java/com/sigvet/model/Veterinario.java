package com.sigvet.model;

import com.sigvet.model.enums.EstadoRegistro;

/**
 * Representa a un veterinario del centro clínico en el sistema SIGVET.
 *
 * <p><strong>Herencia:</strong> extiende {@link Persona} heredando los atributos comunes
 * (nombre, apellido, teléfono, email, estado) y agrega el atributo propio {@link #matricula},
 * que identifica unívocamente a cada profesional habilitado.</p>
 *
 * <p>El veterinario es el actor principal de los casos de uso CU-01 (Configurar Agenda),
 * CU-03 (Registrar Consulta Médica) y CU-07 (Historial Clínico).</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code veterinario}, PK {@code id_veterinario}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Persona
 */
public class Veterinario extends Persona {

    /** Número de matrícula profesional único. Restricción UNIQUE en la BD. */
    private String matricula;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Estado por defecto: {@link EstadoRegistro#Activo}.
     */
    public Veterinario() {
        super();
    }

    /**
     * Constructor completo con todos los atributos del veterinario.
     *
     * @param id       identificador único (PK en BD, columna {@code id_veterinario})
     * @param nombre   nombre de pila del veterinario
     * @param apellido apellido del veterinario
     * @param matricula número de matrícula profesional (único, NOT NULL)
     * @param telefono teléfono de contacto
     * @param email    correo electrónico (puede ser nulo o vacío)
     * @param estado   estado lógico del registro
     */
    public Veterinario(int id, String nombre, String apellido,
                       String matricula, String telefono,
                       String email, EstadoRegistro estado) {
        super(id, nombre, apellido, telefono, email, estado);
        setMatricula(matricula);
    }

    /**
     * Constructor parcial sin email y con estado {@link EstadoRegistro#Activo} por defecto.
     *
     * @param id        identificador único
     * @param nombre    nombre de pila
     * @param apellido  apellido
     * @param matricula número de matrícula profesional
     * @param telefono  teléfono de contacto
     */
    public Veterinario(int id, String nombre, String apellido,
                       String matricula, String telefono) {
        this(id, nombre, apellido, matricula, telefono, "", EstadoRegistro.Activo);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el número de matrícula profesional del veterinario.
     *
     * @return matrícula profesional
     */
    public String getMatricula() {
        return matricula;
    }

    /**
     * Establece el número de matrícula profesional. No puede ser nulo ni vacío.
     *
     * @param matricula número de matrícula
     * @throws IllegalArgumentException si la matrícula es nula o vacía
     */
    public void setMatricula(String matricula) {
        if (matricula == null || matricula.trim().isEmpty()) {
            throw new IllegalArgumentException("La matrícula del veterinario no puede ser nula ni vacía.");
        }
        this.matricula = matricula.trim();
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del veterinario para uso en combo boxes y tablas de la UI.
     *
     * @return cadena con formato {@code "Dr. [Apellido], [Nombre] - Matrícula: [matricula]"}
     */
    @Override
    public String toString() {
        return "Dr. " + getApellido() + ", " + getNombre() + " - Matrícula: " + matricula;
    }

    /**
     * Compara dos veterinarios por su ID (identidad de dominio).
     *
     * @param obj objeto a comparar
     * @return {@code true} si ambos objetos son {@link Veterinario} y tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Veterinario otro = (Veterinario) obj;
        return this.getId() == otro.getId();
    }

    /**
     * Código hash basado en el ID del veterinario.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId());
    }
}
