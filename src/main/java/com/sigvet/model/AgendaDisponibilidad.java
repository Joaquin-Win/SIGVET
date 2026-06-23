package com.sigvet.model;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Representa la configuración de disponibilidad horaria semanal de un veterinario.
 *
 * <p>Cada registro de agenda define un bloque de tiempo recurrente en un día de la semana
 * durante el cual el veterinario está disponible para atender pacientes. A partir de estas
 * configuraciones, el sistema genera los {@link SlotAgenda} individuales (turnos de 30 minutos).</p>
 *
 * <p><strong>Reglas de negocio:</strong></p>
 * <ul>
 *   <li>RN-11: Las franjas horarias no pueden superponerse para el mismo veterinario
 *       en el mismo día. Este control lo implementa el trigger
 *       {@code trg_validar_franjas_no_superpuestas} en la BD.</li>
 * </ul>
 *
 * <p><strong>Constante de dominio:</strong> cada slot tiene una duración fija de
 * {@link #DURACION_SLOT_MINUTOS} minutos.</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code agenda_disponibilidad},
 * PK {@code id_agenda}, FK {@code id_veterinario → veterinario}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Veterinario
 * @see SlotAgenda
 */
public class AgendaDisponibilidad {

    /**
     * Duración fija de cada slot generado a partir de la agenda, en minutos.
     * Valor: {@value}.
     */
    public static final int DURACION_SLOT_MINUTOS = 30;

    /** Identificador único de la agenda (PK en BD). */
    private int idAgenda;

    /** Veterinario propietario de esta franja horaria (FK {@code id_veterinario → veterinario}). */
    private Veterinario veterinario;

    /**
     * Día de la semana para el que aplica esta franja.
     * Valores válidos: {@code "Lunes"}, {@code "Martes"}, {@code "Miercoles"},
     * {@code "Jueves"}, {@code "Viernes"}, {@code "Sabado"}, {@code "Domingo"}.
     */
    private String diaSemana;

    /** Hora de inicio de la franja horaria. */
    private LocalTime horaInicio;

    /** Hora de fin de la franja horaria. Debe ser posterior a {@link #horaInicio}. */
    private LocalTime horaFin;

    /** Valores permitidos para el atributo {@code diaSemana}. */
    private static final String[] DIAS_VALIDOS = {
        "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"
    };

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío.
     */
    public AgendaDisponibilidad() {
    }

    /**
     * Constructor completo con todos los atributos de la agenda.
     *
     * @param idAgenda    identificador único (PK en BD)
     * @param veterinario veterinario propietario; no puede ser nulo
     * @param diaSemana   día de la semana; debe ser uno de los valores válidos
     * @param horaInicio  hora de inicio de la franja
     * @param horaFin     hora de fin de la franja; debe ser posterior a horaInicio
     */
    public AgendaDisponibilidad(int idAgenda, Veterinario veterinario,
                                 String diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.idAgenda = idAgenda;
        setVeterinario(veterinario);
        setDiaSemana(diaSemana);
        setHoraInicio(horaInicio);
        setHoraFin(horaFin);
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único de la agenda.
     *
     * @return ID de la agenda
     */
    public int getIdAgenda() {
        return idAgenda;
    }

    /**
     * Establece el identificador único de la agenda.
     *
     * @param idAgenda ID; debe ser >= 0
     */
    public void setIdAgenda(int idAgenda) {
        if (idAgenda < 0) {
            throw new IllegalArgumentException("El ID de agenda no puede ser negativo.");
        }
        this.idAgenda = idAgenda;
    }

    /**
     * Retorna el veterinario propietario de esta franja.
     *
     * @return veterinario propietario
     */
    public Veterinario getVeterinario() {
        return veterinario;
    }

    /**
     * Establece el veterinario propietario. No puede ser nulo.
     *
     * @param veterinario veterinario propietario
     * @throws IllegalArgumentException si es nulo
     */
    public void setVeterinario(Veterinario veterinario) {
        if (veterinario == null) {
            throw new IllegalArgumentException("El veterinario de la agenda no puede ser nulo.");
        }
        this.veterinario = veterinario;
    }

    /**
     * Retorna el día de la semana de la franja.
     *
     * @return día de la semana
     */
    public String getDiaSemana() {
        return diaSemana;
    }

    /**
     * Establece el día de la semana. Debe ser uno de los valores admitidos.
     *
     * @param diaSemana día de la semana
     * @throws IllegalArgumentException si el valor no es un día válido
     */
    public void setDiaSemana(String diaSemana) {
        if (diaSemana == null) {
            throw new IllegalArgumentException("El día de la semana no puede ser nulo.");
        }
        boolean valido = false;
        for (String dia : DIAS_VALIDOS) {
            if (dia.equalsIgnoreCase(diaSemana.trim())) {
                valido = true;
                break;
            }
        }
        if (!valido) {
            throw new IllegalArgumentException(
                "Día de semana inválido: '" + diaSemana + "'. Use: Lunes, Martes, Miercoles, Jueves, Viernes, Sabado, Domingo.");
        }
        this.diaSemana = diaSemana.trim();
    }

    /**
     * Retorna la hora de inicio de la franja.
     *
     * @return hora de inicio
     */
    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    /**
     * Establece la hora de inicio. No puede ser nula.
     *
     * @param horaInicio hora de inicio de la franja
     * @throws IllegalArgumentException si es nula
     */
    public void setHoraInicio(LocalTime horaInicio) {
        if (horaInicio == null) {
            throw new IllegalArgumentException("La hora de inicio no puede ser nula.");
        }
        this.horaInicio = horaInicio;
    }

    /**
     * Retorna la hora de fin de la franja.
     *
     * @return hora de fin
     */
    public LocalTime getHoraFin() {
        return horaFin;
    }

    /**
     * Establece la hora de fin. Debe ser posterior a la hora de inicio.
     *
     * @param horaFin hora de fin de la franja
     * @throws IllegalArgumentException si es nula o anterior/igual a horaInicio
     */
    public void setHoraFin(LocalTime horaFin) {
        if (horaFin == null) {
            throw new IllegalArgumentException("La hora de fin no puede ser nula.");
        }
        if (horaInicio != null && !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException(
                "La hora de fin debe ser posterior a la hora de inicio.");
        }
        this.horaFin = horaFin;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto de la agenda para la UI.
     *
     * @return cadena descriptiva de la franja horaria
     */
    @Override
    public String toString() {
        String nombreVet = (veterinario != null) ? veterinario.getApellido() : "Sin veterinario";
        return nombreVet + " - " + diaSemana + " " + horaInicio + " a " + horaFin;
    }

    /**
     * Compara dos agendas por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AgendaDisponibilidad otra = (AgendaDisponibilidad) obj;
        return this.idAgenda == otra.idAgenda;
    }

    /**
     * Hash code basado en el ID de la agenda.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idAgenda);
    }
}
