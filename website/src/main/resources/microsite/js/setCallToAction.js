---
---
const ctaButton = document.querySelector('.jumbotron .container p.text-center a');
ctaButton.innerHTML = '{{ site.callToActionText }}';
ctaButton.setAttribute('href', '{{ site.callToActionUrl }}');
