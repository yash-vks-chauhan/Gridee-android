// Initialize Lenis Smooth Scroll
const lenis = new Lenis({
    duration: 1.2,
    easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
    direction: 'vertical',
    gestureDirection: 'vertical',
    smooth: true,
    mouseMultiplier: 1,
    smoothTouch: false,
    touchMultiplier: 2,
});

function raf(time) {
    lenis.raf(time);
    requestAnimationFrame(raf);
}

requestAnimationFrame(raf);

// Sticky Header & Dynamic Title Logic
const stickyHeader = document.querySelector('.sticky-header');
const stickyTitle = document.querySelector('.sticky-title');
const heroTitle = document.querySelector('h1');
const topBtn = document.querySelector('.sticky-action');

// Store original title (e.g., "Gridee Privacy")
const originalTitle = stickyTitle ? stickyTitle.textContent : '';

// Update Top Button to use Lenis
if (topBtn) {
    topBtn.onclick = (e) => {
        e.preventDefault();
        lenis.scrollTo(0);
    };
}

// Helper to smoothly update title
function updateStickyTitle(newText) {
    if (!stickyTitle || stickyTitle.textContent === newText) return;

    stickyTitle.style.opacity = '0';

    setTimeout(() => {
        stickyTitle.textContent = newText;
        stickyTitle.style.opacity = '1';
    }, 200); // Matches CSS transition
}

// Use Lenis scroll event for sticky header visibility
if (stickyHeader && heroTitle) {
    lenis.on('scroll', ({ scroll }) => {
        const heroBottom = heroTitle.offsetTop + heroTitle.offsetHeight;

        if (scroll > heroBottom) {
            stickyHeader.classList.add('visible');
        } else {
            stickyHeader.classList.remove('visible');
            // Reset title when hidden
            updateStickyTitle(originalTitle);
        }
    });
}

// Focus Mode & Cascade Reveal
const observerOptions = {
    root: null,
    rootMargin: '-25% 0px -25% 0px',
    threshold: 0
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('active');
        } else {
            entry.target.classList.remove('active');
        }
    });
}, observerOptions);

document.querySelectorAll('.content-card').forEach(section => {
    observer.observe(section);
});

// Dynamic Title Observer (Tracks which section is reading)
const titleObserverOptions = {
    root: null,
    rootMargin: '-10% 0px -60% 0px', // Active when section is near the top
    threshold: 0
};

const titleObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const sectionTitle = entry.target.querySelector('h2');
            if (sectionTitle) {
                updateStickyTitle(sectionTitle.textContent);
            }
        }
    });
}, titleObserverOptions);

// Only observe sections that have an ID (major sections)
document.querySelectorAll('.content-card[id]').forEach(section => {
    titleObserver.observe(section);
});

// Smart Email Copy & Magnetic Button Logic
const emailBtn = document.getElementById('emailBtn');
const tooltip = document.querySelector('.copy-tooltip');

if (emailBtn) {
    // Magnetic Effect
    emailBtn.addEventListener('mousemove', (e) => {
        const rect = emailBtn.getBoundingClientRect();
        const x = e.clientX - rect.left - rect.width / 2;
        const y = e.clientY - rect.top - rect.height / 2;

        // Strength of the magnet (0.3 = 30% movement)
        emailBtn.style.transform = `translate(${x * 0.3}px, ${y * 0.5}px)`;
    });

    emailBtn.addEventListener('mouseleave', () => {
        emailBtn.style.transform = 'translate(0, 0)';
    });

    // Copy Logic
    emailBtn.addEventListener('click', () => {
        const email = emailBtn.textContent;
        navigator.clipboard.writeText(email).then(() => {
            if (tooltip) {
                tooltip.classList.add('visible');
                setTimeout(() => {
                    tooltip.classList.remove('visible');
                }, 2000);
            }
        });
    });
}
