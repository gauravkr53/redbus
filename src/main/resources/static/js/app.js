// RedBus Web Application JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Initialize date picker to today's date
    const dateInput = document.getElementById('date');
    if (dateInput) {
        const today = new Date();
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);
        dateInput.min = tomorrow.toISOString().split('T')[0];
    }

    // Handle payment method selection
    const paymentMethods = document.querySelectorAll('.payment-method');
    paymentMethods.forEach(method => {
        method.addEventListener('click', function() {
            paymentMethods.forEach(m => m.classList.remove('selected'));
            this.classList.add('selected');
        });
    });

    // Handle form submissions with loading states
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.innerHTML = '<span class="loading"></span> Processing...';
                submitBtn.disabled = true;
            }
        });
    });

    // Auto-focus on first input
    const firstInput = document.querySelector('input[type="text"], input[type="email"], input[type="password"]');
    if (firstInput) {
        firstInput.focus();
    }

    // Handle search form validation
    const searchForm = document.getElementById('searchForm');
    if (searchForm) {
        searchForm.addEventListener('submit', function(e) {
            const sourceCity = document.getElementById('sourceCityId').value;
            const destCity = document.getElementById('destCityId').value;
            
            if (sourceCity === destCity) {
                e.preventDefault();
                alert('Source and destination cities cannot be the same.');
                return false;
            }
        });
    }

    // Handle seat selection
    const seatInput = document.getElementById('seats');
    if (seatInput) {
        seatInput.addEventListener('input', function() {
            const seats = parseInt(this.value);
            const maxSeats = parseInt(this.max) || 10;
            
            if (seats > maxSeats) {
                this.value = maxSeats;
                alert(`Maximum ${maxSeats} seats allowed.`);
            }
            
            if (seats < 1) {
                this.value = 1;
            }
            
            updatePrice();
        });
    }

    // Update price based on seats
    function updatePrice() {
        const seats = parseInt(document.getElementById('seats')?.value) || 1;
        const basePrice = parseFloat(document.getElementById('basePrice')?.value) || 0;
        const totalPrice = basePrice * seats;
        
        const priceDisplay = document.getElementById('totalPrice');
        if (priceDisplay) {
            priceDisplay.textContent = `₹${totalPrice.toFixed(2)}`;
        }
    }

    // Format price display
    function formatPrice(paise) {
        return (paise / 100).toFixed(2);
    }

    // Format time display
    function formatTime(timeString) {
        if (!timeString) return '';
        return timeString.substring(0, 5); // Remove seconds if present
    }

    // Show/hide loading overlay
    function showLoading() {
        let overlay = document.getElementById('loadingOverlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'loadingOverlay';
            overlay.innerHTML = '<div class="loading"></div>';
            overlay.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0,0,0,0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 9999;
            `;
            document.body.appendChild(overlay);
        }
        overlay.style.display = 'flex';
    }

    function hideLoading() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.style.display = 'none';
        }
    }

    // Handle AJAX form submissions
    const ajaxForms = document.querySelectorAll('form[data-ajax]');
    ajaxForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            showLoading();
            
            const formData = new FormData(this);
            const url = this.action;
            
            fetch(url, {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                hideLoading();
                if (data.success) {
                    if (data.redirect) {
                        window.location.href = data.redirect;
                    }
                } else {
                    alert(data.message || 'An error occurred');
                }
            })
            .catch(error => {
                hideLoading();
                console.error('Error:', error);
                alert('An error occurred. Please try again.');
            });
        });
    });

    // Auto-refresh search results every 30 seconds
    const searchResults = document.querySelector('.search-results');
    if (searchResults) {
        setInterval(function() {
            // Only refresh if no user interaction in last 10 seconds
            if (Date.now() - lastUserInteraction > 10000) {
                const form = document.getElementById('searchForm');
                if (form) {
                    form.submit();
                }
            }
        }, 30000);
    }

    let lastUserInteraction = Date.now();
    document.addEventListener('click', function() {
        lastUserInteraction = Date.now();
    });

    // Handle responsive navigation
    const navToggle = document.getElementById('navToggle');
    const navMenu = document.getElementById('navMenu');
    
    if (navToggle && navMenu) {
        navToggle.addEventListener('click', function() {
            navMenu.classList.toggle('active');
        });
    }

    // Smooth scrolling for anchor links
    const anchorLinks = document.querySelectorAll('a[href^="#"]');
    anchorLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    });

    // Initialize tooltips
    const tooltipElements = document.querySelectorAll('[data-tooltip]');
    tooltipElements.forEach(element => {
        element.addEventListener('mouseenter', function() {
            const tooltip = document.createElement('div');
            tooltip.className = 'tooltip';
            tooltip.textContent = this.getAttribute('data-tooltip');
            tooltip.style.cssText = `
                position: absolute;
                background: #333;
                color: white;
                padding: 5px 10px;
                border-radius: 4px;
                font-size: 12px;
                z-index: 1000;
                pointer-events: none;
            `;
            document.body.appendChild(tooltip);
            
            const rect = this.getBoundingClientRect();
            tooltip.style.left = rect.left + (rect.width / 2) - (tooltip.offsetWidth / 2) + 'px';
            tooltip.style.top = rect.top - tooltip.offsetHeight - 5 + 'px';
        });
        
        element.addEventListener('mouseleave', function() {
            const tooltip = document.querySelector('.tooltip');
            if (tooltip) {
                tooltip.remove();
            }
        });
    });
});
