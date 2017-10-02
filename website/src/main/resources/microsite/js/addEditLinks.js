---
---
const width = window.document.documentElement.clientWidth;
const nav = document.querySelector('.nav .pull-right')
// The edit link is too large below this width and it's not useful anyway,
// as the user is probably on a mobile device.
if (width > 500 && nav) {
  const baseUrl = '{{ site.baseurl }}';
  const docsUrl = '{{ site.docsUrl }}';
  let relativePath = window.location.pathname.replace(`${baseUrl}/${docsUrl}/`, '').replace('.html', '.md');
  if (relativePath === '') {
    relativePath = 'index.md'
  }
  const repo = '{{ site.githubOwner }}/{{ site.githubRepo }}';
  const branch = 'microsite';
  const basePath = `https://github.com/${repo}/edit/${branch}/website/src/main/tut/${docsUrl}`;
  const target = `${basePath}/${relativePath}`;
  const editLink = document.createElement('li');
  const text = 'EDIT THIS PAGE ON GITHUB';
  editLink.innerHTML = `<a href='${target}' target='_blank'><span>${text}</span></a>`
  document.querySelector('.nav .pull-right').appendChild(editLink);
  nav.appendChild(editLink);
}
