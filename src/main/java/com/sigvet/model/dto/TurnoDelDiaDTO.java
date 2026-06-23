package com.sigvet.model.dto;

import com.sigvet.model.enums.EstadoTurno;
import java.time.LocalTime;

/**
 * Data Transfer Object (DTO) para mostrar la agenda del día en la interfaz de usuario.
 *
 * <p>Agrupa en un único objeto plano la información de múltiples tablas que retorna la
 * vista {@code vw_turnos_del_dia}, optimizada para el caso de uso CU-08 Agenda del Día.</p>
 *
 * <p>Cada instancia corresponde a un turno del día actual con toda la información
 * relevante para la recepcionista: hora, mascota, dueño, veterinario, estado y motivo.</p>
 *
 * <p><strong>Uso:</strong> CU-08 Agenda del Día — tabla de turnos en
 * {@code AgendaDelDiaDialog}. Se carga consultando la vista {@code vw_turnos_del_dia}
 * que filtra solo los turnos del día actual ({@code sa.fecha = CURDATE()}).</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.dao.AgendaDiaDAO
 * @see com.sigvet.service.AgendaDiaService
 */
public class TurnoDelDiaDTO {

    /** Identificador del turno. */
    private int idTurno;

    /** Hora del slot del turno. */
    private LocalTime hora;

    /** Nombre de la mascota. */
    private String nombreMascota;

    /** Nombre de la especie (ej.: "Canino"). */
    private String especie;

    /** Nombre de la raza (ej.: "Labrador"). */
    private String raza;

    /** Apellido y nombre del dueño. */
    private String nombreDueno;

    /** Teléfono de contacto del dueño. */
    private String telefonoDueno;

    /** Apellido y nombre del veterinario asignado. */
    private String nombreVeterinario;

    /** Estado actual del turno. */
    private EstadoTurno estado;

    /** Motivo del turno (puede ser nulo). */
    private String motivo;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío para uso por DAOs al mapear ResultSets.
     */
    public TurnoDelDiaDTO() {
    }

    /**
     * Constructor completo con todos los campos del DTO.
     *
     * @param idTurno          identificador del turno
     * @param hora             hora del slot
     * @param nombreMascota    nombre de la mascota
     * @param especie          nombre de la especie
     * @param raza             nombre de la raza
     * @param nombreDueno      apellido y nombre del dueño
     * @param telefonoDueno    teléfono del dueño
     * @param nombreVeterinario apellido y nombre del veterinario
     * @param estado           estado del turno
     * @param motivo           motivo del turno (nullable)
     */
    public TurnoDelDiaDTO(int idTurno, LocalTime hora, String nombreMascota,
                           String especie, String raza,
                           String nombreDueno, String telefonoDueno,
                           String nombreVeterinario, EstadoTurno estado, String motivo) {
        this.idTurno = idTurno;
        this.hora = hora;
        this.nombreMascota = nombreMascota;
        this.especie = especie;
        this.raza = raza;
        this.nombreDueno = nombreDueno;
        this.telefonoDueno = telefonoDueno;
        this.nombreVeterinario = nombreVeterinario;
        this.estado = estado;
        this.motivo = motivo;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return identificador del turno */
    public int getIdTurno() { return idTurno; }

    /** @param idTurno identificador del turno */
    public void setIdTurno(int idTurno) { this.idTurno = idTurno; }

    /** @return hora del slot */
    public LocalTime getHora() { return hora; }

    /** @param hora hora del slot */
    public void setHora(LocalTime hora) { this.hora = hora; }

    /** @return nombre de la mascota */
    public String getNombreMascota() { return nombreMascota; }

    /** @param nombreMascota nombre de la mascota */
    public void setNombreMascota(String nombreMascota) { this.nombreMascota = nombreMascota; }

    /** @return nombre de la especie */
    public String getEspecie() { return especie; }

    /** @param especie nombre de la especie */
    public void setEspecie(String especie) { this.especie = especie; }

    /** @return nombre de la raza */
    public String getRaza() { return raza; }

    /** @param raza nombre de la raza */
    public void setRaza(String raza) { this.raza = raza; }

    /** @return apellido y nombre del dueño */
    public String getNombreDueno() { return nombreDueno; }

    /** @param nombreDueno apellido y nombre del dueño */
    public void setNombreDueno(String nombreDueno) { this.nombreDueno = nombreDueno; }

    /** @return teléfono del dueño */
    public String getTelefonoDueno() { return telefonoDueno; }

    /** @param telefonoDueno teléfono del dueño */
    public void setTelefonoDueno(String telefonoDueno) { this.telefonoDueno = telefonoDueno; }

    /** @return apellido y nombre del veterinario */
    public String getNombreVeterinario() { return nombreVeterinario; }

    /** @param nombreVeterinario apellido y nombre del veterinario */
    public void setNombreVeterinario(String nombreVeterinario) { this.nombreVeterinario = nombreVeterinario; }

    /** @return estado del turno */
    public EstadoTurno getEstado() { return estado; }

    /** @param estado estado del turno */
    public void setEstado(EstadoTurno estado) { this.estado = estado; }

    /** @return motivo del turno (nullable) */
    public String getMotivo() { return motivo; }

    /** @param motivo motivo del turno */
    public void setMotivo(String motivo) { this.motivo = motivo; }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del DTO para la tabla de la agenda del día.
     *
     * @return resumen del turno del día
     */
    @Override
    public String toString() {
        return hora + " | " + nombreMascota + " (" + especie + ") | "
            + nombreDueno + " | " + nombreVeterinario + " [" + estado + "]";
    }
}
