const ctaButton = document.querySelector('.jumbotron .container p.text-center a');
if (ctaButton) {
  ctaButton.innerHTML = 'Get started';
  ctaButton.setAttribute('href', 'docs/users/installation');
}
