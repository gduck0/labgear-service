
package com.gduck0.labgear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gduck0.labgear.entity.Account;
import com.gduck0.labgear.entity.Equipment;
import com.gduck0.labgear.entity.Reservation;
import com.gduck0.labgear.entity.ReservationStatus;
import com.gduck0.labgear.exception.ResourceConflictException;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class ReservationStateTest {

    private Reservation newReservation() {
        Account account = new Account("test@example.com", "password");
        Equipment equipment = new Equipment(1, "노트북");

        return new Reservation(
                account,
                equipment,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 12));
    }

    @Test
    void newReservationStartsActive() {
        Reservation reservation = newReservation();

        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    void activeReservationCanBeRented() {
        Reservation reservation = newReservation();

        reservation.rent();

        assertEquals(ReservationStatus.RENTED, reservation.getStatus());
    }

    @Test
    void rentedReservationCannotBeRentedAgain() {
        Reservation reservation = newReservation();
        reservation.rent();

        assertThrows(ResourceConflictException.class, reservation::rent);

        assertEquals(ReservationStatus.RENTED, reservation.getStatus());
    }

    @Test
    void canceledReservationCannotBeRented() {
        Reservation reservation = newReservation();
        reservation.cancel();

        assertThrows(ResourceConflictException.class, reservation::rent);

        assertEquals(ReservationStatus.CANCELED, reservation.getStatus());
    }

    @Test
    void rentedReservationCanBeReturned() {
        Reservation reservation = newReservation();
        reservation.rent();

        reservation.returnEquipment();

        assertEquals(ReservationStatus.RETURNED, reservation.getStatus());
    }

    @Test
    void activeReservationCannotBeReturned() {
        Reservation reservation = newReservation();

        assertThrows(ResourceConflictException.class, reservation::returnEquipment);

        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    void returnedReservationCannotBeReturnedAgain() {
        Reservation reservation = newReservation();
        reservation.rent();
        reservation.returnEquipment();

        assertThrows(ResourceConflictException.class, reservation::returnEquipment);

        assertEquals(ReservationStatus.RETURNED, reservation.getStatus());
    }

    @Test
    void activeReservationCanBeCanceled() {
        Reservation reservation = newReservation();

        reservation.cancel();

        assertEquals(ReservationStatus.CANCELED, reservation.getStatus());
    }

    @Test
    void rentedReservationCannotBeCanceled() {
        Reservation reservation = newReservation();
        reservation.rent();

        assertThrows(ResourceConflictException.class, reservation::cancel);

        assertEquals(ReservationStatus.RENTED, reservation.getStatus());
    }

    @Test
    void returnedReservationCannotBeCanceled() {
        Reservation reservation = newReservation();
        reservation.rent();
        reservation.returnEquipment();

        assertThrows(ResourceConflictException.class, reservation::cancel);

        assertEquals(ReservationStatus.RETURNED, reservation.getStatus());
    }

    @Test
    void returnedReservationCannotBeRentedAgain() {
        Reservation reservation = newReservation();
        reservation.rent();
        reservation.returnEquipment();

        assertThrows(ResourceConflictException.class, reservation::rent);

        assertEquals(ReservationStatus.RETURNED, reservation.getStatus());
    }

    @Test
    void canceledReservationCannotBeCanceledAgain() {
        Reservation reservation = newReservation();
        reservation.cancel();

        assertThrows(ResourceConflictException.class, reservation::cancel);

        assertEquals(ReservationStatus.CANCELED, reservation.getStatus());
    }
}
