---
title: Gridee Privacy Policy
layout: default
---

<div class="page-wrapper">

  <!-- Dynamic Island Sticky Header -->
  <div class="sticky-header">
    <span class="sticky-title">Gridee Privacy</span>
    <span class="sticky-action" onclick="window.scrollTo({top: 0, behavior: 'smooth'})">Top ↑</span>
  </div>

  <div class="hero-header">
    <h1>Privacy Policy</h1>
    <div class="last-updated">Last Updated: December 11, 2025</div>
  </div>

  <!-- At a Glance Section -->
  <div class="feature-grid">
    <a href="#protection" class="feature-item">
      <span class="feature-icon icon-lock">🔒</span>
      <span class="feature-title">Secure</span>
      <p class="feature-desc">Bank-grade encryption for all data</p>
    </a>
    <a href="#collection" class="feature-item">
      <span class="feature-icon icon-eye">👁️</span>
      <span class="feature-title">Transparent</span>
      <p class="feature-desc">No hidden tracking or data selling</p>
    </a>
    <a href="#rights" class="feature-item">
      <span class="feature-icon icon-gear">⚙️</span>
      <span class="feature-title">Control</span>
      <p class="feature-desc">You own your data completely</p>
    </a>
  </div>

  <div class="content-card">
    <h2>Welcome to Gridee</h2>
    <p>At Gridee, your privacy and trust are our top priorities. This Privacy Policy explains in clear, simple terms how we collect, use, protect, and handle your personal information when you use our smart parking management app.</p>
    <p><strong>Our Promise:</strong> We believe in complete transparency. We will never sell your personal data, and we only collect what's necessary to provide you with a seamless parking experience.</p>
  </div>

  <div class="content-card" id="collection">
    <h2>Information We Collect</h2>
    <p>We only collect information that helps us deliver a better parking experience to you.</p>

    <h3>1. Account Information</h3>
    <p><strong>What we collect:</strong> Name and email address via Google Sign-In.<br>
    <strong>Why we need it:</strong> To create your account, authenticate you securely, and personalize your experience.</p>

    <h3>2. Camera Access</h3>
    <p><strong>What we collect:</strong> QR code scans and license plate images captured through your camera.<br>
    <strong>Why we need it:</strong> To enable quick check-in/check-out at parking facilities and automatic vehicle recognition.<br>
    <strong>Note:</strong> Images are processed locally. We never store the actual photos.</p>

    <h3>3. Location Data</h3>
    <p><strong>What we collect:</strong> Approximate and precise location (when you grant permission).<br>
    <strong>Why we need it:</strong> To show you nearby parking spots and provide navigation.</p>

    <h3>4. Payment Information</h3>
    <p><strong>What we collect:</strong> Transaction IDs and payment status.<br>
    <strong>Security:</strong> We NEVER store your full credit/debit card details. All processing is handled by Razorpay.</p>
  </div>

  <div class="content-card">
    <h2>How We Use Your Information</h2>
    <ul>
      <li><strong>Provide Core Services:</strong> Authenticate accounts, process bookings, and enable parking features.</li>
      <li><strong>Ensure Security:</strong> Detect fraud and prevent unauthorized access.</li>
      <li><strong>Improve Experience:</strong> Analyze usage patterns to fix bugs and enhance features.</li>
      <li><strong>Communication:</strong> Send booking confirmations and important service updates.</li>
    </ul>
    <p><strong>We DO NOT:</strong> Sell your data, send spam, or share data with advertisers.</p>
  </div>

  <div class="content-card" id="protection">
    <h2>Data Retention & Protection</h2>
    <p>We retain your data only as long as your account is active or as required by law. You can request deletion at any time.</p>
    <p><strong>Security Measures:</strong></p>
    <ul>
      <li>End-to-end encryption (SSL/TLS)</li>
      <li>PCI-DSS compliant payment processing</li>
      <li>Strict access controls and regular security audits</li>
    </ul>
  </div>

  <div class="content-card" id="rights">
    <h2>Your Rights</h2>
    <p>You have full control over your personal information:</p>
    <ul>
      <li><strong>Access:</strong> Request a copy of your data.</li>
      <li><strong>Correction:</strong> Update your profile in settings.</li>
      <li><strong>Deletion:</strong> Delete your account and data instantly.</li>
      <li><strong>Permissions:</strong> Manage camera and location access in device settings.</li>
    </ul>
  </div>

  <div class="contact-section">
    <h2>Questions?</h2>
    <p>If you have any concerns about your privacy, we are here to help.</p>
    <div class="email-container">
      <button class="email-link" id="emailBtn">gridee.business@gmail.com</button>
      <div class="copy-tooltip">Copied!</div>
    </div>
  </div>

  <footer class="site-footer">
    <p>&copy; 2025 Gridee. All rights reserved.</p>
    <p>Governed by the laws of India.</p>
  </footer>

<script>
  // Sticky Header Logic
  const stickyHeader = document.querySelector('.sticky-header');
  const heroTitle = document.querySelector('h1');
  
  window.addEventListener('scroll', () => {
    const scrollPos = window.scrollY;
    const heroBottom = heroTitle.offsetTop + heroTitle.offsetHeight;

    // Sticky Header (Dynamic Island)
    if (scrollPos > heroBottom) {
      stickyHeader.classList.add('visible');
    } else {
      stickyHeader.classList.remove('visible');
    }
  });

  // Smart Email Copy Logic
  const emailBtn = document.getElementById('emailBtn');
  const tooltip = document.querySelector('.copy-tooltip');

  emailBtn.addEventListener('click', () => {
    const email = emailBtn.textContent;
    navigator.clipboard.writeText(email).then(() => {
      tooltip.classList.add('visible');
      setTimeout(() => {
        tooltip.classList.remove('visible');
      }, 2000);
    });
  });
</script>
