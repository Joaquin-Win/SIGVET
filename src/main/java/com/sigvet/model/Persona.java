package com.sigvet.model;

import com.sigvet.model.enums.EstadoRegistro;
import java.util.Objects;

/**
 * Clase abstracta base que representa a una persona en el sistema SIGVET.
 *
 * <p>Encapsula los atributos comunes a todas las personas del sistema (veterinarios y dueños).
 * Las subclases {@link Veterinario} y {@link Dueno} extienden esta clase agregando sus
 * atributos específicos mediante el mecanismo de herencia de Java.</p>
 *
 * <p><strong>Encapsulamiento:</strong> todos los atributos son {@code private}; el acceso
 * externo se realiza exclusivamente a través de los métodos públicos (getters/setters).
 * Los setters validan los datos antes de asignarlos.</p>
 *
 * <p><strong>Abstracción:</strong> {@code toString()} es abstracto porque cada subclase
 * tiene su propia representación de texto significativa para la UI.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Veterinario
 * @see Dueno
 */
public abstract class Persona {

    /** Identificador único del registro en la base de datos. */
    private int id;

    /** Nombre de pila de la persona. */
    private String nombre;

    /** Apellido de la persona. */
    private String apellido;

    /** Número de teléfono de contacto. */
    private String telefono;

    /** Dirección de correo electrónico. Puede ser nulo o vacío. */
    private String email;

    /** Estado lógico del registro en el sistema ({@code Activo} o {@code Inactivo}). */
    private EstadoRegistro estado;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Inicializa el estado como {@link EstadoRegistro#Activo} por defecto.
     */
    public Persona() {
        this.estado = EstadoRegistro.Activo;
    }

    /**
     * Constructor completo con todos los atributos.
     *
     * @param id       identificador único de la persona (PK en la BD)
     * @param nombre   nombre de pila; no puede ser nulo ni vacío
     * @param apellido apellido; no puede ser nulo ni vacío
     * @param telefono número de teléfono de contacto
     * @param email    dirección de correo electrónico (puede ser nulo)
     * @param estado   estado lógico del registro
     */
    public Persona(int id, String nombre, String apellido,
                   String telefono, String email, EstadoRegistro estado) {
        this.id = id;
        setNombre(nombre);
        setApellido(apellido);
        this.telefono = telefono;
        this.email = email;
        this.estado = (estado != null) ? estado : EstadoRegistro.Activo;
    }

    /**
     * Constructor parcial sin email. El email se inicializa como cadena vacía.
     *
     * @param id       identificador único de la persona
     * @param nombre   nombre de pila; no puede ser nulo ni vacío
     * @param apellido apellido; no puede ser nulo ni vacío
     * @param telefono número de teléfono de contacto
     * @param estado   estado lógico del registro
     */
    public Persona(int id, String nombre, String apellido,
                   String telefono, EstadoRegistro estado) {
        this(id, nombre, apellido, telefono, "", estado);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único de la persona.
     *
     * @return id de la persona (PK en BD)
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único de la persona.
     *
     * @param id identificador; debe ser mayor o igual a 0
     */
    public void setId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo.");
        }
        this.id = id;
    }

    /**
     * Retorna el nombre de pila de la persona.
     *
     * @return nombre de pila
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre de pila. No puede ser nulo ni vacío.
     *
     * @param nombre nombre de pila
     * @throws IllegalArgumentException si el nombre es nulo o vacío
     */
    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacío.");
        }
        this.nombre = nombre.trim();
    }

    /**
     * Retorna el apellido de la persona.
     *
     * @return apellido
     */
    public String getApellido() {
        return apellido;
    }

    /**
     * Establece el apellido. No puede ser nulo ni vacío.
     *
     * @param apellido apellido de la persona
     * @throws IllegalArgumentException si el apellido es nulo o vacío
     */
    public void setApellido(String apellido) {
        if (apellido == null || apellido.trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido no puede ser nulo ni vacío.");
        }
        this.apellido = apellido.trim();
    }

    /**
     * Retorna el teléfono de contacto.
     *
     * @return teléfono
     */
    public String getTelefono() {
        return telefono;
    }

    /**
     * Establece el teléfono de contacto.
     *
     * @param telefono número de teléfono
     */
    public void setTelefono(String telefono) {
        this.telefono = (telefono != null) ? telefono.trim() : "";
    }

    /**
     * Retorna el email de la persona.
     *
     * @return dirección de correo electrónico
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el email de la persona.
     *
     * @param email dirección de correo electrónico (puede ser nulo o vacío)
     */
    public void setEmail(String email) {
        this.email = (email != null) ? email.trim() : "";
    }

    /**
     * Retorna el estado lógico del registro.
     *
     * @return {@link EstadoRegistro#Activo} o {@link EstadoRegistro#Inactivo}
     */
    public EstadoRegistro getEstado() {
        return estado;
    }

    /**
     * Establece el estado lógico del registro.
     *
     * @param estado estado a asignar; no puede ser nulo
     * @throws IllegalArgumentException si el estado es nulo
     */
    public void setEstado(EstadoRegistro estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo.");
        }
        this.estado = estado;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto de la persona. Implementada por cada subclase.
     *
     * @return representación legible de la persona
     */
    @Override
    public abstract String toString();

    /**
     * Compara dos personas por su ID (identidad de dominio).
     *
     * @param obj objeto a comparar
     * @return {@code true} si ambos objetos son de la misma clase y tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Persona otra = (Persona) obj;
        return this.id == otra.id;
    }

    /**
     * Código hash basado en el ID de la persona.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
