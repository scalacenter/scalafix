const menuItems = document.querySelectorAll('.nav .pull-right li');
const twitterElement = menuItems[2];
const fbElement = menuItems[3];
const gplusElement = menuItems[4];

if (twitterElement) {
  twitterElement.remove();
}

if (fbElement) {
  fbElement.remove();
}

if (gplusElement) {
  gplusElement.remove();
}
