// See https://docusaurus.io/docs/site-config.html for all the possible
// site configuration options.

const repoUrl = "https://github.com/scalacenter/scalafix";

const siteConfig = {
  title: "Scalafix",
  tagline: "Refactoring and linting tool for Scala",
  url: "https://scalacenter.github.io/",
  baseUrl: "/scalafix/",

  // Used for publishing and more
  projectName: "scalafix",
  organizationName: "scalacenter",

  algolia: {
    apiKey: "???",
    indexName: "scalafix"
  },

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    { doc: "users/installation", label: "Docs" },
    { doc: "rules/overview", label: "Rules" },
    { href: repoUrl, label: "GitHub", external: true }
  ],

  // If you have users set above, you add it here:
  // users,

  /* path to images for header/footer */
  headerIcon: "img/scalacenter2x.png",
  footerIcon: "img/scalacenter2x.png",
  favicon: "img/favicon.png",

  /* colors for website */
  colors: {
    primaryColor: "#1F3E4B",
    secondaryColor: "#15242B"
  },

  customDocsPath: "website/target/docs",

  // This copyright info is used in /core/Footer.js and blog rss/atom feeds.
  copyright: `Copyright © ${new Date().getFullYear()} Scala Center`,

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks
    theme: "github"
  },

  /* On page navigation for the current documentation page */
  onPageNav: "separate",

  /* Open Graph and Twitter card images */
  ogImage: "img/scalameta-logo.png",
  twitterImage: "img/scalameta-logo.png",

  editUrl: `${repoUrl}/edit/master/docs/`,

  repoUrl
};

module.exports = siteConfig;
