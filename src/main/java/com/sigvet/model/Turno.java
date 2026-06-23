package com.sigvet.model;

import com.sigvet.model.enums.EstadoTurno;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa un turno médico reservado en el sistema SIGVET.
 *
 * <p>Un turno vincula una {@link Mascota} con un {@link SlotAgenda} disponible.
 * El ciclo de vida del turno es gestionado por los SPs y triggers de la BD:</p>
 * <ul>
 *   <li>{@code sp_reservar_turno} – crea el turno y pasa el slot a {@code Reservado}.</li>
 *   <li>{@code sp_cancelar_turno} – cancela el turno y libera el slot.</li>
 *   <li>{@code sp_registrar_consulta_turno} – marca el turno como {@code Atendido}.</li>
 * </ul>
 *
 * <p><strong>Reglas de negocio:</strong></p>
 * <ul>
 *   <li>RN-04: Todo turno debe tener una mascota registrada.</li>
 *   <li>RN-05: El slot se bloquea con {@code SELECT FOR UPDATE} para evitar concurrencia.</li>
 *   <li>RN-08: Un slot solo puede tener un turno activo (trigger {@code trg_verificar_slot_disponible}).</li>
 *   <li>RN-12: El estado {@code Atendido} solo se asigna mediante {@code sp_registrar_consulta_turno}.</li>
 * </ul>
 *
 * <p><strong>Mapeo BD:</strong> tabla {@code turno}, PK {@code id_turno},
 * FKs {@code id_mascota → mascota}, {@code id_slot → slot_agenda}.</p>
 *
 * @author SIGVET
 * @version 1.0
 * @see Mascota
 * @see SlotAgenda
 * @see EstadoTurno
 */
public class Turno {

    /** Identificador único del turno (PK en BD). */
    private int idTurno;

    /** Mascota para la que se reservó el turno (FK {@code id_mascota → mascota}). */
    private Mascota mascota;

    /** Slot horario reservado (FK {@code id_slot → slot_agenda}). */
    private SlotAgenda slot;

    /** Motivo del turno expresado por el dueño (nullable). */
    private String motivo;

    /** Estado actual del turno en su ciclo de vida. */
    private EstadoTurno estado;

    /** Timestamp de registro del turno en el sistema. */
    private LocalDateTime fechaRegistro;

    // =========================================================================
    // Constructores
    // =========================================================================

    /**
     * Constructor vacío. Estado por defecto: {@link EstadoTurno#Pendiente}.
     */
    public Turno() {
        this.estado = EstadoTurno.Pendiente;
        this.fechaRegistro = LocalDateTime.now();
    }

    /**
     * Constructor completo con todos los atributos del turno.
     *
     * @param idTurno       identificador único (PK en BD)
     * @param mascota       mascota del turno; no puede ser nula (RN-04)
     * @param slot          slot horario reservado; no puede ser nulo
     * @param motivo        motivo del turno (puede ser nulo)
     * @param estado        estado actual del turno
     * @param fechaRegistro timestamp de creación del turno
     */
    public Turno(int idTurno, Mascota mascota, SlotAgenda slot,
                 String motivo, EstadoTurno estado, LocalDateTime fechaRegistro) {
        this.idTurno = idTurno;
        setMascota(mascota);
        setSlot(slot);
        this.motivo = motivo;
        this.estado = (estado != null) ? estado : EstadoTurno.Pendiente;
        this.fechaRegistro = (fechaRegistro != null) ? fechaRegistro : LocalDateTime.now();
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /**
     * Retorna el identificador único del turno.
     *
     * @return ID del turno
     */
    public int getIdTurno() {
        return idTurno;
    }

    /**
     * Establece el identificador único del turno.
     *
     * @param idTurno ID del turno; debe ser >= 0
     */
    public void setIdTurno(int idTurno) {
        if (idTurno < 0) {
            throw new IllegalArgumentException("El ID de turno no puede ser negativo.");
        }
        this.idTurno = idTurno;
    }

    /**
     * Retorna la mascota para la que se reservó el turno.
     *
     * @return mascota del turno
     */
    public Mascota getMascota() {
        return mascota;
    }

    /**
     * Establece la mascota del turno. No puede ser nula (RN-04).
     *
     * @param mascota mascota del turno
     * @throws IllegalArgumentException si la mascota es nula
     */
    public void setMascota(Mascota mascota) {
        if (mascota == null) {
            throw new IllegalArgumentException(
                "La mascota del turno no puede ser nula (RN-04).");
        }
        this.mascota = mascota;
    }

    /**
     * Retorna el slot horario reservado.
     *
     * @return slot del turno
     */
    public SlotAgenda getSlot() {
        return slot;
    }

    /**
     * Establece el slot horario del turno. No puede ser nulo.
     *
     * @param slot slot del turno
     * @throws IllegalArgumentException si el slot es nulo
     */
    public void setSlot(SlotAgenda slot) {
        if (slot == null) {
            throw new IllegalArgumentException("El slot del turno no puede ser nulo.");
        }
        this.slot = slot;
    }

    /**
     * Retorna el motivo del turno.
     *
     * @return motivo expresado por el dueño (puede ser nulo)
     */
    public String getMotivo() {
        return motivo;
    }

    /**
     * Establece el motivo del turno.
     *
     * @param motivo motivo del turno (puede ser nulo o vacío)
     */
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    /**
     * Retorna el estado actual del turno.
     *
     * @return estado del turno
     */
    public EstadoTurno getEstado() {
        return estado;
    }

    /**
     * Establece el estado del turno.
     *
     * @param estado estado del turno; no puede ser nulo
     * @throws IllegalArgumentException si el estado es nulo
     */
    public void setEstado(EstadoTurno estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del turno no puede ser nulo.");
        }
        this.estado = estado;
    }

    /**
     * Retorna el timestamp de creación del turno.
     *
     * @return fecha y hora de registro
     */
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Establece el timestamp de creación del turno.
     *
     * @param fechaRegistro timestamp de registro
     */
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    // =========================================================================
    // Métodos de Object
    // =========================================================================

    /**
     * Representación en texto del turno para la UI.
     *
     * @return descripción con slot, mascota y estado
     */
    @Override
    public String toString() {
        String nombreMascota = (mascota != null) ? mascota.getNombre() : "Sin mascota";
        String slotStr = (slot != null) ? slot.toString() : "Sin slot";
        return "Turno #" + idTurno + " - " + slotStr + " | " + nombreMascota + " [" + estado + "]";
    }

    /**
     * Compara dos turnos por su ID.
     *
     * @param obj objeto a comparar
     * @return {@code true} si tienen el mismo ID
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Turno otro = (Turno) obj;
        return this.idTurno == otro.idTurno;
    }

    /**
     * Hash code basado en el ID del turno.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(idTurno);
    }
}
