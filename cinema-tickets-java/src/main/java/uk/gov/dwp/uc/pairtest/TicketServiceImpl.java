package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;

public class TicketServiceImpl implements TicketService {

    private static int MAX_NUM_TICKETS = 20;
    private static int PRICE_ADULT = 20;
    private static int PRICE_CHILD = 10;
    private static int PRICE_INFANT = 0;


    private long numAdults(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).
                filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.ADULT).count();
    }

    private long numChildren(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).
                filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.CHILD).count();
    }

    private long numInfants(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).
                filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.INFANT).count();
    }


    private boolean checkValidRequest(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests.length > MAX_NUM_TICKETS) {
            return false;
        }

        long numAdults = numAdults(ticketTypeRequests);
        long numInfants = numInfants(ticketTypeRequests);

        //lazy evaluation in purchaseTickets ensures this request is nonempty, so 0 adults implies
        //unattended children. Furthermore each infant needs to sit on an adults lap
        return numAdults != 0 && numInfants <= numAdults;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        TicketPaymentService paymentService = new TicketPaymentServiceImpl();
        SeatReservationService seatReservationService = new SeatReservationServiceImpl();

        if (accountId <= 0 || ticketTypeRequests.length == 0 || !checkValidRequest(ticketTypeRequests)) {
            throw new InvalidPurchaseException();
        }

        int numAdults = (int) numAdults(ticketTypeRequests);
        int numChildren = (int) numChildren(ticketTypeRequests);
        int numInfants = (int) numInfants(ticketTypeRequests);

        paymentService.makePayment(accountId,
                numAdults * PRICE_ADULT + numChildren * PRICE_CHILD + numInfants * PRICE_INFANT);

        int numSeats = numAdults + numChildren;
        seatReservationService.reserveSeat(numSeats, numSeats);
    }

}
