import { useEffect } from 'react';
import Lenis from '@studio-freight/lenis';

export const usePageEffects = () => {
    useEffect(() => {
        // Initialize Lenis Smooth Scroll
        const lenis = new Lenis({
            duration: 1.2,
            easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
            touchMultiplier: 2,
        });

        function raf(time: number) {
            lenis.raf(time);
            requestAnimationFrame(raf);
        }

        requestAnimationFrame(raf);

        // Sticky Header & Dynamic Title Logic
        const stickyHeader = document.querySelector('.sticky-header');
        const stickyTitle = document.querySelector('.sticky-title');
        const heroTitle = document.querySelector('h1');
        const topBtn = document.querySelector('.sticky-action') as HTMLElement;

        // Store original title
        const originalTitle = stickyTitle ? stickyTitle.textContent : '';

        // Update Top Button to use Lenis
        if (topBtn) {
            topBtn.onclick = (e) => {
                e.preventDefault();
                lenis.scrollTo(0);
            };
        }

        // Helper to smoothly update title
        function updateStickyTitle(newText: string) {
            if (!stickyTitle || stickyTitle.textContent === newText) return;
            (stickyTitle as HTMLElement).style.opacity = '0';
            setTimeout(() => {
                stickyTitle.textContent = newText;
                (stickyTitle as HTMLElement).style.opacity = '1';
            }, 200);
        }

        // Use Lenis scroll event for sticky header visibility
        if (stickyHeader && heroTitle) {
            lenis.on('scroll', ({ scroll }: { scroll: number }) => {
                const heroBottom = heroTitle.offsetTop + heroTitle.offsetHeight;
                if (scroll > heroBottom) {
                    stickyHeader.classList.add('visible');
                } else {
                    stickyHeader.classList.remove('visible');
                    updateStickyTitle(originalTitle || '');
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

        // Dynamic Title Observer
        const titleObserverOptions = {
            root: null,
            rootMargin: '-10% 0px -60% 0px',
            threshold: 0
        };

        const titleObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const sectionTitle = entry.target.querySelector('h2');
                    if (sectionTitle && sectionTitle.textContent) {
                        updateStickyTitle(sectionTitle.textContent);
                    }
                }
            });
        }, titleObserverOptions);

        document.querySelectorAll('.content-card[id]').forEach(section => {
            titleObserver.observe(section);
        });

        // Smart Email Copy & Magnetic Button Logic
        const emailBtn = document.getElementById('emailBtn');
        const tooltip = document.querySelector('.copy-tooltip');

        if (emailBtn) {
            const handleMouseMove = (e: MouseEvent) => {
                const rect = emailBtn.getBoundingClientRect();
                const x = e.clientX - rect.left - rect.width / 2;
                const y = e.clientY - rect.top - rect.height / 2;
                emailBtn.style.transform = `translate(${x * 0.3}px, ${y * 0.5}px)`;
            };

            const handleMouseLeave = () => {
                emailBtn.style.transform = 'translate(0, 0)';
            };

            const handleClick = () => {
                const email = emailBtn.textContent || '';
                navigator.clipboard.writeText(email).then(() => {
                    if (tooltip) {
                        tooltip.classList.add('visible');
                        setTimeout(() => {
                            tooltip.classList.remove('visible');
                        }, 2000);
                    }
                });
            };

            emailBtn.addEventListener('mousemove', handleMouseMove);
            emailBtn.addEventListener('mouseleave', handleMouseLeave);
            emailBtn.addEventListener('click', handleClick);

            return () => {
                lenis.destroy();
                observer.disconnect();
                titleObserver.disconnect();
                emailBtn.removeEventListener('mousemove', handleMouseMove);
                emailBtn.removeEventListener('mouseleave', handleMouseLeave);
                emailBtn.removeEventListener('click', handleClick);
            };
        }

        return () => {
            lenis.destroy();
            observer.disconnect();
            titleObserver.disconnect();
        };
    }, []);
};
