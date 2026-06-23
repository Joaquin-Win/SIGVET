package com.sigvet.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) para mostrar el historial clínico completo de una mascota.
 *
 * <p>Agrupa en un único objeto plano la información de múltiples tablas que retorna la
 * vista {@code vw_historial_clinico}, evitando la necesidad de instanciar objetos complejos
 * con relaciones anidadas para la capa de presentación.</p>
 *
 * <p>Cada instancia de este DTO corresponde a una fila de la vista y puede incluir
 * varios medicamentos por consulta (una fila por medicamento; si la consulta no tiene
 * medicamentos, el medicamento se muestra como nulo).</p>
 *
 * <p><strong>Uso:</strong> CU-07 Historial Clínico — tabla de historial en
 * {@code HistorialClinicoDialog}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see com.sigvet.dao.HistorialDAO
 * @see com.sigvet.service.HistorialService
 */
public class HistorialClinicoDTO {

    /** Identificador de la consulta médica. */
    private int idConsulta;

    /** Fecha y hora de la consulta. */
    private LocalDateTime fechaConsulta;

    /** Nombre de la mascota paciente. */
    private String nombreMascota;

    /** Combinación de especie y raza (ej.: "Canino Labrador Retriever"). */
    private String especieRaza;

    /** Nombre completo del dueño (apellido + nombre). */
    private String nombreDueno;

    /** Nombre completo del veterinario actuante (apellido + nombre). */
    private String nombreVeterinario;

    /** Síntomas reportados en la consulta. */
    private String sintomas;

    /** Diagnóstico del veterinario. */
    private String diagnostico;

    /** Nombre comercial del medicamento recetado (puede ser nulo si no hay receta). */
    private String medicamento;

    /** Descripción de la dosis recetada (puede ser nulo). */
    private String dosis;

    /** Frecuencia de administración (puede ser nulo). */
    private String frecuencia;

    /** Duración del tratamiento (puede ser nulo). */
    private String duracion;

    /** Número de lote del medicamento dispensado (puede ser nulo). */
    private String lote;

    /** Fecha de vencimiento del lote dispensado (puede ser nulo). */
    private LocalDate fechaVencimiento;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío para uso por DAOs al mapear ResultSets.
     */
    public HistorialClinicoDTO() {
    }

    /**
     * Constructor completo con todos los campos del historial clínico.
     *
     * @param idConsulta       identificador de la consulta médica
     * @param fechaConsulta    fecha y hora de la consulta
     * @param nombreMascota    nombre de la mascota
     * @param especieRaza      especie y raza combinadas
     * @param nombreDueno      nombre completo del dueño
     * @param nombreVeterinario nombre completo del veterinario
     * @param sintomas         síntomas reportados
     * @param diagnostico      diagnóstico
     * @param medicamento      nombre comercial del medicamento (nullable)
     * @param dosis            dosis recetada (nullable)
     * @param frecuencia       frecuencia de administración (nullable)
     * @param duracion         duración del tratamiento (nullable)
     * @param lote             número de lote (nullable)
     * @param fechaVencimiento fecha de vencimiento del lote (nullable)
     */
    public HistorialClinicoDTO(int idConsulta, LocalDateTime fechaConsulta,
                                String nombreMascota, String especieRaza,
                                String nombreDueno, String nombreVeterinario,
                                String sintomas, String diagnostico,
                                String medicamento, String dosis,
                                String frecuencia, String duracion,
                                String lote, LocalDate fechaVencimiento) {
        this.idConsulta = idConsulta;
        this.fechaConsulta = fechaConsulta;
        this.nombreMascota = nombreMascota;
        this.especieRaza = especieRaza;
        this.nombreDueno = nombreDueno;
        this.nombreVeterinario = nombreVeterinario;
        this.sintomas = sintomas;
        this.diagnostico = diagnostico;
        this.medicamento = medicamento;
        this.dosis = dosis;
        this.frecuencia = frecuencia;
        this.duracion = duracion;
        this.lote = lote;
        this.fechaVencimiento = fechaVencimiento;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return identificador de la consulta médica */
    public int getIdConsulta() { return idConsulta; }

    /** @param idConsulta identificador de la consulta */
    public void setIdConsulta(int idConsulta) { this.idConsulta = idConsulta; }

    /** @return fecha y hora de la consulta */
    public LocalDateTime getFechaConsulta() { return fechaConsulta; }

    /** @param fechaConsulta fecha y hora de la consulta */
    public void setFechaConsulta(LocalDateTime fechaConsulta) { this.fechaConsulta = fechaConsulta; }

    /** @return nombre de la mascota */
    public String getNombreMascota() { return nombreMascota; }

    /** @param nombreMascota nombre de la mascota */
    public void setNombreMascota(String nombreMascota) { this.nombreMascota = nombreMascota; }

    /** @return especie y raza combinadas */
    public String getEspecieRaza() { return especieRaza; }

    /** @param especieRaza especie y raza */
    public void setEspecieRaza(String especieRaza) { this.especieRaza = especieRaza; }

    /** @return nombre completo del dueño */
    public String getNombreDueno() { return nombreDueno; }

    /** @param nombreDueno nombre completo del dueño */
    public void setNombreDueno(String nombreDueno) { this.nombreDueno = nombreDueno; }

    /** @return nombre completo del veterinario */
    public String getNombreVeterinario() { return nombreVeterinario; }

    /** @param nombreVeterinario nombre del veterinario */
    public void setNombreVeterinario(String nombreVeterinario) { this.nombreVeterinario = nombreVeterinario; }

    /** @return síntomas reportados */
    public String getSintomas() { return sintomas; }

    /** @param sintomas síntomas reportados */
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }

    /** @return diagnóstico */
    public String getDiagnostico() { return diagnostico; }

    /** @param diagnostico diagnóstico del veterinario */
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    /** @return nombre comercial del medicamento (nullable) */
    public String getMedicamento() { return medicamento; }

    /** @param medicamento nombre comercial del medicamento */
    public void setMedicamento(String medicamento) { this.medicamento = medicamento; }

    /** @return dosis recetada (nullable) */
    public String getDosis() { return dosis; }

    /** @param dosis dosis recetada */
    public void setDosis(String dosis) { this.dosis = dosis; }

    /** @return frecuencia de administración (nullable) */
    public String getFrecuencia() { return frecuencia; }

    /** @param frecuencia frecuencia de administración */
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }

    /** @return duración del tratamiento (nullable) */
    public String getDuracion() { return duracion; }

    /** @param duracion duración del tratamiento */
    public void setDuracion(String duracion) { this.duracion = duracion; }

    /** @return número de lote (nullable) */
    public String getLote() { return lote; }

    /** @param lote número de lote */
    public void setLote(String lote) { this.lote = lote; }

    /** @return fecha de vencimiento del lote (nullable) */
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }

    /** @param fechaVencimiento fecha de vencimiento del lote */
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del DTO para debugging.
     *
     * @return resumen del historial clínico
     */
    @Override
    public String toString() {
        return "HistorialClinicoDTO{consulta=" + idConsulta
            + ", fecha=" + (fechaConsulta != null ? fechaConsulta.toLocalDate() : "null")
            + ", mascota=" + nombreMascota
            + ", medicamento=" + (medicamento != null ? medicamento : "Sin medicamento")
            + "}";
    }
}
