package com.sigvet.model;

import com.sigvet.model.enums.EstadoSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Representa un slot (franja horaria individual) generado a partir de una
 * {@link AgendaDisponibilidad}.
 *
 * <p>Cada slot corresponde a un turno de {@link AgendaDisponibilidad#DURACION_SLOT_MINUTOS}
 * minutos en una fecha y hora específicas. Cuando se reserva un turno, el slot pasa de
 * {@link EstadoSlot#Disponible} a {@link EstadoSlot#Reservado} (trigger
 * {@code trg_actualizar_slot_al_reservar}). Al cancelarse el turno, el slot vuelve
 * a {@link EstadoSlot#Disponible} (trigger {@code trg_liberar_slot_al_cancelar}).</p>
 *
 * <p><strong>Regla de negocio RN-08:</strong> un slot solo puede tener un turno activo
 * a la vez. Este control lo implementa el trigger {@code trg_verificar_slot_disponible}.</p>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code slot_agenda}, PK {@code id_slot},
 * FK {@code id_agenda → agenda_disponibilidad}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see AgendaDisponibilidad
 * @see Turno
 */
public class SlotAgenda {

    /** Identificador único del slot (PK en BD). */
    private int idSlot;

    /** Agenda de la que proviene este slot (FK {@code id_agenda → agenda_disponibilidad}). */
    private AgendaDisponibilidad agenda;

    /** Fecha concreta del slot (día particular, no el día de la semana de la agenda). */
    private LocalDate fecha;

    /** Hora de inicio del slot. */
    private LocalTime hora;

    /** Estado de disponibilidad del slot. */
    private EstadoSlot estado;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Estado por defecto: {@link EstadoSlot#Disponible}.
     */
    public SlotAgenda() {
        this.estado = EstadoSlot.Disponible;
    }

    /**
     * Constructor completo con todos los atributos del slot.
     *
     * @param idSlot  identificador único (PK en BD)
     * @param agenda  agenda de origen; no puede ser nula
     * @param fecha   fecha concreta del slot; no puede ser nula
     * @param hora    hora de inicio del slot; no puede ser nula
     * @param estado  estado de disponibilidad
     */
    public SlotAgenda(int idSlot, AgendaDisponibilidad agenda,
                      LocalDate fecha, LocalTime hora, EstadoSlot estado) {
        this.idSlot = idSlot;
        setAgenda(agenda);
        setFecha(fecha);
        setHora(hora);
        this.estado = (estado != null) ? estado : EstadoSlot.Disponible;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único del slot.
     *
     * @return ID del slot
     */
    public int getIdSlot() {
        return idSlot;
    }

    /**
     * Establece el identificador único del slot.
     *
     * @param idSlot ID del slot; debe ser >= 0
     */
    public void setIdSlot(int idSlot) {
        if (idSlot < 0) {
            throw new IllegalArgumentException("El ID de slot no puede ser negativo.");
        }
        this.idSlot = idSlot;
    }

    /**
     * Retorna la agenda de la que proviene este slot.
     *
     * @return agenda de origen
     */
    public AgendaDisponibilidad getAgenda() {
        return agenda;
    }

    /**
     * Establece la agenda de origen. No puede ser nula.
     *
     * @param agenda agenda de origen
     * @throws IllegalArgumentException si la agenda es nula
     */
    public void setAgenda(AgendaDisponibilidad agenda) {
        if (agenda == null) {
            throw new IllegalArgumentException("La agenda del slot no puede ser nula.");
        }
        this.agenda = agenda;
    }

    /**
     * Retorna la fecha concreta del slot.
     *
     * @return fecha del slot
     */
    public LocalDate getFecha() {
        return fecha;
    }

    /**
     * Establece la fecha concreta del slot. No puede ser nula.
     *
     * @param fecha fecha del slot
     * @throws IllegalArgumentException si la fecha es nula
     */
    public void setFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del slot no puede ser nula.");
        }
        this.fecha = fecha;
    }

    /**
     * Retorna la hora de inicio del slot.
     *
     * @return hora del slot
     */
    public LocalTime getHora() {
        return hora;
    }

    /**
     * Establece la hora de inicio del slot. No puede ser nula.
     *
     * @param hora hora de inicio
     * @throws IllegalArgumentException si la hora es nula
     */
    public void setHora(LocalTime hora) {
        if (hora == null) {
            throw new IllegalArgumentException("La hora del slot no puede ser nula.");
        }
        this.hora = hora;
    }

    /**
     * Retorna el estado de disponibilidad del slot.
     *
     * @return estado del slot
     */
    public EstadoSlot getEstado() {
        return estado;
    }

    /**
     * Establece el estado de disponibilidad del slot.
     *
     * @param estado estado del slot; no puede ser nulo
     * @throws IllegalArgumentException si el estado es nulo
     */
    public void setEstado(EstadoSlot estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del slot no puede ser nulo.");
        }
        this.estado = estado;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del slot para la UI.
     *
     * @return cadena con fecha, hora y estado del slot
     */
    @Override
    public String toString() {
        return fecha + " " + hora + " [" + estado + "]";
    }

    /**
     * Compara dos slots por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SlotAgenda otro = (SlotAgenda) obj;
        return this.idSlot == otro.idSlot;
    }

    /**
     * Hash code basado en el ID del slot.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idSlot);
    }
}
