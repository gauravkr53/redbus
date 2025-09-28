package com.redbus.controller;

import com.redbus.dto.SearchResponseItem;
import com.redbus.model.Booking;
import com.redbus.model.City;
import com.redbus.model.TripPart;
import com.redbus.service.AuthService;
import com.redbus.service.InventoryService;
import com.redbus.service.OrderService;
import com.redbus.service.PaymentService;
import com.redbus.pricing.PricingService;
import com.redbus.repository.CityRepository;
import com.redbus.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {
    private final InventoryService inventoryService;
    private final CityRepository cityRepository;
    private final AuthService authService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;
    private final PricingService pricingService;

    @GetMapping("/")
    public String home() {
        return "login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, 
                       @RequestParam String password,
                       HttpServletRequest request) {
        try {
            System.out.println("Login request received for email: " + email);
            String token = authService.login(email, password);

            request.getSession().setAttribute("token", token);
            request.getSession().setAttribute("userId", authService.extractUserIdFromToken(token));

            return "redirect:/search";
        } catch (Exception e) {
            return "redirect:/login?error=true";
        }
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email, 
                        @RequestParam String password,
                        HttpServletRequest request) {
        try {
            authService.signup(email, password);
            return "redirect:/login?success=true";
        } catch (Exception e) {
            return "redirect:/signup?error=true";
        }
    }

    @GetMapping("/search")
    public String searchPage(Model model, HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        String userId = (String) request.getSession().getAttribute("userId");
        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }

        try {
            List<City> cities = cityRepository.findAll();
            System.out.println("Cities loaded: " + cities.size());
            model.addAttribute("cities", cities);
            return "search";
        } catch (Exception e) {
            System.out.println("Error loading cities: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/search")
    public String searchTrips(@RequestParam String date,
                             @RequestParam String sourceCityId,
                             @RequestParam String destCityId,
                             Model model, HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        String userId = (String) request.getSession().getAttribute("userId");
        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }

        List<SearchResponseItem> trips = inventoryService.search(sourceCityId, destCityId, date);
        List<City> cities = cityRepository.findAll();
        
        model.addAttribute("trips", trips);
        model.addAttribute("cities", cities);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedSourceCityId", sourceCityId);
        model.addAttribute("selectedDestCityId", destCityId);
        
        return "search";
    }

    @GetMapping("/book/{tripId}")
    public String bookingPage(@PathVariable String tripId,
                             @RequestParam String date,
                             @RequestParam String sourceCityId,
                             @RequestParam String destCityId,
                             Model model, HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        String userId = (String) request.getSession().getAttribute("userId");
        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }

        var trip = inventoryService.getTrip(tripId);
        if (trip.isEmpty()) {
            return "redirect:/search";
        }

        SearchResponseItem  searchResponseItem = inventoryService.getTripDetails(tripId, sourceCityId, destCityId);

        model.addAttribute("tripId", searchResponseItem.getTripId());
        model.addAttribute("busId", searchResponseItem.getBusId());
        model.addAttribute("sourceCityName", searchResponseItem.getSource());
        model.addAttribute("destCityName", searchResponseItem.getDest());
        model.addAttribute("date", searchResponseItem.getDate());
        model.addAttribute("sourceCityId", searchResponseItem.getSourceCityId());
        model.addAttribute("destCityId", searchResponseItem.getDestCityId());
        model.addAttribute("sourceTime", searchResponseItem.getSourceTime() != null ? searchResponseItem.getSourceTime() : "N/A");
        model.addAttribute("destTime", searchResponseItem.getDestTime() != null ? searchResponseItem.getDestTime() : "N/A");
        model.addAttribute("pricePaise", searchResponseItem.getPricePaise());
        model.addAttribute("availableSeats", searchResponseItem.getAvailableSeats());

        return "booking";
    }

    @PostMapping("/book")
    public String createBooking(@RequestParam String tripId,
                               @RequestParam String sourceCityId,
                               @RequestParam String destCityId,
                               @RequestParam int seats,
                               HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        String userId = (String) request.getSession().getAttribute("userId");

        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }
        
        try {
            Booking booking = orderService.createBooking(userId, tripId, sourceCityId, destCityId, seats);
            return "redirect:/payment/" + booking.getBookingId();
        } catch (Exception e) {
            return "redirect:/search?error=booking_failed&message=" + e.getMessage();
        }
    }

    @GetMapping("/payment/{bookingId}")
    public String paymentPage(@PathVariable String bookingId, Model model, HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        String userId = (String) request.getSession().getAttribute("userId");
        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }

        var booking = bookingRepository.findById(bookingId);
        if (booking.isEmpty()) {
            return "redirect:/search";
        }

        model.addAttribute("booking", booking.get());
        return "payment";
    }

    @PostMapping("/payment")
    public String processPayment(@RequestParam String bookingId,
                                @RequestParam long amountPaise,
                                HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        String userId = (String) request.getSession().getAttribute("userId");
        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }

        try {
            paymentService.initiatePayment(bookingId, userId, amountPaise, "credit");
            return "redirect:/booking-success/" + bookingId;
        } catch (Exception e) {
            return "redirect:/payment/" + bookingId + "?error=payment_failed";
        }
    }

    @GetMapping("/booking-success/{bookingId}")
    public String bookingSuccess(@PathVariable String bookingId, Model model, HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        if (token == null) {
            return "redirect:/login";
        }

        String userId = (String) request.getSession().getAttribute("userId");
        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }

        var booking = bookingRepository.findById(bookingId);
        if (booking.isEmpty()) {
            return "redirect:/search";
        }

        model.addAttribute("booking", booking.get());
        return "booking-success";
    }

    @GetMapping("/bookings/all-bookings")
    public String allBookingsPage(Model model, HttpServletRequest request) {
        String token = (String) request.getSession().getAttribute("token");
        if (token == null) {
            return "redirect:/login";
        }

        String userId = (String) request.getSession().getAttribute("userId");
        if (!authService.auth(token, userId)) {
            return "redirect:/login";
        }

        // Get all bookings using OrderService
        List<com.redbus.dto.BookingView> allBookings = orderService.getAllBookings(userId);

        model.addAttribute("allBookings", allBookings);

        return "all-bookings";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/login";
    }
}
